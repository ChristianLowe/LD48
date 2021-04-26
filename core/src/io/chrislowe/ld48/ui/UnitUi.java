package io.chrislowe.ld48.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import io.chrislowe.ld48.Attack;
import io.chrislowe.ld48.Ld48;
import io.chrislowe.ld48.TileUnit;

import java.util.*;

public class UnitUi {
    public final static int boxWidth = 224;
    public final static int boxHeight = 280;
    public final static int padding = 24;
    public final static int startX = Ld48.screenWidth - padding - boxWidth;
    public final static int startY = Ld48.screenHeight - padding - boxHeight;
    public TileUnit focusUnit;
    public Map<String, Attack> attacks = new HashMap<>();
    public Map<String, String> attacksDesc = new HashMap<>();
    final private Texture bgBox;

    public UnitUi(List<TileUnit> tileUnits) {
        Pixmap pixmap = new Pixmap(boxWidth, boxHeight, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0.8f);
        pixmap.fillRectangle(0, 0, boxWidth, boxHeight);
        bgBox = new Texture(pixmap);
        pixmap.dispose();

        Json json = new Json();
        JsonReader jsonReader = new JsonReader();
        JsonValue attacksJson = jsonReader.parse(Gdx.files.internal("data/attacks.json"));
        attacksJson.forEach(attackJson -> {
            Attack attack = json.fromJson(Attack.class, attackJson.toJson(JsonWriter.OutputType.json));
            attacks.put(attackJson.name, attack);
        });

        tileUnits.forEach(tileUnit -> {
            StringBuilder attacksStr = new StringBuilder();
            for (int i = 0; i < tileUnit.attacks.size(); i++) {
                attacksStr.append('\n');

                String attackName = tileUnit.attacks.get(i);
                attacksStr.append(i + 1).append(": ").append(attackName.toUpperCase(Locale.ROOT));

                Attack attack = attacks.get(attackName);
                attacksStr.append('\n');
                boolean hasStatLine = false;
                if (attack.damage != 0) {
                    attacksStr.append("Damage: ").append(attack.damage);
                    hasStatLine = true;
                }
                if (attack.recoil != 0) {
                    attacksStr.append(" / Recoil: ").append(attack.recoil);
                    hasStatLine = true;
                }
                if (hasStatLine) {
                    attacksStr.append('\n');
                }
                attacksStr.append(attack.desc).append('\n');
            }
            attacksDesc.put(tileUnit.name, attacksStr.toString());
        });
    }

    public void render(SpriteBatch batch, BitmapFont font) {
        if (focusUnit != null) {
            batch.draw(bgBox, startX, startY);
            batch.draw(focusUnit.texture, startX + 12, startY + boxHeight - 44);

            font.setColor(Color.WHITE);
            font.getData().setScale(1.5f);
            font.draw(batch, focusUnit.name, startX + 56, startY + boxHeight - 22);

            font.getData().setScale(1f);

            StringBuilder descStr = new StringBuilder();
            if (focusUnit.currentHealth != 0) {
                descStr.append("HP: ").append(focusUnit.currentHealth)
                        .append('/').append(focusUnit.maxHealth);
            } else {
                descStr.append("HP: ").append(focusUnit.maxHealth);
            }
            if (focusUnit.moves != 0) {
                descStr.append(" & Moves: ").append(focusUnit.moves)
                        .append('/').append(focusUnit.speed);
            } else {
                descStr.append(" & Moves: ").append(focusUnit.speed);
            }

            descStr.append("\n\n");
            descStr.append(attacksDesc.get(focusUnit.name));
            font.draw(batch, descStr, startX + 12, startY + boxHeight - 44);
        }
    }

    public Integer handleInput() {
        // TODO
        return null;
    }
}
