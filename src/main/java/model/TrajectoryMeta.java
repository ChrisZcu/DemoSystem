package model;

public class TrajectoryMeta {
    private Position[] positions;
    private double score;
    private double metaScore;
    private int trajId;

    private GpsPosition[] gpsPositions;

    public GpsPosition[] getGpsPositions() {
        return gpsPositions;
    }

    public void setGpsPositions(GpsPosition[] gpsPositions) {
        this.gpsPositions = gpsPositions;
    }


    public TrajectoryMeta(int trajId) {
        score = 0;
        this.trajId = trajId;
    }

    public TrajectoryMeta(double score) {
        this.score = score;
    }

    public void setScore(double score) {
        this.score = score;
        metaScore = score;
    }

    public void updateScore(double score){
        this.score = score;
    }

    public double getScore() {
        return score;
    }

    public int getTrajId() {
        return trajId;
    }

    public void setPositions(Position[] posi){
        positions = posi;
    }

    public Position[] getPositions(){
        return positions;
    }

    public void scoreInit() {
        score = metaScore;
    }

}
