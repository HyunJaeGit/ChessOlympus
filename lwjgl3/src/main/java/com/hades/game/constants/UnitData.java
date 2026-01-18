package com.hades.game.constants;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import java.io.Serializable;

// Chess Olympus: HADES vs ZEUS - 유닛 스탯 및 데이터 관리 클래스
public class UnitData {

    public static class Stat implements Serializable {
        private int hp;
        private int atk;
        private int counterAtk;
        private int move;
        private int range;
        private int value;

        private String reservedSkill = null;
        private final Array<String> learnedSkills = new Array<>();
        private final ObjectMap<String, Boolean> usedSkills = new ObjectMap<>();

        public Stat() { } // JSON 로드를 위한 기본 생성자

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

        public Stat(Stat other) {
            if (other == null) return;
            this.hp = other.hp;
            this.atk = other.atk;
            this.counterAtk = other.counterAtk;
            this.move = other.move;
            this.range = other.range;
            this.value = other.value;
            this.learnedSkills.addAll(other.learnedSkills);
        }

        // Getter / Setter
        public int hp() { return hp; }
        public int atk() { return atk; }
        public int counterAtk() { return counterAtk; }
        public int move() { return move; }
        public int range() { return range; }
        public int value() { return value; }
        public Array<String> getLearnedSkills() { return learnedSkills; }
        public boolean isSkillReady(String skillName) { return !usedSkills.get(skillName, false); }
        public String getReservedSkill() { return reservedSkill; }

        // [복구] 기존에 사용하던 skillName() 메서드
        public String skillName() { return (learnedSkills.size > 0) ? learnedSkills.get(0) : "기본 공격"; }

        public void setHp(int hp) { this.hp = hp; }
        public void setAtk(int atk) { this.atk = atk; }
        public void setCounterAtk(int counterAtk) { this.counterAtk = counterAtk; }
        public void setMove(int move) { this.move = move; }
        public void setRange(int range) { this.range = range; }
        public void setValue(int value) { this.value = value; }
        public void setSkillUsed(String skillName, boolean used) { this.usedSkills.put(skillName, used); }
        public void setReservedSkill(String skillName) { this.reservedSkill = skillName; }
        public void clearReservedSkill() { this.reservedSkill = null; }
        public void addSkill(String skillName) { if (!learnedSkills.contains(skillName, false)) learnedSkills.add(skillName); }
        public void resetSkillStatus() { this.usedSkills.clear(); this.reservedSkill = null; }
    }

    // --- [일반 병사 스탯: 밸런싱 유지] ---
    public static final Stat STAT_SHIELD  = new Stat(450, 25, 20, 1, 1, "방어 태세", 150);
    public static final Stat STAT_ARCHER  = new Stat(200, 45, 5, 1, 3, "원거리 사격", 200);
    public static final Stat STAT_KNIGHT  = new Stat(350, 40, 20, 2, 1, "기마 돌격", 250);
    public static final Stat STAT_CHARIOT = new Stat(300, 55, 10, 3, 1, "전차 돌진", 300);
    public static final Stat STAT_SAINT   = new Stat(250, 10, 5, 1, 1, "자동 치유", 200);

    // [복구] StageGenerator에서 참조하는 이미지 상수들
    public static final String IMG_SHIELD  = "방패병";
    public static final String IMG_ARCHER  = "궁병";
    public static final String IMG_KNIGHT  = "기마병";
    public static final String IMG_CHARIOT = "전차병";
    public static final String IMG_SAINT   = "성녀";

    // --- [하데스 진영 영웅: 상향된 밸런싱] ---
    public static final Stat SOM_JUMEOK  = new Stat(900, 55, 45 , 1, 1, "기본 공격", 1000);
    public static final Stat KIMA        = new Stat(550, 75, 15, 3, 1, "기본 공격", 1000);
    public static final Stat CHAN_NA     = new Stat(600, 85, 25, 2, 1, "기본 공격", 1000);
    public static final Stat THING_GYUL  = new Stat(480, 65, 10, 2, 3, "기본 공격", 1000);
    public static final Stat YEON_CHOROK = new Stat(700, 40, 85, 2, 1, "기본 공격", 1000);

    // --- [제우스 진영 영웅: 보스 밸런싱] ---
    public static final Stat DEMETER    = new Stat(1000, 45, 25, 1, 1, "대지의 분노", 1000);
    public static final Stat HESTIA     = new Stat(1100, 50, 30, 2, 1, "영겁의 화로", 1000);
    public static final Stat ATHENA     = new Stat(1200, 55, 45, 1, 1, "전략적 방벽", 1000);
    public static final Stat ARTEMIS    = new Stat(1000, 70, 20, 2, 3, "달빛의 추격", 1000);
    public static final Stat HERA       = new Stat(1300, 65, 30, 2, 1, "여왕의 권위", 1000);
    public static final Stat APHRODITE  = new Stat(1400, 60, 35, 2, 2, "매혹의 향기", 1000);
    public static final Stat ZEUS       = new Stat(2500, 95, 50, 2, 2, "심판의 번개", 1500);

    public static final Color COLOR_HADES = Color.BLUE;
    public static final Color COLOR_ZEUS = Color.RED;

    public static final String[] NAMES_HADES = {"솜주먹", "키마", "챈나", "띵귤", "연초록"};
    public static final Stat[] STATS_HADES = {SOM_JUMEOK, KIMA, CHAN_NA, THING_GYUL, YEON_CHOROK};
    public static final String[] NAMES_ZEUS = {"데메테르", "헤스티아", "아테나", "아르테미스", "헤라", "아프로디테", "제우스"};
    public static final Stat[] STATS_ZEUS = {DEMETER, HESTIA, ATHENA, ARTEMIS, HERA, APHRODITE, ZEUS};
}
