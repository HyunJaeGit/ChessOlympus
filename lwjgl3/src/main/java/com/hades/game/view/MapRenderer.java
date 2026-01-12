package com.hades.game.view;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.hades.game.constants.GameConfig;
import com.hades.game.entities.Unit;
import com.hades.game.logic.BoardManager;
import com.hades.game.logic.IsoUtils;

public class MapRenderer {

    private ShapeRenderer shape;

    public MapRenderer(ShapeRenderer shape) {
        this.shape = shape;
    }

    /* [메서드 설명] 전체 타일 맵의 바닥 격자를 그립니다. */
    public void drawGrid(Vector2 hoveredGrid, String currentTurn) {
        for (int x = 0; x < GameConfig.BOARD_WIDTH; x++) {
            for (int y = 0; y < GameConfig.BOARD_HEIGHT; y++) {
                /* 현재 턴의 팀에 따라 바닥 색상에 차이를 줍니다. */
                Color color = currentTurn.equals("HADES") ?
                    new Color(0, 0, 0.3f, 1) : new Color(0.3f, 0, 0, 1);

                if (x == (int) hoveredGrid.x && y == (int) hoveredGrid.y) {
                    color = Color.WHITE;
                }
                drawIsoTile(x, y, color);
            }
        }
    }

    /* [메서드 설명] 유닛이 이동 가능한 곳과 공격 가능한 범위를 바닥에 표시합니다. */
    public void drawRangeOverlays(Unit unit, Array<Unit> units) {
        if (unit == null) return;

        for (int x = 0; x < GameConfig.BOARD_WIDTH; x++) {
            for (int y = 0; y < GameConfig.BOARD_HEIGHT; y++) {
                int dist = Math.abs(unit.gridX - x) + Math.abs(unit.gridY - y);

                if (dist <= unit.stat.range()) {
                    drawIsoTile(x, y, Color.RED); /* 사거리 안 */
                } else if (BoardManager.canMoveTo(unit, x, y, units)) {
                    drawIsoTile(x, y, Color.CYAN); /* 이동 가능 */
                }
            }
        }
    }

    /* [메서드 설명] 쿼터뷰(Iso) 좌표를 계산하여 다이아몬드 형태의 타일 선을 그립니다. */
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
