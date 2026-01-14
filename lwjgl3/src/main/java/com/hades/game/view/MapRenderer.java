package com.hades.game.view;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.hades.game.constants.GameConfig;
import com.hades.game.entities.Unit;
import com.hades.game.logic.BoardManager;
import com.hades.game.logic.IsoUtils;

/* [클래스 역할] 7x8 쿼터뷰 전장의 바닥 타일 격자와 이동/공격 범위 가이드라인을 렌더링합니다. */
public class MapRenderer {

    private ShapeRenderer shape;

    public MapRenderer(ShapeRenderer shape) {
        this.shape = shape;
    }

    /* [메서드 설명] 전체 타일 맵의 바닥 격자를 그립니다. 현재 턴인 진영에 따라 타일 색상이 변경됩니다. */
    public void drawGrid(Vector2 hoveredGrid, String currentTurn) {
        for (int x = 0; x < GameConfig.BOARD_WIDTH; x++) {
            for (int y = 0; y < GameConfig.BOARD_HEIGHT; y++) {
                // HADES 턴이면 푸른색 계열, ZEUS 턴이면 붉은색 계열로 바닥 강조
                Color color = currentTurn.equals("HADES") ?
                    new Color(0, 0, 0.4f, 0.5f) : new Color(0.4f, 0, 0, 0.5f);

                if (x == (int) hoveredGrid.x && y == (int) hoveredGrid.y) {
                    color = Color.WHITE; // 마우스 오버 시 흰색 강조
                }
                drawIsoTile(x, y, color);
            }
        }
    }

    /* [메서드 설명] 유닛이 선택되었을 때, 해당 유닛의 사거리(빨간색)와
    이동 가능 범위(청록색)를 타일 위에 오버레이로 표시합니다.
    */
    public void drawRangeOverlays(Unit unit, Array<Unit> units) {
        if (unit == null) return;

        for (int x = 0; x < GameConfig.BOARD_WIDTH; x++) {
            for (int y = 0; y < GameConfig.BOARD_HEIGHT; y++) {
                int dist = Math.abs(unit.gridX - x) + Math.abs(unit.gridY - y);

                if (dist <= unit.stat.range() && dist > 0) {
                    drawIsoTile(x, y, new Color(1, 0, 0, 0.4f)); // 사거리 표시
                } else if (BoardManager.canMoveTo(unit, x, y, units)) {
                    drawIsoTile(x, y, new Color(0, 1, 1, 0.4f)); // 이동 범위 표시
                }
            }
        }
    }

    // 마름모 형태의 쿼터뷰 타일 하나를 그리는 내부 헬퍼 메서드입니다.
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
}
