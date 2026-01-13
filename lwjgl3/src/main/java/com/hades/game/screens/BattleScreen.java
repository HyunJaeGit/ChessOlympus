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

/**
 * 실제 전투 게임 플레이를 처리하는 메인 스크린 클래스입니다.
 * 플레이어가 선택한 진영에 따라 입력을 제어하며, 상대 팀은 AI가 조종합니다.
 */
public class BattleScreen extends ScreenAdapter {
    private final HadesGame game;
    private ShapeRenderer shape;
    private Array<Unit> units;
    private Array<Unit> deadPool;
    private Vector2 hoveredGrid = new Vector2(-1, -1);
    private Unit selectedUnit = null;
    private TurnManager turnManager;
    private MapRenderer mapRenderer;

    private String playerTeam; // 플레이어가 선택한 진영 (HADES 또는 ZEUS)
    private String aiTeam;     // 플레이어의 반대 진영
    private float aiDelay = 0;
    private boolean aiBusy = false;
    private boolean gameOver = false;
    private String winner = "";

    /**
     * [메서드 설명] 생성자에서 플레이어가 선택한 진영 정보를 받아 초기화합니다.
     * @param game 메인 게임 인스턴스
     * @param playerTeam 선택된 진영 이름
     */
    public BattleScreen(HadesGame game, String playerTeam) {
        this.game = game;
        this.playerTeam = playerTeam;
        this.aiTeam = playerTeam.equals("HADES") ? "ZEUS" : "HADES";
        init();
    }

    /* 초기 자원을 설정하고 게임 요소를 배치합니다. */
    private void init() {
        shape = new ShapeRenderer();
        mapRenderer = new MapRenderer(shape);
        units = new Array<>();
        deadPool = new Array<>();
        turnManager = new TurnManager();

        // 팀 배치 (진영 선택과 상관없이 고정된 위치에 소환)
        setupTeam("HADES", 0);
        setupTeam("ZEUS", GameConfig.BOARD_HEIGHT - 1);
    }

    /* 팀별 유닛 소환 로직 */
    private void setupTeam(String team, int row) {
        UnitData.Stat[] stats = {
            UnitData.WARRIOR, UnitData.ALCHEMIST, UnitData.ASSASSIN,
            UnitData.RULER, UnitData.KNIGHT, UnitData.PRIEST, UnitData.ARCHER
        };
        String[] names = team.equals("HADES") ? UnitData.NAMES_HADES : UnitData.NAMES_ZEUS;

        for (int x = 0; x < GameConfig.BOARD_WIDTH; x++) {
            units.add(new Unit(names[x], team, stats[x], x, row));
        }
    }

    @Override
    public void render(float delta) {
        update(delta);

        // 사망 유닛 제거 처리
        if (deadPool.size > 0) {
            for (int i = 0; i < deadPool.size; i++) {
                units.removeValue(deadPool.get(i), true);
            }
            deadPool.clear();
        }

        draw();
    }

    // AI 턴 종료 후 딜레이를 확실히 리셋하여 무한 루프를 방지합니다.
    private void update(float delta) {
        if (gameOver) return;

        String currentTurn = turnManager.getCurrentTurn();

        // 1. 플레이어의 턴인 경우
        if (currentTurn.equals(playerTeam)) {
            aiBusy = false;
            aiDelay = 0; // 플레이어 턴 동안 AI 딜레이를 0으로 유지
            handleInput();
        }
        // 2. AI의 턴인 경우
        else {
            aiDelay += delta;
            // 1초 대기 후 AI 실행
            if (aiDelay >= 1.0f && !aiBusy) {
                aiBusy = true;
                try {
                    // AI에게 현재 턴인 진영(aiTeam)을 조종하라고 명령
                    AILogic.processAITurn(units, aiTeam, turnManager, this);

                    // [중요] AI 행동 직후 딜레이를 리셋하여 핑퐁 현상 방지
                    aiDelay = 0;
                } catch (Exception e) {
                    System.err.println("[AI 오류] " + e.getMessage());
                    turnManager.endTurn();
                }
                selectedUnit = null;
            }
        }
    }

    /* 마우스 클릭 입력 처리 */
    private void handleInput() {
        if (!turnManager.isMyTurn(playerTeam) || aiBusy) return;

        float mx = Gdx.input.getX();
        float my = Gdx.graphics.getHeight() - Gdx.input.getY();
        hoveredGrid = IsoUtils.screenToGrid(mx, my);

        if (Gdx.input.justTouched()) {
            int tx = (int) hoveredGrid.x;
            int ty = (int) hoveredGrid.y;
            Unit clicked = BoardManager.getUnitAt(units, tx, ty);

            // 내 유닛을 클릭한 경우 선택
            if (clicked != null && clicked.team.equals(playerTeam)) {
                selectedUnit = clicked;
            }
            // 빈 공간이나 적을 클릭하여 이동 시도
            else if (selectedUnit != null) {
                if (BoardManager.canMoveTo(selectedUnit, tx, ty, units)) {
                    selectedUnit.setPosition(tx, ty);
                    processAutoAttack(playerTeam);
                    selectedUnit = null;
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
            if (attacker != null && attacker.currentHp > 0 && attacker.team.equals(team)) {
                Unit target = BoardManager.findBestTargetInRange(attacker, units);
                if (target != null) performAttack(attacker, target);
            }
        }
    }

    public void performAttack(Unit attacker, Unit target) {
        String turn = turnManager.getCurrentTurn();
        int dmg = attacker.team.equals(turn) ? attacker.stat.atk() : attacker.stat.counterAtk();
        target.currentHp -= dmg;
        if (target.currentHp <= 0) handleDeath(attacker, target);
    }

    private void handleDeath(Unit attacker, Unit target) {
        if ("왕의 위엄".equals(target.stat.skillName())) {
            gameOver = true;
            winner = attacker.team;
        }
        if (!deadPool.contains(target, true)) deadPool.add(target);
    }

    private void draw() {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        shape.begin(ShapeRenderer.ShapeType.Line);
        mapRenderer.drawGrid(hoveredGrid, turnManager.getCurrentTurn());
        if (!gameOver && selectedUnit != null) {
            mapRenderer.drawRangeOverlays(selectedUnit, units);
        }
        drawUnits();
        shape.end();

        game.batch.begin();
        // game.font를 사용하여 그리기 (BattleScreen의 지역 변수 font 제거)
        game.font.setColor(Color.WHITE);
        game.font.draw(game.batch, "현재 턴: " + turnManager.getCurrentTurn(), 20, Gdx.graphics.getHeight() - 20);
        game.font.draw(game.batch, "당신의 진영: " + playerTeam, 20, Gdx.graphics.getHeight() - 50);

        if (selectedUnit != null) drawUnitCard();
        if (gameOver) drawVictoryMessage();
        game.batch.end();
    }

    private void drawUnits() {
        for (int i = 0; i < units.size; i++) {
            Unit unit = units.get(i);
            if (unit.currentHp <= 0) continue;

            Vector2 pos = IsoUtils.gridToScreen(unit.gridX, unit.gridY);
            if (unit == selectedUnit) {
                shape.setColor(Color.YELLOW);
                shape.rect(pos.x - 7, pos.y - 2, 14, 24);
            } else {
                shape.setColor(unit.team.equals("HADES") ? Color.BLUE : Color.RED);
                shape.rect(pos.x - 5, pos.y, 10, 20);
            }
            drawHpBar(pos, unit);
        }
    }

    private void drawHpBar(Vector2 pos, Unit unit) {
        float bW = 20f, bH = 3f;
        shape.setColor(Color.BLACK);
        shape.rect(pos.x - 10, pos.y + 25, bW, bH);
        float hpR = (float) unit.currentHp / unit.stat.hp();
        if (hpR > 0) {
            shape.setColor(Color.GREEN);
            shape.rect(pos.x - 10, pos.y + 25, bW * hpR, bH);
        }
    }

    private void drawUnitCard() {
        float x = 20, y = 120;
        game.font.setColor(Color.GOLD);
        game.font.draw(game.batch, "[ " + selectedUnit.name + " ]", x, y);
        game.font.setColor(Color.WHITE);
        game.font.draw(game.batch, "체력: " + selectedUnit.currentHp + " / " + selectedUnit.stat.hp(), x, y - 25);
        game.font.draw(game.batch, "공격력: " + selectedUnit.stat.atk(), x, y - 50);
        game.font.draw(game.batch, "스킬: " + selectedUnit.stat.skillName(), x, y - 75);
    }

    private void drawVictoryMessage() {
        game.batch.end();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0, 0, 0, 0.5f);
        shape.rect(0, 300, Gdx.graphics.getWidth(), 200);
        shape.end();
        game.batch.begin();

        game.font.getData().setScale(3.0f);
        game.font.setColor(Color.YELLOW);
        game.font.draw(game.batch, "VICTORY: " + winner + " TEAM!", 150, 420);
        game.font.getData().setScale(1.0f);
    }

    @Override
    public void dispose() {
        if (shape != null) shape.dispose();
        // font.dispose()는 HadesGame에서 처리하므로 여기서 하지 않습니다.
    }
}
