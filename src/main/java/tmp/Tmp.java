package tmp;

public class Tmp {

    private static int calSampleSize(double relativeError, double confidenceError, int size) {
        return (int) Math.ceil(
                1 /
                        (2 * Math.pow(relativeError, 2) / Math.log(2 / confidenceError)
                                + 1.0 / size)
        );
    }

    public static void main(String[] args) {
        int size = 2389482;
        double relativeError = 0.003, confidenceError = 0.12;
        int m =calSampleSize(relativeError, confidenceError, size);
        System.out.println(m * 1.0 / size * 100);
    }
}
