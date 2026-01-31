package de.tum.cit.fop.maze.obstacles;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Rectangle;


public class LightningTrap extends Obstacle {

    // Gameplay
    private static final int TRAP_DAMAGE = 1;
    private static final float RETRIGGER_COOLDOWN = 3f;
    private static final float SPEED_PENALTY = 0.1f;
    private static final float VISION_PENALTY = 0.5f;
    private static final float DEBUFF_MAX_DURATION = 3f;

    private boolean playerOnTrap = false;
    private float cooldownTimer = 0f;
    private boolean triggeredThisFrame = false;


    // Animation
    private static final float FRAME_DURATION = 0.08f;
    private static final int FRAME_COLS = 12;
    private static final int FRAME_ROWS = 1;


    private static final Texture IDLE_TEX = new Texture("Traps/LightningTrapIdle.png");
    private static final Texture TRIGGER_TEX = new Texture("Traps/LightningTrapTriggered.png");

    private static final Animation<TextureRegion> IDLE_ANIM;
    private static final Animation<TextureRegion> TRIGGER_ANIM;

    private final boolean vertical;

    public boolean disabled = false;

    private Vector2 wallAnchorA;
    private Vector2 wallAnchorB;



    static {
        IDLE_ANIM = buildAnimation(IDLE_TEX,0,11,true);
        TRIGGER_ANIM = buildAnimation(TRIGGER_TEX,0,FRAME_COLS - 1,false);
    }


    private static Animation<TextureRegion> buildAnimation(Texture tex,int startFrame,int endFrame,boolean loop) {

        TextureRegion[][] split = TextureRegion.split(tex,tex.getWidth() / FRAME_COLS,tex.getHeight() / FRAME_ROWS);

        int frameCount = endFrame - startFrame + 1;
        TextureRegion[] frames = new TextureRegion[frameCount];

        for(int i = 0;i < frameCount;i++) {
            frames[i] = split[0][startFrame + i];
        }

        Animation<TextureRegion> anim = new Animation<>(FRAME_DURATION,frames);

        if (loop) {
            anim.setPlayMode(Animation.PlayMode.LOOP);
        } else{
            anim.setPlayMode(Animation.PlayMode.NORMAL);
        }

        return anim;
    }



    // State
    private float stateTime = 0f;
    private float activeDebuffTimer = 0f;


    // Constructor
    public LightningTrap(Vector2 spawnPosition, boolean vertical) {
        super(spawnPosition, TRAP_DAMAGE, RETRIGGER_COOLDOWN, 32f, 64f);

        this.vertical = vertical;
        float tileSize = 32f; // maze tile size

        float centerX = spawnPosition.x + getWidth() * 0.5f;
        float centerY = spawnPosition.y + getHeight() * 0.5f;

        if(vertical) {
            wallAnchorA = new Vector2(centerX - tileSize,centerY);
            wallAnchorB = new Vector2(centerX + tileSize,centerY);
        }
        else {
            wallAnchorA = new Vector2(centerX,centerY - tileSize);
            wallAnchorB = new Vector2(centerX,centerY + tileSize);
        }
    }


    public boolean isEffectActive() {
        if(disabled) return false;
        return activeDebuffTimer > 0f;
    }


    public float getSlowMultiplier() {
        return SPEED_PENALTY;
    }

    public float getVisionMultiplier() {
        return VISION_PENALTY;
    }


    // Rendering hook
    public TextureRegion getSprite() {

        if(disabled) {

            int startFrame = 7;
            int frameCount = 1;

            float slowFactor = 2.5f;

            int frame = startFrame + ((int)((stateTime / slowFactor) / FRAME_DURATION) % frameCount);

            return IDLE_ANIM.getKeyFrames()[frame];
        }

        if (isEffectActive()) {
            return TRIGGER_ANIM.getKeyFrame(stateTime);
        }
        return IDLE_ANIM.getKeyFrame(stateTime);
    }


    public float getRotationDegrees() {
        if(vertical) return 90f;
        return 0f;
    }


    public void disable() {
        if (disabled)
            return;

        disabled = true;
        activeDebuffTimer = 0f;
        stateTime = 0f;
    }


    public boolean isHitByBullet(float worldX,float worldY) {
        float HALF_TILE = 16f;
        float maxDist2 = HALF_TILE * HALF_TILE;

        if(wallAnchorA.dst2(worldX,worldY) <= maxDist2) return true;
        return wallAnchorB.dst2(worldX,worldY) <= maxDist2;
    }


    public boolean containsPoint(float worldX,float worldY) {
        Rectangle box = getCollisionBox();
        return box.contains(worldX,worldY);
    }


    public void update(float delta) {
        triggeredThisFrame = false;
        stateTime += delta;

        if (disabled) return;

        if (cooldownTimer > 0f) {
            cooldownTimer -= delta;
            if(cooldownTimer < 0f) { cooldownTimer = 0f; }
        }

        if (playerOnTrap && cooldownTimer <= 0f) trigger();

        if(activeDebuffTimer > 0f) {
            activeDebuffTimer -= delta;
        }
    }


    private void trigger() {
        cooldownTimer = RETRIGGER_COOLDOWN;
        activeDebuffTimer = DEBUFF_MAX_DURATION;
        triggeredThisFrame = true;

        stateTime = 0f;

        ObstacleAudioManager.playLightningTrap();
    }


    public void setPlayerOnTrap(boolean onTrap) {
        this.playerOnTrap = onTrap;
    }

    public boolean didTriggerThisFrame() {
        return triggeredThisFrame;
    }

}
