package select;

import model.BlockType;
import model.Region;
import model.RegionType;
import app.SharedObject;

import java.util.ArrayList;
import java.util.concurrent.Callable;

import static select.SelectAlg.*;

/**
 * backend for select algorithm.
 */
public class SelectWorker implements Callable {
    private RegionType regionType;
    private BlockType blockType;
    private ArrayList<Region> regionWList;
    private int begin;
    private int end;

    public SelectWorker(RegionType regionType, BlockType blockType, int begin, int end) {
        this.regionType = regionType;
        this.blockType = blockType;
        this.begin = begin;
        this.end = end;
    }

    @Override
    public int[] call() throws Exception {
        int[] res;
        SharedObject instance = SharedObject.getInstance();
        switch (regionType) {
            case O_D:
                res = getODTraj(begin, end, instance.getTrajArray()[blockType.getValue()]);
                break;
            case WAY_POINT:
                res = getWayPointTraj(begin, end, instance.getTrajArray()[blockType.getValue()], regionWList);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + regionType);
        }
        System.out.println("into thread: res number = " + res.length);

        return res;
    }
}
