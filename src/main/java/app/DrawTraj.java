package app;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.providers.MapBox;
import de.fhpotsdam.unfolding.utils.MapUtils;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import index.QuadTree;
import model.Position;
import model.TrajectoryMeta;
import origin.model.Trajectory;
import processing.core.PApplet;
import processing.core.PImage;
import processing.event.MouseEvent;
import processing.opengl.PJOGL;
import util.DistanceFunc;
import util.PSC;
import util.Util;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Array;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

public class DrawTraj extends PApplet {

    private UnfoldingMap map;
    private static TrajectoryMeta[] trajMetaFull;
    private static TrajectoryMeta[] trajShow;
    private static TrajectoryMeta[] trajFull;

    private String filePath = PSC.cdPath;

    private boolean isDataLoadDone = false;
    private PImage mapImage = null;
    private int zoomCheck = -1;
    private Location centerCheck = new Location(-1, -1);


    public void settings() {
        size(1200, 800, P2D);
    }

    private HashMap<Integer, ArrayList<Integer>> deltaToIdxs(String city) {
        int[] deltas = {0, 4, 16, 32, 64};
        HashMap<Integer, ArrayList<Integer>> res = new HashMap<>();
        for (int d : deltas) {
            String path = String.format("E:\\zcz\\dbgroup\\VQGS\\tot_global_result_1010\\tot_global_result_1010\\result_1010\\%s\\vfgs\\cd_vfgs_%d.txt", city, d);
            ArrayList<String> nums = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(path));
            ) {
                String line;
                while ((line = reader.readLine()) != null) {
                    nums.add(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            ArrayList<Integer> idxs = new ArrayList<>();
            for (String items : nums) {
                idxs.add(Integer.parseInt(items.split(",")[0]));
            }
            res.put(d, idxs);
        }
        return res;
    }


    private ArrayList<Integer> loadDTWRes(String filePath) {
        ArrayList<Integer> res = new ArrayList<>();
        ArrayList<String> metaTmp = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = reader.readLine()) != null) {
                metaTmp.add(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Map<Integer, Double> id2Score = new HashMap<>();

        for (String e : metaTmp) {
            int id = Integer.parseInt(e.split(",")[0]);
            double score = Double.parseDouble(e.split(",")[1]);
            id2Score.put(id, score);
        }

        Comparator<Map.Entry<Integer, Double>> valueComparator = new Comparator<Map.Entry<Integer, Double>>() {
            @Override
            public int compare(Map.Entry<Integer, Double> o1,
                               Map.Entry<Integer, Double> o2) {
                return ((o1.getValue() - o2.getValue()) > 0 ? -1 : (o1.getValue() - o2.getValue()) == 0 ? 0 : 1);
            }
        };
        List<Map.Entry<Integer, Double>> list = new ArrayList<Map.Entry<Integer, Double>>(id2Score.entrySet());
        Collections.sort(list, valueComparator);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, Double> entry : list) {
            res.add(entry.getKey());
            sb.append(entry.getKey()).append(",").append(entry.getValue()).append("\n");
        }
        try (BufferedOutputStream writer = new BufferedOutputStream(Files.newOutputStream(Paths.get("data/baseline/tmp")))
        ) {
            writer.write(sb.toString().getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return res;
    }

    private static double[] loadDTWResPairs(String filePath) {
        ArrayList<Integer> res = new ArrayList<>();
        ArrayList<String> metaTmp = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = reader.readLine()) != null) {
                metaTmp.add(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        double[] scores = new double[metaTmp.size()];
        double maxScore = -1;
        for (String e : metaTmp) {
            int id = Integer.parseInt(e.split(",")[0]);
            double score = Double.parseDouble(e.split(",")[1]);
            maxScore = Math.max(maxScore, score);
            scores[id] = score;
        }
        for (int i = 0; i < metaTmp.size(); ++i) {
            scores[i] /= maxScore;
        }
        double sumScore = Arrays.stream(scores).sum();
        System.out.println(sumScore);
        return scores;
    }


    private ArrayList<Integer> scoreOrdered(TrajectoryMeta[] trajFull) {
        ArrayList<Integer> res = new ArrayList<>();
        Map<Integer, Double> id2Score = new HashMap<>();

        for (TrajectoryMeta e : trajFull) {
            int id = e.getTrajId();
            double score = e.getScore();
            id2Score.put(id, score);
        }
        Comparator<Map.Entry<Integer, Double>> valueComparator = new Comparator<Map.Entry<Integer, Double>>() {
            @Override
            public int compare(Map.Entry<Integer, Double> o1,
                               Map.Entry<Integer, Double> o2) {
                return ((o1.getValue() - o2.getValue()) > 0 ? -1 : (o1.getValue() - o2.getValue()) == 0 ? 0 : 1);
            }
        };
        List<Map.Entry<Integer, Double>> list = new ArrayList<Map.Entry<Integer, Double>>(id2Score.entrySet());
        Collections.sort(list, valueComparator);
        int i = 0;
        for (Map.Entry<Integer, Double> entry : list) {
            if (i < 10)
                System.out.println(entry.getKey() + ", " + entry.getValue());
            i += 1;
            res.add(entry.getKey());
        }
        return res;
    }

    HashMap<Integer, ArrayList<Integer>> deltaToIndexes = new HashMap<>();

    Location[] chenDuCenter = new Location[]{PSC.cdCenter,
            new Location(30.670, 104.063), new Location(30.708, 104.068), new Location(30.691, 104.092),
            new Location(30.704, 104.105), new Location(30.699, 104.049), new Location(30.669, 104.105),
            new Location(30.569, 103.958), new Location(30.669, 104.163), new Location(30.698, 104.167),
            new Location(30.676, 103.999)
    };

    public void setup() {
        map = new UnfoldingMap(this, new MapBox.CustomMapBoxProvider(PSC.WHITE_MAP_PATH));
        map.setZoomRange(0, 20);
        map.setBackgroundColor(255);
//        new Location(41.17129, -8.559485))
//        map.zoomAndPanTo(12, PSC.cdCenter);
        map.zoomAndPanTo(currentZoomLevel,chenDuCenter[centerIndex]);
        MapUtils.createDefaultEventDispatcher(this, map);
//        String targetDir = "data/picture/revision/chengdu/case/" + currentZoomLevel + "-" + centerIndex;
//        File directory = new File(targetDir);
//        directory.mkdir();

        new Thread(() -> {
            try {
//                Porto: 1194931
//                trajShow = new TrajectoryMeta[number];


//                String sPath = "data/vfgs/tkde_revision/reviewer1/half_sparest_0.01.txt";
//                framePath = "data/picture/revision/longest_0.01.png";

//                String sPathDesc = "data/vfgs/tkde_revision/reviewer1/half_sparest_desc.txt";
//                framePath = "data/picture/revision/remove_sparest.png";

//                String vfgsPath = "data/vfgs/tkde_revision/reviewer1/half_sampling_16.txt";
//                String vfgsPath = "data/GPS/vfgs_0.txt";
//                String vfgsPath = "data/baseline/porto/ratio0.01/ConMerge/VASRes";
//                framePath = "data/picture/revision/VAS4_0.01.png";
//
//                framePath = "data/picture/revision/full.png";


//                int cnt = 23894;
//                Random ran = new Random(1);
//                HashSet<Integer> idxsSet = new HashSet<Integer>(cnt);
//                while (idxsSet.size() != cnt) {
//                    idxsSet.add(ran.nextInt(2389482 - 1));
//                }
//                ArrayList<Integer> idxs = new ArrayList<>(idxsSet);
//                trajShow = QuadTree.loadData(new double[4], filePath, idxs);

                trajFull = QuadTree.loadData(new double[4], filePath);
//                calAndStoreTrajDis();
                trajShow = trajFull;
//                updateRandom();
                loadDelta();

//                ArrayList<Integer> distanceIdx = loadDTWRes("data/vfgs/tkde_revision/porto_traj_dis.txt");
//                int size = (int) (trajFull.length * 0.005);
//                trajShow = new TrajectoryMeta[size];
//                for (int i = 0; i < size; ++i) {
//                    trajShow[i] = trajFull[distanceIdx.get(i)];
//                }
//                framePath = "data/picture/revision/porto/rate0.005/traj_distance_longest.png";

//                Random ran = new Random(1);
//                HashSet<Integer> idxsSet = new HashSet<Integer>(size);
//                while (idxsSet.size() != size) {
//                    idxsSet.add(ran.nextInt(trajFull.length - 1));
//                }
//                trajShow = new TrajectoryMeta[size];
//                int i = 0;
//                for (int idx : idxsSet) {
//                    trajShow[i++] = trajFull[idx];
//                }
//                framePath = "data/picture/revision/porto/rate0.005/random.png";

//                deltaIndex = 5;
//                trajShow = new TrajectoryMeta[size];
//                int i = 0;
//                for (int idx : deltaToIndexes.get(deltas[deltaIndex])) {
//                    trajShow[i++] = trajFull[idx];
//                }
//                framePath = "data/picture/revision/porto/rate0.005/vqgs_delta_" + deltas[deltaIndex] + ".png";

//                trajShow = trajFull;
//                TrajectoryMeta[] trajFull = QuadTree.loadData(new double[4], filePath);
//                System.out.println(util.Util.qualityScore(trajShow) * 1.0 / util.Util.qualityScore(trajFull));

//                TrajectoryMeta[] trajFull = QuadTree.loadData(new double[4], filePath);
//                ArrayList<Integer> ordered = scoreOrdered(trajFull);
//                int sampleSize = (int) (trajFull.length * 0.001);
//                trajShow = new TrajectoryMeta[sampleSize];
//                for (int i =0; i < sampleSize; ++i){
//                    trajShow[i] = trajFull[ordered.get(i)];
//                }

//                deltaToIdx = deltaToIdxs("chengdu");
//                trajFull = QuadTree.loadData(new double[4], filePath);
//                trajShow = new TrajectoryMeta[(int) (trajFull.length * 0.01)];
//                getDeltaTrajShow(deltaToIdx.get(0), trajFull);

//                ArrayList<Integer> idxs = Util.loadIdxsFromFile("data/baseline/porto/ratio0.01/VASResFinal5");
//                System.out.println(idxs.size());
//                trajShow = QuadTree.loadData(new double[4], filePath, idxs);
                System.out.println("total load done: " + trajShow.length);
                isDataLoadDone = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadDelta() {
        int[] deltaList = new int[]{0, 4, 8, 16, 32, 64};
        int totalSize = trajFull.length;
        for (int delta : deltaList) {
            ArrayList<Integer> tmp = new ArrayList<>();
            readFile(delta, tmp, totalSize, 0.01);
            deltaToIndexes.put(delta, tmp);
        }
    }

    int deltaIndex = 0;

    private void calAndStoreTrajDis() {
        StringBuilder sb = new StringBuilder();
//        int index = 0;
        for (TrajectoryMeta traj : trajFull) {
//            System.out.println(index);
            index += 1;
            int length = traj.getPositions().length;
            double dis = 0;
            Position lastPosition = traj.getPositions()[0];
            for (int i = 1; i < length; ++i) {
                dis += DistanceFunc.PointEuclidean(lastPosition, traj.getPositions()[i]);
            }
            sb.append(traj.getTrajId()).append(",").append(dis).append("\n");
        }
        try (BufferedOutputStream writer = new BufferedOutputStream(Files.newOutputStream(Paths.get("data/vfgs/tkde_revision/porto_traj_dis.txt")))
        ) {
            writer.write(sb.toString().getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void readFile(int delta, ArrayList<Integer> cellList, int totalSize, double rate) {
        int size = (int) (totalSize * rate);
        // !!!
//        String filePath = String.format("data/vfgs/porto/vfgs/vfgs_%d.txt", delta);
        String filePath = String.format("E:\\zcz\\dbgroup\\VQGS\\tot_global_result_1010" +
                "\\tot_global_result_1010\\result_1010\\chengdu\\vfgs\\cd_vfgs_%d.txt", delta);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            for (int i = 0; i < size; i++) {
                String line = reader.readLine();
                String[] item = line.split(",");
                cellList.add(Integer.parseInt(item[0]));
            }
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void getDeltaTrajShow(ArrayList<Integer> idx, TrajectoryMeta[] trajFull) {
        for (int i = 0; i < trajShow.length; ++i) {
            trajShow[i] = trajFull[idx.get(i)];

        }
    }

    HashMap<Integer, ArrayList<Integer>> deltaToIdx;

    @Override
    public void mouseReleased() {

    }

    int index = 0;
    int[] deltas = {0, 4, 8, 16, 32, 64};

    private void updateDeltaIndex() {
        if (deltaIndex == deltas.length)
            deltaIndex = 0;
        int size = (int) (trajFull.length * 0.01);
        trajShow = new TrajectoryMeta[size];
        int i = 0;
        for (int idx : deltaToIndexes.get(deltas[deltaIndex])) {
            trajShow[i++] = trajFull[idx];
        }
        framePath = "data/picture/revision/chengdu/case/" + currentZoomLevel + "-" + centerIndex + "/vqgs_delta_" + deltas[deltaIndex] + ".png";
        deltaIndex += 1;
    }

    int currentZoomLevel = 12;
    int centerIndex = 0;

    String framePath = "data/picture/revision/chengdu/case/" + currentZoomLevel + "-" + centerIndex + "/full.png";

    private void updateZoomLevel() {
        currentZoomLevel += 1;
        if (currentZoomLevel == 21)
            currentZoomLevel = 13;
        map.zoomTo(currentZoomLevel);
    }


    private void updateCenter() {
        centerIndex += 1;
        if (centerIndex == chenDuCenter.length)
            centerIndex = 0;
        map.panTo(chenDuCenter[centerIndex]);
    }

    private void updateRandom() {
        int cnt = (int) (trajFull.length * 0.01);
        Random ran = new Random(1);
        HashSet<Integer> idxsSet = new HashSet<Integer>(cnt);
        while (idxsSet.size() != cnt) {
            idxsSet.add(ran.nextInt(trajFull.length - 1));
        }
        trajShow = new TrajectoryMeta[cnt];
        int i = 0;
        for (int idx : idxsSet) {
            trajShow[i++] = trajFull[idx];
        }
        framePath = "data/picture/revision/chengdu/case/" + currentZoomLevel + "-" + centerIndex + "/random.png";
    }

    @Override
    public void keyPressed() {
        if (key == 'q') {
            updateDeltaIndex();
            loop();
        } else if (key == 'r') {
            updateRandom();
            loop();
        }
    }

    @Override
    public void mouseWheel(MouseEvent e) {
        loop();
    }
    public void mousePressed(){
        Location loc = map.getLocation(mouseX, mouseY);
        System.out.println(loc);
    }

    boolean isDrawGPU = false;
    boolean drawRandom = false;

    @Override
    public void draw() {
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
            } else if (isDataLoadDone) {
                map.draw();
                if (isDrawGPU) {
                    System.out.println("draw gpu");
                    GL3 gl3;
                    gl3 = ((PJOGL) beginPGL()).gl.getGL3();
                    endPGL();
                    float[] vertexData = vertexInit(trajShow);
                    drawGPU(gl3, vertexData);
                    noLoop();
                } else {
                    System.out.println("draw cpu");
                    noFill();
                    stroke(255, 0, 0);
                    strokeWeight(1);
                    drawCPU();
//                    for (Location l : chenDuCenter) {
//                        ScreenPosition src = map.getScreenPosition(l);
//                        noFill();
//                        strokeWeight(10);
//                        stroke(new Color(19, 149, 186).getRGB());
//                        point(src.x, src.y);
//                    }
//                    saveFrame(framePath);
//                    exit();
                }
//                exit();
                if (false) {

                saveFrame(framePath);
                System.out.println(framePath);
                System.out.println("draw done " + trajShow.length + ", " + deltaIndex);

                if (currentZoomLevel == 20) {
                    if (deltaIndex == 6) {
                        if (drawRandom)
                            noLoop();
                        else {
                            updateRandom();
                            drawRandom = true;
                        }
                    } else {
                        updateDeltaIndex();
                    }
                } else {
                    if (deltaIndex == 6) {
                        if (!drawRandom) {
//                            exit();
                            updateRandom();
                            drawRandom = true;
                        } else {
                            exit();
                            updateZoomLevel();
                            deltaIndex = 0;
                            drawRandom = false;
                        }
                    } else {
                        updateDeltaIndex();
                    }
                }
//                if (deltaIndex == 6)
//                    noLoop();
//                else updateDeltaIndex();
            }else{
                    noLoop();
                }
            }
        }
    }

    private void drawCPU() {
        for (TrajectoryMeta traj : trajShow) {
            beginShape();
            for (Position loc : traj.getPositions()) {
                Location loc1 = new Location(loc.x / 10000.0, loc.y / 10000.0);
                ScreenPosition pos = map.getScreenPosition(loc1);
                vertex(pos.x, pos.y);
            }
            endShape();
        }
    }

    //    private float[]vertexInit(ArrayList<String> rawTraj, HashSet<Integer> idxs){
//        int lineCnt = 0;
//        ArrayList<Float> points = new ArrayList<>();
//        for (int idx: idxs){
//            String[] traj = rawTraj.get(idx).split(";")[1].split(",");
//            for (String p : traj){
//                points.add(Float.parseFloat(p))
//
//            }
//        }
//    }
    private float[] vertexInit(TrajectoryMeta[] trajShowMeta) {
        int line_count = 0;
        for (TrajectoryMeta traj : trajShowMeta) {
            if (traj != null)
                line_count += ((traj.getEnd() - traj.getBegin() + 1));
        }
        float[] tmpCertex = new float[line_count * 2 * 2];
        int j = 0;
        for (TrajectoryMeta trajToSubpart : trajShowMeta) {
            if (trajToSubpart == null)
                continue;
            TrajectoryMeta traj = trajToSubpart;
            for (int i = trajToSubpart.getBegin(); i < trajToSubpart.getEnd() - 1; i++) {
                Location loc1 = new Location(traj.getPositions()[i].x / 10000.0, traj.getPositions()[i].y / 10000.0);
                Location loc2 = new Location(traj.getPositions()[i + 1].x / 10000.0, traj.getPositions()[i + 1].y / 10000.0);
                ScreenPosition pos = map.getScreenPosition(loc1);
                ScreenPosition pos2 = map.getScreenPosition(loc2);
                tmpCertex[j++] = pos.x;
                tmpCertex[j++] = pos.y;
                tmpCertex[j++] = pos2.x;
                tmpCertex[j++] = pos2.y;
            }
        }
        return tmpCertex;
    }

    private void drawGPU(GL3 gl3, float[] vertexData) {
        int shaderProgram = shaderInit(gl3);
        FloatBuffer vertexDataBuffer = GLBuffers.newDirectFloatBuffer(vertexData);

        int[] vboHandles = new int[1];
        gl3.glGenBuffers(1, vboHandles, 0);

        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, vboHandles[0]);
        gl3.glBufferData(GL3.GL_ARRAY_BUFFER, vertexDataBuffer.capacity() * 4, vertexDataBuffer, GL3.GL_STATIC_DRAW);
        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);
        vertexDataBuffer = null;

        IntBuffer vao = GLBuffers.newDirectIntBuffer(1);
        gl3.glGenVertexArrays(1, vao);
        gl3.glBindVertexArray(vao.get(0));

        gl3.glUseProgram(shaderProgram);

        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, vboHandles[0]);
        gl3.glEnableVertexAttribArray(0);
        gl3.glEnableVertexAttribArray(1);
        gl3.glVertexAttribPointer(0, 2, GL3.GL_FLOAT, false, 0, 0);
        gl3.glVertexAttribPointer(1, 2, GL3.GL_FLOAT, false, 0, 0);

        gl3.glDrawArrays(GL3.GL_LINES, 0, vertexData.length / 2);
        gl3.glDisableVertexAttribArray(0);
        gl3.glDisableVertexAttribArray(1);
    }

    private int shaderInit(GL3 gl3) {
        // initializeProgram

        int shaderProgram = gl3.glCreateProgram();

        int fragShader = gl3.glCreateShader(GL3.GL_FRAGMENT_SHADER);
        gl3.glShaderSource(fragShader, 1,
                new String[]{
                        "#ifdef GL_ES\n" +
                                "precision mediump float;\n" +
                                "precision mediump int;\n" +
                                "#endif\n" +
                                "\n" +
                                "varying vec4 vertColor;\n" +
                                "\n" +
                                "void main() {\n" +
                                "  gl_FragColor = vec4(1.0,0.0,0.0,1.0);\n" +
                                "}"
                }, null);
        gl3.glCompileShader(fragShader);

        int vertShader = gl3.glCreateShader(GL3.GL_VERTEX_SHADER);
        gl3.glShaderSource(vertShader, 1,
                new String[]{
                        "#version 330 \n"
                                + "layout (location = 0) in vec4 position;"
                                + "layout (location = 1) in vec4 color;"
                                + "smooth out vec4 theColor;"
                                + "void main(){"
                                + "gl_Position.x = position.x / 600.0 - 1;"
                                + "gl_Position.y = -1 * position.y / 400.0 + 1;"
                                + "theColor = color;"
                                + "}"
                }, null);
        gl3.glCompileShader(vertShader);


        // attach and link
        gl3.glAttachShader(shaderProgram, vertShader);
        gl3.glAttachShader(shaderProgram, fragShader);
        gl3.glLinkProgram(shaderProgram);

        // program compiled we can free the object
        gl3.glDeleteShader(vertShader);
        gl3.glDeleteShader(fragShader);

        return shaderProgram;
    }

    public void runPApplet(int zoomlevel, int cidx) {
        currentZoomLevel = zoomlevel;
        centerIndex = cidx;
        PApplet.main(new String[]{
                DrawTraj.class.getName()
        });
    }

    public static void main(String[] args) {
//        double[] scores = loadDTWResPairs(PSC.PORTO_DTW_PATH);
        PApplet.main(new String[]{
                DrawTraj.class.getName()
        });
    }
}
