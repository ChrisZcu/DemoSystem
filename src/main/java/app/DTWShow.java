package app;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.providers.MapBox;
import de.fhpotsdam.unfolding.utils.MapUtils;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import index.QuadTree;
import javafx.geometry.Pos;
import model.Position;
import model.TrajectoryMeta;
import processing.core.PApplet;
import processing.core.PImage;
import processing.event.MouseEvent;
import processing.opengl.PJOGL;
import util.PSC;
import util.Util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;

public class DTWShow extends PApplet {
    static class Node {
        int trajId;
        double oriVal;
        double agrVal;

        Node next;
        Node prev;

        public Node(double o, double a, int id) {
            oriVal = o;
            agrVal = a;
            trajId = id;
        }
    }

    private UnfoldingMap map;
    private static TrajectoryMeta[] trajMetaFull;
    private static TrajectoryMeta[] trajShow;
    private String filePath = PSC.portoPath;

    private boolean isDataLoadDone = false;
    private PImage mapImage = null;
    private int zoomCheck = -1;
    private Location centerCheck = new Location(-1, -1);


    public void settings() {
        size(1200, 800, P2D);
    }

    String framePath = "";

    public ArrayList<Integer> loadDTWRes(String filePath) {
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
                return -((o1.getValue() - o2.getValue()) > 0 ? -1 : (o1.getValue() - o2.getValue()) == 0 ? 0 : 1);
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
        DecimalFormat format = new DecimalFormat("#.0000");
        for (String e : metaTmp) {
            int id = Integer.parseInt(e.split(",")[0]);
            double score = Double.parseDouble(e.split(",")[1]);
            String str = format.format(score);
            score = Double.parseDouble(str);
            maxScore = Math.max(maxScore, score);
            scores[id] = score;
        }
        return scores;
    }

    private static double[] loadDTWResPairsReverse(String filePath) {
        //TODO
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
        int n = metaTmp.size();
        double maxScore = -1;
        DecimalFormat format = new DecimalFormat("#.0000");
        for (String e : metaTmp) {
            int id = Integer.parseInt(e.split(",")[0]);
            double score = Double.parseDouble(e.split(",")[1]);
            String str = format.format(score);
            score = Double.parseDouble(str);
            maxScore = Math.max(maxScore, score);
//            Porot: 23907, CD: 19618, SZ: 32,439
            scores[id] = 1 / (n - score / 32439);
        }
        System.out.println(metaTmp.size() + ", " + maxScore + ", " + (n - maxScore / 32439));
        return scores;
    }

    private HashMap<String, Double> scoreSumMap = new HashMap<String, Double>() {
        {
            put("porto", 1.0378655134877903E12);
            put("cd", -1.0);
            put("sz", -1.0);
        }
    };

    private static Node constructSampleScores(double[] scores) {
        Node head = new Node(-1, -1, -1);

        double last = 0;
        Node lastHead = head;
//        long[] res = new long[scores.length];
        for (int i = 0; i < scores.length; ++i) {
            Node tmp = new Node(scores[i], scores[i] + last, i);
            lastHead.next = tmp;
            tmp.prev = lastHead;
            lastHead = tmp;
//            scores[i] += last;zhe
            last += scores[i];
//            res[i] = (last + (long) (scores[i] * 10000));
//            last = res[i];
        }
        Node tail = new Node(-1, -1, -1);
        lastHead.next = tail;
        tail.prev = lastHead;
        head.agrVal = lastHead.agrVal;
        return head;
    }

    private static int handleSample(double target, Node head) {
        Node tmp = head.next;
        Node last = tmp;
        while (tmp.trajId > -1) {
            if (tmp.agrVal > target) {
                break;
            }
            last = tmp;
            tmp = tmp.next;
        }
        int trajId = last.trajId;
        double score = last.oriVal;
        last.prev.next = last.next;
        last.next.prev = last.prev;
        last = last.next;
        while (last.trajId > -1) {
            last.agrVal -= score;
            last = last.next;
        }
        head.agrVal = last.prev.agrVal;
        return trajId;
    }

    private ArrayList<Integer> sampledResFromDTWDensity() {
        double[] scores = loadDTWResPairs(PSC.CD_DTW_PATH);
        Node head = constructSampleScores(scores);
//        System.out.println(head.agrVal);
        Random ran = new Random(1);
//        System.out.println(scores[scores.length - 1]);
        ArrayList<Integer> res = new ArrayList<>();
        int tagetSize = (int) (scores.length * 0.01);
        System.out.println(tagetSize);
        double upBound;
        while (res.size() != tagetSize) {
            System.out.println(res.size());
            System.out.println(tagetSize);
            upBound = head.agrVal;
            double r = ran.nextDouble();
            double tmp = upBound * r;
//            System.out.println(head.agrVal +", " + tmp + ", " + r);
            int trajId = handleSample(tmp, head);
            res.add(trajId);
        }
        System.out.println("sample done");
        return res;
    }

    private static ArrayList<Integer> sampledResFromDTWDensityReverse() {
        double[] scores = loadDTWResPairsReverse(PSC.SZ_DTW_PATH);
        Node head = constructSampleScores(scores);
        Random ran = new Random(1);
        ArrayList<Integer> res = new ArrayList<>();
        int tagetSize = (int) (scores.length * 0.01);
        System.out.println(tagetSize);
        double upBound;
        while (res.size() != tagetSize) {
//            System.out.println(res.size());
            upBound = head.agrVal;
            double tmp = upBound * ran.nextDouble();
            int trajId = handleSample(tmp, head);
            res.add(trajId);
        }
        System.out.println("sample done");
        return res;
    }

    private int endIdx = 2000000;

    private void calQuality(TrajectoryMeta[] trajMetaFull) {
        int totalScore = screenScore(trajMetaFull);
//        ArrayList<Integer> idxs = loadDTWRes(PSC.CD_DTW_PATH);
//        ArrayList<Integer> idxs = loadDTWRes("data/baseline/porto/ratio0.01/VASResFinal5");
//        ArrayList<Integer> idxs = loadDTWRes("data/baseline/cd/CDVASResFinal3_34");
        ArrayList<Integer> idxs = loadDTWRes("data/baseline/sz/SZVASResFinal1_9");
        HashMap<String, HashMap<Double, String>> idxsMap = new HashMap<>();
        HashMap<Double, String> portoMap = new HashMap<>();
        portoMap.put(0.01, "data/baseline/porto/ratio0.01/VASResFinal5");
        portoMap.put(0.005, "data/baseline/porto/PortoVASResFinalResample-0.005");
        portoMap.put(0.001, "data/baseline/porto/PortoVASResFinalResample-0.001");
        portoMap.put(0.0005,  "data/baseline/porto/PortoVASResFinalResample-5.0E-4");
        portoMap.put(0.0001, "data/baseline/porto/PortoVASResFinalResample-1.0E-4");
        idxsMap.put("porto", portoMap);

        double[] ratios = {0.01, 0.005, 0.001, 0.0005, 0.0001};
//        double[] ratios = {0.0001, 0.0005, 0.001, 0.005, 0.01};
        for (double r : ratios) {
            System.out.println(">>>>>>>>>>>>>" + r);
            idxs = loadDTWRes(portoMap.get(r));;
            trajShow = QuadTree.loadData(new double[4], filePath, idxs, r);
            System.out.println(screenScore(trajShow) * 1.0 / totalScore);
//
//            System.out.println("reverse: " + r);
//            idxs = Util.loadIdxsFromFile("data/baseline/porto/CDReverseDensity");
//            trajShow = QuadTree.loadData(new double[4], filePath, idxs, r);
//            System.out.println(screenScore(trajShow) * 1.0 / totalScore);
//
//            System.out.println("non-reverse: " + r);
//            idxs = Util.loadIdxsFromFile("data/baseline/porto/CDDensity");
//            trajShow = QuadTree.loadData(new double[4], filePath, idxs, r);
//            System.out.println(screenScore(trajShow) * 1.0 / totalScore);

        }

    }

    private void calQuality(TrajectoryMeta[] trajMetaFull, TrajectoryMeta[] trajShow) {
        int totalScore = screenScore(trajMetaFull);
        System.out.println(screenScore(trajShow) * 1.0 / totalScore);
    }

    private int screenScore(TrajectoryMeta[] trajList) {
        HashSet<Position> tmp = new HashSet<>();
        for (TrajectoryMeta trajToSubpart : trajList) {
            if (trajToSubpart == null)
                continue;
            for (int i = trajToSubpart.getBegin(); i < trajToSubpart.getEnd() - 1; i++) {
                Location loc1 = new Location(trajToSubpart.getPositions()[i].x / 10000.0, trajToSubpart.getPositions()[i].y / 10000.0);
                ScreenPosition pos = map.getScreenPosition(loc1);
                tmp.add(new Position(pos.x, pos.y));
//                tmp.add(loc1);
//                tmp.add(new Position(trajToSubpart.getPositions()[i].x, trajToSubpart.getPositions()[i].y));
            }
        }
        return tmp.size();
    }

    @Override
    public void setup() {
        map = new UnfoldingMap(this, new MapBox.CustomMapBoxProvider(PSC.WHITE_MAP_PATH));
        map.setZoomRange(0, 20);
        map.setBackgroundColor(255);
        map.zoomAndPanTo(11, PSC.portoCenter);
        MapUtils.createDefaultEventDispatcher(this, map);

        new Thread(() -> {
            try {
                System.out.println("begin cal");

                /*
                Random ran = new Random(1);
                ArrayList<Integer> idxs = new ArrayList<>();
                int sampleSize = 30668;
                HashSet<Integer> initSet = new HashSet<Integer>(sampleSize);
                while (initSet.size() != sampleSize) {
                    initSet.add(ran.nextInt(3066862 - 1));
                }
                for (int id : initSet)
                    idxs.add(id);
                trajShow = QuadTree.loadData(new double[4], filePath, idxs);
                */


                TrajectoryMeta[] trajFull = QuadTree.loadData(new double[4], filePath);
                calQuality(trajFull);

                /*
                ArrayList<Integer> idxs = sampledResFromDTWDensityReverse();
                Util.storeIdxs("data/baseline/porto/SZDensityOp", idxs);
//                ArrayList<Integer> idxs = Util.loadIdxsFromFile("data/baseline/porto/PortoDensityOp");

                double[] ratio = {0.0001, 0.0005, 0.001, 0.005, 0.01};
                for (double r : ratio) {
                    trajShow = new TrajectoryMeta[(int) (trajFull.length * r)];
                    int id = 0;
                    for (int i : idxs) {
                        trajShow[id++] = trajFull[i];
                        if (id == (int) (trajFull.length * r))
                            break;
                    }
                    calQuality(trajFull, trajShow);
                }
                */

//                idxs = sampledResFromDTWDensity();
//                util.Util.storeIdxs("data/baseline/porto/SZDensityReverse",idxs);

//                System.out.println("total load done: " + trajShow.length);

                isDataLoadDone = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void mouseReleased() {

    }

    @Override
    public void keyPressed() {
        if (key == 'q') {
            endIdx = 2389863;
        }
        loop();
    }

    @Override
    public void mouseWheel(MouseEvent e) {
        loop();
    }

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
            map.draw();
            if (!map.allTilesLoaded()) {
                if (mapImage == null) {
                    mapImage = map.mapDisplay.getInnerPG().get();
                }
                image(mapImage, 0, 0);
            } else if (isDataLoadDone) {
                GL3 gl3;
                gl3 = ((PJOGL) beginPGL()).gl.getGL3();
                endPGL();
//                TrajectoryMeta[] trajShowTmp = new TrajectoryMeta[endIdx];
//                System.arraycopy(trajShow, 0, trajShowTmp, 0, endIdx);
                float[] vertexData = vertexInit(trajShow);

                drawGPU(gl3, vertexData);
                System.out.println("draw done " + trajShow.length);
                saveFrame(framePath);
                noLoop();
            }
        }
    }

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

    public static void main(String[] args) {
//        trajShow = QuadTree.loadData(new double[4], PSC.portoPath);
        PApplet.main(new String[]{
                DTWShow.class.getName()
        });
//        loadDTWResPairsReverse(PSC.PORTO_DTW_PATH);
    }
}
