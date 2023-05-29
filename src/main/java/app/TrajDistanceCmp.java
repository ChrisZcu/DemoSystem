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
import processing.core.PApplet;
import processing.core.PImage;
import processing.event.MouseEvent;
import processing.opengl.PJOGL;
import util.PSC;

import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class TrajDistanceCmp extends PApplet {

    private UnfoldingMap map;
    private static TrajectoryMeta[] trajMetaFull;
    private static TrajectoryMeta[] trajShow;
    private static String filePath = PSC.portoPath;

    private boolean isDataLoadDone = false;
    private PImage mapImage = null;
    private int zoomCheck = -1;
    private Location centerCheck = new Location(-1, -1);


    public void settings() {
        size(1200, 800, P2D);
    }

    String framePath = "";

    public void setup() {
        map = new UnfoldingMap(this, new MapBox.CustomMapBoxProvider(PSC.WHITE_MAP_PATH));
        map.setZoomRange(0, 20);
        map.setBackgroundColor(255);
        Location tmp = new Location(41.149, -8.59);
        map.zoomAndPanTo(15, PSC.portoCenter);
//        map.zoomAndPanTo(15, tmp);
        MapUtils.createDefaultEventDispatcher(this, map);

        new Thread(() -> {
            try {

                System.out.println("begin cal");
                ArrayList<Integer> idxs = new ArrayList<>();
                int cnt = 11;
                for (int i = 0; i < cnt; ++i) {
                    if (i == 8)
                        continue;
                    idxs.add(i);
                }
                trajShow = QuadTree.loadData(new double[4], filePath, idxs);

                System.out.println("total load done: " + trajShow.length);
                isDataLoadDone = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void mouseReleased() {
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
            if (!map.allTilesLoaded()) {
                if (mapImage == null) {
                    mapImage = map.mapDisplay.getInnerPG().get();
                }
                image(mapImage, 0, 0);
            } else if (isDataLoadDone) {
                map.draw();
                map.draw();
                map.draw();
                map.draw();

//                GL3 gl3;
//                gl3 = ((PJOGL) beginPGL()).gl.getGL3();
//                endPGL();
//                float[] vertexData = vertexInit(trajShow);
//                drawGPU(gl3, vertexData);

                drawCPU();
                System.out.println("draw done");
//                saveFrame(framePath);
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

    private void drawCPU() {
        noFill();
        strokeWeight(2);
        int[] colorList = new int[]{
                new Color(228, 26, 28, 125).getRGB(),
                new Color(166, 86, 40, 125).getRGB(),
                new Color(0,0,0, 125).getRGB(),
                new Color(152, 78, 163, 125).getRGB(),
                new Color(77, 175, 74, 125).getRGB(),
                new Color(254,178,76, 125).getRGB(),
                new Color(228, 26, 28, 125).getRGB(),
                new Color(247, 129, 191, 125).getRGB(),
                new Color(255, 127, 0, 125).getRGB(),
                new Color(55, 126, 184, 125).getRGB(),
        };

        for (int i = 0; i < 10; ++i) {
            TrajectoryMeta traj = trajShow[i];
            stroke(colorList[i]);
            beginShape();
            for (Position p : traj.getPositions()){
                Location loc = new Location(p.x / 10000.0, p.y / 10000.0);
                ScreenPosition src = map.getScreenPosition(loc);
                vertex(src.x, src.y);
            }
            endShape();
        }
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

    private static void CalDistance() {
        ArrayList<Integer> idxs = new ArrayList<>();
        int cnt = 11;
        for (int i = 0; i < cnt; ++i) {
            if (i == 8)
                continue;
            idxs.add(i);
        }
        trajShow = QuadTree.loadData(new double[4], filePath, idxs);
        double[][][] disMetric = new double[10][10][6];
        HashMap<String, Double> disToMax = new HashMap<>();
        disToMax.put("Euclidean", 0.0);
        disToMax.put("DTW", 0.0);
        disToMax.put("LCSS", 0.0);
        disToMax.put("EDR", 0.0);
        disToMax.put("Discrete Frechet", 0.0);
        disToMax.put("Hausdorff", 0.0);

        HashMap<Integer, String> idxToDis = new HashMap<>();
        idxToDis.put(0, "Euclidean");
        idxToDis.put(1, "DTW");
        idxToDis.put(2, "LCSS");
        idxToDis.put(3, "EDR");
        idxToDis.put(4, "Discrete Frechet");
        idxToDis.put(5, "Hausdorff");
        for (int i = 0; i < 10; ++i) {
            for (int j = i + 1; j < 10; ++j) {
                double dis = util.DistanceFunc.EuclideanDistance(trajShow[i], trajShow[j]);
                disToMax.replace("Euclidean", Math.max(disToMax.get("Euclidean"), dis));
                disMetric[i][j][0] = dis;
//                System.out.printf("No.%1d-No.%1d: %10s%5f\n", i, j, "Euclidean Distance=", dis);
                dis = util.DistanceFunc.DTW(trajShow[i], trajShow[j]);
                disToMax.replace("DTW", Math.max(disToMax.get("DTW"), dis));
                disMetric[i][j][1] = dis;

//                System.out.printf("No.%1d-No.%1d: %10s%5f\n", i, j, "DTW Distance=", dis);
                dis = util.DistanceFunc.LCSS(trajShow[i], trajShow[j]);
                disToMax.replace("LCSS", Math.max(disToMax.get("LCSS"), dis));
                disMetric[i][j][2] = dis;
//                System.out.printf("No.%1d-No.%1d: %10s%5f\n", i, j, "LCSS Distance=", dis);

                dis = util.DistanceFunc.EDR(trajShow[i], trajShow[j]); //bug TODO fix
                disToMax.replace("EDR", Math.max(disToMax.get("EDR"), dis));
                disMetric[i][j][3] = dis;
//                System.out.printf("No.%1d-No.%1d: %10s%5f\n", i, j, "EDR Distance=", dis);

                dis = util.DistanceFunc.DiscreteFrechetDistance(trajShow[i], trajShow[j]);
                disToMax.replace("Discrete Frechet", Math.max(disToMax.get("Discrete Frechet"), dis));
                disMetric[i][j][4] = dis;

//                System.out.printf("No.%1d-No.%1d: %10s%5f\n", i, j, "Discrete Frechet Distance=", dis);
                dis = util.DistanceFunc.HausdorffDistance(trajShow[i], trajShow[j]);
                disToMax.replace("Hausdorff", Math.max(disToMax.get("Hausdorff"), dis));
                disMetric[i][j][5] = dis;
//                System.out.printf("No.%1d-No.%1d: %10s%5f\n", i, j, "Hausdorff Distance=", dis);
            }
        }
        for (int i = 0; i < 6; ++i) {
//            if (i == 3) {
//                continue;
//            }
            System.out.println();
            String disName = idxToDis.get(i);
            double maxVal = disToMax.get(disName);
            int src = 0, dst = 0;
            double maxValTmp = -1;

            int srcMin = 0, dstMin = 0;
            double minVal = 10000.0;
            System.out.printf("------------------------------------%10s------------------------------------\n", disName);
            System.out.printf("%-8s", "");
            for (int b = 0; b < 10; b++)
                System.out.printf("%-8s", "|No." + (b + 1));
            System.out.println();
            System.out.println("---------------------------------------------------------------------------------------");
            for (int a = 0; a < 10; ++a) {
                System.out.printf("%-8s", "No." + (a + 1));
                for (int b = 0; b < a + 1; b++) {
                    System.out.printf("%-8s", "|");
                }
                for (int b = a + 1; b < 10; ++b) {
                    double dis = disMetric[a][b][i] /= 1;
                    if (dis > maxValTmp) {
                        src = a + 1;
                        dst = b + 1;
                        maxValTmp = dis;
                    }
                    if (dis < minVal) {
                        srcMin = a + 1;
                        dstMin = b + 1;
                        minVal = dis;
                    }
                    System.out.printf("%-8s", "|" + String.format("%.4f", dis));
                }
                System.out.println();
                System.out.println("---------------------------------------------------------------------------------------");
            }
            System.out.println(src + ", " + dst);
            System.out.println(srcMin + ", " + dstMin);
        }
    }

    public static void main(String[] args) {
        CalDistance();

//        PApplet.main(new String[]{
//                TrajDistanceCmp.class.getName()
//        });
    }
}
