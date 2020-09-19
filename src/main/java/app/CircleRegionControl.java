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

    private ArrayList<ArrayList<CircleRegion>> groupsOfCircle = new ArrayList<>();
    private ArrayList<ArrayList<Integer>> circleO = new ArrayList<>();
    private ArrayList<ArrayList<Integer>> circleD = new ArrayList<>();
    private ArrayList<ArrayList<Integer>> wayPoint = new ArrayList<>();
    private int curDrawingGroupId = 0;
    private CircleRegion curDrawingCircle = null;
    private CircleRegion curMovingCircle = null;

    public ArrayList<ArrayList<CircleRegion>> getGroupsOfCircle() {
        return groupsOfCircle;
    }

    public void setGroupsOfCircle(ArrayList<ArrayList<CircleRegion>> groupsOfCircle) {
        this.groupsOfCircle = groupsOfCircle;
    }

    public ArrayList<ArrayList<Integer>> getCircleO() {
        return circleO;
    }

    public void setCircleO(ArrayList<ArrayList<Integer>> circleO) {
        this.circleO = circleO;
    }

    public ArrayList<ArrayList<Integer>> getCircleD() {
        return circleD;
    }

    public void setCircleD(ArrayList<ArrayList<Integer>> circleD) {
        this.circleD = circleD;
    }

    public ArrayList<ArrayList<Integer>> getWayPoint() {
        return wayPoint;
    }

    public void setWayPoint(ArrayList<ArrayList<Integer>> wayPoint) {
        this.wayPoint = wayPoint;
    }

    public int getCurDrawingGroupId() {
        return curDrawingGroupId;
    }

    public void addCurDrawingGroupId() {
        curDrawingGroupId++;
    }

    public CircleRegion getCurDrawingCircle() {
        return curDrawingCircle;
    }

    public void setCurDrawingCircle(CircleRegion curDrawingCircle) {
        this.curDrawingCircle = curDrawingCircle;
    }

    public CircleRegion getCurMovingCircle() {
        return curMovingCircle;
    }

    public void setCurMovingCircle(CircleRegion curMovingCircle) {
        this.curMovingCircle = curMovingCircle;
    }

    private CircleRegionControl() {
    }

    public static CircleRegionControl getCircleRegionControl() {
        return circleRegionControl;
    }

    public void cleanCircleRegions() {
        groupsOfCircle.clear();
        circleO.clear();
        circleD.clear();
        wayPoint.clear();

        groupsOfCircle = new ArrayList<>();
        circleO = new ArrayList<>();
        circleD = new ArrayList<>();
        wayPoint = new ArrayList<>();

        groupsOfCircle.add(new ArrayList<>());
        circleO.add(new ArrayList<>());
        circleD.add(new ArrayList<>());
        wayPoint.add(new ArrayList<>());

        for (int i = 0; i < groupsOfCircle.size(); ++i) {
            System.out.println(groupsOfCircle.get(i));
        }
        for (int i = 0; i < circleO.size(); ++i) {
            System.out.println(circleO.get(i));
            System.out.println(circleD.get(i));
            System.out.println(wayPoint.get(i));
        }
        System.out.println();
    }


//    public CircleRegion getCircleO() {
//        return circleO;
//    }
//
//    public CircleRegion getCircleD() {
//        return circleD;
//    }
//
//    public ArrayList<CircleRegionGroup> getCircleRegionGroups() {
//        return circleRegionGroups;
//    }
//
//    public CircleRegion[] getCircleOList() {
//        return circleOList;
//    }
//
//    public CircleRegion[] getCircleDList() {
//        return circleDList;
//    }
//
//    public ArrayList<CircleRegionGroup>[] getCircleWayPointList() {
//        return circleWayPointList;
//    }
//
//    public int getWayPointLayer() {
//        return wayPointLayer;
//    }
//
//    public int getWayPointGroup() {
//        return wayPointGroup;
//    }

    // circle modified by the front-end, and only object to update the region list.
//    private CircleRegion circleO = null;
//    private CircleRegion circleD = null;
//    private ArrayList<CircleRegionGroup> circleRegionGroups = new ArrayList<>();
//
//    // draw by the front-end, and back-end alg based.
//    private CircleRegion[] circleOList = new CircleRegion[4];
//    private CircleRegion[] circleDList = new CircleRegion[4];
//    private ArrayList<CircleRegionGroup>[] circleWayPointList = new ArrayList[4];
//
//    private int wayPointLayer = 0;
//    private int wayPointGroup = 0;

//    public void setCircleO(CircleRegion circleO) {
//        this.circleO = circleO;
//        updateCircleOList();
//    }
//
//    public void setCircleD(CircleRegion circleD) {
//        this.circleD = circleD;
//        updateCircleDList();
//    }

//    public void addWayPoint(CircleRegion circle) {
//        if (wayPointGroup == 0) {
//            addNewGroup();
//        }
//        circleRegionGroups.get(wayPointGroup - 1).addWayPoint(circle);
//        updateCircleWayPointList(circle);
//    }
//
//    public void addNewGroup() {
//        circleRegionGroups.add(new CircleRegionGroup(wayPointGroup++));
//    }
//
//    private void updateCircleOList() {
//        for (int i = 0; i < 4; i++) {
//            circleOList[i] = circleO.getCrsRegionCircle(i);
//        }
//    }
//
//    private void updateCircleDList() {
//        for (int i = 0; i < 4; i++) {
//            circleDList[i] = circleD.getCrsRegionCircle(i);
//        }
//    }
//
//    private void updateCircleWayPointList(CircleRegion circle) {
//        for (int i = 0; i < 4; i++) {
//            if (circleWayPointList[i] == null) {
//                circleWayPointList[i] = new ArrayList<>();
//            }
//            if (circleWayPointList[i].size() == wayPointGroup - 1) {
//                circleWayPointList[i].add(new CircleRegionGroup(wayPointGroup - 1));
//            }
//            circleWayPointList[i].get(wayPointGroup - 1).addWayPoint(circle.getCrsRegionCircle(i));
//        }
//    }
//
//    public RegionType getRegionType() {
//        if (circleRegionGroups.size() > 0) {
//            if (circleO == null && circleD == null) {
//                return RegionType.WAY_POINT;
//            } else {
//                return RegionType.O_D_W;
//            }
//        } else {
//            return RegionType.O_D;
//        }
//    }
//
//    public void updateMovedRegion(CircleRegion circle) {
//        for (CircleRegion circleRegion : getAllCircleRegions()) {
//            if (circle.getId() == circleRegion.getId()) {
//                circleRegion.setCircleCenter(circle.getCircleCenter());
//                circleRegion.setRadiusLocation(circle.getRadiusLocation());
//            }
//        }
//        circleO = circleOList[0];
//        circleD = circleDList[0];
//        circleRegionGroups = circleWayPointList[0] == null ? new ArrayList<>() : circleWayPointList[0];
//    }
//
//    public ArrayList<CircleRegion> getAllCircleRegions() {
//        ArrayList<CircleRegion> allCircles = new ArrayList<>();
//        if (circleO != null) {
//            allCircles.addAll(Arrays.asList(circleOList));
//        }
//        if (circleD != null) {
//            allCircles.addAll(Arrays.asList(circleDList));
//        }
//        if (circleRegionGroups != null && circleRegionGroups.size() > 0) {
//            for (ArrayList<CircleRegionGroup> circleRegionGroups : circleWayPointList) {
//                for (CircleRegionGroup circleRegionGroup : circleRegionGroups) {
//                    allCircles.addAll(circleRegionGroup.getAllRegions());
//                }
//            }
//        }
//        return allCircles;
//    }
//
//    public ArrayList<CircleRegion> getAllRegionsInOneMap() {
//        ArrayList<CircleRegion> allCircles = new ArrayList<>();
//        if (circleO != null) {
//            allCircles.add(circleO);
//        }
//        if (circleD != null) {
//            allCircles.add(circleD);
//        }
//        if (circleRegionGroups != null && circleRegionGroups.size() > 0) {
//            for (CircleRegionGroup circleRegionGroup : circleRegionGroups) {
//                allCircles.addAll(circleRegionGroup.getAllRegions());
//            }
//        }
//        return allCircles;
//    }
//
//    public void updateLayer() {
//        circleRegionGroups.get(wayPointGroup - 1).updateWayPointLayer();
//    }
//
//    public int getWayLayer() {
//        return circleRegionGroups.size() == 0 ? 0 : circleRegionGroups.get(wayPointGroup - 1).getWayPointLayer();
//    }
//
//    public int getWayGroupId() {
//        return circleRegionGroups.size() == 0 ? 0 : circleRegionGroups.get(wayPointGroup - 1).getGroupId();
//    }
//
//    public void cleanCircleRegions() {
//        circleO = null;
//        circleD = null;
//        circleRegionGroups.clear();
//        circleRegionGroups = new ArrayList<>();
//
//        circleOList = new CircleRegion[4];
//        circleDList = new CircleRegion[4];
//        circleWayPointList = new ArrayList[4];
//
//        wayPointGroup = 0;
//        wayPointLayer = 0;
//    }
}
