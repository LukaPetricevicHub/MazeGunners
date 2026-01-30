package de.tum.cit.fop.maze.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
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
import de.tum.cit.fop.maze.progress.Progression;

public class GamblingScreen implements Screen {

    private enum Mode { MENU, SHOP, SLOTS, RUSSIAN_ROULETTE, BLACKJACK }


    private final MazeRunnerGame game;
    private final Progression prog;
    private final Player player; // can be null if opened from Menu

    private final Stage stage;

    // UI
    private Table root;
    private Label titleLabel;
    private Label statusLabel;
    private Label coinsLabel;

    private Mode mode = Mode.MENU;

    // Slots constants
    private static final int SLOT_SPIN_COST = 50;
    private static final int STAT_BUY_COST = 20;
    private static final int STAT_MIN_TO_PLAY = 5;
    private static final int STAT_MAX = 10;

    // Roulette constants
    private static final int ROULETTE_HEAL_COST = 200;
    private static final int ROULETTE_PERMANENT_HEART_CHANCE = 6; // 1 in 6

    // Blackjack
    private BlackjackGame blackjack;

    public GamblingScreen(MazeRunnerGame game) {
        this(game, null);
    }

    public GamblingScreen(MazeRunnerGame game, Player player) {
        this.game = game;
        this.prog = game.getProgression();
        this.player = player;
        this.stage = new Stage(new ScreenViewport(), game.getSpriteBatch());
        buildUI();
    }

    // =========================================================
    // UI BUILD
    // =========================================================

    private void buildUI() {
        root = new Table();
        root.setFillParent(true);
        root.pad(24);
        stage.addActor(root);

        titleLabel = new Label("GAMBLING", game.getSkin(), "title");
        titleLabel.setAlignment(Align.center);

        coinsLabel = new Label("", game.getSkin());
        coinsLabel.setAlignment(Align.center);

        statusLabel = new Label("", game.getSkin());
        statusLabel.setAlignment(Align.center);
        statusLabel.setWrap(true);

        root.add(titleLabel).expandX().fillX().row();
        root.add(coinsLabel).padTop(8).expandX().fillX().row();
        root.add(statusLabel).padTop(14).width(760).row();

        root.row();
        root.add(new Label("", game.getSkin())).padTop(10).row();

        showMenu();
        refreshTopBar();
    }

    private void refreshTopBar() {
        coinsLabel.setText("Coins: " + prog.getCoins());

        String info = "";
        info += "Perm Speed: " + prog.getPermanentSpeed() + "   ";
        info += "Perm Strength: " + prog.getPermanentStrength() + "   ";
        info += "Perm Hearts: " + prog.getPermanentHearts() + "   ";
        info += (prog.hasFreezeWeapon() ? " | Freeze Weapon ✓" : " | Freeze Weapon ✗");
        info += (prog.hasPoisonKnuckles() ? " | Poison Knuckles ✓" : " | Poison Knuckles ✗");

        // Only show current run lives IF player exists
        if (player != null) {
            info += "\nCurrent Run Lives: " + player.getLives();
        }

        statusLabel.setText(info);
    }


    private void clearBelowTop() {
        // Keep first 3 rows (title + coins + status). Remove everything else.
        // Easiest: rebuild full content below by clearing & re-adding top area.
        root.clearChildren();

        root.add(titleLabel).expandX().fillX().row();
        root.add(coinsLabel).padTop(8).expandX().fillX().row();
        root.add(statusLabel).padTop(14).width(760).row();
        root.row();
        root.add(new Label("", game.getSkin())).padTop(10).row();
    }

    // =========================================================
    // MAIN MENU
    // =========================================================

    private void showMenu() {
        mode = Mode.MENU;
        clearBelowTop();

        Table menu = new Table();
        menu.defaults().width(360).height(54).pad(6);
        TextButton shopBtn = new TextButton("Shop", game.getSkin());
        TextButton slotsBtn = new TextButton("Slots", game.getSkin());
        TextButton rrBtn = new TextButton("Russian Roulette", game.getSkin());
        TextButton bjBtn = new TextButton("Blackjack", game.getSkin());
        TextButton backBtn = new TextButton("Back", game.getSkin());

        shopBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                showShop();
            }
        });


        slotsBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                showSlots();
            }
        });

        rrBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                showRussianRoulette();
            }
        });

        bjBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                showBlackjack();
            }
        });

        backBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                game.goToMenu();
            }
        });

        menu.add(shopBtn).row();
        menu.add(slotsBtn).row();
        menu.add(rrBtn).row();
        menu.add(bjBtn).row();
        menu.add(backBtn).padTop(18).row();

        root.add(menu).expand().center().row();

        setStatusHint(
                "Choose a mode.\n\n" +
                        "- Slots: win/lose permanent Speed/Strength (+/-1..5), unlock hidden items.\n" +
                        "- Roulette: 1/6 chance for a permanent heart.\n" +
                        "- Blackjack: bet coins, win = 2× bet.\n"
        );
        refreshTopBar();
    }

    private void showShop() {
        mode = Mode.SHOP;
        clearBelowTop();

        Table t = new Table();
        t.defaults().pad(6);

        Label header = new Label("SHOP", game.getSkin(), "title");
        header.setAlignment(Align.center);

        // --- Determine insufficiencies ---
        int ps = prog.getPermanentSpeed();
        int st = prog.getPermanentStrength();

        boolean needSpeed = ps < STAT_MIN_TO_PLAY;
        boolean needStrength = st < STAT_MIN_TO_PLAY;

        // Casino hearts = the only heart value that always exists here
        int casinoHearts = prog.getCasinoHearts();
        boolean needHearts = casinoHearts <= 1;

        StringBuilder sb = new StringBuilder();

        // Only show the warning text if something is insufficient
        if (needSpeed || needStrength || needHearts) {
            sb.append("⚠ You are insufficient in:\n");
            if (needSpeed) sb.append("- Speed (").append(ps).append(" < ").append(STAT_MIN_TO_PLAY).append(")\n");
            if (needStrength) sb.append("- Strength (").append(st).append(" < ").append(STAT_MIN_TO_PLAY).append(")\n");
            if (needHearts) sb.append("- Casino Hearts (").append(casinoHearts).append(")\n");

            sb.append("\nBuy what you need below.");
        } else {
            sb.append("You're good to go.\nLET'S GO GAMBLING!");
        }

        Label info = new Label(sb.toString(), game.getSkin());
        info.setAlignment(Align.center);
        info.setWrap(true);

        // --- Shop buttons ---
        TextButton buySpeedBtn = new TextButton("Buy +1 Speed (" + STAT_BUY_COST + ")", game.getSkin());
        TextButton buyStrBtn   = new TextButton("Buy +1 Strength (" + STAT_BUY_COST + ")", game.getSkin());

        // If you want hearts purchasable in Shop too, reuse roulette cost:
        TextButton buyHeartBtn = new TextButton("Buy +1 Casino Heart (" + ROULETTE_HEAL_COST + ")", game.getSkin());

        TextButton backBtn = new TextButton("Back", game.getSkin());

        buySpeedBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                String msg = buyStat("speed");
                // Refresh the shop so message updates
                showShop();
            }
        });

        buyStrBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                String msg = buyStat("strength");
                showShop();
            }
        });

        buyHeartBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                // Let shop buy a casino heart only when at 1 (same rule as roulette),
                // or change this condition if you want a normal shop purchase.
                buyRouletteLife();
                showShop();
            }
        });

        backBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                showMenu();
            }
        });

        // --- Layout ---
        t.add(header).colspan(2).padBottom(10).row();
        t.add(info).colspan(2).width(760).row();

        t.add(buySpeedBtn).width(320).height(54).padTop(10);
        t.add(buyStrBtn).width(320).height(54).padTop(10).row();

        t.add(buyHeartBtn).colspan(2).width(660).height(54).padTop(8).row();

        t.add(backBtn).colspan(2).width(300).height(54).padTop(16).row();

        root.add(t).expand().center();

        // Button state rules
        // Speed/Strength: allow buying if below min-to-play and < max and has coins
        buySpeedBtn.setDisabled(!(prog.getCoins() >= STAT_BUY_COST && ps < STAT_MIN_TO_PLAY && ps < STAT_MAX));
        buyStrBtn.setDisabled(!(prog.getCoins() >= STAT_BUY_COST && st < STAT_MIN_TO_PLAY && st < STAT_MAX));

        // Heart buying in shop: same as roulette’s rule (only at exactly 1)
        buyHeartBtn.setDisabled(!(casinoHearts == 1 && prog.getCoins() >= ROULETTE_HEAL_COST));

        refreshTopBar();
    }


    private void setStatusHint(String text) {
        // Show this as the main hint line under the top info
        // We reuse statusLabel for both info and hint, so we append nicely.
        String top = coinsLabel.getText().toString();
        // Actually we keep statusLabel = detailed info; hints go below in a separate label.
        // To keep it simple, add a new label row below top each time:
        // But we already have statusLabel used for info.
        // We'll just set statusLabel with combined info+hint.
        // (Good enough for now)
        String info = "";
        info += "Coins: " + prog.getCoins() + "\n";
        info += "Perm Speed: " + prog.getPermanentSpeed() + " | ";
        info += "Perm Strength: " + prog.getPermanentStrength() + " | ";
        info += "Perm Hearts: " + prog.getPermanentHearts() + "\n";
        info += (prog.hasFreezeWeapon() ? "Freeze Weapon ✓" : "Freeze Weapon ✗");
        info += " | ";
        info += (prog.hasPoisonKnuckles() ? "Poison Knuckles ✓" : "Poison Knuckles ✗");
        if (player != null) info += "\nCurrent run lives: " + player.getLives();

        statusLabel.setText(info + "\n\n" + text);
    }

    // =========================================================
    // SLOTS
    // =========================================================

    private void showSlots() {
        mode = Mode.SLOTS;
        clearBelowTop();

        Table t = new Table();
        t.defaults().pad(6);

        Label header = new Label("SLOTS", game.getSkin(), "title");
        header.setAlignment(Align.center);

        Label rules = new Label(
                "Cost per spin: " + SLOT_SPIN_COST + " coins\n" +
                        "You can win/lose permanent Speed/Strength (±1..5)\n" +
                        "Random chance to unlock hidden items:\n" +
                        "- Freeze Weapon (club/knife) → freeze enemies\n" +
                        "- Poison Knuckles → damage over time\n\n" +
                        "To play: Speed AND Strength must be at least " + STAT_MIN_TO_PLAY + ".",
                game.getSkin()
        );
        rules.setAlignment(Align.center);
        rules.setWrap(true);

        Label result = new Label("", game.getSkin());
        result.setAlignment(Align.center);
        result.setWrap(true);

        TextButton spinBtn = new TextButton("SPIN", game.getSkin());
        TextButton backBtn = new TextButton("Back", game.getSkin());

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
        t.add(rules).colspan(2).width(760).row();

        t.add(spinBtn).width(300).height(54).padTop(10);
        t.add(backBtn).width(300).height(54).padTop(10).row();


        t.add(result).colspan(2).width(760).padTop(18).row();

        root.add(t).expand().center();


        setStatusHint("Slots ready. If your stats are below 5, buy them first.");
        refreshTopBar();
    }

    private void updateSlotsButtonStates(TextButton spinBtn) {
        int ps = prog.getPermanentSpeed();
        int st = prog.getPermanentStrength();

        boolean canPlay = (ps >= STAT_MIN_TO_PLAY && st >= STAT_MIN_TO_PLAY);
        boolean hasCoinsForSpin = prog.getCoins() >= SLOT_SPIN_COST;

        spinBtn.setDisabled(!(canPlay && hasCoinsForSpin));
    }


    private String buyStat(String which) {
        int coins = prog.getCoins();
        if (coins < STAT_BUY_COST) return "Not enough coins.";

        if (which.equals("speed")) {
            int v = prog.getPermanentSpeed();
            if (v >= STAT_MAX) return "Speed is already at max (" + STAT_MAX + ").";
            if (v >= STAT_MIN_TO_PLAY) return "You can only buy Speed here if it is below " + STAT_MIN_TO_PLAY + ".";
            if (!prog.spendCoins(STAT_BUY_COST)) return "Not enough coins.";
            prog.addPermanentSpeed(1);
            return "Bought +1 Speed. New Speed = " + prog.getPermanentSpeed();
        }

        if (which.equals("strength")) {
            int v = prog.getPermanentStrength();
            if (v >= STAT_MAX) return "Strength is already at max (" + STAT_MAX + ").";
            if (v >= STAT_MIN_TO_PLAY) return "You can only buy Strength here if it is below " + STAT_MIN_TO_PLAY + ".";
            if (!prog.spendCoins(STAT_BUY_COST)) return "Not enough coins.";
            prog.addPermanentStrength(1);
            return "Bought +1 Strength. New Strength = " + prog.getPermanentStrength();
        }

        return "Unknown stat.";
    }

    private String doSlotSpin() {
        int ps = prog.getPermanentSpeed();
        int st = prog.getPermanentStrength();

        if (ps < STAT_MIN_TO_PLAY || st < STAT_MIN_TO_PLAY) {
            return "You cannot play Slots until Speed AND Strength are at least " + STAT_MIN_TO_PLAY +
                    ". Buy stats for " + STAT_BUY_COST + " coins each.";
        }

        if (!prog.spendCoins(SLOT_SPIN_COST)) {
            return "Not enough coins to spin.";
        }

        // Outcomes:
        // 0-54: stat change (win/lose)
        // 55-69: unlock freeze weapon (if not already)
        // 70-84: unlock poison knuckles (if not already)
        // 85-99: nothing
        int roll = MathUtils.random(0, 99);

        if (roll <= 54) {
            boolean win = MathUtils.randomBoolean(0.55f); // slightly favorable
            int amt = MathUtils.random(1, 5);

            // pick which stat
            boolean affectSpeed = MathUtils.randomBoolean();
            String statName = affectSpeed ? "Speed" : "Strength";

            if (win) {
                if (affectSpeed) prog.addPermanentSpeed(+amt);
                else prog.addPermanentStrength(+amt);

                return "🎰 WIN! +" + amt + " " + statName +
                        "\nSpeed=" + prog.getPermanentSpeed() +
                        " Strength=" + prog.getPermanentStrength();
            } else {
                if (affectSpeed) prog.addPermanentSpeed(-amt);
                else prog.addPermanentStrength(-amt);

                return "💀 LOSS! -" + amt + " " + statName +
                        "\nSpeed=" + prog.getPermanentSpeed() +
                        " Strength=" + prog.getPermanentStrength();
            }
        }

        if (roll <= 69) {
            if (!prog.hasFreezeWeapon()) {
                prog.unlockFreezeWeapon();
                return "🎁 Hidden reward unlocked: FREEZE WEAPON (club/knife)!\n(Freeze enemies on hit)";
            }
            return "You hit the Freeze Weapon slot… but you already own it.";
        }

        if (roll <= 84) {
            if (!prog.hasPoisonKnuckles()) {
                prog.unlockPoisonKnuckles();
                return "🎁 Hidden reward unlocked: POISON KNUCKLES!\n(Poison enemies for damage over time)";
            }
            return "You hit the Poison Knuckles slot… but you already own it.";
        }

        return "Nothing happened. The house always wins… sometimes.";
    }

    // =========================================================
    // RUSSIAN ROULETTE
    // =========================================================

    private void showRussianRoulette() {
        mode = Mode.RUSSIAN_ROULETTE;
        clearBelowTop();

        Table t = new Table();
        t.defaults().pad(6);

        Label header = new Label("RUSSIAN ROULETTE", game.getSkin(), "title");
        header.setAlignment(Align.center);

        Label rules = new Label(
                "1 in 6 chance to gain a PERMANENT HEART.\n" +
                        "Uses Casino Hearts (outside runs).\n" +
                        "If you have 1 Casino Heart left, you cannot play.\n" +
                        "You may pay 200 coins to buy +1 Casino Heart ONLY when you have exactly 1.",
                game.getSkin()
        );

        rules.setAlignment(Align.center);
        rules.setWrap(true);

        Label result = new Label("", game.getSkin());
        result.setAlignment(Align.center);
        result.setWrap(true);

        TextButton playBtn = new TextButton("PULL TRIGGER", game.getSkin());
        TextButton backBtn = new TextButton("Back", game.getSkin());

        playBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                String msg = doRoulette();
                result.setText(msg);
                refreshTopBar();
                updateRouletteButtons(playBtn);
            }
        });


        backBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                showMenu();
            }
        });

        t.add(header).colspan(2).padBottom(10).row();
        t.add(rules).colspan(2).width(760).row();

        t.add(playBtn).width(300).height(54).padTop(10);
        t.add(backBtn).width(300).height(54).padTop(10).row();

        t.add(result).colspan(2).width(760).padTop(18).row();

        root.add(t).expand().center();

        updateRouletteButtons(playBtn);
        setStatusHint("Roulette ready. (This mode is meaningful only when opened in-game with a Player.)");
        refreshTopBar();
    }

    private void updateRouletteButtons(TextButton playBtn) {
        int hearts = prog.getCasinoHearts();
        playBtn.setDisabled(hearts <= 1);
    }


    private String buyRouletteLife() {
        int hearts = prog.getCasinoHearts();
        if (hearts != 1) return "You can only buy a heart when you have exactly 1 heart left.";
        if (!prog.spendCoins(ROULETTE_HEAL_COST)) return "Not enough coins.";

        prog.setCasinoHearts(2); // buying back one heart from 1 -> 2
        return "Bought +1 heart. Casino Hearts = " + prog.getCasinoHearts();
    }

    private String doRoulette() {
        int hearts = prog.getCasinoHearts();

        if (hearts <= 1) {
            return "You cannot play Roulette with 1 heart left. Pay 200 coins to buy +1 heart.";
        }

        // 1 in 6 win: permanent heart
        int roll = MathUtils.random(1, ROULETTE_PERMANENT_HEART_CHANCE);
        if (roll == 1) {
            prog.addPermanentHeart();
            return "💥 CLICK… YOU WIN!\nPermanent Heart gained! Total perm hearts: " + prog.getPermanentHearts();
        }

        // lose 1 casino heart
        prog.setCasinoHearts(hearts - 1);
        return "💥 BANG!\nYou lost 1 casino heart. Casino Hearts = " + prog.getCasinoHearts();
    }


    // =========================================================
    // BLACKJACK
    // =========================================================

    private void showBlackjack() {
        mode = Mode.BLACKJACK;
        clearBelowTop();

        blackjack = new BlackjackGame();

        Table t = new Table();
        t.defaults().pad(6);

        Label header = new Label("BLACKJACK", game.getSkin(), "title");
        header.setAlignment(Align.center);

        Label rules = new Label(
                "Minimum bet: 10 coins\n" +
                        "Win pays 2× bet",
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

        TextButton placeBetBtn = new TextButton("Place Bet / Deal", game.getSkin());
        TextButton hitBtn = new TextButton("Hit", game.getSkin());
        TextButton standBtn = new TextButton("Stand", game.getSkin());
        TextButton backBtn = new TextButton("Back", game.getSkin());

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
                            state.setText("Dealer reveals...\n\n" + blackjack.renderState(true));
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

        root.add(t).expand().center();

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
        if (!prog.spendCoins(bet)) return "Not enough coins for that bet.";

        blackjack.startHand(bet);

        // Immediate resolution if blackjack/bust
        return maybeResolveBlackjackAfterPlayerAction();
    }

    private String maybeResolveBlackjackAfterPlayerAction() {
        if (!blackjack.inHand) return "No active hand.";

        if (blackjack.playerValue() > 21) {
            // Player busts -> loses bet (already spent)
            blackjack.inHand = false;
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

            // Gain a permanent heart on win
            prog.addPermanentHeart();

            // If we are in-game, refresh player max HP immediately
            if (player != null) {
                player.refreshPermanentProgression(game);
                player.setHp(player.getMaxHp()); // optional: fill to new max
            }

            boolean bigWin = blackjack.bet > 1000;
            if (bigWin && game.getAchievementManager() != null) {
                game.getAchievementManager().unlock("win_it_big");
            }

            return "✅ You WIN! Dealer=" + dv + " You=" + pv +
                    "\nPayout: " + payout + " coins" +
                    "\n❤️ Permanent Heart gained! Total perm hearts: " + prog.getPermanentHearts() +
                    (bigWin ? "\n🏆 Achievement unlocked: win_it_big" : "");
        }

        if (pv == dv) {
            prog.addCoins(blackjack.bet);
            return "➖ PUSH. Dealer=" + dv + " You=" + pv + "\nRefunded: " + blackjack.bet + " coins";
        }

        return "❌ You LOSE. Dealer=" + dv + " You=" + pv + "\nLost bet: " + blackjack.bet + " coins";
    }


    // =========================================================
    // BLACKJACK GAME LOGIC (simple)
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


    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { stage.dispose(); }
}
