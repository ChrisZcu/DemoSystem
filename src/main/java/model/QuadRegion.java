package model;

public class QuadRegion {
    double minLat;
    double maxLat;
    double minLon;
    double maxLon;

    QuadRegion[] quadRegionChildren;
    TrajToQuality[] trajQuality;

    public QuadRegion(QuadRegion[] quadRegionChildren, TrajToQuality[] trajQuality) {
        this.quadRegionChildren = quadRegionChildren;
        this.trajQuality = trajQuality;
    }

    public QuadRegion(double minLat, double maxLat, double minLon, double maxLon) {
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.minLon = minLon;
        this.maxLon = maxLon;
    }

    public double getMinLat() {
        return minLat;
    }

    public void setMinLat(double minLat) {
        this.minLat = minLat;
    }

    public double getMaxLat() {
        return maxLat;
    }

    public void setMaxLat(double maxLat) {
        this.maxLat = maxLat;
    }

    public double getMinLon() {
        return minLon;
    }

    public void setMinLon(double minLon) {
        this.minLon = minLon;
    }

    public double getMaxLon() {
        return maxLon;
    }

    public void setMaxLon(double maxLon) {
        this.maxLon = maxLon;
    }

    public QuadRegion[] getQuadRegionChildren() {
        return quadRegionChildren;
    }

    public void setQuadRegionChildren(QuadRegion[] quadRegionChildren) {
        this.quadRegionChildren = quadRegionChildren;
    }

    public TrajToQuality[] getTrajQuality() {
        return trajQuality;
    }

    public void setTrajQuality(TrajToQuality[] trajQuality) {
        this.trajQuality = trajQuality;
    }
}
