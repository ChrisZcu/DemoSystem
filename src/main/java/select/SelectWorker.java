package select;

import de.fhpotsdam.unfolding.UnfoldingMap;
import model.BlockType;
import model.Region;
import model.RegionType;
import app.SharedObject;
import model.Trajectory;

import java.util.ArrayList;
import java.util.concurrent.Callable;

import static select.SelectAlg.*;

/**
 * backend for select algorithm.
 */
public class SelectWorker implements Callable {
    private RegionType regionType;
    private ArrayList<Region> regionWList;
    private int begin;
    private int end;
    private int optIndex;
    private Trajectory[] trajectory;

    public SelectWorker(RegionType regionType, Trajectory[] trajectory, int begin, int end, int optIndex) {
        this.regionType = regionType;
        this.trajectory = trajectory;
        this.begin = begin;
        this.end = end;
        this.optIndex = optIndex;
    }

    @Override
    public int[] call() throws Exception {
        int[] res;
        switch (regionType) {
            case O_D:
                res = getODTraj(begin, end, trajectory, optIndex);
                break;
            case WAY_POINT:
                res = getWayPointTraj(begin, end, trajectory, optIndex);
                break;
            case O_D_W:
                res = getODWTraj(begin, end, trajectory, optIndex);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + regionType);
        }
        System.out.println("into thread: res number = " + res.length);
        return res;
    }
}
