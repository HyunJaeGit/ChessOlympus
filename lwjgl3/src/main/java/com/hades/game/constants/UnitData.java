package com.hades.game.constants;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

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

        // 개별 스킬의 사용 여부를 저장하는 맵(스킬명, 사용됨 여부)
        private final ObjectMap<String, Boolean> usedSkills = new ObjectMap<>();

        public Stat(int hp, int atk, int counterAtk, int move, int range, String initialSkill, int value) {
            this.hp = hp;
            this.atk = atk;
            this.counterAtk = counterAtk;
            this.move = move;
            this.range = range;
            this.value = value;

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

        // 특정 스킬이 아직 사용 가능한지(사용되지 않았는지) 확인
        public boolean isSkillReady(String skillName) {
            return !usedSkills.get(skillName, false);
        }

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

        // 스킬 사용 상태를 설정 (스킬 개별 관리용)
        public void setSkillUsed(String skillName, boolean used) {
            this.usedSkills.put(skillName, used);
        }

        public void setReservedSkill(String skillName) { this.reservedSkill = skillName; }
        public void clearReservedSkill() { this.reservedSkill = null; }

        public void addSkill(String skillName) {
            if (!learnedSkills.contains(skillName, false)) {
                learnedSkills.add(skillName);
            }
        }

        // // 스테이지 시작 시 모든 스킬 사용 상태를 초기화합니다.
        public void resetSkillStatus() {
            this.usedSkills.clear();
            this.reservedSkill = null;
        }
    }

    // --- [기본 유닛 병종별 스탯] 자동공격/반격 시스템 대응 내구도 강화 ---
    public static final Stat STAT_SHIELD  = new Stat(450, 25, 20, 1, 1, "방어 태세", 150);
    public static final Stat STAT_ARCHER  = new Stat(200, 45, 5, 1, 3, "원거리 사격", 200);
    public static final Stat STAT_KNIGHT  = new Stat(350, 40, 20, 2, 1, "기마 돌격", 250);
    public static final Stat STAT_CHARIOT = new Stat(300, 55, 10, 3, 1, "전차 돌진", 300);
    public static final Stat STAT_SAINT   = new Stat(250, 10, 5, 1, 1, "자동 치유", 200);

    // --- [이미지 키워드] ---
    public static final String IMG_SHIELD  = "방패병";
    public static final String IMG_ARCHER  = "궁병";
    public static final String IMG_KNIGHT  = "기마병";
    public static final String IMG_CHARIOT = "전차병";
    public static final String IMG_SAINT   = "성녀";

    // --- [하데스 영웅 스탯] ---
    public static final Stat SOM_JUMEOK  = new Stat(700, 50, 30 , 1, 1, "기본 공격", 1000);
    public static final Stat KIMA        = new Stat(400, 65, 15, 3, 1, "기본 공격", 1000);
    public static final Stat CHAN_NA     = new Stat(450, 75, 25, 2, 1, "기본 공격", 1000);
    public static final Stat THING_GYUL  = new Stat(320, 55, 10, 2, 3, "기본 공격", 1000);
    public static final Stat YEON_CHOROK = new Stat(500, 30, 70, 2, 1, "기본 공격", 1000);

    // --- [제우스 영웅 스탯] ---
    public static final Stat DEMETER    = new Stat(500, 35, 25, 1, 1, "대지의 속박", 1000);
    public static final Stat HESTIA     = new Stat(600, 40, 30, 2, 1, "영겁의 화로", 1000);
    public static final Stat ATHENA     = new Stat(650, 35, 45, 1, 1, "전략적 방벽", 1000);
    public static final Stat ARTEMIS    = new Stat(700, 45, 20, 2, 3, "달빛의 추격", 1000);
    public static final Stat HERA       = new Stat(800, 60, 30, 2, 1, "여왕의 권위", 1000);
    public static final Stat APHRODITE  = new Stat(1000, 55, 35, 2, 2, "매혹의 향기", 1000);
    public static final Stat ZEUS       = new Stat(2000, 80, 50, 2, 2, "심판의 번개", 1500);

    public static final Color COLOR_HADES = Color.BLUE;
    public static final Color COLOR_ZEUS = Color.RED;

    public static final String[] NAMES_HADES = {"솜주먹", "키마", "챈나", "띵귤", "연초록"};
    public static final Stat[] STATS_HADES = {SOM_JUMEOK, KIMA, CHAN_NA, THING_GYUL, YEON_CHOROK};

    public static final String[] NAMES_ZEUS = {"데메테르", "헤스티아", "아테나", "아르테미스", "헤라", "아프로디테", "제우스"};
    public static final Stat[] STATS_ZEUS = {DEMETER, HESTIA, ATHENA, ARTEMIS, HERA, APHRODITE, ZEUS};
}
