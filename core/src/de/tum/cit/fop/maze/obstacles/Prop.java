package de.tum.cit.fop.maze.obstacles;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public class Prop extends Obstacle {

    private final TextureRegion sprite;
    private final PropType type;

    public Prop(Vector2 pos, PropType type, TextureRegion sprite, int tileSize) {
        super(pos, 0, 0, type.blocksMovement);

        this.type = type;
        this.sprite = sprite;

        // World/collision size is based on tile footprint (NOT sprite crop pixels)
        float w = type.tilesW * tileSize;
        float h = type.tilesH * tileSize;

        this.collisionBounds.set(pos.x, pos.y, w, h);
    }

    public TextureRegion getSprite() {
        return sprite;
    }

    public PropType getType() {
        return type;
    }
}
