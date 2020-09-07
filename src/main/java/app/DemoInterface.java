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
    private EleButton[] oneMapButtonList;   // button shown in one map mode

    private float[][] mapLocInfo;
    private static final Location PORTO_CENTER = new Location(41.14, -8.639);//维度经度
    private static final Location PRESENT = PORTO_CENTER;
    private static final int ZOOM_LEVEL = 12;

    private UnfoldingMap[] mapList;
    // 4 -> not show extraMap
    // [-1, -4] -> ready to show the map[0, 3]
    // [0, 3] now it is zoom and pan to mapList[oneMapIdx]
    private int oneMapIdx = 4;
    private boolean isOneMapMode = false;

    private int[] checkLevel = {12, 12, 12, 12};
    private Location[] checkCenter = {PRESENT, PRESENT, PRESENT, PRESENT};

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
    private boolean[] linkedList = {true, true, false, false};     // is the map view linked to others
    private boolean[] imgCleaned = {false, false, false, false};
    private int mapController = 0;

    private boolean loadFinished = false;
    private int regionId = 0;
    private int dragRegionId = -1;
    private int dragRegionIntoMapId = -1;
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
        initOneMapButtonList();     // init button in one map mode
        mapLocInfo = new float[2][];
        mapLocInfo[0] = mapXList;
        mapLocInfo[1] = mapYList;

        SharedObject.getInstance().setMapLocInfo(mapLocInfo);

        background(220, 220, 220);

        SharedObject.getInstance().setMapList(mapList);
        SharedObject.getInstance().initBlockList();

        trajImgMtx = new PGraphics[5][Math.max(PSC.FULL_THREAD_NUM, PSC.SAMPLE_THREAD_NUM)];
        trajImgSltMtx = new PGraphics[5][PSC.SELECT_THREAD_NUM];

        // Warning: the constructor of the TrajDrawManager must be called AFTER initBlockList()
        trajDrawManager = new TrajDrawManager(this, mapList, trajImgMtx, trajImgSltMtx,
                null, mapXList, mapYList, mapWidth, mapHeight);
        SharedObject.getInstance().setTrajDrawManager(trajDrawManager);

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
        updateMap();

        updateTrajImages();

        if (regionDragged) {//drag the region, not finished
            drawAllMapRegion(getSelectRegion(lastClick, optIndex));
        }

        for (Region r : SharedObject.getInstance().getRegionOList()) {
            if (r == null) {
                continue;
            }
            drawRegion(r);
            strokeWeight(circleSize);
            point(r.leftTop.x, r.leftTop.y);
        }
        for (Region r : SharedObject.getInstance().getRegionDList()) {
            if (r == null) {
                continue;
            }
            drawRegion(r);
            strokeWeight(circleSize);
            point(r.leftTop.x, r.leftTop.y);
        }

        for (ArrayList<ArrayList<Region>> regionWList : SharedObject.getInstance().getRegionWList()) {
            if (regionWList == null) {
                continue;
            }
            for (ArrayList<Region> wList : regionWList) {
                for (Region r : wList) {
                    drawRegion(r);
                    strokeWeight(circleSize);
                    point(r.leftTop.x, r.leftTop.y);
                }
            }
        }

        if (SharedObject.getInstance().isScreenShot()) {
            File outputDir = new File(PSC.OUTPUT_PATH);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            int totalFileNum = Objects.requireNonNull(new File(PSC.OUTPUT_PATH).list()).length;

            String path = PSC.OUTPUT_PATH + "screenShot_" + (totalFileNum / 2) + ".png";
            saveFrame(path);

            String infilePath = PSC.OUTPUT_PATH + "screenShotInfo_" + (totalFileNum / 2) + ".txt";
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(infilePath));
                writer.write(SharedObject.getInstance().getBlockInfo());
                writer.close();
                System.out.println("Screenshot Saved");
            } catch (IOException ignored) {
                System.out.println("Save screenshot failed");
            }
            SharedObject.getInstance().setScreenShot(false);
        }

        // add visible logic
        if (oneMapIdx == 4) {
            // not in one map mode
            for (int mapIdx = 0; mapIdx < 4; mapIdx++) {
                if (!viewVisibleList[mapIdx]) {
                    continue;
                }
                for (int eleIdx = mapIdx; eleIdx < dataButtonList.length; eleIdx += 4) {
                    dataButtonList[eleIdx].render(this);
                }
            }
        } else {
            // in one map mode
            for (EleButton btn : oneMapButtonList) {
                btn.render(this);
            }
        }

        int dataButtonXOff = 2;
        int dataButtonYOff = 2;
        if (oneMapIdx == 4) {
            drawInfoTextBox(0, dataButtonXOff, dataButtonYOff + mapDownOff + mapHeight - 20 - 4, 200, 20);
            drawInfoTextBox(1, mapWidth + widthGapDis + dataButtonXOff, dataButtonYOff + mapDownOff + mapHeight - 20 - 4, 200, 20);
            drawInfoTextBox(2, dataButtonXOff, mapHeight + mapDownOff + heighGapDis + mapHeight - 20 - 4, 200, 20);
            drawInfoTextBox(3, mapWidth + widthGapDis + dataButtonXOff, mapHeight + mapDownOff + heighGapDis + mapHeight - 20 - 4, 200, 20);
        } else {
            drawInfoTextBox(4, dataButtonXOff, mapHeight + mapDownOff + heighGapDis + mapHeight - 20 - 4, 200, 20);
        }
    }

    private void updateTrajImages() {
        // draw the main traj buffer images
        drawCanvas(trajImgMtx);
        // draw the double select traj buffer images
        drawCanvas(trajImgSltMtx);
    }

    private void drawCanvas(PGraphics[][] trajImageMtx) {
        nextMap:
        for (int mapIdx = 0; mapIdx < 4; mapIdx++) {
            if (!viewVisibleList[mapIdx]) {
                continue;
            }
            for (PGraphics pg : trajImageMtx[mapIdx]) {
                if (pg == null) {
                    continue nextMap;
                }
                image(pg, mapXList[mapIdx], mapYList[mapIdx]);
            }
        }
    }

    private void drawAllMapRegion(Region selectRegion) {
        for (int i = 0; i < 4; i++) {
            drawRegion(selectRegion.getCorresRegion(i));
        }
    }

    @Override
    public void mousePressed() {
        optIndex = getOptIndex(mouseX, mouseY);

        if (oneMapIdx == 4) {
            buttonClickListener();
        } else {
            // in one map mode
            handleOneMapBtnPressed(oneMapIdx);
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
                if (mouseX >= r.leftTop.x - circleSize / 2 && mouseX <= r.leftTop.x + circleSize / 2
                        && mouseY >= r.leftTop.y - circleSize / 2 && mouseY <= r.leftTop.y + circleSize / 2) {
                    dragRegionId = r.id;
                    dragRegionIntoMapId = r.mapId;
                    mouseMove = !mouseMove;
                    System.out.println(dragRegionId + "," + r.id + ", " + mouseMove);
                    break;
                }
            }
        }
    }

    private void buttonClickListener() {
        // not in one map mode, now there are 4 map in the map
        int eleId = -1;
        for (EleButton dataButton : dataButtonList) {
            boolean visible = viewVisibleList[optIndex];
            if (dataButton.isMouseOver(this, visible)) {
                eleId = dataButton.getEleId();
                break;
            }
        }
        if (eleId != -1) {
            // mentioned the init state
            if (eleId > 15) {
                // for linked
                if (mapController != -1 && !linkedList[eleId - 16]) {
                    if (!isMapSame(mapController, eleId - 16)) {
                        trajDrawManager.cleanImgFor(eleId - 16);
                        trajDrawManager.startNewRenderTaskFor(eleId - 16);

                        mapList[eleId - 16].zoomToLevel(mapList[mapController].getZoomLevel());
                        mapList[eleId - 16].panTo(mapList[mapController].getCenter());

//                        System.out.println("map " + (eleId - 16) + "linked and moved");
                    }
                }

                linkedList[eleId - 16] = !linkedList[eleId - 16];
                dataButtonList[eleId].colorExg();
            } else if (eleId > 11) {
                //for control

                if (eleId - 12 == mapController) {
                    mapController = -1;
                } else if (mapController == -1) {
                    mapController = eleId - 12;
                } else {
                    dataButtonList[mapController + 12].colorExg();
                    mapController = eleId - 12;
                }
                dataButtonList[eleId].colorExg();

                if (mapController != -1) {
                    for (int i = 0; i < 4; ++i) {
                        if (viewVisibleList[i] && linkedList[i] && !isMapSame(i, mapController)) {
                            trajDrawManager.cleanImgFor(i);
                            trajDrawManager.startNewRenderTaskFor(i);

                            mapList[i].zoomToLevel(mapList[mapController].getZoomLevel());
                            mapList[i].panTo(mapList[mapController].getCenter());

                            //System.out.println("map " + (eleId - 12) + " moved");
                        }
                    }
                }
            } else if (eleId > 7) {
                // max the map
                System.out.println("switch one map : " + oneMapIdx);
                switchOneMapMode(eleId % 4);
            } else if (eleId > 3) {
                // FIXME stupid code
                int optMapIdx = (oneMapIdx >= 0 && oneMapIdx <= 3) ? 4 : eleId % 4;
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
                selectDataDialog.showDialogFor(eleId % 4);
            } else {
                System.out.println("not to open dialog");
            }
        }
    }

    /**
     * Switch between one map mode and 4 map mode.
     *
     * @param mapIdx the map that need to maximize / pan back
     */
    private void switchOneMapMode(int mapIdx) {
        UnfoldingMap maxedMap = mapList[mapIdx];
        if (oneMapIdx != 4) {
            // pan back
            mapList[oneMapIdx].zoomAndPanTo(mapList[4].getZoomLevel(), mapList[4].getCenter());
            oneMapIdx = 4;
            Arrays.fill(viewVisibleList, true);
        } else {
            oneMapIdx = -mapIdx - 1;
            TrajBlock[] blockList = SharedObject.getInstance().getBlockList();
            blockList[4] = blockList[mapIdx];
            Arrays.fill(viewVisibleList, false);

            // set max map location
            mapList[4].zoomAndPanTo(maxedMap.getZoomLevel(), maxedMap.getCenter());
        }
        background(220, 220, 220);
        System.out.println(oneMapIdx);
    }

    /**
     * Change the main color between {@link PSC#RED} and {@link PSC#GRAY}.
     *
     * @param blockIdx notice that the block obj with index 4 is a shadow
     *                 copy of one obj in 0-3
     */
    private void changeMainColorFor(int blockIdx) {
        TrajBlock tb = SharedObject.getInstance().getBlockList()[blockIdx];

        // change main layer color
        Color c = tb.getMainColor();
        c = (c == PSC.RED) ? PSC.GRAY : PSC.RED;
        tb.setMainColor(c);
    }

    /**
     * Handle the button press event when it is in one map mode
     */
    private void handleOneMapBtnPressed(int oneMapIdx) {
        if (oneMapIdx == 4 || oneMapIdx < 0) {
            // not in one map mode or not ready
            return;
        }
        if (oneMapButtonList[0].isMouseOver(this, true)) {
            // ColorExg
            changeMainColorFor(oneMapIdx);
        } else if (oneMapButtonList[1].isMouseOver(this, true)) {
            // MinMap
            switchOneMapMode(oneMapIdx);
        }
    }

    @Override
    public void mouseReleased() {
        for (int i = 0; i < 4; ++i) {
            if (imgCleaned[i]) {
                trajDrawManager.startNewRenderTaskFor(i);
            }
        }

        for (int i = 0; i < 4; ++i) {
            if (viewVisibleList[i] && imgCleaned[i]) {
                trajDrawManager.startNewRenderTaskFor(i);
                imgCleaned[i] = false;
                //System.out.println("map " + i + " redrawn");
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

        for (int i = 0; i < 4; ++i) {
            if (mouseX >= mapXList[i] && mouseX <= mapXList[i] + mapWidth
                    && mouseY >= mapYList[i] && mouseY <= mapYList[i] + mapHeight) {
                trajDrawManager.cleanImgFor(i);
                trajDrawManager.startNewRenderTaskFor(i);
                //System.out.println("map " + i + " zoomed and redrawed");

                if (i == mapController) {
                    mapControllerZoomed = true;
                }
            }
        }

        if (mapControllerZoomed) {
            int zoomLevel = mapList[mapController].getZoomLevel();
            Location center = mapList[mapController].getCenter();

            for (int i = 0; i < 4; ++i) {
                if (i!=mapController&&viewVisibleList[i] && linkedList[i]) {
                    mapList[i].zoomToLevel(zoomLevel);
                    mapList[i].panTo(center);

                    checkLevel[i] = zoomLevel;
                    checkCenter[i] = center;

                    trajDrawManager.cleanImgFor(i);
                    trajDrawManager.startNewRenderTaskFor(i);

                    //System.out.println("map " + i + " zoomed and redrawn");
                }
            }
            checkLevel[mapController] = zoomLevel;
            checkCenter[mapController] = center;
        }
    }

    @Override
    public void mouseDragged() {
        if (!regionDragged) {
            for (int i = 0; i < 4; ++i) {
                if (mouseX >= mapXList[i] && mouseX <= mapXList[i] + mapWidth
                        && mouseY >= mapYList[i] && mouseY <= mapYList[i] + mapHeight) {
                    trajDrawManager.cleanImgFor(i);
                    imgCleaned[i] = true;

                    //System.out.println("map " + i + " cleaned");
                }
            }

            if (mapController != -1 && imgCleaned[mapController]) {
                for (int i = 0; i < 4; ++i) {
                    if (i != mapController && viewVisibleList[i] && linkedList[i]) {
                        trajDrawManager.cleanImgFor(i);
                        imgCleaned[i] = true;
                        //System.out.println("map " + i + " cleaned");
                    }
                }
            }
        }
    }

    private void updateMap() {
        if (oneMapIdx < 0) {
            // switch to extraMap mode
            oneMapIdx = -oneMapIdx - 1;
            System.out.println("target : " + oneMapIdx);
            UnfoldingMap targetMap = mapList[oneMapIdx];
            mapList[4].zoomAndPanTo(targetMap.getZoomLevel(), targetMap.getCenter());
        }

        if (oneMapIdx != 4) {
            // show extraMap
            mapList[4].draw();
            return;
        }

        for (int i = 0; i < 4; ++i) {
            if (viewVisibleList[i]) {
                mapList[i].draw();
            }
        }

        if (mapController != -1) {
            boolean mapChanged = checkLevel[mapController] != mapList[mapController].getZoomLevel()
                    || !isLocationSame(checkCenter[mapController], mapList[mapController].getCenter());

            if (mapChanged) {
                int zoomLevel = mapList[mapController].getZoomLevel();
                Location center = mapList[mapController].getCenter();

                for (int i = 0; i < 4; ++i) {
                    if (i != mapController && linkedList[i] && viewVisibleList[i]) {
                        mapList[i].zoomToLevel(zoomLevel);
                        mapList[i].panTo(center);

                        checkLevel[i] = zoomLevel;
                        checkCenter[i] = center;

                        //System.out.println("mapController " + mapController + " changed and map " + i + "moved");
                    }
                }
                checkLevel[mapController] = zoomLevel;
                checkCenter[mapController] = center;
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
        mapList = new UnfoldingMap[5];
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
        mapList[4] = new UnfoldingMap(this, mapXList[0], mapYList[0], screenWidth, screenHeight,
                new MapBox.CustomMapBoxProvider(PSC.WHITE_MAP_PATH));

        for (UnfoldingMap map : mapList) {
            map.setZoomRange(1, 20);
            map.zoomAndPanTo(ZOOM_LEVEL, PRESENT);
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

        if (mapController != -1) {
            dataButtonList[mapController + 12].colorExg();
        }
        for (int i = 0; i < 4; ++i) {
            if (linkedList[i]) {
                dataButtonList[i + 16].colorExg();
            }
        }

    }

    /**
     * init button for one map mode.
     */
    private void initOneMapButtonList() {
        oneMapButtonList = new EleButton[2];
        int dataButtonXOff = 4;
        int dataButtonYOff = 4;
        oneMapButtonList[0] = new EleButton(dataButtonXOff,
                dataButtonYOff + mapDownOff, 70, 20, 0, "ColorExg");
        oneMapButtonList[1] = new EleButton(dataButtonXOff,
                dataButtonYOff + mapDownOff + 35, 70, 20, 1, "MinMap");
    }

    private void drawRegion(Region r) {
        if (r == null || r.leftTop == null || r.rightBtm == null) {
            return;
        }

        r.updateScreenPosition();
        Position lT = r.leftTop;
        Position rB = r.rightBtm;

        if (lT.x < mapXList[r.mapId] || lT.y < mapYList[r.mapId] ||
                rB.x > mapXList[r.mapId] + mapWidth || rB.y > mapYList[r.mapId] + mapHeight) {
            return;
        }

        noFill();


        int length = Math.abs(lT.x - rB.x);
        int high = Math.abs(lT.y - rB.y);

        if (mouseMove && r.id == dragRegionId && r.mapId == dragRegionIntoMapId) {
            float mx = constrain(mouseX, mapXList[optIndex] + 3 + circleSize / 2, mapXList[optIndex] + mapWidth - 3 - length - circleSize / 2);
            float my = constrain(mouseY, mapYList[optIndex] + 3 + circleSize / 2, mapYList[optIndex] + mapHeight - 3 - high - circleSize / 2);

            r.setLeftTopLoc(mapList[dragRegionIntoMapId].getLocation(mx, my));
            r.setRightBtmLoc(mapList[dragRegionIntoMapId].getLocation(mx + length, my + high));

            SharedObject.getInstance().updateRegionList(r);

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
        boolean visible = i == 4 || viewVisibleList[i];
        if (!visible) {
            return;
        }
        String info;
        TrajBlock tb = SharedObject.getInstance().getBlockList()[i];

        info = tb.getBlockInfoStr(PSC.DELTA_LIST, PSC.RATE_LIST);

        fill(240, 240, 240, 160);

        stroke(200, 200, 200, 200);
        strokeWeight(1);
        rect(x, y, width, height);

        fill(0x11);
        textAlign(CENTER, CENTER);
        text(info, x + (width / 2), y + (height / 2));
        textAlign(LEFT, TOP);
    }

    private static boolean isFloatEqual(float a, float b) {
        return abs(a - b) <= min(abs(a), abs(b)) * 0.000001;
    }

    private static boolean isLocationSame(Location l1, Location l2) {
        return isFloatEqual(l1.x, l2.x) && isFloatEqual(l1.y, l2.y);
    }

    private boolean isMapSame(int m1, int m2) {
        int zoomLevel1 = mapList[m1].getZoomLevel();
        Location center1 = mapList[m1].getCenter();
        int zoomLevel2 = mapList[m2].getZoomLevel();
        Location center2 = mapList[m2].getCenter();

        return zoomLevel1 == zoomLevel2 && isLocationSame(center1, center2);
    }

    public static void main(String[] args) {
        PApplet.main(DemoInterface.class.getName());
    }

}
