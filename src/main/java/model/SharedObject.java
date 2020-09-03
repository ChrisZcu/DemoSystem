package model;

import de.fhpotsdam.unfolding.UnfoldingMap;

public class SharedObject {

    private static final SharedObject instance = new SharedObject();

    private SharedObject() {
    }

    public static SharedObject getInstance() {
        return instance;
    }

    // modified
    private static Trajectory[] trajFull;                   // total trajList
    private static Trajectory[][][] trajVfgsMtx = null;     // trajVfgs for delta X rate
    private static Trajectory[][] trajRandList = null;      // trajRand for rate

    private static Trajectory[][] trajArray = new Trajectory[3][];

    private static TrajBlock[] blockList;        // modified

    // regions
    private static Region regionO = null;
    private static Region regionD = null;

    //map
    private static UnfoldingMap map;

    //trajectory
    public Trajectory[] getTrajFull() {
        return trajFull;
    }

    public void setTrajFull(Trajectory[] trajFull) {
        SharedObject.trajFull = trajFull;
    }

    public Trajectory[][] getTrajRandList() {
        return trajRandList;
    }

    public void setTrajRandList(Trajectory[][] trajRandList) {
        SharedObject.trajRandList = trajRandList;
    }

    public Trajectory[][][] getTrajVfgsMtx() {
        return trajVfgsMtx;
    }

    public void setTrajVfgsMtx(Trajectory[][][] trajVfgsMtx) {
        SharedObject.trajVfgsMtx = trajVfgsMtx;
    }

    public TrajBlock[] getBlockList() {
        return blockList;
    }

    public static void setBlockList(TrajBlock[] blockList) {
        SharedObject.blockList = blockList;
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
