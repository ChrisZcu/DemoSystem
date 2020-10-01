package util;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import model.Position;
import model.RectRegion;
import model.Trajectory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

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
                double px = map.getScreenPosition(p).x;
                double py = map.getScreenPosition(p).y;
                totalTrajPos.add(new Position(px, py));
            }
        }
    }

    private static GreedyChoose GreedyChoose;

    private static HashSet<Position> trajSet = new HashSet<>();    // R+

    public static Trajectory[] getCellCover(Trajectory[] trajFull, UnfoldingMap maps, double rate) {//record cal
        GreedyChoose = new GreedyChoose(trajFull.length);
        map = maps;
        initTrajFull(trajFull);
        totalTrajPosInit(trajFull);

        ArrayList<Trajectory> cellList = new ArrayList<>();

        int TRAJNUM = trajFull.length;
        for (int j = 0; j < 1; j++) {
            int DELTA = 0;
            trajSet.clear();
            scoreInit();
            int n = 1;
            int i = 0;
            while (n > 0) {
                n--;
                int trajNum = (int) (rate * TRAJNUM);
                for (; i < trajNum; i++) {
                    while (true) {
                        Traj2CellScore(GreedyChoose.getHeapHead(), DELTA);
                        if (GreedyChoose.GreetOrder()) {
                            Trajectory traj = GreedyChoose.getMaxScoreTraj();   // deleteMax
                            for (int num = 0; num <= n; num++) {
                                cellList.add(traj);    // take this, add to R
                            }
                            CellGridUpdate(traj, DELTA);        // update R+
                            break;
                        } else {
                            GreedyChoose.orderAdjust();
                        }
                    }
                }
            }
        }

        trajSet.clear();
        return cellList.toArray(new Trajectory[0]);
    }

    public static Trajectory[] getCellCover(Trajectory[] trajFull, UnfoldingMap maps, double rate, RectRegion region) {
        Trajectory[] resTmp = getCellCover(trajFull, maps, rate);
        ArrayList<Trajectory> res = new ArrayList<>();
        for (Trajectory traj : resTmp) {
            res.addAll(getRegionInTraj(traj, region));
        }
        System.out.println("rate = " + rate + "," + trajFull.length + " --> " + res.size());
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
        for (Location p : traj.locations) {
            double px = map.getScreenPosition(p).x;
            double py = map.getScreenPosition(p).y;
            for (int i = -DELTA; i <= DELTA; i++) {
                for (int j = -DELTA; j <= DELTA; j++) {
                    Position pos = new Position(px + i, py + j);
                    if (!totalTrajPos.contains(pos))
                        trajSet.add(pos);
                }
            }
        }
    }

    private static void Traj2CellScore(Trajectory traj, int DELTA) {
        traj.setScore(0);
        HashSet<Location> set = new HashSet<>();
        for (Location p : traj.locations) {
            double px = map.getScreenPosition(p).x;
            double py = map.getScreenPosition(p).y;
            if (set.contains(p)
                    || trajSet.contains(new Position(px, py)))  // R+
                continue;
            // not in R+ or self point set
            set.add(p);
            int score = InScoreCheck(px, py, DELTA);
            if (score == 0) {
                continue;
            }
            traj.setScore(traj.getScore() + score);
        }
    }

    private static int InScoreCheck(double px, double py, int DELTA) {
        int score = 1;
        for (int i = -DELTA; i <= DELTA; i++) {
            for (int j = -DELTA; j <= DELTA; j++) {
                if (trajSet.contains(new Position(px + i, py + j))) {
                    return 0;
                }
            }
        }
        return score;
    }

}
