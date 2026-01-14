package com.hades.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
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
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.hades.game.HadesGame;
import com.hades.game.constants.GameConfig;
import com.hades.game.constants.UnitData;
import com.hades.game.entities.Unit;
import com.hades.game.logic.AILogic;
import com.hades.game.logic.BoardManager;
import com.hades.game.logic.IsoUtils;
import com.hades.game.logic.TurnManager;
import com.hades.game.view.MapRenderer;
import com.hades.game.view.UnitRenderer;

// 클래스 역할: 전투 로직을 제어하며, 승리/패배 시 재도전 및 메뉴 이동 UI와 전용 BGM 재생을 담당합니다.
public class BattleScreen extends ScreenAdapter {
    private final HadesGame game;
    private ShapeRenderer shape;
    private Stage stage;
    private Array<Unit> units;
    private Vector2 hoveredGrid = new Vector2(-1, -1);
    private Unit selectedUnit = null;
    private TurnManager turnManager;
    private MapRenderer mapRenderer;
    private UnitRenderer unitRenderer;

    private Texture battleBg;
    private Texture logInfoBg;
    private Texture stageInfoBg;
    private Texture unitInfoBg;
    private Texture timerBoxBg;
    private Texture tileTop;

    private final String playerTeam;
    private final String aiTeam;
    private final String heroName;
    private final UnitData.Stat heroStat;
    private int stageLevel;

    private float aiDelay = 0;
    private boolean aiBusy = false;
    private boolean gameOver = false;

    private final float MENU_W = 180;
    private final float MENU_H = 60;
    private Rectangle menuHitbox;

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
    }

    // 화면이 나타날 때 음악 전환 로직
    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);

        // 메뉴 BGM 중지
        if (game.menuBgm != null && game.menuBgm.isPlaying()) {
            game.menuBgm.stop();
        }

        // 전투 BGM 재생
        if (game.battleBgm != null && !game.battleBgm.isPlaying()) {
            game.battleBgm.play();
        }
    }

    // 화면이 숨겨지거나 바뀔 때 음악 정지
    @Override
    public void hide() {
        if (game.battleBgm != null) {
            game.battleBgm.stop();
        }
        Gdx.input.setInputProcessor(null);
    }

    private void loadResources() {
        String path = "images/background/";
        battleBg = new Texture(Gdx.files.internal(path + "battle_background.png"));
        logInfoBg = new Texture(Gdx.files.internal(path + "log_info.png"));
        stageInfoBg = new Texture(Gdx.files.internal(path + "stage_info.png"));
        unitInfoBg = new Texture(Gdx.files.internal(path + "unit_info.png"));
        timerBoxBg = new Texture(Gdx.files.internal(path + "timer_box.png"));
        tileTop = new Texture(Gdx.files.internal(path + "tile_top.png"));
    }

    private void init() {
        shape = new ShapeRenderer();
        mapRenderer = new MapRenderer(shape, game.batch, tileTop);
        unitRenderer = new UnitRenderer(game.batch, shape, game.unitFont, playerTeam);
        units = new Array<>();
        turnManager = new TurnManager();
        setupBattleUnits();
    }

    private void setupBattleUnits() {
        units.clear();
        units.add(new Unit(heroName, playerTeam, heroStat, 3, 0));
        for (int x = 0; x < GameConfig.BOARD_WIDTH; x++) {
            if (x == 3) continue;
            units.add(new Unit("케로", playerTeam, UnitData.HADES_SOLDIER, x, 0));
        }
        int enemyRow = GameConfig.BOARD_HEIGHT - 1;
        int bossIdx = Math.min(stageLevel - 1, UnitData.STATS_ZEUS.length - 1);
        UnitData.Stat bossStat = UnitData.STATS_ZEUS[bossIdx];
        String bossName = UnitData.NAMES_ZEUS[bossIdx];
        units.add(new Unit(bossName, aiTeam, bossStat, 3, enemyRow));
        for (int x = 0; x < GameConfig.BOARD_WIDTH; x++) {
            if (x == 3) continue;
            units.add(new Unit("병사", aiTeam, UnitData.ZEUS_SOLDIER, x, enemyRow));
        }
    }

    @Override
    public void render(float delta) {
        update(delta);
        cleanupDeadUnits();
        game.batch.setProjectionMatrix(stage.getViewport().getCamera().combined);
        shape.setProjectionMatrix(stage.getViewport().getCamera().combined);
        draw();
    }

    private void handleInput() {
        if (gameOver) return;

        Vector2 touchPos = new Vector2(Gdx.input.getX(), Gdx.input.getY());
        stage.getViewport().unproject(touchPos);
        float mx = touchPos.x;
        float my = touchPos.y;

        if (Gdx.input.justTouched() && menuHitbox.contains(mx, my)) {
            game.playClick(1.0f);
            toggleFullscreen();
            return;
        }

        if (!turnManager.getCurrentTurn().equals(playerTeam) || aiBusy) return;

        hoveredGrid = IsoUtils.screenToGrid(mx, my);

        if (Gdx.input.justTouched()) {
            if (selectedUnit != null) {
                int tx = (int) hoveredGrid.x;
                int ty = (int) hoveredGrid.y;
                if (tx >= 0 && ty >= 0 && selectedUnit.team.equals(playerTeam) && BoardManager.canMoveTo(selectedUnit, tx, ty, units)) {
                    selectedUnit.setPosition(tx, ty);
                    processAutoAttack(playerTeam);
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
                    break;
                }
            }
            selectedUnit = (clickedUnit != null) ? clickedUnit : null;
        }
    }

    private void toggleFullscreen() {
        if (Gdx.graphics.isFullscreen()) {
            Gdx.graphics.setWindowedMode((int) GameConfig.VIRTUAL_WIDTH, (int) GameConfig.VIRTUAL_HEIGHT);
        } else {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        }
    }

    private void draw() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.batch.begin();
        game.batch.draw(battleBg, 0, 0, GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT);
        game.batch.end();

        mapRenderer.drawTiles(hoveredGrid);

        if (!gameOver && selectedUnit != null) {
            mapRenderer.drawRangeOverlays(selectedUnit, units);
        }

        game.batch.begin();
        for (Unit unit : units) { if (unit.isAlive()) unitRenderer.renderShadow(unit, selectedUnit); }
        game.batch.end();

        game.batch.begin();
        for (Unit unit : units) { if (unit.isAlive()) unitRenderer.renderBody(unit, selectedUnit); }
        drawUIElements();
        game.batch.end();

        if (gameOver) {
            drawGameOverOverlay();
            stage.act();
            stage.draw();
        }
    }

    private void drawUIElements() {
        game.batch.draw(stageInfoBg, 20, GameConfig.VIRTUAL_HEIGHT - 100, 240, 80);
        game.unitFont2.setColor(Color.WHITE);
        game.unitFont2.draw(game.batch, "STAGE " + stageLevel, 80, GameConfig.VIRTUAL_HEIGHT - 45);

        String currentTurn = turnManager.getCurrentTurn();
        game.unitFont2.setColor(currentTurn.equals(playerTeam) ? Color.LIME : Color.RED);
        game.unitFont2.draw(game.batch, currentTurn.equals(playerTeam) ? "YOUR TURN" : "ENEMY TURN", 40, GameConfig.VIRTUAL_HEIGHT - 110);

        game.batch.draw(timerBoxBg, menuHitbox.x, menuHitbox.y, menuHitbox.width, menuHitbox.height);
        String screenModeText = Gdx.graphics.isFullscreen() ? "window" : "fullscreen";
        game.unitFont3.setColor(Color.valueOf("4FB9AF"));
        game.unitFont3.draw(game.batch, screenModeText, menuHitbox.x, menuHitbox.y + 42, menuHitbox.width, Align.center, false);

        game.batch.draw(logInfoBg, 240, 20, GameConfig.VIRTUAL_WIDTH - 260, 200);
        if (selectedUnit != null) {
            game.batch.draw(unitInfoBg, 10, 20, 300, 400);
        }
    }

    private void drawGameOverOverlay() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shape.setProjectionMatrix(stage.getViewport().getCamera().combined);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0, 0, 0, 0.7f);
        shape.rect(0, 0, GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT);
        shape.end();
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
                    try { AILogic.processAITurn(units, aiTeam, turnManager, this); }
                    catch (Exception e) { turnManager.endTurn(); }
                    aiBusy = false;
                    aiDelay = 0;
                    selectedUnit = null;
                } else { aiBusy = true; }
            }
        }
    }

    private void cleanupDeadUnits() {
        for (int i = units.size - 1; i >= 0; i--) {
            Unit u = units.get(i);
            if (u.status == Unit.DEAD) {
                if (selectedUnit == u) selectedUnit = null;
                units.removeIndex(i);
            }
        }
    }

    public void processAutoAttack(String team) {
        for (int i = 0; i < units.size; i++) {
            Unit attacker = units.get(i);
            if (attacker != null && attacker.isAlive() && attacker.team.equals(team)) {
                Unit target = BoardManager.findBestTargetInRange(attacker, units);
                if (target != null) performAttack(attacker, target);
            }
        }
    }

    public void performAttack(Unit attacker, Unit target) {
        if (target == null || !target.isAlive()) return;
        int dmg = attacker.team.equals(turnManager.getCurrentTurn()) ? attacker.stat.atk() : attacker.stat.counterAtk();
        target.currentHp -= dmg;
        if (target.currentHp <= 0) {
            target.currentHp = 0;
            handleDeath(attacker, target);
        }
    }

    private void handleDeath(Unit attacker, Unit target) {
        target.status = Unit.DEAD;

        boolean isEnemyBoss = target.team.equals(aiTeam) && !target.stat.skillName().equals("일반 병사");
        boolean isPlayerHero = target.team.equals(playerTeam) && target.name.equals(heroName);

        if (isEnemyBoss) {
            gameOver = true;
            showGameOverMenu(true);
        } else if (isPlayerHero) {
            gameOver = true;
            showGameOverMenu(false);
        }
    }

    private void showGameOverMenu(boolean isVictory) {
        Table table = new Table();
        table.setFillParent(true);
        table.center();

        String resultText = isVictory ? "VICTORY!" : "DEFEAT...";
        Color titleColor = isVictory ? Color.GOLD : Color.FIREBRICK;
        Label titleLabel = new Label(resultText, new Label.LabelStyle(game.titleFont, titleColor));
        table.add(titleLabel).padBottom(60).row();

        if (isVictory) {
            if (stageLevel < 7) {
                Label nextBtn = new Label("[ NEXT STAGE ]", new Label.LabelStyle(game.mainFont, Color.valueOf("4FB9AF")));
                nextBtn.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        game.playClick(1.0f);
                        game.setScreen(new BattleScreen(game, playerTeam, heroName, heroStat, stageLevel + 1));
                    }
                });
                table.add(nextBtn).padBottom(20).row();
            }
        } else {
            Label retryBtn = new Label("[ RE-TRY ]", new Label.LabelStyle(game.mainFont, Color.WHITE));
            retryBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    game.playClick(1.0f);
                    game.setScreen(new BattleScreen(game, playerTeam, heroName, heroStat, stageLevel));
                }
            });
            table.add(retryBtn).padBottom(20).row();
        }

        Label homeBtn = new Label("[ BACK TO MENU ]", new Label.LabelStyle(game.mainFont, Color.LIGHT_GRAY));
        homeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.playClick(1.0f);
                game.setScreen(new MenuScreen(game));
            }
        });
        table.add(homeBtn);

        stage.addActor(table);
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        if (shape != null) shape.dispose();
        if (unitRenderer != null) unitRenderer.dispose();
        if (stage != null) stage.dispose();
        battleBg.dispose();
        logInfoBg.dispose();
        stageInfoBg.dispose();
        unitInfoBg.dispose();
        timerBoxBg.dispose();
        tileTop.dispose();
    }
}
