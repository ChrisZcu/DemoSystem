package index;

import app.TimeProfileSharedObject;
import model.Position;
import model.TrajToSubpart;
import model.TrajectoryMeta;
import util.GreedyChooseMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class VfgsForIndexPart {
    public static final int threshold = 2500;
    public static final double alpha = 0.01;

    public static TrajectoryMeta[] trajMetaFull;

    public static TrajToSubpart[] getVfgs(TrajectoryMeta[] trajMetaList) {
        trajMetaFull = TimeProfileSharedObject.getInstance().trajMetaFull;

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
//                        if (lastScore >= totalScore) {
//                            i = trajMetaList.length + 1;
//                        }
                        break;
                    } else {
                        greedyChooseMeta.orderAdjust();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.printf(">>> select %d out of %d by VfgsForIndexPart%n", vfgsTraj.size(), trajMetaList.length);
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
        influSet.addAll(generatePosList(TrajectoryMeta));
    }

    private static void trajScoreInit(TrajectoryMeta[] trajectories, GreedyChooseMeta greedyChooseMeta) {
        for (TrajectoryMeta traj : trajectories) {
            traj.scoreInit();
            greedyChooseMeta.addNewTraj(traj);
        }
    }

    /** @deprecated */
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
            totalScoreSet.addAll(generatePosList(traj));
        }
        return totalScoreSet.size();
    }

    private static void updateTrajScore(TrajectoryMeta TrajectoryMeta, HashSet<Position> influScoreSet) {
        double score = 0;
        for (Position position : generatePosList(TrajectoryMeta)) {
            if (!influScoreSet.contains(position)) {
                score++;
            }
        }
        TrajectoryMeta.updateScore(score);
    }

    private static List<Position> generatePosList(TrajectoryMeta trajMeta) {
        int trajId = trajMeta.getTrajId();
        int begin = trajMeta.getBegin();
        int end = trajMeta.getEnd();      // notice that the end is included

        return Arrays.asList(trajMetaFull[trajId].getPositions()).subList(begin, end + 1);
    }
}
