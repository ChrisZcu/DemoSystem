package app;

import model.Trajectory;
import processing.core.PGraphics;

public class TimeProfileSharedObject {
    private static TimeProfileSharedObject instance = new TimeProfileSharedObject();

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
