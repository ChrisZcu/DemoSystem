package app;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.providers.MapBox;
import de.fhpotsdam.unfolding.utils.MapUtils;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import model.RectRegion;
import model.Trajectory;
import model.TrajectoryMeta;
import processing.core.PApplet;
import processing.core.PImage;
import processing.opengl.PJOGL;
import select.TimeProfileManager;
import util.PSC;

import java.io.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;

public class VFGSSampling extends PApplet {
    UnfoldingMap map;
    UnfoldingMap mapClone;

    @Override
    public void settings() {
        size(1000, 800, P2D);
        PJOGL.profile = 3;
    }

    @Override
    public void setup() {
        map = new UnfoldingMap(this, new MapBox.CustomMapBoxProvider(PSC.WHITE_MAP_PATH));
        map.setZoomRange(0, 20);
        map.zoomAndPanTo(ZOOMLEVEL, PRESENT);
        map.setBackgroundColor(255);
        MapUtils.createDefaultEventDispatcher(this, map);

        mapClone = new UnfoldingMap(this, new MapBox.CustomMapBoxProvider(PSC.WHITE_MAP_PATH));
        mapClone.setZoomRange(0, 20);
        mapClone.zoomAndPanTo(20, PRESENT);

        sparestLoad();
//        vfqsSampling();
        System.out.println("done");
        isDataLoadDone = true;
        exit();
    }

    public void sparestLoad() {
        ArrayList<int[]> scores = sparestLoad(inputPath);
        scores.sort(new Comparator<int[]>() {
            @Override
            public int compare(int[] a, int[] b) {
                return -a[0] + b[0];
            }
        });
        util.Util.storeSparest("data/vfgs/tkde_revision/reviewer1/half_sparest_0.01.txt", scores, 0.01);
    }

    public void vfqsSampling() {
        loadData(inputPath);
//        Trajectory[] trajectories = loadVfgs("data/GPS/dwt_24k.txt", 0.001);
        Trajectory[] trajList = util.VFGS.getCellCover(trajFull, mapClone, 0.5, delta);
        String writePath = outPath;
        util.Util.storeVQGSRes(writePath, trajList);
    }

    private int ZOOMLEVEL = 11;
    private int rectRegionID = 0;
    private double recNumId = 1.0 / 64;
    private Location[] rectRegionLoc = {new Location(41.315205, -8.629877), new Location(41.275997, -8.365519),
            new Location(41.198544, -8.677942), new Location(41.213013, -8.54542),
            new Location(41.1882, -8.35178), new Location(41.137554, -8.596918),
            new Location(41.044403, -8.470575), new Location(40.971338, -8.591425)};
    private Location PRESENT = rectRegionLoc[rectRegionID]/*new Location(41.206, -8.627)*/;
    private int alg = 2;
    private double[] rate = {0.01, 0.005, 0.001, 0.0005, 0.0001, 0.00005, 0.00001};
    private int rateId = 0;
    private int[] deltaList = {0, 4, 8, 16, 32, 64};
    private int deltaId = 0;

    private RectRegion rectRegion = new RectRegion();
    private Location centerCheck = new Location(-1, -1);

    private PImage mapImage = null;

    private int zoomCheck = -1;
    private boolean isDataLoadDone = false;

//    @Override
//    public void draw() {
//        if (!(zoomCheck == map.getZoomLevel() && centerCheck.equals(map.getCenter()))) {
//            map.draw();
//            if (!map.allTilesLoaded()) {
//                if (mapImage == null) {
//                    mapImage = map.mapDisplay.getInnerPG().get();
//                }
//                image(mapImage, 0, 0);
//            } else {
//                zoomCheck = map.getZoomLevel();
//                centerCheck = map.getCenter();
//                System.out.println("load map done");
//                map.draw();
//                map.draw();
//                map.draw();
//                map.draw();
//            }
//        } else {
//            if (isDataLoadDone) {
//                GL3 gl3;
//                gl3 = ((PJOGL) beginPGL()).gl.getGL3();
//                endPGL();
//                float[] vertexData = vertexInit(trajShow);
//                drawGPU(gl3, vertexData);
//
//                saveFrame("data/picture/graduation/dtw/porto.png");
//                noLoop();
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


    boolean noLoop = false;
    boolean thread = false;

    @Override
    public void keyPressed() {
        if (key == 'q') {
            trajShow.clear();
            noLoop = false;
            thread = false;
            loop();
        } else if (key == 'w') {
            Collections.addAll(trajShow, trajFull);
            noLoop = true;
        } else if (key == 'e') {
            float x = (float) (500 * recNumId);
            float y = (float) (400 * recNumId);
            rectRegion.setLeftTopLoc(map.getLocation(500 - x, 400 - y));
            rectRegion.setRightBtmLoc(map.getLocation(500 + x, 400 + y));
            System.out.println(rectRegion.getLeftTopLoc());
            System.out.println(rectRegion.getRightBtmLoc());

            System.out.println("using: " + recNumId);
            recNumId *= 2;

            startCalWayPoint();

            trajShow.clear();
            noLoop = true;
            thread = true;
            loop();
        }
        System.out.println(key);
    }

    Trajectory[] trajFull;
    ArrayList<Trajectory> trajShow = new ArrayList<>();

    public int comp(int[] a, int[] b) {
        return a[0] > b[0] ? 1 : 0;
    }

    private ArrayList<int[]> sparestLoad(String filePath) {
        ArrayList<int[]> scores = new ArrayList<>();
        try {
            ArrayList<String> trajStr = new ArrayList<>(2400000);
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = reader.readLine()) != null) {
                trajStr.add(line);
            }
            reader.close();
            System.out.println("load done");
            int j = 0;
            for (String traj : trajStr) {
                int score = Integer.parseInt(traj.split(";")[0]);
                scores.add(new int[]{score, j++});
            }
            trajStr.clear();
            System.out.println("load done");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return scores;
    }


    private void loadData(String filePath) {
        try {
            ArrayList<String> trajStr = new ArrayList<>(2400000);
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = reader.readLine()) != null) {
                trajStr.add(line);
            }
            reader.close();
            System.out.println("load done");
            int j = 0;

            trajFull = new Trajectory[trajStr.size()];

            for (String trajM : trajStr) {
                String[] item = trajM.split(";");
                String[] data = item[1].split(",");
                Trajectory traj = new Trajectory(j);
                ArrayList<Location> locations = new ArrayList<>();
//                Position[] metaGPS = new Position[data.length / 2 - 1];
                for (int i = 0; i < data.length - 2; i = i + 2) {
                    locations.add(new Location(Float.parseFloat(data[i + 1]),
                            Float.parseFloat(data[i])));
//                    metaGPS[i / 2] = new Position(Float.parseFloat(data[i + 1]), Float.parseFloat(data[i]));
                }
                traj.setLocations(locations.toArray(new Location[0]));
                traj.setScore(Integer.parseInt(item[0]));
//                traj.setMetaGPS(metaGPS);
                trajFull[j++] = traj;
            }
            trajStr.clear();
            System.out.println("load done");
            System.out.println("traj number: " + trajFull.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    Trajectory[] selectTrajList;

    private void startCalWayPoint() {
        TimeProfileManager tm = new TimeProfileManager(1, trajFull, rectRegion);
        tm.startRun();
    }

    int[][] VFGSIdList;
    int VFGS = 0;

    private Trajectory[] loadVfgs(String filePath, double rate) {
        int trajNum = (int) (trajFull.length * rate);
        ArrayList<String> idsSet = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            for (int i = 0; i < trajNum; i++) {
                line = reader.readLine();
                idsSet.add(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Trajectory[] trajectories = new Trajectory[trajNum];
        int i = 0;
        for (String str : idsSet) {
            trajectories[i++] = trajFull[Integer.parseInt(str.split(",")[0])];
        }
        return trajectories;
    }

    private int[][] loadVfgs(String filePath) {

        int[][] resId = new int[8][];
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            ArrayList<Integer> id = new ArrayList<>();
            int i = 0;
            while ((line = reader.readLine()) != null) {
                if (!line.contains(",")) {
                    resId[i] = new int[id.size()];
                    System.out.println(id.size());
                    int j = 0;
                    for (Integer e : id) {
                        resId[i][j++] = e;
                    }
                    i++;
                    id.clear();
                } else {
                    id.add(Integer.parseInt(line.split(",")[0]));
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resId;
    }

    private void loadTrajList() {
        selectTrajList = new Trajectory[VFGSIdList[VFGS].length];
        int i = 0;
        for (Integer e : VFGSIdList[VFGS]) {
            selectTrajList[i++] = trajFull[e];
        }
    }

    int[][] randomIdList;

    private void initRandomIdList() {
        randomIdList = new int[rate.length][];
        int i = 0;
        Random ran = new Random(1);
        HashSet<Integer> tmp = new HashSet<Integer>((int) (trajFull.length * 0.05));

        for (double rate : rate) {
            tmp.clear();
            int num = (int) (rate * trajFull.length);
            randomIdList[i] = new int[num];
            int j = 0;
            while (tmp.size() < num) {
                tmp.add(ran.nextInt(trajFull.length - 1));
            }
            for (Integer e : tmp) {
                randomIdList[i][j++] = e;
            }
            i++;
        }
    }

    private void loadRandomTrajList() {
        selectTrajList = new Trajectory[randomIdList[VFGS].length];
        int i = 0;
        for (Integer e : randomIdList[VFGS]) {
            selectTrajList[i++] = trajFull[e];
        }
    }

    GL3 gl3;
    int[] vboHandles;
    int shaderProgram, vertShader, fragShader;
    int vertexBufferObject;

    IntBuffer vao = GLBuffers.newDirectIntBuffer(1);
    float[] vertexData = {};

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
                                + "gl_Position.x = position.x / 500.0 - 1;"
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

    private void vertexInit() {
        int line_count = 0;
        for (Trajectory traj : trajShow) {
            line_count += (traj.locations.length);
        }
        vertexData = new float[line_count * 2 * 2];

        int j = 0;
        for (Trajectory traj : trajShow) {
            for (int i = 0; i < traj.locations.length - 2; i++) {
                ScreenPosition pos = map.getScreenPosition(traj.locations[i]);
                ScreenPosition pos2 = map.getScreenPosition(traj.locations[i + 1]);
                vertexData[j++] = pos.x;
                vertexData[j++] = pos.y;
                vertexData[j++] = pos2.x;
                vertexData[j++] = pos2.y;
            }
        }

    }

    private long[] drawCPU() {
        long[] timeList = new long[2];
        ArrayList<ArrayList<Point>> trajPointList = new ArrayList<>();
        long t0 = System.currentTimeMillis();

        for (Trajectory traj : trajShow) {
            ArrayList<Point> pointList = new ArrayList<>();
            for (Location loc : traj.getLocations()) {
                ScreenPosition pos = map.getScreenPosition(loc);
                pointList.add(new Point(pos.x, pos.y));
            }
            trajPointList.add(pointList);
        }
        timeList[0] = (System.currentTimeMillis() - t0);
        long t1 = System.currentTimeMillis();
        for (ArrayList<Point> traj : trajPointList) {
            beginShape();
            for (Point pos : traj) {
                vertex(pos.x, pos.y);
            }
            endShape();
        }
        timeList[1] = (System.currentTimeMillis() - t1);
        return timeList;
    }

    private Trajectory[] getRan(Trajectory[] trajectories, double rate) {
        int size = (int) (rate * trajectories.length);
        Trajectory[] res = new Trajectory[size];
        HashSet<Integer> set = new HashSet<>();
        Random ran = new Random();
        while (set.size() != size) {
            set.add(ran.nextInt(trajectories.length - 1));
        }
        int i = 0;
        for (Integer e : set) {
            res[i++] = trajectories[e];
        }
        return res;
    }

    public static String inputPath = "data/GPS/porto_full.txt";
//    public static String inputPath = "data/GPS/Porto5w/Porto5w.txt";

    public static String outPath = "data/vfgs/tkde_revision/reviewer1/half_sample.txt";
    static int delta = 0;

    public static void main(String[] args) {
        if (args.length > 2) {
            inputPath = args[0];
            outPath = args[1];
            delta = Integer.parseInt(args[2]);
        }
        PApplet.main(new String[]{VFGSSampling.class.getName()});
    }

    class Point {
        float x;
        float y;

        public Point(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}
