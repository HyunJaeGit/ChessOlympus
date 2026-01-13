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
 * 전투 화면의 모든 로직과 유닛 수명 주기를 관리하는 메인 스크린 클래스입니다.
 */
public class BattleScreen extends ScreenAdapter {
    private final HadesGame game;
    private ShapeRenderer shape;
    private Array<Unit> units;
    private Vector2 hoveredGrid = new Vector2(-1, -1);
    private Unit selectedUnit = null;
    private TurnManager turnManager;
    private MapRenderer mapRenderer;

    private String playerTeam;
    private String aiTeam;
    private float aiDelay = 0;
    private boolean aiBusy = false;
    private boolean gameOver = false;
    private String winner = "";

    public BattleScreen(HadesGame game, String playerTeam) {
        this.game = game;
        this.playerTeam = playerTeam;
        this.aiTeam = playerTeam.equals("HADES") ? "ZEUS" : "HADES";
        init();
    }

    // 초기 자원 설정 및 팀 배치 수행
    private void init() {
        shape = new ShapeRenderer();
        mapRenderer = new MapRenderer(shape);
        units = new Array<>();
        turnManager = new TurnManager();

        setupTeam("HADES", 0);
        setupTeam("ZEUS", GameConfig.BOARD_HEIGHT - 1);
    }

    // 지정된 진영의 유닛들을 시작 지점에 소환
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

        // DEAD 상태인 유닛들을 일괄 제거하여 메모리 및 참조 안전성 확보
        cleanupDeadUnits();

        draw();
    }

    // 사망 처리된 유닛들을 실제 배열에서 제거 (역순 순회)
    private void cleanupDeadUnits() {
        for (int i = units.size - 1; i >= 0; i--) {
            Unit u = units.get(i);
            if (u.status == Unit.DEAD) {
                if (selectedUnit == u) selectedUnit = null;
                units.removeIndex(i);
            }
        }
    }

    // 게임 로직 업데이트 및 턴제 상태 제어
    private void update(float delta) {
        if (gameOver) return;

        String currentTurn = turnManager.getCurrentTurn();

        if (currentTurn.equals(playerTeam)) {
            aiBusy = false;
            aiDelay = 0;
            handleInput();
        } else {
            aiDelay += delta;
            if (aiDelay >= 1.0f && !aiBusy) {
                aiBusy = true;
                try {
                    AILogic.processAITurn(units, aiTeam, turnManager, this);
                    aiDelay = 0;
                } catch (Exception e) {
                    System.err.println("[AI 오류] " + e.getMessage());
                    turnManager.endTurn();
                }
                selectedUnit = null;
            }
        }
    }

    // 마우스 좌표를 격자로 변환하여 유닛 선택 및 이동 처리
    private void handleInput() {
        if (!turnManager.isMyTurn(playerTeam) || aiBusy) return;

        float mx = Gdx.input.getX();
        float my = Gdx.graphics.getHeight() - Gdx.input.getY();
        hoveredGrid = IsoUtils.screenToGrid(mx, my);

        if (Gdx.input.justTouched()) {
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
                    turnManager.endTurn();
                } else {
                    selectedUnit = null;
                }
            }
        }
    }

    // 해당 팀 유닛들의 자동 공격 범위를 확인하여 공격 실행
    public void processAutoAttack(String team) {
        // units.size를 직접 참조하는 인덱스 루프가 가장 안전합니다.
        for (int i = 0; i < units.size; i++) {
            Unit attacker = units.get(i);
            if (attacker != null && attacker.isAlive() && attacker.team.equals(team)) {
                Unit target = BoardManager.findBestTargetInRange(attacker, units);
                if (target != null) performAttack(attacker, target);
            }
        }
    }

    // 유닛 간의 대미지 계산 및 사망 상태 트리거
    public void performAttack(Unit attacker, Unit target) {
        if (target == null || !target.isAlive()) return;

        String turn = turnManager.getCurrentTurn();
        int dmg = attacker.team.equals(turn) ? attacker.stat.atk() : attacker.stat.counterAtk();

        target.currentHp -= dmg;

        if (target.currentHp <= 0) {
            target.currentHp = 0;
            handleDeath(attacker, target);
        }
    }

    // 유닛 사망 시 DEAD 표식을 남기고 승리 조건 검사
    private void handleDeath(Unit attacker, Unit target) {
        target.status = Unit.DEAD;

        if ("왕의 위엄".equals(target.stat.skillName())) {
            gameOver = true;
            winner = attacker.team;
        }
        System.out.println("[사망] " + target.team + " - " + target.name);
    }

    // 화면 지우기 및 레이어 순서대로 렌더링 호출
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
        game.font.setColor(Color.WHITE);
        game.font.draw(game.batch, "현재 턴: " + turnManager.getCurrentTurn(), 20, Gdx.graphics.getHeight() - 20);
        game.font.draw(game.batch, "진영: " + playerTeam, 20, Gdx.graphics.getHeight() - 50);

        if (selectedUnit != null) drawUnitCard();
        if (gameOver) drawVictoryMessage();
        game.batch.end();
    }

    // 살아있는 모든 유닛을 화면에 표시
    private void drawUnits() {
        for (Unit unit : units) {
            if (!unit.isAlive()) continue;

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

    // 유닛 머리 위에 체력 바 표시
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

    // 선택된 유닛의 상세 스탯 카드 UI 표시
    private void drawUnitCard() {
        float x = 20, y = 120;
        game.font.setColor(Color.GOLD);
        game.font.draw(game.batch, "[ " + selectedUnit.name + " ]", x, y);
        game.font.setColor(Color.WHITE);
        game.font.draw(game.batch, "체력: " + selectedUnit.currentHp + " / " + selectedUnit.stat.hp(), x, y - 25);
        game.font.draw(game.batch, "공격력: " + selectedUnit.stat.atk(), x, y - 50);
        game.font.draw(game.batch, "스킬: " + selectedUnit.stat.skillName(), x, y - 75);
    }

    // 게임 종료 시 중앙에 승리 팀 메시지 출력
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
    }
}
