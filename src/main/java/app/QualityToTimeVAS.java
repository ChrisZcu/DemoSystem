package app;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.providers.MapBox;
import de.fhpotsdam.unfolding.utils.MapUtils;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import index.QuadTree;
import index.VfgsForIndexPart;
import model.*;
import processing.core.PApplet;
import processing.core.PImage;
import processing.opengl.PJOGL;
import util.PSC;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;


public class QualityToTimeVAS extends PApplet {

    private UnfoldingMap map;

    @Override
    public void settings() {
        size(1200, 800, P2D);
    }

    private boolean isDataLoadDone = false;
    private PImage mapImage = null;

    private static TrajectoryMeta[] trajMetaFull;
    private TrajToSubpart[] trajVQGS;

    //cd注意zoomlevel12

    private String filePath = PSC.portoPath;
    private ArrayList<Integer> dtwList = new ArrayList<>();

    @Override
    public void setup() {
        map = new UnfoldingMap(this, new MapBox.CustomMapBoxProvider(PSC.WHITE_MAP_PATH));
        map.setZoomRange(0, 20);
        map.setBackgroundColor(255);
        map.zoomAndPanTo(11, PSC.portoCenter);

        MapUtils.createDefaultEventDispatcher(this, map);

        new Thread(() -> {
            try {
//                dtwList = loadDTWRes(PSC.CD_DTW_PATH);
//                System.out.println("dtw load done: " + dtwList.size());

                trajMetaFull = QuadTree.loadData(new double[4], filePath);
                TimeProfileSharedObject.getInstance().trajMetaFull = trajMetaFull;
                trajVQGS = VfgsForIndexPart.getVfgs(trajMetaFull, 2);
                totlaScore = VfgsForIndexPart.getTotalScore(trajMetaFull).size();
                System.out.println("total load done: " + trajMetaFull.length);
                System.out.println("total score:" + totlaScore);
                System.out.println("VQGS length: " + trajVQGS.length);
                isDataLoadDone = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        gl3 = ((PJOGL) beginPGL()).gl.getGL3();
        endPGL();
    }

    private int zoomCheck = -1;
    private Location centerCheck = new Location(-1, -1);

    private int quality = 0;

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
                return (o1.getValue() - o2.getValue()) > 0 ? -1 : (o1.getValue() - o2.getValue()) == 0 ? 0 : 1;
            }
        };
        List<Map.Entry<Integer, Double>> list = new ArrayList<Map.Entry<Integer, Double>>(id2Score.entrySet());
        Collections.sort(list, valueComparator);
        for (Map.Entry<Integer, Double> entry : list) {
            res.add(entry.getKey());
        }
        return res;
    }

    private TrajectoryMeta[] qualityCal(double quality) {
        double qualitySearch = 0;
        ArrayList<TrajectoryMeta> res = new ArrayList<>();
        for (TrajToSubpart traj : trajVQGS) {
            if (qualitySearch >= quality) {
                break;
            }
            qualitySearch = traj.quality;
            res.add(trajMetaFull[traj.getTrajId()]);
        }
        return res.toArray(new TrajectoryMeta[0]);
    }

    private TrajectoryMeta[] ranQuality(double quality) {
        double targetScore = totlaScore * quality;
        ArrayList<TrajectoryMeta> res = new ArrayList<>();
        Random ran = new Random(0);
        HashSet<Position> influSet = new HashSet<>();
        HashSet<Integer> idSet = new HashSet<>();
        while (influSet.size() < targetScore) {
            int trajId = ran.nextInt(trajMetaFull.length);
            if (!idSet.contains(trajId)) {
                influSet.addAll(generatePosList(trajMetaFull[trajId]));
                res.add(trajMetaFull[trajId]);
                idSet.add(trajId);
            }
        }
        return res.toArray(new TrajectoryMeta[0]);
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
            if (isDataLoadDone) {
                System.out.println("calculating......");

                double qualitySearch = quality / 10.0;

                TrajectoryMeta[] trajShow;
                int num = 0;
                trajShow = qualityCal(qualitySearch);
                //   HashSet<Position> tmp = new HashSet<>();
                //   for (TrajectoryMeta traj : trajShow) {
                //      num += traj.getPositions().length;
                //     tmp.addAll(Arrays.asList(traj.getPositions()));
                //  }
                //  System.out.println("vfgs number: " + trajShow.length + ", trajectory point number: " + num +
                //          ", special point number: " + tmp.size());

                //  trajShow = ranQuality(qualitySearch);
                //num = 0;
                //tmp.clear();
                //for (TrajectoryMeta traj : trajShow) {
                //    num += traj.getPositions().length;
                //   tmp.addAll(Arrays.asList(traj.getPositions()));
                // }
                //  System.out.println("random number: " + trajShow.length + ", trajectory point number: " + num +
                //        ", special point number: " + tmp.size());
                //                trajShow = dtwQuality(qualitySearch);
                long beginTime = System.currentTimeMillis();
                long mappingTime = vertexInit(trajShow);
                drawGPU();
                long endTime = System.currentTimeMillis() - beginTime;
                System.out.println(qualitySearch + ", vis time: " + (endTime) + "ms, "
                        + "mapping time: " + mappingTime + "ms, "
                        + "rendering: " + (endTime - mappingTime) + "ms");
                String name = "vqgs_" + qualitySearch + ".png";
                saveFrame("data/tmp/" + name);
                if (quality == 10) {
                    exit();
                } else {
                    quality++;
                }
                int[] tmpList = new int[20000000];
                for (int i = 0; i < tmpList.length; i++) {
                    tmpList[i] = i;
                }

            }
        }
    }

    private int totlaScore;

    private TrajectoryMeta[] dtwQuality(double quality) {
        double targetScore = totlaScore * quality;
        ArrayList<TrajectoryMeta> res = new ArrayList<>();
        HashSet<Position> influSet = new HashSet<>();
        int trajId = 0;
        while (influSet.size() < targetScore) {
            influSet.addAll(generatePosList(trajMetaFull[dtwList.get(trajId)]));
            res.add(trajMetaFull[dtwList.get(trajId)]);
            trajId++;
        }
        return res.toArray(new TrajectoryMeta[0]);

    }

    private static List<Position> generatePosList(TrajectoryMeta trajMeta) {
        int trajId = trajMeta.getTrajId();
//        int begin = trajMeta.getBegin();
//        int end = trajMeta.getEnd();      // notice that the end is included

        return Arrays.asList(trajMetaFull[trajId].getPositions());
    }

    private GL3 gl3;
    private int[] vboHandles;
    int shaderProgram, vertShader, fragShader;
    int vertexBufferObject;

    IntBuffer vao = GLBuffers.newDirectIntBuffer(1);
    float[] vertexData = {};

    private void drawGPU() {
        shaderInit();
        FloatBuffer vertexDataBuffer = GLBuffers.newDirectFloatBuffer(vertexData);

        vboHandles = new int[1];
        gl3.glGenBuffers(1, vboHandles, 0);

        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, vboHandles[0]);
        gl3.glBufferData(GL3.GL_ARRAY_BUFFER, vertexDataBuffer.capacity() * 4, vertexDataBuffer, GL3.GL_STATIC_DRAW);
        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);
        vertexDataBuffer = null;

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

    private void shaderInit() {
        // initializeProgram

        shaderProgram = gl3.glCreateProgram();

        fragShader = gl3.glCreateShader(GL3.GL_FRAGMENT_SHADER);
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

        vertShader = gl3.glCreateShader(GL3.GL_VERTEX_SHADER);
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

    }

    private long vertexInit(TrajectoryMeta[] trajShowMeta) {
        int line_count = 0;
        for (TrajectoryMeta traj : trajShowMeta) {
            line_count += ((traj.getEnd() - traj.getBegin() + 1));
        }
        System.out.println(line_count);
        float[] tmpCertex = new float[line_count * 2 * 2];
        long timeBegin = System.currentTimeMillis();
        int j = 0;
        int num = 0;
        for (TrajectoryMeta trajToSubpart : trajShowMeta) {
            TrajectoryMeta traj = trajMetaFull[trajToSubpart.getTrajId()];
            for (int i = trajToSubpart.getBegin(); i < trajToSubpart.getEnd() - 1; i++) {
                num++;
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
        //18908722
        vertexData = tmpCertex;
        System.out.println("num: " + num);
        return (System.currentTimeMillis() - timeBegin);
    }

    //20159005 67941ms
    //75083265 1.0, 1.0, fly time: 65522ms, response time: 48385ms, query time: 11212ms, mapping time: 42801ms, rendering: 5584ms


    private long vertexInit(TrajToSubpart[] trajShowMeta) {
        int line_count = 0;
        for (TrajToSubpart traj : trajShowMeta) {
            line_count += ((traj.getEndPosIdx() - traj.getBeginPosIdx() + 1));
        }
        System.out.println(line_count);
        float[] tmpCertex = new float[line_count * 2 * 2];
        long t0 = System.currentTimeMillis();
        int j = 0;
        for (TrajToSubpart trajToSubpart : trajShowMeta) {
            TrajectoryMeta traj = trajMetaFull[trajToSubpart.getTrajId()];
            for (int i = trajToSubpart.getBeginPosIdx(); i < trajToSubpart.getEndPosIdx() - 1; i++) {
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
        vertexData = tmpCertex;
        return (System.currentTimeMillis() - t0);
    }


    public static void main(String[] args) {
        PApplet.main(new String[]{QualityToTimeVAS.class.getName()});
    }
}
