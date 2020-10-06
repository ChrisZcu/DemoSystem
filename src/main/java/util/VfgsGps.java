package util;

import app.TimeProfileSharedObject;
import model.*;

import java.util.Arrays;
import java.util.HashSet;

public class VfgsGps {

    public static TrajectoryMeta[] getVfgs(TrajectoryMeta[] trajFull, double rate, int delta, double minLat, double minLon, double latP, double lonP, StringBuilder sb) {
        System.out.println(minLat + ", " + minLon + ", " + latP + ", " + lonP);
        int trajNum = (int) (trajFull.length * rate);
        TrajectoryMeta[] trajectories = new TrajectoryMeta[trajNum];
        try {
            long t0 = System.currentTimeMillis();
            HashSet<GpsPosition> totalScoreSet = getTotalScore(trajFull, minLat, minLon, latP, lonP);
            sb.append((System.currentTimeMillis() - t0)).append(",");
//            System.out.println("total score time: " + (System.currentTimeMillis() - t0) + " ms");
            System.out.println("total score: " + totalScoreSet.size());
            long t1 = System.currentTimeMillis();
            GreedyChooseMeta greedyChooseMeta = new GreedyChooseMeta(trajFull.length);
            trajScoreInit(trajFull, greedyChooseMeta);
            HashSet<GpsPosition> influScoreSet = new HashSet<>();
            for (int i = 0; i < trajNum; i++) {
                while (true) {
                    updateTrajScore(greedyChooseMeta.getHeapHead(), influScoreSet);
                    if (greedyChooseMeta.GreetOrder()) {
                        TrajectoryMeta traj = greedyChooseMeta.getMaxScoreTraj();
                        updateInfluScoreSet(traj, influScoreSet, totalScoreSet, delta);
                        trajectories[i] = traj;
                        break;
                    } else {
                        greedyChooseMeta.orderAdjust();
                    }
                }
            }
            sb.append((System.currentTimeMillis() - t1)).append(",");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(trajFull.length + "-->" + trajNum);
        return trajectories;
    }

    private static void updateInfluScoreSet(TrajectoryMeta trajectoryMeta, HashSet<GpsPosition> influSet, HashSet<GpsPosition> totalSet, int delta) {
        for (GpsPosition gpsPosition : trajectoryMeta.getGpsPositions()) {
             for (int i=-delta; i <delta+1; i++){
                 for (int j=-delta;j<delta+1;j++){
                     GpsPosition gpsPosition1 = new GpsPosition(gpsPosition.x + i, gpsPosition.y +j);
                     if (totalSet.contains(gpsPosition1)){
                         influSet.add(gpsPosition);
                     }
                 }
             }
        }
    }

    private static void trajScoreInit(TrajectoryMeta[] trajectories, GreedyChooseMeta greedyChooseMeta) {
        for (TrajectoryMeta traj : trajectories) {
            traj.scoreInit();
            greedyChooseMeta.addNewTraj(traj);
        }
    }

    private HashSet<Position> getTotalScoreSet(TrajectoryMeta[] trajFull) {
        HashSet<Position> totalScoreSet = new HashSet<>(trajFull.length);
        for (TrajectoryMeta traj : trajFull) {
            totalScoreSet.addAll(Arrays.asList(traj.getPositions()));
        }
        return totalScoreSet;
    }

    private static HashSet<GpsPosition> getTotalScore(TrajectoryMeta[] trajFull, double minLat, double minLon, double latP, double lonP) {
        HashSet<GpsPosition> totalScoreSet = new HashSet<>(trajFull.length);
        for (TrajectoryMeta traj : trajFull) {
            for (GpsPosition gpsPosition : traj.getGpsPositions()) {
                gpsPosition.x = (int) ((gpsPosition.lat - minLat) / latP);
                gpsPosition.y = (int) ((gpsPosition.lon - minLon) / lonP);
            }
            totalScoreSet.addAll(Arrays.asList(traj.getGpsPositions()));
        }
//        System.out.println(cnt + ", " + totalScoreSet.size());
        return totalScoreSet;
    }

    private static void updateTrajScore(TrajectoryMeta TrajectoryMeta, HashSet<GpsPosition> influScoreSet) {
        double score = 0;
        for (GpsPosition gpsPosition : TrajectoryMeta.getGpsPositions()) {
            if (!influScoreSet.contains(gpsPosition)) {
                score++;
            }
        }
        TrajectoryMeta.updateScore(score);
    }
}
