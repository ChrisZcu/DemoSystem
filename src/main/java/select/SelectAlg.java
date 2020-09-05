package select;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import model.Position;
import model.Region;
import app.SharedObject;
import model.Trajectory;

import java.util.ArrayList;
import java.util.List;

/**
 * implements all the backend select algorithms.
 */
public class SelectAlg {

    /**
     * Based on the begin and end index to calculate the sub-array trajectory
     * with the particular origin and destination.
     * <br>
     * regions are all in the SO, regionO, regionD are exclusive.
     *
     * @param begin      the begin index, included.
     * @param end        the end index, not included.
     * @param trajectory the trajectory based, including Full, VFGS, Random
     */
    public static Trajectory[] getODTraj(int begin, int end, Trajectory[] trajectory, int optIndex) {
        SharedObject instance = SharedObject.getInstance();
        UnfoldingMap map = instance.getMapList()[optIndex];

        float xOff = instance.getMapLocInfo()[0][optIndex];
        float yOff = instance.getMapLocInfo()[1][optIndex];

        Region regionO = instance.getRegionO(), regionD = instance.getRegionD();


        ArrayList<Trajectory> res = new ArrayList<>();
        for (int i = begin; i < end; i++) {
            Trajectory traj = trajectory[i];
            if (inCheck(regionO, traj.locations[0], map) && inCheck(regionD, traj.locations[traj.locations.length - 1], map)) {
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


    /**
     * calculates the sub-array of trajectory based on way-point region, on the same layer.
     */
    public static ArrayList<Integer> getWayPointTraj(int begin, int end, Trajectory[] trajectory, ArrayList<Region> regionWList, int optIndex) {
        ArrayList<Integer> res = new ArrayList<>();
        SharedObject instance = SharedObject.getInstance();
        UnfoldingMap map = instance.getMapList()[optIndex];
        float xOff = instance.getMapLocInfo()[0][optIndex];
        float yOff = instance.getMapLocInfo()[1][optIndex];

        for (int i = begin; i < end; i++) {
            Trajectory traj = trajectory[i];
            for (int j = 1, bound = traj.locations.length - 1; j < bound; j++) {
                if (inCheck(regionWList, traj.locations[j], map)) {
                    res.add(i);
                    break;
                }
            }
        }
        return res;
    }

    public static Trajectory[] getWayPointTraj(int begin, int end, Trajectory[] trajectory, int optIndex) {

        ArrayList<ArrayList<Region>> regionWList = SharedObject.getInstance().getRegionWLayerList();
        ArrayList<Integer> res = getWayPointTraj(begin, end, trajectory, regionWList.get(0), optIndex);

        for (int i = 0; i < regionWList.size(); i++) {
            ArrayList<Integer> resTmp = getWayPointTraj(begin, end, trajectory, regionWList.get(i), optIndex);
            res.retainAll(resTmp);
        }
        Trajectory[] trajAry = new Trajectory[res.size()];
        int i = 0;
        for (Integer e : res) {
            trajAry[i] = SharedObject.getInstance().getTrajFull()[e];
            i++;
        }
        return trajAry;
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
//        float[] sp = map.mapDisplay.getScreenFromInnerObjectPosition(loc.x, loc.y);     // FIXME
//        float px = sp[0], py = sp[1];

        double px = map.getScreenPosition(loc).x;
        double py = map.getScreenPosition(loc).y;
        Position leftTop = r.leftTop;
        Position rightBtm = r.rightBtm;
        return (px >= leftTop.x && px <= rightBtm.x) && (py >= leftTop.y && py <= rightBtm.y);
    }

    //16.26
    private static boolean inCheck(ArrayList<Region> rList, Location loc, UnfoldingMap map) {
        if (rList == null) {
            return true;
        }
        double px = map.getScreenPosition(loc).x;
        double py = map.getScreenPosition(loc).y;
        for (Region r : rList) {
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
