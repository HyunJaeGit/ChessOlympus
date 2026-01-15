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
    private static final float TILE_PADDING = 24f;

    public MapRenderer(ShapeRenderer shape, SpriteBatch batch, Texture tileTop) {
        this.shape = shape;
        this.batch = batch;
        this.tileTop = tileTop;
    }

    // [수정] 이동 가능 범위를 타일 색상으로 표현하기 위해 파라미터 추가
    public void drawTiles(Vector2 hoveredGrid, Unit selectedUnit, Array<Unit> units) {
        batch.begin();

        float drawW = GameConfig.TILE_WIDTH - TILE_PADDING;
        float drawH = GameConfig.TILE_HEIGHT - (TILE_PADDING / 2f);

        for (int y = 0; y < GameConfig.BOARD_HEIGHT; y++) {
            for (int x = 0; x < GameConfig.BOARD_WIDTH; x++) {
                Vector2 pos = IsoUtils.gridToScreen(x, y);

                // 1. 타일 옆면(두께) 그리기
                for (int i = TILE_DEPTH; i > 0; i--) {
                    float brightness = 0.35f + (0.3f * (1.0f - (float)i / TILE_DEPTH));
                    batch.setColor(brightness, brightness, brightness, 1.0f);
                    batch.draw(tileTop, pos.x - drawW / 2f, pos.y - drawH / 2f - i, drawW, drawH);
                }

                // 2. 타일 윗면 색상 결정 로직
                Color tileColor = Color.WHITE;

                // 이동 가능 범위 하이라이트 (청록색 계열)
                if (selectedUnit != null && BoardManager.canMoveTo(selectedUnit, x, y, units)) {
                    tileColor = new Color(0.1f, 0.7f, 0.8f, 1.0f); // 선명한 청록색
                }

                // 마우스 오버 하이라이트 (이동 범위보다 우선순위 높음)
                if (x == (int) hoveredGrid.x && y == (int) hoveredGrid.y) {
                    tileColor = Color.LIGHT_GRAY;
                }

                batch.setColor(tileColor);
                batch.draw(tileTop, pos.x - drawW / 2f, pos.y - drawH / 2f, drawW, drawH);
            }
        }
        batch.setColor(Color.WHITE);
        batch.end();
    }

    // [수정] 이동 범위 로직을 제거하고 공격 사거리 테두리만 출력
    public void drawRangeOverlays(Unit unit) {
        if (unit == null) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glLineWidth(3f); // 사거리 테두리를 더 선명하게 두께 조절
        shape.begin(ShapeRenderer.ShapeType.Line);

        for (int x = 0; x < GameConfig.BOARD_WIDTH; x++) {
            for (int y = 0; y < GameConfig.BOARD_HEIGHT; y++) {
                if (x == unit.gridX && y == unit.gridY) continue;

                Vector2 pos = IsoUtils.gridToScreen(x, y);
                int dx = Math.abs(unit.gridX - x);
                int dy = Math.abs(unit.gridY - y);
                int dist = dx + dy;

                boolean canAttackTile = false;

                // 병과별 사거리 로직 일치화
                if (unit.unitClass == Unit.UnitClass.ARCHER) {
                    if ((dx == 0 || dy == 0) && dist <= unit.stat.range()) canAttackTile = true;
                } else if (unit.unitClass == Unit.UnitClass.KNIGHT) {
                    if (dx <= 1 && dy <= 1) canAttackTile = true;
                } else {
                    if (dist <= unit.stat.range()) canAttackTile = true;
                }

                if (canAttackTile) {
                    shape.setColor(1, 0, 0, 0.9f); // 공격 사거리는 빨간색 테두리
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
