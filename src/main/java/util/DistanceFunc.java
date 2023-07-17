package util;

import model.Position;
import model.Trajectory;
import model.TrajectoryMeta;

public class DistanceFunc {
    public static double EuclideanDistance(TrajectoryMeta traj1, TrajectoryMeta traj2) {
        boolean flag = traj1.getPositions().length > traj2.getPositions().length;
        Position[] p1s, p2s;
        if (flag) {
            p1s = traj1.getPositions();
            p2s = traj2.getPositions();
        } else {
            p2s = traj1.getPositions();
            p1s = traj2.getPositions();
        }

        int m = p1s.length, n = p2s.length;
        double dis = Double.MAX_VALUE;
        for (int j = 0; j < m - n + 1; ++j) {
            double tmp = 0;
            for (int i = 0; i < n; ++i) {
                tmp += PointEuclidean(p1s[i + j], p2s[i]);
            }
            dis = Math.min(dis, tmp);
        }
        return dis;
    }

    public static double DTW(TrajectoryMeta traj1, TrajectoryMeta traj2) {
        Position[] p1s = traj1.getPositions();
        Position[] p2s = traj2.getPositions();

        double[][] disMatrix = new double[p1s.length][p2s.length];
        double a = p2s.length * 1.0 / p1s.length;

        for (int i = 0; i < p1s.length; i++) {
            int base = (int) (a * i);
            int lowBound = Math.max(base - 5, 0);
            int upBound = Math.min(base + 5, p2s.length);
            for (int j = lowBound; j < upBound; j++) {
                disMatrix[i][j] = PointEuclidean(p1s[i], p2s[j]);
            }
        }
        for (int i = 1; i < p1s.length; i++) {
            int base = (int) (a * i);

            int lowBound = Math.max(base - 5, 1);
            int upBound = Math.min(base + 5, p2s.length);
            for (int j = lowBound; j < upBound; j++) {
                disMatrix[i][j] = Math.min(Math.min(disMatrix[i - 1][j - 1], disMatrix[i - 1][j]), disMatrix[i][j - 1]) + disMatrix[i][j];
            }
        }
        return disMatrix[p1s.length - 1][p2s.length - 1];
    }

    private static double SLCSS(Position[] p1s, Position[] p2s, int p1Begin, int p2Begin, int n, int m) {
        if (p1Begin == n || p2Begin == m) {
            return 0;
        }
        double dis = PointEuclidean(p1s[p1Begin], p2s[p2Begin]);
//        System.out.println(dis);
        System.out.println(p1Begin + ", " + n + ", " + p2Begin + ", " + m);

        if (dis <= 0.015) {
            return SLCSS(p1s, p2s, p1Begin + 1, p2Begin + 1, n, m) + 1;
        }
        return Math.max(SLCSS(p1s, p2s, p1Begin + 1, p2Begin, n, m), SLCSS(p1s, p2s, p1Begin, p2Begin + 1, n, m));
    }

    public static double LCSS(TrajectoryMeta traj1, TrajectoryMeta traj2) {
//        Position[] p1s = traj1.getPositions();
//        Position[] p2s = traj2.getPositions();
//        int n = p1s.length;
//        int m = p2s.length;

//        return m + n - 2 * SLCSS(p1s, p2s, 0, 0, n, m);
        Position[] p1s = traj1.getPositions();
        Position[] p2s = traj2.getPositions();
        int m = p1s.length;
        int n = p2s.length;
        double[][] dp = new double[m + 1][n + 1];
        for (int i = 0; i < n + 1; i++) {
            dp[0][i] = 0;
        }
        for (int j = 0; j < m + 1; j++) {
            dp[j][0] = 0;
        }
        for (int i = 1; i < m + 1; i++) {
            for (int j = 1; j < n + 1; j++) {
                double dis = PointEuclidean(p1s[i - 1], p2s[j - 1]);
                double min;
                if (dis <= 0.015){
                    min = dp[i - 1][j - 1] + 1;
                }else{
                    min = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
                dp[i][j] = min;
            }
        }
        return 1 - (dp[m][n]) / (m + n - dp[m][n]);
    }

    private static int SubCost(Position p1, Position p2) {
        double dis = PointEuclidean(p1, p2);
//        System.out.printf("%5f,%s,%s\n", dis, p1, p2);
        return dis <= 0.015 ? 0 : 1;
    }

    private static double EDR(Position[] p1s, Position[] p2s, int p1Begin, int p2Begin, int n, int m) {
//        System.out.println(p1Begin + ", " + n + ", " + p2Begin + ", " + m);
        if (p1Begin == 23) {
            System.out.println(p1Begin);
        }
        if (p1Begin == n) {
            return m - p2Begin;
        }
        if (p2Begin == m) {
            return n - p1Begin;
        }
//        return EDR(p1s, p2s, p1Begin + 1, p2Begin, n, m);
        return Math.min(EDR(p1s, p2s, p1Begin + 1, p2Begin + 1, n, m) + SubCost(p1s[p1Begin], p2s[p2Begin]),
                Math.min(EDR(p1s, p2s, p1Begin + 1, p2Begin, n, m) + 1, EDR(p1s, p2s, p1Begin, p2Begin + 1, n, m) + 1));
    }

    public static double EDR(TrajectoryMeta traj1, TrajectoryMeta traj2) {
        Position[] p1s = traj1.getPositions();
        Position[] p2s = traj2.getPositions();
        int m = p1s.length;
        int n = p2s.length;
        double[][] dp = new double[m + 1][n + 1];
        for (int i = 0; i < n + 1; i++) {
            dp[0][i] = i;
        }
        for (int j = 0; j < m + 1; j++) {
            dp[j][0] = j;
        }
        for (int i = 1; i < m + 1; i++) {
            for (int j = 1; j < n + 1; j++) {
                double subCost = SubCost(p1s[i - 1], p2s[j - 1]);
                double min = Math.min(dp[i - 1][j - 1] + subCost, dp[i][j - 1] + 1);
                min = Math.min(min, dp[i - 1][j] + 1);
                dp[i][j] = min;
            }
        }
        return dp[m][n];
//        return EDR(p1s, p2s, 0, 0, m, n);
    }

    private static double DiscreteFrechetDistance(double[][] disMatrix, int n, int m, Position[] p1s, Position[] p2s) {
        if (disMatrix[n][m] > -1) {
            return disMatrix[n][m];
        }
        if (n == 0 && m == 0) {
            disMatrix[n][m] = PointEuclidean(p1s[0], p2s[0]);
        } else if (n > 0 && m == 0) {
            disMatrix[n][m] = Math.max(DiscreteFrechetDistance(disMatrix, n - 1, 0, p1s, p2s), PointEuclidean(p1s[n], p2s[0]));
        } else if (n == 0 && m > 0) {
            disMatrix[n][m] = Math.max(DiscreteFrechetDistance(disMatrix, 0, m - 1, p1s, p2s), PointEuclidean(p1s[0], p2s[m]));
        } else if (n > 0 && m > 0) {
            disMatrix[n][m] = Math.max(Math.min(DiscreteFrechetDistance(disMatrix, n, m - 1, p1s, p2s), DiscreteFrechetDistance(disMatrix, n - 1, m, p1s, p2s)), PointEuclidean(p1s[n], p2s[m]));
        } else {
            disMatrix[n][m] = Double.MAX_VALUE;
        }
        return disMatrix[n][m];

    }

    public static double DiscreteFrechetDistance(TrajectoryMeta traj1, TrajectoryMeta traj2) {
        Position[] p1s = traj1.getPositions();
        Position[] p2s = traj2.getPositions();
        int n = p1s.length;
        int m = p2s.length;
        double[][] disMatrix = new double[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; ++j) {
                disMatrix[i][j] = -1;
            }
        }
        return DiscreteFrechetDistance(disMatrix, n - 1, m - 1, p1s, p2s);
    }

    public static double HausdorffDistance(TrajectoryMeta traj1, TrajectoryMeta traj2) {
        Position[] p1s = traj1.getPositions();
        Position[] p2s = traj2.getPositions();
        double dis = 0;
        for (Position p1 : p1s) {
            double tmpDis = Double.MAX_VALUE;
            for (Position p2 : p2s) {
                tmpDis = Math.min(PointEuclidean(p1, p2), tmpDis);
            }
            dis = Math.max(dis, tmpDis);
        }

        return dis;
    }

    public static double PointEuclidean(Position p1, Position p2) {
//        System.out.println(p1.x + ", " + p1.y + ", " + p2.x + ", " + p2.y);
        double x1 = p1.x / 10000.0;
        double y1 = p1.y / 10000.0;
        double x2 = p2.x / 10000.0;
        double y2 = p2.y / 10000.0;

        return Math.hypot(x1 - x2, y1 - y2);
    }
}
