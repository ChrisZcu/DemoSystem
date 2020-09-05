package draw;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import model.Trajectory;
import processing.core.PGraphics;

import java.awt.*;

/**
 * Draw the trajectory to the buffer images. Started and Managed by {@link TrajDrawManager}.
 * <br> The details of the pg is hide from this. (no matter what the block is)
 */
public class TrajDrawWorker extends Thread {
    private final UnfoldingMap map;
    // temp image that this thread paint on
    // the pg has already be translated.
    private final PGraphics pg;
    private final PGraphics[] trajImages;   // all traj image parts
    private final Trajectory[] trajList;    // all traj
    private final int[] trajCnt;    // record the # of painted traj
    private final int mapIdx, index;        // param to locate the pg this worker dealing with
    private final float offsetX, offsetY;     // offset of the map
    private final int begin, end;       // the param for select traj
    private final Color color;      // the color for the traj

    public TrajDrawWorker(String name, UnfoldingMap map, PGraphics pg,
                          PGraphics[] trajImageList, Trajectory[] trajList, int[] trajCnt,
                          int index, float offsetX, float offsetY,
                          int begin, int end, Color color) {
        super(name);
        this.map = map;
        this.pg = pg;
        this.trajImages = trajImageList;
        this.trajList = trajList;
        this.trajCnt = trajCnt;
        this.mapIdx = -1;       // not used for now
        this.index = index;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.begin = begin;
        this.end = end;
        this.color = color;

        // init priority
        this.setPriority(index % 9 + 1);
    }

    @Deprecated
    public int getMapIdx() {
        return mapIdx;
    }

    @Override
    public void run() {
        pg.beginDraw();
        pg.translate(-1 * offsetX, -1 * offsetY);
        pg.noFill();
        pg.strokeWeight(1);
        pg.stroke(color.getRGB());

        System.out.println(this.getName() + " start");

        for (int i = begin; i < end; i++) {
            pg.beginShape();

            // draw the traj
            for (Location loc : trajList[i].getLocations()) {

                // stop the thread if it is interrupted
                if (this.isInterrupted()) {
                    System.out.println(this.getName() + " cancel");
                    pg.endShape();
                    pg.endDraw();
                    return;
                }

                ScreenPosition pos = map.getScreenPosition(loc);
                pg.vertex(pos.x, pos.y);
            }
            pg.endShape();

//            trajCnt[index] ++;
        }

        System.out.println(this.getName() + " finished");
        trajImages[index] = pg;
    }
}
