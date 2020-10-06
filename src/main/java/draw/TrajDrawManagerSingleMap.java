package draw;

import app.TimeProfileSharedObject;
import de.fhpotsdam.unfolding.UnfoldingMap;
import model.Trajectory;
import processing.core.PApplet;
import processing.core.PGraphics;

import java.util.Arrays;
import java.util.concurrent.*;

public class TrajDrawManagerSingleMap {

    private Trajectory[] trajTotal;
    private int threadNum;
    private PApplet app;
    private UnfoldingMap map;

    public TrajDrawManagerSingleMap(Trajectory[] trajTotal, int threadNum, PApplet app, UnfoldingMap map) {
        this.trajTotal = trajTotal;
        this.threadNum = threadNum;
        this.app = app;
        this.map = map;

        TimeProfileSharedObject.getInstance().trajImageMtx = new PGraphics[threadNum];
    }

    TrajDrawWorkerSingleMap[] workerList;

    public void startDraw() {
        workerList = startDrawWorker();
    }

    private TrajDrawWorkerSingleMap[] startDrawWorker() {
        ExecutorService drawPool = new ThreadPoolExecutor(threadNum, threadNum,
                0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

        int segLen = trajTotal.length / threadNum;

        TrajDrawWorkerSingleMap[] workList = new TrajDrawWorkerSingleMap[threadNum];
        for (int i = 0; i < threadNum; i++) {
            workList[i] = new TrajDrawWorkerSingleMap(app.createGraphics(2544, 1425), map,
                    i * segLen, (i + 1) * segLen, trajTotal);
            drawPool.submit(workList[i]);
        }
        return workList;
    }

    public void interrupt() {
        TimeProfileSharedObject.getInstance().trajImageMtx = new PGraphics[0];
        for (TrajDrawWorkerSingleMap worker : workerList) {
            if (worker == null) {
                return;
            }
            worker.stop = true;
        }
        Arrays.fill(workerList, null);
    }
}
