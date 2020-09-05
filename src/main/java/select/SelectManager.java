package select;


import de.fhpotsdam.unfolding.UnfoldingMap;
import draw.TrajDrawManager;
import model.BlockType;
import model.RegionType;
import app.SharedObject;
import model.TrajBlock;
import model.Trajectory;
import org.apache.commons.lang3.ArrayUtils;

import java.util.concurrent.*;

/**
 * select thread pool manager, return the traj index array.
 */
public class SelectManager {
    private RegionType regionType; // ODW
    private UnfoldingMap[] mapList;
    private TrajBlock[] blockList;

    public SelectManager(RegionType regionType, UnfoldingMap[] mapList, TrajBlock[] blockList) {
        this.regionType = regionType;
        this.mapList = mapList;
        this.blockList = blockList;

        System.out.println(regionType);
    }


    private Trajectory[] startMapCal(TrajBlock trajBlock, int opIndex) {
        if (trajBlock.getBlockType() == BlockType.NONE) {
            return new Trajectory[0];
        }

        int threadNum = trajBlock.getThreadNum();

        ExecutorService threadPool = new ThreadPoolExecutor(threadNum, threadNum, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());

        UnfoldingMap map = mapList[opIndex];
        long start_time = System.currentTimeMillis();

        int totalLength = trajBlock.getTrajList().length;
        int threadSize = totalLength / trajBlock.getThreadNum();
        Trajectory[] resShowIndex = {};
        try {
            for (int i = 0; i < threadNum - 1; i++) {
                SelectWorker sw = new SelectWorker(regionType, trajBlock.getTrajList(), i * threadSize, (i + 1) * threadSize, opIndex);
                Trajectory[] trajIndexAry = (Trajectory[]) threadPool.submit(sw).get();
                resShowIndex = ArrayUtils.addAll(resShowIndex, trajIndexAry);
            }
            SelectWorker sw = new SelectWorker(regionType, trajBlock.getTrajList(), (threadNum - 1) * threadSize, totalLength, opIndex);
            Trajectory[] trajIndexAry = (Trajectory[]) threadPool.submit(sw).get();
            resShowIndex = ArrayUtils.addAll(resShowIndex, trajIndexAry);

            threadPool.shutdown();
            try {
                threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                System.err.println(e);
            }

        } catch (ExecutionException | InterruptedException e) {
            System.err.println(e);
        }
        System.out.println(trajBlock.getBlockType() + " time: " + (System.currentTimeMillis() - start_time));
        System.out.println("ALL Done");
        return resShowIndex;
    }

    public void startRun() {
        for (int i = 0; i < blockList.length; i++) {
            Trajectory[] trajAry = startMapCal(blockList[i], i);
            TrajBlock trajBlock = blockList[i];
            trajBlock.setTrajSltList(trajAry);

            TrajDrawManager tdm = SharedObject.getInstance().getTrajDrawManager();
            tdm.cleanImgFor(i, TrajDrawManager.SLT);
            tdm.startNewRenderTaskFor(i, TrajDrawManager.SLT);

        }
    }
}
