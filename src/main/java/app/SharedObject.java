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
import java.util.HashSet;
import java.util.Random;

import static util.Util.translateRate;

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
            allRegion.add(regionO);
        }
        if (regionD != null) {
            allRegion.add(regionD);
        }
        for (ArrayList<Region> wList : regionWLayerList) {
            allRegion.addAll(wList);
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
        instance.setTrajVfgsMtx(getTrajVfgsMatrix(trajFull, rateCntList));     // modified
        instance.setTrajRandList(getTrajRandList(trajFull, rateCntList));
    }

    public boolean checkRegion(int index) {
        return regionPresent[index];
    }

    /**
     * Compute {@code trajVfgsMtx} from {@link PSC#RES_PATH}.
     */
    private Trajectory[][][] getTrajVfgsMatrix(Trajectory[] trajFull, int[] rateCntList) {
        int[] deltaList = PSC.DELTA_LIST;
        double[] rateList = PSC.RATE_LIST;
        int dLen = deltaList.length;
        int rLen = rateList.length;
        Trajectory[][][] trajVfgsMtx = new Trajectory[dLen][rLen][];

        Trajectory[][] vfgsRes = IOHandle.loadVfgsResList(PSC.RES_PATH, trajFull,
                deltaList, rateList);

        // now the results of different rate are in same array.
        // next we split them.

        for (int dIdx = 0; dIdx < dLen; dIdx++) {
            for (int rIdx = 0; rIdx < rLen; rIdx++) {
                int rateCnt = rateCntList[rIdx];
                trajVfgsMtx[dIdx][rIdx] = Arrays.copyOf(vfgsRes[dIdx], rateCnt);
            }
        }

        return trajVfgsMtx;
    }

    /**
     * Compute {@code trajRandList}.
     */
    private Trajectory[][] getTrajRandList(Trajectory[] trajFull, int[] rateCntList) {
        Random rand = new Random(1);
        int dLen = rateCntList.length;
        Trajectory[][] trajRandList = new Trajectory[dLen][];

        for (int dIdx = 0; dIdx < dLen; dIdx++) {
            int rateCnt = rateCntList[dIdx];
            Trajectory[] trajRand = new Trajectory[rateCnt];
            HashSet<Integer> set = new HashSet<>(rateCnt * 4 / 3 + 1);

            int cnt = 0;
            while (cnt < trajRand.length) {
                int idx = rand.nextInt(trajFull.length);
                if (set.contains(idx)) {
                    continue;
                }
                set.add(idx);
                trajRand[cnt++] = trajFull[idx];
            }

            trajRandList[dIdx] = trajRand;
        }

        return trajRandList;
    }

    /**
     * This must be called before use {@link #setBlockAt}
     */
    public void initBlockList() {
        blockList = new TrajBlock[4];
        for (int mapIdx = 0; mapIdx < 4; mapIdx++) {
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

    private RegionType getRegionType() {
        if (regionWLayerList.size() > 0) {
            if (regionO != null) {
                return RegionType.O_D_W;
            } else {
                return RegionType.O_D;
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
}
