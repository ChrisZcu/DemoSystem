package select;

import app.TimeProfileSharedObject;
import de.fhpotsdam.unfolding.geo.Location;
import model.RectRegion;
import model.Trajectory;

import java.util.ArrayList;

public class TimeProfileWorker extends Thread {
    private int begin;
    private int end;
    private Trajectory[] trajectory;
    private int id;
    private float leftLon;
    private float leftLat;
    private float rightLon;
    private float rightLat;

    public TimeProfileWorker(int begin, int end, Trajectory[] trajectory, int id, RectRegion region) {
        this.begin = begin;
        this.end = end;
        this.trajectory = trajectory;
        this.id = id;
        leftLat = region.getLeftTopLoc().getLat();
        leftLon = region.getLeftTopLoc().getLon();
        rightLon = region.getRightBtmLoc().getLon();
        rightLat = region.getRightBtmLoc().getLat();
    }

    @Override
    public void run() {
        try {
            Trajectory[] res = getWayPoint();
            TimeProfileSharedObject.getInstance().trajRes[id] = res;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Trajectory[] getWayPoint() {
        ArrayList<Trajectory> res = new ArrayList<>();
        for (int i = begin; i < end; i++) {
            Trajectory traj = trajectory[i];
            for (Location loc : traj.locations) {
                if (inCheck(loc)) {
                    res.add(traj);
                    break;
                }
            }
        }

        return res.toArray(new Trajectory[0]);
    }


    private boolean inCheck(Location loc) {
//        System.out.println(loc.getLat() +", " + loc.getLon());
//        System.out.println(leftLat + "," +
//                leftLon + "," +
//                rightLon + "," +
//                rightLat);
        return loc.getLat() >= Math.min(leftLat, rightLat) && loc.getLat() <= Math.max(leftLat, rightLat)
                && loc.getLon() >= Math.min(leftLon, rightLon) && loc.getLon() <= Math.max(leftLon, rightLon);

    }


}
