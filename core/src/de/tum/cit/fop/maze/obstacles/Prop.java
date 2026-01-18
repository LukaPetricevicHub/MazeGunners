package de.tum.cit.fop.maze.obstacles;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public class Prop extends Obstacle {

    private final TextureRegion sprite;
    private final PropType type;

    public Prop(Vector2 pos, PropType type, TextureRegion sprite, int tileSize) {
        super(pos, type.tilesW * tileSize, type.tilesH * tileSize, true); // blocks
        this.type = type;
        this.sprite = sprite;
    }

    public TextureRegion getSprite() { return sprite; }
    public PropType getType() { return type; }
}
