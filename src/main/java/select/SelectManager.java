package select;


import model.BlockType;
import model.RegionType;
import app.SharedObject;
import org.apache.commons.lang3.ArrayUtils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * select thread pool manager.
 */
public class SelectManager {
    private ExecutorService threadPool;
    private int threadNum;
    private BlockType blockType; // Full, VFGS
    private RegionType regionType; // ODW

    public SelectManager(ExecutorService threadPool, int threadNum, BlockType blockType, RegionType regionType) {
        this.threadPool = threadPool;
        this.threadNum = threadNum;
        this.blockType = blockType;
        this.regionType = regionType;
    }

    public int[] start() {
        threadPool.shutdownNow();

        long start_time = System.currentTimeMillis();

        int totalLength = SharedObject.getInstance().getTrajArray()[blockType.getValue()].length;
        int threadSize = totalLength / threadNum;
        int[] resShowIndex = {};
        try {
            for (int i = 0; i < threadNum - 1; i++) {
                SelectWorker sw = new SelectWorker(regionType, blockType, i * threadSize, (i + 1) * threadSize);
                int[] trajIndexAry = (int[]) threadPool.submit(sw).get();
                resShowIndex = (int[]) ArrayUtils.addAll(resShowIndex, trajIndexAry);
            }
            SelectWorker sw = new SelectWorker(regionType, blockType, (threadNum - 1) * threadSize, totalLength);
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
        System.out.println("time: " + (System.currentTimeMillis() - start_time));
        System.out.println("ALL Done");
        return resShowIndex;
    }
}
