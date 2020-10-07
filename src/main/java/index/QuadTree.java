package index;

import app.TimeProfileSharedObject;
import de.fhpotsdam.unfolding.geo.Location;
import model.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QuadTree {
    private static double minGLat = Float.MAX_VALUE;
    private static double maxGLat = -Float.MAX_VALUE;
    private static double minGLon = Float.MAX_VALUE;
    private static double maxGLon = -Float.MAX_VALUE;

    public static QuadRegion quadRegionRoot;

    public static TrajectoryMeta[] trajMetaFull;

    public QuadTree() {
    }

    public static TrajectoryMeta[] loadData(double[] latLon, String filePath) {
        TrajectoryMeta[] trajFull;
        ArrayList<String> trajFullStr = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = reader.readLine()) != null) {
                trajFullStr.add(line);
            }
            reader.close();
            System.out.println("Read done");
        } catch (IOException e) {
            e.printStackTrace();
        }
//        trajFull = new TrajectoryMeta[trajFullStr.size()];
        ArrayList<TrajectoryMeta> res = new ArrayList<>();
        int i = 0;
        for (String line : trajFullStr) {
            String[] metaData = line.split(";");
            double score = Double.parseDouble(metaData[0]);
            String[] item = metaData[1].split(",");
            boolean next = false;
            Position[] positions = new Position[item.length / 2 - 1];
            for (int j = 0; j < item.length - 2; j += 2) {
//                int srcX = Integer.parseInt(item[j]);
//                int srcY = Integer.parseInt(item[j + 1]);
                float lat = Float.parseFloat(item[j + 1]);
                float lon = Float.parseFloat(item[j]);
                //debug
//                if (lat >41.345 || lat < 40.953 || lon < -8.86 || lon > -8.280) {
                if (lat < 38.429 || lon > -6.595) {
                    j = item.length;
                    next = true;
                    continue;
                }
                //debug done
//                if (i > 10){
//                    j = item.length;
//                    next = true;
//                    continue;
//                }

                positions[j / 2] = new Position((int) (lat * 10000), (int) (lon * 10000));

                minGLat = Math.min(lat, minGLat);
                maxGLat = Math.max(lat, maxGLat);
                minGLon = Math.min(lon, minGLon);
                maxGLon = Math.max(lon, maxGLon);

            }
//
            if (next)
                continue;
            TrajectoryMeta traj = new TrajectoryMeta(i);
            traj.setScore(score);
            traj.setPositions(positions);

            traj.setBegin(0);
            traj.setEnd(positions.length - 1);
//            trajFull[i] = traj;
            i++;
            res.add(traj);
        }
        trajFull = res.toArray(new TrajectoryMeta[0]);
        System.out.println("Transfer done " + trajFull.length);
        System.out.println(minGLat + ", " + maxGLat + ", " + minGLon + ", " + maxGLon);
        latLon[0] = minGLat;
        latLon[1] = maxGLat;
        latLon[2] = minGLon;
        latLon[3] = maxGLon;
        trajFullStr.clear();

        return trajFull;
    }


    private static QuadRegion local(double minLat, double maxLat, double minLon, double maxLon, int H,
                                    TrajectoryMeta[] trajFull) {
        RectRegion rectRegion = new RectRegion();
        rectRegion.initLoc(new Location(minLat, minLon), new Location(maxLat, maxLon));
//        System.out.println(H + " :(" + minLat + ", " + maxLat + ", " + minLon + ", " + maxLon + ")");
        TimeProfileSharedObject.getInstance().addQuadRectRegion(rectRegion);
        QuadRegion quadRegion = new QuadRegion(minLat, maxLat, minLon, maxLon);
        TrajToQuality[] trajToQualities = VfgsForIndex.getVfgs(trajFull);
        quadRegion.setTrajQuality(trajToQualities);
        if (H > 1) {
            QuadRegion[] quadChildren = new QuadRegion[4];
            double latOff = (maxLat - minLat) / 2;
            double lonOff = (maxLon - minLon) / 2;
            for (int i = 0; i < 4; i++) {
                int laxId = i / 2;
                int lonId = i % 2;
                double tmpLatMin = minLat + latOff * laxId;
                double tmpLonMin = minLon + lonOff * lonId;
                TrajectoryMeta[] wayPoint = getWayPointPos(trajFull, tmpLatMin, tmpLatMin + latOff, tmpLonMin, tmpLonMin + lonOff);
                quadChildren[i] = local(tmpLatMin, tmpLatMin + latOff, tmpLonMin, tmpLonMin + lonOff, H - 1, wayPoint);
            }
            quadRegion.setQuadRegionChildren(quadChildren);
        }
        return quadRegion;
    }

    /**
     * Generate the quad tree by {@link VfgsForIndexPart}
     */
    private static QuadRegion localPart(double minLat, double maxLat, double minLon, double maxLon, int H,
                                        TrajectoryMeta[] trajMetaList, int delta) {
        RectRegion rectRegion = new RectRegion();
        rectRegion.initLoc(new Location(minLat, minLon), new Location(maxLat, maxLon));
//        System.out.println(H + " :(" + minLat + ", " + maxLat + ", " + minLon + ", " + maxLon + ")");
        TimeProfileSharedObject.getInstance().addQuadRectRegion(rectRegion);
        QuadRegion quadRegion = new QuadRegion(minLat, maxLat, minLon, maxLon);

        TrajToSubpart[] trajToSubparts = VfgsForIndexPart.getVfgs(trajMetaList, delta);
        quadRegion.setTrajToSubparts(trajToSubparts);
        if (H > 1) {
            QuadRegion[] quadChildren = new QuadRegion[4];
            double latOff = (maxLat - minLat) / 2;
            double lonOff = (maxLon - minLon) / 2;
            for (int i = 0; i < 4; i++) {
                int laxId = i / 2;
                int lonId = i % 2;
                double tmpLatMin = minLat + latOff * laxId;
                double tmpLonMin = minLon + lonOff * lonId;
                TrajectoryMeta[] wayPoint = getWayPointPos(trajMetaList, tmpLatMin, tmpLatMin + latOff, tmpLonMin, tmpLonMin + lonOff);
                quadChildren[i] = localPart(tmpLatMin, tmpLatMin + latOff, tmpLonMin, tmpLonMin + lonOff, H - 1, wayPoint, delta);
            }
            quadRegion.setQuadRegionChildren(quadChildren);
        }
        return quadRegion;
    }


    public static TrajectoryMeta[] getWayPointPos(TrajectoryMeta[] trajFull, double minLat, double maxLat, double minLon, double maxLon) {
        ArrayList<TrajectoryMeta> res = new ArrayList<>();
        for (TrajectoryMeta traj : trajFull) {
            for (Position position : generatePosList(traj)) {
                if (inCheck(position, minLat, maxLat, minLon, maxLon)) {
                    res.add(traj);
                    break;
                }
            }
        }
        return cutTrajsPos(res.toArray(new TrajectoryMeta[0]), minLat, maxLat, minLon, maxLon);
    }

    private static boolean inCheck(Position position, double minLat, double maxLat, double minLon, double maxLon) {
        return position.x / 10000.0 >= minLat && position.x / 10000.0 <= maxLat
                && position.y / 10000.0 >= minLon && position.y / 10000.0 <= maxLon;
    }


    /**
     * Run {@link #getRegionInTrajPos} (which cut the traj into subpart) on all trajs.
     */
    private static TrajectoryMeta[] cutTrajsPos(TrajectoryMeta[] trajectories, double minLat, double maxLat, double minLon, double maxLon) {
        ArrayList<TrajectoryMeta> res = new ArrayList<>();
        for (TrajectoryMeta traj : trajectories) {
            res.addAll(getRegionInTrajPos(traj, minLat, maxLat, minLon, maxLon));
        }
        return res.toArray(new TrajectoryMeta[0]);
    }

    /**
     * Divide traj into trajMeta according to the giving region.
     */
    private static ArrayList<TrajectoryMeta> getRegionInTrajPos(TrajectoryMeta traj, double minLat, double maxLat, double minLon, double maxLon) {
        ArrayList<TrajectoryMeta> res = new ArrayList<>();
        int trajId = traj.getTrajId();
        List<Position> partPosList = generatePosList(traj);
        for (int i = 0; i < partPosList.size(); i++) {
            if (inCheck(partPosList.get(i), minLat, maxLat, minLon, maxLon)) {
                TrajectoryMeta trajTmp = new TrajectoryMeta(trajId);
                /* add */
                int begin = i;
                trajTmp.setBegin(i);
                /* add end */
                Position position = partPosList.get(i++);
                ArrayList<Position> locTmp = new ArrayList<>();
                while (inCheck(position, minLat, maxLat, minLon, maxLon) && i < partPosList.size()) {
                    locTmp.add(position);
                    position = partPosList.get(i++);
                }
                trajTmp.setPositions(locTmp.toArray(new Position[0]));
                /* add */
                if (i - 1 - begin != locTmp.size()) {
                    System.err.println("Error!");
                }
                trajTmp.setScore(i - 1 - begin);
                trajTmp.setEnd(i - 1);
                /* add end */
                res.add(trajTmp);
            }
        }
        return res;
    }

    public static QuadRegion getQuadIndex(String filePath, int height) {
        TrajectoryMeta[] trajectories = loadData(new double[4], filePath);
        return local(minGLat, maxGLat, minGLon, maxGLon, height, trajectories);
    }

    public static QuadRegion getQuadIndex(double minLat, double maxLat, double minLon, double maxLon,
                                          TrajectoryMeta[] trajectories, int height) {
        return local(minLat, maxLat, minLon, maxLon, height, trajectories);
    }

    public static QuadRegion getQuadIndexPart(String filePath, int height) {
        TrajectoryMeta[] trajectories = loadData(new double[4], filePath);
        return localPart(minGLat, maxGLat, minGLon, maxGLon, height, trajectories, 0);
    }

    public static QuadRegion getQuadIndexPart(double minLat, double maxLat, double minLon, double maxLon,
                                              TrajectoryMeta[] trajectories, int height, int delta) {
        return localPart(minLat, maxLat, minLon, maxLon, height, trajectories, delta);
    }

    private static List<Position> generatePosList(TrajectoryMeta trajMeta) {
        int trajId = trajMeta.getTrajId();
        int begin = trajMeta.getBegin();
        int end = trajMeta.getEnd();      // notice that the end is included

        return Arrays.asList(trajMetaFull[trajId].getPositions()).subList(begin, end + 1);
    }


    //lat41 lon8
    public static void main(String[] args) {
//        TrajectoryMeta[] trajectories = loadData("data/GPS/Porto5w/Porto5w.txt");
        TrajectoryMeta[] trajectories = loadData(new double[4], "data/GPS/porto_full.txt");

        TimeProfileSharedObject.getInstance().trajMetaFull = trajectories;
        trajMetaFull = trajectories;

        long t0 = System.currentTimeMillis();
        QuadRegion quadRegion = localPart(minGLat, maxGLat, minGLon, maxGLon, 3, trajectories, 0);
        System.out.println("index time: " + (System.currentTimeMillis() - t0));
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
