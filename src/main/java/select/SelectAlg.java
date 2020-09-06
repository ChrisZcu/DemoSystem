package select;

import app.SharedObject;
import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import model.Position;
import model.Region;
import model.Trajectory;

import java.util.ArrayList;
import java.util.List;

/**
 * implements all the backend select algorithms.
 */
public class SelectAlg {

    /**
     * Based on the begin and end index to calculate the sub-array trajList
     * with the particular origin and destination.
     * <br>
     * regions are all in the SO, regionO, regionD are exclusive.
     *
     * @param begin    the begin index, included.
     * @param end      the end index, not included.
     * @param trajList the trajList based, including Full, VFGS, Random
     */
    public static Trajectory[] getODTraj(int begin, int end, Trajectory[] trajList, int optIndex) {
        SharedObject instance = SharedObject.getInstance();
        UnfoldingMap map = instance.getMapList()[optIndex];

        Region regionO = instance.getRegionOList()[optIndex];
        Region regionD = instance.getRegionDList()[optIndex];

        ArrayList<Trajectory> res = new ArrayList<>();
        for (int i = begin; i < end; i++) {
            Trajectory traj = trajList[i];
            Location[] locations = traj.locations;
            if (inCheck(regionO, locations[0], map)
                    && inCheck(regionD, locations[locations.length - 1], map)) {
                res.add(traj);
            }
        }
        return res.toArray(new Trajectory[0]);
    }


    /**
     * calculates the sub-array of trajectory based on way-point region, on the same layer.
     */
    public static ArrayList<Trajectory> getWayPointTraj(int begin, int end, Trajectory[] trajList,
                                                     ArrayList<Region> regionWList, int optIndex) {
        ArrayList<Trajectory> res = new ArrayList<>();
        SharedObject instance = SharedObject.getInstance();
        UnfoldingMap map = instance.getMapList()[optIndex];
//        float xOff = instance.getMapLocInfo()[0][optIndex];
//        float yOff = instance.getMapLocInfo()[1][optIndex];

        for (int i = begin; i < end; i++) {
            Trajectory traj = trajList[i];
            Location[] locations = traj.locations;
            for (int j = 1, bound = locations.length - 1; j < bound; j++) {
                if (inCheck(regionWList, locations[j], map)) {
                    res.add(traj);
                    break;
                }
            }
        }
        return res;
    }

    /**
     * Calculate way point result according to multi-level layers in {@link SharedObject}.
     * This method will call {@link #getWayPointTraj} as underlying method.
     */
    public static Trajectory[] getWayPointTraj(int begin, int end, Trajectory[] trajectory, int optIndex) {

        ArrayList<ArrayList<Region>> regionWList = SharedObject.getInstance().getRegionWList()[optIndex];

        ArrayList<Trajectory> res = getWayPointTraj(begin, end, trajectory, regionWList.get(0), optIndex);

        for (int i = 1; i < regionWList.size(); i++) {
            ArrayList<Trajectory> resTmp = getWayPointTraj(begin, end, trajectory, regionWList.get(i), optIndex);
            res.retainAll(resTmp);
        }
        return res.toArray(new Trajectory[0]);
    }

    /**
     * calculates the sub-array of trajectory based on all-in region.
     */
    public static Trajectory[] getAllIn(int begin, int end, Trajectory[] trajectory, Region r, int optIndex) {
        SharedObject instance = SharedObject.getInstance();
        UnfoldingMap map = instance.getMapList()[optIndex];
        float xOff = instance.getMapLocInfo()[0][optIndex];
        float yOff = instance.getMapLocInfo()[1][optIndex];

        List<Trajectory> res = new ArrayList<>();
        for (int i = begin; i < end; i++) {
            Trajectory traj = trajectory[i];
            boolean inFlag = true;
            for (Location loc : traj.getLocations()) {
                if (!inCheck(r, loc, map)) {
                    inFlag = false;
                    break;
                }
            }
            if (inFlag) {
                res.add(traj);
            }
        }
        Trajectory[] trajAry = new Trajectory[res.size()];
        int i = 0;
        for (Trajectory traj : res) {
            trajAry[i] = traj;
            i++;
        }
        return trajAry;
    }

    public static Trajectory[] getODWTraj(int begin, int end, Trajectory[] trajectory, int optIndex) {
        Trajectory[] ODTraj = getODTraj(begin, end, trajectory, optIndex);
        return getWayPointTraj(0, ODTraj.length, ODTraj, optIndex);
    }

    /**
     * checks whether the location is in the region
     */
    private static boolean inCheck(Region r, Location loc, UnfoldingMap map) {
        if (r == null) {
            return true;
        }

        r.updateScreenPosition(map);    // TODO How about run it before the whole alg ?

        ScreenPosition sp = map.getScreenPosition(loc);
        double px = sp.x;
        double py = sp.y;

        Position leftTop = r.leftTop;
        Position rightBtm = r.rightBtm;

        return (px >= leftTop.x && px <= rightBtm.x)
                && (py >= leftTop.y && py <= rightBtm.y);
    }

    //16.26
    private static boolean inCheck(ArrayList<Region> rList, Location loc, UnfoldingMap map) {
        if (rList == null) {
            return false;
        }
        ScreenPosition sp = map.getScreenPosition(loc);
        double px = sp.x;
        double py = sp.y;
        for (Region r : rList) {
            r.updateScreenPosition(map);        // TODO How about run it before the whole alg ?
            Position leftTop = r.leftTop;
            Position rightBtm = r.rightBtm;
            if ((px >= leftTop.x && px <= rightBtm.x)
                    && (py >= leftTop.y && py <= rightBtm.y)) {
                return true;
            }
        }

        return false;
    }
}
