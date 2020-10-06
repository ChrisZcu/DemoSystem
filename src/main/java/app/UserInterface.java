package app;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.providers.MapBox;
import de.fhpotsdam.unfolding.utils.MapUtils;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import draw.TrajDrawManagerSingleMap;
import model.*;
import processing.core.PApplet;
import processing.core.PGraphics;
import select.TimeProfileManager;
import util.PSC;
import util.VFGS;

import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class UserInterface extends PApplet {

    String partTrajFilePath = "data/GPS/Porto5w/Porto5w.txt";
    String totalFilePath = "data/GPS/porto_full.txt";

    String filePath = totalFilePath;
    UnfoldingMap map;
    UnfoldingMap mapClone;
    long t1, t2, t4;

    private boolean regionDrawing = false;
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

    String[] tasks = new String[]{"Global - FULL", "Global - Random (rate=0.01)", "Global - VFGS (rate=0.01, delta=0)",
            "Global - VFGS+ (rate=0.01, delta=32)", "Local – Full", "Local – Random (rate=0.01)",
            "Local – VFGS (rate=0.01, delta=0)", "Local – VFGS+ (rate=0.01, delta=32)"};

    //    Location[] regions = new Location[]{
//            new Location(41.315205, -8.629877),
//            new Location(41.275997, -8.365519),
//            new Location(41.1882, -8.35178),
//            new Location(41.044403, -8.470575),
//            new Location(40.971338, -8.591425),
//
//            new Location(41.198544, -8.677942),
//            new Location(41.213013, -8.54542),
//            new Location(41.137554, -8.596918)
//    };
//    Location[] regions = new Location[]{
//            new Location(41.39543, -8.469396),
//            new Location(41.371895, -8.264764),
//            new Location(41.450687, -8.4920435),
//            new Location(41.25237, -8.38321),
//            new Location(41.265537, -8.276437),
//            new Location(41.21856, -8.155588)
//    };
    Location[] regions = new Location[]{
            //new Location(41.2005, -8.310094),
            new Location(41.143646, -8.63213),
            new Location(41.191044, -8.522085)
    };

    double[] rates = new double[]{0.01, 0.05, 0.1, 0.2, 0.4};

    int progress = 2;
    int regionNum = 0;
    int rateNum = 0;

    boolean capture = false;

    @Override
    public void settings() {
        //size(1000, 800, P2D);
        fullScreen(P2D);
    }
    //15

    private int ZOOMLEVEL = 11;
    private Location PRESENT = new Location(41.151, -8.634)/*new Location(41.206, -8.627)*/;
    private boolean loadDone = false;

    RectRegion[] r = new RectRegion[6];

    @Override
    public void setup() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        System.out.println((int) screenSize.getWidth());//2560
        System.out.println((int) screenSize.getHeight());//1440
        map = new UnfoldingMap(this, new MapBox.CustomMapBoxProvider(PSC.WHITE_MAP_PATH));
        map.setZoomRange(0, 20);
        map.setBackgroundColor(255);
        map.zoomAndPanTo(ZOOMLEVEL, PRESENT);
        map.setTweening(true);

        mapClone = new UnfoldingMap(this, new MapBox.CustomMapBoxProvider(PSC.WHITE_MAP_PATH));
        mapClone.setZoomRange(0, 20);
        mapClone.zoomAndPanTo(ZOOMLEVEL, PRESENT);

        MapUtils.createDefaultEventDispatcher(this, map);


        initButton();

        new Thread() {
            @Override
            public void run() {
                loadTotalData(filePath);
                loadDone = true;
            }
        }.start();

//        for (int i = 0; i < 6; ++i) {
//            ScreenPosition center = map.getScreenPosition(regions[i]);
//
//            double lenth = 79.9, width = 44.9;
//            Position lt = new Position(center.x - lenth, center.y - width);
//            Position rb = new Position(center.x + lenth, center.y + width);
//
//            RectRegion selectRegion = new RectRegion(lt, rb);
//            selectRegion.color = PSC.COLOR_LIST[1];
//
//
//            selectRegion.initLoc(map.getLocation(selectRegion.leftTop.x, selectRegion.leftTop.y),
//                    map.getLocation(selectRegion.rightBtm.x, selectRegion.rightBtm.y));
//            r[i] = selectRegion;
//        }
    }

    boolean cleanTime = true;

    @Override
    public void draw() {
        if (!map.allTilesLoaded()) {
            map.draw();
        } else {
            if (cleanTime) {
                map.draw();
            }
            if (regionDrawing) {
                rectRegion = getSelectRegion(lastClick);
            }

            drawRecRegion();

            if (TimeProfileSharedObject.getInstance().calDone) {
                trajShow = TimeProfileSharedObject.getInstance().trajShow;
                TrajDrawManagerSingleMap trajManager = new TrajDrawManagerSingleMap(trajShow, 1, this, map);
                trajManager.startDraw();
                TimeProfileSharedObject.getInstance().calDone = false;
                System.out.println(">>>>way point time: " + wayPointCalTime +
                        " ms\n" + ">>>>VFGS time: " + algTime + " ms");
            }
            //draw traj
            long t3 = System.currentTimeMillis();
            drawTrajCPU();
            drawTime = System.currentTimeMillis() - t3;

            //drawComponent();
        }
//        for(int i = 0;i<6;++i){
//            noFill();
//            strokeWeight(2);
//            stroke(new Color(19, 149, 186).getRGB());
//
//            ScreenPosition src1 = map.getScreenPosition(r[i].getLeftTopLoc());
//            ScreenPosition src2 = map.getScreenPosition(r[i].getRightBtmLoc());
//            rect(src1.x, src1.y, src2.x - src1.x, src2.y - src1.y);
//        }
    }

    Trajectory[] trajShow = new Trajectory[0];
    RectRegion rectRegion;
    Position lastClick;

    @Override
    public void mousePressed() {
        if (mouseButton == RIGHT) {
            regionDrawing = true;
            TimeProfileSharedObject.getInstance().trajImageMtx = new PGraphics[0];
            lastClick = new Position(mouseX, mouseY);
        } else {
            buttonClickListener();
        }
    }

    @Override
    public void mouseReleased() {
        if (regionDrawing) {
            regionDrawing = false;
            rectRegion = getSelectRegion(lastClick);
            //System.out.println(rectRegion);
        }
        if (panning || zoom) {
            panning = false;
            zoom = false;
            finishClick(4);
        }

    }

    private boolean panning = false;

    @Override
    public void mouseDragged() {
        if (mouseButton == LEFT) {
            panning = true;

            TimeProfileSharedObject.getInstance().trajImageMtx = new PGraphics[0];
        }
    }

    boolean zoom = false;

    @Override
    public void mouseWheel() {
        zoom = true;
        TimeProfileSharedObject.getInstance().trajImageMtx = new PGraphics[0];
    }

    @Override
    public void keyReleased() {
        consumeChangeDataSetOpt();
    }

    private long wayPointCalTime = 0L;
    public static long algTime = 0L;

    private long drawTime = 0L;

    private void finishClick(int kind) {
        if (!loadDone) {
            System.out.println("!!!!!!Data not done, wait....");
            return;
        }
        if (rectRegion == null) {
            TimeProfileSharedObject.getInstance().trajShow = totalTrajector;
            TimeProfileSharedObject.getInstance().calDone = true;
            return;
        }
        //changeMap();
        new Thread() {
            @Override
            public void run() {
                System.out.println("calculating....");
                long tt1 = System.currentTimeMillis();
                long tt2;

                switch (kind) {
                    case 1:
                        //Global - FULL – Region
                        TimeProfileSharedObject.getInstance().trajShow = VFGS.cut(totalTrajector, rectRegion);
                        tt2 = System.currentTimeMillis();
                        algTime = tt2 - tt1;
                        wayPointCalTime = 0;
                        System.out.println("waypoint traj num>>>" + trajShow.length);
                        break;
                    case 2:
                        //Global - Random – Region
                        TimeProfileSharedObject.getInstance().trajShow = VFGS.cut(VFGS.getCellCover(totalTrajector, 0.01), rectRegion);
                        tt2 = System.currentTimeMillis();
                        algTime = tt2 - tt1;
                        break;
                    case 3:
                        //Global - VFGS – Region
                        TimeProfileSharedObject.getInstance().trajShow = VFGS.cut(VFGS.getCellCover(totalTrajector, mapClone, 0.01, 0), rectRegion);
                        tt2 = System.currentTimeMillis();
                        algTime = tt2 - tt1;
                        break;
                    case 4:
                        //Global - VFGS+ – Region
                        TimeProfileSharedObject.getInstance().trajShow = VFGS.cut(VFGS.getCellCover(totalTrajector, mapClone, 0.005, 64), rectRegion);
                        tt2 = System.currentTimeMillis();
                        algTime = tt2 - tt1;
                        break;
                    case 5:
                        //Local – Full – Region
                        TimeProfileSharedObject.getInstance().trajShow = VFGS.cut(getWayPoint().toArray(new Trajectory[0]), rectRegion);
                        tt2 = System.currentTimeMillis();
                        algTime = tt2 - tt1;
                        break;
                    case 6:
                        //Local – Random – Region
                        TimeProfileSharedObject.getInstance().trajShow = VFGS.cut(VFGS.getCellCover(getWayPoint().toArray(new Trajectory[0]), 0.01), rectRegion);
                        tt2 = System.currentTimeMillis();
                        algTime = tt2 - tt1;
                        break;
                    case 7:
                        //Local – VFGS – Region
//                        Trajectory[] trajFull = getWayPoint().toArray(new Trajectory[0]);
//                        double rate = (double) 2312 / trajFull.length;
//                        System.out.println((regionNum + 1) + " " + progress + " " + rate);
                        TimeProfileSharedObject.getInstance().trajShow = VFGS.cut(VFGS.getCellCover(getWayPoint().toArray(new Trajectory[0]), mapClone, 0.01, 0), rectRegion);
                        tt2 = System.currentTimeMillis();
                        algTime = tt2 - tt1;
                        break;
                    case 8:
                        //Local – VFGS+ – Region
//                        Trajectory[] trajFull1 = getWayPoint().toArray(new Trajectory[0]);
//                        double rate1 = (double) 2312 / trajFull1.length;
//                        System.out.println((regionNum + 1) + " " + progress + " " + rate1);
                        TimeProfileSharedObject.getInstance().trajShow = VFGS.cut(VFGS.getCellCover(getWayPoint().toArray(new Trajectory[0]), mapClone, 0.005, 64), rectRegion);
                        tt2 = System.currentTimeMillis();
                        algTime = tt2 - tt1;
                        break;
                }

                TimeProfileSharedObject.getInstance().calDone = true;
            }
        }.start();
    }

    private ArrayList<Trajectory> getWayPoint() {
        long t1 = System.currentTimeMillis();
        startCalWayPoint();
        long t2 = System.currentTimeMillis();
        wayPointCalTime = t2 - t1;

        ArrayList<Trajectory> trajShows = new ArrayList<>();
        for (Trajectory[] trajList : TimeProfileSharedObject.getInstance().trajRes) {
            Collections.addAll(trajShows, trajList);
        }
        return trajShows;
    }

    private void drawRecRegion() {
        if (rectRegion == null)
            return;
        noFill();
        strokeWeight(2);
        stroke(new Color(19, 149, 186).getRGB());

        ScreenPosition src1 = map.getScreenPosition(rectRegion.getLeftTopLoc());
        ScreenPosition src2 = map.getScreenPosition(rectRegion.getRightBtmLoc());
        rect(src1.x, src1.y, src2.x - src1.x, src2.y - src1.y);
    }

    private void drawTrajCPU() {
        if (TimeProfileSharedObject.getInstance().trajImageMtx == null) {
            return;
        }
        for (PGraphics pg : TimeProfileSharedObject.getInstance().trajImageMtx) {
            if (pg == null) {
                continue;
            }
            image(pg, 0, 0);
        }
    }

    EleButton[] dataButtonList = new EleButton[0];

    private void initButton() {
        dataButtonList = new EleButton[2];
        int dataButtonXOff = 4;
        int dataButtonYOff = 4;
        dataButtonList[0] = new EleButton(dataButtonXOff, dataButtonYOff + 5, 70, 20, 0, "Finish");
        dataButtonList[1] = new EleButton(dataButtonXOff, dataButtonYOff + 35, 70, 20, 1, "All");

    }

    private void drawComponent() {
        for (EleButton eleButton : dataButtonList) {
            eleButton.render(this);
        }
    }

    boolean finishClick = false;

    private void buttonClickListener() {
        // not in one map mode, now there are 4 map in the map
        int eleId = -1;
        for (EleButton dataButton : dataButtonList) {
            if (dataButton.isMouseOver(this, true)) {
                eleId = dataButton.getEleId();
                break;
            }
        }
        if (eleId != -1) {
            if (eleId == 0) {//finish
                finishClick(4);
                finishClick = true;
            } else if (eleId == 1) {
                rectRegion = null;
                finishClick(4);
                finishClick = true;
            }
        }
    }

    private RectRegion getSelectRegion(Position lastClick) {
        ScreenPosition center = map.getScreenPosition(regions[0]);

        double lenth = 79.9, width = 44.9;
        Position lt = new Position(center.x - lenth, center.y - width);
        Position rb = new Position(center.x + lenth, center.y + width);

//        if (lastClick.x < curClick.x) {//left
//            if (lastClick.y < curClick.y) {//up
//                selectRegion.leftTop = lastClick;
//                selectRegion.rightBtm = curClick;
//            } else {//left_down
//                Position left_top = new Position(lastClick.x, curClick.y);
//                Position right_btm = new Position(curClick.x, lastClick.y);
//                selectRegion = new RectRegion(left_top, right_btm);
//            }
//        } else {//right
//            if (lastClick.y < curClick.y) {//up
//                Position left_top = new Position(curClick.x, lastClick.y);
//                Position right_btm = new Position(lastClick.x, curClick.y);
//                selectRegion = new RectRegion(left_top, right_btm);
//            } else {
//                selectRegion = new RectRegion(curClick, lastClick);
//            }
//        }

        RectRegion selectRegion = new RectRegion(lt, rb);
        selectRegion.color = PSC.COLOR_LIST[1];


        selectRegion.initLoc(map.getLocation(selectRegion.leftTop.x, selectRegion.leftTop.y),
                map.getLocation(selectRegion.rightBtm.x, selectRegion.rightBtm.y));

        return selectRegion;
    }

    private RectRegion getSelectRegion(int i) {
        ScreenPosition center = map.getScreenPosition(regions[i]);

        double lenth = 79.9, width = 44.9;
        Position lt = new Position(center.x - lenth, center.y - width);
        Position rb = new Position(center.x + lenth, center.y + width);

        RectRegion selectRegion = new RectRegion(lt, rb);
        selectRegion.color = PSC.COLOR_LIST[1];


        selectRegion.initLoc(map.getLocation(selectRegion.leftTop.x, selectRegion.leftTop.y),
                map.getLocation(selectRegion.rightBtm.x, selectRegion.rightBtm.y));

        return selectRegion;
    }

    //below for way-point cal
    private void startCalWayPoint() {
        TimeProfileManager tm = new TimeProfileManager(1, totalTrajector, rectRegion);
        tm.startRun();
    }

    private Trajectory[] totalTrajector;
    private Trajectory[] vfgsTrajector;
//    private int[] trajScore;

    private void loadTotalData(String filePath) {
        System.out.println("data pre-processing......");
        ArrayList<String> totalTraj = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            int limit = 50000;
            int cnt = 0;
            while ((line = reader.readLine()) != null /*&& cnt < limit*/) {
                totalTraj.add(line);
                cnt++;
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        totalTrajector = new Trajectory[totalTraj.size()];
//        trajScore = new int[totalTraj.size()];
        int i = 0;
        for (String trajStr : totalTraj) {
            String[] item = trajStr.split(";");
//            trajScore[i] = Integer.parseInt(item[0]);
            String[] trajPoint = item[1].split(",");

            Trajectory traj = new Trajectory(i);
            Location[] locations = new Location[trajPoint.length / 2 - 1];
            for (int j = 0; j < trajPoint.length - 2; j += 2) {
                locations[j / 2] = new Location(Float.parseFloat(trajPoint[j + 1]), Float.parseFloat(trajPoint[j]));
            }
            traj.setLocations(locations);
            traj.setScore(Integer.parseInt(item[0]));
            totalTrajector[i++] = traj;
        }
        System.out.println(totalTrajector.length);
        System.out.println("data preprocess done");
    }

    private void consumeChangeDataSetOpt() {
        String fileKind = ".tif";
        switch (key) {
            case 'm':
                map.zoomTo(11);
                regionNum++;
                rectRegion = getSelectRegion(regionNum);
                progress = 2;
                finishClick(progress);
                System.out.println(regionNum + " " + progress);
                break;
            case 'n':
//                nextPic();
//                break;
                progress++;
                finishClick(progress);
                System.out.println((regionNum + 1) + " " + progress);
                break;
            case 's':
                String path = "D:\\arslanaWu\\VFGS\\DemoSystem\\output\\region" + (regionNum + 1) + "\\" + tasks[progress - 1] + fileKind;
                saveFrame(path);
                System.out.println((regionNum + 1) + " " + progress + " saved");
                break;
            case 'p':
                ScreenPosition lt = map.getScreenPosition(rectRegion.getLeftTopLoc());
                ScreenPosition rb = map.getScreenPosition(rectRegion.getRightBtmLoc());
                Location loc = map.getLocation(lt.x + (rb.x - lt.x) / 2, lt.y + (rb.y - lt.y) / 2);

                map.panTo(loc);
                break;
            case 'z':
                map.zoomTo(11);
                break;
            case 'x':
                map.zoomTo(15);
                break;
        }

    }

    public void changeMap() {
        map.zoomTo(15);

        ScreenPosition lt = map.getScreenPosition(rectRegion.getLeftTopLoc());
        ScreenPosition rb = map.getScreenPosition(rectRegion.getRightBtmLoc());
        Location loc = map.getLocation(lt.x + (rb.x - lt.x) / 2, lt.y + (rb.y - lt.y) / 2);

        map.panTo(loc);
    }

//    public void nextPic() {
//        map.zoomTo(11);
//
//        if (progress == 8) {
//            map.zoomTo(11);
//            regionNum++;
//            rectRegion = getSelectRegion(regionNum);
//            progress = 2;
//            finishClick(progress);
//            System.out.println(regionNum + " " + progress);
//        } else {
//            progress++;
//            finishClick(progress);
//            System.out.println((regionNum + 1) + " " + progress);
//        }
//
//    }
//
//    public void savePic() {
//        String path = "D:\\arslanaWu\\VFGS\\DemoSystem\\output\\region" + (regionNum + 1) + "\\" + tasks[progress - 1] + ".tif";
//        saveFrame(path);
//        System.out.println((regionNum + 1) + " " + progress + " saved");
//    }

    public static void main(String[] args) {

        PApplet.main(new String[]{UserInterface.class.getName()});
    }
}

