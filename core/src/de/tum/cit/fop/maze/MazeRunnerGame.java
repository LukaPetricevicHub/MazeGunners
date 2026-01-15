package de.tum.cit.fop.maze;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import games.spooky.gdx.nativefilechooser.NativeFileChooser;
import de.tum.cit.fop.maze.util.AudioManager;
import de.tum.cit.fop.maze.util.GamePreferences;

/**
 * The MazeRunnerGame class represents the core of the Maze Runner game.
 * It manages the screens and global resources like SpriteBatch and Skin.
 */
public class MazeRunnerGame extends Game {
    // Screens
    private MenuScreen menuScreen;
    private GameScreen gameScreen;

    // Sprite Batch for rendering
    private SpriteBatch spriteBatch;

    // UI Skin
    private Skin skin;

    private AudioManager audioManager;
    private GamePreferences preferences;

    // Character animation downwards
    private Animation<TextureRegion> characterDownAnimation;

    private Texture tileset;
    private TextureRegion[][] tiles;

    //Sound
    private Sound wallHitSound;

    /**
     * Constructor for MazeRunnerGame.
     *
     * @param fileChooser The file chooser for the game, typically used in desktop environment.
     */
    public MazeRunnerGame(NativeFileChooser fileChooser) {
        super();
    }

    /**
     * Called when the game is created. Initializes the SpriteBatch and Skin.
     */
    @Override
    public void create() {
        spriteBatch = new SpriteBatch(); // Create SpriteBatch
        skin = new Skin(Gdx.files.internal("craft/craftacular-ui.json")); // Load UI skin

        preferences = new GamePreferences();
        audioManager = new AudioManager();

        audioManager.playBackgroundMusic("music/background.mp3");

        this.loadCharacterAnimation(); // Load character animation

        wallHitSound =Gdx.audio.newSound(Gdx.files.internal("bumpintowall_X5CNQPB.wav"));



        goToMenu(); // Navigate to the menu screen

        tileset = new Texture(Gdx.files.internal("basictiles.png"));
        tiles = TextureRegion.split(tileset, 16, 16);
        if(tiles.length < 2 || tiles[0].length < 4){
            throw new IllegalStateException("basictiles.png doesn't have enough tiles!");
        }
    }

    public void playWallHitSound(){
        if(wallHitSound != null){
            wallHitSound.play(0.6f);
        }
    }

    /**
     * Switches to the menu screen.
     */
    public void goToMenu() {
        if (audioManager != null) {
            audioManager.playBackgroundMusic("music/background.mp3");
        }
        this.setScreen(new MenuScreen(this));
        if (gameScreen != null) {
            gameScreen.dispose();
            gameScreen = null;
        }

        this.setScreen(new MenuScreen(this)); // Set the current screen to MenuScreen
        if (gameScreen != null) {
            gameScreen.dispose(); // Dispose the game screen if it exists
            gameScreen = null;
        }
    }

    /**
     * Switches to the game screen.
     */
    public void goToGame() {
        this.setScreen(new LevelMenuScreen(this));
        if (menuScreen != null) {
            menuScreen.dispose();
            menuScreen = null;
        }
    }


    /**
     * Loads the character animation from the character.png file.
     */
    private void loadCharacterAnimation() {
        Texture walkSheet = new Texture(Gdx.files.internal("Main_character.png"));

        int frameWidth = 108;
        int frameHeight = 112;
        int animationFrames = 5;

        int rowIndex = 4; //  5th row (0-based)

        Array<TextureRegion> walkFrames = new Array<>(TextureRegion.class);

        for (int col = 0; col < animationFrames; col++) {
            walkFrames.add(new TextureRegion(
                    walkSheet,
                    col * frameWidth,
                    rowIndex * frameHeight, //  여기만 바뀜
                    frameWidth,
                    frameHeight
            ));
        }

        characterDownAnimation = new Animation<>(0.09f, walkFrames);
        characterDownAnimation.setPlayMode(Animation.PlayMode.LOOP);
    }


    /**
     * Cleans up resources when the game is disposed.
     */
    @Override
    public void dispose() {
        getScreen().hide(); // Hide the current screen
        getScreen().dispose(); // Dispose the current screen
        spriteBatch.dispose(); // Dispose the spriteBatch
        skin.dispose();// Dispose the skin
        tileset.dispose();
        if(wallHitSound != null){
            wallHitSound.dispose();
        }
        if (audioManager != null) {
            audioManager.dispose();
        }

        if(wallHitSound != null){
            wallHitSound.dispose();
        }

    }

    // Getter methods
    public Skin getSkin() {
        return skin;
    }

    public Animation<TextureRegion> getCharacterDownAnimation() {
        return characterDownAnimation;
    }

    public SpriteBatch getSpriteBatch() {
        return spriteBatch;
    }

    public TextureRegion getTile(int row, int col){
        return tiles[row][col];
    }

    public AudioManager getAudioManager() {
        return audioManager;
    }

    public GamePreferences getPreferences() {
        return preferences;
    }

}
