package de.tum.cit.fop.maze.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.player.Player;
import de.tum.cit.fop.maze.gambling.Progression;
import de.tum.cit.fop.maze.gambling.RunGambleState;

public class GamblingScreen implements Screen {

    private enum Mode { MENU, SLOTS, BLACKJACK }


    private final MazeRunnerGame game;
    private final Progression prog;
    private final RunGambleState run;
    private final Player player; // can be null if opened from Menu

    private final Stage stage;

    // UI
    private Stack root;
    private Table contentRoot;
    private Table dialogueTable;
    private Label titleLabel;
    private Label statusLabel;
    private Label coinsLabel;
    private Label dealerLabel;
    private TextButton dialogueNextBtn;
    private Image dealerImage;
    private Texture dealerTexture;
    private Sound cardsSound;
    private String[] dialogueLines = null;
    private int dialogueIndex = 0;
    private Runnable dialogueOnDone = null;
    private boolean dialogueActive = false;
    private boolean dialogueShowContentOnEnd = true;

    private Mode mode = Mode.MENU;

    // Slots constants
    private static final int SLOT_SPIN_COST = 100;
    private static final int STAT_BUY_COST = 20;
    private static final int STAT_MIN_TO_PLAY = 5;
    private static final int STAT_MAX = 99;

    // Blackjack
    private BlackjackGame blackjack;

    public GamblingScreen(MazeRunnerGame game) {
        this(game, null);
    }

    public GamblingScreen(MazeRunnerGame game, Player player) {
        this.game = game;
        this.prog = game.getProgression();
        this.run = game.getRunGamble();
        this.player = player;
        this.stage = new Stage(new ScreenViewport(), game.getSpriteBatch());
        buildUI();
    }

    // =========================================================
    // UI BUILD
    // =========================================================

    private void buildUI() {
        root = new Stack();
        root.setFillParent(true);
        stage.addActor(root);

        dealerTexture = new Texture("dealer.png");
        dealerImage = new Image(dealerTexture);
        dealerImage.setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        Table bg = new Table();
        bg.setFillParent(true);
        bg.add(dealerImage).grow();
        root.add(bg);

        Table uiLayer = new Table();
        uiLayer.setFillParent(true);
        root.add(uiLayer);

        contentRoot = new Table();
        contentRoot.top().pad(24);

        dialogueTable = new Table();
        dialogueTable.bottom().pad(16);

        titleLabel = new Label("THE CASINO", game.getSkin(), "title");
        titleLabel.setAlignment(Align.center);

        coinsLabel = new Label("", game.getSkin());
        coinsLabel.setAlignment(Align.center);

        statusLabel = new Label("", game.getSkin());
        statusLabel.setAlignment(Align.center);
        statusLabel.setWrap(true);

        contentRoot.add(titleLabel).expandX().fillX().row();
        contentRoot.add(coinsLabel).padTop(8).expandX().fillX().row();
        contentRoot.add(statusLabel).padTop(14).width(700).row();

        contentRoot.row();
        contentRoot.add(new Label("", game.getSkin())).padTop(10).row();

        dealerLabel = new Label("", game.getSkin());
        dealerLabel.setWrap(true);
        dealerLabel.setAlignment(Align.center);

        dialogueNextBtn = UiButtons.button("Next", game.getSkin());
        dialogueNextBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                advanceDialogue();
            }
        });

        dialogueTable.add(dealerLabel).width(900).pad(10).row();
        dialogueTable.add(dialogueNextBtn).width(160).height(50).padBottom(8).row();

        dialogueTable.row();

        uiLayer.add(contentRoot).expand().fill().row();
        uiLayer.add(dialogueTable).expandX().fillX().bottom().pad(8);

        showMenu();
        refreshTopBar();
    }

    private void refreshTopBar() {
        coinsLabel.setText("Coins: " + prog.getCoins());

        String info = "";
        int baseSpeed = 5;
        int baseStrength = 5;
        int speedBonus = Math.max(0, run.speed - baseSpeed);
        int strengthBonus = Math.max(0, run.strength - baseStrength);

        info += "\nCaution: gambling stats apply only to the current run.";
        statusLabel.setText(info);
    }


    private void clearBelowTop() {
        // Keep first 3 rows (title + coins + status). Remove everything else.
        // Easiest: rebuild full content below by clearing & re-adding top area.
        contentRoot.clearChildren();

        contentRoot.add(titleLabel).expandX().fillX().row();
        contentRoot.add(coinsLabel).padTop(8).expandX().fillX().row();
        contentRoot.add(statusLabel).padTop(14).width(700).row();
        contentRoot.row();
        contentRoot.add(new Label("", game.getSkin())).padTop(10).row();
    }

    // =========================================================
    // MAIN MENU
    // =========================================================

    private void showMenu() {
        mode = Mode.MENU;
        clearBelowTop();

        Table menu = new Table();
        menu.defaults().width(240).height(70).pad(8);
        TextButton slotsBtn = UiButtons.button("Slots", game.getSkin());
        TextButton bjBtn = UiButtons.button("Blackjack", game.getSkin());
        TextButton backBtn = UiButtons.button("Return", game.getSkin());


        slotsBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                startDialogue(
                        new String[] {
                                "Slots cost coins per spin.",
                                "You can win run Speed or Strength.",
                                "You might unlock rare items."
                        },
                        GamblingScreen.this::showSlots
                );
            }
        });

        bjBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                startDialogue(
                        new String[] {
                                "Blackjack is a straight bet.",
                                "Beat the dealer and win double your bet.",
                                "A win also grants a run heart."
                        },
                        GamblingScreen.this::showBlackjack
                );
            }
        });

        backBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                game.goToMenu();
            }
        });

        menu.add(slotsBtn);
        menu.add(bjBtn);
        menu.add(backBtn);

        contentRoot.add(menu).expandY().bottom().padBottom(16).row();

        setStatusHint(
                "Choose a mode.\n\n" +
                        "- Slots: win/lose run Speed/Strength, unlock hidden items.\n" +
                        "- Blackjack: bet coins, win = 2x bet.\n"
        );
        refreshTopBar();
        startDialogue(
                new String[] {
                        "Welcome to the casino.",
                        "Don't mind who or what I am and where we are.",
                        "Just pick a game and I will explain the rules."
                },
                () -> contentRoot.setVisible(true)
        );
    }


    private void setStatusHint(String text) {
        String info = "";
        info += "Coins: " + prog.getCoins() + "\n";
        int baseSpeed = 5;
        int baseStrength = 5;
        int speedBonus = Math.max(0, run.speed - baseSpeed);
        int strengthBonus = Math.max(0, run.strength - baseStrength);
        info += "Run Bonuses: Speed +" + speedBonus + " | ";
        info += "Strength +" + strengthBonus + " | ";
        info += "HP Bonus +" + run.bonusHearts + "\n";
        info += (run.freezeWeapon ? "Freeze Gloves: YES" : "Freeze Gloves: NO");
        info += " | ";
        info += (run.poisonKnuckles ? "Poison Knuckles: YES" : "Poison Knuckles: NO");
        if (player != null) info += "\nCurrent run lives: " + player.getLives();

        statusLabel.setText(info + "\nCaution: gambling stats apply only to the current run.\n\n" + text);
    }

    private void setDealerLine(String text) {
        dealerLabel.setText(text);
        if (!dialogueActive) {
            dialogueNextBtn.setVisible(false);
            dialogueTable.setVisible(true);
        }
    }

    private void startDialogue(String[] lines, Runnable onDone) {
        dialogueLines = lines;
        dialogueIndex = 0;
        dialogueOnDone = onDone;
        dialogueActive = true;
        dialogueShowContentOnEnd = true;

        contentRoot.setVisible(false);
        dialogueTable.setVisible(true);

        dealerLabel.setText(lines.length > 0 ? lines[0] : "");
        dialogueNextBtn.setText("Next");
        dialogueNextBtn.setVisible(lines.length > 0);
    }

    private void advanceDialogue() {
        if (!dialogueActive || dialogueLines == null) return;

        dialogueIndex++;
        if (dialogueIndex < dialogueLines.length) {
            dealerLabel.setText(dialogueLines[dialogueIndex]);
            dialogueNextBtn.setText("Next");
            return;
        }

        dialogueActive = false;
        dialogueNextBtn.setVisible(false);

        if (dialogueOnDone != null) {
            Runnable r = dialogueOnDone;
            dialogueOnDone = null;
            r.run();
        }
        if (dialogueShowContentOnEnd) {
            contentRoot.setVisible(true);
        }
    }

    // =========================================================
    // SLOTS
    // =========================================================

    private void showSlots() {
        mode = Mode.SLOTS;
        contentRoot.setVisible(true);
        clearBelowTop();
        setDealerLine("Slots are quick. Big swings, bigger stories.");

        Table t = new Table();
        t.defaults().pad(6);

        Label header = new Label("SLOTS", game.getSkin(), "title");
        header.setAlignment(Align.center);

        Label rules = new Label(
                "Cost per spin: " + SLOT_SPIN_COST + " coins\n" +
                        "You can win run Speed/Strength\n" +
                        "Random chance to unlock hidden items:\n" +
                        "- Freeze Gloves \n" +
                        "- Poison Knuckles \n",
                game.getSkin()
        );
        rules.setAlignment(Align.center);
        rules.setWrap(true);

        Label result = new Label("", game.getSkin());
        result.setAlignment(Align.center);
        result.setWrap(true);

        TextButton spinBtn = UiButtons.button("SPIN", game.getSkin());
        TextButton backBtn = UiButtons.button("Back", game.getSkin());

        spinBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                String msg = doSlotSpin();
                result.setText(msg);
                refreshTopBar();
                updateSlotsButtonStates(spinBtn);
            }
        });
        backBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                showMenu();
            }
        });

        t.add(header).colspan(2).padBottom(10).row();
        t.add(rules).colspan(2).width(700).row();

        t.add(spinBtn).width(300).height(54).padTop(10);
        t.add(backBtn).width(300).height(54).padTop(10).row();

        t.add(result).colspan(2).width(700).padTop(18).row();

        contentRoot.add(t).expand().center();
        refreshTopBar();
    }

    private void updateSlotsButtonStates(TextButton spinBtn) {
        int ps = run.speed;
        int st = run.strength;

        boolean canPlay = (ps >= STAT_MIN_TO_PLAY && st >= STAT_MIN_TO_PLAY);
        boolean hasCoinsForSpin = prog.getCoins() >= SLOT_SPIN_COST;

        spinBtn.setDisabled(!(canPlay && hasCoinsForSpin));
    }

    private String doSlotSpin() {
        int ps = run.speed;
        int st = run.strength;

        if (!prog.spendCoins(SLOT_SPIN_COST)) {
            setDealerLine("No coins, no spin. That's the rule.");
            return "Not enough coins to spin.";
        }

        // Outcomes:
        // 0-59: nothing
        // 60-89: stat buff
        // 90-94: unlock freeze gloves (run-only)
        // 95-99: unlock poison knuckles (run-only)
        int roll = MathUtils.random(0, 99);

        if (roll <= 59) {
            setDealerLine("Look's like you got nothing this time...");
            return "Nothing happened. Better luck next time!";
        }

        if (roll <= 89) {
            int amt = MathUtils.random(1, 3);
            boolean affectSpeed = MathUtils.randomBoolean();
            String statName = affectSpeed ? "Speed" : "Strength";

            if (affectSpeed) run.speed = Math.min(STAT_MAX, run.speed + amt);
            else run.strength = Math.min(STAT_MAX, run.strength + amt);

            setDealerLine("That's a win. Good for you.");
            return "BUFF! +" + amt + " " + statName +
                    "\nSpeed=" + run.speed +
                    " Strength=" + run.strength;
        }

        if (roll <= 94) {
            if (!run.freezeWeapon) {
                run.freezeWeapon = true;
                run.speed = Math.min(STAT_MAX, run.speed + 10);
                run.strength = Math.min(STAT_MAX, run.strength + 10);
                if (player != null) {
                    player.applyRunGamble(run);
                }
                setDealerLine("Oh nice you got the freeze gloves. I use those to keep my food fresh sometimes...");
                return "Hidden reward unlocked: FREEZE GLOVES!\n" +
                        "Bonus: +10 Speed, +10 Strength";
            }
            setDealerLine("Oh nice you got the freeze gloves. But you got those on already...");
            return "You hit the Freeze Gloves slot... but you already own it.";
        }

        if (!run.poisonKnuckles) {
            run.poisonKnuckles = true;
            run.speed = Math.min(STAT_MAX, run.speed + 10);
            run.strength = Math.min(STAT_MAX, run.strength + 10);
            if (player != null) {
                player.applyRunGamble(run);
            }
            setDealerLine("Poison knuckles, I don't mind losing those, they're hard to wear...");
            return "Hidden reward unlocked: POISON KNUCKLES!\n" +
                    "Bonus: +10 Speed, +10 Strength";
        }

        setDealerLine("Poison knuckles, you already got them on...");
        return "You hit the Poison Knuckles slot... but looks like you've already got it.";
    }

    // =========================================================
    // BLACKJACK
    // =========================================================

    private void showBlackjack() {
        mode = Mode.BLACKJACK;
        contentRoot.setVisible(true);
        clearBelowTop();
        setDealerLine("Blackjack is skill and nerve. Place your bet.");

        blackjack = new BlackjackGame();

        Table t = new Table();
        t.defaults().pad(6);

        Label header = new Label("BLACKJACK", game.getSkin(), "title");
        header.setAlignment(Align.center);

        Label rules = new Label(
                "Minimum bet: 10 coins\n" +
                        "Win pays 2x bet",
                game.getSkin()
        );

        rules.setAlignment(Align.center);
        rules.setWrap(true);

        Label state = new Label("", game.getSkin());
        state.setAlignment(Align.center);
        state.setWrap(true);

        TextField betField = new TextField("10", game.getSkin());
        betField.setMessageText("Bet amount");
        betField.setTextFieldFilter(new TextField.TextFieldFilter.DigitsOnlyFilter());

        TextButton placeBetBtn = UiButtons.button("Place Bet / Deal", game.getSkin());
        TextButton hitBtn = UiButtons.button("Hit", game.getSkin());
        TextButton standBtn = UiButtons.button("Stand", game.getSkin());
        TextButton backBtn = UiButtons.button("Back", game.getSkin());

        hitBtn.setDisabled(true);
        standBtn.setDisabled(true);

        placeBetBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                int bet = parseBet(betField.getText());
                String msg = startBlackjackHand(bet);
                state.setText(msg + "\n\n" + blackjack.renderState());
                refreshTopBar();

                hitBtn.setDisabled(!blackjack.inHand);
                standBtn.setDisabled(!blackjack.inHand);
            }
        });

        hitBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                if (!blackjack.inHand) return;

                setBlackjackButtonsEnabled(placeBetBtn, hitBtn, standBtn, false);

                // Dramatic pause: draw -> update -> maybe resolve
                stage.addAction(Actions.sequence(
                        Actions.delay(0.35f),
                        Actions.run(() -> {
                            blackjack.playerHit();
                            playCardsSound();
                            String msg = maybeResolveBlackjackAfterPlayerAction();
                            state.setText(msg + "\n\n" + blackjack.renderState(false));
                            refreshTopBar();
                        }),
                        Actions.delay(0.35f),
                        Actions.run(() -> {
                            if (!blackjack.inHand) {
                                // hand resolved (bust or 21 auto-stand)
                                setBlackjackButtonsEnabled(placeBetBtn, hitBtn, standBtn, true);
                                hitBtn.setDisabled(true);
                                standBtn.setDisabled(true);
                            } else {
                                setBlackjackButtonsEnabled(placeBetBtn, hitBtn, standBtn, true);
                            }
                        })
                ));
            }
        });


        standBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                if (!blackjack.inHand) return;

                setBlackjackButtonsEnabled(placeBetBtn, hitBtn, standBtn, false);

                // Reveal dealer, then dealer draws step-by-step with pauses
                blackjack.revealDealer = true;

                stage.addAction(Actions.sequence(
                        Actions.delay(0.5f),
                        Actions.run(() -> {
                            state.setText("My turn now...\n\n" + blackjack.renderState(true));
                        }),
                        Actions.delay(0.6f),
                        Actions.run(() -> animateDealerPlay(state, placeBetBtn, hitBtn, standBtn))
                ));
            }
        });


        backBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                showMenu();
            }
        });

        t.add(header).colspan(2).padBottom(10).row();
        t.add(rules).colspan(2).width(760).row();

        Table betRow = new Table();
        betRow.defaults().pad(6);
        betRow.add(new Label("Bet:", game.getSkin()));
        betRow.add(betField).width(200);
        betRow.add(placeBetBtn).width(240).height(50);
        t.add(betRow).colspan(2).padTop(10).row();

        Table actionRow = new Table();
        actionRow.defaults().pad(6);
        actionRow.add(hitBtn).width(240).height(50);
        actionRow.add(standBtn).width(240).height(50);
        t.add(actionRow).colspan(2).row();

        t.add(backBtn).colspan(2).width(300).height(54).padTop(10).row();
        t.add(state).colspan(2).width(760).padTop(18).row();

        contentRoot.add(t).expand().center();

        setStatusHint("Blackjack ready. Place a bet and deal.");
        refreshTopBar();
    }

    private int parseBet(String text) {
        try {
            if (text == null || text.isEmpty()) return 0;
            return Integer.parseInt(text);
        } catch (Exception e) {
            return 0;
        }
    }

    private String startBlackjackHand(int bet) {
        if (bet < 10) return "Bet must be at least 10 coins.";
        if (bet > prog.getCoins()) return "Not enough coins for that bet.";

        // Deduct bet up-front
        if (!prog.spendCoins(bet)) return "Looks like you don't have enough coins to play this game.";

        blackjack.startHand(bet);

        // Immediate resolution if blackjack/bust
        return maybeResolveBlackjackAfterPlayerAction();
    }

    private String maybeResolveBlackjackAfterPlayerAction() {
        if (!blackjack.inHand) return "No active hand.";

        if (blackjack.playerValue() > 21) {
            // Player busts -> loses bet (already spent)
            blackjack.inHand = false;
            setDealerLine("Bust. Better luck next hand.");
            return "You BUSTED. You lose your bet (" + blackjack.bet + ").";
        }

        if (blackjack.playerValue() == 21) {
            blackjack.revealDealer = true;
            blackjack.dealerPlay(); // dealer finishes immediately here
            return resolveBlackjackAndMaybeGrantHeart();
        }


        return "Your move: Hit or Stand.";
    }

    private String resolveBlackjackAndMaybeGrantHeart() {
        int pv = blackjack.playerValue();
        int dv = blackjack.dealerValue();

        blackjack.inHand = false;

        if (dv > 21 || pv > dv) {
            int payout = blackjack.bet * 2;
            prog.addCoins(payout);

            // Gain a run heart on win
            run.bonusHearts += 1;

            if (player != null) {
                player.applyRunGamble(run);
            }

            boolean bigWin = blackjack.bet > 1000;
            if (bigWin && game.getAchievementManager() != null) {
                game.getAchievementManager().unlockAchievement("win_it_big");
            }

            setDealerLine("You won. Take what you've earned.");
            String msg = "You WIN! Dealer=" + dv + " You=" + pv +
                    "\nPayout: " + payout + " coins" +
                    "\nHP Bonus gained! Total HP bonus: " + run.bonusHearts +
                    (bigWin ? "\nAchievement unlocked: win_it_big" : "");
            return msg;
        }

        if (pv == dv) {
            prog.addCoins(blackjack.bet);
            setDealerLine("Push. I guess we can call it even.");
            String msg = "PUSH. Dealer=" + dv + " You=" + pv + "\nRefunded: " + blackjack.bet + " coins";
            return msg;
        }

        setDealerLine("You lose, better luck next time.");
        String msg = "You LOSE. Dealer=" + dv + " You=" + pv + "\nLost bet: " + blackjack.bet + " coins";
        return msg;
    }


    // =========================================================
    // BLACKJACK GAME LOGIC (simple) (simple)
    // =========================================================

    private static class BlackjackGame {
        boolean revealDealer = false;
        int bet = 0;
        boolean inHand = false;

        int dealerDraw() {
            int c = drawCard();
            dealer[dealerCount++] = c;
            return c;
        }


        // each card is 1..11 (Ace=11), face cards treated as 10
        int[] player = new int[12];
        int playerCount = 0;

        int[] dealer = new int[12];
        int dealerCount = 0;

        void startHand(int bet) {
            this.bet = bet;
            this.inHand = true;
            this.revealDealer = false;

            playerCount = 0;
            dealerCount = 0;

            // Deal 2 each
            player[playerCount++] = drawCard();
            player[playerCount++] = drawCard();

            dealer[dealerCount++] = drawCard();
            dealer[dealerCount++] = drawCard();
        }

        void playerHit() {
            if (!inHand) return;
            player[playerCount++] = drawCard();
        }

        void playerStand() {
            // no-op here, resolve handled by screen
        }

        void dealerPlay() {
            // Dealer hits until 17 or more
            while (dealerValue() < 17) {
                dealer[dealerCount++] = drawCard();
            }
        }

        int playerValue() {
            return handValue(player, playerCount);
        }

        int dealerValue() {
            return handValue(dealer, dealerCount);
        }

        String renderState() {
            return renderState(revealDealer);
        }

        String renderState(boolean revealDealer) {
            StringBuilder sb = new StringBuilder();
            sb.append("Your hand: ").append(renderHand(player, playerCount))
                    .append(" (").append(playerValue()).append(")\n");

            if (!revealDealer && dealerCount >= 1) {
                sb.append("Dealer hand: ").append(cardName(dealer[0])).append(", ?\n");
            } else {
                sb.append("Dealer hand: ").append(renderHand(dealer, dealerCount))
                        .append(" (").append(dealerValue()).append(")\n");
            }

            sb.append("Bet: ").append(bet);
            return sb.toString();
        }


        private int handValue(int[] cards, int count) {
            int sum = 0;
            int aces = 0;

            for (int i = 0; i < count; i++) {
                int c = cards[i];
                sum += c;
                if (c == 11) aces++;
            }

            // Convert Ace 11 -> 1 if bust
            while (sum > 21 && aces > 0) {
                sum -= 10;
                aces--;
            }

            return sum;
        }

        private String renderHand(int[] cards, int count) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < count; i++) {
                sb.append(cardName(cards[i]));
                if (i < count - 1) sb.append(", ");
            }
            return sb.toString();
        }

        private String cardName(int v) {
            if (v == 11) return "A";
            if (v == 10) return "10";
            return String.valueOf(v);
        }

        private int drawCard() {
            // 1..13 mapped to values: Ace=11, 2..10=2..10, J/Q/K=10
            int r = MathUtils.random(1, 13);
            if (r == 1) return 11;        // Ace
            if (r >= 11) return 10;       // Face
            return r;                     // 2..10
        }
    }

    // =========================================================
    // Screen lifecycle
    // =========================================================

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        if (game.getAudioManager() != null) {
            game.getAudioManager().playBackgroundMusic("casino.mp3");
        }
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            showMenu();
        }

        Gdx.gl.glClearColor(0, 0, 0, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    private void setBlackjackButtonsEnabled(TextButton placeBet, TextButton hit, TextButton stand, boolean enabled) {
        placeBet.setDisabled(!enabled);
        if (enabled) {
            hit.setDisabled(!blackjack.inHand);
            stand.setDisabled(!blackjack.inHand);
        } else {
            hit.setDisabled(true);
            stand.setDisabled(true);
        }
    }
    private void animateDealerPlay(Label state, TextButton placeBetBtn, TextButton hitBtn, TextButton standBtn) {

        if (blackjack.dealerValue() < 17) {
            blackjack.dealerDraw(); // draw exactly ONE card
            playCardsSound();

            state.setText("Dealer draws...\n\n" + blackjack.renderState(true));

            stage.addAction(Actions.sequence(
                    Actions.delay(0.6f),
                    Actions.run(() -> animateDealerPlay(state, placeBetBtn, hitBtn, standBtn))
            ));
            return;
        }

        // Dealer finished -> resolve outcome
        String msg = resolveBlackjackAndMaybeGrantHeart();
        state.setText(msg + "\n\n" + blackjack.renderState(true));
        refreshTopBar();

        setBlackjackButtonsEnabled(placeBetBtn, hitBtn, standBtn, true);
        hitBtn.setDisabled(true);
        standBtn.setDisabled(true);
    }


    @Override public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        if (dealerImage != null) dealerImage.setSize(width, height);
    }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {
        stage.dispose();
        if (dealerTexture != null) dealerTexture.dispose();
        if (cardsSound != null) cardsSound.dispose();
    }

    private void playCardsSound() {
        if (cardsSound == null) {
            cardsSound = Gdx.audio.newSound(Gdx.files.internal("cards.mp3"));
        }
        float vol = game.getPreferences().getSoundVolume();
        cardsSound.play(vol);
    }
}








