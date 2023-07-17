package util;

import model.Position;
import model.Trajectory;
import model.TrajectoryMeta;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

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

    public static int qualityScore(TrajectoryMeta[] trajs) {
        HashSet<Position> totalScoreSet = new HashSet<>(trajs.length);
        for (TrajectoryMeta traj : trajs) {
            totalScoreSet.addAll(Arrays.asList(traj.getPositions()));
        }
        return totalScoreSet.size();
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

    public static void storeIds(String filePath, int[] indexes) {
        StringBuilder sb = new StringBuilder();
        for (int idx : indexes) {
            sb.append(idx).append(",0").append("\n");
        }
        try (BufferedOutputStream writer = new BufferedOutputStream(Files.newOutputStream(Paths.get(filePath)))
        ) {
            writer.write(sb.toString().getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void storeInitVAS(String filePath, int[] indexes, double[] scores) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indexes.length; ++i) {
            sb.append(indexes[i]).append(",").append(scores[i]).append("\n");
        }
        try (BufferedOutputStream writer = new BufferedOutputStream(Files.newOutputStream(Paths.get(filePath)))
        ) {
            writer.write(sb.toString().getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void loadInitVAS(String filePath, int[] trajIds, double[] scores) {
        ArrayList<String> res = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath));) {
            String line;
            while ((line = reader.readLine()) != null) {
                res.add(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int i = 0;
        for (String item : res) {
            String[] tmp = item.split(",");
            int idx = Integer.parseInt(tmp[0]);
            double score = Double.parseDouble(tmp[1]);
            trajIds[i] = idx;
            scores[i++] = score;
        }
    }

    public static void storeIdxs(String filePath, ArrayList<Integer> idxs) {
        StringBuilder sb = new StringBuilder();
        for (Integer id : idxs) {
            sb.append(id).append(",").append("\n");
        }
        try (BufferedOutputStream writer = new BufferedOutputStream(Files.newOutputStream(Paths.get(filePath)))
        ) {
            writer.write(sb.toString().getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void storeIdxWithScore(String filePath, HashMap<Integer, Double> idxToScore){
        StringBuilder sb = new StringBuilder();
        idxToScore.forEach((id, dis) ->{
            sb.append(id).append(",").append(dis).append("\n");
        });
        try (BufferedOutputStream writer = new BufferedOutputStream(Files.newOutputStream(Paths.get(filePath)))
        ) {
            writer.write(sb.toString().getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ArrayList<Integer> loadIdxsFromFile(String filePath) {
        ArrayList<String> res = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                res.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        ArrayList<Integer> idxs = new ArrayList<>();
        for (String item : res) {
            idxs.add(Integer.parseInt(item.split(",")[0]));
        }
        return idxs;
    }

    public static void storeSparest(String filePath, ArrayList<int[]> scores, double ratio) {
        StringBuilder sb = new StringBuilder();
        int num = (int) (scores.size() * ratio);
        System.out.println(scores.size() + ", " + num);
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
