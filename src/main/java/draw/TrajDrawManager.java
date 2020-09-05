package draw;

import app.DemoInterface;
import app.SharedObject;
import de.fhpotsdam.unfolding.UnfoldingMap;
import model.BlockType;
import model.TrajBlock;
import model.Trajectory;
import processing.core.PApplet;
import processing.core.PGraphics;
import util.PSC;

import java.awt.*;
import java.util.Arrays;
import java.util.concurrent.*;

/**
 * Manage the draw workers and divide the draw task.
 * Then provide draw results (PGraph) to app interface.
 * <br> It will ask {@link app.SharedObject} for the data.
 * <p>
 * It can handle the task for both main result
 * and the double select result.
 *
 * @see TrajDrawWorker
 * @see app.DemoInterface
 */
public class TrajDrawManager {
    private final PApplet app;
    private final UnfoldingMap[] mapList;
    private final PGraphics[][] trajImageMtx;       // for main traj result
    private final PGraphics[][] trajImageSltMtx;    // for double select result
    private final int[] trajCnt;        // # of traj that already drawn.

    private final TrajBlock[] blockList;    // for both (main & double select)
    // workers for main traj result
    private final TrajDrawWorker[][] trajDrawWorkerMtx;
    // workers for double select result
    private final TrajDrawWorker[][] trajDrawSltWorkerMtx;
    private final boolean[] refreshList;

    private final float[] mapXList, mapYList;
    private final int width, height;        // size for one map view

    // multi-thread for part image painting
    private final ExecutorService threadPool;

    // single thread pool for images controlling
    private final ExecutorService controlPool;
    private Thread controlThread;

    public TrajDrawManager(DemoInterface app, UnfoldingMap[] mapList,
                           PGraphics[][] trajImageMtx, PGraphics[][] trajImageSltMtx,
                           int[] trajCnt, float[] mapXList, float[] mapYList,
                           int width, int height) {
        this.app = app;
        this.mapList = mapList;
        this.trajImageMtx = trajImageMtx;
        this.trajImageSltMtx = trajImageSltMtx;
        this.trajCnt = trajCnt;
        this.mapXList = mapXList;
        this.mapYList = mapYList;
        this.blockList = SharedObject.getInstance().getBlockList();
        this.width = width;
        this.height = height;

        int bgThreadNum = Math.max(PSC.FULL_THREAD_NUM, PSC.SAMPLE_THREAD_NUM);
        int sltThreadNum = PSC.SELECT_THREAD_NUM;       // thread num of double select result
        int totThreadNum = bgThreadNum + sltThreadNum;
        this.trajDrawWorkerMtx = new TrajDrawWorker[4][bgThreadNum];
        this.trajDrawSltWorkerMtx = new TrajDrawWorker[4][sltThreadNum];

        // init pool
        // drop last thread if full
        this.threadPool = new ThreadPoolExecutor(totThreadNum, totThreadNum , 0L,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
                new ThreadPoolExecutor.DiscardOldestPolicy());
        // single thread pool in sure the control orders run one by one
        this.controlPool = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(PSC.CONTROL_POOL_SIZE),
                new ThreadPoolExecutor.AbortPolicy());

        this.refreshList = new boolean[4];
        Arrays.fill(refreshList, true);
    }

    /* Main traj draw part */

    /**
     * Inner class for the task that starts all painting workers.
     * Before run it, the workers for modified map views should have been interrupt
     * by calling {@link #interruptUnfinished}.
     */
    private final class DrawWorkerStarter extends Thread {
        private final int mapIdx;
        private final boolean doubleSelect;

        public DrawWorkerStarter(String name, int mapIdx, boolean doubleSelect) {
            super(name);
            this.mapIdx = mapIdx;       // not used for now
            this.doubleSelect = doubleSelect;
        }

        @Override
        public void run() {
            String layer = doubleSelect ? "slt" : "main";
            // start painting tasks
            UnfoldingMap map = mapList[mapIdx];
            TrajBlock tb = blockList[mapIdx];

            if (tb.getBlockType().equals(BlockType.NONE)) {
                return;       // no need to draw
            }

            Trajectory[] trajList = doubleSelect ?
                    tb.getTrajSltList() : tb.getTrajList();
            int totLen = trajList.length;
            int threadNum = tb.getThreadNum();
            int segLen = totLen / threadNum;
            float offsetX = mapXList[mapIdx];
            float offsetY = mapYList[mapIdx];
            TrajDrawWorker[] trajDrawWorkerList = trajDrawWorkerMtx[mapIdx];

            for (int idx = 0; idx < threadNum; idx++) {
                int begin = segLen * idx;
                int end = Math.min(begin + segLen, totLen);    // exclude
                PGraphics pg = app.createGraphics(width, height);

                String workerName = "worker-" + mapIdx + "-" + idx + "-" + layer;
                TrajDrawWorker worker = new TrajDrawWorker(workerName,
                        map, pg, trajImageMtx[mapIdx], trajList,
                        trajCnt, idx, offsetX, offsetY, begin, end, Color.RED);     // FIXME update color

                trajDrawWorkerList[idx] = worker;
                threadPool.submit(worker);
            }
            System.out.println(getName() + " finished work partition");
        }
    }

    /**
     * Update all traj painting (main or double select).
     * <p>
     * Notice that all the traj will be redrawn, even if they are
     * not changed / not visible / not linked.
     * Before call it, the pg should be cleaned.
     */
    public void startAllNewRenderTask(boolean doubleSelect) {
        for (int mapIdx = 0; mapIdx < 4; mapIdx++) {
            interruptUnfinished(mapIdx, doubleSelect);
            updateTrajImageFor(mapIdx, doubleSelect);
        }
    }

    /**
     * Update the traj painting for specific map view (main or double select).
     * Other map view will not change.
     * Before call it, the pg should be cleaned.
     */
    public void startNewRenderTaskFor(int optViewIdx, boolean doubleSelect) {
        interruptUnfinished(optViewIdx, doubleSelect);
        updateTrajImageFor(optViewIdx, doubleSelect);
    }

    /**
     * Clean the traj buffer image for ALL map view
     */
    public void cleanAllImg(boolean doubleSelect) {
        if (doubleSelect) {
            for (PGraphics[] trajImageList : trajImageSltMtx) {
                Arrays.fill(trajImageList, null);
            }
        } else {
            for (PGraphics[] trajImageList : trajImageMtx) {
                Arrays.fill(trajImageList, null);
            }
        }
    }

    /**
     * Clean the traj buffer image for one map view
     */
    public void cleanImgFor(int optViewIdx, boolean doubleSelect) {
        if (doubleSelect) {
            Arrays.fill(trajImageSltMtx[optViewIdx], null);
        } else {
            Arrays.fill(trajImageMtx[optViewIdx], null);
        }
    }

    /**
     * Clean the unfinished thread of this map
     */
    private void interruptUnfinished(int mapIdx, boolean doubleSelect) {
        TrajDrawWorker[] trajDrawWorkerList = doubleSelect ?
                trajDrawSltWorkerMtx[mapIdx] : trajDrawWorkerMtx[mapIdx];
        for (TrajDrawWorker worker : trajDrawWorkerList) {
            if (worker == null) {
                return;
            }
            worker.interrupt();
        }
        Arrays.fill(trajDrawWorkerList, null);
    }


    /**
     * Start multi-thread (by start a control thread) for a specific task
     * and paint traj to flash images separately.
     */
    private void updateTrajImageFor(int mapIdx, boolean doubleSelect) {
        // create new control thread
        String threadName = "manager-" + mapIdx + "-" + "slt";
        controlThread = new DrawWorkerStarter(threadName, mapIdx, doubleSelect);

        controlThread.setPriority(10);
        controlPool.submit(controlThread);
    }
}
