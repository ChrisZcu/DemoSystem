package select;


import app.SharedObject;
import de.fhpotsdam.unfolding.UnfoldingMap;
import draw.TrajDrawManager;
import model.BlockType;
import model.RegionType;
import model.TrajBlock;
import model.Trajectory;
import org.apache.commons.lang3.ArrayUtils;
import util.PSC;

import java.util.concurrent.*;

/**
 * select thread pool manager, return the traj index array.
 */
public class SelectManager {
    private UnfoldingMap[] mapList;
    private TrajBlock[] blockList;

    public SelectManager(UnfoldingMap[] mapList, TrajBlock[] blockList) {
        this.mapList = mapList;
        this.blockList = blockList;

    }


    private Trajectory[] startMapCal(TrajBlock trajBlock, int opIndex) {
        if (trajBlock.getBlockType() == BlockType.NONE) {
            return new Trajectory[0];
        }
        int threadNum = trajBlock.getThreadNum();
        // TODO Recreate thread pool? Create once may be a better choice.
        ExecutorService threadPool = new ThreadPoolExecutor(threadNum, threadNum, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());

        long startTime = System.currentTimeMillis();

        Trajectory[] trajList = trajBlock.getTrajList();
        int totLen = trajList.length;
        int segLen = totLen / threadNum;
        Trajectory[] resShowIndex = {};

        if (segLen < PSC.MULTI_THREAD_BOUND) {
            // use single thread instead of multi thread
            threadNum = 1;
            segLen = totLen;
        }
        System.out.println("threadNum: " + threadNum);

        for (int i = 0; i < threadNum; i++) {
            SelectWorker sw = new SelectWorker(trajBlock.getTrajList(), i * segLen, (i + 1) * segLen, opIndex, i);
            threadPool.submit(sw);
        }
        threadPool.shutdown();
        try {
            threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            System.err.println(e);
        }
        System.out.println("ALL Done");
        for (int i = 0; i < threadNum; i++) {
            resShowIndex = ArrayUtils.addAll(resShowIndex, SharedObject.getInstance().getTrajSelectRes()[i]);
        }
        System.out.println(trajBlock.getBlockType() + " time: " + (System.currentTimeMillis() - startTime)
                + " select size: " + resShowIndex.length);
        return resShowIndex;
    }

    public void startRun() {
        for (int i = 0; i < 4; i++) {
            TrajBlock trajBlock = blockList[i];
            SharedObject.getInstance().setTrajSelectRes(new Trajectory[trajBlock.getThreadNum()][]);
            Trajectory[] trajAry = startMapCal(trajBlock, i);
            trajBlock.setTrajSltList(trajAry);
        }

        TrajDrawManager tdm = SharedObject.getInstance().getTrajDrawManager();
        if (SharedObject.getInstance().isIntoMaxMap()) {
            checkAndRedraw(tdm, 4);
        } else {
            for (int i = 0; i < 4; i++) {
                checkAndRedraw(tdm, i);
            }
        }
    }

    private void checkAndRedraw(TrajDrawManager tdm, int mapIdx) {
        TrajBlock trajBlock = blockList[mapIdx];
        if (trajBlock.getMainColor() != PSC.GRAY) {
            // need to repaint
            trajBlock.setMainColor(PSC.GRAY);
            tdm.cleanImgFor(mapIdx, TrajDrawManager.MAIN);
            tdm.startNewRenderTaskFor(mapIdx, TrajDrawManager.MAIN);
        }

        tdm.cleanImgFor(mapIdx, TrajDrawManager.SLT);
        tdm.startNewRenderTaskFor(mapIdx, TrajDrawManager.SLT);
    }
}
