package select;


import de.fhpotsdam.unfolding.UnfoldingMap;
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


    private int[] startMapCal(TrajBlock trajBlock, int opIndex) {
        if (trajBlock.getBlockType() == BlockType.NONE)
            return new int[0];

        int threadNum = trajBlock.getThreadNum();

        ExecutorService threadPool = new ThreadPoolExecutor(threadNum, threadNum, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());

        UnfoldingMap map = mapList[opIndex];
        long start_time = System.currentTimeMillis();

        int totalLength = trajBlock.getTrajList().length;
        int threadSize = totalLength / trajBlock.getThreadNum();
        int[] resShowIndex = {};
        try {
            for (int i = 0; i < threadNum - 1; i++) {
                SelectWorker sw = new SelectWorker(regionType, trajBlock.getTrajList(), i * threadSize, (i + 1) * threadSize, opIndex);
                int[] trajIndexAry = (int[]) threadPool.submit(sw).get();
                resShowIndex = (int[]) ArrayUtils.addAll(resShowIndex, trajIndexAry);
            }
            SelectWorker sw = new SelectWorker(regionType, trajBlock.getTrajList(), (threadNum - 1) * threadSize, totalLength, opIndex);
            int[] trajIndexAry = (int[]) threadPool.submit(sw).get();
            resShowIndex = (int[]) ArrayUtils.addAll(resShowIndex, trajIndexAry);

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
            int[] trajIndexAry = startMapCal(blockList[i], i);
            TrajBlock trajBlock = blockList[i];
            Trajectory[] trajTmp = new Trajectory[trajIndexAry.length];
            for (int j = 0; j < trajTmp.length; j++) {
                trajTmp[j] = SharedObject.getInstance().getTrajFull()[trajIndexAry[j]];
            }
            trajBlock.setTrajSltList(trajTmp);
        }
    }
}
