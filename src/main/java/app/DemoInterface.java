package app;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.providers.MapBox;
import de.fhpotsdam.unfolding.utils.MapUtils;
import draw.TrajDrawManager;
import model.BlockType;
import model.EleButton;
import processing.core.PApplet;
import processing.core.PGraphics;
import util.PSC;
import util.Swing;

import javax.swing.*;
import java.awt.*;

import static util.Swing.createTopMenu;


public class DemoInterface extends PApplet {
    private TrajDrawManager trajDrawManager;
    private PGraphics[][] trajImgMtx;
    private EleButton[] dataButtonList;

    private static final Location PORTO_CENTER = new Location(41.14, -8.639);//维度经度
    private static final Location PRESENT = PORTO_CENTER;

    private UnfoldingMap[] mapList;

    private int checkLevel = -1;
    private Location checkCenter = new Location(-1, -1);

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

    private boolean[] viewVisibleList;      // is the map view visible
    private boolean[] linkedList;       // is the map view linked to others

    private boolean loadFinished = false;

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
            println("--well yeah something went wrong but i dont think we needa know that");
        }

        initMapSurface();

        initDataButton();
        background(220, 220, 220);

        SharedObject.getInstance().setMap(mapList[0]);
        SharedObject.getInstance().initBlockList();

        trajImgMtx = new PGraphics[4][Math.max(PSC.FULL_THREAD_NUM, PSC.SAMPLE_THREAD_NUM)];
        trajDrawManager = new TrajDrawManager(this, mapList, trajImgMtx, null,
                mapXList, mapYList, mapWidth, mapHeight);

        // move to correct position
//        Insets screenInsets = Toolkit.getDefaultToolkit()
//                .getScreenInsets(frame.getGraphicsConfiguration());
//        System.out.println("screenInsets.left = " + screenInsets.left);
//        System.out.println("screenInsets.top = " + screenInsets.top);
//        surface.setLocation(screenInsets.left - 4, screenInsets.top);


        (new Thread(this::loadData)).start();

        createTopMenu(screenWidth, mapDownOff - 5, frame, this);
    }

    private void loadData() {
        SharedObject.getInstance().loadTrajData();
        SharedObject.getInstance().setBlockAt(0, BlockType.FULL, -1, -1);
        SharedObject.getInstance().setBlockAt(1, BlockType.VFGS, 0, 0);
        SharedObject.getInstance().setBlockAt(2, BlockType.RAND, 0, -1);
        trajDrawManager.startNewRenderTask(-1, null, null);
        loadFinished = true;
    }

    @Override
    public void draw() {
        boolean mapChanged = true;
        for (UnfoldingMap map : mapList) {
            map.draw();
            mapChanged = checkLevel != map.getZoomLevel() || !checkCenter.equals(map.getCenter());
        }
        for (EleButton dataButton : dataButtonList) {
            dataButton.render(this);
        }

        if (mapChanged) {
            //TODO update the map
        }

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
    }

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
            System.out.println("open dialog");
            Swing.getSwingDialog(frame, eleId).setVisible(true);
        } else {
            System.out.println("eleId == -1");
        }
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

    public static void main(String[] args) {
        PApplet.main(DemoInterface.class.getName());
    }
}
