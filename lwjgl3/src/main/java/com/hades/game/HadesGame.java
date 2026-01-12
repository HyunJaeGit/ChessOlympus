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

/* HadesGame: 게임의 메인 루프를 담당하며 렌더링, 사용자 입력, 전투 및 승리 판정 등
   게임의 전반적인 흐름을 관리하는 핵심 클래스입니다.
*/
public class HadesGame extends ApplicationAdapter {

    private ShapeRenderer shape;
    private SpriteBatch batch; // 텍스트와 이미지를 그리기 위한 도구
    private BitmapFont font; // 화면에 글자를 표시하기 위한 도구
    private Array<Unit> units;
    private Vector2 hoveredGrid = new Vector2(-1, -1);
    private Unit selectedUnit = null;
    private TurnManager turnManager;

    private boolean gameOver = false; // 게임 종료 상태 관리
    private String winner = ""; // 승리한 팀 이름

    @Override
    public void create() {
        shape = new ShapeRenderer();
        batch = new SpriteBatch();

        /* 폰트 생성기 초기화 */
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/malgun.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();

        parameter.size = 20;
        parameter.color = Color.WHITE;

        /* [핵심 수정] 한글 범위를 유니코드로 생성하여 전달 */
        StringBuilder sb = new StringBuilder();
        sb.append(FreeTypeFontGenerator.DEFAULT_CHARS); // 기본 영문/숫자
        for (char c = 0xAC00; c <= 0xD7A3; c++) sb.append(c); // 가 ~ 힣
        for (char c = 0x3131; c <= 0x3163; c++) sb.append(c); // ㄱ ~ ㅣ

        parameter.characters = sb.toString();

        font = generator.generateFont(parameter);
        generator.dispose();

        units = new Array<>();
        turnManager = new TurnManager();

        setupTeam("HADES", 0);
        setupTeam("ZEUS", GameConfig.BOARD_HEIGHT - 1);
    }

    /* 지정된 행에 진영별 유닛들을 생성하고 초기 위치에 배치합니다. */
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
        if (!gameOver) {
            updateInput();
        }

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

        /* 상단 턴 정보 한글 표시 */
        font.setColor(Color.WHITE);
        font.draw(batch, "현재 턴: " + turnManager.getCurrentTurn(), 20, Gdx.graphics.getHeight() - 20);

        if (selectedUnit != null) {
            drawUnitCard();
        }

        /* 게임 종료 메시지 한글 표시 */
        if (gameOver) {
            font.setColor(Color.YELLOW);
            font.draw(batch, "게임 종료! 승리팀: " + winner, 300, 400);
        }

        batch.end();
    }

    /* 선택된 유닛의 상세 정보(이름, 스탯, 스킬)를 화면 하단에 표시함 */
    private void drawUnitCard() {
        float cardX = 20;
        float cardY = 120;

        font.setColor(Color.GOLD);
        font.draw(batch, "[ " + selectedUnit.name + " ]", cardX, cardY);

        font.setColor(Color.WHITE);
        // "HP", "ATK", "SKILL"을 "체력", "공격력", "스킬"로 변경
        font.draw(batch, "체력: " + selectedUnit.currentHp + " / " + selectedUnit.stat.hp(), cardX, cardY - 25);
        font.draw(batch, "공격력: " + selectedUnit.stat.atk(), cardX, cardY - 50);
        font.draw(batch, "스킬: " + selectedUnit.stat.skillName(), cardX, cardY - 75);
    }

    /* 마우스 입력 좌표를 격자 좌표로 변환하고 클릭 이벤트를 처리합니다. */
    private void updateInput() {
        float mx = Gdx.input.getX();
        float my = Gdx.graphics.getHeight() - Gdx.input.getY();
        hoveredGrid = IsoUtils.screenToGrid(mx, my);

        if (Gdx.input.justTouched()) {
            int tx = (int) hoveredGrid.x;
            int ty = (int) hoveredGrid.y;

            Unit clickedUnit = BoardManager.getUnitAt(units, tx, ty);

            if (clickedUnit != null) {
                if (selectedUnit != null && !selectedUnit.team.equals(clickedUnit.team)) {
                    if (BoardManager.canAttack(selectedUnit, clickedUnit)) {
                        performAttack(selectedUnit, clickedUnit);
                        selectedUnit = null;
                        turnManager.endTurn();
                    }
                } else {
                    if (turnManager.isMyTurn(clickedUnit.team)) {
                        selectedUnit = clickedUnit;
                    }
                }
            }
            else if (selectedUnit != null) {
                if (BoardManager.canMoveTo(selectedUnit, tx, ty, units)) {
                    if (selectedUnit.stat.skillName().equals("유연한 발걸음")) {
                        processPathAttack(selectedUnit, tx, ty);
                    }
                    selectedUnit.setPosition(tx, ty);
                    selectedUnit = null;
                    turnManager.endTurn();
                } else {
                    selectedUnit = null;
                }
            }
        }
    }

    /* 알케미스트 전용 스킬: 이동 경로상의 적을 공격함 */
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

    /* 대상 유닛의 체력을 차감하고 승리를 판정함 */
    private void performAttack(Unit attacker, Unit target) {
        target.currentHp -= attacker.stat.atk();
        if (target.currentHp <= 0) {
            if (target.stat.skillName().equals("왕의 위엄")) {
                gameOver = true;
                winner = attacker.team;
            }
            units.removeValue(target, true);
        }
    }

    /* 격자판 렌더링 */
    private void drawGrid() {
        for (int x = 0; x < GameConfig.BOARD_WIDTH; x++) {
            for (int y = 0; y < GameConfig.BOARD_HEIGHT; y++) {
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

    /* 유닛 및 체력 바 렌더링 */
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

    /* 이동 및 공격 범위 가이드 표시 */
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

    /* HadesGame 클래스 내부의 턴 처리 부분 */
    private void updateTurn() {
        if (turnManager.getCurrentTurn().equals("ZEUS")) {
            // AI 로직 클래스에 모든 판단을 맡깁니다.
            AILogic.processAITurn(units, "ZEUS");

            // 행동이 끝났으므로 턴을 넘깁니다.
            turnManager.endTurn();
        }
    }

    @Override
    public void dispose() {
        shape.dispose();
        batch.dispose();
        font.dispose(); // 폰트 메모리 해제
    }
}
