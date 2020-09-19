package model;


import app.SharedObject;
import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.utils.ScreenPosition;

import java.awt.*;

public class CircleRegion extends RegionModel {
    private Location circleCenter;

    private float centerX;
    private float centerY;
    private Location radiusLocation;

    private int id; // click judge
    private int groupId;
    private int mapId; // map update
    private int kind; //0: O  1: D  2: W
    private Color color;

    private boolean moving = false;

    public CircleRegion() {

    }

    public CircleRegion(Location circleCenter, Location radiusLocation, int mapId) {
        this.circleCenter = circleCenter;
        this.mapId = mapId;
        this.radiusLocation = radiusLocation;
        updateCircleScreenPosition();
    }

    public CircleRegion(Location circleCenter, Location radiusLocation, int groupId, int id, int kind) {
        this.circleCenter = circleCenter;

        this.radiusLocation = radiusLocation;
        this.groupId = groupId;
        this.id = id;
        this.kind = kind;

        updateCircleScreenPosition();
    }

    public CircleRegion(CircleRegion circle, int groupId, int id) {
        this.circleCenter = circle.getCircleCenter();

        this.radiusLocation = circle.getRadiusLocation();
        this.groupId = groupId;
        this.id = id;
        this.kind = circle.getKind();

        updateCircleScreenPosition();
    }

    public float getRadius() {
        updateCircleScreenPosition();
        ScreenPosition lastClick = SharedObject.getInstance().getMapList()[mapId].getScreenPosition(radiusLocation);
        return (float) Math.pow((Math.pow(centerX - lastClick.x, 2) + Math.pow(centerY - lastClick.y, 2)), 0.5);
    }

    public float getRadius(int mouseX, int mouseY) {
        UnfoldingMap map = SharedObject.getInstance().getMapList()[mapId];
        ScreenPosition posCenter = map.getScreenPosition(circleCenter);

        return (float) Math.pow((Math.pow(posCenter.x - mouseX, 2) + Math.pow(posCenter.y - mouseY, 2)), 0.5);
    }

    public Location getCircleCenter() {
        return circleCenter;
    }

    public void setCircleCenter(Location circleCenter) {
        this.circleCenter = circleCenter;
    }

    public Location getRadiusLocation() {
        return radiusLocation;
    }

    public void setRadiusLocation(Location radiusLocation) {
        this.radiusLocation = radiusLocation;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public int getKind() {
        return kind;
    }

    public void setKind(int kind) {
        this.kind = kind;
    }

    public boolean isMoving() {
        return moving;
    }

    public void setMoving(boolean moving) {
        this.moving = moving;
    }


    public float getCenterX() {
        return centerX;
    }

    public float getCenterY() {
        return centerY;
    }


    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMapId() {
        return mapId;
    }

    public void setMapId(int mapId) {
        this.mapId = mapId;
    }

    public CircleRegion getCrsRegionCircle(int mapId) {
        UnfoldingMap map = SharedObject.getInstance().getMapList()[mapId];

        CircleRegion newCricleRegion = new CircleRegion(circleCenter, radiusLocation, mapId);
        newCricleRegion.setColor(color);
        newCricleRegion.setId(id);

        return newCricleRegion;
    }

    public void updateCircleScreenPosition() {
        UnfoldingMap map = SharedObject.getInstance().getMapList()[mapId];
        ScreenPosition pos = map.getScreenPosition(circleCenter);
        centerX = pos.x;
        centerY = pos.y;
    }

    @Override
    public String toString() {
        return "RegionCircle{" +
                "circleCenter=" + circleCenter +
                ", radiusLocation=" + radiusLocation +
                ", color=" + color +
                ", id=" + id +
                ", groupId=" + groupId +
                ", mapId=" + mapId +
                ", kind=" + kind +
                '}';
    }


}
