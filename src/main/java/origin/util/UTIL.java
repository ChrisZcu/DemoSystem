package origin.util;

import de.fhpotsdam.unfolding.geo.Location;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import origin.model.Trajectory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UTIL {
    public static void totalListInitV2(List<Trajectory> TrajTotal, String dataPath) {
        int trajId = 0;
        ArrayList<String> trajFullStr = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(dataPath));
            String line;
            while ((line = reader.readLine()) != null) {
                trajFullStr.add(line);
            }
            reader.close();
            System.out.println("Read done");
            System.out.println(trajFullStr.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (String line : trajFullStr) {
            Trajectory traj = new Trajectory(trajId);
            trajId++;
            String[] item = line.split(";");
            String[] data = item[1].split(",");
            double score = Double.parseDouble(item[0]);
            int j = 0;
            for (; j < data.length - 2; j = j + 2) {
                Location point = new Location(Double.parseDouble(data[j + 1]), Double.parseDouble(data[j]));
                traj.points.add(point);
            }
            traj.setScore(score);
            TrajTotal.add(traj);
        }
    }

    public static void totalListInit(List<Trajectory> TrajTotal, String dataPath) {
        int trajId = 0;

        try {
            File theFile = new File(dataPath);
            LineIterator it = FileUtils.lineIterator(theFile, "UTF-8");
            String line;
            String[] data;
            try {
                while (it.hasNext()) {
                    Trajectory traj = new Trajectory(trajId);
                    trajId++;
                    line = it.nextLine();
                    String[] item = line.split(";");
                    data = item[1].split(",");
                    double score = Double.parseDouble(item[0]);
                    int j = 0;
                    for (; j < data.length - 2; j = j + 2) {
                        Location point = new Location(Double.parseDouble(data[j + 1]), Double.parseDouble(data[j]));
                        traj.points.add(point);
                    }
                    traj.setScore(score);
                    TrajTotal.add(traj);
                }
            } finally {
                LineIterator.closeQuietly(it);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {

    }
}
