package io.chrislowe.ld48.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.chrislowe.ld48.Ld48;
import io.chrislowe.ld48.TileUnit;

import java.util.ArrayList;
import java.util.List;

public class LaunchUi {
    public List<TileUnit> tileUnits = new ArrayList<>();
    public Texture unitBackgroundTexture;
    public boolean isActive;
    final private Texture bgBox;

    public LaunchUi() {
        Pixmap pixmap = new Pixmap(Ld48.screenWidth, 48, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.BLACK);
        pixmap.fillRectangle(0, 0, Ld48.screenWidth, 48);
        bgBox = new Texture(pixmap);
        pixmap.dispose();
    }

    public void render(SpriteBatch batch) {
        if (isActive) {
            batch.draw(bgBox, 0, 0);
            for (int i = 0; i < tileUnits.size(); i++) {
                Texture t = tileUnits.get(i).texture;
                float x = 8 + (i * 48), y = 8;
                batch.draw(unitBackgroundTexture, x, y);
                batch.draw(t, x, y);
            }
        }
    }

    public Integer handleInput() {
        // returns index of the clicked unit tile
        if (isActive) {
            int x = Gdx.input.getX(), y = (Ld48.screenHeight - Gdx.input.getY());
            int maxX = 16 + tileUnits.size() * 48;
            if (x > 0 && x < maxX && y > 0 && y < 48) {
                return x / 48;
            }
        }
        return null;
    }
}
