package app;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.providers.MapBox;
import de.fhpotsdam.unfolding.utils.MapUtils;
import draw.TrajDrawManager;
import model.*;
import processing.core.PApplet;
import processing.core.PGraphics;
import swing.MenuWindow;
import swing.SelectDataDialog;
import util.PSC;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;


public class DemoInterface extends PApplet {
    private TrajDrawManager trajDrawManager;
    private PGraphics[][] trajImgMtx;           // the 4 trajImg buffer layers list
    private PGraphics[][] trajImgSltMtx;    // the 4 trajImg buffer list for double select result
    private EleButton[] dataButtonList;

    private float[][] mapLocInfo;
    private static final Location PORTO_CENTER = new Location(41.14, -8.639);//维度经度
    private static final Location PRESENT = PORTO_CENTER;
    private static final int ZOOMLEVEL = 12;

    private UnfoldingMap[] mapList;

    private int[] checkLevel = {-1, -1, -1, -1};
    private Location[] checkCenter = {new Location(-1, -1), new Location(-1, -1),
            new Location(-1, -1), new Location(-1, -1)};

    private int screenWidth;
    private int screenHeight;
    private int optIndex;

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
    private boolean[] imgCleaned = {false, false, false, false};

    private boolean loadFinished = false;
    private int regionId = 0;
    private int dragRegionId = -1;

    private boolean regionDragged = false;
    private Position lastClick;

    private int circleSize = 15;
    private boolean mouseMove = false;
    private boolean dragged = false;

    private boolean mainLayerIsGray = false;

    /* Other interface component */

    private MenuWindow menuWindow;
    private SelectDataDialog selectDataDialog;

    @Override
    public void settings() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        screenWidth = (int) screenSize.getWidth();
        screenHeight = (int) screenSize.getHeight();

        mapWidth = (screenWidth - widthGapDis) / 2;
        mapHeight = (screenHeight - mapDownOff - heighGapDis) / 2;

        SharedObject.getInstance().setMapWidth(mapWidth);
        SharedObject.getInstance().setMapHeight(mapHeight);

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
        trajImgSltMtx = new PGraphics[4][PSC.SELECT_THREAD_NUM];

        // Warning: the constructor of the TrajDrawManager must be called AFTER initBlockList()
        trajDrawManager = new TrajDrawManager(this, mapList, trajImgMtx, trajImgSltMtx,
                null, mapXList, mapYList, mapWidth, mapHeight);
        SharedObject.getInstance().setTrajDrawManager(trajDrawManager);

        viewVisibleList = new boolean[4];
        Arrays.fill(viewVisibleList, true);     // temp
        SharedObject.getInstance().setViewVisibleList(viewVisibleList);

        // init other interface component
        menuWindow = new MenuWindow(screenWidth, mapDownOff - 5, this);
        menuWindow.setVisible(true);
        selectDataDialog = new SelectDataDialog(frame);

        // other settings
        textFont(createFont("宋体", 12));

        (new Thread(this::loadData)).start();
    }

    private void loadData() {
        SharedObject.getInstance().loadTrajData();

        // temp:

        SharedObject.getInstance().setBlockAt(0, BlockType.FULL, -1, -1);
        SharedObject.getInstance().setBlockAt(1, BlockType.VFGS, 0, 0);
        SharedObject.getInstance().setBlockAt(2, BlockType.RAND, 0, -1);

        SharedObject.getInstance().setAllMainColor(PSC.RED);
        SharedObject.getInstance().setAllSltColor(PSC.RED);

        trajDrawManager.startAllNewRenderTask(TrajDrawManager.MAIN);
        loadFinished = true;
    }

    @Override
    public void draw() {
        updateMap(mapController);

        // draw the main traj buffer images
        nextMap:
        for (int mapIdx = 0; mapIdx < 4; mapIdx++) {
            for (PGraphics pg : trajImgMtx[mapIdx]) {
                if (pg == null) {
                    continue nextMap;
                }
                image(pg, mapXList[mapIdx], mapYList[mapIdx]);
            }
        }

        // draw the double select traj buffer images
        nextMap:
        for (int mapIdx = 0; mapIdx < 4; mapIdx++) {
            for (PGraphics pg : trajImgSltMtx[mapIdx]) {
                if (pg == null) {
                    continue nextMap;
                }
                image(pg, mapXList[mapIdx], mapYList[mapIdx]);
            }
        }

        if (regionDragged) {//drag the region, not finished
            drawAllMapRegion(getSelectRegion(lastClick, optIndex));
        }

        for (Region r : SharedObject.getInstance().getRegionOList()) {
            if (r == null)
                continue;
            drawRegion(r);
            strokeWeight(circleSize);
            point(r.leftTop.x, r.leftTop.y);
        }
        for (Region r : SharedObject.getInstance().getRegionDList()) {
            if (r == null)
                continue;
            drawRegion(r);
            strokeWeight(circleSize);
            point(r.leftTop.x, r.leftTop.y);
        }

        for (ArrayList<ArrayList<Region>> regionWList : SharedObject.getInstance().getRegionWList()) {
            if (regionWList == null)
                continue;
            for (ArrayList<Region> wList : regionWList)
                for (Region r : wList) {
                    drawRegion(r);
                    strokeWeight(circleSize);
                    point(r.leftTop.x, r.leftTop.y);
                }
        }

        if (SharedObject.getInstance().isScreenShot()) {
            int totalFileNum = Objects.requireNonNull(new File(PSC.OUTPUT_PATH1).list()).length;

            String path = PSC.OUTPUT_PATH1 + "screenShot_" + (totalFileNum / 2) + ".png";
            saveFrame(path);

            String infilePath = PSC.OUTPUT_PATH1 + "screenShotInfo_" + (totalFileNum / 2) + ".txt";
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(infilePath));
                writer.write(SharedObject.getInstance().getBlockInfo());
                writer.close();
            } catch (IOException ignored) {
            }
            SharedObject.getInstance().setScreenShot(false);
        }

        for (EleButton dataButton : dataButtonList) {
            dataButton.render(this);
        }

        int dataButtonXOff = 2;
        int dataButtonYOff = 2;
        drawInfoTextBox(0, dataButtonXOff, dataButtonYOff + mapDownOff + mapHeight - 20 - 4, 200, 20);
        drawInfoTextBox(1, mapWidth + widthGapDis + dataButtonXOff, dataButtonYOff + mapDownOff + mapHeight - 20 - 4, 200, 20);
        drawInfoTextBox(2, dataButtonXOff, mapHeight + mapDownOff + heighGapDis + mapHeight - 20 - 4, 200, 20);
        drawInfoTextBox(3, mapWidth + widthGapDis + dataButtonXOff, mapHeight + mapDownOff + heighGapDis + mapHeight - 20 - 4, 200, 20);
    }

    private void drawAllMapRegion(Region selectRegion) {
        for (UnfoldingMap map : mapList) {
            drawRegion(selectRegion.getCorresRegion(map));
        }
    }


    @Override
    public void mousePressed() {
        optIndex = getOptIndex(mouseX, mouseY);

        int eleId = -1;
        for (EleButton dataButton : dataButtonList) {
            if (dataButton.isMouseOver(this)) {
                eleId = dataButton.getEleId();
                break;
            }
        }
        if (eleId != -1) {
            // mentioned the init state
            if (eleId > 15) {// for linked
                //TODO @Shangxuan add link logic
                dataButtonList[eleId].colorExg();
            } else if (eleId > 12) {//for control
                //TODO @Shangxuan add control logic, mention the unique only one control
                dataButtonList[eleId].colorExg();
            } else if (eleId > 7) {
                //TODO max the map
                System.out.println("max Map");
            } else if (eleId > 3) {
                int optMapIdx = eleId - 4;      // here mapIdx = eleIdx - 4
                TrajBlock tb = SharedObject.getInstance().getBlockList()[optMapIdx];

                // change main layer color
                Color c = tb.getMainColor();
                c = (c == PSC.RED) ? PSC.GRAY : PSC.RED;
                tb.setMainColor(c);

                // redraw it
                TrajDrawManager tdm = SharedObject.getInstance().getTrajDrawManager();
                tdm.cleanImgFor(optMapIdx, TrajDrawManager.MAIN);
                tdm.startNewRenderTaskFor(optMapIdx, TrajDrawManager.MAIN);
            } else if (loadFinished) {
                System.out.println("open dialog");
                selectDataDialog.showDialogFor(eleId);  // here eleId = mapIdx
            } else {
                System.out.println("not to open dialog");
            }
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
                System.out.println(r.leftTop.x + ", " + r.leftTop.y);
                System.out.println(mouseX + ", " + mouseY);
                if (mouseX >= r.leftTop.x - circleSize / 2 && mouseX <= r.leftTop.x + circleSize / 2
                        && mouseY >= r.leftTop.y - circleSize / 2 && mouseY <= r.leftTop.y + circleSize / 2) {
                    dragRegionId = r.id;
                    mouseMove = !mouseMove;
                    System.out.println(dragRegionId + "," + r.id + ", " + mouseMove);
                    break;
                }
            }
        }
    }

    @Override
    public void mouseReleased() {
        if (dragged && !regionDragged) { //only pan the map
            updateMap();
            dragged = false;
        }
        for (int i = 0; i < mapList.length; ++i) {
            if (viewVisibleList[i] && imgCleaned[i]) {
                trajDrawManager.startNewRenderTaskFor(i);
                imgCleaned[i] = false;
                System.out.println("map " + i + "redrawn");
            }
        }

        if (regionDragged) {
            regionDragged = false;
            Region selectRegion = getSelectRegion(lastClick, optIndex);
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

    @Override
    public void mouseWheel() {
        boolean mapControllerZoomed = false;

        for (int i = 0; i < mapList.length; ++i) {
            if (mouseX >= mapXList[i] && mouseX <= mapXList[i] + mapWidth
                    && mouseY >= mapYList[i] && mouseY <= mapYList[i] + mapHeight) {
                trajDrawManager.cleanImgFor(i);
                trajDrawManager.startNewRenderTaskFor(i);
                System.out.println("map " + i + " zoomed and redrawed");

                if (i == mapController) {
                    mapControllerZoomed = true;
                }
            }
        }

        if (mapControllerZoomed) {
            for (int i = 0; i < mapList.length; ++i) {
                if (viewVisibleList[i] && linkedList[i]) {
                    trajDrawManager.cleanImgFor(i);
                    imgCleaned[i] = true;
                    trajDrawManager.startNewRenderTaskFor(i);
                    System.out.println("map " + i + "zoomed and redrawed");
                }
            }
        }
    }

    @Override
    public void mouseDragged() {
        if (!regionDragged) {
            for (int i = 0; i < mapList.length; i++) {
                trajDrawManager.cleanImgFor(i);
                imgCleaned[i] = true;
            }
            dragged = true;
        }
    }

    private void updateMap() {
        boolean mapControllerPressed = false;

        for (int i = 0; i < mapList.length; ++i) {
            if (mouseX >= mapXList[i] && mouseX <= mapXList[i] + mapWidth
                    && mouseY >= mapYList[i] && mouseY <= mapYList[i] + mapHeight) {

                if (i == mapController) {
                    mapControllerPressed = true;
                }
            }
        }

        if (mapControllerPressed) {
            for (int i = 0; i < mapList.length; ++i) {
                if (viewVisibleList[i] && linkedList[i]) {
                    trajDrawManager.cleanImgFor(i);
                    imgCleaned[i] = true;
                    System.out.println("map " + i + " cleaned");
                }
            }
        }
    }

    private int getOptIndex(int mouseX, int mouseY) {
        for (int i = 0; i < 4; i++) {
            if (mouseX >= mapXList[i] && mouseX <= mapXList[i] + mapWidth
                    && mouseY >= mapYList[i] && mouseY <= mapYList[i] + mapHeight) {
                return i;
            }
        }
        return 0;
    }

    private Region getSelectRegion(Position lastClick, int optIndex) {
        float mx = constrain(mouseX, mapXList[optIndex] + 3 + circleSize / 2, mapXList[optIndex] + mapWidth - 3 - circleSize / 2);
        float my = constrain(mouseY, mapYList[optIndex] + 3 + circleSize / 2, mapYList[optIndex] + mapHeight - 3 - circleSize / 2);

        Position curClick = new Position(mx, my);
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

        if (SharedObject.getInstance().checkRegion(0)) {    // O
            selectRegion.color = PSC.COLOR_LIST[0];
        } else if (SharedObject.getInstance().checkRegion(1)) {     // D
            selectRegion.color = PSC.COLOR_LIST[1];
        } else {
            int nextColorIdx = SharedObject.getInstance().getWayLayer() + 1;
            selectRegion.color = PSC.COLOR_LIST[nextColorIdx];
        }

        selectRegion.initLoc(mapList[optIndex].getLocation(selectRegion.leftTop.x, selectRegion.leftTop.y),
                mapList[optIndex].getLocation(selectRegion.rightBtm.x, selectRegion.rightBtm.y));

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
            map.zoomAndPanTo(ZOOMLEVEL, PRESENT);
            map.setBackgroundColor(255);
            map.setTweening(false);
            MapUtils.createDefaultEventDispatcher(this, map);
        }

    }

    private void initDataButton() {
        dataButtonList = new EleButton[20];
        int dataButtonXOff = 4;
        int dataButtonYOff = 4;
        dataButtonList[0] = new EleButton(dataButtonXOff, dataButtonYOff + mapDownOff, 70, 20, 0, "DataSelect");
        dataButtonList[1] = new EleButton(mapWidth + widthGapDis + dataButtonXOff, dataButtonYOff + mapDownOff, 70, 20, 1, "DataSelect");
        dataButtonList[2] = new EleButton(dataButtonXOff, mapHeight + mapDownOff + heighGapDis, 70, 20, 2, "DataSelect");
        dataButtonList[3] = new EleButton(mapWidth + widthGapDis + dataButtonXOff, mapHeight + mapDownOff + heighGapDis, 70, 20, 3, "DataSelect");

//        dataButtonList[4] = new EleButton(dataButtonXOff, dataButtonYOff + mapDownOff + 35, 70, 20, 4, "ColorExg");
//        dataButtonList[5] = new EleButton(mapWidth + widthGapDis + dataButtonXOff, dataButtonYOff + mapDownOff + 35, 70, 20, 5, "ColorExg");
//        dataButtonList[6] = new EleButton(dataButtonXOff, mapHeight + mapDownOff + heighGapDis + 35, 70, 20, 6, "ColorExg");
//        dataButtonList[7] = new EleButton(mapWidth + widthGapDis + dataButtonXOff, mapHeight + mapDownOff + heighGapDis + 35, 70, 20, 7, "ColorExg");

        for (int i = 4; i < 8; i++) {
            dataButtonList[i] = new EleButton(dataButtonList[i - 4].getX(), dataButtonList[i - 4].getY() + 35, 70, 20, i, "ColorExg");

        }
        for (int i = 8; i < 12; i++) {
            dataButtonList[i] = new EleButton(dataButtonList[i - 4].getX(), dataButtonList[i - 4].getY() + 28, 70, 20, i, "MaxMap");
        }
        for (int i = 12; i < 20; i++) {
            String buttonInfo = (i < 16) ? "Control" : "Linked";

            int yOff = i < 16 ? 35 : 28;
            dataButtonList[i] = new MapControlButton(dataButtonList[i - 4].getX(), dataButtonList[i - 4].getY() + yOff, 70, 20, i, buttonInfo);
        }


    }

    private void drawRegion(Region r) {
        if (r == null || r.leftTop == null || r.rightBtm == null) {
            return;
        }
        noFill();

        Position lT = r.leftTop;
        Position rB = r.rightBtm;

        int length = Math.abs(lT.x - rB.x);
        int high = Math.abs(lT.y - rB.y);

        if (mouseMove && r.id == dragRegionId) {
            float mx = constrain(mouseX, mapXList[optIndex] + 3 + circleSize / 2, mapXList[optIndex] + mapWidth - 3 - length - circleSize / 2);
            float my = constrain(mouseY, mapYList[optIndex] + 3 + circleSize / 2, mapYList[optIndex] + mapHeight - 3 - high - circleSize / 2);

            r.leftTop = new Position(mx, my);
            r.rightBtm = new Position(mx + length, my + high);
        }

        lT = r.leftTop;
        rB = r.rightBtm;
        stroke(r.color.getRGB());

        length = Math.abs(lT.x - rB.x);
        high = Math.abs(lT.y - rB.y);

        lT = r.leftTop;
        strokeWeight(3);
        rect(lT.x, lT.y, length, high);
    }

    private void drawInfoTextBox(int i, int x, int y, int width, int height) {
        TrajBlock tb = SharedObject.getInstance().getBlockList()[i];
        String info = tb.getBlockInfoStr(PSC.DELTA_LIST, PSC.RATE_LIST);

        fill(240, 240, 240, 160);

        stroke(200, 200, 200, 200);
        strokeWeight(1);
        rect(x, y, width, height);

        fill(0x11);
        textAlign(CENTER, CENTER);
        text(info, x + (width / 2), y + (height / 2));
        textAlign(LEFT, TOP);
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

    public static void main(String[] args) {
        PApplet.main(DemoInterface.class.getName());
    }

}
