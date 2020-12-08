package app;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

public class FileMerge {

    public static void main(String[] args) {

        java.text.DecimalFormat df = new java.text.DecimalFormat("#.00000");

        String filePath = "E:\\zcz\\dbgroup\\KDE\\simplification.txt";
        int i = 0;
        ArrayList<ArrayList<String>> result = new ArrayList<>();
        ArrayList<String> tmp = new ArrayList<>();
        i++;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals("--")) {
                    result.add(new ArrayList<>(tmp));
                    tmp.clear();
                } else
                    tmp.add(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(result.size());

        i = 0;
        StringBuilder sb = new StringBuilder();
        for (ArrayList<String> traj : result) {
            i++;
            for (String str : traj) {
                String[] tmpStr = str.split(",");
                sb.append(df.format(Double.parseDouble(tmpStr[0])));
                sb.append(",");
                sb.append(df.format(Double.parseDouble(tmpStr[1])));
                sb.append(",");
            }
            if (sb.length() == 0) {
                System.out.println(i);
                System.out.println(Arrays.deepToString(traj.toArray(new String[0])));
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append("\n");
        }
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("data/simplification.txt", true));
            writer.write(sb.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
