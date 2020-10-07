package index;

import model.Position;
import model.TrajToSubpart;
import model.TrajectoryMeta;
import util.GreedyChooseMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class VfgsForIndexPart {
    public static final int threshold = 2500;
    public static final double alpha = 0.01;

    public static TrajToSubpart[] getVfgs(TrajectoryMeta[] trajMetaList) {
        ArrayList<TrajToSubpart> vfgsTraj = new ArrayList<>();
        try {

            double totalScore = getTotalScore(trajMetaList);
            double lastScore = 0.0;

            int limit = getRealSize(trajMetaList.length);

            GreedyChooseMeta greedyChooseMeta = new GreedyChooseMeta(trajMetaList.length);
            trajScoreInit(trajMetaList, greedyChooseMeta);
            HashSet<Position> influScoreSet = new HashSet<>();
            for (int i = 0; i < limit; i++) {
                while (true) {
                    updateTrajScore(greedyChooseMeta.getHeapHead(), influScoreSet);
                    if (greedyChooseMeta.GreetOrder()) {
                        TrajectoryMeta trajMeta = greedyChooseMeta.getMaxScoreTraj();
                        updateInfluScoreSet(trajMeta, influScoreSet);
                        vfgsTraj.add(new TrajToSubpart(trajMeta.getTrajId(), trajMeta.getBegin(), trajMeta.getEnd()));
                        lastScore += trajMeta.getScore();
                        if (lastScore >= totalScore) {
                            i = trajMetaList.length + 1;
                        }
                        break;
                    } else {
                        greedyChooseMeta.orderAdjust();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.printf(">>> select %d out of %d by VfgsForIndexPart%n", trajMetaList.length, vfgsTraj.size());
        return vfgsTraj.toArray(new TrajToSubpart[0]);
    }

    /**
     * TODO
     * @return the real size of the quad tree node, according to full size
     */
    private static int getRealSize(int fullSize) {
        if (fullSize <= threshold) {
            return fullSize;
        }
        return (int) (fullSize * alpha);
    }

    private static void updateInfluScoreSet(TrajectoryMeta TrajectoryMeta, HashSet<Position> influSet) {
        influSet.addAll(Arrays.asList(TrajectoryMeta.getPositions()));
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

    private static int getTotalScore(TrajectoryMeta[] trajFull) {

        HashSet<Position> totalScoreSet = new HashSet<>(trajFull.length);
        for (TrajectoryMeta traj : trajFull) {
            totalScoreSet.addAll(Arrays.asList(traj.getPositions()));
        }
        return totalScoreSet.size();
    }

    private static void updateTrajScore(TrajectoryMeta TrajectoryMeta, HashSet<Position> influScoreSet) {
        double score = 0;
        for (Position position : TrajectoryMeta.getPositions()) {
            if (!influScoreSet.contains(position)) {
                score++;
            }
        }
        TrajectoryMeta.updateScore(score);
    }
}
