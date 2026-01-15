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
import com.badlogic.gdx.graphics.Color;
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
import com.hades.game.view.GameUI;
import com.hades.game.view.MapRenderer;
import com.hades.game.view.UnitRenderer;

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
    private GameUI gameUI;

    private Texture battleBg;
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

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        if (game.menuBgm != null && game.menuBgm.isPlaying()) game.menuBgm.stop();
        if (game.battleBgm != null && !game.battleBgm.isPlaying()) game.battleBgm.play();
    }

    private void loadResources() {
        battleBg = new Texture(Gdx.files.internal("images/background/battle_background.png"));
        tileTop = new Texture(Gdx.files.internal("images/background/tile_top.png"));
    }

    private void init() {
        shape = new ShapeRenderer();
        mapRenderer = new MapRenderer(shape, game.batch, tileTop);
        unitRenderer = new UnitRenderer(game.batch, shape, game.unitFont, playerTeam);
        gameUI = new GameUI(game);

        units = new Array<>();
        turnManager = new TurnManager();
        setupBattleUnits();
    }

    // [업데이트] 브레인스토밍된 7인 체제 배치 (영웅은 중앙, 나머지는 레인 배치)
    private void setupBattleUnits() {
        units.clear();

        // --- 플레이어 진영 (HADES) ---
        // 레인 순서: 궁병(0), 기병(1), 방패병(2), 영웅(3), 방패병(4), 전차병(5), 성녀(6)
        units.add(new Unit(heroName, playerTeam, heroStat, Unit.UnitClass.HERO, 3, 0));
        units.add(new Unit("궁병", playerTeam, UnitData.CLASS_ARCHER, Unit.UnitClass.ARCHER, 0, 0));
        units.add(new Unit("기병", playerTeam, UnitData.CLASS_KNIGHT, Unit.UnitClass.KNIGHT, 1, 0));
        units.add(new Unit("방패병1", playerTeam, UnitData.CLASS_SHIELD, Unit.UnitClass.SHIELD, 2, 0));
        units.add(new Unit("방패병2", playerTeam, UnitData.CLASS_SHIELD, Unit.UnitClass.SHIELD, 4, 0));
        units.add(new Unit("전차병", playerTeam, UnitData.CLASS_CHARIOT, Unit.UnitClass.CHARIOT, 5, 0));
        units.add(new Unit("성녀", playerTeam, UnitData.CLASS_SAINT, Unit.UnitClass.SAINT, 6, 0));

        // --- 적 진영 (ZEUS) ---
        int enemyRow = GameConfig.BOARD_HEIGHT - 1;
        int bossIdx = Math.min(stageLevel - 1, UnitData.STATS_ZEUS.length - 1);

        units.add(new Unit(UnitData.NAMES_ZEUS[bossIdx], aiTeam, UnitData.STATS_ZEUS[bossIdx], Unit.UnitClass.HERO, 3, enemyRow));
        units.add(new Unit("적 궁병", aiTeam, UnitData.CLASS_ARCHER, Unit.UnitClass.ARCHER, 0, enemyRow));
        units.add(new Unit("적 기병", aiTeam, UnitData.CLASS_KNIGHT, Unit.UnitClass.KNIGHT, 1, enemyRow));
        units.add(new Unit("적 방패병1", aiTeam, UnitData.CLASS_SHIELD, Unit.UnitClass.SHIELD, 2, enemyRow));
        units.add(new Unit("적 방패병2", aiTeam, UnitData.CLASS_SHIELD, Unit.UnitClass.SHIELD, 4, enemyRow));
        units.add(new Unit("적 전차병", aiTeam, UnitData.CLASS_CHARIOT, Unit.UnitClass.CHARIOT, 5, enemyRow));
        units.add(new Unit("적 성녀", aiTeam, UnitData.CLASS_SAINT, Unit.UnitClass.SAINT, 6, enemyRow));
    }

    @Override
    public void render(float delta) {
        update(delta);
        cleanupDeadUnits();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.batch.setProjectionMatrix(stage.getViewport().getCamera().combined);
        shape.setProjectionMatrix(stage.getViewport().getCamera().combined);

        game.batch.begin();
        game.batch.draw(battleBg, 0, 0, GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT);
        game.batch.end();

        mapRenderer.drawTiles(hoveredGrid);
        if (!gameOver && selectedUnit != null) {
            mapRenderer.drawRangeOverlays(selectedUnit, units);
        }

        game.batch.begin();
        for (Unit u : units) { if (u.isAlive()) unitRenderer.renderShadow(u, selectedUnit); }
        for (Unit u : units) { if (u.isAlive()) unitRenderer.renderBody(u, selectedUnit); }

        gameUI.render(stageLevel, turnManager.getCurrentTurn(), playerTeam, menuHitbox, selectedUnit);
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
                    try { AILogic.processAITurn(units, aiTeam, turnManager, this); }
                    catch (Exception e) { turnManager.endTurn(); }
                    aiBusy = false;
                    aiDelay = 0;
                    selectedUnit = null;
                } else { aiBusy = true; }
            }
        }
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

                    // 이동 후 자동 액션 수행 (공격 및 힐)
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
            selectedUnit = clickedUnit;
        }
    }

    // 자동 공격 및 성녀의 자동 치유 로직
    public void processAutoAttack(String team) {
        // 1. 공격 단계
        for (int i = 0; i < units.size; i++) {
            Unit attacker = units.get(i);
            if (attacker != null && attacker.isAlive() && attacker.team.equals(team)) {

                // 기병(KNIGHT)은 사거리 내 모든 적 광역 공격
                if (attacker.unitClass == Unit.UnitClass.KNIGHT) {
                    // findAllTargetsInRange 내부에서도 인덱스 for문을 써야 안전합니다.
                    Array<Unit> targets = BoardManager.findAllTargetsInRange(attacker, units);
                    for (int j = 0; j < targets.size; j++) {
                        performAttack(attacker, targets.get(j));
                    }
                }
                // 나머지 병과 및 영웅은 단일 공격
                else {
                    Unit target = BoardManager.findBestTargetInRange(attacker, units);
                    if (target != null) {
                        performAttack(attacker, target);
                    }
                }
            }
        }

        // 2. 성녀의 치유 단계
        processAutoHeal(team);
    }

    // [수정] 중첩 반복 에러 해결을 위해 인덱스 기반 for문 사용
    private void processAutoHeal(String team) {
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            // 해당 팀의 성녀 찾기
            if (u.isAlive() && u.team.equals(team) && u.unitClass == Unit.UnitClass.SAINT) {
                for (int j = 0; j < units.size; j++) {
                    Unit ally = units.get(j);
                    // 주변 상하좌우 1칸 아군 중 체력이 깎인 유닛 회복
                    if (ally.isAlive() && ally.team.equals(team) && ally != u) {
                        int dist = Math.abs(u.gridX - ally.gridX) + Math.abs(u.gridY - ally.gridY);
                        if (dist == 1 && ally.currentHp < ally.stat.hp()) {
                            ally.currentHp = Math.min(ally.stat.hp(), ally.currentHp + 15);
                            // // 회복 로그 (필요시 사용)
                        }
                    }
                }
            }
        }
    }

    public void performAttack(Unit attacker, Unit target) {
        if (attacker == null || target == null || !target.isAlive() || !attacker.isAlive()) return;

        boolean isAttackerTurn = turnManager.isMyTurn(attacker.team);
        int atkDmg = attacker.getPower(isAttackerTurn);
        target.currentHp -= atkDmg;

        if (target.currentHp <= 0) {
            target.currentHp = 0;
            handleDeath(target);
            return;
        }

        // 수비자의 반격 (기병 광역 공격 시에도 각자 반격함)
        if (target.canReach(attacker)) {
            int counterDmg = target.getPower(turnManager.isMyTurn(target.team));
            attacker.currentHp -= counterDmg;

            if (attacker.currentHp <= 0) {
                attacker.currentHp = 0;
                handleDeath(attacker);
            }
        }
    }

    private void handleDeath(Unit target) {
        target.status = Unit.DEAD;
        boolean isEnemyBoss = target.team.equals(aiTeam) && target.unitClass == Unit.UnitClass.HERO;
        boolean isPlayerHero = target.team.equals(playerTeam) && target.unitClass == Unit.UnitClass.HERO;

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
        Label titleLabel = new Label(resultText, new Label.LabelStyle(game.titleFont, isVictory ? Color.GOLD : Color.FIREBRICK));
        table.add(titleLabel).padBottom(60).row();

        if (isVictory && stageLevel < 7) {
            Label nextBtn = new Label("[ NEXT STAGE ]", new Label.LabelStyle(game.mainFont, Color.valueOf("4FB9AF")));
            nextBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    game.playClick(1.0f);
                    game.setScreen(new BattleScreen(game, playerTeam, heroName, heroStat, stageLevel + 1));
                }
            });
            table.add(nextBtn).padBottom(20).row();
        } else if (!isVictory) {
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

    private void cleanupDeadUnits() {
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
        Gdx.gl.glDisable(GL20.GL_BLEND);
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
        if (gameUI != null) gameUI.dispose();
        battleBg.dispose();
        tileTop.dispose();
    }
}
