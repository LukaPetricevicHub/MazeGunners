package de.tum.cit.fop.maze.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.util.Random;

import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.player.Player;

public class GamblingScreen implements Screen {

    private final MazeRunnerGame game;
    private final Stage stage;
    private final Player player; // can be null if opened from menu
    private final Random rng = new Random();

    // UI
    private Label coinsLabel;
    private Label infoLabel;

    private TextButton rrBtn, slotsBtn, bjBtn, backBtn;

    // panels
    private Table root;
    private Table menuPanel;
    private Table roulettePanel;
    private Table slotsPanel;
    private Table blackjackPanel;

    // shared rolling label
    private Label rollLabel;
    private boolean rolling = false;

    // blackjack state
    private int bjBet = 10;
    private int bjPlayerTotal = 0;
    private int bjDealerTotal = 0;
    private boolean bjInRound = false;

    public GamblingScreen(MazeRunnerGame game) {
        this(game, null);
    }

    public GamblingScreen(MazeRunnerGame game, Player player) {
        this.game = game;
        this.player = player;
        this.stage = new Stage(new ScreenViewport(), game.getSpriteBatch());
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);

        root = new Table();
        root.setFillParent(true);
        stage.addActor(root);
        root.center();      // <--- important
        root.defaults().center();


        Label title = new Label("GAMBLING", game.getSkin(), "title");
        title.setAlignment(Align.center);

        coinsLabel = new Label("", game.getSkin());
        coinsLabel.setAlignment(Align.center);

        infoLabel = new Label("Pick a mode.", game.getSkin());
        infoLabel.setAlignment(Align.center);
        infoLabel.setWrap(true);

        rollLabel = new Label("", game.getSkin());
        rollLabel.setAlignment(Align.center);
        rollLabel.setColor(Color.LIGHT_GRAY);

        // ---------- MAIN MENU PANEL ----------
        menuPanel = new Table();

        rrBtn = new TextButton("RUSSIAN ROULETTE (10 coins)", game.getSkin());
        slotsBtn = new TextButton("SLOTS (10 coins)", game.getSkin());
        bjBtn = new TextButton("BLACKJACK (bet 5..100)", game.getSkin());

        // BIG buttons
        float btnW = 700f;
        float btnH = 85f;

        menuPanel.add(rrBtn).width(btnW).height(btnH).padBottom(18).row();
        menuPanel.add(slotsBtn).width(btnW).height(btnH).padBottom(18).row();
        menuPanel.add(bjBtn).width(btnW).height(btnH).padBottom(18).row();

        backBtn = new TextButton("Back", game.getSkin());
        backBtn.getLabel().setFontScale(1.0f);

        // ---------- ROULETTE PANEL ----------
        roulettePanel = new Table();
        roulettePanel.defaults().pad(10);
        roulettePanel.setVisible(false);

        Label rrTitle = new Label("RUSSIAN ROULETTE", game.getSkin(), "title");
        rrTitle.setAlignment(Align.center);

        Label rrDesc = new Label(
                "Cost: 10 coins\nWin: +1 Heart\nLose: -1 Heart",
                game.getSkin()
        );
        rrDesc.setAlignment(Align.center);

        TextButton rrPlay = new TextButton("PULL TRIGGER", game.getSkin());
        TextButton rrBack = new TextButton("Back to modes", game.getSkin());

        roulettePanel.add(rrTitle).padBottom(10).row();
        roulettePanel.add(rrDesc).padBottom(10).row();
        roulettePanel.add(rollLabel).width(600).padBottom(15).row();
        roulettePanel.add(rrPlay).width(520).height(75).padBottom(10).row();
        roulettePanel.add(rrBack).width(520).height(65).row();

        // ---------- SLOTS PANEL ----------
        slotsPanel = new Table();
        slotsPanel.defaults().pad(10);
        slotsPanel.setVisible(false);

        Label sTitle = new Label("SLOTS", game.getSkin(), "title");
        sTitle.setAlignment(Align.center);

        Label sDesc = new Label(
                "Cost: 10 coins\nBonuses can be good or bad.\nRare drops: SHOES / GLOVES",
                game.getSkin()
        );
        sDesc.setAlignment(Align.center);

        TextButton sSpin = new TextButton("SPIN", game.getSkin());
        TextButton sBack = new TextButton("Back to modes", game.getSkin());

        slotsPanel.add(sTitle).padBottom(10).row();
        slotsPanel.add(sDesc).padBottom(10).row();
        slotsPanel.add(rollLabel).width(700).padBottom(15).row();
        slotsPanel.add(sSpin).width(520).height(75).padBottom(10).row();
        slotsPanel.add(sBack).width(520).height(65).row();

        // ---------- BLACKJACK PANEL ----------
        blackjackPanel = new Table();
        blackjackPanel.defaults().pad(10);
        blackjackPanel.setVisible(false);

        Label bjTitle = new Label("BLACKJACK", game.getSkin(), "title");
        bjTitle.setAlignment(Align.center);

        Label bjStatus = new Label("", game.getSkin());
        bjStatus.setAlignment(Align.center);

        Label bjTotals = new Label("", game.getSkin());
        bjTotals.setAlignment(Align.center);

        TextField betField = new TextField(String.valueOf(bjBet), game.getSkin());
        betField.setMessageText("5..100");
        betField.setAlignment(Align.center);

        TextButton bjStart = new TextButton("DEAL", game.getSkin());
        TextButton bjHit = new TextButton("HIT", game.getSkin());
        TextButton bjStand = new TextButton("STAND", game.getSkin());
        TextButton bjBack = new TextButton("Back to modes", game.getSkin());

        bjHit.setDisabled(true);
        bjStand.setDisabled(true);

        Table betRow = new Table();
        betRow.add(new Label("Bet:", game.getSkin())).padRight(10);
        betRow.add(betField).width(140).height(40);

        Table actionRow = new Table();
        actionRow.add(bjStart).width(170).height(60).padRight(10);
        actionRow.add(bjHit).width(170).height(60).padRight(10);
        actionRow.add(bjStand).width(170).height(60);

        blackjackPanel.add(bjTitle).padBottom(10).row();
        blackjackPanel.add(betRow).padBottom(10).row();
        blackjackPanel.add(rollLabel).width(700).padBottom(10).row();
        blackjackPanel.add(bjTotals).padBottom(10).row();
        blackjackPanel.add(bjStatus).padBottom(10).row();
        blackjackPanel.add(actionRow).padBottom(10).row();
        blackjackPanel.add(bjBack).width(520).height(65).row();

    // ---------- LAYOUT (CENTERED CONTENT) ----------
        root.clear();
        root.center();

        Table content = new Table();
        content.center();

        content.add(title).padBottom(25).row();
        content.add(coinsLabel).padBottom(8).row();
        content.add(infoLabel).width(900).padBottom(25).row();

    // Put all panels in ONE "stack" cell so centering works nicely
        Stack stack = new Stack();
        stack.add(menuPanel);
        stack.add(roulettePanel);
        stack.add(slotsPanel);
        stack.add(blackjackPanel);

        content.add(stack).padBottom(25).row();
        content.add(backBtn).width(250).height(70);

        root.add(content);


        // ---------- LISTENERS ----------
        backBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                game.goToMenu();
            }
        });

        rrBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                openPanel(roulettePanel);
                infoLabel.setText("Russian Roulette: pay 10 coins, win +1 heart or lose -1 heart.");
            }
        });

        slotsBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                openPanel(slotsPanel);
                infoLabel.setText("Slots: pay 10 coins, roll bonuses (good or bad).");
            }
        });

        bjBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                openPanel(blackjackPanel);
                infoLabel.setText("Blackjack: choose bet 5..100, win pays 2x, push returns bet.");
                bjStatus.setText("Enter bet and press DEAL.");
                bjTotals.setText("");
                rollLabel.setText("");
            }
        });

        rrBack.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                openPanel(menuPanel);
                infoLabel.setText("Pick a mode.");
            }
        });

        sBack.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                openPanel(menuPanel);
                infoLabel.setText("Pick a mode.");
            }
        });

        bjBack.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                openPanel(menuPanel);
                infoLabel.setText("Pick a mode.");
            }
        });

        rrPlay.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                if (rolling) return;
                if (!spendCoins(10)) {
                    infoLabel.setText("Not enough coins for Russian Roulette.");
                    return;
                }
                refreshCoins();
                playRollingAnimation("SPINNING CYLINDER", 1.1f, () -> {
                    boolean win = rng.nextBoolean(); // 50/50
                    if (player == null) {
                        // menu gambling: no player hearts -> just message
                        rollLabel.setText(win ? "CLICK... YOU LIVED (no player in menu)" : "BANG... (no player in menu)");
                        infoLabel.setText("Open gambling from in-game to apply hearts.");
                        return;
                    }

                    if (win) {
                        player.addLife();
                        rollLabel.setText("CLICK... YOU WIN! +1 HEART");
                        infoLabel.setText("Russian Roulette win: +1 heart.");
                    } else {
                        player.takeDamage(1);
                        rollLabel.setText("BANG... YOU LOSE! -1 HEART");
                        infoLabel.setText("Russian Roulette loss: -1 heart.");
                    }
                });
            }
        });

        sSpin.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                if (rolling) return;
                if (!spendCoins(10)) {
                    infoLabel.setText("Not enough coins for Slots.");
                    return;
                }
                refreshCoins();
                playRollingAnimation("ROLLING REELS", 1.25f, () -> {
                    // Weighted outcomes
                    //  - common: +/- speed, +/- damage (placeholder), + skill point
                    //  - rare: SHOES, GLOVES
                    int roll = rng.nextInt(1000);

                    if (roll < 10) { // 1.0% shoes
                        rollLabel.setText("JACKPOT: SHOES!");
                        infoLabel.setText("Shoes: faster + damage by running through enemies (implement in player/combat later).");
                        // You can store unlock in Progression later.
                        return;
                    }
                    if (roll < 20) { // 1.0% gloves
                        rollLabel.setText("JACKPOT: GLOVES!");
                        infoLabel.setText("Gloves: more damage + destroy traps (implement later).");
                        // You can map to progression glove level if you want:
                        // game.getProgression().addAttackGloveLevel(1);
                        return;
                    }

                    if (roll < 260) { // 24% +1 skill point
                        if (player != null) player.setSkillPoints(player.getSkillPoints() + 1);
                        game.getProgression().addPermanentSkillPoints(1); // keep it permanent
                        rollLabel.setText("WIN: +1 SKILL POINT");
                        infoLabel.setText("You gained a skill point (also saved permanently).");
                        return;
                    }

                    if (roll < 520) { // 26% speed up
                        if (player != null) player.activateSpeedBoost(30f, 1.25f);
                        rollLabel.setText("WIN: SPEED UP!");
                        infoLabel.setText("Temporary speed boost for this run.");
                        return;
                    }

                    if (roll < 720) { // 20% speed down
                        if (player != null) player.activateSpeedBoost(20f, 0.80f);
                        rollLabel.setText("BAD LUCK: SPEED DOWN");
                        infoLabel.setText("Temporary speed debuff for this run.");
                        return;
                    }

                    // “damage” placeholder (you need a real damage stat to apply properly)
                    if (roll < 860) { // 14% "damage up"
                        rollLabel.setText("WIN: DAMAGE UP!");
                        infoLabel.setText("You need a player damage stat to apply this. (Hook it into CombatSystem later.)");
                        return;
                    }

                    // 14% "damage down"
                    rollLabel.setText("BAD LUCK: DAMAGE DOWN");
                    infoLabel.setText("You need a player damage stat to apply this. (Hook it into CombatSystem later.)");
                });
            }
        });

        bjStart.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                if (rolling) return;

                int bet = parseIntSafe(betField.getText(), 10);
                bet = MathUtils.clamp(bet, 5, 100);
                betField.setText(String.valueOf(bet));
                bjBet = bet;

                if (!spendCoins(bjBet)) {
                    bjStatus.setText("Not enough coins to bet " + bjBet + ".");
                    return;
                }

                refreshCoins();
                bjInRound = true;
                bjPlayerTotal = 0;
                bjDealerTotal = 0;

                bjHit.setDisabled(true);
                bjStand.setDisabled(true);

                playRollingAnimation("DEALING CARDS", 1.0f, () -> {
                    // basic dealing: 2 cards player, 2 cards dealer (dealer shows one)
                    bjPlayerTotal = drawCard() + drawCard();
                    bjDealerTotal = drawCard() + drawCard();

                    bjTotals.setText("Player: " + bjPlayerTotal + " | Dealer shows: " + (bjDealerTotal - lastDrawnCardValueApprox()));
                    bjStatus.setText("Your move: HIT or STAND.");

                    bjHit.setDisabled(false);
                    bjStand.setDisabled(false);

                    // auto-bust check
                    if (bjPlayerTotal > 21) {
                        endBlackjackRound(false, false, bjStatus, bjTotals);
                    }
                });
            }
        });

        bjHit.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                if (!bjInRound || rolling) return;
                bjPlayerTotal += drawCard();
                bjTotals.setText("Player: " + bjPlayerTotal + " | Dealer: ?");
                if (bjPlayerTotal > 21) {
                    endBlackjackRound(false, false, bjStatus, bjTotals);
                }
            }
        });

        bjStand.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                if (!bjInRound || rolling) return;

                bjHit.setDisabled(true);
                bjStand.setDisabled(true);

                playRollingAnimation("DEALER PLAYS", 0.9f, () -> {
                    // dealer hits until 17+
                    while (bjDealerTotal < 17) {
                        bjDealerTotal += drawCard();
                    }

                    boolean dealerBust = bjDealerTotal > 21;
                    boolean playerWins = dealerBust || bjPlayerTotal > bjDealerTotal;
                    boolean push = (!dealerBust && bjPlayerTotal == bjDealerTotal);

                    endBlackjackRound(playerWins, push, bjStatus, bjTotals);
                });
            }
        });

        refreshCoins();
        refreshButtons();
    }

    private void openPanel(Table panelToShow) {
        menuPanel.setVisible(panelToShow == menuPanel);
        roulettePanel.setVisible(panelToShow == roulettePanel);
        slotsPanel.setVisible(panelToShow == slotsPanel);
        blackjackPanel.setVisible(panelToShow == blackjackPanel);

        rollLabel.clearActions();
        rollLabel.setText("");
        rolling = false;

        refreshButtons();
    }

    private void playRollingAnimation(String baseText, float seconds, Runnable onFinish) {
        rolling = true;
        rollLabel.clearActions();

        final float step = 0.08f;
        int repeats = Math.max(1, (int)(seconds / step));

        rollLabel.addAction(Actions.sequence(
                Actions.run(() -> setAllModeButtonsDisabled(true)),
                Actions.repeat(repeats, Actions.sequence(
                        Actions.run(() -> rollLabel.setText(baseText + dots(rng.nextInt(4)))),
                        Actions.delay(step)
                )),
                Actions.run(() -> {
                    rolling = false;
                    setAllModeButtonsDisabled(false);
                    refreshButtons();
                    onFinish.run();
                })
        ));
    }

    private static String dots(int n) {
        return switch (n) {
            case 1 -> ".";
            case 2 -> "..";
            case 3 -> "...";
            default -> "";
        };
    }

    private void setAllModeButtonsDisabled(boolean disabled) {
        rrBtn.setDisabled(disabled);
        slotsBtn.setDisabled(disabled);
        bjBtn.setDisabled(disabled);
        backBtn.setDisabled(disabled);
    }

    private void refreshCoins() {
        coinsLabel.setText("Coins: " + getCoins());
    }

    private void refreshButtons() {
        int c = getCoins();
        rrBtn.setDisabled(rolling || c < 10);
        slotsBtn.setDisabled(rolling || c < 10);
        bjBtn.setDisabled(rolling || c < 5);
    }

    private int getCoins() {
        if (player != null) return player.getCoins();
        return game.getProgression().getCoins();
    }

    private boolean spendCoins(int amount) {
        if (player != null) return player.spendCoins(amount);
        return game.getProgression().spendCoins(amount);
    }

    private void addCoins(int amount) {
        if (amount <= 0) return;
        if (player != null) player.addCoins(amount);
        else game.getProgression().addCoins(amount);
    }

    private int parseIntSafe(String s, int fallback) {
        try { return Integer.parseInt(s.trim()); }
        catch (Exception e) { return fallback; }
    }

    // ---------------- BLACKJACK HELPERS ----------------
    // Very simple card model:
    // 2..10 = face value
    // J/Q/K = 10
    // A = 11 (no soft-ace logic for now; keep it simple)
    private int lastCard = 10;

    private int drawCard() {
        int r = rng.nextInt(13) + 1; // 1..13
        int v;
        if (r == 1) v = 11;          // Ace
        else if (r >= 11) v = 10;    // J/Q/K
        else v = r;                  // 2..10
        lastCard = v;
        return v;
    }

    // (just for “dealer shows” text — not important)
    private int lastDrawnCardValueApprox() {
        return lastCard;
    }

    private void endBlackjackRound(boolean playerWins, boolean push, Label bjStatus, Label bjTotals) {
        bjInRound = false;

        bjTotals.setText("Player: " + bjPlayerTotal + " | Dealer: " + bjDealerTotal);

        if (push) {
            addCoins(bjBet); // return bet
            bjStatus.setText("PUSH! Bet returned.");
        } else if (playerWins) {
            addCoins(bjBet * 2); // win pays 2x (profit = +bet)
            bjStatus.setText("YOU WIN! +" + (bjBet * 2) + " coins paid.");
        } else {
            bjStatus.setText("YOU LOSE! Bet lost.");
        }

        refreshCoins();
        refreshButtons();
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.goToMenu();
            return;
        }

        ScreenUtils.clear(0, 0, 0, 1);

        refreshCoins();
        refreshButtons();

        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { stage.dispose(); }
}
