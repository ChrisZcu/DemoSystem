package app;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.providers.MapBox;
import de.fhpotsdam.unfolding.utils.MapUtils;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import draw.TrajDrawManager;
import draw.TrajDrawManagerSingleMap;
import javafx.geometry.Pos;
import model.*;
import org.lwjgl.Sys;
import processing.core.PApplet;
import processing.core.PGraphics;
import select.TimeProfileManager;
import util.PSC;
import util.VFGS;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.jar.JarOutputStream;

public class UserInterface extends PApplet {

    String partTrajFilePath = "data/GPS/Porto5w/Porto5w.txt";
    String totalFilePath = "data/GPS/porto_full.txt";

    String filePath = totalFilePath;
    UnfoldingMap map;
    private boolean regionDrawing = false;

    @Override
    public void settings() {
        size(1000, 800, P2D);
    }

    private int ZOOMLEVEL = 11;
    private Location PRESENT = new Location(41.151, -8.634)/*new Location(41.206, -8.627)*/;
    private boolean loadDone = false;

    @Override
    public void setup() {

        map = new UnfoldingMap(this, new MapBox.CustomMapBoxProvider(PSC.WHITE_MAP_PATH));
        map.setZoomRange(0, 20);
        map.setBackgroundColor(255);
        map.zoomAndPanTo(ZOOMLEVEL, PRESENT);

        MapUtils.createDefaultEventDispatcher(this, map);


        initButton();

        new Thread() {
            @Override
            public void run() {
                loadTotalData(filePath);
                loadDone = true;
            }
        }.start();
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
                System.out.println(">>>>way point time: " + wayPointCalTime  +
                        " ms\n" + ">>>>vfgs cal time: " + vfgsTime  + " ms");
            }
            //draw traj
            long t3 = System.currentTimeMillis();
            drawTrajCPU();
            drawTime = System.currentTimeMillis() - t3;


            drawComponent();
        }
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
        }
        if (panning || zoom) {
            panning = false;
            zoom = false;
            finishClick();
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

    private long wayPointCalTime = 0L;
    private long vfgsTime = 0L;
    private long drawTime = 0L;

    private void finishClick() {
        if (!loadDone) {
            System.out.println("!!!!!!Data not done, wait....");
            return;
        }
        if (rectRegion == null) {
            TimeProfileSharedObject.getInstance().trajShow = totalTrajector;
            TimeProfileSharedObject.getInstance().calDone = true;
            return;
        }
        new Thread() {
            @Override
            public void run() {
                System.out.println("calculating....");
                long t0 = System.currentTimeMillis();
                startCalWayPoint();
                wayPointCalTime = System.currentTimeMillis() - t0;

                long t1 = System.currentTimeMillis();
                ArrayList<Trajectory> trajShows = new ArrayList<>();
                for (Trajectory[] trajList : TimeProfileSharedObject.getInstance().trajRes) {
                    Collections.addAll(trajShows, trajList);
                }
                TimeProfileSharedObject.getInstance().trajShow = VFGS.getCellCover(trajShows.toArray(new Trajectory[0]), map, 0.01, rectRegion);
                TimeProfileSharedObject.getInstance().calDone = true;
                vfgsTime = System.currentTimeMillis() - t1;
            }
        }.start();
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
                finishClick();
                finishClick = true;
            } else if (eleId == 1) {
                rectRegion = null;
                finishClick();
                finishClick = true;
            }
        }
    }

    private RectRegion getSelectRegion(Position lastClick) {
        float mapWidth = 1000;
        float mapHeight = 800;

        float mx = constrain(mouseX, 3, mapWidth - 3);
        float my = constrain(mouseY, 3, mapHeight - 3);

        Position curClick = new Position(mx, my);
        RectRegion selectRegion = new RectRegion();
        if (lastClick.x < curClick.x) {//left
            if (lastClick.y < curClick.y) {//up
                selectRegion.leftTop = lastClick;
                selectRegion.rightBtm = curClick;
            } else {//left_down
                Position left_top = new Position(lastClick.x, curClick.y);
                Position right_btm = new Position(curClick.x, lastClick.y);
                selectRegion = new RectRegion(left_top, right_btm);
            }
        } else {//right
            if (lastClick.y < curClick.y) {//up
                Position left_top = new Position(curClick.x, lastClick.y);
                Position right_btm = new Position(lastClick.x, curClick.y);
                selectRegion = new RectRegion(left_top, right_btm);
            } else {
                selectRegion = new RectRegion(curClick, lastClick);
            }
        }
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
    private int[] trajScore;

    private void loadTotalData(String filePath) {
        System.out.println("data pre-processing......");
        ArrayList<String> totalTraj = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = reader.readLine()) != null) {
                totalTraj.add(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        totalTrajector = new Trajectory[totalTraj.size()];
        trajScore = new int[totalTraj.size()];
        int i = 0;
        for (String trajStr : totalTraj) {
            String[] item = trajStr.split(";");
            trajScore[i] = Integer.parseInt(item[0]);
            String[] trajPoint = item[1].split(",");

            Trajectory traj = new Trajectory(i);
            Location[] locations = new Location[trajPoint.length / 2 - 1];
            for (int j = 0; j < trajPoint.length - 2; j += 2) {
                locations[j / 2] = new Location(Float.parseFloat(trajPoint[j + 1]), Float.parseFloat(trajPoint[j]));
            }
            traj.setLocations(locations);
            totalTrajector[i++] = traj;
        }
        System.out.println(totalTrajector.length);
        System.out.println("data preprocess done");
    }

    public static void main(String[] args) {

        PApplet.main(new String[]{UserInterface.class.getName()});
    }
}

