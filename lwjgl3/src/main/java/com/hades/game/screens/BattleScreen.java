package com.hades.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
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
 * [클래스 역할] 전투 화면을 담당하며, 선택한 영웅과 병사들을 배치하고 스테이지를 관리합니다.
 */
public class BattleScreen extends ScreenAdapter {
    private final HadesGame game;
    private ShapeRenderer shape;
    private Array<Unit> units;
    private Vector2 hoveredGrid = new Vector2(-1, -1);
    private Unit selectedUnit = null;
    private TurnManager turnManager;
    private MapRenderer mapRenderer;
    private UnitRenderer unitRenderer;

    private final String playerTeam;
    private final String aiTeam;

    // --- 추가된 데이터 필드 ---
    private final String heroName;
    private final UnitData.Stat heroStat;
    private int stageLevel;

    private float aiDelay = 0;
    private boolean aiBusy = false;
    private boolean gameOver = false;
    private String winner = "";

    /**
     * [수정] HeroSelectionScreen에서 전달하는 인자(영웅 정보, 스테이지 번호)를 받도록 생성자를 변경했습니다.
     */
    public BattleScreen(HadesGame game, String playerTeam, String heroName, UnitData.Stat heroStat, int stageLevel) {
        this.game = game;
        this.playerTeam = playerTeam;
        this.heroName = heroName;
        this.heroStat = heroStat;
        this.stageLevel = stageLevel;
        this.aiTeam = playerTeam.equals("HADES") ? "ZEUS" : "HADES";
        init();
    }

    private void init() {
        shape = new ShapeRenderer();
        mapRenderer = new MapRenderer(shape);
        unitRenderer = new UnitRenderer(game.batch, shape, game.font, playerTeam);

        units = new Array<>();
        turnManager = new TurnManager();

        // [수정] 기존 setupTeam 대신 새로운 배치 로직 호출
        setupBattleUnits();
    }

    /**
     * [신규] 영웅 1명 + 병사 6명 시스템에 맞게 유닛을 배치합니다.
     */
    private void setupBattleUnits() {
        // 1. 플레이어 팀 배치 (0번 행)
        units.add(new Unit(heroName, playerTeam, heroStat, 3, 0)); // 영웅 중앙 배치
        for (int x = 0; x < GameConfig.BOARD_WIDTH; x++) {
            if (x == 3) continue;
            units.add(new Unit("병사", playerTeam, UnitData.SOLDIER, x, 0));
        }

        // 2. 적 팀 배치 (마지막 행)
        int enemyRow = GameConfig.BOARD_HEIGHT - 1;
        String[] enemyPool = aiTeam.equals("HADES") ? UnitData.NAMES_HADES : UnitData.NAMES_ZEUS;
        String bossName = enemyPool[(stageLevel - 1) % enemyPool.length];

        units.add(new Unit(bossName, aiTeam, UnitData.RULER, 3, enemyRow)); // 적 킹(보스)
        for (int x = 0; x < GameConfig.BOARD_WIDTH; x++) {
            if (x == 3) continue;
            units.add(new Unit("적 병사", aiTeam, UnitData.SOLDIER, x, enemyRow));
        }
    }

    @Override
    public void render(float delta) {
        update(delta);
        cleanupDeadUnits();
        draw();
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
                        System.err.println("[AI 오류] " + e.getMessage());
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

    private void handleInput() {
        if (!turnManager.getCurrentTurn().equals(playerTeam) || aiBusy || gameOver) return;

        float mx = Gdx.input.getX();
        float my = Gdx.graphics.getHeight() - Gdx.input.getY();
        hoveredGrid = IsoUtils.screenToGrid(mx, my);

        if (Gdx.input.justTouched()) {
            if (hoveredGrid.x < 0 || hoveredGrid.y < 0) return;

            int tx = (int) hoveredGrid.x;
            int ty = (int) hoveredGrid.y;
            Unit clicked = BoardManager.getUnitAt(units, tx, ty);

            if (clicked != null && clicked.isAlive() && clicked.team.equals(playerTeam)) {
                selectedUnit = clicked;
            } else if (selectedUnit != null) {
                if (BoardManager.canMoveTo(selectedUnit, tx, ty, units)) {
                    selectedUnit.setPosition(tx, ty);
                    processAutoAttack(playerTeam);
                    selectedUnit = null;
                    aiBusy = true;
                    turnManager.endTurn();
                } else {
                    selectedUnit = null;
                }
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

    /**
     * [수정] 적 킹(보스) 사망 시 다음 스테이지로 넘어가도록 로직을 확장했습니다.
     */
    private void handleDeath(Unit attacker, Unit target) {
        target.status = Unit.DEAD;

        // 적 팀 보스(RULER) 처치 시
        if (target.team.equals(aiTeam) && "왕의 위엄".equals(target.stat.skillName())) {
            if (stageLevel < 6) {
                System.out.println("STAGE CLEAR! " + (stageLevel + 1) + "단계로 진입합니다.");
                game.setScreen(new BattleScreen(game, playerTeam, heroName, heroStat, stageLevel + 1));
            } else {
                gameOver = true;
                winner = attacker.team;
                System.out.println("전부 클리어! 최종 승리!");
            }
        }
        // 아군 영웅 사망 시 (이름으로 대조)
        else if (target.name.equals(heroName)) {
            gameOver = true;
            winner = aiTeam;
            System.out.println("영웅 사망... 게임 오버");
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
                unitRenderer.render(unit);
                if (unit == selectedUnit) drawSelectionHighlight(unit);
            }
        }

        game.font.setColor(Color.WHITE);
        game.font.draw(game.batch, "STAGE " + stageLevel, 20, Gdx.graphics.getHeight() - 20);
        game.font.draw(game.batch, "HERO: " + heroName, 20, Gdx.graphics.getHeight() - 50);

        if (selectedUnit != null) drawUnitCard();
        if (gameOver) drawVictoryMessage();
        game.batch.end();
    }

    private void drawSelectionHighlight(Unit unit) {
        Vector2 pos = IsoUtils.gridToScreen(unit.gridX, unit.gridY);
        game.batch.end();
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(Color.YELLOW);
        shape.ellipse(pos.x - 20, pos.y - 12, 40, 20);
        shape.end();
        game.batch.begin();
    }

    private void drawUnitCard() {
        float x = 20, y = 120;
        game.font.setColor(Color.GOLD);
        game.font.draw(game.batch, "[ " + selectedUnit.name + " ]", x, y);
        game.font.setColor(Color.WHITE);
        game.font.draw(game.batch, "HP: " + selectedUnit.currentHp + " / " + selectedUnit.stat.hp(), x, y - 25);
        game.font.draw(game.batch, "ATK: " + selectedUnit.stat.atk(), x, y - 50);
    }

    private void drawVictoryMessage() {
        game.batch.end();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0, 0, 0, 0.5f);
        shape.rect(0, 300, Gdx.graphics.getWidth(), 200);
        shape.end();
        game.batch.begin();

        game.font.setColor(Color.YELLOW);
        game.font.draw(game.batch, "STAGE CLEAR / VICTORY!", 150, 420);
    }

    @Override
    public void dispose() {
        if (shape != null) shape.dispose();
        if (unitRenderer != null) unitRenderer.dispose();
    }
}
