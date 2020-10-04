package model;

import javafx.geometry.Pos;

public class Position implements Comparable<Position> {
    public int x;
    public int y;
    public float delta;
    int timeOrder;
    public float lat;
    public float lon;

    public Position(float x, float y, int t) {
        lat = x;
        lon = y;
        timeOrder = t;
    }


    public Position(float x, float y) {
        lat = x;
        lon = y;
        this.x = (int) (x * 1000000);
        this.y = (int) (y * 1000000);
    }

    public Position(float x, float y, float delta) {
        lat = x;
        lon = y;
        this.delta = delta;
    }

//    public Position(float x, float y) {
//        this.x = x;
//        this.y = y;
//    }

    public Position(double x, double y) {
        this.x = (int) x;
        this.y = (int) y;
    }


    public Position(int x, int y) {
        this.x = x;
        this.y = y;
        lat = (float) (x / 1000000.0);
        lon = (float) (y / 1000000.0);
    }

//    public boolean equals(Position pos) {
//        return this.x == pos.x && this.y == pos.y;
//    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Position)) {
            return false;
        }
        Position p = (Position) obj;
//        return (p.lat <= lat + delta && p.lat >= lat - delta)
//                && (p.lon <= lon + delta && p.lon >= lon - delta);
        return (p.x == this.x && p.y == this.y);
    }

    @Override
    public int hashCode() {
        long ln = (long) (0.5 * (x + y) * (x + y + 1) + y);
        return (int) (ln ^ (ln >>> 32));
    }

//    @Override
//    public int hashCode() {
//        int x = (int) (lat * 10000);
//        int y = (int) (lon * 10000);
//
//        long ln = (long) (0.5 * (x + y) * (x + y + 1) + y);
//        return (int) (ln ^ (ln >>> 32));
//    }

    @Override
    public String toString() {
        return "(" + lat + ", " + lon + ")";
    }

    @Override
    public int compareTo(Position o) {
        return timeOrder - o.timeOrder;
    }
}
