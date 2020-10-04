package model;

public class TrajToQuality {
    Trajectory trajectory;
    double quality;

    public TrajToQuality(Trajectory trajectory, double quality){
        this.trajectory = trajectory;
        this.quality = quality;
    }

    public Trajectory getTrajectory() {
        return trajectory;
    }

    public void setTrajectory(Trajectory trajectory) {
        this.trajectory = trajectory;
    }

    public double getQuality() {
        return quality;
    }

    public void setQuality(double quality) {
        this.quality = quality;
    }
}
