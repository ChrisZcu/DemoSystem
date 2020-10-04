package util;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import model.Position;
import model.Trajectory;

import java.util.ArrayList;
import java.util.HashSet;

public class VFGSGps {
    private static HashSet<Position> totalTrajPos = new HashSet<>();
    public static UnfoldingMap map;
    public static Trajectory[] trajFull;

    public static void initTrajFull(Trajectory[] trajFulls) {
        trajFull = trajFulls;
        for (Trajectory traj : trajFull) {
            traj.scoreInit();
        }
    }

    public static void totalTrajPosInit(Trajectory[] TrajTotal, float delta) {
        totalTrajPos.clear();
        for (Trajectory traj : TrajTotal) {
            for (Position p : traj.getPositions()) {
                totalTrajPos.add(new Position(p.lat, p.y, delta));
            }
        }
    }

    private static GreedyChoose GreedyChoose;

    private static HashSet<Position> trajSet = new HashSet<>();    // R+

    public static Trajectory[] getCellCover(Trajectory[] trajFull, double rate, int delta, float deltaDis) {//record cal
        GreedyChoose = new GreedyChoose(trajFull.length);

        initTrajFull(trajFull);
        totalTrajPosInit(trajFull, delta);

        ArrayList<Trajectory> cellList = new ArrayList<>();

        int TRAJNUM = trajFull.length;
        trajSet.clear();
        heapInit();
        int trajNum = (int) (rate * TRAJNUM);
        System.out.println(trajFull.length + "-->" + trajNum);

        for (int i = 0; i < trajNum; i++) {
            while (true) {
                Traj2CellScore(GreedyChoose.getHeapHead());
                if (GreedyChoose.GreetOrder()) {
                    Trajectory traj = GreedyChoose.getMaxScoreTraj();   // deleteMax
                    cellList.add(traj);    // take this, add to R
                    CellGridUpdate(traj, delta, deltaDis);        // update R+
                    break;
                } else {
                    GreedyChoose.orderAdjust();
                }
            }
        }

        trajSet.clear();
        return cellList.toArray(new Trajectory[0]);
    }


    private static void heapInit() {
        GreedyChoose.clean();
        for (Trajectory traj : trajFull) {
            GreedyChoose.addNewTraj(traj);
        }
    }

    private static void CellGridUpdate(Trajectory traj, int delta, float deltaDis) {
        for (Position p : traj.getPositions()) {
            for (int i = -delta; i <= delta; i++) {
                for (int j = -delta; j <= delta; j++) {
                    Position pos = new Position(p.lat + i * deltaDis, p.lon + j * deltaDis, deltaDis * 32);
                    if (totalTrajPos.contains(pos))
                        trajSet.add(pos);
                }
            }
        }
    }

    private static void Traj2CellScore(Trajectory traj) {
        traj.setScore(0);
        for (Position p : traj.getPositions()) {
            if (trajSet.contains(new Position(p.lat, p.lon)))  // R+
                continue;
            // not in R+ or self point set
            traj.setScore(traj.getScore() + 1);
        }
    }


    public static double getQuality(Trajectory[] origin, Trajectory[] res, UnfoldingMap map, int delta) {
        HashSet<Position> totalPositionSet = getPositionSet(origin, map);
        HashSet<Position> trajSet = new HashSet<>();

        double totalScore = 0;
        for (Trajectory traj : res) {
            for (Location loc : traj.locations) {
                ScreenPosition pos = map.getScreenPosition(loc);
                for (int i = -delta; i < delta + 1; i++) {
                    for (int j = -delta; j < delta + 1; j++) {
                        Position position = new Position(pos.x + i, pos.y + j);
                        if (!trajSet.contains(position) && totalPositionSet.contains(position)) {
                            trajSet.add(position);
                            totalScore++;
                        }
                    }
                }
            }
        }
        if (totalPositionSet.size() == 0)
            return 0;
        return totalScore / totalPositionSet.size();
    }

    private static HashSet<Position> getPositionSet(Trajectory[] trajectories, UnfoldingMap map) {
        HashSet<Position> totalSet = new HashSet<>();
        for (Trajectory traj : trajectories) {
            for (Location loc : traj.locations) {
                ScreenPosition pos = map.getScreenPosition(loc);
                totalSet.add(new Position(pos.x, pos.y));
            }
        }
        return totalSet;
    }


}
