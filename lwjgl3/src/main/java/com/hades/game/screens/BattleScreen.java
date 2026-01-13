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

// [클래스 역할] 전투 화면의 메인 루프를 담당하며, 유닛의 상태 업데이트와 시각적 렌더링을 통합 관리합니다.
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

    // 렌더러와 유닛 리스트, 턴 관리자를 초기화합니다.
    private void init() {
        shape = new ShapeRenderer();
        mapRenderer = new MapRenderer(shape);
        // UnitRenderer 생성 시 playerTeam 인자를 전달합니다.
        unitRenderer = new UnitRenderer(game.batch, shape, game.font, playerTeam);

        units = new Array<>();
        turnManager = new TurnManager();

        setupTeam("HADES", 0);
        setupTeam("ZEUS", GameConfig.BOARD_HEIGHT - 1);
    }

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
            // [수정] 내 턴이 오면 '이동 완료 차단'과 'AI 실행 차단'을 모두 해제합니다.
            aiBusy = false;
            aiDelay = 0;
            handleInput();
        } else {
            // AI 턴일 때
            aiDelay += delta;

            // [수정] 딜레이가 다 찼을 때만 aiBusy를 체크하여 실행합니다.
            // handleInput에서 이미 입력을 막고 있으므로 여기 조건은 단순화해도 안전합니다.
            if (aiDelay >= 1.0f) {
                // 아직 이번 AI 턴의 행동을 안 했다면 실행
                if (aiBusy) { // 플레이어가 이동 후 true로 만든 상태라면
                    try {
                        System.out.println("[AI 행동 시작]");
                        AILogic.processAITurn(units, aiTeam, turnManager, this);
                        // processAITurn 내부에서 turnManager.endTurn()이 호출되면
                        // 다음 프레임에서 위쪽 playerTeam 조건문으로 들어가 aiBusy가 false가 됩니다.
                    } catch (Exception e) {
                        System.err.println("[AI 오류] " + e.getMessage());
                        turnManager.endTurn();
                    }
                    aiBusy = false; // 행동 완료 후 플래그 리셋
                    aiDelay = 0;
                    selectedUnit = null;
                } else {
                    // 혹시 플래그가 꺼져있다면 켜줍니다 (안전장치)
                    aiBusy = true;
                }
            }
        }
    }

    private void handleInput() {
        // [보안강화] 내 턴이 아니거나 AI가 준비 중이면 절대 클릭 불가
        if (!turnManager.getCurrentTurn().equals(playerTeam) || aiBusy || gameOver) {
            return;
        }

        float mx = Gdx.input.getX();
        float my = Gdx.graphics.getHeight() - Gdx.input.getY();
        hoveredGrid = IsoUtils.screenToGrid(mx, my);

        if (Gdx.input.justTouched()) {
            // 격자 밖 클릭 방어
            if (hoveredGrid.x < 0 || hoveredGrid.y < 0) return;

            int tx = (int) hoveredGrid.x;
            int ty = (int) hoveredGrid.y;
            Unit clicked = BoardManager.getUnitAt(units, tx, ty);

            // 1. 내 유닛을 선택하는 경우
            if (clicked != null && clicked.isAlive() && clicked.team.equals(playerTeam)) {
                selectedUnit = clicked;
            }
            // 2. 이미 유닛을 선택한 상태에서 다른 곳을 클릭한 경우
            else if (selectedUnit != null) {
                if (BoardManager.canMoveTo(selectedUnit, tx, ty, units)) {
                    // --- 이동 프로세스 시작 ---
                    selectedUnit.setPosition(tx, ty);
                    processAutoAttack(playerTeam);

                    // [핵심] 이동 직후 모든 선택 정보와 플래그를 세팅하여 즉시 클릭을 차단합니다.
                    selectedUnit = null;
                    aiBusy = true;

                    turnManager.endTurn();
                    // --- 이동 프로세스 종료 ---
                } else {
                    // 이동할 수 없는 곳을 누르면 선택 해제
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

        // 현재 턴의 주인인지 확인하여 공격력 또는 반격력 결정
        int dmg = attacker.team.equals(turnManager.getCurrentTurn()) ? attacker.stat.atk() : attacker.stat.counterAtk();

        target.currentHp -= dmg;

        if (target.currentHp <= 0) {
            target.currentHp = 0;
            // [중요] handleDeath를 호출하기 전에 상태를 DEAD로 먼저 바꾸지 않고
            // 메서드 내부에서 처리하도록 흐름을 맞춥니다.
            handleDeath(attacker, target);
        }
    }

    private void handleDeath(Unit attacker, Unit target) {
        target.status = Unit.DEAD;

        // 왕(RULER)이 죽었을 때만 게임 종료 판정
        if ("왕의 위엄".equals(target.stat.skillName())) {
            gameOver = true;
            winner = attacker.team;
            System.out.println("!!! 게임 종료 !!! 승리 팀: " + winner);
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
            game.font.draw(game.batch, "현재 턴: " + turnManager.getCurrentTurn(), 20, Gdx.graphics.getHeight() - 20);
            game.font.draw(game.batch, "진영: " + playerTeam, 20, Gdx.graphics.getHeight() - 50);

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

        game.font.getData().setScale(1.0f);
        game.font.setColor(Color.YELLOW);
        game.font.draw(game.batch, "VICTORY: " + winner + " TEAM!", 150, 420);
        game.font.getData().setScale(1.0f);
    }

    @Override
    public void dispose() {
        if (shape != null) shape.dispose();
        if (unitRenderer != null) unitRenderer.dispose();
    }
}
