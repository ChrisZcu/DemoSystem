package app;

import de.fhpotsdam.unfolding.UnfoldingMap;
import model.BlockType;
import model.Region;
import model.TrajBlock;
import model.Trajectory;
import util.IOHandle;
import util.PSC;

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

    private static TrajBlock[] blockList;

    // regions
    private static Region regionO = null;
    private static Region regionD = null;
    private static ArrayList<ArrayList<Region>> regionWLayerList;

    private static boolean[] regionPresent = new boolean[3];// indicate the current region draw.

    // map & app
    private static DemoInterface app;
    private static UnfoldingMap map;

    private boolean finishSelectRegion;
    private boolean screenShot;

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

    public TrajBlock[] getBlockList() {
        return blockList;
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

    public void setApp(DemoInterface app) {
        SharedObject.app = app;
    }

    public void setMap(UnfoldingMap map) {
        SharedObject.map = map;
    }

    public UnfoldingMap getMap() {
        return map;
    }

    public void eraseRegionPren() {
        Arrays.fill(regionPresent, false);
    }

    public void updateRegionPreList(int regionId) {
        eraseRegionPren();
        regionPresent[regionId] = true;
    }

    public void setFinishSelectRegion(boolean status) {
        finishSelectRegion = status;
    }

    public void setScreenShot(boolean shot) {
        screenShot = shot;
    }

    public void cleanRegions() {
        regionO = regionD = null;
        regionWLayerList.clear();
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
}
