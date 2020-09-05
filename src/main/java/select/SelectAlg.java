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
    public static int[] getODTraj(int begin, int end, Trajectory[] trajectory, int optIndex) {
        SharedObject instance = SharedObject.getInstance();
        UnfoldingMap map = instance.getMapList()[optIndex];
        float xOff = instance.getMapLocInfo()[0][optIndex];
        float yOff = instance.getMapLocInfo()[1][optIndex];

        Region regionO = instance.getRegionO(), regionD = instance.getRegionD();


        ArrayList<Integer> res = new ArrayList<>();
        for (int i = begin; i < end; i++) {
            Trajectory traj = trajectory[i];
            if (inCheck(regionO, traj.locations[0], map, xOff, yOff) && inCheck(regionD, traj.locations[traj.locations.length - 1], map, xOff, yOff)) {
                res.add(traj.getTrajId());
            }
        }
        return res.stream().mapToInt(Integer::intValue).toArray();
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
                if (inCheck(regionWList, traj.locations[j], map,xOff, yOff)) {
                    res.add(j);
                    break;
                }
            }
        }
        return res;
    }

    public static int[] getWayPointTraj(int begin, int end, Trajectory[] trajectory, int optIndex) {
        SharedObject instance = SharedObject.getInstance();
        UnfoldingMap map = instance.getMapList()[optIndex];

        ArrayList<ArrayList<Region>> regionWList = SharedObject.getInstance().getRegionWLayerList();
        ArrayList<Integer> res = getWayPointTraj(begin, end, trajectory, regionWList.get(0), optIndex);

        for (int i = 0; i < regionWList.size(); i++) {
            ArrayList<Integer> resTmp = getWayPointTraj(begin, end, trajectory, regionWList.get(i), optIndex);
            res.retainAll(resTmp);
        }
        return res.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * calculates the sub-array of trajectory based on all-in region.
     */
    public static int[] getAllIn(int begin, int end, Trajectory[] trajectory, Region r, int optIndex) {
        SharedObject instance = SharedObject.getInstance();
        UnfoldingMap map = instance.getMapList()[optIndex];
        float xOff = instance.getMapLocInfo()[0][optIndex];
        float yOff = instance.getMapLocInfo()[1][optIndex];

        List<Integer> res = new ArrayList<>();
        for (int i = begin; i < end; i++) {
            Trajectory traj = trajectory[i];
            boolean inFlag = true;
            for (Location loc : traj.getLocations()) {
                if (!inCheck(r, loc, map,xOff, yOff)) {
                    inFlag = false;
                    break;
                }
            }
            if (inFlag) {
                res.add(traj.getTrajId());
            }
        }
        return res.stream().mapToInt(Integer::intValue).toArray();
    }

    public static int[] getODWTraj(int begin, int end, Trajectory[] trajectory, int optIndex) {
        SharedObject instance = SharedObject.getInstance();
        UnfoldingMap map = instance.getMapList()[optIndex];

        int[] ODTraj = getODTraj(begin, end, trajectory, optIndex);
        Trajectory[] trajOD = new Trajectory[ODTraj.length];
        for (int i = 0; i < ODTraj.length; i++) {
            trajOD[i] = SharedObject.getInstance().getTrajFull()[ODTraj[i]];
        }
        return getWayPointTraj(0, ODTraj.length, trajOD, optIndex);
    }

    /**
     * checks whether the location is in the region
     */
    private static boolean inCheck(Region r, Location loc, UnfoldingMap map, float xOff, float yOff) {
        if (r == null) {
            return true;
        }
//        float[] sp = map.mapDisplay.getScreenFromInnerObjectPosition(loc.x, loc.y);     // FIXME
//        float px = sp[0], py = sp[1];

        double px = map.getScreenPosition(loc).x;
        double py = map.getScreenPosition(loc).y;
        Position leftTop = r.leftTop;
        Position rightBtm = r.rightBtm;
        return (px >= leftTop.x + xOff && px <= rightBtm.x + xOff) && (py >= leftTop.y + yOff && py <= rightBtm.y + yOff);
    }

    private static boolean inCheck(ArrayList<Region> rList, Location loc, UnfoldingMap map, float xOff, float yOff) {
        if (rList == null) {
            return true;
        }
        double px = map.getScreenPosition(loc).x;
        double py = map.getScreenPosition(loc).y;
        for (Region r : rList) {
            Position leftTop = r.leftTop;
            Position rightBtm = r.rightBtm;
            if ((px >= leftTop.x + xOff && px <= rightBtm.x + xOff)
                    && (py >= leftTop.y + yOff && py <= rightBtm.y + yOff)) {
                return true;
            }
        }

        return false;
    }
}
