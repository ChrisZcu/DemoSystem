package select;

import app.CircleRegionControl;
import app.SharedObject;
import model.CircleRegion;
import model.RectRegion;
import model.RegionType;
import model.Trajectory;

import java.util.ArrayList;
import java.util.concurrent.Callable;

import static select.SelectAlg.*;

/**
 * backend for select algorithm.
 */
public class SelectWorker extends Thread {
    private int begin;
    private int end;
    private int optIndex;
    private Trajectory[] trajectory;
    private int trajResId;

    public SelectWorker(Trajectory[] trajectory, int begin, int end, int optIndex, int trajResId) {
        this.trajectory = trajectory;
        this.begin = begin;
        this.end = end;
        this.optIndex = optIndex;
        this.trajResId = trajResId;
    }

    @Override
    public void run() {
        Trajectory[] res;
        ArrayList<Trajectory> resList = new ArrayList<>();
        ArrayList<ArrayList<Integer>> circleOIdList = CircleRegionControl.getCircleRegionControl().getCircleO();
        ArrayList<ArrayList<Integer>> circleDIdList = CircleRegionControl.getCircleRegionControl().getCircleD();
        ArrayList<ArrayList<Integer>> circleWIdList = CircleRegionControl.getCircleRegionControl().getWayPoint();
        ArrayList<ArrayList<CircleRegion>> circleRegionGroupList = CircleRegionControl.getCircleRegionControl().getGroupsOfCircle();

        CircleRegion circleO;
        CircleRegion circleD;
        ArrayList<Trajectory> tmpRes;
        ArrayList<CircleRegion> wayPointCircle;
        for (int i = 0; i < circleRegionGroupList.size(); i++) {
            ArrayList<CircleRegion> curGroupList = circleRegionGroupList.get(i);
            RegionType regionType = getRegionType(i);
            System.out.println(regionType);
            switch (regionType) {
                case O_D:
                    circleO = circleOIdList.size() > i && circleOIdList.get(i).size() > 0 ? curGroupList.get(circleOIdList.get(i).get(0)) : null;
                    circleD = circleDIdList.size() > i && circleDIdList.get(i).size() > 0 ? curGroupList.get(circleDIdList.get(i).get(0)) : null;

                    tmpRes = getODTraj(circleO, circleD, begin, end, trajectory, optIndex);
                    break;
                case WAY_POINT:
                    circleO = circleOIdList.size() > i && circleOIdList.get(i).size() > 0 ? curGroupList.get(circleOIdList.get(i).get(0)) : null;
                    circleD = circleDIdList.size() > i && circleDIdList.get(i).size() > 0 ? curGroupList.get(circleDIdList.get(i).get(0)) : null;

                    wayPointCircle = new ArrayList<>();
                    for (Integer e : circleWIdList.get(i)) {
                        wayPointCircle.add(curGroupList.get(e));
                    }

                    tmpRes = getWayPointTraj(circleO, circleD, wayPointCircle, begin, end, trajectory, optIndex);
                    break;
                case O_D_W:
                    circleO = circleOIdList.size() > i && circleOIdList.get(i).size() > 0 ? curGroupList.get(circleOIdList.get(i).get(0)) : null;
                    circleD = circleDIdList.size() > i && circleDIdList.get(i).size() > 0 ? curGroupList.get(circleDIdList.get(i).get(0)) : null;

                    wayPointCircle = new ArrayList<>();
                    for (Integer e : circleWIdList.get(i)) {
                        wayPointCircle.add(curGroupList.get(e));
                    }

                    tmpRes = getODWTraj(circleO, circleD, wayPointCircle, begin, end, trajectory, optIndex);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + regionType);
            }
            resList.addAll(tmpRes);
        }


        res = resList.toArray(new Trajectory[0]);
        System.out.println("into thread" + trajResId + " : res number = " + res.length);
        SharedObject.getInstance().getTrajSelectRes()[trajResId] = res;
    }

    private RegionType getRegionType(int circleGroupId) {
        ArrayList<Integer> circleOIdList = CircleRegionControl.getCircleRegionControl().getCircleO().get(circleGroupId);
        ArrayList<Integer> circleDIdList = CircleRegionControl.getCircleRegionControl().getCircleD().get(circleGroupId);
        ArrayList<Integer> circleWIdList = CircleRegionControl.getCircleRegionControl().getWayPoint().get(circleGroupId);


        if (circleOIdList.size() > 0 || circleDIdList.size() > 0) {
            if (circleWIdList.size() > 0) {
                return RegionType.O_D_W;
            } else {
                return RegionType.O_D;
            }
        } else {
            return RegionType.WAY_POINT;
        }

    }
}
