package model;

import de.fhpotsdam.unfolding.UnfoldingMap;
import model.Region;
import model.Trajectory;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static model.Color.*;

public class SharedObject {

    private static SharedObject instance = new SharedObject();

    private SharedObject() {
        regionWList = new ArrayList<>();
    }

    public static SharedObject getInstance() {
        return instance;
    }

    /**
     * regions s
     */
    private static Region regionA = null;
    private static Region regionO = null;
    private static Region regionD = null;
    private static Region regionW = null;
    private static ArrayList<Region> regionWList;

    /**
     * trajectory info
     */
    private static List<Trajectory> trajTotal;
    private static HashSet<Integer> bgSet;
    private static List<ArrayList<Double>> trajSortByX;
    private static List<ArrayList<Double>> trajSortByY;
    private static ArrayList<Region> allRegion;
    private static UnfoldingMap map;

    private java.awt.Color[] colors = new java.awt.Color[]{
            new java.awt.Color(255, 32, 28),
            new java.awt.Color(124, 99, 28),
            new java.awt.Color(8, 99, 28),
            new java.awt.Color(22, 0, 103),
            new java.awt.Color(137, 48, 163),
            new java.awt.Color(255, 253, 0)};

    private Color[] all_color = {RED, BROWN, GREEN, BLUE, PINK, YELLOW};

    public void clearRegion() {
        regionA = regionD = regionO = regionW = null;
        regionWList.clear();
    }

    public void initTotalRegion(List tt, Region r_a, Region r_o, Region r_d, Region r_w) {
        trajTotal = tt;
        regionA = r_a;
        regionO = r_o;
        regionD = r_d;
        regionW = r_w;
    }

    public void initRegion(Region r_a, Region r_o, Region r_d, Region r_w) {
        regionA = r_a;
        regionO = r_o;
        regionD = r_d;
        regionW = r_w;
    }

    public void addRegionW(Region r) {
        regionWList.add(r);
    }

    public void initTrajList(List tt) {
        trajTotal = tt;
    }

    public void initSortListX(List listX) {
        trajSortByX = listX;
    }

    public void initSortListY(List listY) {
        trajSortByY = listY;
    }

    public void initBgSet(HashSet<Integer> set) {
        bgSet = set;
    }

    public void initRA(Region r_a) {
        regionA = r_a;
    }

    public void initRO(Region r_o) {
        regionO = r_o;
    }

    public void initRD(Region r_d) {
        regionD = r_d;
    }

    public void initRW(Region r_w) {
        regionW = r_w;
    }

    public void initMap(UnfoldingMap map) {
        SharedObject.map = map;
    }

    public UnfoldingMap getMap() {
        return map;
    }

    public List<Trajectory> getTotalTraj() {
        return trajTotal;
    }

    public List<Trajectory> getVFGSTraj() {
        List<Trajectory> VFGSTraj = new ArrayList<>();
        for (Integer e : bgSet)
            VFGSTraj.add(trajTotal.get(e));
        return VFGSTraj;
    }

    public List<Trajectory> getRandomTraj() {
        List<Trajectory> ranTraj = new ArrayList<>();
        Random ran = new Random(1);
        for (int i = 0; i < trajTotal.size() * 0.01; i++) {
            int r = ran.nextInt(trajTotal.size() - 1);
            ranTraj.add(trajTotal.get(r));
        }
        return ranTraj;
    }

    public ArrayList<Integer> getRandomTrajId() {
        ArrayList<Integer> ranTraj = new ArrayList<>();
        Random ran = new Random(1);
        for (int i = 0; i < trajTotal.size() * 0.01; i++) {
            int r = ran.nextInt(trajTotal.size() - 1);
            ranTraj.add(r);
        }
        return ranTraj;
    }

    public Region getRegionA() {
        return regionA;
    }

    public Region getRegionO() {
        return regionO;
    }

    public Region getRegionD() {
        return regionD;
    }

    public Region getRegionW() {
        return regionW;
    }

    public Region[] getAllRegions() {
        Region[] tmp = new Region[2 + regionWList.size()];
        tmp[0] = regionO;
        tmp[1] = regionD;
        for (int i = 0; i < regionWList.size(); i++) {
            tmp[i + 2] = regionWList.get(i);
        }
        return tmp;
    }

    public java.awt.Color[] getColors() {
        return colors;
    }

    public Color[] getColorList() {
        return all_color;
    }

    public HashSet<Integer> getBgSet() {
        return bgSet;
    }

    public List<ArrayList<Double>> getTrajSortByX() {
        return trajSortByX;
    }

    public List<ArrayList<Double>> getTrajSortByY() {
        return trajSortByY;
    }

    public ArrayList<Region> getRegionWList() {
        return regionWList;
    }
}


