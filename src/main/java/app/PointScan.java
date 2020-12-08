package app;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class PointScan {
    public static void main(String[] args) throws IOException {
        String filePath = "data/simplification.txt";
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        ArrayList<String> item = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            item.add(line);
        }
        reader.close();
        double minLat = 100;
        double minLon = 100;
        double maxLat = -100;
        double maxLon = -100;
        for (String str : item) {
            String[] data = str.split(",");
            for (int i = 0; i < data.length - 2; i += 2) {
                minLat = Math.min(minLat, Double.parseDouble(data[i + 1]));
                maxLat = Math.max(maxLat, Double.parseDouble(data[i + 1]));
                minLon = Math.min(minLon, Double.parseDouble(data[i]));
                maxLon = Math.max(maxLon, Double.parseDouble(data[i]));
            }
        }
        System.out.println(minLat + ", " + maxLat + ", " + minLon + ", " + maxLon);
    }
}
