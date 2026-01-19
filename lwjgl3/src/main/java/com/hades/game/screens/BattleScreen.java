package com.hades.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.hades.game.HadesGame;
import com.hades.game.constants.GameConfig;
import com.hades.game.constants.SkillData;
import com.hades.game.constants.UnitData;
import com.hades.game.entities.Unit;
import com.hades.game.logic.*;
import com.hades.game.view.GameUI;
import com.hades.game.view.MapRenderer;
import com.hades.game.view.UnitRenderer;
import com.hades.game.view.UI;

// Chess Olympus: HADES vs ZEUS - 메인 전투 화면
public class BattleScreen extends ScreenAdapter {
    private final HadesGame game;
    private ShapeRenderer shape;
    private final Stage stage;
    private CameraManager cameraManager;
    private Array<Unit> units;
    private Vector2 hoveredGrid = new Vector2(-1, -1);
    private Unit selectedUnit = null;
    private TurnManager turnManager;
    private CombatManager combatManager;
    private MapRenderer mapRenderer;
    private UnitRenderer unitRenderer;
    private GameUI gameUI;
    private boolean showHelp = false;

    private Texture battleBg;
    private Texture tileTop;

    private final String playerTeam;
    private final String aiTeam;
    private final String heroName;
    private final UnitData.Stat heroStat;
    private final int stageLevel;

    private float aiDelay = 0;
    private boolean aiBusy = false;
    private boolean gameOver = false;
    private float stageTime = 0; // 추가: 플레이 타임 측정

    private final float MENU_W = 180;
    private final float MENU_H = 60;
    private final Rectangle menuHitbox;

    public BattleScreen(HadesGame game, String playerTeam, String heroName, UnitData.Stat heroStat, int stageLevel) {
        this.game = game;
        this.playerTeam = playerTeam;
        this.heroName = heroName;
        this.heroStat = heroStat;
        this.stageLevel = stageLevel;
        this.aiTeam = playerTeam.equals("HADES") ? "ZEUS" : "HADES";

        this.stage = new Stage(new FitViewport(GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT));
        this.menuHitbox = new Rectangle(
            GameConfig.VIRTUAL_WIDTH - MENU_W - 20,
            GameConfig.VIRTUAL_HEIGHT - MENU_H - 20,
            MENU_W,
            MENU_H
        );

        loadResources();
        init();
        gameUI.addLog("STAGE " + stageLevel + " 전투 시작!", "SYSTEM", playerTeam);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        stage.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
                cameraManager.handleScroll(amountY);
                return true;
            }
        });

        com.hades.game.screens.cutscene.CutsceneData data =
            com.hades.game.screens.cutscene.CutsceneManager.getStageData(stageLevel);

        if (data != null && data.bgmPath() != null) {
            game.audioManager.playBgm(data.bgmPath());
        }
    }

    private void loadResources() {
        battleBg = new Texture(Gdx.files.internal("images/background/battle_background.png"));
        tileTop = new Texture(Gdx.files.internal("images/background/tile_top.png"));
    }

    private void init() {
        shape = new ShapeRenderer();
        mapRenderer = new MapRenderer(shape, game.batch, tileTop);
        unitRenderer = new UnitRenderer(game.batch, shape, game.battleFont, playerTeam);
        gameUI = new GameUI(game);
        cameraManager = new CameraManager((OrthographicCamera) stage.getCamera());

        if (heroStat != null) {
            heroStat.resetSkillStatus();
            heroStat.clearReservedSkill();
        }

        turnManager = new TurnManager();
        turnManager.setBattleScreen(this);
        combatManager = new CombatManager(gameUI, turnManager, playerTeam, this::handleDeath);
        units = StageGenerator.create(stageLevel, playerTeam, heroName, heroStat);
    }

    @Override
    public void render(float delta) {
        cameraManager.update();

        if (Gdx.input.isButtonPressed(com.badlogic.gdx.Input.Buttons.RIGHT)) {
            cameraManager.handlePan(Gdx.input.getDeltaX(), Gdx.input.getDeltaY());
        } else {
            cameraManager.stopPanning();
        }

        if (!gameOver) {
            stageTime += delta; // 시간 업데이트
            for (Unit u : units) u.update(delta);
            update(delta);
            cleanupDeadUnits();
        }

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        Vector2 touchPos = new Vector2(Gdx.input.getX(), Gdx.input.getY());
        stage.getViewport().unproject(touchPos);
        float mx = touchPos.x;
        float my = touchPos.y;

        game.batch.setProjectionMatrix(stage.getViewport().getCamera().combined);
        shape.setProjectionMatrix(stage.getViewport().getCamera().combined);

        game.batch.begin();
        game.batch.draw(battleBg, 0, 0, GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT);
        game.batch.end();

        mapRenderer.drawTiles(hoveredGrid, selectedUnit, units);

        if (!gameOver && selectedUnit != null && selectedUnit.team.equals(playerTeam)) {
            String reserved = selectedUnit.stat.getReservedSkill();
            if (reserved != null) {
                mapRenderer.drawSkillRange(selectedUnit, SkillData.get(reserved).range);
            } else {
                mapRenderer.drawRangeOverlays(selectedUnit);
            }
        }

        game.batch.begin();
        for (Unit u : units) {
            if (u.isAlive() || (gameOver && u.unitClass == Unit.UnitClass.HERO)) {
                unitRenderer.renderShadow(u, selectedUnit);
                unitRenderer.renderBody(u, selectedUnit);
                unitRenderer.renderSpeechBubble(u);
            }
        }

        gameUI.render(stageLevel, turnManager.getCurrentTurn(), playerTeam, menuHitbox, selectedUnit, mx, my, showHelp, stageTime);
        game.batch.end();

        if (gameOver) {
            drawGameOverOverlay();
            stage.act();
            stage.draw();
        }
    }

    private void update(float delta) {
        if (gameOver) return;

        if (turnManager.getCurrentTurn().equals(playerTeam)) {
            aiBusy = false;
            aiDelay = 0;
            handleInput();
        } else {
            aiDelay += delta;
            if (aiDelay >= 1.0f) {
                if (aiBusy) {
                    try {
                        AILogic.processAITurn(units, aiTeam, turnManager, this);
                    } catch (Exception e) {
                        if (!gameOver) turnManager.endTurn();
                    }
                    aiBusy = false;
                    aiDelay = 0;
                    selectedUnit = null;
                } else {
                    aiBusy = true;
                }
            }
        }
    }

    private void handleInput() {
        com.hades.game.utils.DebugManager.handleBattleDebug(game, units, aiTeam, this::handleDeath);
        if (gameOver) return;

        Vector2 touchPos = new Vector2(Gdx.input.getX(), Gdx.input.getY());
        stage.getViewport().unproject(touchPos);
        float mx = touchPos.x;
        float my = touchPos.y;

        if (Gdx.input.justTouched()) {
            if (gameUI.isHelpClicked(mx, my)) {
                game.playClick();
                showHelp = !showHelp;
                return;
            }
            if (showHelp) {
                showHelp = false;
                return;
            }
            if (menuHitbox.contains(mx, my)) {
                game.playClick();
                toggleFullscreen();
                return;
            }

            if (selectedUnit != null && selectedUnit.team.equals(playerTeam) && selectedUnit.unitClass == Unit.UnitClass.HERO) {
                String clickedSkill = gameUI.getClickedSkill(mx, my, selectedUnit);
                if (clickedSkill != null) {
                    String currentReserved = selectedUnit.stat.getReservedSkill();
                    if (clickedSkill.equals(currentReserved)) {
                        selectedUnit.stat.clearReservedSkill();
                    } else {
                        selectedUnit.stat.setReservedSkill(clickedSkill);
                    }
                    game.playClick(1.1f);
                    return;
                }
            }
        }

        if (!turnManager.getCurrentTurn().equals(playerTeam) || aiBusy) return;
        hoveredGrid = IsoUtils.screenToGrid(mx, my);

        if (Gdx.input.justTouched()) {
            if (selectedUnit != null) {
                int tx = (int) hoveredGrid.x;
                int ty = (int) hoveredGrid.y;
                if (tx >= 0 && ty >= 0 && selectedUnit.team.equals(playerTeam) && BoardManager.canMoveTo(selectedUnit, tx, ty, units)) {
                    selectedUnit.setPosition(tx, ty);
                    processMoveEnd(selectedUnit);
                    selectedUnit = null;
                    aiBusy = true;
                    turnManager.endTurn();
                    return;
                }
            }

            Unit clickedUnit = null;
            for (Unit u : units) {
                if (u.isAlive() && unitRenderer.isMouseInsideHitbox(u, mx, my)) {
                    clickedUnit = u;
                    game.playClick();
                    break;
                }
            }
            selectedUnit = clickedUnit;
        }
    }

    public void processMoveEnd(Unit unit) {
        if (gameOver) return;
        String reserved = unit.stat.getReservedSkill();
        if (reserved != null && !reserved.equals("기본 공격")) {
            executeHeroSkill(unit, reserved);
        }
        combatManager.processAutoAttack(units, unit.team);
    }

    private void executeHeroSkill(Unit hero, String skillName) {
        hero.say(skillName + "!!");
        SkillManager.executeSkill(hero, skillName, units, gameUI, playerTeam, this);
        hero.stat.clearReservedSkill();
    }

    public void handleDeath(Unit target) {
        if (gameOver) return;
        target.status = Unit.DEAD;

        boolean isEnemyBoss = target.team.equals(aiTeam) && target.unitClass == Unit.UnitClass.HERO;
        boolean isPlayerHero = target.team.equals(playerTeam) && target.unitClass == Unit.UnitClass.HERO;

        if (isEnemyBoss || isPlayerHero) {
            game.audioManager.stopBgm();
            gameOver = true;
            if (isEnemyBoss) {
                game.runState.stageBestTimes.put(stageLevel, stageTime);
                if (stageLevel == 7) {
                    game.setScreen(new com.hades.game.screens.cutscene.BaseCutsceneScreen(
                        game, com.hades.game.screens.cutscene.CutsceneManager.getStageData(8), new EndingScreen(game)
                    ));
                } else {
                    gameUI.addLog("승리! 적의 수장을 물리쳤습니다.", "SYSTEM", playerTeam);
                    showGameOverMenu(true);
                }
            } else {
                gameUI.addLog("패배... 하데스의 영웅이 전사했습니다.", "SYSTEM", playerTeam);
                showGameOverMenu(false);
            }
        }
    }

    private void showGameOverMenu(boolean isVictory) {
        Table table = new Table();
        table.setFillParent(true);
        table.center();

        Label titleLabel = new Label(isVictory ? "VICTORY!" : "DEFEAT...",
            new Label.LabelStyle(game.titleFont, isVictory ? Color.GOLD : Color.FIREBRICK));
        table.add(titleLabel).padBottom(50).row();

        if (isVictory) {
            int rewardSouls = (int)(Math.random() * 3) + 1;
            game.runState.soulFragments += rewardSouls;
            game.runState.olympusSeals += 1;
            if (game.runState.currentStageLevel <= stageLevel) {
                game.runState.currentStageLevel = stageLevel + 1;
            }
            game.saveGame();
            Label rewardLabel = new Label("보상: 영혼 파편 +" + rewardSouls + ", 인장 +1", new Label.LabelStyle(game.mainFont, Color.CYAN));
            table.add(rewardLabel).padBottom(30).row();

            Label upgradeBtn = new Label("[ 명계의 제단으로 ]", new Label.LabelStyle(game.mainFont, Color.valueOf("4FB9AF")));
            upgradeBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    game.playClick();
                    game.setScreen(new UpgradeScreen(game, heroName, game.runState.heroStat, stageLevel));
                }
            });
            UI.addHoverEffect(game, upgradeBtn, Color.valueOf("4FB9AF"), Color.WHITE);
            table.add(upgradeBtn).padBottom(20).row();
        } else {
            Label retryBtn = new Label("[ RE-TRY ]", new Label.LabelStyle(game.mainFont, Color.WHITE));
            retryBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    game.playClick();
                    game.audioManager.stopBgm();
                    game.setScreen(new BattleScreen(game, playerTeam, heroName, heroStat, stageLevel));
                }
            });
            UI.addHoverEffect(game, retryBtn, Color.WHITE, Color.GOLD);
            table.add(retryBtn).padBottom(20).row();
        }

        Label homeBtn = new Label("[ GO TO MAP ]", new Label.LabelStyle(game.mainFont, Color.LIGHT_GRAY));
        homeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.playClick();
                game.audioManager.stopBgm();
                game.setScreen(new LoadingScreen(game, new StageMapScreen(game)));
            }
        });
        UI.addHoverEffect(game, homeBtn, Color.LIGHT_GRAY, Color.WHITE);
        table.add(homeBtn).padBottom(20).row();

        Label titleBtn = new Label("[ RETURN HOME ]", new Label.LabelStyle(game.mainFont, Color.valueOf("7F8C8D")));
        titleBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.playClick();
                game.audioManager.stopBgm();
                game.setScreen(new MenuScreen(game));
            }
        });
        UI.addHoverEffect(game, titleBtn, Color.valueOf("7F8C8D"), Color.WHITE);
        table.add(titleBtn).padBottom(10);

        stage.addActor(table);
        Gdx.input.setInputProcessor(stage);
    }

    private void cleanupDeadUnits() {
        if (gameOver) return;
        for (int i = units.size - 1; i >= 0; i--) {
            if (units.get(i).status == Unit.DEAD) {
                if (selectedUnit == units.get(i)) selectedUnit = null;
                units.removeIndex(i);
            }
        }
    }

    private void toggleFullscreen() {
        if (Gdx.graphics.isFullscreen()) {
            Gdx.graphics.setWindowedMode((int) GameConfig.VIRTUAL_WIDTH, (int) GameConfig.VIRTUAL_HEIGHT);
        } else {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        }
    }

    private void drawGameOverOverlay() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0, 0, 0, 0.7f);
        shape.rect(0, 0, GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT);
        shape.end();
    }

    // TurnManager가 참조하는 필수 메서드
    public boolean isGameOver() {
        return gameOver;
    }

    public Array<Unit> getUnits() {
        return this.units;
    }

    public GameUI getGameUI() {
        return this.gameUI;
    }

    @Override
    public void resize(int w, int h) {
        stage.getViewport().update(w, h, true);
    }

    @Override
    public void dispose() {
        if (shape != null) shape.dispose();
        if (unitRenderer != null) unitRenderer.dispose();
        if (stage != null) stage.dispose();
        if (gameUI != null) gameUI.dispose();
        if (battleBg != null) battleBg.dispose();
        if (tileTop != null) tileTop.dispose();
    }
}
