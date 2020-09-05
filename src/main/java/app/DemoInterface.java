package app;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.providers.MapBox;
import de.fhpotsdam.unfolding.utils.MapUtils;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import draw.TrajDrawManager;
import model.*;
import processing.core.PApplet;
import processing.core.PGraphics;
import util.PSC;
import util.SelectDataDialog;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import static util.Swing.createTopMenu;


public class DemoInterface extends PApplet {
    private TrajDrawManager trajDrawManager;
    private PGraphics[][] trajImgMtx;       // the 4 trajImg buffer list for main layer
    private PGraphics[][] trajImgSltMtx;    // the 4 trajImg buffer list for double select result
    private EleButton[] dataButtonList;

    private float[][] mapLocInfo;
    private static final Location PORTO_CENTER = new Location(41.14, -8.639);//维度经度
    private static final Location PRESENT = PORTO_CENTER;

    private UnfoldingMap[] mapList;

    private int[] checkLevel = {-1, -1, -1, -1};
    private Location[] checkCenter = {new Location(-1, -1), new Location(-1, -1),
            new Location(-1, -1), new Location(-1, -1)};

    private int screenWidth;
    private int screenHeight;

    private int mapWidth;
    private float[] mapXList;       // the x coordination of the all maps
    private float[] mapYList;       // the y coordination of the all maps
    private int mapHeight;
    private final int dataButtonXOff = 2;
    private final int dataButtonYOff = 2;
    private final int mapDownOff = 40;
    private final int heighGapDis = 4;
    private final int widthGapDis = 6;

    private boolean[] viewVisibleList = {true, true, true, true};  // is the map view visible
    private boolean[] linkedList = {true, true, true, true};       // is the map view linked to others
    //private boolean[] linkedList = {true, true, false, false};
    private int mapController = 0;

    private boolean loadFinished = false;
    private int regionId = 0;
    private int dragRegionId = -1;

    private boolean regionDragged = false;
    private Position lastClick;

    private int circleSize = 15;
    private boolean mouseMove = false;

    /* Other interface component */

    private SelectDataDialog selectDataDialog;

    @Override
    public void settings() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        screenWidth = (int) screenSize.getWidth();
        screenHeight = (int) screenSize.getHeight();

        mapWidth = (screenWidth - widthGapDis) / 2;
        mapHeight = (screenHeight - mapDownOff - heighGapDis) / 2;

        size(screenWidth, screenHeight - 1, P2D);
    }

    @Override
    public void setup() {
        try {
            String lookAndFeel = UIManager.getSystemLookAndFeelClassName();
            UIManager.setLookAndFeel(lookAndFeel);
        } catch (Exception e) {
            System.err.println("Set look and feel failed!");
        }

        initMapSurface();
        initDataButton();
        mapLocInfo = new float[2][];
        mapLocInfo[0] = mapXList;
        mapLocInfo[1] = mapYList;

        SharedObject.getInstance().setMapLocInfo(mapLocInfo);

        background(220, 220, 220);

        SharedObject.getInstance().setMapList(mapList);
        SharedObject.getInstance().initBlockList();

        trajImgMtx = new PGraphics[4][Math.max(PSC.FULL_THREAD_NUM, PSC.SAMPLE_THREAD_NUM)];
        trajImgMtx = new PGraphics[4][PSC.SELECT_THREAD_NUM];

        // Warning: the constructor of the TrajDrawManager must be called AFTER initBlockList()
        trajDrawManager = new TrajDrawManager(this, mapList, trajImgMtx, trajImgSltMtx,
                null, mapXList, mapYList, mapWidth, mapHeight);
        SharedObject.getInstance().setTrajDrawManager(trajDrawManager);

        viewVisibleList = new boolean[4];
        Arrays.fill(viewVisibleList, true);     // temp
        SharedObject.getInstance().setViewVisibleList(viewVisibleList);

        // init other interface component
        createTopMenu(screenWidth, mapDownOff - 5, frame, this);
        this.selectDataDialog = new SelectDataDialog(frame);

        (new Thread(this::loadData)).start();

    }

    private void loadData() {
        SharedObject.getInstance().loadTrajData();
        SharedObject.getInstance().setBlockAt(0, BlockType.FULL, -1, -1);
        SharedObject.getInstance().setBlockAt(1, BlockType.VFGS, 0, 0);
        SharedObject.getInstance().setBlockAt(2, BlockType.RAND, 0, -1);
        trajDrawManager.startAllNewRenderTask(false);
        loadFinished = true;
    }

    private void updateMap(int currentMapController) {
        boolean[] mapChanged = {false, false, false, false};
        boolean mapControllerChanged = !(currentMapController == mapController);

        for (int i = 0; i < mapList.length; ++i) {
            mapList[i].draw();
            mapChanged[i] = checkLevel[i] != mapList[i].getZoomLevel() || !checkCenter[i].equals(mapList[i].getCenter());
        }

        if (mapControllerChanged) {
            for (int i = 0; i < mapList.length; ++i) {
                int zoomLevel = mapList[currentMapController].getZoomLevel();
                Location center = mapList[currentMapController].getCenter();

                if (linkedList[i] && viewVisibleList[i] && mapChanged[i]) {
                    mapList[i].zoomToLevel(zoomLevel);
                    mapList[i].panTo(center);

                    checkLevel[i] = zoomLevel;
                    checkCenter[i] = center;
                }
            }
            mapController = currentMapController;
        } else {
            if (mapChanged[mapController]) {
                //System.out.println(mapControllerChanged+" "+mapChanged[mapController]);
                int zoomLevel = mapList[mapController].getZoomLevel();
                Location center = mapList[mapController].getCenter();

                for (int i = 0; i < mapList.length; ++i) {
                    if (linkedList[i] && viewVisibleList[i]) {
                        mapList[i].zoomToLevel(zoomLevel);
                        mapList[i].panTo(center);

                        checkLevel[i] = zoomLevel;
                        checkCenter[i] = center;
                    }
                }
            }
        }

    }



    @Override
    public void draw() {
        updateMap(mapController);

//        boolean mapChanged = true;
//        for (UnfoldingMap map : mapList) {
//            map.draw();
//            mapChanged = checkLevel != map.getZoomLevel() || !checkCenter.equals(map.getCenter());
//        }
        if (SharedObject.getInstance().isScreenShot()) {
            int totalFileNum = Objects.requireNonNull(new File(PSC.OUTPUT_PATH).list()).length;

            String path = PSC.OUTPUT_PATH1 + "screenShot_" + (totalFileNum / 2) + ".png";
            saveFrame(path);

            String infilePath = PSC.OUTPUT_PATH1 + "screenShotInfo_" + (totalFileNum / 2) + ".txt";
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(infilePath));
                writer.write(SharedObject.getInstance().getBlockInfo());
            } catch (IOException ignored) {
            }
        }

        for (EleButton dataButton : dataButtonList) {
            dataButton.render(this);
        }

//        if (mapChanged) {
//            //TODO update the map
//        }

        nextMap:
        for (int mapIdx = 0; mapIdx < 4; mapIdx++) {
            /*if (!viewVisibleList[mapIdx]) {
                continue;
            }*/
            for (PGraphics pg : trajImgMtx[mapIdx]) {
                if (pg == null) {
                    continue nextMap;
                }
                image(pg, mapXList[mapIdx], mapYList[mapIdx]);
            }
        }
        if (regionDragged) {//drag the region, not finished
            drawRegion(getSelectRegion(lastClick));
        }

        if (SharedObject.getInstance().isFinishSelectRegion()) {//finish select
            SharedObject instance = SharedObject.getInstance();
            for (int i = 0; i < instance.getTrajSelectResList().length; i++) {
                for (Integer trajId : instance.getTrajSelectResList()[i]) {
                    drawTraj(instance.getTrajFull()[trajId], mapXList[i], mapYList[i],
                            0, mapWidth - widthGapDis, 0, mapHeight - heighGapDis, mapList[i]);
                }
            }
        }


        for (Region r : SharedObject.getInstance().getRegionWithoutWList()) {
            drawRegion(r);
            strokeWeight(circleSize);
            point(r.leftTop.x, r.leftTop.y);
        }
        for (ArrayList<Region> wList : SharedObject.getInstance().getRegionWLayerList()) {
            for (Region r : wList) {
                drawRegion(r);
                strokeWeight(circleSize);
                point(r.leftTop.x, r.leftTop.y);
            }
        }
    }

//    private boolean regionDragged = false;
//    private Position lastClick;
//    private int circleSize = 15;
//    private boolean mouseMove = false;

    @Override
    public void mousePressed() {
        int eleId = -1;
        for (EleButton dataButton : dataButtonList) {
            if (dataButton.isMouseOver(this)) {
                eleId = dataButton.getEleId();
                break;
            }
        }
        if (eleId != -1) {
            if (loadFinished) {
                System.out.println("open dialog");
                selectDataDialog.showDialogFor(eleId);
            } else {
                System.out.println("not to open dialog");
            }
        } else {
            System.out.println("eleId == -1");
        }

        if (mouseButton == RIGHT) {
            if (SharedObject.getInstance().checkSelectRegion()) {
                regionDragged = true;
                lastClick = new Position(mouseX, mouseY);
            }
        }
        //drag
        if (SharedObject.getInstance().isDragRegion()) {
            for (Region r : SharedObject.getInstance().getAllRegions()) {
                if (mouseX >= r.leftTop.x - circleSize / 2 && mouseX <= r.rightBtm.x + circleSize / 2
                        && mouseY >= r.leftTop.y - circleSize / 2 && mouseY <= r.rightBtm.y + circleSize / 2) {
                    dragRegionId = r.id;
                    mouseMove = !mouseMove;
                    System.out.println(dragRegionId);
                    break;
                }
            }
        }
    }

    @Override
    public void mouseReleased() {
        if (regionDragged) {
            regionDragged = false;
            Region selectRegion = getSelectRegion(lastClick);
            selectRegion.id = regionId++;
            if (SharedObject.getInstance().checkRegion(0)) {        // O
                SharedObject.getInstance().setRegionO(selectRegion);
            } else if (SharedObject.getInstance().checkRegion(1)) { // D
                SharedObject.getInstance().setRegionD(selectRegion);
            } else {
                SharedObject.getInstance().addWayPoint(selectRegion);
            }
            SharedObject.getInstance().eraseRegionPren();
        }
    }

    private Region getSelectRegion(Position lastClick) {
        Position curClick = new Position(mouseX, mouseY);
        Region selectRegion = new Region();
        if (lastClick.x < curClick.x) {//left
            if (lastClick.y < curClick.y) {//up
                selectRegion.leftTop = lastClick;
                selectRegion.rightBtm = curClick;
            } else {//left_down
                Position left_top = new Position(lastClick.x, curClick.y);
                Position right_btm = new Position(curClick.x, lastClick.y);
                selectRegion = new Region(left_top, right_btm);
            }
        } else {//right
            if (lastClick.y < curClick.y) {//up
                Position left_top = new Position(curClick.x, lastClick.y);
                Position right_btm = new Position(lastClick.x, curClick.y);
                selectRegion = new Region(left_top, right_btm);
            } else {
                selectRegion = new Region(curClick, lastClick);
            }
        }

        if (SharedObject.getInstance().checkRegion(0)) // O
        {
            selectRegion.color = PSC.COLORS[0];
        } else if (SharedObject.getInstance().checkRegion(1)) //D
        {
            selectRegion.color = PSC.COLORS[1];
        } else {
            selectRegion.color = PSC.COLORS[SharedObject.getInstance().getWayLayer() + 1];
        }

        return selectRegion;
    }

    private void initMapSurface() {

        mapList = new UnfoldingMap[4];
        mapXList = new float[]{
                0, mapWidth + widthGapDis,
                0, mapWidth + widthGapDis
        };
        mapYList = new float[]{
                mapDownOff, mapDownOff,
                mapDownOff + mapHeight + heighGapDis, mapDownOff + mapHeight + heighGapDis
        };

        for (int i = 0; i < 4; i++) {
            mapList[i] = new UnfoldingMap(this, mapXList[i], mapYList[i], mapWidth, mapHeight,
                    new MapBox.CustomMapBoxProvider(PSC.WHITE_MAP_PATH));
        }

        for (UnfoldingMap map : mapList) {
            map.setZoomRange(1, 20);
            map.zoomAndPanTo(11, PRESENT);
            map.setBackgroundColor(255);
            map.setTweening(false);
            MapUtils.createDefaultEventDispatcher(this, map);
        }

    }

    private void initDataButton() {
        dataButtonList = new EleButton[4];
        dataButtonList[0] = new EleButton(dataButtonXOff, dataButtonYOff + mapDownOff, 70, 20, 0, "DataSelect");
        dataButtonList[1] = new EleButton(mapWidth + widthGapDis + dataButtonXOff, dataButtonYOff + mapDownOff, 70, 20, 1, "DataSelect");
        dataButtonList[2] = new EleButton(dataButtonXOff, mapHeight + mapDownOff + heighGapDis, 70, 20, 2, "DataSelect");
        dataButtonList[3] = new EleButton(mapWidth + widthGapDis + dataButtonXOff, mapHeight + mapDownOff + heighGapDis, 70, 20, 3, "DataSelect");
    }

    private void drawRegion(Region r) {
        if (r == null || r.leftTop == null || r.rightBtm == null) {
            return;
        }

        Position lT = r.leftTop;
        Position rB = r.rightBtm;

        int length = Math.abs(lT.x - rB.x);
        int high = Math.abs(lT.y - rB.y);

        if (mouseMove && r.id == dragRegionId) {
            r.leftTop = new Position(mouseX, mouseY);
            r.rightBtm = new Position(mouseX + length, mouseY + high);
        }

        lT = r.leftTop;
        rB = r.rightBtm;
        stroke(r.color.getRGB());

        length = Math.abs(lT.x - rB.x);
        high = Math.abs(lT.y - rB.y);

        lT = r.leftTop;
        noFill();
        strokeWeight(3);
        rect(lT.x, lT.y, length, high);
    }

    private void drawTraj(Trajectory traj, float xOff, float yOff, int minX, int maxX, int minY, int maxY, UnfoldingMap map) {
        noFill();
        stroke(PSC.COLORS[3].getRGB());
        strokeWeight(1);
        int i = 0;
        while (i < traj.locations.length) {
            Location loc = traj.locations[i];
            ScreenPosition pos = map.getScreenPosition(loc);
            while (i < traj.locations.length && !intoMap(pos, minX, maxX, minY, maxY)) {//找到第一个在内的
                loc = traj.locations[i];
                pos = map.getScreenPosition(loc);
                i += 1;
            }
            if (i == traj.locations.length) {
                break;
            }
            beginShape();
            while (i < traj.locations.length && intoMap(pos, minX, maxX, minY, maxY)) {//找到最后一个在内的
                loc = traj.locations[i];
                pos = map.getScreenPosition(loc);
                vertex(pos.x + xOff, pos.y + yOff);
                i += 1;
            }
            endShape();
        }

    }

    private boolean intoMap(ScreenPosition pos, int minX, int maxX, int minY, int maxY) {
        return (pos.x > minX && pos.x < maxX && pos.y > minY && pos.y < maxY);
    }

    public static void main(String[] args) {
        PApplet.main(DemoInterface.class.getName());
    }

}
