package model;

public class Position implements Comparable<Position> {
    public int x;
    public int y;

    int timeOrder;
    float lat;
    float lon;

    public Position(float x, float y, int t) {
        lat = x;
        lon = y;
        timeOrder = t;
    }

    public Position(double x, double y) {
        this.x = (int) x;
        this.y = (int) y;
    }

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public boolean equals(Position pos) {
        return this.x == pos.x && this.y == pos.y;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Position)) {
            return false;
        }
        Position p = (Position) obj;
        return (p.x == this.x && p.y == this.y);
    }

    @Override
    public int hashCode() {
        long ln = (long) (0.5 * (x + y) * (x + y + 1) + y);
        return (int) (ln ^ (ln >>> 32));
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }

    @Override
    public int compareTo(Position o) {
        return timeOrder - o.timeOrder;
    }
}
