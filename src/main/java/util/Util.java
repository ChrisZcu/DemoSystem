package util;

import model.Trajectory;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

public class Util {
    /**
     * Translate simple rate to real traj count.
     */
    public static int[] translateRate(int trajNum, double[] rateList) {
        int len = rateList.length;
        int[] ret = new int[len];
        for (int i = 0; i < len; i++) {
            ret[i] = (int) Math.round(trajNum * rateList[i]);
        }
        return ret;
    }

    /**
     * Compute {@code trajVfgsMtx} from {@link PSC#RES_PATH}.
     */
    public static Trajectory[][][] calTrajVfgsMatrix(Trajectory[] trajFull, int[] rateCntList) {
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
    public static Trajectory[][] calTrajRandList(Trajectory[] trajFull, int[] rateCntList) {
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

    public static void storeVQGSRes(String filePath, Trajectory[] trajList) {
        StringBuilder sb = new StringBuilder();
        for (Trajectory t : trajList) {
            int tId = t.getTrajId();
            sb.append(tId).append(",0").append("\n");
        }
        try (BufferedOutputStream writer = new BufferedOutputStream(Files.newOutputStream(Paths.get(filePath)))
        ) {
            writer.write(sb.toString().getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void storeSparest(String filePath, ArrayList<int[]>scores) {
        StringBuilder sb = new StringBuilder();
        int num = (int)scores.size() / 2;
        System.out.println(scores.size()+ ", " +  num);
        for (int[] t : scores) {
            sb.append(t[1]).append(",").append(t[0]).append("\n");
            num -= 1;
            if (num == 0)
                break;
        }
        try (BufferedOutputStream writer = new BufferedOutputStream(Files.newOutputStream(Paths.get(filePath)))
        ) {
            writer.write(sb.toString().getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        StringBuilder sb = new StringBuilder();
        sb.append(1).append(",0").append("\n");
        sb.append(2).append(",0").append("\n");
        sb.append(3).append(",0").append("\n");
        sb.append(4).append(",0").append("\n");
        System.out.println(sb);
    }
}
