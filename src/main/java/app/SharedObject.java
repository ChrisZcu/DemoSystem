package app;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import draw.TrajDrawManager;
import model.*;
import select.SelectManager;
import util.IOHandle;
import util.PSC;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

import static util.Util.*;

public class SharedObject {

    private static final SharedObject instance = new SharedObject();

    private SharedObject() {
    }

    public static SharedObject getInstance() {
        return instance;
    }

    private static Trajectory[] trajFull;                   // total trajList
    private static Trajectory[][][] trajVfgsMtx = null;     // trajVfgs for delta X rate
    private static Trajectory[][] trajRandList = null;      // trajRand for rate

    private static Trajectory[][] trajArray = new Trajectory[3][];

    private static TrajDrawManager trajDrawManager;
    private static TrajBlock[] blockList;

    private static boolean[] viewVisibleList;

    private int mapWidth;
    private int mapHeight;

    private Location[] mapCenter;

    public Location[] getMapCenter() {
        return mapCenter;
    }

    public void setMapCenter(Location[] mapCenter) {
        this.mapCenter = mapCenter;
    }

    public int getMapWidth() {
        return mapWidth;
    }

    public void setMapWidth(int mapWidth) {
        this.mapWidth = mapWidth;
    }

    public int getMapHeight() {
        return mapHeight;
    }

    public void setMapHeight(int mapHeight) {
        this.mapHeight = mapHeight;
    }

    // regions
    private static Region regionO = null;
    private static Region regionD = null;
    private static ArrayList<ArrayList<Region>> regionWLayerList = new ArrayList<>();
    private int wayPointLayer = 1;

    private float[][] mapLocInfo = new float[2][4];

    public void setMapLocInfo(float[][] mapLocInfo) {
        this.mapLocInfo = mapLocInfo;
    }

    public float[][] getMapLocInfo() {
        return mapLocInfo;
    }

    private static boolean[] regionPresent = new boolean[3];// indicate the current region draw.

    // map
    private static UnfoldingMap[] mapList;

    private boolean finishSelectRegion;

    public boolean isScreenShot() {
        return screenShot;
    }

    private boolean screenShot;
    private boolean dragRegion = false;

    public int[][] getTrajSelectResList() {
        return trajSelectResList;
    }

    public void setTrajSelectResList(int[][] trajSelectResList) {
        this.trajSelectResList = trajSelectResList;
    }

    private int[][] trajSelectResList = new int[4][];//region select res

    public boolean isDragRegion() {
        return dragRegion;
    }

    // trajectory
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

    public TrajDrawManager getTrajDrawManager() {
        return trajDrawManager;
    }

    public void setTrajDrawManager(TrajDrawManager trajDrawManager) {
        SharedObject.trajDrawManager = trajDrawManager;
    }

    public TrajBlock[] getBlockList() {
        return blockList;
    }

    public boolean[] getViewVisibleList() {
        return viewVisibleList;
    }

    public void setViewVisibleList(boolean[] visibleList) {
        SharedObject.viewVisibleList = visibleList;
    }

    public Trajectory[][] getTrajArray() {
        return trajArray;
    }

    public void setTrajArray(Trajectory[][] trajArray) {
        SharedObject.trajArray = trajArray;
    }

    // regions
    public void setRegionO(Region r) {
        regionO = r;
        updateRegionList();
    }

    public Region getRegionO() {
        return regionO;
    }


    public void setRegionD(Region r) {
        regionD = r;
        updateRegionList();

    }

    public Region getRegionD() {
        return regionD;
    }

    // map & app


    public void setMapList(UnfoldingMap[] mapList) {
        mapCenter = new Location[mapList.length];
        int i = 0;
        for (UnfoldingMap map : mapList) {
            mapCenter[i] = map.getCenter();
            i++;
        }
        SharedObject.mapList = mapList;
    }

    public UnfoldingMap[] getMapList() {
        return mapList;
    }

    public void eraseRegionPren() {
        Arrays.fill(regionPresent, false);
    }

    public void updateRegionPreList(int regionId) {
        eraseRegionPren();
        regionPresent[regionId] = true;
    }

    public boolean checkSelectRegion() {
        for (boolean f : regionPresent) {
            if (f) {
                return true;
            }
        }

        return false;
    }

    public void setFinishSelectRegion(boolean status) {
        finishSelectRegion = status;
    }

    public boolean isFinishSelectRegion() {
        return finishSelectRegion;
    }

    public void setDragRegion() {
        dragRegion = !dragRegion;
    }

    public void setScreenShot(boolean shot) {
        screenShot = shot;
    }

    public void cleanRegions() {
        regionO = regionD = null;
        regionWLayerList.clear();
        regionOList = new Region[4];
        regionDList = new Region[4];
        regionWList = new ArrayList[4];
        wayPointLayer = 1;
    }

    public ArrayList<Region> getRegionWithoutWList() {
        ArrayList<Region> res = new ArrayList<>();
        if (regionO != null) {
            res.add(regionO);
        }
        if (regionD != null) {
            res.add(regionD);
        }
        return res;
    }

    public ArrayList<ArrayList<Region>> getRegionWLayerList() {
        return regionWLayerList;
    }

    public void addWayPoint(Region r) {
        if (regionWLayerList.size() < wayPointLayer) {
            regionWLayerList.add(new ArrayList<>());
        }

        regionWLayerList.get(wayPointLayer - 1).add(r);
        updateRegionList();
    }

    public void updateWLayer() {
        if (wayPointLayer == regionWLayerList.size()) {
            wayPointLayer++;
        }
    }

    public int getWayLayer() {
        return wayPointLayer;
    }

    public ArrayList<Region> getAllRegions() {
        ArrayList<Region> allRegion = new ArrayList<>();
        if (regionO != null) {
            allRegion.addAll(Arrays.asList(regionOList));
        }
        if (regionD != null) {
            allRegion.addAll(Arrays.asList(regionDList));
        }
        if (getRegionWLayerList() != null && getRegionWLayerList().size() > 0) {
            for (ArrayList<ArrayList<Region>> regionWList : regionWList) {
                for (ArrayList<Region> wList : regionWList) {
                    allRegion.addAll(wList);
                }
            }
        }
        return allRegion;
    }

    /**
     * Load trajectory data from file (FULL)
     * Then generate VFGS and RAND
     */
    public void loadTrajData() {
        Trajectory[] trajFull = IOHandle.loadRowData(PSC.ORIGIN_PATH, PSC.LIMIT);
        instance.setTrajFull(trajFull);
        int[] rateCntList = translateRate(trajFull.length, PSC.RATE_LIST);
        instance.setTrajVfgsMtx(calTrajVfgsMatrix(trajFull, rateCntList));
        instance.setTrajRandList(calTrajRandList(trajFull, rateCntList));
    }

    public boolean checkRegion(int index) {
        return regionPresent[index];
    }

    /**
     * This must be called before use {@link #setBlockAt}
     */
    public void initBlockList() {
        blockList = new TrajBlock[5];
        for (int mapIdx = 0; mapIdx < 5; mapIdx++) {
            blockList[mapIdx] = new TrajBlock(mapIdx);
        }
    }

    /**
     * Set the color of all main layers
     */
    public void setAllMainColor(Color color) {
        for (TrajBlock tb : blockList) {
            tb.setMainColor(color);
        }
    }

    /**
     * Set the color of all double select result layers
     */
    public void setAllSltColor(Color color) {
        for (TrajBlock tb : blockList) {
            tb.setSltColor(color);
        }
    }

    public void setBlockAt(int idx, BlockType type, int rateIdx, int deltaIdx) {
        System.out.println("Set block at : idx = " + idx + ", type = " + type
                + ", rateIdx = " + rateIdx + ", deltaIdx = " + deltaIdx);

        Trajectory[] trajList;
        int threadNum;
        switch (type) {
            case NONE:
                trajList = null;
                threadNum = -1;
                break;
            case FULL:
                trajList = this.getTrajFull();
                threadNum = PSC.FULL_THREAD_NUM;
                break;
            case VFGS:
                trajList = this.getTrajVfgsMtx()[deltaIdx][rateIdx];
                threadNum = PSC.SAMPLE_THREAD_NUM;
                break;
            case RAND:
                trajList = this.getTrajRandList()[rateIdx];
                threadNum = PSC.SAMPLE_THREAD_NUM;
                break;
            default:
                // never go here
                throw new IllegalArgumentException("Can't handle t" +
                        "this block type : " + type);
        }

        this.getBlockList()[idx].setNewBlock(type, trajList,
                threadNum, deltaIdx, rateIdx);
    }

    public void setBlockSltAt(int idx, Trajectory[] trajSltList) {
        System.out.println("Set block double select info at : idx = " + idx);
        this.getBlockList()[idx].setTrajSltList(trajSltList);
    }

    public void calTrajSelectResList() {
        SelectManager slm = new SelectManager(getRegionType(), mapList, blockList);
        slm.startRun();
        setFinishSelectRegion(true); // finish select
    }

    private Region[] regionOList = new Region[4];
    private Region[] regionDList = new Region[4];
    private ArrayList<ArrayList<Region>>[] regionWList = new ArrayList[4];

    private void updateRegionList() {
        // TODO add logic for one map
        for (int i = 0; i < 4; i++) {
            if (regionO != null) {
                regionOList[i] = regionO.getCorresRegion(i);
            }
            if (regionD != null) {
                regionDList[i] = regionD.getCorresRegion(i);
            }
            if (regionWLayerList.size() > 0) {
                ArrayList<ArrayList<Region>> regionWLayerListTmp = new ArrayList<>();
                for (ArrayList<Region> wList : regionWLayerList) {
                    ArrayList<Region> tmpWList = new ArrayList<>();
                    for (Region r : wList) {
                        tmpWList.add(r.getCorresRegion(i));
                    }
                    regionWLayerListTmp.add(tmpWList);
                }
                regionWList[i] = regionWLayerListTmp;
            }
        }
    }

    private RegionType getRegionType() {
        if (regionWLayerList.size() > 0) {
            if (regionO == null && regionD == null) {
                return RegionType.WAY_POINT;
            } else {
                return RegionType.O_D_W;
            }
        } else {
            return RegionType.O_D;
        }
    }

    public String getBlockInfo() {
        StringBuilder info = new StringBuilder("Region info:");
        if (regionO == null) {
            info.append("\nOrigin: NONE");
        } else {
            info.append("\nOrigin:\n").append(regionO.toString());
        }
        if (regionD == null) {
            info.append("\n\nDestination: NONE");
        } else {
            info.append("\n\nDestination:\n").append(regionD.toString());
        }
        if (regionWLayerList.size() > 0) {
            info.append("\n\nWay points:\n");
        }
        for (ArrayList<Region> wList : regionWLayerList) {
            for (Region r : wList) {
                info.append("\n").append(r.toString());
            }
        }
        info.append("\nTrajectory info:");
        for (int i = 0; i < 4; i++) {
            TrajBlock bt = blockList[i];
            info.append("\n").append("map").append(i).append(bt.getBlockInfoStr(PSC.DELTA_LIST, PSC.RATE_LIST));
        }
        return info.toString();
    }

    public boolean isPan() {
        int i = 0;
        boolean pan = false;
        for (UnfoldingMap map : mapList) {
            if (!map.getCenter().equals(mapCenter[i])) {
                pan = true;
                mapCenter[i] = map.getCenter();
            }
            i++;
        }
        return pan;
    }

    public Region[] getRegionOList() {
        return regionOList;
    }

    public void setRegionOList(Region[] regionOList) {
        this.regionOList = regionOList;
    }

    public Region[] getRegionDList() {
        return regionDList;
    }

    public void setRegionDList(Region[] regionDList) {
        this.regionDList = regionDList;
    }

    public ArrayList<ArrayList<Region>>[] getRegionWList() {
        return regionWList;
    }

    public void setRegionWList(ArrayList[] regionWList) {
        this.regionWList = regionWList;
    }

    public void updateRegionList(Region region) {
        for (Region r : getAllRegions()) {
            if (r.id == region.id) {
                r.setLeftTopLoc(region.getLeftTopLoc());
                r.setRightBtmLoc(region.getRightBtmLoc());
            }
        }
        regionO = regionOList[0];
        regionD = regionDList[0];
        regionWLayerList = regionWList[0]==null ? new ArrayList<ArrayList<Region>>() : regionWList[0];
    }

    public void dropAllSelectRes() {
        // Only 4 is ok because last trajBlock is
        // a pointer of one of the first four block
        for (int i = 0; i < 4; i++) {
            this.getBlockList()[i].setTrajSltList(null);
        }
    }
}
