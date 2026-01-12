package com.hades.game.constants;

import com.badlogic.gdx.graphics.Color;

public class UnitData {

    /**
     * @param hp 체력
     * @param atk 공격력
     * @param move 이동 거리
     * @param range 사거리
     * @param skillName 스킬명
     * @param value 기물 가치 (AI 판단 기준)
     */
    public record Stat(int hp, int atk, int move, int range, String skillName, int value) {}

    // --- 직업별 공통 스탯 (가치 포함) ---
    public static final Stat RULER = new Stat(200, 20, 1, 1, "왕의 위엄", 10);
    public static final Stat WARRIOR = new Stat(150, 40, 1, 1, "불굴", 40);
    public static final Stat ALCHEMIST = new Stat(100, 25, 2, 1, "유연한 발걸음", 35);
    public static final Stat ASSASSIN = new Stat(80, 45, 3, 1, "도약", 50);
    public static final Stat KNIGHT = new Stat(120, 30, 1, 1, "그림자 습격", 30);
    public static final Stat PRIEST = new Stat(90, 10, 1, 2, "신의 가호", 25);
    public static final Stat ARCHER = new Stat(80, 30, 1, 3, "연발 사격", 45);

    public static final Color COLOR_HADES = Color.BLUE;
    public static final Color COLOR_ZEUS = Color.RED;

    public static final String[] NAMES_HADES = {"솜주먹", "키마", "민지", "봉준", "챈나", "띵귤", "연초록"};
    public static final String[] NAMES_ZEUS = {"하루비", "히누히누", "코코미", "고세구", "윤이제", "마로니", "시몽"};
}
