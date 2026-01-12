package com.hades.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.hades.game.constants.GameConfig;
import com.hades.game.constants.UnitData;
import com.hades.game.entities.Unit;
import com.hades.game.logic.AILogic;
import com.hades.game.logic.BoardManager;
import com.hades.game.logic.IsoUtils;
import com.hades.game.logic.TurnManager;

/**
 * HadesGame: 게임의 메인 루프를 담당하며 렌더링, 사용자 입력, 전투 및 승리 판정 등
 * 게임의 전반적인 흐름을 관리하는 핵심 클래스입니다.
 */
public class HadesGame extends ApplicationAdapter {

    private ShapeRenderer shape;
    private SpriteBatch batch;
    private BitmapFont font;
    private Array<Unit> units;
    private Vector2 hoveredGrid = new Vector2(-1, -1);
    private Unit selectedUnit = null;
    private TurnManager turnManager;
    private float aiDelayTimer = 0; // AI 행동 대기 타이머
    private boolean isAIProcessing = false; // AI가 행동 중인지 체크
    private boolean gameOver = false;
    private String winner = "";

    @Override
    public void create() {
        shape = new ShapeRenderer();
        batch = new SpriteBatch();

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/malgun.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 20;
        parameter.color = Color.WHITE;

        StringBuilder sb = new StringBuilder();
        sb.append(FreeTypeFontGenerator.DEFAULT_CHARS);
        for (char c = 0xAC00; c <= 0xD7A3; c++) sb.append(c);
        for (char c = 0x3131; c <= 0x3163; c++) sb.append(c);
        parameter.characters = sb.toString();

        font = generator.generateFont(parameter);
        generator.dispose();

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
    public void render() {
        // 게임이 끝나지 않았을 때 턴에 따른 로직 수행
        if (!gameOver) {
            if (turnManager.getCurrentTurn().equals("HADES")) {
                // 플레이어 턴이 돌아오면 AI 관련 상태를 초기화합니다.
                isAIProcessing = false;
                aiDelayTimer = 0;
                updateInput();
            } else {
                // AI 턴일 때 1초 대기 후 실행 (중복 실행 방지)
                aiDelayTimer += Gdx.graphics.getDeltaTime();
                if (aiDelayTimer >= 1.0f && !isAIProcessing) {
                    updateTurn();
                }
            } // <- 누락되었던 if-else 블록의 닫는 괄호 추가
        }

        // --- 렌더링 시작 ---
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        shape.begin(ShapeRenderer.ShapeType.Line);
        drawGrid();

        if (!gameOver && selectedUnit != null) {
            drawRangeOverlays(selectedUnit);
        }

        drawUnits();
        shape.end();

        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "현재 턴: " + turnManager.getCurrentTurn(), 20, Gdx.graphics.getHeight() - 20);

        if (selectedUnit != null) {
            drawUnitCard();
        }

        if (gameOver) {
            // 반투명한 검은 배경 (선택 사항)
            batch.end();
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shape.begin(ShapeRenderer.ShapeType.Filled);
            shape.setColor(0, 0, 0, 0.5f); // 50% 투명도
            shape.rect(0, 300, Gdx.graphics.getWidth(), 200);
            shape.end();
            batch.begin();

            // 승리 문구
            font.getData().setScale(3.0f); // 글자 크기 3배
            font.setColor(Color.YELLOW);
            String resultText = "VICTORY: " + winner + " TEAM!";
            font.draw(batch, resultText, 150, 420);
            font.getData().setScale(1.0f); // 다시 원래대로 복구
        }
        batch.end();
    }

    private void drawUnitCard() {
        float cardX = 20;
        float cardY = 120;
        font.setColor(Color.GOLD);
        font.draw(batch, "[ " + selectedUnit.name + " ]", cardX, cardY);
        font.setColor(Color.WHITE);
        font.draw(batch, "체력: " + selectedUnit.currentHp + " / " + selectedUnit.stat.hp(), cardX, cardY - 25);
        font.draw(batch, "공격력: " + selectedUnit.stat.atk(), cardX, cardY - 50);
        font.draw(batch, "스킬: " + selectedUnit.stat.skillName(), cardX, cardY - 75);
    }

    /**
     * [수정] 사용자의 마우스 입력을 처리합니다.
     * 이제 클릭 시 공격하지 않고 오직 '이동'만 수행합니다.
     */
    private void updateInput() {
        float mx = Gdx.input.getX();
        float my = Gdx.graphics.getHeight() - Gdx.input.getY();
        hoveredGrid = IsoUtils.screenToGrid(mx, my);

        if (Gdx.input.justTouched()) {
            int tx = (int) hoveredGrid.x;
            int ty = (int) hoveredGrid.y;

            Unit clickedUnit = BoardManager.getUnitAt(units, tx, ty);

            // 유닛 선택: 내 턴의 유닛만 선택 가능
            if (clickedUnit != null && turnManager.isMyTurn(clickedUnit.team)) {
                selectedUnit = clickedUnit;
            }
            // 이동 처리: 빈 공간 클릭 시 이동 후 즉시 자동 공격 및 턴 종료
            else if (selectedUnit != null) {
                if (BoardManager.canMoveTo(selectedUnit, tx, ty, units)) {
                    selectedUnit.setPosition(tx, ty);

                    // [핵심] 이동 직후 해당 팀의 모든 유닛이 자동 공격 실행
                    processAutoAttack(turnManager.getCurrentTurn());

                    selectedUnit = null;
                    turnManager.endTurn();
                } else {
                    selectedUnit = null;
                }
            }
        }
    }

    /**
     * [신규] 특정 팀의 모든 유닛이 사거리 내 적을 1회 자동 공격합니다.
     */
    private void processAutoAttack(String team) {
        System.out.println("=== [" + team + "] 진영 자동 협공 개시 ===");

        // 리스트 변동에 안전하도록 인덱스 기반 순회
        for (int i = 0; i < units.size; i++) {
            Unit attacker = units.get(i);
            if (attacker.team.equals(team) && attacker.currentHp > 0) {
                Unit target = BoardManager.findBestTargetInRange(attacker, units);
                if (target != null) {
                    performAttack(attacker, target);
                }
            }
        }
    }

    private void processPathAttack(Unit actor, int tx, int ty) {
        int startX = Math.min(actor.gridX, tx);
        int endX = Math.max(actor.gridX, tx);
        int startY = Math.min(actor.gridY, ty);
        int endY = Math.max(actor.gridY, ty);

        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                Unit target = BoardManager.getUnitAt(units, x, y);
                if (target != null && !target.team.equals(actor.team)) {
                    performAttack(actor, target);
                }
            }
        }
    }

    /**
     * [메서드 설명]
     * 공격자가 대상을 공격하여 체력을 차감하고, 사망 시 리스트에서 제거합니다.
     */
    private void performAttack(Unit attacker, Unit target) {
        if (attacker == null || target == null) return;

        int damage = attacker.stat.atk();
        target.currentHp -= damage;

        // [보정] 체력이 0보다 작아지면 0으로 표시
        int displayHp = Math.max(0, target.currentHp);

        System.out.println("[전투] " + attacker.name + "가 " + target.name + "을 공격! (데미지: " + damage + ", 남은 체력: " + displayHp + ")");

        if (target.currentHp <= 0) {
            if ("왕의 위엄".equals(target.stat.skillName())) {
                gameOver = true;
                winner = attacker.team;
                System.out.println("!!! " + winner + " 팀이 승리하였습니다 !!!");
            }
            System.out.println("[처치] " + target.name + " 전사.");
            units.removeValue(target, true);
        }
    }

    private void drawGrid() {
        for (int x = 0; x < GameConfig.BOARD_WIDTH; x++) {
            for (int y = 0; y < GameConfig.BOARD_HEIGHT; y++) {
                // 현재 턴에 따라 바닥 색상 변경 (시각적 피드백)
                Color color = turnManager.getCurrentTurn().equals("HADES") ?
                    new Color(0, 0, 0.3f, 1) : new Color(0.3f, 0, 0, 1);
                if (x == (int) hoveredGrid.x && y == (int) hoveredGrid.y) color = Color.WHITE;
                drawIsoTile(x, y, color);
            }
        }
    }

    private void drawIsoTile(int x, int y, Color color) {
        Vector2 pos = IsoUtils.gridToScreen(x, y);
        shape.setColor(color);
        float hw = GameConfig.TILE_WIDTH / 2;
        float hh = GameConfig.TILE_HEIGHT / 2;
        shape.line(pos.x, pos.y + hh, pos.x - hw, pos.y);
        shape.line(pos.x - hw, pos.y, pos.x, pos.y - hh);
        shape.line(pos.x, pos.y - hh, pos.x + hw, pos.y);
        shape.line(pos.x + hw, pos.y, pos.x, pos.y + hh);
    }

    private void drawUnits() {
        for (Unit unit : units) {
            Vector2 pos = IsoUtils.gridToScreen(unit.gridX, unit.gridY);
            if (unit == selectedUnit) {
                shape.setColor(Color.YELLOW);
                shape.rect(pos.x - 7, pos.y - 2, 14, 24);
            } else {
                shape.setColor(unit.team.equals("HADES") ? Color.BLUE : Color.RED);
                shape.rect(pos.x - 5, pos.y, 10, 20);
            }
            float barWidth = 20f;
            float barHeight = 3f;
            shape.setColor(Color.BLACK);
            shape.rect(pos.x - 10, pos.y + 25, barWidth, barHeight);
            float hpRatio = (float) unit.currentHp / unit.stat.hp();
            if (hpRatio > 0) {
                shape.setColor(Color.GREEN);
                shape.rect(pos.x - 10, pos.y + 25, barWidth * hpRatio, barHeight);
            }
        }
    }

    private void drawRangeOverlays(Unit unit) {
        for (int x = 0; x < GameConfig.BOARD_WIDTH; x++) {
            for (int y = 0; y < GameConfig.BOARD_HEIGHT; y++) {
                int dist = Math.abs(unit.gridX - x) + Math.abs(unit.gridY - y);
                if (dist <= unit.stat.range()) {
                    drawIsoTile(x, y, Color.RED);
                } else if (BoardManager.canMoveTo(unit, x, y, units)) {
                    drawIsoTile(x, y, Color.CYAN);
                }
            }
        }
    }

    /**
     * AI의 턴 로직을 실행합니다.
     * 중복 실행을 방지하기 위해 isAIProcessing 플래그를 사용합니다.
     */
    private void updateTurn() {
        if (turnManager.getCurrentTurn().equals("ZEUS") && !isAIProcessing) {
            isAIProcessing = true; // AI 행동 시작 알림

            System.out.println("AI(ZEUS)가 전략을 생각하고 움직입니다.");
            AILogic.processAITurn(units, "ZEUS", turnManager);

            selectedUnit = null; // AI 행동 후 선택된 유닛 강조 표시 해제
        }
    }

    @Override
    public void dispose() {
        shape.dispose();
        batch.dispose();
        font.dispose();
    }
}
