//package TKDERevision;
//
//import de.fhpotsdam.unfolding.geo.Location;
//import model.Trajectory;
//
//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.io.IOException;
//import java.util.ArrayList;
//
//class Exe{
//    Trajectory[] trajFull;
//
//    private void loadData(String filePath) {
//        try {
//            ArrayList<String> trajStr = new ArrayList<>(2400000);
//            BufferedReader reader = new BufferedReader(new FileReader(filePath));
//            String line;
//            while ((line = reader.readLine()) != null) {
//                trajStr.add(line);
//            }
//            reader.close();
//            System.out.println("load done");
//            int j = 0;
//
//            trajFull = new Trajectory[trajStr.size()];
//
//            for (String trajM : trajStr) {
//                String[] item = trajM.split(";");
//                String[] data = item[1].split(",");
//                Trajectory traj = new Trajectory(j);
//                ArrayList<Location> locations = new ArrayList<>();
////                Position[] metaGPS = new Position[data.length / 2 - 1];
//                for (int i = 0; i < data.length - 2; i = i + 2) {
//                    locations.add(new Location(Float.parseFloat(data[i + 1]),
//                            Float.parseFloat(data[i])));
////                    metaGPS[i / 2] = new Position(Float.parseFloat(data[i + 1]), Float.parseFloat(data[i]));
//                }
//                traj.setLocations(locations.toArray(new Location[0]));
//                traj.setScore(Integer.parseInt(item[0]));
////                traj.setMetaGPS(metaGPS);
//                trajFull[j++] = traj;
//            }
//            trajStr.clear();
//            System.out.println("load done");
//            System.out.println("traj number: " + trajFull.length);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void sample(){
//        loadData("data/GPS/porto_full.txt");
////        Trajectory[] trajectories = loadVfgs("data/GPS/dwt_24k.txt", 0.001);
//        Trajectory[] trajList = util.VFGS.getCellCover(trajFull, mapClone, 0.5, 0);
//        String writePath = "data/vfgs/tkde_revision/reviewer1/half_sample.txt";
//        util.Util.storeVQGSRes(writePath, trajList);
//    }
//}
//public class Sampling {
//
//
//    public static void main(String[] args) {
//
//    }
//}
