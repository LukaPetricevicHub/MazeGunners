package de.tum.cit.fop.maze.items;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import de.tum.cit.fop.maze.player.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * A collectable coin that gives the player coins when picked up.
 *
 * Coins are animated and shared animations are cached per coin value
 * to avoid loading the same textures multiple times.
 */
public class Coin extends Collectable {

    private final int amount;
    private float stateTime = 0f;

    private static final Map<Integer, AnimBundle> CACHE = new HashMap<>();

    private static final float DRAW_W = 32f;
    private static final float DRAW_H = 32f;

    /**
     * Creates a coin at the given position with the given value.
     *
     * @param x world x-position
     * @param y world y-position
     * @param amount coin value (e.g. 5, 20, 50)
     */
    public Coin(float x, float y, int amount) {
        super(x, y, "coin5.png"); // placeholder
        this.amount = amount;

        if (this.texture != null) {
            this.texture.dispose();
            this.texture = null;
        }

        ensureLoaded(amount);
    }

    /**
     * Adds the coin value to the player and marks the coin as collected.
     */
    @Override
    public void applyEffect(Player player) {
        player.addCoins(amount);
        collect();
    }

    /**
     * Renders the animated coin.
     */
    @Override
    public void render(SpriteBatch batch) {
        if (!isActive()) return;

        AnimBundle b = CACHE.getOrDefault(amount, CACHE.get(1));

        stateTime += Gdx.graphics.getDeltaTime();
        TextureRegion frame = b.animation.getKeyFrame(stateTime, true);

        batch.draw(frame, bounds.x, bounds.y, DRAW_W, DRAW_H);
    }


    private static class AnimBundle {
        Texture sheet;
        Animation<TextureRegion> animation;

        AnimBundle(Texture s, Animation<TextureRegion> a) {
            sheet = s;
            animation = a;
        }
    }

    private static void ensureLoaded(int amount) {
        if (CACHE.containsKey(amount)) return;

        String file = switch (amount) {
            case 5 -> "coin5.png";
            case 20 -> "coin20.png";
            case 50 -> "coin50.png";
            default -> "coin2.png";
        };

        int FRAME_W = 16;
        int FRAME_H = 16;

        Texture sheet = new Texture(file);
        TextureRegion[][] split = TextureRegion.split(sheet, FRAME_W, FRAME_H);

        TextureRegion[] frames = new TextureRegion[split.length * split[0].length];
        int idx = 0;

        for (TextureRegion[] row : split) {
            for (TextureRegion r : row) {
                frames[idx++] = r;
            }
        }

        Animation<TextureRegion> anim = new Animation<>(0.10f, frames);
        anim.setPlayMode(Animation.PlayMode.LOOP);

        CACHE.put(amount, new AnimBundle(sheet, anim));
    }

    /**
     * Disposes all shared coin textures and clears the cache.
     * Should be called once when the game shuts down.
     */
    public static void disposeShared() {
        for (AnimBundle b : CACHE.values()) {
            b.sheet.dispose();
        }
        CACHE.clear();
    }
}
