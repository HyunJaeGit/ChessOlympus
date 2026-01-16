package com.hades.game.constants;

import com.badlogic.gdx.graphics.Color;

public class UnitData {

    // Record 정의
    public record Stat(int hp, int atk, int counterAtk, int move, int range, String skillName, int value) {
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

    // --- [하데스 영웅 스탯] ---
    public static final Stat SOM_JUMEOK  = new Stat(160, 45, 30, 1, 1, "연옥의 불꽃", 1000);
    public static final Stat KIMA        = new Stat(110, 50, 15, 1, 1, "도약", 1000);
    public static final Stat CHAN_NA     = new Stat(125, 35, 17, 1, 1, "그림자 습격", 1000);
    public static final Stat THING_GYUL  = new Stat(95, 20, 5, 1, 1, "신의 가호", 1000);
    public static final Stat YEON_CHOROK = new Stat(85, 35, 5, 1, 1, "연발 사격", 1000);

    // --- [제우스 영웅 스탯] ---
    public static final Stat DEMETER    = new Stat(110, 20, 10, 1, 1, "치유의 빛", 1000); // // 구 COCO_MI
    public static final Stat HESTIA     = new Stat(130, 35, 15, 1, 1, "신속", 1000);      // // 구 HINU_HINU
    public static final Stat ATHENA     = new Stat(140, 30, 20, 1, 1, "지혜의 방패", 1000); // // 구 YOON_IJE
    public static final Stat ARTEMIS    = new Stat(170, 45, 25, 1, 1, "거인의 힘", 1000);  // // 구 HARU_BI
    public static final Stat HERA       = new Stat(100, 55, 10, 1, 1, "암습", 1000);      // // 구 SI_MONG
    public static final Stat APHRODITE  = new Stat(210, 30, 15, 1, 1, "왕의 위엄", 1000);  // // 구 MA_RONI
    public static final Stat ZEUS       = new Stat(450, 60, 25, 1, 1, "천상의 화살", 1500); // // 구 GO_SEGU

    public static final Color COLOR_HADES = Color.BLUE;
    public static final Color COLOR_ZEUS = Color.RED;

    public static final String[] NAMES_HADES = {"솜주먹", "키마", "챈나", "띵귤", "연초록"};
    public static final Stat[] STATS_HADES = {SOM_JUMEOK, KIMA, CHAN_NA, THING_GYUL, YEON_CHOROK};

    public static final String[] NAMES_ZEUS = {"데메테르", "헤스티아", "아테나", "아르테미스", "헤라", "아프로디테", "제우스"};
    public static final Stat[] STATS_ZEUS = {DEMETER, HESTIA, ATHENA, ARTEMIS, HERA, APHRODITE, ZEUS};
}
