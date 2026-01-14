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

    // 타일의 수직 두께 (픽셀 단위)
    private static final int TILE_DEPTH = 12;
    private static final float TILE_PADDING = 24f; // 타일 사이의 간격 (픽셀)

    public MapRenderer(ShapeRenderer shape, SpriteBatch batch, Texture tileTop) {
        this.shape = shape;
        this.batch = batch;
        this.tileTop = tileTop;
    }

    // 전장 타일을 입체적으로 출력 (타일 겹침 방지를 위해 렌더링 순서를 최적화)
    public void drawTiles(Vector2 hoveredGrid) {
        batch.begin();

        float drawW = GameConfig.TILE_WIDTH - TILE_PADDING;
        float drawH = GameConfig.TILE_HEIGHT - (TILE_PADDING / 2f);

        // 아이소메트릭 겹침을 해결하기 위한 루프 순서 변경
        // Y축(깊이)을 먼저 바깥 루프에 두고, 위에서 아래로 내려오며 그립니다.
        for (int y = 0; y < GameConfig.BOARD_HEIGHT; y++) {
            for (int x = 0; x < GameConfig.BOARD_WIDTH; x++) {
                Vector2 pos = IsoUtils.gridToScreen(x, y);

                // 1. 타일 옆면(두께) 그리기
                for (int i = TILE_DEPTH; i > 0; i--) {
                    float brightness = 0.35f + (0.3f * (1.0f - (float)i / TILE_DEPTH));
                    batch.setColor(brightness, brightness, brightness, 1.0f);

                    batch.draw(tileTop,
                        pos.x - drawW / 2f,
                        pos.y - drawH / 2f - i,
                        drawW,
                        drawH);
                }

                // 2. 타일 윗면 그리기
                if (x == (int) hoveredGrid.x && y == (int) hoveredGrid.y) {
                    batch.setColor(Color.LIGHT_GRAY);
                } else {
                    batch.setColor(Color.WHITE);
                }

                batch.draw(tileTop,
                    pos.x - drawW / 2f,
                    pos.y - drawH / 2f,
                    drawW,
                    drawH);
            }
        }
        batch.setColor(Color.WHITE);
        batch.end();
    }

    // 유닛 선택 시 이동/공격 가능 범위 출력
    public void drawRangeOverlays(Unit unit, Array<Unit> units) {
        if (unit == null) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shape.begin(ShapeRenderer.ShapeType.Line);

        for (int x = 0; x < GameConfig.BOARD_WIDTH; x++) {
            for (int y = 0; y < GameConfig.BOARD_HEIGHT; y++) {
                int dist = Math.abs(unit.gridX - x) + Math.abs(unit.gridY - y);
                Vector2 pos = IsoUtils.gridToScreen(x, y);

                if (dist <= unit.stat.range() && dist > 0) {
                    shape.setColor(1, 0, 0, 0.8f); // 공격 사거리 표시 (빨강)
                    drawIsoShape(pos.x, pos.y);
                } else if (BoardManager.canMoveTo(unit, x, y, units)) {
                    shape.setColor(0, 1, 1, 0.8f); // 이동 가능 범위 표시 (청록)
                    drawIsoShape(pos.x, pos.y);
                }
            }
        }
        shape.end();
    }

    // 마름모 형태의 가이드 라인 생성
    private void drawIsoShape(float cx, float cy) {
        float hw = GameConfig.TILE_WIDTH / 2f;
        float hh = GameConfig.TILE_HEIGHT / 2f;

        shape.line(cx, cy + hh, cx - hw, cy);
        shape.line(cx - hw, cy, cx, cy - hh);
        shape.line(cx, cy - hh, cx + hw, cy);
        shape.line(cx + hw, cy, cx, cy + hh);
    }
}
