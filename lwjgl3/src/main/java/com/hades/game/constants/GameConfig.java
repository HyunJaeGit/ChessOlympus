package com.hades.game.constants;

/**
 * [클래스 역할] 게임의 물리적 수치와 격자 설정을 관리합니다.
 */
public class GameConfig {
    // --- 격자 설정 ---
    public static final int BOARD_WIDTH = 7;
    public static final int BOARD_HEIGHT = 8;

    // --- 쿼터뷰 타일 설정 ---
    /* 타일 크기로 시야 조절 */
    public static final float TILE_WIDTH = 80f;
    public static final float TILE_HEIGHT = 40f; // 너비의 절반 유지를 권장합니다.

    // --- 화면 중앙 정렬 오프셋 조정 ---
    /* 타일이 커지면 전체 판이 우하단으로 치우칠 수 있으므로 ORIGIN 좌표를 조정해야 합니다. */
    public static final float ORIGIN_X = 350f;
    public static final float ORIGIN_Y = 100f;
}
