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

public class GamblingScreen implements Screen {

    private final MazeRunnerGame game;
    private final Stage stage;

    private Label resultLabel;
    private Label statsLabel;

    public GamblingScreen(MazeRunnerGame game) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport(), game.getSpriteBatch());
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        buildUI();
        refreshStats();
    }

    private void buildUI() {
        stage.clear();

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        Label title = new Label("Slot Machine", game.getSkin(), "title");
        title.setAlignment(Align.center);
        root.add(title).padBottom(20).row();

        statsLabel = new Label("", game.getSkin());
        statsLabel.setAlignment(Align.center);
        root.add(statsLabel).padBottom(20).row();

        resultLabel = new Label("Press SPIN!", game.getSkin());
        resultLabel.setAlignment(Align.center);
        root.add(resultLabel).padBottom(20).row();

        TextButton spinBtn = new TextButton("SPIN", game.getSkin());
        spinBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                spin();
            }
        });
        root.add(spinBtn).width(260).padBottom(12).row();

        TextButton backBtn = new TextButton("Back", game.getSkin());
        backBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.goToMenu();
            }
        });
        root.add(backBtn).width(200).padTop(10).row();
    }

    private enum Reward {
        NOTHING,
        SKILL_POINTS,
        PERMANENT_HEARTS,
        ATTACK_GLOVE,
        OLD_GAMBLER
    }

    private void spin() {
        // Example weighting:
        // 40% nothing
        // 25% skill points
        // 15% permanent hearts
        // 15% attack glove
        // 5% hidden achievement
        int roll = MathUtils.random(1, 100);

        Reward reward;
        if (roll <= 40) reward = Reward.NOTHING;
        else if (roll <= 65) reward = Reward.SKILL_POINTS;
        else if (roll <= 80) reward = Reward.PERMANENT_HEARTS;
        else if (roll <= 95) reward = Reward.ATTACK_GLOVE;
        else reward = Reward.OLD_GAMBLER;

        applyReward(reward);
        refreshStats();
    }

    private void applyReward(Reward r) {
        switch (r) {
            case NOTHING -> resultLabel.setText("You got... nothing. Ouch.");
            case SKILL_POINTS -> {
                int sp = MathUtils.random(1, 3);
                game.getProgression().addPermanentSkillPoints(sp);
                resultLabel.setText("You won +" + sp + " Permanent Skill Points!");
            }
            case PERMANENT_HEARTS -> {
                int hearts = 1;
                game.getProgression().addPermanentHearts(hearts);
                resultLabel.setText("You won +" + hearts + " Permanent Heart!");
            }
            case ATTACK_GLOVE -> {
                game.getProgression().addAttackGloveLevel(1);
                resultLabel.setText("You won a Permanent Attack Glove upgrade!");
            }
            case OLD_GAMBLER -> {
                if (!game.getProgression().hasOldGamblerUnlocked()) {
                    game.getProgression().unlockOldGambler();

                    // OPTIONAL: If your AchievementManager has a method to unlock by ID,
                    // call it here. (Change this line to match your manager's API.)
                    // game.getAchievementManager().unlock("old_gambler");
                }
                resultLabel.setText("??? The machine recognizes you... (Hidden)");
            }
        }
    }

    private void refreshStats() {
        var p = game.getProgression();
        statsLabel.setText(
                "Permanent Rewards:\n" +
                        "Skill Points: " + p.getPermanentSkillPoints() + "\n" +
                        "Hearts: " + p.getPermanentHearts() + "\n" +
                        "Attack Glove Lv: " + p.getAttackGloveLevel() + "\n" +
                        "Old Gambler: " + (p.hasOldGamblerUnlocked() ? "YES" : "NO")
        );
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
