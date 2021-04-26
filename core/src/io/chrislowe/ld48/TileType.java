package io.chrislowe.ld48;

public enum TileType {
    Empty1("tileEmpty1"),
    Launch("tileLaunch"),
    Blue("tileBlue"),
    Red("tileRed");

    String textureName;

    TileType(String textureName) {
        this.textureName = textureName;
    }
}
