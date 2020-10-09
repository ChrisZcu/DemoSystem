package app;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.providers.MapBox;
import de.fhpotsdam.unfolding.utils.MapUtils;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import model.Trajectory;
import processing.core.PApplet;
import processing.core.PImage;
import util.PSC;
import util.VFGS;

import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

public class ScreenShot extends PApplet {
    //    private Trajectory[] trajShow;
    UnfoldingMap map;
    UnfoldingMap mapClone;


    private String fullFile = "data/GPS/porto_full.txt";
    private String partFilePath = "data/GPS/Porto5w/Porto5w.txt";

    private String filePath = fullFile;

    int wight = 1200, hight = 800;

    @Override
    public void settings() {
        size(wight, hight, P2D);
    }

    private boolean isDataLoadDone = false;
    private Trajectory[] trajFull;

    @Override
    public void setup() {
        map = new UnfoldingMap(this, new MapBox.CustomMapBoxProvider(PSC.WHITE_MAP_PATH));
        map.setZoomRange(0, 20);
        map.setBackgroundColor(255);
        map.zoomAndPanTo(zoomLevel, centerList[curCenterId]);

        mapClone = new UnfoldingMap(this, new MapBox.CustomMapBoxProvider(PSC.WHITE_MAP_PATH));
        mapClone.setZoomRange(0, 20);
        mapClone.setBackgroundColor(255);
        mapClone.zoomAndPanTo(zoomLevel, centerList[curCenterId]);
        MapUtils.createDefaultEventDispatcher(this, map);

        new Thread() {
            @Override
            public void run() {
                trajFull = loadData(filePath);
                isDataLoadDone = true;
                System.out.println("Total data load done: " + trajFull.length);
                vfgsSet = new HashSet[2];

                vfgsSet[0] = loadVfgs("data/GPS/vfgs_0.txt", 0.01);
                vfgsSet[1] = loadVfgs("data/GPS/vfgs_32.txt", 0.01);

            }
        }.start();
    }

    @Override
    public void mousePressed() {
        System.out.println(map.getLocation(mouseX, mouseY));
    }

    int zoomCheck = -1;
    Location centerCheck = new Location(-1, -1);


    Location[] totalLocationList = {
            new Location(41.150, -8.639), new Location(41.183, -8.608),
            new Location(41.194, -8.665), new Location(41.164, -8.584),
            new Location(41.128, -8.611), new Location(41.202, -8.565),
            new Location(41.236, -8.623), new Location(41.182, -8.508),
            new Location(41.069, -8.644), new Location(41.075, -8.570),
            new Location(41.008, -8.642), new Location(40.997, -8.525),
            new Location(41.113, -8.491), new Location(41.234, -8.537),
            new Location(41.290, -8.682), new Location(41.330, -8.562),
            new Location(41.183, -8.409), new Location(41.276, -8.378),
            new Location(41.202, -8.324), new Location(41.067, -8.295),
            new Location(41.202, -8.301), new Location(41.207, -8.532)
    };
    //    private String[] locationName = new String[]{
//            "P0", "P6", "A", "B"
//    };
//    private Location[] centerList = {
//            new Location(41.150, -8.639), new Location(41.236, -8.623),
//            new Location(41.202, -8.301), new Location(41.207, -8.532)
//    };
    private Location[] centerList = {
            new Location(41.183, -8.608),
            new Location(41.194, -8.665), new Location(41.164, -8.584),
            new Location(41.128, -8.611), new Location(41.202, -8.565),
            new Location(41.236, -8.623), new Location(41.182, -8.508),
            new Location(41.069, -8.644), new Location(41.075, -8.570),
            new Location(40.997, -8.525),
            new Location(41.113, -8.491), new Location(41.234, -8.537),
            new Location(41.290, -8.682), new Location(41.330, -8.562),
            new Location(41.183, -8.409), new Location(41.276, -8.378),
            new Location(41.202, -8.324), new Location(41.067, -8.295),
    };
    String[] locationName = {
            "P1",
            "P2", "P3",
            "P4", "P5",
            "P6", "P7",
            "P8", "P9",
            "P11",
            "P12", "P13",
            "P14", "P15",
            "P16", "P17",
            "P18", "P19"
    };
    private PImage mapImage = null;


    private HashSet<Integer>[] vfgsSet;
    Trajectory[] trajShow;


    private boolean isGlobal = true;
    private int alg = 0;//0 for full, 1 for random, 2 for vfgs
    private int vfgsDeltaId = 0;
    private int curCenterId = 0;
    private int zoomLevel = 11;

    private int[] deltaList = {0, 32};

    @Override
    public void draw() {
        map.draw();
        if (!(zoomCheck == map.getZoomLevel() && centerCheck.equals(map.getCenter()))) {
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
                StringBuilder name = new StringBuilder();
                if (isGlobal) {
                    name.append("global_");
                    if (alg == 0) {//full
                        trajShow = trajFull;
                    } else if (alg == 1) {//random
                        trajShow = getRandom(trajFull, 0.01);
                    } else {
                        trajShow = getVfgsTraj(trajFull, vfgsSet[vfgsDeltaId]);
                    }
                } else {
                    name.append("local_");
                    Location leftTop = map.getLocation(0, 0);
                    Location rightBtm = map.getLocation(wight, hight);
                    Trajectory[] waypoint = getWayPointPos(trajFull, leftTop.getLat(), rightBtm.getLat(),
                            leftTop.getLon(), rightBtm.getLon());
                    if (alg == 0) {//full
                        trajShow = waypoint;
                    } else if (alg == 1) {//random
                        trajShow = getRandom(waypoint, 0.01);
                    } else {//vfgs
                        trajShow = VFGS.getCellCover(waypoint, mapClone, 0.01, deltaList[vfgsDeltaId]);
                    }
                }


                if (alg == 0) {
                    name.append("full_");
                } else if (alg == 1) {
                    name.append("random_");
                } else {
                    name.append("vfgs").append(deltaList[vfgsDeltaId]).append("_");

                }
                name.append("rate0.01_threshold_trajNo").append(trajShow.length).append("_");
                System.out.println("drawing......");
                drawTraj(trajShow);
                String picPath = "data/picture/20201009/" + zoomLevel + "/" + locationName[curCenterId] + "/"
                        + name.toString() + ".png";
                saveFrame(picPath);
                System.out.println(picPath + ", number: " + trajShow.length + " done");

                if (isRegionDone()) {
                    mapChage();
                }
            }
        }
    }

    private Trajectory[] getWayPointPos(Trajectory[] trajectory,
                                        double leftLat, double rightLat, double leftLon, double rightLon) {
        ArrayList<Trajectory> res = new ArrayList<>();
        for (Trajectory traj : trajectory) {
            for (Location location : traj.locations) {
                if (inCheck(location, leftLat, rightLat, leftLon, rightLon)) {
                    res.add(traj);
                    break;
                }
            }
        }
        return cutTrajsPos(res.toArray(new Trajectory[0]), leftLat, rightLat, leftLon, rightLon);
    }

    private Trajectory[] cutTrajsPos(Trajectory[] trajectories,
                                     double leftLat, double rightLat, double leftLon, double rightLon) {
        ArrayList<Trajectory> res = new ArrayList<>();
        for (Trajectory traj : trajectories) {
            res.addAll(getRegionInTrajPos(traj, leftLat, rightLat, leftLon, rightLon));
        }
        return res.toArray(new Trajectory[0]);
    }

    private ArrayList<Trajectory> getRegionInTrajPos(Trajectory traj,
                                                     double leftLat, double rightLat, double leftLon, double rightLon) {
        ArrayList<Trajectory> res = new ArrayList<>();
        for (int i = 0; i < traj.locations.length; i++) {
            if (inCheck(traj.locations[i], leftLat, rightLat, leftLon, rightLon)) {
                Trajectory trajTmp = new Trajectory(-1);
                Location location = traj.locations[i++];
                ArrayList<Location> locTmp = new ArrayList<>();
                while (inCheck(location, leftLat, rightLat, leftLon, rightLon) && i < traj.locations.length) {
                    locTmp.add(location);
                    location = traj.locations[i++];
                }
                trajTmp.locations = (locTmp.toArray(new Location[0]));
                trajTmp.setScore(locTmp.size());
                res.add(trajTmp);
            }
        }
        return res;
    }

    private boolean inCheck(Location loc, double leftLat, double rightLat, double leftLon, double rightLon) {
        return loc.getLat() >= Math.min(leftLat, rightLat) && loc.getLat() <= Math.max(leftLat, rightLat)
                && loc.getLon() >= Math.min(leftLon, rightLon) && loc.getLon() <= Math.max(leftLon, rightLon);
    }

    private Trajectory[] getVfgsTraj(Trajectory[] trajectories, HashSet<Integer> trajIdSet) {
        Trajectory[] res = new Trajectory[trajIdSet.size()];
        int i = 0;
        for (Integer e : trajIdSet) {
            res[i++] = trajectories[e];
        }
        return res;
    }

    private Trajectory[] getRandom(Trajectory[] trajectories, double rate) {
        Trajectory[] trajectory;
        int trajNum;
        if (isGlobal) {
            trajNum = (int) (trajectories.length * rate);
            trajectory = new Trajectory[trajNum];

        } else {
            trajNum = getLimit(trajectories.length, rate);
            trajectory = new Trajectory[trajNum];
        }
        Random random = new Random(0);
        for (int i = 0; i < trajNum; i++) {
            trajectory[i] = trajectories[random.nextInt(trajectories.length - 1)];
        }
        return trajectory;
    }

    private int getLimit(int length, double rate) {
        if (length < 2500)
            return length;
        else if (length * rate < 2500)
            return 2500;
        else
            return (int) (length * rate);
    }


    private boolean isRegionDone() {
        if (!isGlobal && alg == 2 && vfgsDeltaId == deltaList.length - 1) {
            alg = 0;
            vfgsDeltaId = 0;
            isGlobal = true;
            return true;
        }
        if (isGlobal) {
            if (alg < 2) { // global not done
                alg++;
            } else if (alg == 2) {
                if (vfgsDeltaId == deltaList.length - 1) {// global vfgs+32 0.01 done
                    alg = 0;
                    vfgsDeltaId = 0;
                    isGlobal = false;
                    return false;
                } else {
                    vfgsDeltaId++;//next delta
                }
            }
        }
        if (!isGlobal) {//global is done before, local here
            if (alg < 2) {//no vfgs run, full for one time and ran for one
                alg++;
                vfgsDeltaId = 0;// for delta 0 and 32
            } else if (alg == 2) {//vfgs has run before
                vfgsDeltaId++;
            }
        }
        return false;
    }

    private void mapChage() {
        if (curCenterId == centerList.length - 1) {
            if (zoomLevel == 19) {
                System.out.println("total done");
                exit();
            } else {
                zoomLevel++;
                curCenterId = 0;
            }
        } else {
            curCenterId++;
        }
        map.zoomAndPanTo(zoomLevel, centerList[curCenterId]);
        zoomCheck = -1;
        centerCheck = new Location(-1, -1);
    }

    private void drawTraj(Trajectory[] trajectories) {
        noFill();
        strokeWeight(1);
        stroke(new Color(190, 46, 29).getRGB());

        for (Trajectory trajectory : trajectories) {
            beginShape();
            for (Location location : trajectory.locations) {
                ScreenPosition scr = map.getScreenPosition(location);
                vertex(scr.x, scr.y);
            }
            endShape();
        }
    }

    private HashSet<Integer> loadVfgs(String filePath, double rate) {
        ArrayList<String> vfgsStr = new ArrayList<>();
        try {
            String line;
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            while ((line = reader.readLine()) != null) {
                vfgsStr.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        int trajNum = (int) (trajFull.length * rate);
        HashSet<Integer> res = new HashSet<>(trajNum);
        for (int i = 0; i < trajNum; i++) {
            res.add(Integer.parseInt(vfgsStr.get(i).split(",")[0]));
        }
        return res;
    }

    private Trajectory[] loadData(String filePath) {
        ArrayList<String> metaStr = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = reader.readLine()) != null) {
                metaStr.add(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("read done");
        Trajectory[] trajectories = new Trajectory[metaStr.size()];
        int trajId = 0;
        for (String line : metaStr) {
            String[] info = line.split(";");
            double score = Double.parseDouble(info[0]);

            String[] gpsList = info[1].split(",");
            Location[] locations = new Location[gpsList.length / 2 - 1];
            for (int i = 0; i < gpsList.length - 2; i += 2) {
                locations[i / 2] = new Location(Float.parseFloat(gpsList[i + 1]), Float.parseFloat(gpsList[i]));
            }
            Trajectory trajectory = new Trajectory(trajId);
            trajectory.setLocations(locations);
            trajectory.setScore(score);
            trajectories[trajId++] = trajectory;
        }
        return trajectories;
    }

    public static void main(String[] args) {
        PApplet.main(new String[]{ScreenShot.class.getName()});
    }
}
