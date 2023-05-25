package tmp;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.providers.MapBox;
import de.fhpotsdam.unfolding.utils.MapUtils;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import index.QuadTree;
import model.TrajectoryMeta;
import processing.core.PApplet;
import processing.core.PImage;
import processing.opengl.PJOGL;
import util.PSC;

import java.io.*;
import java.net.Socket;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import static app.KernelMatrixRender.getDTWId;
import static tmp.RegionShow.getRandomTraj;

public class Experiment extends PApplet {
    private UnfoldingMap map;
    private static TrajectoryMeta[] trajMetaFull;
    private boolean isDataLoadDone = false;

    private PImage mapImage = null;
    private int zoomCheck = -1;
    private Location centerCheck = new Location(-1, -1);

    private static double fullPixel = 0;

    public void settings() {
        size(1200, 800, P2D);
    }

    public void setup() {
        map = new UnfoldingMap(this, new MapBox.CustomMapBoxProvider(PSC.WHITE_MAP_PATH));
        map.setZoomRange(0, 20);
        map.setBackgroundColor(255);

        //porto
//        map.zoomAndPanTo(12, new Location(41.17129, -8.559485));
        map.zoomAndPanTo(12, PSC.cdCenter);
        MapUtils.createDefaultEventDispatcher(this, map);

        new Thread(() -> {
            String filePath = "data/kernelMatrix/chengdu/chengdu_region.txt";
            trajMetaFull = QuadTree.loadData(new double[4], filePath);

            isDataLoadDone = true;
            System.out.println("total load done, trajectory dataset: " + trajMetaFull.length);
        }).start();

    }

    private static int indexId = 0;
    private static int methodTag = 3;//0 for random, 1 for dtw, 2 for qis, 3 for full
    private static double[] rate = {0.001, 0.002, 0.003, 0.004, 0.005};

    @Override
    public void keyPressed() {
        System.out.println("key pressed: " + key);
        if (key == 'q') {
            if (indexId < 4)
                indexId++;
        } else if (key == 'w') {
            if (indexId > 0) {
                indexId--;
            }
        } else if (key == 'a') {
            if (methodTag < 3)
                methodTag++;
        } else if (key == 's') {
            if (methodTag > 0) {
                methodTag--;
            }
        }
        loop();
    }

    //    private static TrajectoryMeta[] trajMetaFullTmp = QuadTree.loadData(new double[4], PSC.cdPath);
    private static boolean isTimeProfile = false;

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
                map.draw();
                TrajectoryMeta[] trajShow;
                switch (methodTag) {
                    case 0://random
                        trajShow = getRandomTraj(rate[indexId], trajMetaFull);
                        break;

                    case 1://dtw
                        String dtwPath = "E:\\zcz\\dbgroup\\sigir\\dtw\\data\\dtw\\kernel_matrix\\ordered\\cd.txt";
                        trajShow = new TrajectoryMeta[(int) (rate[indexId] * trajMetaFull.length)];
//                        int[] trajIdAry = getDTWId(rate[indexId], dtwPath, trajMetaFullTmp);
                        int i = 0;
//                        for (int id : trajIdAry) {
//                            if (i == trajShow.length)
//                                break;
//                            trajShow[i++] = trajMetaFullTmp[id];
//                        }
                        break;

                    case 2://qis
                        String qisPath = "E:\\zcz\\dbgroup\\JavaGPU\\data\\kernel_matrix\\parameter\\chengdu\\qisRate0.01\\range800" +
                                "\\index_full_dis_pixel_cov\\chengdu_qis_location_cov0_01.txt7.txt";
                        trajShow = new TrajectoryMeta[(int) (rate[indexId] * trajMetaFull.length)];
                        int[] trajIdAryQis = getDTWId(rate[indexId], qisPath, trajMetaFull);
                        int iQis = 0;
                        for (int id : trajIdAryQis) {
                            trajShow[iQis++] = trajMetaFull[id];
                        }
                        break;
                    case 3://full
                        trajShow = trajMetaFull;
                        break;

                    default:
                        trajShow = new TrajectoryMeta[(int) (rate[indexId] * trajMetaFull.length)];
                        break;

                }
                long timeBegin = System.currentTimeMillis();
                GL3 gl3;
                gl3 = ((PJOGL) beginPGL()).gl.getGL3();
                endPGL();
                float[] vertexData = vertexInit(trajShow);
                drawGPU(gl3, vertexData);
                long timeEnd = System.currentTimeMillis();
                System.out.println(">>>>>>>>>>0 for random, 1 for dtw, 2 for qis, 3 for full");
                System.out.println("trajShow length: " + trajShow.length);
                System.out.println("method tag: " + methodTag + ", rate: " + rate[indexId] + ", rendering time: " + (timeEnd - timeBegin) + "ms");
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
        PApplet.main(new String[]{
                Experiment.class.getName()
        });
    }
}
