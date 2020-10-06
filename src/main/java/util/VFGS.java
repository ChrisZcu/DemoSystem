package util;

import app.UserInterface;
import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import model.Position;
import model.RectRegion;
import model.Trajectory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

public class VFGS {
    private static HashSet<Position> totalTrajPos = new HashSet<>();
    public static UnfoldingMap map;
    static double[] RATELIST = {0.05, 0.01, 0.005, 0.001, 0.0005, 0.0001, 0.00005, 0.00001};
    public static Trajectory[] trajFull;

    public static void initTrajFull(Trajectory[] trajFulls) {
        trajFull = trajFulls;
        initScore = new double[trajFull.length];
        int i = 0;
        for (Trajectory traj : trajFull) {
            initScore[i++] = traj.getScore();
        }
    }

    private VFGS() {
    }

    public static VFGS instance = new VFGS();

    public static VFGS getInstance() {
        return instance;
    }

    public static void totalTrajPosInit(Trajectory[] TrajTotal) {
        totalTrajPos.clear();
        for (Trajectory traj : TrajTotal) {
            for (Location p : traj.locations) {
//                double px = map.getScreenPosition(p).x;
//                double py = map.getScreenPosition(p).y;
                totalTrajPos.add(new Position(p.getLat(), p.getLon()));
            }
        }
    }

    private static GreedyChoose GreedyChoose;

    private static HashSet<Position> trajSet = new HashSet<>();    // R+

    public static Trajectory[] getCellCover(Trajectory[] trajFull, UnfoldingMap maps, double rate, int delta) {//record cal
//        if (trajFull.length > 2312) {
//            rate = (double) 2312 / trajFull.length;
//        } else {
//            rate = 1;
//        }
        System.out.println(rate);

        long t1 = System.currentTimeMillis();
        Location loc = maps.getCenter();
        int TRAJNUM = trajFull.length;
        int trajNum = (int) (rate * TRAJNUM);

        System.out.println(trajFull.length + "-->" + trajNum);
        GreedyChoose = new GreedyChoose(trajFull.length);
        map = maps;
        map.zoomAndPanTo(20, loc);
        initTrajFull(trajFull);
        totalTrajPosInit(trajFull);
        System.out.println("total score: " + totalTrajPos.size());

        ArrayList<Trajectory> cellList = new ArrayList<>();

        for (int j = 0; j < 1; j++) {
            trajSet.clear();
            scoreInit();
            int n = 1;
            int i = 0;
            while (n > 0) {
                n--;
                for (; i < trajNum; i++) {
                    while (true) {
                        Traj2CellScore(GreedyChoose.getHeapHead());
                        if (GreedyChoose.GreetOrder()) {
                            Trajectory traj = GreedyChoose.getMaxScoreTraj();   // deleteMax
                            for (int num = 0; num <= n; num++) {
                                cellList.add(traj);    // take this, add to R
                            }
                            CellGridUpdate(traj, delta);        // update R+
                            break;
                        } else {
                            GreedyChoose.orderAdjust();
                        }
                    }
                }
            }
        }

        trajSet.clear();
        System.out.println(trajFull.length + "-->" + cellList.size());

        return cellList.toArray(new Trajectory[0]);
    }

    public static Trajectory[] cut(Trajectory[] traj, RectRegion region) {
        ArrayList<Trajectory> res = new ArrayList<>();
        for (Trajectory tra : traj) {
            res.addAll(getRegionInTraj(tra, region));
        }
        return res.toArray(new Trajectory[0]);
    }

    private static ArrayList<Trajectory> getRegionInTraj(Trajectory traj, RectRegion region) {
        ArrayList<Trajectory> res = new ArrayList<>();
        //TODO, move method
        for (int i = 0; i < traj.locations.length; i++) {
            if (inCheck(traj.locations[i], region)) {
                Trajectory trajTmp = new Trajectory(-1);
                Location loc = traj.locations[i++];
                ArrayList<Location> locTmp = new ArrayList<>();
                while (inCheck(loc, region) && i < traj.locations.length) {
                    locTmp.add(loc);
                    loc = traj.locations[i++];
                }
                trajTmp.locations = locTmp.toArray(new Location[0]);
                res.add(trajTmp);
            }
        }
        return res;
    }

    private static boolean inCheck(Location loc, RectRegion region) {
        float leftLat = region.getLeftTopLoc().getLat();
        float leftLon = region.getLeftTopLoc().getLon();
        float rightLon = region.getRightBtmLoc().getLon();
        float rightLat = region.getRightBtmLoc().getLat();

        return loc.getLat() >= Math.min(leftLat, rightLat) && loc.getLat() <= Math.max(leftLat, rightLat)
                && loc.getLon() >= Math.min(leftLon, rightLon) && loc.getLon() <= Math.max(leftLon, rightLon);
    }

    public static Trajectory[] getCellCover(Trajectory[] trajFull, double rate) {
        long t1 = System.currentTimeMillis();
        int lenFull = trajFull.length;
        HashSet<Integer> set = new HashSet<>(lenFull);
        Random rand = new Random(1);

        int lenRes = (int) (trajFull.length * rate) + 1;
        Trajectory[] res = new Trajectory[lenRes];

        int cnt = 0;
        while (true) {
            int trajId = rand.nextInt(lenFull);
            if (set.contains(trajId)) {
                continue;
            }
            res[cnt] = trajFull[trajId];
            cnt++;
            set.add(trajId);
            if (cnt >= lenRes) {
                break;
            }
        }
        long t2 = System.currentTimeMillis();
//        UserInterface.algTime = t2 - t1;

        return res;
    }

    private static double[] initScore;

    private static void scoreInit() {
        GreedyChoose.clean();
        int i = 0;
        for (Trajectory traj : trajFull) {
            traj.setScore(initScore[i++]);
            GreedyChoose.addNewTraj(traj);
        }
    }

    private static void CellGridUpdate(Trajectory traj, int DELTA) {
        if (DELTA == 0)
            return;
        for (Location p : traj.locations) {
            float px = map.getScreenPosition(p).x;
            float py = map.getScreenPosition(p).y;
            for (int i = -DELTA; i <= DELTA; i++) {
                for (int j = -DELTA; j <= DELTA; j++) {
                    Position pos = new Position(px + i, py + j);
                    if (totalTrajPos.contains(pos))
                        trajSet.add(pos);
                }
            }
        }
    }

    private static void Traj2CellScore(Trajectory traj) {
        traj.setScore(0);
        for (Location p : traj.locations) {
//            double px = map.getScreenPosition(p).x;
//            double py = map.getScreenPosition(p).y;
            if (trajSet.contains(new Position(p.getLat(), p.getLon())))  // R+
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
