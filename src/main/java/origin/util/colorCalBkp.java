package origin.util;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.providers.MapBox;
import de.fhpotsdam.unfolding.utils.MapUtils;
import origin.model.Position;
import origin.model.Trajectory;
import processing.core.PApplet;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;


public class colorCalBkp extends PApplet {
    private static int[] DELTALIST = {64, 32, 16, 8, 4, 0};/*{0, 4, 8, 16, 32, 64}*/
    private static final double[] RATELIST = {/*0.05,*/ 0.01, 0.005, 0.001, 0.0005, 0.0001/*, 0.00005, 0.00001*/};
    private static List<HashSet<Integer>[]> deltaCellList = new ArrayList<HashSet<Integer>[]>();
    private static List<Trajectory> TrajTotal; //所有traj 等同于arr

    private static String dataPath = "E:\\zcz\\dbgroup\\VQGS\\DTW\\data\\sz_cd\\cd_new_score.txt";

    private static void preProcess() throws IOException {

        // !!!
//        String dataPath = "data/porto_test/__score_set.txt"; //"data/portScore.txt";

        TrajTotal = new ArrayList<>();

        UTIL.totalListInitV2(TrajTotal, dataPath);
        for (Integer e : DELTALIST) {
            HashSet[] cellList = new HashSet[RATELIST.length];
            for (int i = 0; i < RATELIST.length; i++) {
                cellList[i] = new HashSet<Integer>();
            }
            readFile(e, cellList);
            deltaCellList.add(cellList);
        }
        System.out.println("processing done ");
    }

    private static int[] szDaySizeList = {21425, 4285, 2142, 428, 214, 42, 21, 4};
    private static int[] cdPartList = {24000, 12000, 2400, 1200, 240};
    private static int[] szPartList = {(int) (3066862 * 0.01), (int) (3066862 * 0.005), (int) (3066862 * 0.001), (int) (3066862 * 0.0005), (int) (3066862 * 0.0001)};
    private static String vfgsDir = "E:\\zcz\\dbgroup\\VQGS\\tot_global_result_1010" +
            "\\tot_global_result_1010\\result_1010\\chengdu\\vfgs\\";

    private static void readFile(int delta, HashSet<Integer>[] cellList) throws IOException {
        int[] sizeList = cdPartList;
        for (int j = 0; j < sizeList.length; j++) {
            // !!!
//            String filePath = String.format("data" +
//                    "/porto_test/vfgs_%d.txt", delta);
            String filePath = String.format(vfgsDir + "cd_vfgs_%d.txt", delta);
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            for (int i = 0; i < sizeList[j]; i++) {
                String line = reader.readLine();
                String[] item = line.split(",");
                cellList[j].add(Integer.parseInt(item[0]));
            }
            reader.close();
        }
    }

    @Override
    public void settings() {
        size(1200, 800, P2D);
    }

    @Override
    public void setup() {

        map = new UnfoldingMap(this);

        String mapStyle = whiteMapPath;

        map = new UnfoldingMap(this, "VIS_Demo", new MapBox.CustomMapBoxProvider(mapStyle));

//        map.zoomAndPanTo(20, new Location(41.101, -8.58));
        map.zoomAndPanTo(20, new Location(30.658524, 104.065747));

        for (Trajectory trajectory : TrajTotal) {
            trajectory.unitPositions(map);
        }
        influCal();

        map.setZoomRange(1, 20);
        MapUtils.createDefaultEventDispatcher(this, map);

        System.out.println("SET UP DONE");
        exit();

    }

    private void influCal() {
        for (int i = 0; i < DELTALIST.length; i++) {
            System.out.println("begin cal delta: " + DELTALIST[i]);
            HashSet<Integer>[] cellList = deltaCellList.get(i);
            HashSet<Integer> influList = cellList[0];// 最大的集合，做初始化用，以此减小，进行判断减少计算
            HashMap<Integer, HashSet<Position>> id2poi = new HashMap<>();
            for (Integer e : influList) {//处理选中的轨迹的position集合
                Trajectory traj = TrajTotal.get(e);
                //                for (Location p : traj.getPoints()) {
//                    double px = map.getScreenPosition(p).x;
//                    double py = map.getScreenPosition(p).y;
//                    trajSet.add(new Position(px, py));
//                }
                HashSet<Position> trajSet = new HashSet<>(traj.getPositions());
                id2poi.put(e, trajSet);
            }
            HashMap<Integer, Integer> maxSetIR = initMaxSetIR(id2poi, influList);

            HashMap<Integer, Integer> influId2Score = new HashMap<>();
            for (Integer e : maxSetIR.keySet()) {
                int influId = maxSetIR.get(e);  // id -> influId
                int score = influId2Score.getOrDefault(influId, 0);
                influId2Score.put(influId, score + 1);
            }
            write2file(influId2Score, DELTALIST[i], RATELIST[0]);


            for (int j = 1; j < cellList.length; j++) {//计算其他集合下
                System.out.println("begin other list" + RATELIST[j]);
                influList = cellList[j];
                HashMap<Integer, HashSet<Position>> id2poiSub = new HashMap<>();
                for (Integer e : influList) {
                    id2poiSub.put(e, id2poi.get(e));
                }
                influId2Score.clear();
                HashMap<Integer, Integer> setIR = new HashMap<>();
                for (int k = 0; k < TrajTotal.size(); k++) {
                    if (influList.contains(k)) {
                        continue;
                    }
                    if (maxSetIR.containsKey(k) && influList.contains(maxSetIR.get(k))) {
                        setIR.put(k, maxSetIR.get(k));
                        continue;
                    }
                    Trajectory traj = TrajTotal.get(k);
                    int influId = getMaxInfluId(traj, id2poiSub);
                    setIR.put(k, influId);
                }
                for (Integer e : setIR.keySet()) {
                    int influId = setIR.get(e);
                    int score = influId2Score.getOrDefault(influId, 0);
                    influId2Score.put(influId, score + 1);
                }
                write2file(influId2Score, DELTALIST[i], RATELIST[j]);
            }
        }
    }

    /**
     * Calculate the map from traj id (not include traj in R)
     * to another traj id that has most common pixels with it.
     *
     * @param id2poi    the map from traj to its position set
     * @param influList R, i.e. the list of traj that need to compute color score
     */
    private HashMap<Integer, Integer> initMaxSetIR(HashMap<Integer, HashSet<Position>> id2poi, HashSet<Integer> influList) {
        HashMap<Integer, Integer> id2influId = new HashMap<>();
        int num = TrajTotal.size();
        for (int i = 0; i < TrajTotal.size(); i++) {
            if (influList.contains(i)) {
                continue;
            }
            Trajectory traj = TrajTotal.get(i);
            int influId = getMaxInfluId(traj, id2poi);
            if (i % 1000 == 0)
                System.out.println("calculating influence " + i + ", total " + num);
            id2influId.put(i, influId);
        }
        return id2influId;
    }

    private int getMaxInfluId(Trajectory traj, HashMap<Integer, HashSet<Position>> id2poi) {
//        HashSet<Position> trajSet = new HashSet<>();
//        for (Location p : traj.getPoints()) { // 计算未选中轨迹对应的position
//            double px = map.getScreenPosition(p).x;
//            double py = map.getScreenPosition(p).y;
//            trajSet.add(new Position(px, py));
//        }
        HashSet<Position> trajSet = new HashSet<>(traj.getPositions());

        int influId = -1;
        double influScore = -1;
        for (Integer e : id2poi.keySet()) {
            double score = 0;
            HashSet<Position> poi = id2poi.get(e);
            for (Position p : poi) {
                if (trajSet.contains(p)) {
                    score += 1;
                }
            }
            if (score > influScore) {
                influId = e;
                influScore = score;
            }
        }
        return influId;
    }

    private static String outDir = "data/cd_20230703/";

    private void write2file(HashMap<Integer, Integer> influId2Score, int delta, double rate) {
        // !!!
        String filePath = String.format(outDir + "color_%s_%d.txt", rate, delta);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
            for (Integer e : influId2Score.keySet()) {
                writer.write(e + "," + influId2Score.get(e));
                writer.newLine();
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //    private String whiteMapPath = "https://api.mapbox.com/styles/v1/pacemaker-yc/ck4gqnid305z61cp1dtvmqh5y/tiles/512/{z}/{x}/{y}@2x?access_token=pk.eyJ1IjoicGFjZW1ha2VyLXljIiwiYSI6ImNrNGdxazl1aTBsNDAzZW41MDhldmQyancifQ.WPAckWszPCEHWlyNmJfY0A";
    public static final String whiteMapPath = "https://api.mapbox.com/styles/v1/chriszcu/ck8ieh81a0o0i1il0x83wi4at/tiles/512/{z}/{x}/{y}@2x?" +
            "access_token=pk.eyJ1IjoiY2hyaXN6Y3UiLCJhIjoiY2s3YTgyamh3MTE3dzNmcWtiZ2E5eW16eiJ9.bjxrNwbXNPIz6D_tuUQKMA";

    private static UnfoldingMap map;


    public static void main(String[] args) {
        if (args.length > 0) {
            dataPath = args[0];
            vfgsDir = args[1];
            outDir = args[2];
            int delta = Integer.parseInt(args[3]);
            DELTALIST = new int[]{delta};
        }
        try {
            preProcess();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String title = "origin.util.colorCalBkp";
        PApplet.main(new String[]{title});
    }
}
