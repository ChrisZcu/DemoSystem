package model;

import java.util.ArrayList;

/**
 * different group of way point
 */
public class WayPointGroup {
    private int groupId;
    private int wayPointLayer = 0;
    private ArrayList<ArrayList<Region>> wayPointLayerList;

    public WayPointGroup() {
        wayPointLayerList = new ArrayList<>();
    }

    public WayPointGroup(int groupId) {
        this.groupId = groupId;
        wayPointLayerList = new ArrayList<>();
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public int getWayPointLayer() {
        return wayPointLayer;
    }

    public void setWayPointLayer(int wayPointLayer) {
        this.wayPointLayer = wayPointLayer;
    }

    public ArrayList<ArrayList<Region>> getWayPointLayerList() {
        return wayPointLayerList;
    }

    public void setWayPointLayerList(ArrayList<ArrayList<Region>> wayPointLayerList) {
        this.wayPointLayerList = wayPointLayerList;
    }

    public void cleanWayPointRegions() {
        wayPointLayerList.clear();
    }

    public void updateWayPointLayer() {
        if (wayPointLayerList.size() == wayPointLayer + 1) {
            wayPointLayer++;
        }
    }

    public ArrayList<Region> getAllRegions() {
        ArrayList<Region> res = new ArrayList<>();
        if (wayPointLayerList != null) {
            for (ArrayList<Region> regionList : wayPointLayerList) {
                res.addAll(regionList);
            }
        }
        return res;
    }

    public void addWayPoint(Region r) {
        if (wayPointLayerList.size() <= wayPointLayer) {
            wayPointLayerList.add(new ArrayList<>());
        }
        wayPointLayerList.get(wayPointLayer).add(r);

        //TODO update the shared object
    }

    public WayPointGroup getCorrWayPointGroup(int mapId) {
        WayPointGroup wayPointGroup = new WayPointGroup(groupId);
        wayPointGroup.setWayPointLayer(wayPointLayer);
        for (ArrayList<Region> wList : wayPointLayerList) {
            ArrayList<Region> tmp = new ArrayList<>();
            for (Region r : wList) {
                tmp.add(r.getCorresRegion(mapId));
            }
            wayPointGroup.getWayPointLayerList().add(tmp);
        }
        return wayPointGroup;
    }
}
