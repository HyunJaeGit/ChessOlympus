package com.hades.game.constants;

/**
 * [클래스 역할] 게임의 가상 해상도, 격자 크기, 타일 수치 등 모든 물리적 설정값을 중앙 관리합니다.
 */
public class GameConfig {
    // --- 해상도 설정 ---
    /* [설명] 게임 설계의 기준이 되는 가상 해상도입니다. 모든 UI와 유닛 배치는 이 수치를 기준으로 계산됩니다. */
    public static final float VIRTUAL_WIDTH = 1280f;
    public static final float VIRTUAL_HEIGHT = 720f;

    // --- 격자 설정 ---
    public static final int BOARD_WIDTH = 7;
    public static final int BOARD_HEIGHT = 8;

    // --- 쿼터뷰 타일 설정 ---
    /* [설명] 타일의 너비와 높이입니다. 아이소메트릭의 표준인 2:1 비율을 유지합니다. */
    public static final float TILE_WIDTH = 80f;
    public static final float TILE_HEIGHT = 40f;

    /**
     * [메서드 설명] 화면 가로 중앙 좌표를 반환합니다.
     */
    public static float getOriginX() {
        return VIRTUAL_WIDTH / 2f;
    }

    /**
     * [메서드 설명] 화면 세로 중앙에서 맵의 전체 높이를 고려한 보정 좌표를 반환합니다.
     */
    public static float getOriginY() {
        /* [설명] 맵이 화면 정중앙보다 약간 아래쪽에 배치되도록 전체 맵 높이의 절반만큼 보정합니다. */
        return (VIRTUAL_HEIGHT / 2f) - (BOARD_HEIGHT * TILE_HEIGHT / 4f);
    }
}
