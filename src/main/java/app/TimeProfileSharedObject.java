package app;

import model.RectRegion;
import model.Trajectory;
import org.w3c.dom.css.Rect;
import processing.core.PGraphics;

import java.util.ArrayList;

public class TimeProfileSharedObject {
    private static TimeProfileSharedObject instance = new TimeProfileSharedObject();
    private ArrayList<RectRegion> qudaRegion = new ArrayList<>();

    public void addQuadRectRegion(RectRegion rectRegion) {
        qudaRegion.add(rectRegion);
    }

    public ArrayList<RectRegion> getQudaRegion() {
        return qudaRegion;
    }

    public static TimeProfileSharedObject getInstance() {
        return instance;
    }

    private TimeProfileSharedObject() {
    }

    public PGraphics[] trajImageMtx;
    public Trajectory[][] trajRes;

    public Trajectory[] trajShow;
    boolean calDone = false;

    public void setTrajMatrix(PGraphics pg, int id) {
        trajImageMtx[id] = pg;
    }
}
