package app;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.providers.MapBox;
import de.fhpotsdam.unfolding.utils.MapUtils;
import draw.TrajDrawManager;
import app.SharedObject;
import processing.core.PApplet;
import processing.core.PGraphics;
import util.PSC;

import javax.swing.*;
import java.awt.*;


public class DemoInterface extends PApplet {
    private TrajDrawManager trajDrawManager;
    private PGraphics[][] trajImgMtx;

    private static final Location PORTO_CENTER = new Location(41.14, -8.639);//维度经度
    private static final Location PRESENT = PORTO_CENTER;

    private UnfoldingMap map0;
    private UnfoldingMap map1;
    private UnfoldingMap map2;
    private UnfoldingMap map3;
    private UnfoldingMap[] mapList;

    private int checkLevel = -1;
    private Location checkCenter = new Location(-1, -1);

    private int screenWidth;
    private int screenHeight;

    private int mapWidth;
    private int mapHeigh;

    private boolean[] viewVisibleList; // 可视
    private boolean[] linkedList; // 联动

    public void settings() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        screenWidth = (int) screenSize.getWidth();
        screenHeight = (int) screenSize.getHeight();

        mapWidth = (screenWidth - 6) / 2;
        mapHeigh = (screenHeight - 4) / 2;

        size(screenWidth, screenHeight, P2D);

    }

    public void setup() {
        initMapSuface();
        trajImgMtx = new PGraphics[4][Math.max(PSC.FULL_THREAD_NUM, PSC.SAMPLE_THREAD_NUM)];
        trajDrawManager = new TrajDrawManager(this, mapList, trajImgMtx, null);

        SharedObject.getInstance().setMap(mapList[0]);
    }

    public void draw() {
        boolean mapChanged = true;
        for (UnfoldingMap map : mapList) {
            map.draw();
            mapChanged = checkLevel != map.getZoomLevel() || !checkCenter.equals(map.getCenter());
        }
//
        if (mapChanged) {
            trajDrawManager.startNewRenderTask(-1, viewVisibleList, linkedList); // update the graphics
            UnfoldingMap map = SharedObject.getInstance().getMap();
            checkCenter = map.getCenter();
            checkLevel = map.getZoomLevel();
        }
        for (PGraphics[] pgList : trajImgMtx) {
            for (PGraphics pg : pgList)
                if (pg != null)
                    image(pg, 0, 0);
        }
    }

    private void initMapSuface() {
        mapList = new UnfoldingMap[4];

        // **声明map
        //overview, 左上
        mapList[0] = new UnfoldingMap(this, 0, 0, mapWidth, mapHeigh, new MapBox.CustomMapBoxProvider(PSC.WHITE_MAP_PATH));
        mapList[1] = new UnfoldingMap(this, screenWidth - mapWidth, 0, mapWidth, mapHeigh, new MapBox.CustomMapBoxProvider(PSC.WHITE_MAP_PATH));
        mapList[2] = new UnfoldingMap(this, 0, screenHeight - mapHeigh, mapWidth, mapHeigh, new MapBox.CustomMapBoxProvider(PSC.WHITE_MAP_PATH));
        mapList[3] = new UnfoldingMap(this, screenWidth - mapWidth, screenHeight - mapHeigh, mapWidth, mapHeigh, new MapBox.CustomMapBoxProvider(PSC.WHITE_MAP_PATH));

        for (UnfoldingMap map : mapList) {
            map.setZoomRange(1, 20);
            map.zoomAndPanTo(11, PRESENT);
            map.setBackgroundColor(255);
            map.setTweening(false);
            MapUtils.createDefaultEventDispatcher(this, map);
        }

    }
}
