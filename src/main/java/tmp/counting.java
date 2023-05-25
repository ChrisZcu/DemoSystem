package tmp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class counting {
    public static void main(String[] args) throws IOException {
        String ChengDuFilePath = "data/kernelMatrix/chengdu/chengdu_region.txt";
        String PortoFilePath = "data/kernelMatrix/porto_test.txt";
        String ShenZhenFilePath = "data/kernelMatrix/shenzhen/shenzhen_region.txt";

        BufferedReader reader = new BufferedReader(new FileReader(ChengDuFilePath));
        String line;
        ArrayList<String> item = new ArrayList<>();

        while ((line = reader.readLine()) != null) {
            item.add(line);
        }
        reader.close();
        System.out.println("trajectory number: " + item.size());

        int pointNum = 0;
        int longestTraj = 0;
        for (String traj : item) {
            int trajLen = traj.split(",").length / 2;
            longestTraj = Math.max(longestTraj, trajLen);
            pointNum += trajLen;
        }

        System.out.println("point number: " + pointNum);
        System.out.println("longest trajectory length: " + longestTraj);
    }
}
