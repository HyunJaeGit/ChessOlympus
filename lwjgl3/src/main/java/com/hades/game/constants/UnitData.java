package com.hades.game.constants;

import com.badlogic.gdx.graphics.Color;

public class UnitData {

    public record Stat(int hp, int atk, int counterAtk, int move, int range, String skillName, int value, String imageName) {
        public SkillData.Skill getSkillInfo() {
            return SkillData.get(this.skillName);
        }
    }

    // --- [기본 유닛 병종별 스탯] ---
    public static final Stat CLASS_SHIELD = new Stat(120, 20, 18, 1, 1, "방어 태세", 150, "제우스일반병사");
    public static final Stat CLASS_KNIGHT = new Stat(100, 25, 10, 1, 1, "기마 돌격", 250, "제우스일반병사");
    public static final Stat CLASS_ARCHER = new Stat(70, 25, 5, 1, 3, "원거리 사격", 200, "제우스일반병사");
    public static final Stat CLASS_CHARIOT = new Stat(90, 40, 5, 3, 1, "전차 돌진", 300, "제우스일반병사");
    public static final Stat CLASS_SAINT = new Stat(60, 5, 5, 1, 1, "자동 치유", 200, "제우스일반병사");

    // --- [하데스 진영 영웅] ---
    public static final Stat SOM_JUMEOK = new Stat(160, 45, 30, 1, 1, "불굴", 1000, "솜주먹");
    public static final Stat KIMA = new Stat(110, 50, 15, 1, 1, "도약", 1000, "키마");
    public static final Stat CHAN_NA = new Stat(125, 35, 17, 1, 1, "그림자 습격", 1000, "챈나");
    public static final Stat THING_GYUL = new Stat(95, 20, 5, 1, 1, "신의 가호", 1000, "띵귤");
    public static final Stat YEON_CHOROK = new Stat(85, 35, 5, 1, 1, "연발 사격", 1000, "연초록");

    // --- [제우스 진영 영웅] ---
    public static final Stat COCO_MI = new Stat(110, 20, 10, 1, 1, "치유의 빛", 1000, "코코미");
    public static final Stat HINU_HINU = new Stat(130, 35, 15, 1, 1, "신속", 1000, "히누히누");
    public static final Stat YOON_IJE = new Stat(140, 30, 20, 1, 1, "지혜의 방패", 1000, "윤이제");
    public static final Stat HARU_BI = new Stat(170, 45, 25, 1, 1, "거인의 힘", 1000, "하루비");
    public static final Stat SI_MONG = new Stat(100, 55, 10, 1, 1, "암습", 1000, "시몽");
    public static final Stat MA_RONI = new Stat(210, 30, 15, 1, 1, "왕의 위엄", 1000, "마로니");
    public static final Stat GO_SEGU = new Stat(450, 60, 25, 1, 1, "천상의 화살", 1500, "고세구");

    // --- [진영 설정 및 데이터 배열 (에러 해결 핵심)] ---
    public static final Color COLOR_HADES = Color.BLUE;
    public static final Color COLOR_ZEUS = Color.RED;

    // 하데스 영웅 리스트 (HeroSelectionScreen용)
    public static final String[] NAMES_HADES = {"솜주먹", "키마", "챈나", "띵귤", "연초록"};
    public static final Stat[] STATS_HADES = {SOM_JUMEOK, KIMA, CHAN_NA, THING_GYUL, YEON_CHOROK};

    // 제우스 보스 리스트 (BattleScreen 및 HeroSelectionScreen용)
    public static final String[] NAMES_ZEUS = {"데메테르", "헤스티아", "아테나", "아르테미스", "헤라", "아프로디테", "제우스"};
    public static final Stat[] STATS_ZEUS = {COCO_MI, HINU_HINU, YOON_IJE, HARU_BI, SI_MONG, MA_RONI, GO_SEGU};

    // 일반 병사 데이터 (기존 코드 유지용)
    public static final Stat HADES_SOLDIER = CLASS_SHIELD; // 기존 코드를 위해 매칭
    public static final Stat ZEUS_SOLDIER = CLASS_SHIELD;
}
