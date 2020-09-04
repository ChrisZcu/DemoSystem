package app;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.providers.MapBox;
import de.fhpotsdam.unfolding.utils.MapUtils;
import draw.TrajDrawManager;
import model.BlockType;
import processing.core.PApplet;
import processing.core.PGraphics;
import util.PSC;

import java.awt.*;
import java.util.Arrays;


public class DemoInterface extends PApplet {
    private TrajDrawManager trajDrawManager;
    private PGraphics[][] trajImgMtx;

    private static final Location PORTO_CENTER = new Location(41.14, -8.639);//维度经度
    private static final Location PRESENT = PORTO_CENTER;

    private UnfoldingMap[] mapList;

    private int checkLevel = -1;
    private Location checkCenter = new Location(-1, -1);

    private int screenWidth;
    private int screenHeight;

    private int mapWidth;
    private int mapHeight;
    private int[] mapXList;       // the x coordination of the all maps
    private int[] mapYList;       // the y coordination of the all maps

    private boolean[] viewVisibleList;      // is the map view visible
    private boolean[] linkedList;       // is the map view linked to others

    private boolean loadFinished = false;

    @Override
    public void settings() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Rectangle rect = ge.getMaximumWindowBounds();
        screenWidth = rect.width;
        screenHeight = rect.height - 30;

        mapWidth = (screenWidth - 6) / 2;
        mapHeight = (screenHeight - 4) / 2;

        size(screenWidth, screenHeight, P2D);
    }

    @Override
    public void setup() {
        initMapSurface();
        SharedObject.getInstance().setMap(mapList[0]);
        SharedObject.getInstance().initBlockList();

        trajImgMtx = new PGraphics[4][Math.max(PSC.FULL_THREAD_NUM, PSC.SAMPLE_THREAD_NUM)];
        trajDrawManager = new TrajDrawManager(this, mapList, trajImgMtx, null, mapWidth, mapHeight);

        // move to correct position
        Insets screenInsets = Toolkit.getDefaultToolkit()
                .getScreenInsets(frame.getGraphicsConfiguration());
        System.out.println("screenInsets.left = " + screenInsets.left);
        System.out.println("screenInsets.top = " + screenInsets.top);
        surface.setLocation(screenInsets.left - 4, screenInsets.top);

        (new Thread(this::loadData)).start();
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

        if (mapChanged) {
            for (PGraphics[] trajImgList : trajImgMtx) {
                Arrays.fill(trajImgList, null);
            }
            System.out.println("changed");
            trajDrawManager.startNewRenderTask(-1, viewVisibleList, linkedList); // update the graphics
            UnfoldingMap map = SharedObject.getInstance().getMap();
            checkCenter = map.getCenter();
            checkLevel = map.getZoomLevel();
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

    private void initMapSurface() {
        mapList = new UnfoldingMap[4];
        mapXList = new int[]{0, screenWidth - mapWidth, 0, screenWidth - mapWidth};
        mapYList = new int[]{0, 0, screenHeight - mapHeight, screenHeight - mapHeight};

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
}
