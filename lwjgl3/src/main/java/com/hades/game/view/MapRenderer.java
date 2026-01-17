package com.hades.game.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.hades.game.constants.GameConfig;
import com.hades.game.entities.Unit;
import com.hades.game.logic.BoardManager;
import com.hades.game.logic.IsoUtils;

// 입체적인 유적 타일 및 범위 가이드 렌더링 담당
public class MapRenderer {
    private final ShapeRenderer shape;
    private final SpriteBatch batch;
    private final Texture tileTop;

    private static final int TILE_DEPTH = 12;
    private static final float TILE_PADDING = 22f;

    public MapRenderer(ShapeRenderer shape, SpriteBatch batch, Texture tileTop) {
        this.shape = shape;
        this.batch = batch;
        this.tileTop = tileTop;
    }

    // 타일 렌더링 로직
    public void drawTiles(Vector2 hoveredGrid, Unit selectedUnit, Array<Unit> units) {
        batch.begin();
        float drawW = GameConfig.TILE_WIDTH - TILE_PADDING;
        float drawH = GameConfig.TILE_HEIGHT - (TILE_PADDING / 2f);

        for (int y = 0; y < GameConfig.BOARD_HEIGHT; y++) {
            for (int x = 0; x < GameConfig.BOARD_WIDTH; x++) {
                Vector2 pos = IsoUtils.gridToScreen(x, y);

                // 1. 타일 옆면 (입체감 효과)
                for (int i = TILE_DEPTH; i > 0; i--) {
                    float b = 0.2f + (0.2f * (1.0f - (float)i / TILE_DEPTH));
                    batch.setColor(b, b, b, 1.0f);
                    batch.draw(tileTop, pos.x - drawW / 2f, pos.y - drawH / 2f - i, drawW, drawH);
                }

                // 2. 타일 윗면 색상
                Color tileColor = new Color(0.9f, 0.9f, 0.9f, 1f);

                // 이동 가능 범위 표시
                if (selectedUnit != null && BoardManager.canMoveTo(selectedUnit, x, y, units)) {
                    tileColor = new Color(0.2f, 0.5f, 0.7f, 0.6f);
                }

                // 마우스 오버 효과
                if (x == (int) hoveredGrid.x && y == (int) hoveredGrid.y) {
                    tileColor = tileColor.cpy().add(0.2f, 0.2f, 0.2f, 0);
                }

                batch.setColor(tileColor);
                batch.draw(tileTop, pos.x - drawW / 2f, pos.y - drawH / 2f, drawW, drawH);
            }
        }
        batch.setColor(Color.WHITE);
        batch.end();
    }

    // 기본 공격 사거리 표시
    public void drawRangeOverlays(Unit unit) {
        if (unit == null) return;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glLineWidth(1.2f);
        shape.begin(ShapeRenderer.ShapeType.Line);

        for (int x = 0; x < GameConfig.BOARD_WIDTH; x++) {
            for (int y = 0; y < GameConfig.BOARD_HEIGHT; y++) {
                if (x == unit.gridX && y == unit.gridY) continue;

                Vector2 pos = IsoUtils.gridToScreen(x, y);
                int dx = Math.abs(unit.gridX - x);
                int dy = Math.abs(unit.gridY - y);
                int dist = dx + dy;

                boolean canAttackTile = false;
                if (unit.unitClass == Unit.UnitClass.ARCHER) {
                    if ((dx == 0 || dy == 0) && dist <= unit.stat.range()) canAttackTile = true;
                } else if (unit.unitClass == Unit.UnitClass.KNIGHT) {
                    if (dx <= 1 && dy <= 1) canAttackTile = true;
                } else {
                    if (dist <= unit.stat.range()) canAttackTile = true;
                }

                if (canAttackTile) {
                    shape.setColor(new Color(0.6f, 0.1f, 0.1f, 0.7f));
                    drawIsoShape(pos.x, pos.y);
                }
            }
        }
        shape.end();
        Gdx.gl.glLineWidth(1f);
    }

    // [신규] 스킬 장전 시 주황색 사거리 가이드 표시
    public void drawSkillRange(Unit unit, int skillRange) {
        if (unit == null) return;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glLineWidth(2.0f); // 스킬 범위는 더 굵게 강조
        shape.begin(ShapeRenderer.ShapeType.Line);

        // 권능 상태를 나타내는 선명한 주황색
        shape.setColor(new Color(1.0f, 0.65f, 0.0f, 0.9f));

        for (int x = 0; x < GameConfig.BOARD_WIDTH; x++) {
            for (int y = 0; y < GameConfig.BOARD_HEIGHT; y++) {
                int dist = Math.abs(unit.gridX - x) + Math.abs(unit.gridY - y);

                // 유닛 자신을 제외한 사거리 내 타일들에 가이드 출력
                if (dist > 0 && dist <= skillRange) {
                    Vector2 pos = IsoUtils.gridToScreen(x, y);
                    drawIsoShape(pos.x, pos.y);
                }
            }
        }
        shape.end();
        Gdx.gl.glLineWidth(1f);
    }

    // 마름모 형태 그리기 공통 메서드
    private void drawIsoShape(float cx, float cy) {
        float hw = GameConfig.TILE_WIDTH / 2f - 2f;
        float hh = GameConfig.TILE_HEIGHT / 2f - 1f;

        shape.line(cx, cy + hh, cx - hw, cy);
        shape.line(cx - hw, cy, cx, cy - hh);
        shape.line(cx, cy - hh, cx + hw, cy);
        shape.line(cx + hw, cy, cx, cy + hh);
    }
}
