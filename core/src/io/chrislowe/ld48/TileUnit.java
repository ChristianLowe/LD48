package io.chrislowe.ld48;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Json;

import java.util.List;
import java.util.Map;

public class TileUnit {
    public String textureName;
    public Texture texture;
    public String name;
    public int currentHealth;
    public int maxHealth;
    public int moves;
    public int speed;
    public int cost;
    public boolean canAttack;
    public int gridX;
    public int gridY;
    public List<String> attacks;

    public void loadTexture(Map<String, Texture> textureMap, String textureName) {
        this.textureName = textureName;
        this.texture = textureMap.get(textureName);
    }

    public TileUnit copy(Map<String, Texture> textureMap) {
        // super.clone() not supported in GWT
        TileUnit tileUnit = new TileUnit();
        tileUnit.name = this.name;
        tileUnit.maxHealth = this.maxHealth;
        tileUnit.speed = this.speed;
        tileUnit.attacks = this.attacks;
        tileUnit.loadTexture(textureMap, this.textureName);
        return tileUnit;
    }
}
