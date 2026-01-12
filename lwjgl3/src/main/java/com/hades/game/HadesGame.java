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
import com.hades.game.view.MapRenderer;

public class HadesGame extends ApplicationAdapter {

    private ShapeRenderer shape;
    private SpriteBatch batch;
    private BitmapFont font;
    private Array<Unit> units;
    private Vector2 hoveredGrid = new Vector2(-1, -1);
    private Unit selectedUnit = null;
    private TurnManager turnManager;
    private MapRenderer mapRenderer; // 맵 렌더링 전담 객체
    private float aiDelayTimer = 0;
    private boolean isAIProcessing = false;
    private boolean gameOver = false;
    private String winner = "";

    @Override
    public void create() {
        shape = new ShapeRenderer();
        batch = new SpriteBatch();
        mapRenderer = new MapRenderer(shape); // 렌더러 초기화

        // 폰트 설정 (한글 지원)
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
        // 게임 상태 업데이트
        if (!gameOver) {
            if (turnManager.getCurrentTurn().equals("HADES")) {
                isAIProcessing = false;
                aiDelayTimer = 0;
                updateInput();
            } else {
                aiDelayTimer += Gdx.graphics.getDeltaTime();
                if (aiDelayTimer >= 1.0f && !isAIProcessing) {
                    updateTurn();
                }
            }
        }

        // 렌더링 시작
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        shape.begin(ShapeRenderer.ShapeType.Line);
        // MapRenderer에게 격자 그리기를 맡김
        mapRenderer.drawGrid(hoveredGrid, turnManager.getCurrentTurn());

        if (!gameOver && selectedUnit != null) {
            mapRenderer.drawRangeOverlays(selectedUnit, units);
        }

        drawUnits(); // 유닛 본체 및 체력바 렌더링
        shape.end();

        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "현재 턴: " + turnManager.getCurrentTurn(), 20, Gdx.graphics.getHeight() - 20);

        if (selectedUnit != null) drawUnitCard();
        if (gameOver) drawVictoryMessage();
        batch.end();
    }

    private void drawUnitCard() {
        float cardX = 20, cardY = 120;
        font.setColor(Color.GOLD);
        font.draw(batch, "[ " + selectedUnit.name + " ]", cardX, cardY);
        font.setColor(Color.WHITE);
        font.draw(batch, "체력: " + selectedUnit.currentHp + " / " + selectedUnit.stat.hp(), cardX, cardY - 25);
        font.draw(batch, "공격력: " + selectedUnit.stat.atk(), cardX, cardY - 50);
        font.draw(batch, "반격력: " + selectedUnit.stat.counterAtk(), cardX, cardY - 75);
        font.draw(batch, "스킬: " + selectedUnit.stat.skillName(), cardX, cardY - 100);
    }

    private void updateInput() {
        float mx = Gdx.input.getX();
        float my = Gdx.graphics.getHeight() - Gdx.input.getY();
        hoveredGrid = IsoUtils.screenToGrid(mx, my);

        if (Gdx.input.justTouched()) {
            int tx = (int) hoveredGrid.x;
            int ty = (int) hoveredGrid.y;
            Unit clickedUnit = BoardManager.getUnitAt(units, tx, ty);

            if (clickedUnit != null && turnManager.isMyTurn(clickedUnit.team)) {
                selectedUnit = clickedUnit;
            } else if (selectedUnit != null) {
                if (BoardManager.canMoveTo(selectedUnit, tx, ty, units)) {
                    selectedUnit.setPosition(tx, ty);
                    processAutoAttack(turnManager.getCurrentTurn());
                    selectedUnit = null;
                    turnManager.endTurn();
                } else {
                    selectedUnit = null;
                }
            }
        }
    }

    /* [메서드 설명] 특정 팀의 모든 유닛이 자동 공격을 수행합니다. */
    public void processAutoAttack(String team) {
        System.out.println("=== [" + team + "] 진영 자동 협공 ===");
        for (int i = 0; i < units.size; i++) {
            Unit attacker = units.get(i);
            if (attacker.team.equals(team) && attacker.currentHp > 0) {
                Unit target = BoardManager.findBestTargetInRange(attacker, units);
                if (target != null) performAttack(attacker, target);
            }
        }
    }

    /* [메서드 설명] 전투 데미지를 계산하고 사망을 판정하는 중앙 제어 메서드입니다. */
    public void performAttack(Unit attacker, Unit target) {
        if (attacker == null || target == null || target.currentHp <= 0) return;

        String currentTurn = turnManager.getCurrentTurn();
        int damage = attacker.team.equals(currentTurn) ? attacker.stat.atk() : attacker.stat.counterAtk();

        target.currentHp -= damage;
        int displayHp = Math.max(0, target.currentHp);
        String type = attacker.team.equals(currentTurn) ? "[주공격]" : "[협공/반격]";
        System.out.println(type + " " + attacker.name + " -> " + target.name + " (피해: " + damage + ", 남은 HP: " + displayHp + ")");

        if (target.currentHp <= 0) {
            handleDeath(attacker, target);
        }
    }

    private void handleDeath(Unit attacker, Unit target) {
        if ("왕의 위엄".equals(target.stat.skillName())) {
            gameOver = true;
            winner = attacker.team;
        }
        System.out.println("[전사] " + target.name);
        units.removeValue(target, true);
    }

    private void updateTurn() {
        if (turnManager.getCurrentTurn().equals("ZEUS") && !isAIProcessing) {
            isAIProcessing = true;
            AILogic.processAITurn(units, "ZEUS", turnManager, this);
            selectedUnit = null;
        }
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
            // 체력 바 그리기
            drawHpBar(pos, unit);
        }
    }

    private void drawHpBar(Vector2 pos, Unit unit) {
        float barWidth = 20f, barHeight = 3f;
        shape.setColor(Color.BLACK);
        shape.rect(pos.x - 10, pos.y + 25, barWidth, barHeight);
        float hpRatio = (float) unit.currentHp / unit.stat.hp();
        if (hpRatio > 0) {
            shape.setColor(Color.GREEN);
            shape.rect(pos.x - 10, pos.y + 25, barWidth * hpRatio, barHeight);
        }
    }

    private void drawVictoryMessage() {
        batch.end();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0, 0, 0, 0.5f);
        shape.rect(0, 300, Gdx.graphics.getWidth(), 200);
        shape.end();
        batch.begin();

        font.getData().setScale(3.0f);
        font.setColor(Color.YELLOW);
        font.draw(batch, "VICTORY: " + winner + " TEAM!", 150, 420);
        font.getData().setScale(1.0f);
    }

    @Override
    public void dispose() {
        shape.dispose();
        batch.dispose();
        font.dispose();
    }
}
