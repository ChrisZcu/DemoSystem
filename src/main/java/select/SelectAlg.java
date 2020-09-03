package select;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import model.Position;
import model.Region;
import model.SharedObject;
import model.Trajectory;

import java.util.ArrayList;
import java.util.List;

/**
 * implements all the backend select algorithms.
 */
public class SelectAlg {

    /**
     * based on the begin and end index to calculate the sub-array trajectory with the particular origin and destination.
     * <br></>
     * regions are all in the SO, regionO, regionD are exclusive.
     *
     * @param begin      the begin index, included. <br></>
     * @param end        the end index, not included.
     * @param trajectory the trajectory based, including Full, VFGS, Random
     **/
    public static int[] getODTraj(int begin, int end, Trajectory[] trajectory) {
        Region regionO = SharedObject.getInstance().getRegionO(), regionD = SharedObject.getInstance().getRegionD();

        ArrayList<Integer> res = new ArrayList<>();
        for (int i = begin; i < end; i++) {
            Trajectory traj = trajectory[i];
            if (inCheck(regionO, traj.locations[0]) && inCheck(regionD, traj.locations[traj.locations.length - 1])) {
                res.add(traj.getTrajId());
            }
        }

        return res.stream().mapToInt(Integer::intValue).toArray();
    }


    /**
     * calculates the sub-array of trajectory based on way-point region, on the same layer.
     */
    public static int[] getWayPointTraj(int begin, int end, Trajectory[] trajectory, ArrayList<Region> regionWList) {
        ArrayList<Integer> res = new ArrayList<>();

        for (int i = begin; i < end; i++) {
            Trajectory traj = trajectory[i];
            for (int j = 1, bound = traj.locations.length - 1; j < bound; j++) {
                if (inCheck(regionW, traj.locations[j])) {
                    res.add(j);
                    break;
                }
            }
        }
        return res.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * calculates the sub-array of trajectory based on all-in region.
     */

    public static int[] getAllIn(int begin, int end, Trajectory[] trajectory, Region r) {
        List<Integer> res = new ArrayList<>();
        for (int i = begin; i < end; i++) {
            Trajectory traj = trajectory[i];
            boolean inFlag = true;
            for (Location loc : traj.getLocations()) {
                if (!inCheck(r, loc)) {
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

    public static int[] getWMutliLayer() {
        //TODO add logic
        return null;
    }

    /**
     * checks whether the location is in the region
     */
    private static boolean inCheck(Region r, Location loc) {
        if (r == null) {
            return true;
        }
        UnfoldingMap map = SharedObject.getInstance().getMap();
        float[] sp = map.mapDisplay.getScreenFromInnerObjectPosition(loc.x, loc.y);     // FIXME
        float px = sp[0], py = sp[1];
        Position left_top = r.leftTop;
        Position right_btm = r.rightBtm;
        return (px >= left_top.x && px <= right_btm.x) && (py >= left_top.y && py <= right_btm.y);
    }

    private static boolean inCheck(ArrayList<Region> rList, Location loc) {
        if (rList == null) {
            return true;
        }
        UnfoldingMap map = SharedObject.getInstance().getMap();
        double px = map.getScreenPosition(loc).x;
        double py = map.getScreenPosition(loc).y;
        for (Region r : rList) {
            Position left_top = r.leftTop;
            Position right_btm = r.rightBtm;
            if ((px >= left_top.x && px <= right_btm.x) && (py >= left_top.y && py <= right_btm.y))
                return true;
        }

        return false;
    }
}
