package draw;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import model.Trajectory;
import processing.core.PGraphics;

/**
 * Draw the trajectory to the buffer images.
 * Started and Managed by {@link TrajDrawManager}.
 */
public class TrajDrawWorker extends Thread {
    private final UnfoldingMap map;
    private final PGraphics pg;     // temp image that this thread paint on
    private final PGraphics[] trajImages;   // all traj image parts
    private final Trajectory[] trajList;    // all traj
    private final int[] trajCnt;    // record the # of painted traj
    private final int mapIdx, index;        // param to locate the pg this worker dealing with
    private final int begin, end;     // the param for select traj

    public TrajDrawWorker(UnfoldingMap map, PGraphics pg, PGraphics[] trajImages,
                          Trajectory[] trajList, int[] trajCnt,
                          int mapIdx, int index, int begin, int end) {
        this.map = map;
        this.pg = pg;
        this.trajImages = trajImages;
        this.trajList = trajList;
        this.trajCnt = trajCnt;
        this.mapIdx = mapIdx;
        this.index = index;
        this.begin = begin;
        this.end = end;

        // init priority
        this.setPriority(index % 9 + 1);
    }

    public int getMapIdx() {
        return mapIdx;
    }

    @Override
    public void run() {
        pg.beginDraw();
        pg.noFill();
        pg.strokeWeight(1);
        pg.stroke(255, 0, 0);

        for (int i = begin; i < end; i++) {
            pg.beginShape();


            // draw the traj
            for (Location loc : trajList[i].getLocations()) {

                // stop the thread if it is interrupted
                if (this.isInterrupted()) {
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

        trajImages[index] = pg;
    }
}
