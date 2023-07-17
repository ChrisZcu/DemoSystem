package tmp;

import app.TimeProfileSharedObject;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.providers.MapBox;
import de.fhpotsdam.unfolding.utils.MapUtils;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import index.QuadTree;
import model.Position;
import model.TrajToSubpart;
import model.TrajectoryMeta;
import processing.core.PApplet;
import processing.core.PImage;
import processing.event.MouseEvent;
import processing.opengl.PJOGL;
import util.PSC;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

public class RegionShow extends PApplet {
    private UnfoldingMap map;
    private static TrajectoryMeta[] trajMetaFull;
    private static TrajectoryMeta[] trajShow;

    //    private String filePath = PSC.portoPath;
//    private String filePath = PSC.PORTO_KERNEL_REGION;
//        private String filePath = "E:\\zcz\\dbgroup\\JavaGPU\\data\\kernel_matrix\\0.01\\kernel_matrix_no_pixel_no_dup.txt";
    private String filePath = PSC.szPath;
    private TrajToSubpart[] trajVQGS;

    public void settings() {
        size(1200, 800, P2D);
    }

    public void setup() {
        map = new UnfoldingMap(this, new MapBox.CustomMapBoxProvider(PSC.WHITE_MAP_PATH));
        map.setZoomRange(0, 20);
        map.setBackgroundColor(255);
//        map.zoomAndPanTo(12, new Location(41.17129, -8.559485));
        map.zoomAndPanTo(11, PSC.szCenter);
        MapUtils.createDefaultEventDispatcher(this, map);

        new Thread(() -> {
            try {

                //porto
//                double minLon = -8.76514;
//                double minLat = 41.06809;
//                double maxLon = -8.35452;
//                double maxLat = 41.27406;

                //chengdu
                double minLon = 103.86;
                double minLat = 30.54;
                double maxLon = 104.272;
                double maxLat = 30.777;

                //shenzhen
                minLon = 113.617;
                minLat = 22.375;
                maxLon = 114.441;
                maxLat = 22.882;

                trajMetaFull = QuadTree.loadData(new double[4], filePath);

                TimeProfileSharedObject.getInstance().trajMetaFull = trajMetaFull;
                trajShow = trajMetaFull;
//                trajShow = getRandomTraj(0.01, trajMetaFull);
//                trajShow = QuadTree.loadData(new double[4], "data/tmp/porto_region_random.txt");

//                {
//                trajVQGS = VfgsForIndexPart.getVfgs(trajMetaFull, 0);//计算quality按照无delta计算
//                BufferedWriter writer = new BufferedWriter(new FileWriter("data/kernelMatrix/porto_test_vqgs0.txt"));
//                BufferedWriter writer = new BufferedWriter(new FileWriter("data/kernelMatrix/porto_test_random0.01.txt"));
//                {
//                    for (TrajToSubpart traj : trajVQGS) {
//                        StringBuilder line = new StringBuilder(";");
//                        int trajId = traj.getTrajId();
//                        if (trajMetaFull[trajId].getPositions().length == 0) {
//                            continue;
//                        }
//                        for (int i = 0; i < trajMetaFull[trajId].getPositions().length - 1; i++) {
//                            line.append(trajMetaFull[trajId].getPositions()[i]).append(",");
//                        }
//                        line.append(trajMetaFull[trajId].getPositions()[trajMetaFull[trajId].getPositions().length - 1]);
//                        writer.write(line.toString());
//                        writer.newLine();
//                    }
//                    writer.close();
//                }

//                for (TrajectoryMeta traj : trajShow) {
//                    int trajId = traj.getTrajId();
//                    if (trajMetaFull[trajId].getPositions().length == 0) {
//                        continue;
//                    }
//                    StringBuilder line = new StringBuilder(trajId + ";");
//                    for (int i = 0; i < trajMetaFull[trajId].getPositions().length - 1; i++) {
//                        line.append(trajMetaFull[trajId].getPositions()[i]).append(",");
//                    }
//                    line.append(trajMetaFull[trajId].getPositions()[trajMetaFull[trajId].getPositions().length - 1]);
//                    writer.write(line.toString());
//                    writer.newLine();
//                }
//                writer.close();
//                }
                regionProcess(minLat, maxLat, minLon, maxLon);

                trajMetaFull = QuadTree.loadData(new double[4], "data/kernelMatrix/shenzhen/shenzhen_region.txt");
                TimeProfileSharedObject.getInstance().trajMetaFull = trajMetaFull;
                trajShow = trajMetaFull;
                isDataLoadDone = true;
                System.out.println("total load done: " + trajShow.length);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public TrajectoryMeta[] getRandomTraj(int totalScore, TrajectoryMeta[] trajMetaFull) {
        ArrayList<TrajectoryMeta> res = new ArrayList<>();
        Random ran = new Random(0);
        HashSet<Position> influSet = new HashSet<>();
        HashSet<Integer> idSet = new HashSet<>();
        while (influSet.size() < totalScore) {
            int trajId = ran.nextInt(trajMetaFull.length);
            if (!idSet.contains(trajId)) {
                influSet.addAll(Arrays.asList(trajMetaFull[trajId].getPositions()));
                res.add(trajMetaFull[trajId]);
                idSet.add(trajId);
            }
        }
        return res.toArray(new TrajectoryMeta[0]);
    }


    public static TrajectoryMeta[] getRandomTraj(double rate, TrajectoryMeta[] trajMetaFull) {
        ArrayList<TrajectoryMeta> res = new ArrayList<>();
        Random ran = new Random(0);
        HashSet<Integer> idSet = new HashSet<>();
        int num = 0;
        while (num < (trajMetaFull.length * rate)) {
            int trajId = ran.nextInt(trajMetaFull.length);
            if (!idSet.contains(trajId)) {
                idSet.add(trajId);
                num++;
                res.add(trajMetaFull[trajId]);
            }
        }
        return res.toArray(new TrajectoryMeta[0]);
    }

    public static int[] getRandomTrajId(double rate, TrajectoryMeta[] trajMetaFull) {
        int[] res = new int[(int) (trajMetaFull.length * rate)];
        Random ran = new Random(0);
        HashSet<Integer> idSet = new HashSet<>();
        int num = 0;
        while (num < (int) (trajMetaFull.length * rate)) {
            int trajId = ran.nextInt(trajMetaFull.length);
            if (!idSet.contains(trajId)) {
                idSet.add(trajId);
                res[num] = trajId;
                num++;
            }
        }
        return res;
    }

    private int getTotalScore(TrajectoryMeta[] trajMetaFull) {
        HashSet<Position> influSet = new HashSet<>();
        for (TrajectoryMeta traj : trajMetaFull) {
            influSet.addAll(Arrays.asList(traj.getPositions()));
        }
        return influSet.size();
    }

    private void regionProcess(double minLat, double maxLat, double minLon, double maxLon) {
        try {
            TimeProfileSharedObject.getInstance().trajMetaFull = trajMetaFull;
            QuadTree.trajMetaFull = trajMetaFull;
            System.out.println("total load done: " + trajMetaFull.length);
            TrajectoryMeta[] trajRegion = QuadTree.getWayPointPos(trajMetaFull, minLat, maxLat, minLon, maxLon);
            BufferedWriter writer = new BufferedWriter(new FileWriter("data/kernelMatrix/shenzhen/shenzhen_region.txt"));
            for (TrajectoryMeta traj : trajRegion) {
                StringBuilder line = new StringBuilder(";");
                int trajId = traj.getTrajId();
                for (int i = traj.getBegin(); i < traj.getEnd(); i++) {
                    line.append(trajMetaFull[trajId].getPositions()[i]).append(",");
                }
                line.append(trajMetaFull[trajId].getPositions()[traj.getEnd()]);
                writer.write(line.toString());
                writer.newLine();
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isDataLoadDone = false;
    private PImage mapImage = null;
    private int zoomCheck = -1;
    private Location centerCheck = new Location(-1, -1);

    @Override
    public void mousePressed() {
        System.out.println(map.getLocation(0, 0));
        System.out.println(map.getLocation(1200, 0));
        System.out.println(map.getLocation(0, 800));
        Location loc = map.getLocation(mouseX, mouseY);
        System.out.print(loc + ">>>>>");
        System.out.println(loc.getLon() + ", " + loc.getLat());
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
            if (isDataLoadDone) {
                GL3 gl3;
                gl3 = ((PJOGL) beginPGL()).gl.getGL3();
                endPGL();
                float[] vertexData = vertexInit(trajShow);
                drawGPU(gl3, vertexData);
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
        PApplet.main(new String[]{RegionShow.class.getName()});
    }
}