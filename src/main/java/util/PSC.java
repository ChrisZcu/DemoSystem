package util;

import java.awt.*;

public class PSC {
    public static final String WHITE_MAP_PATH = "https://api.mapbox.com/styles/v1/pacemaker-yc/ck4gqnid305z61cp1dtvmqh5y/tiles/512/{z}/{x}/{y}@2x?access_token=pk.eyJ1IjoicGFjZW1ha2VyLXljIiwiYSI6ImNrNGdxazl1aTBsNDAzZW41MDhldmQyancifQ.WPAckWszPCEHWlyNmJfY0A";

    // i.e. alpha.
    // These two settings must match the results
    public static double[] RATE_LIST
            = {0.05, 0.01, 0.005, 0.001, 0.0005, 0.0001, 0.00005, 0.00001};
    public static int[] DELTA_LIST
            = {0, 4, 8, 16, 32, 50, 64, 128};

    // origin data src path
    public static String ORIGIN_PATH
            = "data/porto_5k/porto_5k.txt";

    public static String PATH_PREFIX
            = "data/tmp/";

    // traj limit for full set. -1 for no limit
    public static final int LIMIT = -1;

    // recommend: core # * 2 or little higher
    public static final int FULL_THREAD_NUM = 5;
    public static final int SAMPLE_THREAD_NUM = 5;

    /**
     * VFGS result set (i.e. R / R+ in paper).
     * <br>Must contain %d for delta.
     * <br>Format: traj id, score
     */
    public static final String RES_PATH = PATH_PREFIX + "vfgs_%d.csv";

    public static final String OUTPUT_PATH = PATH_PREFIX + "output/";

    // six provided color to algorithm
    public static final Color[] COLORS = {
            new Color(15, 91, 120),
            new Color(19, 149, 186),
            new Color(162, 184, 108),
            new Color(235, 200, 68),
            new Color(241, 108, 32),
            new Color(190, 46, 29),
    };
}