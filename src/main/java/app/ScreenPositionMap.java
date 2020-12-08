package app;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.providers.MapBox;
import de.fhpotsdam.unfolding.utils.MapUtils;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import model.Trajectory;
import processing.core.PApplet;
import util.PSC;

import java.io.*;
import java.util.ArrayList;

public class ScreenPositionMap extends PApplet {
    UnfoldingMap map;
    private Location PRESENT = new Location(41.151, -8.634);


    @Override
    public void settings() {
        size(1000, 800, P2D);
    }

    @Override
    public void setup() {
        map = new UnfoldingMap(this, new MapBox.CustomMapBoxProvider(PSC.WHITE_MAP_PATH));
        map.setZoomRange(0, 20);
        map.zoomAndPanTo(20, PRESENT);
        map.setBackgroundColor(255);
        MapUtils.createDefaultEventDispatcher(this, map);
        Trajectory[] trajFull = loadData("data/GPS/porto_full.txt");
        mapScreen(trajFull, "data/GPS/portoFullScreen20.txt");
        exit();
    }

    private Trajectory[] loadData(String filePath) {
        ArrayList<String> item = new ArrayList<>();
        try {
            String line;
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            while ((line = reader.readLine()) != null) {
                item.add(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Trajectory[] trajectory = new Trajectory[item.size()];
        int trajId = 0;
        for (String line : item) {
            String[] data = line.split(";")[1].split(",");

            Trajectory traj = new Trajectory(trajId);
            ArrayList<Location> locations = new ArrayList<>();
            for (int i = 0; i < data.length - 2; i += 2) {
                locations.add(new Location(Float.parseFloat(data[i + 1]),
                        Float.parseFloat(data[i])));
            }
            traj.setLocations(locations.toArray(new Location[0]));
            trajectory[trajId++] = traj;
        }
        return trajectory;
    }

    private void mapScreen(Trajectory[] trajectories, String filePath) {
        StringBuilder screenString = new StringBuilder();
        for (Trajectory traj : trajectories) {
            StringBuilder sb = new StringBuilder();
            for (Location location : traj.locations) {
                ScreenPosition screenPosition = map.getScreenPosition(location);
                sb.append(screenPosition.x).append(",").append(screenPosition.y).append(";");
            }
            sb.deleteCharAt(sb.length() - 1);
            screenString.append(sb).append("\n");
        }
        writeIntoFile(screenString, filePath);
    }

    private void writeIntoFile(StringBuilder sb, String filePah) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePah));
            writer.write(sb.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args)  {
        PApplet.main(new String[]{ScreenPositionMap.class.getName()});
    }
}
