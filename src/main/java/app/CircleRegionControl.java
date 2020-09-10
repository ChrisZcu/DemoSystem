package app;

import model.CircleRegionGroup;
import model.CircleRegion;
import model.RegionType;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * The only interface to control the circle region.
 **/

public class CircleRegionControl {
    private static final CircleRegionControl circleRegionControl = new CircleRegionControl();

    private CircleRegionControl() {
    }

    public static CircleRegionControl getCircleRegionControl() {
        return circleRegionControl;
    }

    // circle modified by the front-end, and only object to update the region list.
    private CircleRegion circleO = null;
    private CircleRegion circleD = null;
    private ArrayList<CircleRegionGroup> circleRegionGroups = new ArrayList<>();

    // draw by the front-end, and back-end alg based.
    private CircleRegion[] circleOList = new CircleRegion[4];
    private CircleRegion[] circleDList = new CircleRegion[4];
    private ArrayList<CircleRegionGroup>[] circleWayPointList = new ArrayList[4];

    private int wayPointLayer = 0;
    private int wayPointGroup = 0;

    public void setCircleO(CircleRegion circleO) {
        this.circleO = circleO;
        updateCircleOList();
    }

    public void setCircleD(CircleRegion circleD) {
        this.circleD = circleD;
        updateCircleDList();
    }

    public void addWayPoint(CircleRegion circle) {
        if (wayPointGroup == 0) {
            addNewGroup();
        }
        circleRegionGroups.get(wayPointGroup - 1).addWayPoint(circle);
        updateCircleWayPointList(circle);
    }

    public void addNewGroup() {
        circleRegionGroups.add(new CircleRegionGroup(wayPointGroup++));
    }

    private void updateCircleOList() {
        for (int i = 0; i < 4; i++) {
            circleOList[i] = circleO.getCrsRegionCircle(i);
        }
    }

    private void updateCircleDList() {
        for (int i = 0; i < 4; i++) {
            circleDList[i] = circleD.getCrsRegionCircle(i);
        }
    }

    private void updateCircleWayPointList(CircleRegion circle) {
        for (int i = 0; i < 4; i++) {
            if (circleWayPointList[i] == null) {
                circleWayPointList[i] = new ArrayList<>();
            }
            if (circleWayPointList[i].size() == wayPointGroup - 1) {
                circleWayPointList[i].add(new CircleRegionGroup(wayPointGroup - 1));
            }
            circleWayPointList[i].get(wayPointGroup - 1).addWayPoint(circle.getCrsRegionCircle(i));
        }
    }

    private RegionType getRegionType() {
        if (circleRegionGroups.size() > 0) {
            if (circleO == null && circleD == null) {
                return RegionType.WAY_POINT;
            } else {
                return RegionType.O_D_W;
            }
        } else {
            return RegionType.O_D;
        }
    }

    public void updateMovedRegion(CircleRegion circle) {
        for (CircleRegion circleRegion : getAllCircleRegions()) {
            if (circle.getId() == circleRegion.getId()) {
                circleRegion.setCircleCenter(circle.getCircleCenter());
            }
        }
        circleO = circleOList[0];
        circleD = circleDList[0];
        circleRegionGroups = circleWayPointList[0] == null ? new ArrayList<>() : circleWayPointList[0];
    }

    public ArrayList<CircleRegion> getAllCircleRegions() {
        ArrayList<CircleRegion> allCircles = new ArrayList<>();
        if (circleO != null) {
            allCircles.addAll(Arrays.asList(circleOList));
        }
        if (circleD != null) {
            allCircles.addAll(Arrays.asList(circleDList));
        }
        if (circleRegionGroups != null && circleRegionGroups.size() > 0) {
            for (ArrayList<CircleRegionGroup> circleRegionGroups : circleWayPointList) {
                for (CircleRegionGroup circleRegionGroup : circleRegionGroups) {
                    allCircles.addAll(circleRegionGroup.getAllRegions());
                }
            }
        }
        return allCircles;
    }

    public ArrayList<CircleRegion> getAllRegionsInOneMap() {
        ArrayList<CircleRegion> allCircles = new ArrayList<>();
        if (circleO != null) {
            allCircles.add(circleO);
        }
        if (circleD != null) {
            allCircles.add(circleD);
        }
        if (circleRegionGroups != null && circleRegionGroups.size() > 0) {
            for (CircleRegionGroup circleRegionGroup : circleRegionGroups) {
                allCircles.addAll(circleRegionGroup.getAllRegions());
            }
        }
        return allCircles;
    }

    public void updateLayer() {
        circleRegionGroups.get(wayPointGroup - 1).updateWayPointLayer();
    }

    public int getWayLayer() {
        return circleRegionGroups.size() == 0 ? 0 : circleRegionGroups.get(wayPointGroup - 1).getWayPointLayer();
    }

    public int getWayGroupId() {
        return circleRegionGroups.size() == 0 ? 0 : circleRegionGroups.get(wayPointGroup - 1).getGroupId();
    }

    public void cleanCircleRegions() {
        circleO = circleD = null;
        circleRegionGroups.clear();
        circleRegionGroups = new ArrayList<>();

        circleOList = circleDList = new CircleRegion[4];
        circleWayPointList = new ArrayList[4];

        wayPointGroup = 0;
    }
}
