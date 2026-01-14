package com.hades.game.logic;

import com.badlogic.gdx.math.Vector2;
import com.hades.game.constants.GameConfig;

/* [클래스 역할] 평면 격자 좌표와 아이소메트릭 화면 좌표 간의 정밀한 변환을 담당합니다. */
public class IsoUtils {

    /**
     * [메서드 설명] 격자 좌표를 GameConfig에 설정된 해상도 기준의 화면 중앙 좌표로 변환합니다.
     */
    public static Vector2 gridToScreen(int gridX, int gridY) {
        /* 1. 기본 아이소메트릭 변환 (마름모꼴 좌표 생성) */
        float screenX = (gridX - gridY) * (GameConfig.TILE_WIDTH / 2f);
        float screenY = (gridX + gridY) * (GameConfig.TILE_HEIGHT / 2f);

        /* 2. 중앙 정렬 오프셋 적용:
           GameConfig에서 동적으로 계산된 중앙 좌표를 가져와 더해줍니다. */
        float offsetX = GameConfig.getOriginX();
        float offsetY = GameConfig.getOriginY();

        return new Vector2(screenX + offsetX, screenY + offsetY);
    }

    /**
     * [메서드 설명] 마우스(가상 좌표)를 역산하여 정확한 그리드 인덱스(타일 번호)를 찾아냅니다.
     */
    public static Vector2 screenToGrid(float screenX, float screenY) {
        /* 1. gridToScreen에서 사용한 것과 동일한 오프셋을 제거하여 원점으로 복원합니다. */
        float x = screenX - GameConfig.getOriginX();
        float y = screenY - GameConfig.getOriginY();

        /* 2. 아이소메트릭 역변환 공식 적용 */
        float gx = (x / (GameConfig.TILE_WIDTH / 2f) + y / (GameConfig.TILE_HEIGHT / 2f)) / 2f;
        float gy = (y / (GameConfig.TILE_HEIGHT / 2f) - x / (GameConfig.TILE_WIDTH / 2f)) / 2f;

        /**
         * 0.5f를 더해 반올림(Floor) 처리함으로써,
         * 마우스 커서가 타일의 중앙이 아닌 모서리에 있더라도 가장 가까운 타일을 인식하게 합니다.
         */
        return new Vector2((float)Math.floor(gx + 0.5f), (float)Math.floor(gy + 0.5f));
    }
}
