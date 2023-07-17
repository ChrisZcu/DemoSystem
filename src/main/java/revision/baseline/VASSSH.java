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

public class VASSSH {
    private double error;
    private TrajectoryMeta[] trajFull;
    private String filePath = PSC.partFilePath;
    private String initExpandFile = "data/baseline/VASInit.text";
    private String VASResFile = "data/baseline/VASRes.text";

    private final double sampleRatio = 0.005;
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

    private void initExpand() {
        int n = sampleResIds.length;
        for (int i = 0; i < n; ++i) {
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

    private void shrink(int trajId, double rsp) {
        sampleResIds[removedTrajId] = trajId;
        sampleRespScore[removedTrajId] = rsp;
        for (int i = 0; i < sampleResIds.length; ++i) {
            int tmpTrajId = sampleResIds[i];
            double l = kappa(trajFull[removedTrajId], trajFull[tmpTrajId]);
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

        error = Math.max(pointEuclidean(minMax[0], minMax[2], minMax[1], minMax[3]),
                Math.max(pointEuclidean(minMax[0], minMax[2], minMax[0], minMax[3]), pointEuclidean(minMax[0], minMax[2], minMax[1], minMax[2])));
        error = Math.pow(error, 2) / 100;

        int sampleSize = (int) (trajFull.length * sampleRatio);
        sampleResIds = new int[sampleSize];
        sampleRespScore = new double[sampleSize];

        Random ran = new Random(1);
        initSet = new HashSet<Integer>(sampleSize);
        while (initSet.size() != sampleSize) {
            initSet.add(ran.nextInt(trajFull.length - 1));
        }
        int j = 0;
        for (Integer item : initSet) {
            sampleResIds[j++] = item;
        }
        initExpand();
        util.Util.storeInitVAS(initExpandFile,sampleResIds, sampleRespScore);
//        System.out.println(Arrays.toString(sampleRespScore));
//        System.out.println(error);
        System.out.printf("%-10s\n", ">>>>>>init done");
    }

    public void VASSample() {
        int sampleSize = (int) (trajFull.length * sampleRatio);
        for (int i = 0; i < trajFull.length; ++i) {
            if (initSet.contains(i))
                continue;
            TrajectoryMeta traj = trajFull[i];
            double rsp = expand(traj, sampleSize);
            shrink(i, rsp);
            if (i % 500 == 0)
                System.out.println(i + ", " + sampleSize);
        }
        util.Util.storeIds(VASResFile, sampleResIds);
    }

    public static void main(String[] args) {
        VASSSH vas = new VASSSH();
        long stime = System.currentTimeMillis();
        vas.init();
        vas.VASSample();
        long etime = System.currentTimeMillis();
        System.out.println(etime - stime);

    }
}