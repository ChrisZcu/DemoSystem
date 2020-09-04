package app;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.providers.MapBox;
import de.fhpotsdam.unfolding.utils.MapUtils;
import draw.TrajDrawManager;
import model.EleButton;
import processing.core.PApplet;
import processing.core.PGraphics;
import util.PSC;

import java.awt.*;


public class DemoInterface extends PApplet {
    private TrajDrawManager trajDrawManager;
    private PGraphics[][] trajImgMtx;
    private EleButton[] dataButtonList;

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
    private final int dataButtonXOff = 2; //
    private final int dataButtonYOff = 2;
    private final int mapDownOff = 40;
    private final int heighGapDis = 4;
    private final int widthGapDis = 6;

    private boolean[] viewVisibleList; // 可视
    private boolean[] linkedList; // 联动


    public void settings() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        screenWidth = (int) screenSize.getWidth();
        screenHeight = (int) screenSize.getHeight();

        mapWidth = (screenWidth - widthGapDis) / 2;
        mapHeigh = (screenHeight - mapDownOff - heighGapDis) / 2;

//        size(screenWidth, screenHeight, P2D);
        fullScreen(P2D);
    }

    public void setup() {
        background(220, 220, 220);
        initMapSuface();
        initDataButton();
        trajImgMtx = new PGraphics[4][Math.max(PSC.FULL_THREAD_NUM, PSC.SAMPLE_THREAD_NUM)];
        trajDrawManager = new TrajDrawManager(this, mapList, trajImgMtx, null);


        SharedObject.getInstance().setMap(mapList[0]);
    }

    public void draw() {
        boolean mapChanged = false;
        for (UnfoldingMap map : mapList) {
            map.draw();
            if (checkLevel != map.getZoomLevel() || !checkCenter.equals(map.getCenter()))
                mapChanged = true;
        }
        for (EleButton dataButton : dataButtonList)
            dataButton.render(this);

        if (mapChanged) {
            //TODO update the map
        }
        for (PGraphics[] pgList : trajImgMtx) {
            for (PGraphics pg : pgList)
                if (pg != null)
                    image(pg, 0, 0);
        }
    }

    public void mousePressed() {
        int eleId = -1;
        for (EleButton dataButton : dataButtonList) {
            if (dataButton.isMouseOver(this)) {
                eleId = dataButton.getEleId();
                break;
            }
        }
        if (eleId != -1) {
            //TODO set the dialog visible
            System.out.println("hello data button");
        } else System.out.println("1");
    }

    private void initMapSuface() {
        mapList = new UnfoldingMap[4];

        mapList[0] = new UnfoldingMap(this, 0, mapDownOff, mapWidth, mapHeigh, new MapBox.CustomMapBoxProvider(PSC.WHITE_MAP_PATH));
        mapList[1] = new UnfoldingMap(this, mapWidth + widthGapDis, mapDownOff, mapWidth, mapHeigh, new MapBox.CustomMapBoxProvider(PSC.WHITE_MAP_PATH));
        mapList[2] = new UnfoldingMap(this, 0, mapDownOff + mapHeigh + heighGapDis, mapWidth, mapHeigh, new MapBox.CustomMapBoxProvider(PSC.WHITE_MAP_PATH));
        mapList[3] = new UnfoldingMap(this, mapWidth + widthGapDis, mapDownOff + mapHeigh + heighGapDis, mapWidth, mapHeigh, new MapBox.CustomMapBoxProvider(PSC.WHITE_MAP_PATH));

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
        dataButtonList[2] = new EleButton(dataButtonXOff, mapHeigh + mapDownOff+heighGapDis, 70, 20, 2, "DataSelect");
        dataButtonList[3] = new EleButton(mapWidth + widthGapDis + dataButtonXOff, mapHeigh + mapDownOff+heighGapDis, 70, 20, 3, "DataSelect");
    }

    public static void main(String[] args) {
        PApplet.main(DemoInterface.class.getName());
    }
}
