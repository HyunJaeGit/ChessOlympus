package com.hades.game.constants;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;

// 게임 내 모든 유닛의 기본 능력치와 데이터를 정의하는 클래스입니다.
public class UnitData {

    public static class Stat {
        private int hp;
        private int atk;
        private int counterAtk;
        private int move;
        private int range;
        private int value;

        private String reservedSkill = null;
        private final Array<String> learnedSkills = new Array<>();
        private boolean skillUsedThisStage = false;

        public Stat(int hp, int atk, int counterAtk, int move, int range, String initialSkill, int value) {
            this.hp = hp;
            this.atk = atk;
            this.counterAtk = counterAtk;
            this.move = move;
            this.range = range;
            this.value = value;

            // 기본 공격으로 설정하면 스테이지 1에서 특수 권능이 발동하지 않습니다.
            if (initialSkill != null && !initialSkill.isEmpty()) {
                this.learnedSkills.add(initialSkill);
            }
        }

        public int hp() { return hp; }
        public int atk() { return atk; }
        public int counterAtk() { return counterAtk; }
        public int move() { return move; }
        public int range() { return range; }
        public int value() { return value; }
        public Array<String> getLearnedSkills() { return learnedSkills; }
        public boolean isSkillUsed() { return skillUsedThisStage; }
        public String getReservedSkill() { return reservedSkill; }

        public String skillName() {
            return (learnedSkills.size > 0) ? learnedSkills.get(0) : "기본 공격";
        }

        public void setHp(int hp) { this.hp = hp; }
        public void setAtk(int atk) { this.atk = atk; }
        public void setCounterAtk(int counterAtk) { this.counterAtk = counterAtk; }
        public void setMove(int move) { this.move = move; }
        public void setRange(int range) { this.range = range; }
        public void setValue(int value) { this.value = value; }
        public void setSkillUsed(boolean used) { this.skillUsedThisStage = used; }
        public void setReservedSkill(String skillName) { this.reservedSkill = skillName; }
        public void clearReservedSkill() { this.reservedSkill = null; }

        public void addSkill(String skillName) {
            if (!learnedSkills.contains(skillName, false)) {
                learnedSkills.add(skillName);
            }
        }

        public void resetSkillStatus() {
            this.skillUsedThisStage = false;
            this.reservedSkill = null;
        }
    }

    // --- [기본 유닛 병종별 스탯] ---
    public static final Stat STAT_SHIELD  = new Stat(120, 20, 18, 1, 1, "방어 태세", 150);
    public static final Stat STAT_ARCHER  = new Stat(70, 25, 5, 1, 3, "원거리 사격", 200);
    public static final Stat STAT_KNIGHT  = new Stat(100, 25, 10, 1, 1, "기마 돌격", 250);
    public static final Stat STAT_CHARIOT = new Stat(90, 40, 5, 3, 1, "전차 돌진", 300);
    public static final Stat STAT_SAINT   = new Stat(60, 5, 5, 1, 1, "자동 치유", 200);

    // --- [이미지 키워드] ---
    public static final String IMG_SHIELD  = "방패병";
    public static final String IMG_ARCHER  = "궁병";
    public static final String IMG_KNIGHT  = "기마병";
    public static final String IMG_CHARIOT = "전차병";
    public static final String IMG_SAINT   = "성녀";

    // --- [하데스 영웅 스탯] 초기 스킬을 "기본 공격"으로 수정 ---
    public static final Stat SOM_JUMEOK  = new Stat(200, 145, 30 , 5, 5, "기본 공격", 1000);
    public static final Stat KIMA        = new Stat(110, 50, 15, 1, 1, "기본 공격", 1000);
    public static final Stat CHAN_NA     = new Stat(125, 35, 17, 1, 1, "기본 공격", 1000);
    public static final Stat THING_GYUL  = new Stat(95, 20, 5, 1, 1, "기본 공격", 1000);
    public static final Stat YEON_CHOROK = new Stat(85, 35, 5, 1, 1, "기본 공격", 1000);

    // --- [제우스 영웅 스탯] ---
    public static final Stat DEMETER    = new Stat(150, 10, 10, 1, 1, "기본 공격", 1000);
    public static final Stat HESTIA     = new Stat(170, 15, 15, 3, 1, "기본 공격", 1000);
    public static final Stat ATHENA     = new Stat(200, 10, 30, 1, 1, "기본 공격", 1000);
    public static final Stat ARTEMIS    = new Stat(250, 20, 10, 1, 2, "기본 공격", 1000);
    public static final Stat HERA       = new Stat(300, 40, 10, 2, 1, "기본 공격", 1000);
    public static final Stat APHRODITE  = new Stat(350, 30, 15, 2, 2, "기본 공격", 1000);
    public static final Stat ZEUS       = new Stat(450, 50, 25, 2, 2, "기본 공격", 1500);

    public static final Color COLOR_HADES = Color.BLUE;
    public static final Color COLOR_ZEUS = Color.RED;

    public static final String[] NAMES_HADES = {"솜주먹", "키마", "챈나", "띵귤", "연초록"};
    public static final Stat[] STATS_HADES = {SOM_JUMEOK, KIMA, CHAN_NA, THING_GYUL, YEON_CHOROK};

    public static final String[] NAMES_ZEUS = {"데메테르", "헤스티아", "아테나", "아르테미스", "헤라", "아프로디테", "제우스"};
    public static final Stat[] STATS_ZEUS = {DEMETER, HESTIA, ATHENA, ARTEMIS, HERA, APHRODITE, ZEUS};
}
