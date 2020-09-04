package model;


import processing.core.PApplet;

public class EleButton extends Element {
    public EleButton(int x, int y, int width, int height, int eleId, String eleName) {
        super(x, y, width, height);
        this.eleId = eleId;
        this.eleName = eleName;
    }

    @Override
    public void render(PApplet pApplet) {
        pApplet.fill(112, 128, 144);

        pApplet.stroke(112, 128, 144);
        pApplet.strokeWeight(2);
        pApplet.rect(x, y, width, height);

        pApplet.fill(0x11);
        pApplet.textAlign(CENTER, CENTER);
        pApplet.text(eleName, x + (width / 2), y + (height / 2));
        pApplet.textAlign(LEFT, TOP);
    }

    public void render(int x, int y, PApplet pApplet) {
        pApplet.fill(112, 128, 144);

        pApplet.stroke(112, 128, 144);
        pApplet.strokeWeight(2);
        pApplet.rect(x, y, width, height);

        pApplet.fill(0x11);
        pApplet.textAlign(CENTER, CENTER);
        pApplet.text(eleName, x + (width / 2), y + (height / 2));
        pApplet.textAlign(LEFT, TOP);
    }

    public int getEleId() {
        return eleId;
    }
}