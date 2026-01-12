package com.hades.game.constants;


// GameConfig : 게임 전반에 사용되는 물리적 수치와 설정값들을 관리
// 7x8 격자 크기 및 쿼터뷰 타일의 비율을 정의
public class GameConfig {
    // --- 격자 설정 ---
    public static final int BOARD_WIDTH = 7;
    public static final int BOARD_HEIGHT = 8;

    // --- 쿼터뷰 타일 설정 (픽셀 단위) ---
    /* 쿼터뷰 타일은 보통 너비가 높이의 2배일 때 가장 자연스럽습니다. */
    public static final float TILE_WIDTH = 64f;
    public static final float TILE_HEIGHT = 32f;

    // --- 화면 중앙 정렬을 위한 오프셋 (임시) ---
    public static final float ORIGIN_X = 400f;
    public static final float ORIGIN_Y = 100f;
}
