package revision.baseline;

import index.QuadTree;
import model.TrajectoryMeta;
import org.lwjgl.system.CallbackI;
import util.DistanceFunc;
import util.PSC;

import java.util.HashMap;

public class FischerDistance {
    private static String filePath = PSC.partFilePath;
    private static String outPutPath = "data/baseline/FischerDistance/tmp";
    private static int beginIdx = 0;
    private static int endIdx = 0;

    private static int processNum = 2;

    public static void run() {
        TrajectoryMeta[] trajFull = QuadTree.loadData(new double[4], filePath);
        if (endIdx == 0)
            endIdx = trajFull.length;
        int bucketSize = (endIdx - beginIdx) / processNum;
        for (int i = 0; i < processNum - 1; ++i) {
            int beginTmp = beginIdx + i * bucketSize;
            int endTmp = beginTmp + bucketSize;
            new Thread(() -> {
                HashMap<Integer, Double> idxToDis = new HashMap<>(bucketSize);
                int counter = 0;
                for (int trajId = beginTmp; trajId < endTmp; ++trajId) {
                    TrajectoryMeta traj1 = trajFull[trajId];
                    double dis = 0;
                    for (int tarTrajId = 0; tarTrajId < trajFull.length; ++tarTrajId) {
                        if (tarTrajId == trajId)
                            continue;
                        dis += DistanceFunc.DiscreteFrechetDistance(traj1, trajFull[tarTrajId]);
                    }
                    idxToDis.put(trajId, dis);
                    if (counter % 100 == 0) {
                        System.out.printf("Thread begin from %,d to %,d for trajectory %,d (total %,d now).\n", beginTmp, endTmp, trajId, counter);
                    }
                    util.Util.storeIdxWithScore(outPutPath + "_" + beginTmp + "-" + endTmp, idxToDis);
                    counter += 1;
                }
                util.Util.storeIdxWithScore(outPutPath + "_" + beginTmp + "-" + endTmp, idxToDis);
            }).start();
        }

        int beginTmp = beginIdx + (processNum - 1) * bucketSize;
        int endTmp = trajFull.length;
        new Thread(() -> {
            HashMap<Integer, Double> idxToDis = new HashMap<>(bucketSize);
            int counter = 0;
            for (int trajId = beginTmp; trajId < endTmp; ++trajId) {
                TrajectoryMeta traj1 = trajFull[trajId];
                double dis = 0;
                for (int tarTrajId = 0; tarTrajId < trajFull.length; ++tarTrajId) {
                    if (tarTrajId == trajId)
                        continue;
                    dis += DistanceFunc.DiscreteFrechetDistance(traj1, trajFull[tarTrajId]);
                }
                idxToDis.put(trajId, dis);
                if (counter % 100 == 0) {
                    System.out.printf("Thread begin from %,d to %,d for trajectory %,d. (total%,d now)\n", beginTmp, endTmp, trajId, counter);
                }
                util.Util.storeIdxWithScore(outPutPath + "_" + beginTmp + "-" + endTmp, idxToDis);
                counter += 1;
            }
            util.Util.storeIdxWithScore(outPutPath + "_" + beginTmp + "-" + endTmp, idxToDis);
        }).start();
    }

    public static void main(String[] args) {
        if (args.length > 2) {
            filePath = args[0];
            outPutPath = args[1];
            beginIdx = Integer.parseInt(args[2]);
            endIdx = Integer.parseInt(args[3]);
            processNum = Integer.parseInt(args[4]);
            System.out.println(">>>>>>>>>>init done");
            System.out.println(">>>>>>>>>>init info: ");
            System.out.println(">>>>>>>>>>input file: " + filePath);
            System.out.println(">>>>>>>>>>output file: " + outPutPath);
            System.out.println(">>>>>>>>>>Begin index: " + beginIdx);
            System.out.println(">>>>>>>>>>End index: " + endIdx);
            System.out.println(">>>>>>>>>>Parallelism: " + processNum);
        }
        run();
    }


}
