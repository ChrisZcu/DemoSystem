package draw;

import app.DemoInterface;
import de.fhpotsdam.unfolding.UnfoldingMap;
import app.SharedObject;
import model.TrajBlock;
import processing.core.PApplet;
import processing.core.PGraphics;
import util.PSC;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Manage the draw workers and divide the draw task.
 * Then provide draw results (PGraph) to app interface. <br>
 * It will ask {@link app.SharedObject} for the data.
 *
 * @see TrajDrawWorker
 * @see app.DemoInterface
 */
public class TrajDrawManager {
    private final PApplet app;
    private final UnfoldingMap[] mapList;
    private final PGraphics[][] trajImageMtx;
    private final int[] trajCnt;        // # of traj that already drawn.

    private final TrajBlock[] blockList;
    private final TrajDrawWorker[][] trajDrawWorkerMtx;
    private final boolean[] refreshList;

    private int width = 300, height = 300;      // size for one map view

    // multi-thread for part image painting
    private final ExecutorService threadPool;

    // single thread pool for images controlling
    private final ExecutorService controlPool;
    private Thread controlThread;

    public TrajDrawManager(DemoInterface app, UnfoldingMap[] mapList,
                           PGraphics[][] trajImageMtx, int[] trajCnt) {
        this.app = app;
        this.mapList = mapList;
        this.trajImageMtx = trajImageMtx;
        this.trajCnt = trajCnt;
        this.blockList = SharedObject.getInstance().getBlockList();

        int len = Math.max(PSC.FULL_THREAD_NUM, PSC.SAMPLE_THREAD_NUM);
        this.trajDrawWorkerMtx = new TrajDrawWorker[4][len];

        // init pool
        this.threadPool = new ThreadPoolExecutor(len, len, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>()) {{
            // drop last thread
            this.setRejectedExecutionHandler(new DiscardOldestPolicy());
        }};
        this.controlPool = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>()) {{
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
                TrajDrawWorker[] trajDrawWorkerList = trajDrawWorkerMtx[mapIdx];

                for (int idx = 0; idx < threadNum; idx++) {
                    int begin = segLen * idx;
                    int end = Math.min(begin + segLen, totLen);    // exclude
                    // TODO width height are not assigned now.
                    TrajDrawWorker worker = new TrajDrawWorker(mapList[mapIdx], app.createGraphics(width, height),
                            trajImageMtx[mapIdx], tb.getTrajList(), trajCnt,
                            mapIdx, idx, begin, end);

                    trajDrawWorkerList[idx] = worker;
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
        TrajDrawWorker[] trajDrawWorkerList = trajDrawWorkerMtx[mapIdx];
        int threadNum = blockList[mapIdx].getThreadNum();
        for (int idx = 0; idx < threadNum; idx++) {
            trajDrawWorkerList[idx].interrupt();
        }
    }


    /**
     * Start multi-thread (by start a control thread)
     * and paint traj to flash images separately.
     */
    private void updateTrajImages() {
        if (controlThread != null) {
            controlThread.interrupt();
        }

        // create new control thread
        controlThread = new DrawWorkerStarter();

        controlThread.setPriority(10);
        controlPool.submit(controlThread);
    }
}
