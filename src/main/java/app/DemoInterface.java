package app;

import draw.TrajDrawManager;
import app.SharedObject;
import processing.core.PApplet;
import processing.core.PGraphics;
import util.PSC;

public class DemoInterface extends PApplet {
    private TrajDrawManager trajDrawManager;
    private PGraphics[][] trajImgMtx;


    public void setup() {
        trajImgMtx = new PGraphics[4][Math.max(PSC.FULL_THREAD_NUM, PSC.SAMPLE_THREAD_NUM)];
        trajDrawManager = new TrajDrawManager(this, SharedObject.getInstance().getMap(), trajImgMtx, null);


    }

    public void draw() {



    }
}
