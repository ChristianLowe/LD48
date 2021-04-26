package io.chrislowe.ld48;

import com.badlogic.gdx.math.Rectangle;

public class GridPiece {
    int x;
    int y;
    Rectangle rectangle;
    TileType tileType;
    TileUnit tileUnit;

    public GridPiece(Rectangle rectangle, int x, int y) {
        this.rectangle = rectangle;
        this.x = x;
        this.y = y;
    }
}
