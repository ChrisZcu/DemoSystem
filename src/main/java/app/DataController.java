package app;

import draw.TrajDrawManager;
import model.BlockType;
import model.SharedObject;
import model.Trajectory;
import util.IOHandle;
import util.PSC;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

import static util.Util.translateRate;

public class DataController {
    public TrajDrawManager trajDrawManager;
    private final SharedObject sharedObject;

    public DataController() {
        sharedObject = SharedObject.getInstance();
    }

    /**
     * Load trajectory data from file (FULL)
     * Then generate VFGS and RAND
     */
    public void load() {
        Trajectory[] trajFull = IOHandle.loadRowData(PSC.ORIGIN_PATH, PSC.LIMIT);
        sharedObject.setTrajFull(trajFull);
        int[] rateCntList = translateRate(trajFull.length, PSC.RATE_LIST);
        sharedObject.setTrajVfgsMtx(getTrajVfgsMatrix(trajFull, rateCntList));     // modified
        sharedObject.setTrajRandList(getTrajRandList(trajFull, rateCntList));
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
        Random rand = new Random();
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

    public void setBlockAt(int idx, BlockType type, int deltaIdx, int rateIdx) {
        Trajectory[] trajList;
        int threadNum;
        switch (type) {
            case FULL:
                trajList = sharedObject.getTrajFull();
                threadNum = PSC.FULL_THREAD_NUM;
                break;
            case VFGS:
                trajList = sharedObject.getTrajVfgsMtx()[deltaIdx][rateIdx];
                threadNum = PSC.SAMPLE_THREAD_NUM;
                break;
            case RAND:
                trajList = sharedObject.getTrajRandList()[rateIdx];
                threadNum = PSC.SAMPLE_THREAD_NUM;
                break;
            default:
                // never go here
                throw new IllegalArgumentException("Can't handle t" +
                        "his block type : " + type);
        }

        sharedObject.getBlockList()[idx].setNewBlock(type, trajList,
                threadNum, deltaIdx, rateIdx);
    }
}
