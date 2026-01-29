package de.tum.cit.fop.maze.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.player.Player;

public class GamblingScreen implements Screen {

    private final MazeRunnerGame game;
    private final Stage stage;

    // ✅ allow null for menu gambling
    private final Player player;

    private Label coinsLabel;
    private Label resultLabel;

    // Menu gambling (no player)
    public GamblingScreen(MazeRunnerGame game) {
        this.game = game;
        this.player = null;
        this.stage = new Stage(new ScreenViewport(), game.getSpriteBatch());
    }

    // In-game gambling (uses current run coins)
    public GamblingScreen(MazeRunnerGame game, Player player) {
        this.game = game;
        this.player = player;
        this.stage = new Stage(new ScreenViewport(), game.getSpriteBatch());
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);

        Table root = new Table();
        root.setFillParent(true);
        root.center();
        stage.addActor(root);

        Label title = new Label("GAMBLING", game.getSkin(), "title");
        title.setAlignment(Align.center);

        coinsLabel = new Label("", game.getSkin());
        coinsLabel.setAlignment(Align.center);

        resultLabel = new Label("Pick a mode.", game.getSkin());
        resultLabel.setAlignment(Align.center);

        TextButton slotBtn = new TextButton("SLOT (10 coins)  50%: +1 Skill Point", game.getSkin());
        TextButton riskBtn = new TextButton("RISK (20 coins)  30%: +1 Permanent Heart", game.getSkin());
        TextButton allInBtn = new TextButton("ALL-IN (40 coins)  15%: Unlock OLD GAMBLER", game.getSkin());

        TextButton back = new TextButton("Back", game.getSkin());

        slotBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                doGambleSlot();
            }
        });

        riskBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                doGambleRisk();
            }
        });

        allInBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                doGambleAllIn();
            }
        });

        back.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                game.goToMenu();
            }
        });

        root.add(title).padBottom(25).row();
        root.add(coinsLabel).padBottom(10).row();
        root.add(resultLabel).padBottom(25).row();

        root.add(slotBtn).width(700).padBottom(10).row();
        root.add(riskBtn).width(700).padBottom(10).row();
        root.add(allInBtn).width(700).padBottom(25).row();

        root.add(back).width(250);

        refreshCoinsLabel();
    }

    private int getCoins() {
        if (player != null) return player.getCoins();
        return game.getProgression().getCoins();
    }

    private boolean spendCoins(int amount) {
        if (player != null) return player.spendCoins(amount);
        return game.getProgression().spendCoins(amount);
    }

    private void refreshCoinsLabel() {
        coinsLabel.setText("Coins: " + getCoins());
    }

    // =========================
    // MODES
    // =========================
    private void doGambleSlot() {
        int cost = 10;
        if (!spendCoins(cost)) {
            resultLabel.setText("Not enough coins for SLOT.");
            refreshCoinsLabel();
            return;
        }

        boolean win = MathUtils.randomBoolean(0.50f);
        if (win) {
            game.getProgression().addPermanentSkillPoints(1);
            resultLabel.setText("✅ WIN! +1 Permanent Skill Point");
        } else {
            resultLabel.setText("❌ LOST... Better luck next time.");
        }
        refreshCoinsLabel();
    }

    private void doGambleRisk() {
        int cost = 20;
        if (!spendCoins(cost)) {
            resultLabel.setText("Not enough coins for RISK.");
            refreshCoinsLabel();
            return;
        }

        boolean win = MathUtils.randomBoolean(0.30f);
        if (win) {
            game.getProgression().addPermanentHearts(1);
            resultLabel.setText("✅ WIN! +1 Permanent Heart");
        } else {
            resultLabel.setText("❌ LOST... Pain.");
        }
        refreshCoinsLabel();
    }

    private void doGambleAllIn() {
        int cost = 40;
        if (!spendCoins(cost)) {
            resultLabel.setText("Not enough coins for ALL-IN.");
            refreshCoinsLabel();
            return;
        }

        if (game.getProgression().hasOldGamblerUnlocked()) {
            resultLabel.setText("You already unlocked OLD GAMBLER.");
            refreshCoinsLabel();
            return;
        }

        boolean win = MathUtils.randomBoolean(0.15f);
        if (win) {
            game.getProgression().unlockOldGambler();
            resultLabel.setText("✅ JACKPOT! OLD GAMBLER unlocked!");
        } else {
            resultLabel.setText("❌ LOST EVERYTHING. Brutal.");
        }
        refreshCoinsLabel();
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.goToMenu();
        }

        ScreenUtils.clear(0, 0, 0, 1);
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { stage.dispose(); }
}
