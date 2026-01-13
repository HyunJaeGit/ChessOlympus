package com.hades.game.constants;

import com.badlogic.gdx.graphics.Color;

/**
 * [클래스 역할] 유닛의 기본 능력치와 진영 정보를 정의합니다.
 */
public class UnitData {
    // getSkill() 메서드를 추가하여 SkillData와의 연결 고리를 만듭니다.
    public record Stat(int hp, int atk, int counterAtk, int move, int range, String skillName, int value) {
        /**
         * [메서드 설명] 이 스탯이 가진 skillName을 키값으로 SkillData에서 상세 정보를 가져옵니다.
         */
        public SkillData.Skill getSkillInfo() {
            return SkillData.get(this.skillName);
        }
    }

    // --- 이하 스탯 데이터들은 기존 코드와 동일 (생략) ---
    public static final Stat RULER = new Stat(200, 20, 10, 1, 1, "왕의 위엄", 10);
    public static final Stat WARRIOR = new Stat(150, 40, 30, 1, 1, "불굴", 40);
    public static final Stat ALCHEMIST = new Stat(100, 25, 15, 2, 1, "유연한 발걸음", 35);
    public static final Stat ASSASSIN = new Stat(80, 45, 10, 3, 1, "도약", 50);
    public static final Stat KNIGHT = new Stat(120, 30, 20, 1, 1, "그림자 습격", 30);
    public static final Stat PRIEST = new Stat(90, 10, 5, 1, 2, "신의 가호", 25);
    public static final Stat ARCHER = new Stat(80, 30, 5, 1, 3, "연발 사격", 45);
    public static final Stat SOLDIER = new Stat(100, 20, 10, 1, 1, "일반 병사", 10);

    public static final Color COLOR_HADES = Color.BLUE;
    public static final Color COLOR_ZEUS = Color.RED;

    public static final String[] NAMES_HADES = {"솜주먹", "키마", "챈나", "띵귤", "연초록"};
    public static final Stat[] STATS_HADES = {WARRIOR, KNIGHT, PRIEST, ARCHER, ALCHEMIST};

    public static final String[] NAMES_ZEUS = {"하루비", "히누히누", "코코미", "고세구", "윤이제"};
    public static final Stat[] STATS_ZEUS = {WARRIOR, KNIGHT, PRIEST, ARCHER, ALCHEMIST};
}
