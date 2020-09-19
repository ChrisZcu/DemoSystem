package app;

import model.CircleRegionGroup;
import model.CircleRegion;
import model.RegionType;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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

    private Map<CircleRegion, Integer> reuseMap = new HashMap<>();
    private volatile boolean addFinished = true;

    private ArrayList<ArrayList<CircleRegion>> reusedCircles = new ArrayList<>();

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

    public Map<CircleRegion, Integer> getReuseMap() {
        return reuseMap;
    }

    public ArrayList<ArrayList<CircleRegion>> getReusedCircles() {
        return reusedCircles;
    }

    public boolean isAddFinished() {
        return addFinished;
    }

    public void setAddFinished(boolean addFinished) {
        this.addFinished = addFinished;
    }

    public void addReusedCircle(CircleRegion circle, CircleRegion newCircle) {
        if (!reuseMap.containsKey(circle)) {
            int pos = reusedCircles.size();

            reuseMap.put(circle, pos);
            reuseMap.put(newCircle, pos);

            reusedCircles.add(new ArrayList<>());
            reusedCircles.get(pos).add(circle);
            reusedCircles.get(pos).add(newCircle);
        } else {
            int pos = reuseMap.get(circle);
            reuseMap.put(newCircle, pos);
            reusedCircles.get(pos).add(newCircle);
        }
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

        reuseMap.clear();
        reusedCircles.clear();
        reusedCircles = new ArrayList<>();

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
}
