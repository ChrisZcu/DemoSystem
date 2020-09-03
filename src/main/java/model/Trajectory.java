package model;

import de.fhpotsdam.unfolding.geo.Location;

public class Trajectory {
    public Location[] locations;
    private double score;
    private int trajId;
    private double greedyScore;
    private double cellScore;

    public Trajectory(int trajId) {
        score = 0;
        this.trajId = trajId;
    }

    public Trajectory(double score) {
        this.score = score;
    }

    Trajectory() {
        score = 0;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public double getScore() {
        return score;
    }

    public int getTrajId() {
        return trajId;
    }

    public void setLocations(Location[] locations) {
        this.locations = locations;
    }

    public Location[] getLocations() {
        return locations;
    }

    public void setGreedyScore(double greedyScore) {
        this.greedyScore = greedyScore;
    }

    public double getGreedyScore() {
        return greedyScore;
    }

    public void setTrajId(int trajId) {
        this.trajId = trajId;
    }

    public void setCellScore(double cellScore) {
        this.cellScore = cellScore;
    }

    public double getCellScore() {
        return cellScore;
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        for (Location p : this.locations) {
            res.append(",").append(p.y).append(",").append(p.x);
        }
        return res.substring(1);
    }
}
