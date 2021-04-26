package io.chrislowe.ld48;

public class MPoint extends Point {
    public Point moveDir;

    public MPoint(int x, int y) {
        super(x, y);
    }

    public MPoint(int x, int y, Point moveDir) {
        super(x, y);
        this.moveDir = moveDir;
    }

    public MPoint(int x, int y, MPoint parent) {
        super(x, y);
        this.moveDir = parent.moveDir;
    }
}
