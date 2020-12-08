package app;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.providers.MapBox;
import de.fhpotsdam.unfolding.utils.MapUtils;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import model.Trajectory;
import processing.core.PApplet;
import util.PSC;

import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class TestDemo extends PApplet {
    UnfoldingMap map;
    private int ZOOMLEVEL = 11;
    private Location PRESENT = new Location(41.151, -8.634);
    Trajectory[] trajFull;

    @Override
    public void settings() {
        size(1000, 800, P2D);
    }

    @Override
    public void setup() {
        map = new UnfoldingMap(this, new MapBox.CustomMapBoxProvider(PSC.WHITE_MAP_PATH));
        map.setZoomRange(0, 20);
        map.zoomAndPanTo(ZOOMLEVEL, PRESENT);
        map.setBackgroundColor(255);
        MapUtils.createDefaultEventDispatcher(this, map);
        loadData("data/simplification.txt");
    }

    @Override
    public void draw() {
        if (!map.allTilesLoaded()) {
            map.draw();
        } else {
            map.draw();
            noFill();
            strokeWeight(1);
            stroke(new Color(190, 46, 29).getRGB());

            ArrayList<Trajectory> trajShow = new ArrayList<>();

            trajShow.addAll(Arrays.asList(trajFull).subList(0, 13000));

            for (Trajectory traj : trajShow) {
                beginShape();
                for (Location loc : traj.locations) {
                    ScreenPosition src = map.getScreenPosition(loc);
                    vertex(src.x, src.y);
                }
                endShape();
            }
            saveFrame("data/pic1119/test.png");

            noLoop();
        }
    }

    @Override
    public void mousePressed() {
        Location loc = map.getLocation(mouseX, mouseY);
        System.out.println(loc);
    }

    @Override
    public void keyPressed() {
        if (key == 'q'){
            saveFrame("data/pic1119/test.png");
        }
    }

    private void loadData(String filePath) {
        try {
            ArrayList<String> trajStr = new ArrayList<>(2400000);
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = reader.readLine()) != null) {
                trajStr.add(line);
            }
            reader.close();
            System.out.println("load done");
            int j = 0;

            trajFull = new Trajectory[trajStr.size()];

            for (String trajM : trajStr) {
                String[] data = trajM.split(";")[0].split(",");

                Trajectory traj = new Trajectory(j);
                ArrayList<Location> locations = new ArrayList<>();
                for (int i = 0; i < data.length - 2; i = i + 2) {
                    locations.add(new Location(Float.parseFloat(data[i]),
                            Float.parseFloat(data[i + 1])));
                }
                traj.setLocations(locations.toArray(new Location[0]));

                trajFull[j++] = traj;
            }
            trajStr.clear();
            System.out.println("load done");
            System.out.println("traj number: " + trajFull.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        PApplet.main(new String[]{TestDemo.class.getName()});
    }
}
