package index;

import javafx.geometry.Pos;
import model.Position;
import model.TrajToQuality;
import model.Trajectory;
import util.GreedyChoose;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class VfgsForIndex {

    public static TrajToQuality[] getVfgs(Trajectory[] trajFull) {
        ArrayList<TrajToQuality> vfgsTraj = new ArrayList<>();
        try {

            double totalScore = getTotalScore(trajFull);
            double lastScore = 0.0;

            GreedyChoose greedyChoose = new GreedyChoose(trajFull.length);
            trajScoreInit(trajFull, greedyChoose);
            HashSet<Position> influScoreSet = new HashSet<>();
            for (int i = 0; i < trajFull.length; i++) {
                while (true) {
                    updateTrajScore(greedyChoose.getHeapHead(), influScoreSet);
                    if (greedyChoose.GreetOrder()) {
                        Trajectory traj = greedyChoose.getMaxScoreTraj();
                        updateInfluScoreSet(traj, influScoreSet);
                        vfgsTraj.add(new TrajToQuality(traj, (traj.getScore() + lastScore) / totalScore));
                        lastScore += traj.getScore();
                        if (lastScore >= totalScore) {
                            i = trajFull.length + 1;
                        }
                        break;
                    } else {
                        greedyChoose.orderAdjust();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return vfgsTraj.toArray(new TrajToQuality[0]);
    }

    private static void updateInfluScoreSet(Trajectory trajectory, HashSet<Position> influSet) {
        influSet.addAll(Arrays.asList(trajectory.getPositions()));
    }

    private static void trajScoreInit(Trajectory[] trajectories, GreedyChoose greedyChoose) {
        for (Trajectory traj : trajectories) {
            traj.scoreInit();
            greedyChoose.addNewTraj(traj);
        }
    }

    private HashSet<Position> getTotalScoreSet(Trajectory[] trajFull) {
        HashSet<Position> totalScoreSet = new HashSet<>(trajFull.length);
        for (Trajectory traj : trajFull) {
            totalScoreSet.addAll(Arrays.asList(traj.getPositions()));
        }
        return totalScoreSet;
    }

    private static int getTotalScore(Trajectory[] trajFull) {

        HashSet<Position> totalScoreSet = new HashSet<>(trajFull.length);
        for (Trajectory traj : trajFull) {
            totalScoreSet.addAll(Arrays.asList(traj.getPositions()));
        }
        return totalScoreSet.size();
    }

    private static void updateTrajScore(Trajectory trajectory, HashSet<Position> influScoreSet) {
        double score = 0;
        for (Position position : trajectory.getPositions()) {
            if (!influScoreSet.contains(position)) {
                score++;
            }
        }
        trajectory.updateScore(score);
    }
}
