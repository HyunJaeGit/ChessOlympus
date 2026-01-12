package com.hades.game.logic;

import com.badlogic.gdx.math.Vector2;
import com.hades.game.constants.GameConfig;


// IsoUtils는 평면 격자 좌표와 쿼터뷰 화면 좌표 간의 변환을 담당합니다.
public class IsoUtils {

    /**
     * 격자 좌표(gridX, gridY)를 받아 쿼터뷰 화면 좌표로 변환합니다.
     * @param gridX 격자의 가로 인덱스
     * @param gridY 격자의 세로 인덱스
     * @return 화면상의 x, y 좌표값 (Vector2)
     */
    public static Vector2 gridToScreen(int gridX, int gridY) {
        /* 쿼터뷰 변환 공식:
           screenX = (gridX - gridY) * (tileWidth / 2)
           screenY = (gridX + gridY) * (tileHeight / 2)
        */
        float screenX = (gridX - gridY) * (GameConfig.TILE_WIDTH / 2f);
        float screenY = (gridX + gridY) * (GameConfig.TILE_HEIGHT / 2f);

        // 기준점(Origin)을 더해 화면 중앙 근처로 옮깁니다.
        return new Vector2(screenX + GameConfig.ORIGIN_X, screenY + GameConfig.ORIGIN_Y);
    }

    public static Vector2 screenToGrid(float screenX, float screenY) {
        // 기준점(Origin) 제거
        float x = screenX - GameConfig.ORIGIN_X;
        float y = screenY - GameConfig.ORIGIN_Y;

        // 쿼터뷰 역변환 공식
    /* gridX = (x / (tileWidth / 2) + y / (tileHeight / 2)) / 2
       gridY = (y / (tileHeight / 2) - x / (tileWidth / 2)) / 2
    */
        float gx = (x / (GameConfig.TILE_WIDTH / 2f) + y / (GameConfig.TILE_HEIGHT / 2f)) / 2f;
        float gy = (y / (GameConfig.TILE_HEIGHT / 2f) - x / (GameConfig.TILE_WIDTH / 2f)) / 2f;

        // 반올림하여 정확한 칸 인덱스 반환
        return new Vector2((float)Math.floor(gx + 0.5f), (float)Math.floor(gy + 0.5f));
    }

}
