package model;

import de.fhpotsdam.unfolding.UnfoldingMap;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class SharedObject {

    private static SharedObject instance = new SharedObject();

    private SharedObject() {
    }

    public static SharedObject getInstance() {
        return instance;
    }


    // total trajList
    private static Trajectory[] totalTraj;
    private static Trajectory[] randomTraj;
    private static Trajectory[] VFGSTraj;

    private static Trajectory[][] trajArray = new Trajectory[3][];

    // regions
    private static Region regionO = null;
    private static Region regionD = null;
    private static ArrayList<ArrayList<Region>> regionWLayerList;
    //map
    private static UnfoldingMap map;

    //trajectory
    public Trajectory[] getTotalTraj() {
        return totalTraj;
    }

    public void setTotalTraj(Trajectory[] totalTraj) {
        SharedObject.totalTraj = totalTraj;
        SharedObject.trajArray[0] = totalTraj;
    }

    public Trajectory[] getRandomTraj() {
        return randomTraj;
    }

    public void setRandomTraj(Trajectory[] randomTraj) {
        SharedObject.randomTraj = randomTraj;
        SharedObject.trajArray[1] = randomTraj;
    }

    public Trajectory[] getVFGSTraj() {
        return VFGSTraj;
    }

    public void setVFGSTraj(Trajectory[] VFGSTraj) {
        SharedObject.VFGSTraj = VFGSTraj;
        SharedObject.trajArray[2] = VFGSTraj;
    }

    public Trajectory[][] getTrajArray() {
        return trajArray;
    }

    public void setTrajArray(Trajectory[][] trajArray) {
        SharedObject.trajArray = trajArray;
    }

    //regions
    public void setRegionO(Region r) {
        regionO = r;
    }

    public Region getRegionO() {
        return regionO;
    }


    public void setRegionD(Region r) {
        regionD = r;
    }

    public Region getRegionD() {
        return regionD;
    }

    //map
    public void setMap(UnfoldingMap map) {
        SharedObject.map = map;
    }

    public UnfoldingMap getMap() {
        return map;
    }
}
