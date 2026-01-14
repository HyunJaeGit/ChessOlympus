package com.hades.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
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

/**
 * [클래스 역할] 전투 화면의 핵심 로직을 담당하며, 아이소메트릭 좌표계 기반의 유닛 이동,
 * 전투 진행, AI 턴 처리 및 승리 조건을 관리합니다.
 */
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

    private final String playerTeam;
    private final String aiTeam;
    private final String heroName;
    private final UnitData.Stat heroStat;
    private int stageLevel;

    private float aiDelay = 0;
    private boolean aiBusy = false;
    private boolean gameOver = false;
    private String winner = "";

    public BattleScreen(HadesGame game, String playerTeam, String heroName, UnitData.Stat heroStat, int stageLevel) {
        this.game = game;
        this.playerTeam = playerTeam;
        this.heroName = heroName;
        this.heroStat = heroStat;
        this.stageLevel = stageLevel;
        this.aiTeam = playerTeam.equals("HADES") ? "ZEUS" : "HADES";

        /**
         * [수정] HadesGame.VIRTUAL_WIDTH 대신 GameConfig.VIRTUAL_WIDTH를 사용합니다.
         */
        this.stage = new Stage(new FitViewport(GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT));

        init();
    }

    private void init() {
        shape = new ShapeRenderer();
        mapRenderer = new MapRenderer(shape);
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
            units.add(new Unit("하데스 병사", playerTeam, UnitData.HADES_SOLDIER, x, 0));
        }

        int enemyRow = GameConfig.BOARD_HEIGHT - 1;
        int bossIdx = Math.min(stageLevel - 1, UnitData.STATS_ZEUS.length - 1);
        UnitData.Stat bossStat = UnitData.STATS_ZEUS[bossIdx];
        String bossName = UnitData.NAMES_ZEUS[bossIdx];

        units.add(new Unit(bossName, aiTeam, bossStat, 3, enemyRow));

        for (int x = 0; x < GameConfig.BOARD_WIDTH; x++) {
            if (x == 3) continue;
            units.add(new Unit("제우스 병사", aiTeam, UnitData.ZEUS_SOLDIER, x, enemyRow));
        }
    }

    @Override
    public void render(float delta) {
        update(delta);
        cleanupDeadUnits();

        /**
         * [수정] 뷰포트 카메라 시점 동기화
         */
        game.batch.setProjectionMatrix(stage.getViewport().getCamera().combined);
        shape.setProjectionMatrix(stage.getViewport().getCamera().combined);

        draw();
    }

    private void handleInput() {
        if (!turnManager.getCurrentTurn().equals(playerTeam) || aiBusy || gameOver) return;

        Vector2 touchPos = new Vector2(Gdx.input.getX(), Gdx.input.getY());
        stage.getViewport().unproject(touchPos);

        float mx = touchPos.x;
        float my = touchPos.y;

        hoveredGrid = IsoUtils.screenToGrid(mx, my);

        if (Gdx.input.justTouched()) {
            if (selectedUnit != null) {
                int tx = (int) hoveredGrid.x;
                int ty = (int) hoveredGrid.y;

                if (tx >= 0 && ty >= 0 && BoardManager.canMoveTo(selectedUnit, tx, ty, units)) {
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

            if (clickedUnit != null && clickedUnit.team.equals(playerTeam)) {
                selectedUnit = clickedUnit;
            } else {
                selectedUnit = null;
            }
        }
    }

    private void draw() {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        shape.begin(ShapeRenderer.ShapeType.Line);
        mapRenderer.drawGrid(hoveredGrid, turnManager.getCurrentTurn());
        if (!gameOver && selectedUnit != null) {
            mapRenderer.drawRangeOverlays(selectedUnit, units);
        }
        shape.end();

        game.batch.begin();
        for (Unit unit : units) {
            if (unit.isAlive()) {
                unitRenderer.render(unit, selectedUnit);
            }
        }

        /**
         * [수정] UI 텍스트 위치 계산 시 GameConfig 참조
         */
        game.detailFont.setColor(Color.WHITE);
        game.detailFont.draw(game.batch, "STAGE " + stageLevel, 20, GameConfig.VIRTUAL_HEIGHT - 20);
        game.detailFont.draw(game.batch, "HERO: " + heroName, 20, GameConfig.VIRTUAL_HEIGHT - 50);

        if (selectedUnit != null) drawUnitCard();
        if (gameOver) drawVictoryMessage();
        game.batch.end();
    }

    private void drawUnitCard() {
        float x = 20, y = 140;
        game.detailFont.setColor(Color.GOLD);
        game.detailFont.draw(game.batch, "[ " + selectedUnit.name + " ]", x, y);
        game.detailFont.setColor(Color.CYAN);
        game.detailFont.draw(game.batch, "SKILL: " + selectedUnit.stat.skillName(), x, y - 25);
        game.detailFont.setColor(Color.WHITE);
        game.detailFont.draw(game.batch, "HP: " + selectedUnit.currentHp + " / " + selectedUnit.stat.hp(), x, y - 55);
        game.detailFont.draw(game.batch, "ATK: " + selectedUnit.stat.atk(), x, y - 80);
        game.detailFont.draw(game.batch, "RANGE: " + selectedUnit.stat.range(), x, y - 105);
    }

    private void drawVictoryMessage() {
        game.batch.end();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0, 0, 0, 0.5f);
        /**
         * [수정] 승리 박스 크기 GameConfig 참조
         */
        shape.rect(0, 300, GameConfig.VIRTUAL_WIDTH, 200);
        shape.end();

        game.batch.begin();
        game.detailFont.setColor(Color.YELLOW);
        game.detailFont.draw(game.batch, "STAGE CLEAR / VICTORY!", 150, 420);
    }

    private void update(float delta) {
        if (gameOver) return;
        String currentTurn = turnManager.getCurrentTurn();

        if (currentTurn.equals(playerTeam)) {
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
                        turnManager.endTurn();
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
        if (target.team.equals(aiTeam) && !target.stat.skillName().equals("일반 병사")) {
            if (stageLevel < 7) {
                game.setScreen(new BattleScreen(game, playerTeam, heroName, heroStat, stageLevel + 1));
            } else {
                gameOver = true;
                winner = attacker.team;
            }
        } else if (target.team.equals(playerTeam) && target.name.equals(heroName)) {
            gameOver = true;
            winner = aiTeam;
        }
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
    }
}
