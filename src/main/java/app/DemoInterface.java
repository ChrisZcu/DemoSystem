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
import processing.event.MouseEvent;
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
import java.util.Map;
import java.util.Objects;

import static model.BlockType.NONE;


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
    private boolean intoMaxMap = false;

    private UnfoldingMap[] mapList;
    // 4 -> not show extraMap
    // [-1, -4] -> ready to show the map[0, 3]
    // [0, 3] now it is zoom and pan to mapList[oneMapIdx]
    private int oneMapIdx = 4;
    private boolean isOneMapMode = false;

    private int[] checkLevel = {12, 12, 12, 12};
    private Location[] checkCenter = {PRESENT, PRESENT, PRESENT, PRESENT};
    private int[] levelBeforeOneMapMode = {12, 12, 12, 12, 12};
    private Location[] centerBeforeOneMapMode = {PRESENT, PRESENT, PRESENT, PRESENT, PRESENT};

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

    private final boolean[] viewVisibleList = {true, true, true, true, false};  // is the map view visible
    private final boolean[] linkedList = {true, true, false, false};     // is the map view linked to others
    private final boolean[] imgCleaned = {false, false, false, false, false};
    private int mapController = 0;

    // is the map main layer (i.e. background) visible
    private final boolean[] trajBgVisibleList = {true, true, true, true, true};
    // is the map select layer (i.e. background) visible, not used for now
    private final boolean[] trajSltVisibleList = {true, true, true, true, true};

    private boolean loadFinished = false;
    private int regionId = 0;
    private int dragRegionId = -1;
    private int dragRegionIntoMapId = -1;
    private boolean regionDrawing = false;
    private Position lastClick;

    private int circleSize = 8;
    private boolean mouseMove = false;
    private boolean dragged = false;

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
        PSC.initRegionColorList();
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
                null, mapXList, mapYList);
        SharedObject.getInstance().setTrajDrawManager(trajDrawManager);

        SharedObject.getInstance().setViewVisibleList(viewVisibleList);

        // init other interface component
        menuWindow = new MenuWindow(screenWidth, mapDownOff - 5, this);
        menuWindow.setVisible(true);
        selectDataDialog = new SelectDataDialog(frame);

        // other settings
        textFont(createFont("宋体", 12));

        (new Thread(this::loadData)).start();

        CircleRegionControl.getCircleRegionControl().getGroupsOfCircle().add(new ArrayList<>());
        CircleRegionControl.getCircleRegionControl().getCircleO().add(new ArrayList<>());
        CircleRegionControl.getCircleRegionControl().getCircleD().add(new ArrayList<>());
        CircleRegionControl.getCircleRegionControl().getWayPoint().add(new ArrayList<>());
    }

    @Override
    public void draw() {
        background(220, 220, 220);

        updateMap();
        updateTrajImages();

        //if(CircleRegionControl.getCircleRegionControl().isAddFinished()){
        drawRegions();


        handleScreenShot();
        drawCompoment();
    }

    @Override
    public void mousePressed() {
        if (oneMapIdx == 4) {
            buttonClickListener();
        } else {
            // in one map mode
            handleOneMapBtnPressed(oneMapIdx);
        }

        Location mouse = SharedObject.getInstance().getMapList()[getOptIndex(mouseX, mouseY)].getLocation(mouseX, mouseY);
        CircleRegionControl control = CircleRegionControl.getCircleRegionControl();

        if (SharedObject.getInstance().isDragRegion() && mouseButton == LEFT) {
            //move exist region
            if (control.getCurMovingCircle() != null) {
                control.setCurMovingCircle(null);
                control.setMapOfCurMovingCircle(-1);
            } else {
                for (int i = 0; i < control.getGroupsOfCircle().size(); ++i) {
                    ArrayList<CircleRegion> group = control.getGroupsOfCircle().get(i);
                    for (int j = 0; j < group.size(); ++j) {
                        Location center = group.get(j).getCircleCenter();
                        Location radius = group.get(j).getRadiusLocation();

                        if (calLocationDist(center, mouse) < calLocationDist(center, radius)) {
                            control.setCurMovingCircle(group.get(j));
                            control.setMapOfCurMovingCircle(getOptIndex(mouseX, mouseY));
                            setVisibleCircle(group.get(j));
                            break;
                        }
                    }
                }
            }
        } else if (mouseButton == LEFT) {
            //circle region reuse
            CircleRegion existCircle = null;

            for (int i = 0; i < control.getGroupsOfCircle().size(); ++i) {
                ArrayList<CircleRegion> group = control.getGroupsOfCircle().get(i);
                for (int j = 0; j < group.size(); ++j) {
                    Location center = group.get(j).getCircleCenter();
                    Location radius = group.get(j).getRadiusLocation();

                    if (calLocationDist(center, mouse) < calLocationDist(center, radius)) {
                        existCircle = group.get(j);
                        break;
                    }
                }
            }

            if (existCircle != null) {
                if (isAddValid(existCircle.getKind())) {
                    int newId = control.getGroupsOfCircle().get(control.getCurDrawingGroupId()).size();
                    CircleRegion circle = new CircleRegion(existCircle, control.getCurDrawingGroupId(), newId);
                    circle.setColor(PSC.COLOR_LIST[circle.getKind()]);
                    addCircle(circle);

                    control.addReusedCircle(existCircle, circle);

//                    System.out.print("add ");
//                    System.out.println(existCircle + " into group " + control.getCurDrawingGroupId());
                }
            }
        }

        if (mouseButton == RIGHT && SharedObject.getInstance().checkSelectRegion()) {
            Location center;
            if (!intoMaxMap) {
                optIndex = getOptIndex(mouseX, mouseY);
                center = mapList[optIndex].getLocation(mouseX, mouseY);
            } else {
                center = mapList[4].getLocation(mouseX, mouseY);
            }

            int id = control.getGroupsOfCircle().get(control.getCurDrawingGroupId()).size();
            int kind = isWhichKindOfRegion();

            if (isAddValid(kind)) {
                CircleRegion circle = new CircleRegion(center, center, control.getCurDrawingGroupId(), id, kind);
                circle.setColor(PSC.COLOR_LIST[kind]);

                control.setCurDrawingCircle(circle);
            }
        }

    }

    @Override
    public void mouseClicked(MouseEvent event){
        if(event.getCount()==2){
            mouseWheel();
        }
    }

    @Override
    public void mouseReleased() {
        for (int i = 0; i < 5; ++i) {
            if (viewVisibleList[i] && imgCleaned[i]) {
                trajDrawManager.startNewRenderTaskFor(i);
                imgCleaned[i] = false;
            }
        }
        CircleRegionControl control = CircleRegionControl.getCircleRegionControl();

        if (control.getCurDrawingCircle() != null) {
            if (!control.getCurDrawingCircle().getCircleCenter().equals(control.getCurDrawingCircle().getRadiusLocation())) {
                addCircle(control.getCurDrawingCircle());
                //System.out.println(curDrawingCircle);
            }
            control.setCurDrawingCircle(null);
        }
    }

    @Override
    public void mouseWheel() {
        if (oneMapIdx < 4 && oneMapIdx >= 0) {
            // in one map mode
            trajDrawManager.cleanImgFor(4);
            trajDrawManager.startNewRenderTaskFor(4);
            return;
        }

        if (oneMapIdx == 4) {
            boolean mapControllerZoomed = false;

            for (int i = 0; i < 4; ++i) {
                if (mouseX >= mapXList[i] && mouseX <= mapXList[i] + mapWidth
                        && mouseY >= mapYList[i] && mouseY <= mapYList[i] + mapHeight) {
                    trajDrawManager.cleanImgFor(i);
                    trajDrawManager.startNewRenderTaskFor(i);
                    //System.out.println("map " + i + " zoomed and redrawn");

                    if (i == mapController) {
                        mapControllerZoomed = true;
                    }
                }
            }

            if (mapControllerZoomed) {
                int zoomLevel = mapList[mapController].getZoomLevel();
                Location center = mapList[mapController].getCenter();

                for (int i = 0; i < 4; ++i) {
                    if (i != mapController && viewVisibleList[i] && linkedList[i]) {
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
        } else {
            trajDrawManager.cleanImgFor(4);
            trajDrawManager.startNewRenderTaskFor(4);
        }

    }

    @Override
    public void keyPressed() {
        if (key == 'q') {
            printCircleInfo();
        }
    }

    @Override
    public void mouseDragged() {
        if (mouseButton != RIGHT) {
            if (oneMapIdx == 4) {
                for (int i = 0; i < 4; ++i) {
                    if (mouseX >= mapXList[i] && mouseX <= mapXList[i] + mapWidth
                            && mouseY >= mapYList[i] && mouseY <= mapYList[i] + mapHeight) {
                        trajDrawManager.cleanImgFor(i);
                        imgCleaned[i] = true;
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
            } else {
                // in one map mode
                trajDrawManager.cleanImgFor(4);
                imgCleaned[4] = true;
            }
        }


    }

    private void addCircle(CircleRegion circle) {
        CircleRegionControl control = CircleRegionControl.getCircleRegionControl();

        int curDrawingGroupId = control.getCurDrawingGroupId();

        control.getGroupsOfCircle().get(curDrawingGroupId).add(circle);

        int kind = circle.getKind();
        int id = circle.getId();
        if (kind == 0) {
            control.getCircleO().get(curDrawingGroupId).add(id);
        } else if (kind == 1) {
            if (control.getCircleD().get(curDrawingGroupId).size() != 1) {
                control.getCircleD().get(curDrawingGroupId).add(id);
            }
        } else {
            control.getWayPoint().get(curDrawingGroupId).add(id);
        }
    }

    private void printCircleInfo() {
        CircleRegionControl control = CircleRegionControl.getCircleRegionControl();
        System.out.println("group size: " + control.getGroupsOfCircle().size());
        for (int i = 0; i < control.getGroupsOfCircle().size(); ++i) {
            System.out.println(control.getGroupsOfCircle().get(i));
        }
        for (int i = 0; i < control.getCircleO().size(); ++i) {
            System.out.println(control.getCircleO().get(i));
            System.out.println(control.getCircleD().get(i));
            System.out.println(control.getWayPoint().get(i));
        }
        System.out.println();
    }

    private boolean isAddValid(int kind) {
        CircleRegionControl control = CircleRegionControl.getCircleRegionControl();

        boolean isValid = true;
        if (kind == 0) {
            if (control.getCircleO().get(control.getCurDrawingGroupId()).size() == 1) {
                isValid = false;
            }
        } else if (kind == 1) {
            if (control.getCircleD().get(control.getCurDrawingGroupId()).size() == 1) {
                isValid = false;
            }
        }
        return isValid;
    }

    private void loadData() {
        menuWindow.setTips("Load data ...");
        SharedObject.getInstance().loadTrajData();

        // temp:

        SharedObject.getInstance().setBlockAt(0, BlockType.FULL, -1, -1);
        SharedObject.getInstance().setBlockAt(1, BlockType.VFGS, 0, 0);
        SharedObject.getInstance().setBlockAt(2, BlockType.RAND, 0, -1);

        SharedObject.getInstance().setAllMainColor(PSC.RED);
        SharedObject.getInstance().setAllSltColor(PSC.RED);

        trajDrawManager.startAllNewRenderTask(TrajDrawManager.MAIN);
        loadFinished = true;
        menuWindow.setTips("Data loaded.");
    }

    private void handleScreenShot() {
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
    }

    private void drawRegions() {
        CircleRegionControl control = CircleRegionControl.getCircleRegionControl();

        if (control.isClean()) {
            control.cleanCircleRegions();
        }

        if (!control.isAddFinished()) {
            ArrayList<ArrayList<CircleRegion>> tmpCirGList = CircleRegionControl.getCircleRegionControl().getGroupsOfCircle();
            if (tmpCirGList.get(tmpCirGList.size() - 1).size() == 0) {
                control.setAddFinished(true);
                return;
            }
            control.getGroupsOfCircle().add(new ArrayList<>());
            control.getCircleO().add(new ArrayList<>());
            control.getCircleD().add(new ArrayList<>());
            control.getWayPoint().add(new ArrayList<>());
            control.addCurDrawingGroupId();
            control.setAddFinished(true);
        }

        if (!intoMaxMap) {
            for (ArrayList<CircleRegion> group : control.getGroupsOfCircle()) {
                for (CircleRegion circle : group) {
                    drawRegion(circle);
                }
            }
        } else {
            for (ArrayList<CircleRegion> group : control.getGroupsOfCircle()) {
                for (CircleRegion circle : group) {
                    drawRegion(circle);
                }
            }
        }

        if (control.getCurDrawingCircle() != null) {
            drawRegion(control.getCurDrawingCircle());
        }
    }

    private void drawRegion(CircleRegion circle) {
        if(!circle.isVisible()){
            return;
        }
        CircleRegionControl control = CircleRegionControl.getCircleRegionControl();

        if (circle.equals(control.getCurDrawingCircle())) {
            if (intoMaxMap) {
                circle.setRadiusLocation(mapList[4].getLocation(mouseX, mouseY));
            } else {
                optIndex = getOptIndex(mouseX, mouseY);
                circle.setRadiusLocation(mapList[optIndex].getLocation(mouseX, mouseY));
            }
        }

        if (circle.equals(control.getCurMovingCircle())) {
            int mapId;
            if (intoMaxMap) {
                mapId = 4;
            } else {
                //mapId = getOptIndex(mouseX, mouseY);
                mapId = control.getMapOfCurMovingCircle();
            }

            UnfoldingMap map = SharedObject.getInstance().getMapList()[mapId];
            ScreenPosition pos = map.getScreenPosition(circle.getCircleCenter());
            float x = pos.x;
            float y = pos.y;
            ScreenPosition lastClick = SharedObject.getInstance().getMapList()[mapId].getScreenPosition(circle.getRadiusLocation());
            float radius = (float) Math.pow((Math.pow(x - lastClick.x, 2) + Math.pow(y - lastClick.y, 2)), 0.5);

            if (intoMaxMap) {
                circle.setCircleCenter(mapList[mapId].getLocation(mouseX, mouseY));
                circle.setRadiusLocation(mapList[mapId].getLocation(mouseX + radius, mouseY));
                circle.updateCircleScreenPosition(mapId);
            } else {
                float mx = constrain(mouseX,
                        mapXList[mapId] + 3 + circleSize / 2, mapXList[mapId] + mapWidth - 3 - radius - circleSize / 2);
                float my = constrain(mouseY,
                        mapYList[mapId] + 3 + circleSize / 2, mapYList[mapId] + mapHeight - 3 - radius - circleSize / 2);

                circle.setCircleCenter(mapList[mapId].getLocation(mx, my));
                circle.setRadiusLocation(mapList[mapId].getLocation(mx + radius, my));
                circle.updateCircleScreenPosition(mapId);
            }

            updateReuseCircles(circle);
        }

        if (intoMaxMap) {
            UnfoldingMap map = SharedObject.getInstance().getMapList()[4];
            ScreenPosition pos = map.getScreenPosition(circle.getCircleCenter());
            float x = pos.x;
            float y = pos.y;

            ScreenPosition radiusPosition = SharedObject.getInstance().getMapList()[4].getScreenPosition(circle.getRadiusLocation());
            float radius = (float) Math.pow((Math.pow(x - radiusPosition.x, 2) + Math.pow(y - radiusPosition.y, 2)), 0.5);

            stroke(circle.getColor().getRGB());
            strokeWeight(3);
            noFill();
            ellipseMode(RADIUS);
            ellipse(x, y, radius, radius);

            strokeWeight(circleSize);
            point(x, y);
        } else {
            for (int i = 0; i < 4; ++i) {
                if (i == 3 && SharedObject.getInstance().getBlockList()[3].getBlockType().equals(NONE)) {
                    continue;
                }
                UnfoldingMap map = SharedObject.getInstance().getMapList()[i];
                ScreenPosition pos = map.getScreenPosition(circle.getCircleCenter());
                float x = pos.x;
                float y = pos.y;

                ScreenPosition lastClick = SharedObject.getInstance().getMapList()[i].getScreenPosition(circle.getRadiusLocation());
                float radius = (float) Math.pow((Math.pow(x - lastClick.x, 2) + Math.pow(y - lastClick.y, 2)), 0.5);

                stroke(circle.getColor().getRGB());
                strokeWeight(3);
                noFill();
                ellipseMode(RADIUS);
                ellipse(x, y, radius, radius);

                strokeWeight(circleSize);
                point(x, y);
            }
        }

    }

    private void updateReuseCircles(CircleRegion circle) {
        CircleRegionControl control = CircleRegionControl.getCircleRegionControl();

        if (control.getReuseMap().containsKey(circle)) {
            int pos = control.getReuseMap().get(circle);
            ArrayList<CircleRegion> reusedCircles = control.getReusedCircles().get(pos);

            for (CircleRegion reusedCircle : reusedCircles) {
                reusedCircle.setCircleCenter(circle.getCircleCenter());
                reusedCircle.setRadiusLocation(circle.getRadiusLocation());
            }
        }
    }

    private void setVisibleCircle(CircleRegion circle){
        CircleRegionControl control = CircleRegionControl.getCircleRegionControl();

        if (control.getReuseMap().containsKey(circle)) {
            int pos = control.getReuseMap().get(circle);
            ArrayList<CircleRegion> reusedCircles = control.getReusedCircles().get(pos);

            for (CircleRegion reusedCircle : reusedCircles) {
                reusedCircle.setVisible(false);
            }
            circle.setVisible(true);
        }
    }

    private void drawCompoment() {
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
        drawCanvas(trajImgMtx, trajBgVisibleList);
        // draw the double select traj buffer images
        drawCanvas(trajImgSltMtx, trajSltVisibleList);
    }

    private void drawCanvas(PGraphics[][] trajImageMtx, boolean[] layerVisibleList) {
        nextMap:
        for (int mapIdx = 0; mapIdx < 5; mapIdx++) {
            if (!viewVisibleList[mapIdx] || !layerVisibleList[mapIdx]) {
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
            int mapIdx = eleId % 4;
            // mentioned the init state
            if (eleId > 19) {
                // for linked
                if (mapController != -1 && !linkedList[mapIdx]) {
                    if (!isMapSame(mapController, mapIdx)) {
                        trajDrawManager.cleanImgFor(mapIdx);
                        trajDrawManager.startNewRenderTaskFor(mapIdx);

                        mapList[mapIdx].zoomToLevel(mapList[mapController].getZoomLevel());
                        mapList[mapIdx].panTo(mapList[mapController].getCenter());

//                        System.out.println("map " + (eleId - 16) + "linked and moved");
                    }
                }

                linkedList[mapIdx] = !linkedList[mapIdx];
                dataButtonList[eleId].colorExg();
            } else if (eleId > 15) {
                //for control

                if (mapIdx == mapController) {
                    mapController = -1;
                } else if (mapController == -1) {
                    mapController = mapIdx;
                } else {
                    dataButtonList[mapController + 16].colorExg();
                    mapController = mapIdx;
                }
                dataButtonList[eleId].colorExg();

                if (mapController != -1) {
                    for (int i = 0; i < 4; ++i) {
                        if (viewVisibleList[i] && linkedList[i] && !isMapSame(i, mapController)) {
                            trajDrawManager.cleanImgFor(i);
                            trajDrawManager.startNewRenderTaskFor(i);

                            mapList[i].zoomToLevel(mapList[mapController].getZoomLevel());
                            mapList[i].panTo(mapList[mapController].getCenter());
                        }
                    }
                }
            } else if (eleId > 11) {
                // max the map
                System.out.println("switch one map, mapIdx=" + mapIdx);
                switchOneMapMode(mapIdx);
                intoMaxMap = true;
                SharedObject.getInstance().setIntoMaxMap(true);
            } else if (eleId > 7) {
                // HideBG / ShowBG
                System.out.println("change BG visible, mapIdx=" + mapIdx);
                changeBgVisible(mapIdx);
            } else if (eleId > 3) {
                TrajBlock tb = SharedObject.getInstance().getBlockList()[mapIdx];

                // change main layer color
                Color c = tb.getMainColor();
                c = (c == PSC.RED) ? PSC.GRAY : PSC.RED;
                tb.setMainColor(c);

                // redraw it
                TrajDrawManager tdm = SharedObject.getInstance().getTrajDrawManager();
                tdm.cleanImgFor(mapIdx, TrajDrawManager.MAIN);
                tdm.startNewRenderTaskFor(mapIdx, TrajDrawManager.MAIN);
            } else if (loadFinished) {
                System.out.println("open dialog");
                selectDataDialog.showDialogFor(mapIdx);
            } else {
                System.out.println("not to open dialog");
                menuWindow.setTips("Data not loaded. Not to open the dialog.");
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
            System.out.println(Arrays.toString(levelBeforeOneMapMode));
            System.out.println(Arrays.toString(centerBeforeOneMapMode));

            // update visibleList
            Arrays.fill(viewVisibleList, 0, 4, true);
            viewVisibleList[4] = false;

            // pan back
            int levelAfterOneMapMode = mapList[4].getZoomLevel();
            Location centerAfterOneMapMode = mapList[4].getCenter();

            if (oneMapIdx == mapController) {
                for (int i = 0; i < 4; ++i) {
                    if (viewVisibleList[i] && (linkedList[i] || i == oneMapIdx)) {
                        mapList[i].zoomToLevel(levelAfterOneMapMode);
                        mapList[i].panTo(centerAfterOneMapMode);
                        trajDrawManager.cleanImgFor(i);
                        trajDrawManager.startNewRenderTaskFor(i);
                    } else if (viewVisibleList[i]) {
                        mapList[i].zoomToLevel(levelBeforeOneMapMode[i]);
                        mapList[i].panTo(centerBeforeOneMapMode[i]);
                        trajDrawManager.cleanImgFor(i);
                        trajDrawManager.startNewRenderTaskFor(i);
                    } // else: no need to update
                }
            } else {
                mapList[oneMapIdx].zoomAndPanTo(levelAfterOneMapMode, centerAfterOneMapMode);
                trajDrawManager.cleanImgFor(oneMapIdx);
                trajDrawManager.startNewRenderTaskFor(oneMapIdx);

                for (int i = 0; i < 4; ++i) {
                    if (i != oneMapIdx && viewVisibleList[i]) {
                        mapList[i].zoomToLevel(levelBeforeOneMapMode[i]);
                        mapList[i].panTo(centerBeforeOneMapMode[i]);
                        trajDrawManager.cleanImgFor(i);
                        trajDrawManager.startNewRenderTaskFor(i);
                    }
                }
            }

            oneMapIdx = 4;
        } else {
            oneMapIdx = -mapIdx - 1;
            TrajBlock[] blockList = SharedObject.getInstance().getBlockList();
            blockList[4] = blockList[mapIdx];

            Arrays.fill(viewVisibleList, 0, 4, false);
            viewVisibleList[4] = true;

            for (int i = 0; i < 5; ++i) {
                levelBeforeOneMapMode[i] = mapList[i].getZoomLevel();
                centerBeforeOneMapMode[i] = mapList[i].getCenter();
            }

            System.out.println(Arrays.toString(levelBeforeOneMapMode));
            System.out.println(Arrays.toString(centerBeforeOneMapMode));

            // set max map location
            mapList[4].zoomAndPanTo(maxedMap.getZoomLevel(), maxedMap.getCenter());

            trajDrawManager.cleanImgFor(4);
            trajDrawManager.startNewRenderTaskFor(4);
        }
        background(220, 220, 220);
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
     * When not in one map mode, call it to change
     * the bg shown state for specific map.
     */
    private void changeBgVisible(int mapIdx) {
        boolean visibleNow = !trajBgVisibleList[mapIdx];
        trajBgVisibleList[mapIdx] = visibleNow;
        String str = visibleNow ? "HideBG" : "ShowBG";
        dataButtonList[mapIdx + 8].setEleName(str);
//        for (int i = 8; i < 12; i++) {
//            dataButtonList[i].setEleName(str);
//        }
    }

    /**
     * When in one map mode, call it to change bg show or not
     */
    private void changeBgVisibleOneMap() {
        boolean visibleNow = !trajBgVisibleList[oneMapIdx];
        trajBgVisibleList[oneMapIdx] = visibleNow;
        trajBgVisibleList[4] = visibleNow;
        oneMapButtonList[1].setEleName(visibleNow ?
                "HideBG" : "ShowBG");
    }

    /**
     * Handle the button press event when it is in one map mode
     */
    private void handleOneMapBtnPressed(int oneMapIdx) {
        if (oneMapIdx == 4 || oneMapIdx < 0) {
            // not in one map mode or not ready
            return;
        }
        int eleId = -1;
        for (EleButton dataButton : oneMapButtonList) {
            if (dataButton.isMouseOver(this, true)) {
                eleId = dataButton.getEleId();
                break;
            }
        }
        switch (eleId) {
            case 0:
                // ColorExg
                changeMainColorFor(oneMapIdx);
                trajDrawManager.startNewRenderTaskFor(4, TrajDrawManager.MAIN);
                break;
            case 1:
                // HideBG / ShowBG
                changeBgVisibleOneMap();
                break;
            case 2:
                // MinMap
                switchOneMapMode(oneMapIdx);
                intoMaxMap = false;
                SharedObject.getInstance().setIntoMaxMap(false);
                break;
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
        if (intoMaxMap) {
            return 4;
        }
        for (int i = 0; i < 4; i++) {
            if (mouseX >= mapXList[i] && mouseX <= mapXList[i] + mapWidth
                    && mouseY >= mapYList[i] && mouseY <= mapYList[i] + mapHeight) {
                return i;
            }
        }
        return 0;
    }

    private void initMapSurface() {
        mapList = new UnfoldingMap[5];
        mapXList = new float[]{
                0, mapWidth + widthGapDis,
                0, mapWidth + widthGapDis,
                0
        };
        mapYList = new float[]{
                mapDownOff, mapDownOff,
                mapDownOff + mapHeight + heighGapDis, mapDownOff + mapHeight + heighGapDis,
                0
        };

        for (int i = 0; i < 4; i++) {
            mapList[i] = new UnfoldingMap(this, mapXList[i], mapYList[i], mapWidth, mapHeight,
                    new MapBox.CustomMapBoxProvider(PSC.WHITE_MAP_PATH));
        }
        mapList[4] = new UnfoldingMap(this, mapXList[4], mapYList[4], screenWidth, screenHeight,
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
        dataButtonList = new EleButton[24];
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
        // WARNING: if you want to change this id,
        // remember to change the id reference in changeBgVisible function.
        for (int i = 8; i < 12; i++) {
            dataButtonList[i] = new EleButton(dataButtonList[i - 4].getX(), dataButtonList[i - 4].getY() + 28, 70, 20, i, "HideBG");
        }
        for (int i = 12; i < 16; i++) {
            dataButtonList[i] = new EleButton(dataButtonList[i - 4].getX(), dataButtonList[i - 4].getY() + 28, 70, 20, i, "MaxMap");
        }
        for (int i = 16; i < 24; i++) {
            String buttonInfo = (i < 20) ? "Control" : "Linked";

            int yOff = i < 20 ? 35 : 28;
            dataButtonList[i] = new MapControlButton(dataButtonList[i - 4].getX(), dataButtonList[i - 4].getY() + yOff, 70, 20, i, buttonInfo);
        }

        if (mapController != -1) {
            dataButtonList[mapController + 16].colorExg();
        }
        for (int i = 0; i < 4; ++i) {
            if (linkedList[i]) {
                dataButtonList[i + 20].colorExg();
            }
        }

    }

    /**
     * init button for one map mode.
     */
    private void initOneMapButtonList() {
        oneMapButtonList = new EleButton[3];
        int dataButtonXOff = 4;
        int dataButtonYOff = 4;
        oneMapButtonList[0] = new EleButton(dataButtonXOff,
                dataButtonYOff + mapDownOff, 70, 20, 0, "ColorExg");
        oneMapButtonList[1] = new EleButton(dataButtonXOff,
                dataButtonYOff + mapDownOff + 35, 70, 20, 1, "HideBG");
        oneMapButtonList[2] = new EleButton(dataButtonXOff,
                dataButtonYOff + mapDownOff + 70, 70, 20, 2, "MinMap");
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

    private int isWhichKindOfRegion() {
        if (SharedObject.getInstance().checkRegion(0)) {   // O
            return 0;
        } else if (SharedObject.getInstance().checkRegion(1)) {    // D
            return 1;
        } else if (SharedObject.getInstance().checkRegion(2)) {
            return 2;
        } else {
            return -1;
        }
    }

    private double calLocationDist(Location Loc1, Location Loc2) {
        return Math.pow((Math.pow(Loc1.x - Loc2.x, 2) + Math.pow(Loc1.y - Loc2.y, 2)), 0.5);
    }

    public static void main(String[] args) {
        PApplet.main(DemoInterface.class.getName());
    }

}
