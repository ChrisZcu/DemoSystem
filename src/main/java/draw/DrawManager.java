package draw;

import app.DemoInterface;
import de.fhpotsdam.unfolding.UnfoldingMap;
import model.BlockType;
import model.TrajBlock;
import model.Trajectory;
import processing.core.PGraphics;
import util.IOHandle;
import util.ParamSettings;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Manage the draw workers and divide the draw task.
 * Then provide draw results (PGraph) to app interface.
 *
 * @see DrawWorker
 * @see app.DemoInterface
 */
public class DrawManager {
    private final DemoInterface app;
    private final ParamSettings ps;
    private final UnfoldingMap map;
    private final PGraphics[][] trajImageMtx;
    private final int[] trajCnt;        // # of traj that already drawn.

    public Trajectory[] trajFull = null;
    private Trajectory[][][] trajVfgsMatrix = null;     // trajVfgs for delta X rate
    private Trajectory[][] trajRandList = null;         // trajRand for rate

    private final int[] deltaList;
    private final double[] rateList;

    private final TrajBlock[] blockList;
    private final DrawWorker[][] drawWorkerMtx;
    private final boolean[] refreshList;

    private boolean showRenderTime = false;
    private int width, height;      // size for one map view

    // multi-thread for part image painting
    private final ExecutorService threadPool;

    // single thread pool for images controlling
    private final ExecutorService controlPool;
    private Thread controlThread;

    public DrawManager(DemoInterface app, ParamSettings ps, UnfoldingMap map,
                       PGraphics[][] trajImageMtx, int[] trajCnt) {
        this.app = app;
        this.ps = ps;
        this.map = map;
        this.trajImageMtx = trajImageMtx;
        this.trajCnt = trajCnt;

        this.deltaList = ps.DELTA_LIST;
        this.rateList = ps.RATE_LIST;
        this.blockList = new TrajBlock[4];
        for (int i = 0; i < 4; i++) {
            blockList[i] = new TrajBlock();
        }

        int len = Math.max(ps.FULL_THREAD_NUM, ps.SAMPLE_TRHEAD_NUM);
        this.drawWorkerMtx = new DrawWorker[4][len];

        // init pool
        this.threadPool = new ThreadPoolExecutor(len, len, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>()) {{
            // drop last thread
            this.setRejectedExecutionHandler(new DiscardOldestPolicy());
        }};
        this.controlPool = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>()) {{
            // drop last thread and begin next
            this.setRejectedExecutionHandler(new DiscardOldestPolicy());
        }};


        this.refreshList = new boolean[4];
        Arrays.fill(refreshList, true);
    }

    /**
     * Inner class for the task that starts all painting workers.
     */
    private class DrawWorkerStarter extends Thread {
        @Override
        public void run() {
            for (int mapIdx = 0; mapIdx < 4; mapIdx++) {
                if (!refreshList[mapIdx]) {
                    continue;       // this map view won't be refreshed
                }

                // start painting tasks
                TrajBlock tb = blockList[mapIdx];
                int totLen = tb.getTrajList().length;
                int threadNum = tb.getThreadNum();
                int segLen = totLen / threadNum;
                DrawWorker[] DrawWorkerList = drawWorkerMtx[mapIdx];

                for (int idx = 0; idx < threadNum; idx++) {
                    int begin = segLen * idx;
                    int end = Math.min(begin + segLen, totLen);    // exclude
                    DrawWorker worker = new DrawWorker(map, app.createGraphics(width, height),
                            trajImageMtx[mapIdx], tb.getTrajList(), trajCnt,
                            mapIdx, idx, begin, end);

                    DrawWorkerList[idx] = worker;
                    threadPool.submit(worker);
                }
            }
        }
    }

    /**
     * Update traj painting according to two param list.
     * if optViewIdx == -1, update all forcibly
     */
    public void startNewRenderTask(int optViewIdx, boolean[] viewVisibleList,
                                   boolean[] linkedList) {
        // verify which view need to be changed according to two boolean list.
        if (optViewIdx == -1) {
            // -1: refresh all traj (test)
            Arrays.fill(refreshList, true);
        } else if (linkedList[optViewIdx]) {
            // this view is linked
            for (int mapIdx = 0; mapIdx < 4; mapIdx++) {
                // refresh iff it is linked and is visible
                refreshList[mapIdx] = linkedList[mapIdx] && viewVisibleList[mapIdx];
                interruptUnfinished(mapIdx);
            }
        } else {
            // not linked
            refreshList[optViewIdx] = viewVisibleList[optViewIdx];
        }
        updateTrajImages();
    }

    /**
     * Update the traj painting for specific map view
     */
    public void startNewRenderTaskFor(int optViewIdx) {
        for (int i = 0; i < 4; i++) {
            refreshList[i] = (i == optViewIdx);
        }
        interruptUnfinished(optViewIdx);
        updateTrajImages();
    }

    /**
     * Clean the unfinished thread of this map
     */
    private void interruptUnfinished(int mapIdx) {
        DrawWorker[] drawWorkerList = drawWorkerMtx[mapIdx];
        int threadNum = blockList[mapIdx].getThreadNum();
        for (int idx = 0; idx < threadNum; idx++) {
            drawWorkerList[idx].interrupt();
        }
    }


    /**
     * Start multi-thread (by start a control thread)
     * and paint traj to flash images separately.
     */
    public void updateTrajImages() {
        if (controlThread != null) {
            controlThread.interrupt();
        }

        // create new control thread
        controlThread = new DrawWorkerStarter();

        controlThread.setPriority(10);
        controlPool.submit(controlThread);
    }

    public int getTrajFullNum() {
        return trajFull.length;
    }

    public int getPointNum() {
        int ret = 0;
        for (Trajectory traj : trajFull) {
            ret += traj.getLocations().length;
        }
        return ret;
    }

    public void setBlockAt(int idx, BlockType type, int deltaIdx, int rateIdx) {
        Trajectory[] trajList;
        int threadNum;
        switch (type) {
            case FULL:
                trajList = trajFull;
                threadNum = ps.FULL_THREAD_NUM;
                break;
            case VFGS:
                trajList = trajVfgsMatrix[deltaIdx][rateIdx];
                threadNum = ps.SAMPLE_THREAD_NUM;
                break;
            case RAND:
                trajList = trajRandList[rateIdx];
                threadNum = ps.SAMPLE_THREAD_NUM;
                break;
            case PART:
                trajList = null;
                threadNum = ps.SAMPLE_THREAD_NUM;
                break;
            default:
                // never go here
                throw new IllegalArgumentException("Can't handle t" +
                        "his block type : " + type);
        }

        blockList[idx].setNewBlock(type, trajList, threadNum,
                deltaIdx, rateIdx);
    }

    public void setShowRenderTime(boolean showRenderTime) {
        this.showRenderTime = showRenderTime;
    }

    /**
     * Load trajectory data from file (FULL)
     * Then generate VFGS and RAND
     */
    public void load() throws IOException {
        trajFull = IOHandle.loadRowData(ps.ORIGIN_PATH, ps.LIMIT);
        int[] rateCntList = IOHandle.translateRate(trajFull.length, rateList);
        initTrajVfgsMatrix(rateCntList);
        initTrajRandList(rateCntList);
    }

    /**
     * Init {@link #trajVfgsMatrix} from {@link ps.RES_PATH}.
     */
    private void initTrajVfgsMatrix(int[] rateCntList) throws IOException {
        int dLen = deltaList.length;
        int rLen = rateList.length;
        trajVfgsMatrix = new Trajectory[dLen][rLen][];

        for (int dIdx = 0; dIdx < dLen; dIdx++) {
            int[] vfgsRes = IOHandle.loadVfgsRes(ps.RES_PATH, deltaList[dIdx], rateCntList);

            for (int rIdx = 0; rIdx < rLen; rIdx++) {
                int rateCnt = rateCntList[rIdx];
                Trajectory[] trajVfgs = new Trajectory[rateCnt];
                for (int i = 0; i < rateCnt; i++) {
                    trajVfgs[i] = trajFull[vfgsRes[i]];
                }
                trajVfgsMatrix[dIdx][rIdx] = trajVfgs;
            }
        }
    }

    /**
     * Init {@link trajRandList}.
     */
    private void initTrajRandList(int[] rateCntList) {
        Random rand = new Random();
        int dLen = rateCntList.length;
        trajRandList = new Trajectory[dLen][];

        for (int dIdx = 0; dIdx < dLen; dIdx++) {
            int rateCnt = rateCntList[dIdx];
            Trajectory[] trajRand = new Trajectory[rateCnt];
            HashSet<Integer> set = new HashSet<>(rateCnt * 4 / 3 + 1);

            int cnt = 0;
            while (cnt < trajRand.length) {
                int idx = rand.nextInt(trajFull.length);
                if (set.contains(idx)) {
                    continue;
                }
                set.add(idx);
                trajRand[cnt++] = trajFull[idx];
            }

            trajRandList[dIdx] = trajRand;
        }
    }
}
