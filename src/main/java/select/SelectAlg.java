package select;

import app.CircleRegionControl;
import app.SharedObject;
import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import model.*;

import java.util.ArrayList;
import java.util.HashSet;
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
    public static ArrayList<Trajectory> getODTraj(CircleRegion circleO, CircleRegion circleD, int begin, int end, Trajectory[] trajList, int optIndex) {
        return getSingleODTrajId(circleO, circleD, begin, end, trajList, optIndex);
    }

    private static ArrayList<Trajectory> getSingleODTrajId(CircleRegion circleO, CircleRegion circleD, int begin, int end, Trajectory[] trajList, int optIndex) {
        SharedObject instance = SharedObject.getInstance();
        UnfoldingMap map = instance.getMapList()[optIndex];
        ArrayList<Trajectory> trajResSet = new ArrayList<>();
        if (circleO != null) {
            circleO.updateCircleScreenPosition(optIndex);
        }
        if (circleD != null) {
            circleD.updateCircleScreenPosition(optIndex);
        }

        for (int i = begin; i < end; i++) {
            Trajectory traj = trajList[i];
            Location[] locations = traj.locations;
            if (inCheck(circleO, locations[0], map)
                    && inCheck(circleD, locations[locations.length - 1], map)) {
                trajResSet.add(traj);
            }
        }
        return trajResSet;
    }

    /**
     * Calculate way point result according to multi-level layers in {@link SharedObject}.
     * This method will call {@link #getWayPointTraj} as underlying method.
     */
    public static ArrayList<Trajectory> getWayPointTraj(CircleRegion circleO, CircleRegion circleD, ArrayList<CircleRegion> groupList, int begin, int end, Trajectory[] trajectory, int optIndex) {
        return getSingleWayPoint(circleO, circleD, groupList, begin, end, trajectory, optIndex);
    }

    private static ArrayList<Trajectory> getSingleWayPoint(CircleRegion circleO, CircleRegion circleD, ArrayList<CircleRegion> wayPointList,
                                                           int begin, int end, Trajectory[] trajectory, int optIndex) {
        ArrayList<Trajectory> trajIdSet = new ArrayList<>();
        UnfoldingMap map = SharedObject.getInstance().getMapList()[optIndex];
        if (circleO != null) {
            circleO.updateCircleScreenPosition(optIndex);
        }
        if (circleD != null) {
            circleD.updateCircleScreenPosition(optIndex);
        }
        for (int i = begin; i < end; i++) {

            Trajectory traj = trajectory[i];
            boolean crsAll = true;
            if (inCheck(circleO, traj.locations[0], map) && inCheck(circleD, traj.locations[traj.locations.length - 1], map)) {
                if (wayPointList != null && wayPointList.size() > 0) {
                    for (CircleRegion circleRegion : wayPointList) {
                        circleRegion.updateCircleScreenPosition(optIndex);
                        if (!isSingleRegionWCrs(traj, circleRegion, map)) {
                            crsAll = false;
                            break;
                        }
                    }
                }
                if (crsAll) {
                    trajIdSet.add(traj);
                }
            }
        }
        return trajIdSet;
    }

    private static boolean isSingleRegionWCrs(Trajectory traj, CircleRegion circleRegion, UnfoldingMap map) {
        for (Location location : traj.locations) {
            if (inCheck(circleRegion, location, map)) {
                return true;
            }
        }
        return false;
    }

    /**
     * calculates the sub-array of trajectory based on all-in region.
     */

    public static ArrayList<Trajectory> getODWTraj(CircleRegion circleO, CircleRegion circleD, ArrayList<CircleRegion> wayPointList, int begin, int end, Trajectory[] trajectory, int optIndex) {
        ArrayList<Trajectory> ODTraj = getODTraj(circleO, circleD, begin, end, trajectory, optIndex);
        return getWayPointTraj(circleO, circleD, wayPointList, 0, ODTraj.size(), ODTraj.toArray(new Trajectory[0]), optIndex);
    }

    /**
     * checks whether the location is in the region
     */

    private static boolean inCheck(CircleRegion circleRegion, Location loc, UnfoldingMap map) {
        if (circleRegion == null) {
            return true;
        }


        ScreenPosition sp = map.getScreenPosition(loc);

        double px = sp.x;
        double py = sp.y;

        float radius = circleRegion.getRadius();
        ScreenPosition circleCenter = map.getScreenPosition(circleRegion.getCircleCenter());
        float centerX = circleCenter.x;
        float centerY = circleCenter.y;

        return Math.pow((Math.pow(centerX - px, 2) + Math.pow(centerY - py, 2)), 0.5) < radius;
    }
}
