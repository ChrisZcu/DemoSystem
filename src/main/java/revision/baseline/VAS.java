package revision.baseline;


import com.sun.javafx.image.IntPixelGetter;
import index.QuadTree;
import model.Position;
import model.Trajectory;
import model.TrajectoryMeta;
import util.DistanceFunc;
import util.PSC;

import java.sql.Time;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

public class VAS {
    private double error;
    private TrajectoryMeta[] trajFull;
    private static String filePath = PSC.portoPath;
    private static String VASResFile = "data/baseline/porto/PortoVASRes0.01.text";
    private static double sampleRatio = 0.01;
    private static String initExpandFile = String.format("data/baseline/porto/PortoVASInit%s.txt", "0.01");

    public int[] sampleResIds;
    public double[] sampleRespScore;
    private int removedTrajId;

    private HashSet<Integer> initSet;

    private double kappa(TrajectoryMeta traj1, TrajectoryMeta traj2) {
        double dis = DistanceFunc.DiscreteFrechetDistance(traj1, traj2);

        return Math.exp(-dis * dis / error);
    }

    private double pointEuclidean(double x1, double y1, double x2, double y2) {
        return Math.hypot(x1 - x2, y1 - y2);
    }

    private void initFromFile(String filePath) {
        util.Util.loadInitVAS(filePath, sampleResIds, sampleRespScore);
//        System.out.println(Arrays.toString(sampleResIds));
//        System.out.println(Arrays.toString(sampleRespScore));
    }

    private void initExpand() {
        int n = sampleResIds.length;
        for (int i = 0; i < n; ++i) {
            System.out.println(i + ", " + n);
            TrajectoryMeta traj = trajFull[sampleResIds[i]];
            double rsp = expand(traj, i);
            sampleRespScore[i] = rsp;
        }
    }

    private double expand(TrajectoryMeta traj, int n) {
        double rep = 0;
        double largestScore = -1;
        for (int i = 0; i < n; ++i) {
            int trajId = sampleResIds[i];
            double l = kappa(traj, trajFull[trajId]);
            sampleRespScore[i] += l;
            if (largestScore < sampleRespScore[i]) {
                largestScore = sampleRespScore[i];
                removedTrajId = i;
            }
            rep += l;
        }
        return rep;
    }

    private double[] expand(TrajectoryMeta traj, int n, int[] sampleResIds, double[] sampleRespScore) {
        double rep = 0;
        double largestScore = -1;
        int removedTrajId = -1;
        for (int i = 0; i < n; ++i) {
            int trajId = sampleResIds[i];
            double l = kappa(traj, trajFull[trajId]);
            sampleRespScore[i] += l;
            if (largestScore < sampleRespScore[i]) {
                largestScore = sampleRespScore[i];
                removedTrajId = i;
            }
            rep += l;
        }
        return new double[]{rep, removedTrajId * 1.0};
    }

    private void shrink(int trajId, double rsp) {
        TrajectoryMeta removedTraj = trajFull[sampleResIds[removedTrajId]];
        sampleResIds[removedTrajId] = trajId;
        sampleRespScore[removedTrajId] = rsp;
        for (int i = 0; i < sampleResIds.length; ++i) {
            int tmpTrajId = sampleResIds[i];
            double l = kappa(removedTraj, trajFull[tmpTrajId]);
            sampleRespScore[i] -= l;
        }
    }

    private void shrink(int trajId, double rsp, int[] sampleResIds, double[] sampleRespScore, int removedTrajId) {
        TrajectoryMeta removedTraj = trajFull[sampleResIds[removedTrajId]];
        sampleResIds[removedTrajId] = trajId;
        sampleRespScore[removedTrajId] = rsp;
        for (int i = 0; i < sampleResIds.length; ++i) {
            int tmpTrajId = sampleResIds[i];
            double l = kappa(removedTraj, trajFull[tmpTrajId]);
            sampleRespScore[i] -= l;
        }
    }

    private double pointEuclidean(Position p1, Position p2) {
//        System.out.println(p1.x + ", " + p1.y + ", " + p2.x + ", " + p2.y);
        double x1 = p1.x / 10000.0;
        double y1 = p1.y / 10000.0;
        double x2 = p2.x / 10000.0;
        double y2 = p2.y / 10000.0;

        return Math.hypot(x1 - x2, y1 - y2);
    }

    public void init() {
        double[] minMax = new double[4];
        trajFull = QuadTree.loadData(minMax, filePath);
//        int tp =0;
//        for (TrajectoryMeta traj :trajFull)
//            tp+=traj.getPositions().length;
//        System.out.println(tp);
//        System.out.println();
//        for (int j =0; j < 10; ++j) {
//            double dis= 0;
//            long startTime = System.currentTimeMillis();
//            for (int i = 0; i < trajFull.length; ++i) {
//                dis += DistanceFunc.DiscreteFrechetDistance(trajFull[j], trajFull[i]);
//            }
//            long endTime = System.currentTimeMillis();
//            System.out.println(trajFull[j].getPositions().length);
//            System.out.println("time usage " + j +  ", " + (endTime - startTime) / 1000);
//        }
        error = Math.max(pointEuclidean(minMax[0], minMax[2], minMax[1], minMax[3]),
                Math.max(pointEuclidean(minMax[0], minMax[2], minMax[0], minMax[3]), pointEuclidean(minMax[0], minMax[2], minMax[1], minMax[2])));
        System.out.println(error);
        System.out.println(error / 100);
        System.out.println(error / 100 * 4);
        error = Math.pow(error, 2) / 100;
        System.out.println(error);
        int sampleSize = (int) (trajFull.length * sampleRatio);
        sampleResIds = new int[sampleSize];
        sampleRespScore = new double[sampleSize];

        Random ran = new Random(1);
        initSet = new HashSet<Integer>(sampleSize);
        while (initSet.size() != sampleSize) {
            initSet.add(ran.nextInt(trajFull.length - 1));
        }
//        int j = 0;
//        for (Integer item : initSet) {
//            sampleResIds[j++] = item;
//        }
//        initExpand();
//        concurInit();
        initFromFile(initExpandFile);
//        for (int idx : sampleResIds) {
//            if (!initSet.contains(idx)) {
//                System.out.println("Fuckkkkkkkkkkkkkkkkkkkkk");
//            }
//        }
//        util.Util.storeInitVAS(initExpandFile, sampleResIds, sampleRespScore);
//        System.out.println(Arrays.toString(sampleRespScore));
//        System.out.println(error);
        System.out.printf("%-10s\n", ">>>>>>init done");
    }

    private static int beginIdx, endIdx;
    private static int loopNum = 10;

    public void resample() {
//        (int) (trajFull.length * sampleRatio)
        double[] ratio = {0.005, 0.001, 0.0005, 0.0001};
        int curSize = (int) (trajFull.length * 0.01);
        int n = sampleResIds.length;
        for (double r : ratio) {
            int targetSize = (int) (trajFull.length * r);
            int diff = curSize - targetSize;
            while (diff > 0) {
                System.out.println(diff);
                double maxScore = -1;
                int trajId = -1, listId = -1;
                for (int i = 0; i < n; ++i) {
                    if (sampleRespScore[i] > maxScore) {
                        maxScore = sampleRespScore[i];
                        trajId = sampleResIds[i];
                        listId = i;
                    }
                }
                sampleRespScore[listId] = -1;
                TrajectoryMeta traj = trajFull[trajId];
                for (int i = 0; i < sampleResIds.length; ++i) {
                    if (sampleRespScore[i] == -1)
                        continue;
                    int tmpTrajId = sampleResIds[i];
                    double l = kappa(traj, trajFull[tmpTrajId]);
                    sampleRespScore[i] -= l;
                }
                --diff;
            }
            int[] res = new int[targetSize];
            int idx = 0;
            for (int i = 0; i < n; ++i) {
                if (sampleRespScore[i] == -1)
                    continue;
                res[idx++] = sampleResIds[i];
            }
            util.Util.storeInitVAS(VASResFile + "-" + r, res, new double[n]);
            curSize = targetSize;
        }
    }

    public void VASSample() {
        int sampleSize = (int) (trajFull.length * sampleRatio);
        if (endIdx == 0) {
            endIdx = trajFull.length;
        }
        boolean con = false;
        if (con) {
            int conNum = 8, buckets = ((int) ((endIdx - beginIdx) / conNum));
            for (int i1 = 0; i1 < conNum; ++i1) {
                int beginIdxTmp = i1 * buckets + beginIdx;
                int endIdxTmp = i1 == conNum - 1 ? endIdx : (i1 + 1) * buckets + beginIdx;
                new Thread(() -> {
                    int[] sampleIdsTmp = new int[sampleSize];
                    double[] sampleScores = new double[sampleSize];
                    for (int i = 0; i < sampleSize; i++) {
                        sampleIdsTmp[i] = sampleResIds[i];
                        sampleScores[i] = sampleRespScore[i];
                    }
                    int counter = 0;
                    for (int i = beginIdxTmp; i < endIdxTmp; ++i) {
                        if (initSet.contains(i))
                            continue;
                        TrajectoryMeta traj = trajFull[i];
                        double[] tmp = expand(traj, sampleSize, sampleIdsTmp, sampleScores);

                        shrink(i, tmp[0], sampleIdsTmp, sampleScores, (int) tmp[1]);
                        if (counter % 500 == 0) {
                            System.out.println((beginIdxTmp) + ", " + endIdxTmp + ": " + i + ", total " + trajFull.length);
                            util.Util.storeInitVAS(VASResFile + beginIdxTmp + "-" + endIdxTmp, sampleIdsTmp, sampleScores);
                        }
                        counter += 1;
                    }
                    util.Util.storeInitVAS(VASResFile + beginIdxTmp + "-" + endIdxTmp, sampleIdsTmp, sampleScores);
                }).start();
            }
            return;
        }
        int counter = 0;
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < endIdx; ++i) {
            if (initSet.contains(i))
                continue;
            TrajectoryMeta traj = trajFull[i];
            double rsp = expand(traj, sampleSize);
            shrink(i, rsp);
            if (counter % loopNum == 0) {
                System.out.println(i + ", " + (endIdx - i) + ", " + trajFull.length);
                util.Util.storeInitVAS(VASResFile, sampleResIds, sampleRespScore);
            }
            counter += 1;
            long endTime = System.currentTimeMillis();
            System.out.println("time usage: " + counter + ", " + (endTime - startTime) / 1000);
        }
//        util.Util.storeIds(VASResFile, sampleResIds);
        util.Util.storeInitVAS(VASResFile, sampleResIds, sampleRespScore);

    }

    private void concurInit() {
        int corsNum = 12;
        int sampleSize = (int) (trajFull.length * sampleRatio);
        int buckets = sampleSize / corsNum;
        for (int i = 0; i < corsNum - 1; ++i) {
            int beginIdx = i * buckets, endIdx = (i + 1) * buckets;
            new Thread(() -> {
                for (int i1 = beginIdx; i1 < endIdx; ++i1) {
                    TrajectoryMeta traj = trajFull[sampleResIds[i1]];
                    double rep = 0;
                    for (int j = 0; j < sampleSize; ++j) {
                        int trajId = sampleResIds[j];
                        double l = kappa(traj, trajFull[trajId]);
                        sampleRespScore[i1] += l;
                        rep += l;
                    }
                    sampleRespScore[i1] = rep;
                    if (i1 % 50 == 0) {
                        System.out.println(beginIdx + "-" + endIdx + ": " + i1);
                        util.Util.storeInitVAS(initExpandFile + beginIdx, sampleResIds, sampleRespScore);
                    }
                }
                util.Util.storeInitVAS(initExpandFile + beginIdx, sampleResIds, sampleRespScore);
            }).start();
        }

        int beginIdx = (corsNum - 1) * buckets;
        new Thread(() -> {
            for (int i1 = beginIdx; i1 < sampleSize; ++i1) {
                TrajectoryMeta traj = trajFull[sampleResIds[i1]];
                double rep = 0;
                for (int j = 0; j < sampleSize; ++j) {
                    int trajId = sampleResIds[j];
                    double l = kappa(traj, trajFull[trajId]);
                    sampleRespScore[i1] += l;
                    rep += l;
                }
                sampleRespScore[i1] = rep;
                if (i1 % 50 == 0) {
                    System.out.println(beginIdx + "-" + sampleSize + ": " + i1);
                    util.Util.storeInitVAS(initExpandFile + beginIdx, sampleResIds, sampleRespScore);
                }
            }
            util.Util.storeInitVAS(initExpandFile + beginIdx, sampleResIds, sampleRespScore);
        }).start();
    }

    public static void main(String[] args) {

        if (args.length > 3) {
            filePath = args[0];
            initExpandFile = args[1];
            VASResFile = args[2];
            sampleRatio = Double.parseDouble(args[3]);
            beginIdx = Integer.parseInt(args[4]);
            endIdx = Integer.parseInt(args[5]);
            loopNum = Integer.parseInt(args[6]);
            System.out.println("file path" + filePath);
            System.out.println("initExpandFile" + initExpandFile);
            System.out.println("VASResFile" + VASResFile);
            System.out.println("sampleRatio" + sampleRatio);
            System.out.println("beginIdx" + beginIdx);
            System.out.println("endIdx" + endIdx);
            System.out.println("loopNum" + loopNum);

        }
        VAS vas = new VAS();
        long stime = System.currentTimeMillis();
        vas.init();
//        vas.VASSample();
        vas.resample();
        long etime = System.currentTimeMillis();
        System.out.println(etime - stime);
    }
}