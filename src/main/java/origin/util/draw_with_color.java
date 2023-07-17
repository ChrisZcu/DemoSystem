package origin.util;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.providers.MapBox;
import de.fhpotsdam.unfolding.utils.MapUtils;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import origin.model.Trajectory;
import processing.core.PApplet;
import processing.core.PImage;
import util.PSC;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class draw_with_color extends PApplet {

    private static final int ZOOMLEVEL = 11;
    private static final Location PortugalCenter = new Location(22.641, 113.835);
    private static UnfoldingMap map;
    private static List<List<Trajectory>> TrajShow = new ArrayList<>();
    private static HashMap<Integer, Double> colorSet = new HashMap<>();
    final static Integer[] COLORS = {
            15, 91, 120,
            19, 149, 186,
            162, 184, 108,
            235, 200, 68,
            241, 108, 32,
            190, 46, 29
    };
    private static final int[] ZOOMLIST = {11, 12, 13, 14, 15, 16, 17};
    private static final Location[] CENTERLIST = {PSC.cdCenter};
            /*{new Location(41.144, -8.639),new Location(41.093, -8.589), new Location(41.112, -8.525),
                    new Location(41.193, -8.520), new Location(41.23, -8.63), new Location(41.277, -8.281),
                    new Location(41.18114, -8.414669), new Location(41.2037, -8.3045), new Location(41.2765, -8.3762)};*/
    //            {new Location(41.72913, -8.67484), new Location(41.529533, -8.676072), new Location(41.4784759, -8.404744), new Location(41.451811, -8.655799),
//            new Location(41.215459, -7.70277508), new Location(41.14514, -7.996912), new Location(41.10344, -8.181597),
//            new Location(41.4819594, -8.0645941)
//    };
    /*shenzhen*/
//            {/**/new Location(22.641, 113.835), new Location(22.523, 113.929),
//                    new Location(22.691, 113.929), new Location(22.533, 114.014), new Location(22.629, 114.029),
//                    new Location(22.535, 114.060), new Location(22.544, 114.117), new Location(22.717, 114.269)};

    /*chengdu*/
//            {new Location(30.670, 104.063), new Location(30.708, 104.068),new Location(30.691, 104.092),
//                    new Location(30.704, 104.105), new Location(30.699, 104.049), new Location(30.669, 104.105)};


    private static double sampleRate = 0.01;
    private static int offset = 0;


    private static int zoomidx = 0;
    private static int centeridx = 0;
    //  119493 0.05  23898 0.01  11949 0.005  2389 0.001  1194 0.0005 238 0.0001 119 0.00005 23 0.00001
    private static HashMap<Double, Integer> rate2num = new HashMap<Double, Integer>() {
        {
            put(0.01, 4285);
            put(0.005, 2142);
        }
    };
    private static Double[] segmentPoint = {0.0, 0.0, 0.0, 0.0, 0.0};


    private static double getMeans(ArrayList<Double> arr) {
        double sum = 0.0;
        for (double val : arr) {
            sum += val;
        }
        return sum / arr.size();
    }

    private static double getStandadDiviation(ArrayList<Double> arr) {
        int len = arr.size();
        double avg = getMeans(arr);
        double dVar = 0.0;
        for (double val : arr) {
            dVar += (val - avg) * (val - avg);
        }
        return Math.sqrt(dVar / len);
    }

    Location[] chenDuCenter = new Location[]{PSC.cdCenter,
            new Location(30.670, 104.063), new Location(30.708, 104.068), new Location(30.691, 104.092),
            new Location(30.704, 104.105), new Location(30.699, 104.049), new Location(30.669, 104.105),
            new Location(30.569, 103.958), new Location(30.669, 104.163), new Location(30.698, 104.167),
            new Location(30.676, 103.999)
    };

    private static String vfgsDir = "E:\\zcz\\dbgroup\\VQGS\\tot_global_result_1010" +
            "\\tot_global_result_1010\\result_1010\\chengdu\\vfgs\\";
    int currentZoomLevel = 13;
    int centerIndex =0;
    static int delta = 5;
    static int[] deltas = {0, 4, 8, 16, 32, 64};

    private static void initColorSet() throws IOException {
        delta = deltas[delta];
        int num = rate2num.get(sampleRate);
        ArrayList<Double> valueArr = new ArrayList<>();
        String path = "data/cd_20230703/color_merge/color_0.01_" + delta + ".txt";
        System.out.println(path);
        BufferedReader reader = new BufferedReader(new FileReader(path));
        String line;
        while (true) {
            line = reader.readLine();
            if (line == null) break;
            try {
                String[] item = line.split(",");
                int trajId = Integer.valueOf(item[0]);
                double value = Double.valueOf(item[1]);
                valueArr.add(value);
                colorSet.put(trajId, value);
            } catch (NullPointerException e) {
                System.out.println("......." + line);
            }
        }

        path = String.format(vfgsDir + "cd_vfgs_%d.txt", delta);
        reader = new BufferedReader(new FileReader(path));
        HashSet<Integer> trajSet = new HashSet<>();
        while (true) {
            line = reader.readLine();
            if (line == null) break;
            try {
                String[] item = line.split(",");
                int trajId = Integer.parseInt(item[0]);
                trajSet.add(trajId);
            } catch (NullPointerException e) {
                System.out.println("......." + line);
            }
        }
        Collections.sort(valueArr);
        double minVal = valueArr.get(0);
        for (int trajId : trajSet) {
            if (!colorSet.containsKey(trajId)) {
                colorSet.put(trajId, minVal);
                valueArr.add(minVal);
            }
        }
        Collections.sort(valueArr);
        double outlier = getMeans(valueArr) + 3 * getStandadDiviation(valueArr);
        int i;
        for (i = 0; i < valueArr.size(); i++) {
            if (valueArr.get(i) > outlier) {
                break;
            }
        }
        i -= 1;
        i /= 5;
        segmentPoint[0] = valueArr.get(i);
        segmentPoint[1] = valueArr.get(i * 2);
        segmentPoint[2] = valueArr.get(i * 3);
        segmentPoint[3] = valueArr.get(i * 4);
        segmentPoint[4] = valueArr.get(i * 5);
    }

    private static void preProcess() {
        for (int i = 0; i < 6; i++) {
            TrajShow.add(new ArrayList<>());
        }
        String dataPath = PSC.cdPath;
        int trajId = 0;
        try {
            File theFile = new File(dataPath);
            LineIterator it = FileUtils.lineIterator(theFile, "UTF-8");
            String line;
            String[] data;
            try {
                while (it.hasNext()) {
                    line = it.nextLine();
                    if (colorSet.containsKey(trajId)) {
                        data = line.split(";")[1].split(",");
                        Trajectory traj = new Trajectory(trajId);
                        for (int i = 0; i < data.length - 2; i = i + 2) {
                            Location point = new Location(Double.parseDouble(data[i + 1]), Double.parseDouble(data[i]));
                            traj.points.add(point);
                        }
                        double color = colorSet.get(traj.getTrajId());
                        if (color <= segmentPoint[0]) {
                            TrajShow.get(0).add(traj);
                        } else if (color <= segmentPoint[1]) {
                            TrajShow.get(1).add(traj);
                        } else if (color <= segmentPoint[2]) {
                            TrajShow.get(2).add(traj);
                        } else if (color <= segmentPoint[3]) {
                            TrajShow.get(3).add(traj);
                        } else if (color <= segmentPoint[4]) {
                            TrajShow.get(4).add(traj);
                        } else {
                            TrajShow.get(5).add(traj);
                        }
                    }
                    trajId++;
                }
            } finally {
                LineIterator.closeQuietly(it);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void settings() {
        size(1200, 800, P2D);
        smooth();
    }

    public void setup() {
        String whiteMapPath = PSC.WHITE_MAP_PATH;
        map = new UnfoldingMap(this, "draw_with_color2", new MapBox.CustomMapBoxProvider(whiteMapPath));
        MapUtils.createDefaultEventDispatcher(this, map);
        map.setZoomRange(1, 20);
        map.zoomAndPanTo(currentZoomLevel, chenDuCenter[centerIndex]);
        System.out.println("SET UP DONE");
    }

    private int done = 0;
    private boolean isDataLoadDone = false;
    private PImage mapImage = null;
    private int zoomCheck = -1;
    private Location centerCheck = new Location(-1, -1);

    public void draw() { // 有地图
        map.draw();
        if (!(zoomCheck == map.getZoomLevel() && centerCheck.equals(map.getCenter()))) {
            map.draw();
            if (!map.allTilesLoaded()) {
                if (mapImage == null) {
                    mapImage = map.mapDisplay.getInnerPG().get();
                }
                image(mapImage, 0, 0);
            } else {
                zoomCheck = map.getZoomLevel();
                centerCheck = map.getCenter();
                System.out.println("load map done");
                map.draw();
                map.draw();
                map.draw();
                map.draw();
            }
        } else {
            if (!map.allTilesLoaded()) {
                if (mapImage == null) {
                    mapImage = map.mapDisplay.getInnerPG().get();
                }
                image(mapImage, 0, 0);
            } else {
                noFill();
                strokeWeight(1);
                for (int i = 0; i < 6; i++) {
                    stroke(COLORS[3 * i], COLORS[3 * i + 1], COLORS[3 * i + 2]);    // set color here
                    for (Trajectory traj : TrajShow.get(i)) {
                        beginShape();
                        for (Location loc : traj.points) {
                            ScreenPosition pos = map.getScreenPosition(loc);
                            vertex(pos.x, pos.y);
                        }
                        endShape();
                    }
                }
                int sum = 0;
                for (List<Trajectory> trajList : TrajShow) {
                    sum += trajList.size();
                }
                System.out.println("total draw: " + sum);
                System.out.println(framePath);
                saveFrame(framePath);
                exit();
                noLoop();
            }
        }
    }
//    int[] deltas = {0, 4, 8, 16, 32, 64};

    String framePath = "data/picture/revision/chengdu/case/" + currentZoomLevel + "-" + centerIndex + "/vqgs_color_delta_" + delta + ".png";


    public static void main(String[] args) {

        try {
            initColorSet();
            preProcess();
            System.out.println("init done");
        } catch (IOException e) {
            e.printStackTrace();
        }

        PApplet.main(new String[]{draw_with_color.class.getName()});
    }

}

