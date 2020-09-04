package app;

import de.fhpotsdam.unfolding.UnfoldingMap;
import model.*;
import select.SelectManager;
import draw.TrajDrawManager;
import model.BlockType;
import model.Region;
import model.TrajBlock;
import model.Trajectory;
import util.IOHandle;
import util.PSC;

import java.lang.reflect.Array;
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

    // regions
    private static Region regionO = null;
    private static Region regionD = null;
    private static ArrayList<ArrayList<Region>> regionWLayerList = new ArrayList<>();
    private int wayPointLayer = 1;

    private static boolean[] regionPresent = new boolean[3];// indicate the current region draw.

    // map & app
    private static UnfoldingMap[] mapList;

    private boolean finishSelectRegion;
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
        for (boolean f : regionPresent)
            if (f)
                return true;

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
        if (regionO != null)
            res.add(regionO);
        if (regionD != null)
            res.add(regionD);
        return res;
    }

    public ArrayList<ArrayList<Region>> getRegionWLayerList() {
        return regionWLayerList;
    }

    public void addWayPoint(Region r) {
        if (regionWLayerList.size() < wayPointLayer)
            regionWLayerList.add(new ArrayList<Region>());

        regionWLayerList.get(wayPointLayer - 1).add(r);
    }

    public void updateWLayer() {
        if (wayPointLayer == regionWLayerList.size())
            wayPointLayer++;
    }

    public int getWayLayer() {
        return wayPointLayer;
    }

    public ArrayList<Region> getAllRegions() {
        ArrayList<Region> allRegion = new ArrayList<>();
        if (regionO != null)
            allRegion.add(regionO);
        if (regionD != null)
            allRegion.add(regionD);
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
        for (int i = 0; i < 4; i++) {
            blockList[i] = new TrajBlock();
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
                trajList = instance.getTrajFull();
                threadNum = PSC.FULL_THREAD_NUM;
                break;
            case VFGS:
                trajList = instance.getTrajVfgsMtx()[deltaIdx][rateIdx];
                threadNum = PSC.SAMPLE_THREAD_NUM;
                break;
            case RAND:
                trajList = instance.getTrajRandList()[rateIdx];
                threadNum = PSC.SAMPLE_THREAD_NUM;
                break;
            default:
                // never go here
                throw new IllegalArgumentException("Can't handle t" +
                        "this block type : " + type);
        }

        instance.getBlockList()[idx].setNewBlock(type, trajList,
                threadNum, deltaIdx, rateIdx);
    }

    public void calTrajSelectResList() {
        SelectManager slm = new SelectManager(getRegionType(), mapList, blockList);
        slm.startRun();
        setFinishSelectRegion(true); // finish select
    }

    private RegionType getRegionType() {
        if (regionWLayerList.size() > 0) {
            if (regionO != null)
                return RegionType.O_D_W;
            else return RegionType.O_D;

        } else return RegionType.O_D;
    }
}
