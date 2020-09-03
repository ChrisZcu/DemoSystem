package model;
/**
 * Indicates the left-top position and right-bottom position of selected region.
 *
 * */
public class Region {
    public Position left_top;
    public Position right_btm;

    public int id;

    public Region() {

    }

    public Region(Position lt, Position rb) {
        this.left_top = lt;
        this.right_btm = rb;
    }

    public boolean equal(Region r) {
        return this.left_top.equals(r.left_top) && this.right_btm.equals(r.right_btm);
    }

    public void clear() {
        left_top = right_btm = null;
    }


}
