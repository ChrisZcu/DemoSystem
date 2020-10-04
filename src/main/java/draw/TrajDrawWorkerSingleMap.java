package draw;

import app.TimeProfileSharedObject;
import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import model.Position;
import model.Trajectory;
import org.lwjgl.Sys;
import processing.core.PGraphics;

import java.awt.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

public class TrajDrawWorkerSingleMap extends Thread {
    private PGraphics pg;
    private UnfoldingMap map;
    private int begin;
    private int end;
    private Trajectory[] trajList;
    private int id;

    public volatile boolean stop = false;

    public TrajDrawWorkerSingleMap(PGraphics pg, UnfoldingMap map, int begin, int end, Trajectory[] trajList) {
        this.pg = pg;
        this.map = map;
        this.begin = begin;
        this.end = end;
        this.trajList = trajList;
        this.stop = stop;

        this.setPriority(9);
    }

    @Override
    public void run() {
        try {
            long t0 = System.currentTimeMillis();

            pg.beginDraw();
            pg.noFill();
            pg.strokeWeight(1);
            pg.stroke(new Color(190, 46, 29).getRGB());
            ArrayList<ArrayList<Point>> trajPointList = new ArrayList<>();
            for (int i = begin; i < end; i++) {
                ArrayList<Point> pointList = new ArrayList<>();
                for (Position position : trajList[i].getPositions()) {
                    if (this.stop) {
                        System.out.println(this.getName() + " cancel");
                        return;
                    }
                    Location loc = new Location(position.lat, position.lon);
                    ScreenPosition pos = map.getScreenPosition(loc);
                    pointList.add(new Point(pos.x, pos.y));
                }
                trajPointList.add(pointList);
            }
/*
        for (int i = begin; i < end; i++) {
            ArrayList<Point> pointList = new ArrayList<>();
            for (Location loc : trajList[i].getLocations()) {
                if (this.stop) {
                    System.out.println(this.getName() + " cancel");
                    return;
                }
                ScreenPosition pos = map.getScreenPosition(loc);
                pointList.add(new Point(pos.x, pos.y));
            }
            trajPointList.add(pointList);
        }


 */
            for (ArrayList<Point> traj : trajPointList) {
                pg.beginShape();
                for (Point pos : traj) {
                    if (this.stop) {
                        System.out.println(this.getName() + " cancel");
                        pg.endShape();
                        pg.endDraw();
                        return;
                    }
                    pg.vertex(pos.x, pos.y);
                }
                pg.endShape();
            }
            System.out.println(">>>>render time: " + (System.currentTimeMillis() - t0) + " ms");

            TimeProfileSharedObject.getInstance().setTrajMatrix(pg, id);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class Point {
        float x;
        float y;

        public Point(float x, float y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }
}
