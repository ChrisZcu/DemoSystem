package origin.model;

import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.UnfoldingMap;
import java.util.ArrayList;
import java.util.List;

public class Trajectory {
    public List<Location> points = new ArrayList<>();
    public ArrayList<Position> positions = new ArrayList<>();

    private double score;
    private int trajId;
    private double greedyScore;
    private double cellScore;

    private List<Position> posList;

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

    public List<Location> getPoints() {
        return points;
    }
    public void unitPositions(UnfoldingMap map){
        for (Location p : this.points) {
            double px = map.getScreenPosition(p).x;
            double py = map.getScreenPosition(p).y;
            positions.add(new Position(px, py));
        }
    }
    public List<Position> getPositions(){
        return positions;
    };
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

    public List<Position> getPosList() {
        return posList;
    }

    public void setPosList(List<Position> posList) {
        this.posList = posList;
    }

    @Override
    public String toString() {
        String res = "";
        for (Location p : this.points) {
            res = res + "," + p.y + "," + p.x;
        }
        return res.substring(1);
    }
}


