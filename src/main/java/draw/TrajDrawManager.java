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
    public static final int MAIN = 0;       // main background layer
    public static final int SLT = 1;        // double select result layer

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

    private final float[] mapXList, mapYList;
    private final int width, height;        // size for one map view

    // multi-thread for part image painting
    private final ExecutorService threadPool;
    // single thread pool for images controlling
    private final ExecutorService controlPool;

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
        this.threadPool = new ThreadPoolExecutor(totThreadNum, totThreadNum, 0L,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
                new ThreadPoolExecutor.DiscardOldestPolicy());
        // single thread pool in sure the control orders run one by one
        this.controlPool = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(PSC.CONTROL_POOL_SIZE),
                new ThreadPoolExecutor.DiscardOldestPolicy());
    }

    /**
     * Inner class for the task that starts all painting workers.
     * Before run it, the workers for modified map views should have been interrupt
     * by calling {@link #interruptUnfinished}.
     */
    private final class DrawWorkerStarter extends Thread {
        private final int mapIdx;
        private final int layerType;

        public DrawWorkerStarter(String name, int mapIdx, int layerType) {
            super(name);
            this.mapIdx = mapIdx;
            this.layerType = layerType;
        }

        @Override
        public void run() {
            long time = System.currentTimeMillis();

            // start painting tasks
            UnfoldingMap map = mapList[mapIdx];
            TrajBlock tb = blockList[mapIdx];

            if (tb.getBlockType().equals(BlockType.NONE)) {
                return;       // no need to draw
            }

            String layer;
            PGraphics[] trajImageList;
            Color color;

            if (layerType == MAIN) {
                layer = "main";
                trajImageList = trajImageMtx[mapIdx];
                color = tb.getMainColor();
            } else {
                layer = "slt";
                trajImageList = trajImageSltMtx[mapIdx];
                color = tb.getSltColor();
            }

            Trajectory[] trajList = layerType == 0 ?
                    tb.getTrajList() : tb.getTrajSltList();
            int totLen = trajList.length;
            System.out.println(">>>> " + getName() + " trajList len = " + totLen);
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
                        map, pg, trajImageList, trajList, trajCnt,
                        idx, offsetX, offsetY, begin, end, color);

                trajDrawWorkerList[idx] = worker;
                threadPool.submit(worker);
            }
            System.out.println(getName() + " finished work partition in "
                    + (System.currentTimeMillis() - time));
        }
    }

    /**
     * Update all traj painting (main or double select).
     * <p>
     * Notice that all the traj will be redrawn, even if they are
     * not changed / not visible / not linked.
     * Before call it, the pg should be cleaned.
     *
     * @param layerType {@link #MAIN} or {@link #SLT}
     */
    public void startAllNewRenderTask(int layerType) {
        for (int mapIdx = 0; mapIdx < 4; mapIdx++) {
            interruptUnfinished(mapIdx, layerType);
            updateTrajImageFor(mapIdx, layerType);
        }
    }

    /**
     * Update the traj painting for specific map view (main or double select).
     * Other map view will not change.
     * <p>
     * Before call it, the pg should be cleaned.
     *
     * @param layerType {@link #MAIN} or {@link #SLT}
     */
    public void startNewRenderTaskFor(int optViewIdx, int layerType) {
        interruptUnfinished(optViewIdx, layerType);
        updateTrajImageFor(optViewIdx, layerType);
    }

    /**
     * Update the traj painting for specific map view (main or double select),
     * both layer.
     * Other map view will not change.
     * <p>
     * Before call it, the pg should be cleaned.
     */
    public void startNewRenderTaskFor(int optViewIdx) {
        interruptUnfinished(optViewIdx, MAIN);
        interruptUnfinished(optViewIdx, SLT);
        updateTrajImageFor(optViewIdx, MAIN);
        updateTrajImageFor(optViewIdx, SLT);
    }

    /**
     * Clean the traj buffer image for ALL map view
     *
     * @param layerType {@link #MAIN} or {@link #SLT}
     */
    public void cleanAllImg(int layerType) {
        if (layerType == MAIN) {
            for (PGraphics[] trajImageList : trajImageMtx) {
                Arrays.fill(trajImageList, null);
            }
        } else {
            for (PGraphics[] trajImageList : trajImageSltMtx) {
                Arrays.fill(trajImageList, null);
            }
        }
    }

    /**
     * Clean the traj buffer image for one map view
     *
     * @param layerType {@link #MAIN} or {@link #SLT}
     */
    public void cleanImgFor(int optViewIdx, int layerType) {
        if (layerType == MAIN) {
            Arrays.fill(trajImageMtx[optViewIdx], null);
        } else {
            Arrays.fill(trajImageSltMtx[optViewIdx], null);
        }
    }

    /**
     * Clean the traj buffer images for one map view, both layers
     */
    public void cleanImgFor(int optViewIdx) {
        Arrays.fill(trajImageMtx[optViewIdx], null);
        Arrays.fill(trajImageSltMtx[optViewIdx], null);
    }

    /**
     * Clean the unfinished thread of this map
     *
     * @param layerType {@link #MAIN} or {@link #SLT}
     */
    private void interruptUnfinished(int mapIdx, int layerType) {
        TrajDrawWorker[] trajDrawWorkerList = (layerType == MAIN) ?
                trajDrawWorkerMtx[mapIdx] : trajDrawSltWorkerMtx[mapIdx];
        try {
            for (TrajDrawWorker worker : trajDrawWorkerList) {
                if (worker == null) {
                    return;
                }
                worker.stop = true;
                worker.join();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Arrays.fill(trajDrawWorkerList, null);
    }


    /**
     * Start multi-thread (by start a control thread) for a specific task
     * and paint traj to flash images separately.
     *
     * @param layerType {@link #MAIN} or {@link #SLT}
     */
    private void updateTrajImageFor(int mapIdx, int layerType) {
        // create new control thread
        String threadName = "manager-" + mapIdx + "-"
                + (layerType == MAIN ? "main" : "slt");
        Thread controlThread = new DrawWorkerStarter(threadName, mapIdx, layerType);
        controlThread.setPriority(10);
        controlPool.submit(controlThread);
    }
}
