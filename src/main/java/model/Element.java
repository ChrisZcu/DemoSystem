package model;

import processing.core.PApplet;

public class Element extends PApplet {
    int x, y, width, height;
    int eleId;
    String eleName;

    public Element(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        eleId = -1;
        eleName = "None";
    }

    public void render(PApplet pApplet) {
        pApplet.noFill();
        pApplet.rect(x, y, width, height);
    }

    public boolean isMouseOver(PApplet pApplet) {
        int mouseX = pApplet.mouseX;
        int mouseY = pApplet.mouseY;

        return (mouseX > x) && (mouseX < (x + width)) &&
                (mouseY > y) && (mouseY < (y + height));
    }
}