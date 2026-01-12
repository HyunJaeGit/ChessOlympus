package com.hades.game.constants;

import com.badlogic.gdx.graphics.Color;


// UnitData는 모든 직업의 기본 능력치와 팀 색상을 관리합니다.
//Java 21의 record를 사용하여 데이터 객체를 간결하고 안전하게 정의합니다.
public class UnitData {

    /**
     * @param hp 체력
     * @param atk 공격력
     * @param move 이동 거리
     * @param range 사거리
     * @param skillName 스킬명
     */
    public record Stat(int hp, int atk, int move, int range, String skillName) {}

    // --- 직업별 공통 스탯 ---
    public static final Stat RULER = new Stat(200, 20, 1, 1, "왕의 위엄");
    public static final Stat WARRIOR = new Stat(150, 40, 1, 1, "불굴");
    public static final Stat ALCHEMIST = new Stat(100, 25, 2, 1, "유연한 발걸음"); // 경로 공격
    public static final Stat ASSASSIN = new Stat(80, 45, 3, 1, "도약");
    public static final Stat KNIGHT = new Stat(120, 30, 1, 1, "그림자 습격");
    public static final Stat PRIEST = new Stat(90, 10, 1, 2, "신의 가호");
    public static final Stat ARCHER = new Stat(80, 30, 1, 3, "연발 사격");

    // --- 팀 색상 정의 ---
    public static final Color COLOR_HADES = Color.BLUE;
    public static final Color COLOR_ZEUS = Color.RED;

    // 하데스 진영 이름 (직업 순서: WARRIOR, ALCHEMIST, ASSASSIN, RULER, KNIGHT, PRIEST, ARCHER)
    public static final String[] NAMES_HADES = {
        "솜주먹", "키마", "민지", "봉준", "챈나", "띵귤", "연초록"
    };

    // 제우스 진영 이름
    public static final String[] NAMES_ZEUS = {
        "하루비", "히누히누", "코코미", "고세구", "윤이제", "마로니", "시몽"
    };
}
