package util;

public class Util {
    /**
     * Translate simple rate to real traj count.
     */
    public static int[] translateRate(int trajNum, double[] rateList) {
        int len = rateList.length;
        int[] ret = new int[len];
        for (int i = 0; i < len; i++) {
            ret[i] = (int) Math.round(trajNum * rateList[i]);
        }
        return ret;
    }
}
