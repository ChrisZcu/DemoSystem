package app;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.providers.MapBox;
import de.fhpotsdam.unfolding.utils.MapUtils;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import index.QuadTree;
import model.TrajectoryMeta;
import org.lwjgl.Sys;
import org.lwjgl.system.CallbackI;
import processing.core.PApplet;
import processing.core.PImage;
import processing.opengl.PJOGL;
import tmp.RegionShow;
import util.PSC;

import java.io.*;
import java.net.Socket;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import static tmp.RegionShow.getRandomTraj;
import static tmp.RegionShow.getRandomTrajId;

public class KernelMatrixRender extends PApplet {
    private UnfoldingMap map;
    private static TrajectoryMeta[] trajMetaFull;
    private String filePath = PSC.PORTO_KERNEL_REGION;

    private boolean isDataLoadDone = false;

    private PImage mapImage = null;
    private int zoomCheck = -1;
    private Location centerCheck = new Location(-1, -1);

    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;

    public void settings() {
        size(1200, 800, P2D);
    }

    public void setup() {
        map = new UnfoldingMap(this, new MapBox.CustomMapBoxProvider(PSC.WHITE_MAP_PATH));
        map.setZoomRange(0, 20);
        map.setBackgroundColor(255);
//        map.zoomAndPanTo(12, new Location(41.17129, -8.559485));
        map.zoomAndPanTo(12, PSC.cdCenter);

        new Thread(() -> {
            try {

                MapUtils.createDefaultEventDispatcher(this, map);
                socket = new Socket("10.20.96.98", 5000);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                out.write("new");
                out.write("\n");
                out.flush();

                System.out.println("request for new index ary......");
                String line = in.readLine();
                if (line.contains(",")) {
                    String[] indexAry = line.split(",");
                    BufferedWriter writer = new BufferedWriter(new FileWriter("E:\\zcz\\dbgroup\\JavaGPU\\data\\kernel_matrix" +
                            "\\parameter\\chengdu\\qisRate0.01\\range800\\index_full_dis_pixel_cov\\shenzhen_qis0.00625.txt"));
                    for (String index : indexAry) {
                        writer.write(index);
                        writer.newLine();
                    }
                    writer.close();
                }


                trajMetaFull = QuadTree.loadData(new double[4], filePath);
                TimeProfileSharedObject.getInstance().trajMetaFull = trajMetaFull;
//                String filePath = "E:\\zcz\\dbgroup\\JavaGPU\\data\\kernel_matrix\\parameter\\location\\";
//                String filePath = "E:\\zcz\\dbgroup\\JavaGPU\\data\\kernel_matrix\\parameter\\index_full_dis_pixel_cov\\";
//                String filePath = "E:\\zcz\\dbgroup\\JavaGPU\\data\\kernel_matrix\\parameter\\index_pixel\\";
//                String filePath = "E:\\zcz\\dbgroup\\JavaGPU\\data\\kernel_matrix\\parameter\\qis\\range400\\index_pixel\\";
                if (!isClient) {
                    String filePath = "E:\\zcz\\dbgroup\\JavaGPU\\data\\kernel_matrix\\parameter\\index_full_dis_pixel_cov\\";

                    String[] fileName = new File(filePath).list();
                    for (int i = 0; i < fileName.length; i++) {
                        String file = fileName[i];
                        String path = filePath + file;
//                String path = "E:\\zcz\\dbgroup\\JavaGPU\\data\\kernel_matrix\\parameter\\tmp\\qis_local_rate0.05.txt";
                        BufferedReader reader = new BufferedReader(new FileReader(path));
                        String lines;
                        ArrayList<String> item = new ArrayList<>();
                        while ((line = reader.readLine()) != null) {
                            item.add(line);
                        }
                        qisIndex[i] = item.stream().mapToInt(Integer::parseInt).toArray();
                    }
                    qisIndex[1] = getDTWId(0.001, "E:\\zcz\\dbgroup\\sigir\\dtw\\data\\dtw\\kernel_matrix\\ordered\\porto.txt", trajMetaFull);
                    qisIndex[2] = getRandomTrajId(0.001, trajMetaFull);
                    qisIndex[3] = getFullId(trajMetaFull);
                }
                isDataLoadDone = true;
                System.out.println("total load done: " + trajMetaFull.length);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private int[] getFullId(TrajectoryMeta[] trajMetaFull) {
        int[] res = new int[trajMetaFull.length];
        for (int i = 0; i < trajMetaFull.length; i++) {
            res[i] = i;
        }
        return res;
    }

    public static int[] getDTWId(double rate, String filePath, TrajectoryMeta[] trajMetaFull) {
        int num = (int) (rate * trajMetaFull.length);
        ArrayList<String> item = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = reader.readLine()) != null) {
                item.add(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        int[] res = new int[num];
        for (int i = 0; i < num; i++) {
            res[i] = Integer.parseInt(item.get(i).split(",")[0]);
        }
        return res;
    }

    private int[][] qisIndex = new int[4][];
    private long timeGap = 0;
    private TrajectoryMeta[] trajShow = {};
    private boolean isClient = true;
    private int qisId = 0;

    @Override
    public void keyPressed() {

        if (key == 'q') {
            if (qisId < qisIndex.length)
                qisId++;
            System.out.println("trajectory number: " + qisIndex[qisId].length);
            loop();
        } else if (key == 'w') {
            if (qisId > 0)
                qisId--;
            System.out.println("trajectory number: " + qisIndex[qisId].length);
            loop();
        }
    }

    @Override
    public void mousePressed() {
        System.out.println("click>>>>>>>>>" + map.getLocation(mouseX, mouseY));
    }

    @Override
    public void mouseClicked() {
        loop();
    }

    @Override
    public void mouseWheel() {
        loop();
    }

    private double[] rate = new double[]{1 / 10.0, 1 / 5.0, 1 / 20.0, 1 / 40.0, 1 / 80.0, 1 / 160.0};

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
                if (isClient) {
                    try {
                        long timeGapEnd = System.currentTimeMillis();
                        if (timeGapEnd - timeGap >= 200) {
                            timeGap = System.currentTimeMillis();
                            socket = new Socket("127.0.0.1", 5000);
                            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                            out.write("new");
                            out.write("\n");
                            out.flush();

                            System.out.println("request for new index ary......");
                            String line = in.readLine();
                            if (line.contains(",")) {
                                String[] indexAry = line.split(",");
                                BufferedWriter writer = new BufferedWriter(new FileWriter("E:\\zcz\\dbgroup\\JavaGPU\\data\\kernel_matrix\\" +
                                        "parameter\\shenzhen\\qisRate0.01\\range800\\index_full_dis_pixel_cov\\shenzhen_qis0.00625.txt"));
                                for (String index : indexAry) {
                                    writer.write(index);
                                    writer.newLine();
                                }
                                writer.close();

                                trajShow = new TrajectoryMeta[indexAry.length];
                                for (int i = 0; i < indexAry.length; i++) {
                                    int trajId = Integer.parseInt(indexAry[i]);
                                    if (trajId > 0)
                                        trajShow[i] = trajMetaFull[trajId];
                                }
                            }

                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    trajShow = new TrajectoryMeta[qisIndex[qisId].length];
                    for (int i = 0; i < qisIndex[qisId].length; i++) {
                        int trajId = qisIndex[qisId][i];
                        if (trajId > 0)
                            trajShow[i] = trajMetaFull[trajId];
                    }
                    System.out.println(qisId);

                }
                GL3 gl3;
                gl3 = ((PJOGL) beginPGL()).gl.getGL3();
                endPGL();
                float[] vertexData = vertexInit(trajShow);
                drawGPU(gl3, vertexData);
                if (!isClient) {
                    saveFrame("data/picture/graduation/detailed/" + qisId + ".png");
                    noLoop();
                }
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

        PApplet.main(new String[]{KernelMatrixRender.class.getName()});
    }
}
