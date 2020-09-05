package model;

import app.SharedObject;
import de.fhpotsdam.unfolding.UnfoldingMap;

import java.awt.*;

/**
 * Indicates the left-top position and right-bottom position of selected region.
 */
public class Region {
    public Position leftTop;
    public Position rightBtm;
    public Color color;
    public int id;

    public Region() {

    }

    public Region(Position lt, Position rb) {
        this.leftTop = lt;
        this.rightBtm = rb;
    }

    public boolean equal(Region r) {
        return this.leftTop.equals(r.leftTop) && this.rightBtm.equals(r.rightBtm);
    }

    public void clear() {
        leftTop = rightBtm = null;
    }

    @Override
    public String toString() {
        UnfoldingMap map = SharedObject.getInstance().getMapList()[0];
        //TODO add info for each map
        return null;
    }
}
