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
//        return res.toArray(new Trajectory[0]);
        System.out.println(res.size());
        return cutTrajs(res.toArray(new Trajectory[0]));
    }

    private boolean inCheck(Location loc) {
        return loc.getLat() >= Math.min(leftLat, rightLat) && loc.getLat() <= Math.max(leftLat, rightLat)
                && loc.getLon() >= Math.min(leftLon, rightLon) && loc.getLon() <= Math.max(leftLon, rightLon);
    }

    private Trajectory[] cutTrajs(Trajectory[] trajectories) {
        ArrayList<Trajectory> res = new ArrayList<>();
        for (Trajectory traj : trajectories) {
            res.addAll(getRegionInTraj(traj));
        }
        return res.toArray(new Trajectory[0]);
    }

    private ArrayList<Trajectory> getRegionInTraj(Trajectory traj) {
        ArrayList<Trajectory> res = new ArrayList<>();
        for (int i = 0; i < traj.locations.length; i++) {
            if (inCheck(traj.locations[i])) {
                Trajectory trajTmp = new Trajectory(-1);
                Location loc = traj.locations[i++];
                ArrayList<Location> locTmp = new ArrayList<>();
                while (inCheck(loc) && i < traj.locations.length) {
                    locTmp.add(loc);
                    loc = traj.locations[i++];
                }
                trajTmp.locations = locTmp.toArray(new Location[0]);
                trajTmp.setScore(locTmp.size());
                res.add(trajTmp);
            }
        }
        return res;
    }

}
