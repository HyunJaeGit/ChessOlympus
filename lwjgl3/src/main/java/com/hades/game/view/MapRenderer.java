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

    public void drawTiles(Vector2 hoveredGrid, Unit selectedUnit, Array<Unit> units) {
        batch.begin();
        float drawW = GameConfig.TILE_WIDTH - TILE_PADDING;
        float drawH = GameConfig.TILE_HEIGHT - (TILE_PADDING / 2f);

        for (int y = 0; y < GameConfig.BOARD_HEIGHT; y++) {
            for (int x = 0; x < GameConfig.BOARD_WIDTH; x++) {
                Vector2 pos = IsoUtils.gridToScreen(x, y);

                // 1. 타일 옆면 (어둡게 처리하여 입체감 강조)
                for (int i = TILE_DEPTH; i > 0; i--) {
                    float b = 0.2f + (0.2f * (1.0f - (float)i / TILE_DEPTH));
                    batch.setColor(b, b, b, 1.0f);
                    batch.draw(tileTop, pos.x - drawW / 2f, pos.y - drawH / 2f - i, drawW, drawH);
                }

                // 2. 타일 윗면 색상 설정
                Color tileColor = new Color(0.9f, 0.9f, 0.9f, 1f); // 기본 타일 색상

                // 이동 범위: 세련된 다크 시안 (투명도 활용)
                if (selectedUnit != null && BoardManager.canMoveTo(selectedUnit, x, y, units)) {
                    tileColor = new Color(0.2f, 0.5f, 0.7f, 0.6f);
                }

                // 마우스 오버: 살짝 밝아지는 효과
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
    // 사거리 오버레이
    public void drawRangeOverlays(Unit unit) {
        if (unit == null) return;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glLineWidth(1.2f); // 얇고 날카로운 선
        shape.begin(ShapeRenderer.ShapeType.Line);

        for (int x = 0; x < GameConfig.BOARD_WIDTH; x++) {
            for (int y = 0; y < GameConfig.BOARD_HEIGHT; y++) {
                if (x == unit.gridX && y == unit.gridY) continue;

                Vector2 pos = IsoUtils.gridToScreen(x, y);
                int dx = Math.abs(unit.gridX - x);
                int dy = Math.abs(unit.gridY - y);
                int dist = dx + dy;

                // [추가] 변수 선언 및 병과별 사거리 판정
                boolean canAttackTile = false;
                if (unit.unitClass == Unit.UnitClass.ARCHER) {
                    if ((dx == 0 || dy == 0) && dist <= unit.stat.range()) canAttackTile = true;
                } else if (unit.unitClass == Unit.UnitClass.KNIGHT) {
                    if (dx <= 1 && dy <= 1) canAttackTile = true;
                } else {
                    if (dist <= unit.stat.range()) canAttackTile = true;
                }

                if (canAttackTile) {
                    // 깊은 와인색 (Deep Crimson) 적용
                    shape.setColor(new Color(0.6f, 0.1f, 0.1f, 0.7f));
                    drawIsoShape(pos.x, pos.y);
                }
            }
        }
        shape.end();
        Gdx.gl.glLineWidth(1f);
    }

    // 마름모 형태의 가이드 라인
    private void drawIsoShape(float cx, float cy) {
        float hw = GameConfig.TILE_WIDTH / 2f;
        float hh = GameConfig.TILE_HEIGHT / 2f;

        shape.line(cx, cy + hh, cx - hw, cy);
        shape.line(cx - hw, cy, cx, cy - hh);
        shape.line(cx, cy - hh, cx + hw, cy);
        shape.line(cx + hw, cy, cx, cy + hh);
    }
}
