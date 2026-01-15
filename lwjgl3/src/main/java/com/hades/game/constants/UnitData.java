package com.hades.game.constants;

import com.badlogic.gdx.graphics.Color;

/**
 * [클래스 역할] 각 영웅 및 병사들의 고유 스탯과 이미지 매칭 데이터를 관리합니다.
 * 1월 15일 업데이트: 개별 영웅 스탯 관리 및 고세구 최종 보스 설정 반영.
 */
public class UnitData {

    /**
     * [구조 설명] 유닛의 세부 능력치를 저장하는 레코드입니다.
     * @param imageName 이미지 파일명 (확장자 제외)
     * @param value AI 점수 산정 및 가치 척도
     */
    public record Stat(int hp, int atk, int counterAtk, int move, int range, String skillName, int value, String imageName) {
        /**
         * [메서드 설명] 이 스탯이 가진 skillName을 키값으로 SkillData에서 상세 정보를 가져옵니다.
         */
        public SkillData.Skill getSkillInfo() {
            return SkillData.get(this.skillName);
        }
    }

    // --- [하데스 진영 영웅 (플레이어)] ---
    // 솜주먹: 전형적인 탱커. 반격력이 상대적으로 높음 (약 66%)
    public static final Stat SOM_JUMEOK = new Stat(160, 45, 30,
        1, 1, "불굴", 400, "솜주먹");
    // 키마: 기동력 중심. 반격은 매우 취약 (30%)
    public static final Stat KIMA = new Stat(110, 50, 15,
        3, 1, "도약", 550, "키마");
    // 챈나: 밸런스형.
    public static final Stat CHAN_NA = new Stat(125, 35, 17,
        1, 1, "그림자 습격", 350, "챈나");
    // 띵귤: 서포터/원거리. 반격력은 상징적인 수준.
    public static final Stat THING_GYUL = new Stat(95, 20, 5,
        1, 2, "신의 가호", 300, "띵귤");
    // 연초록: 저격수. 사거리가 긴 대신 근접 반격은 최악.
    public static final Stat YEON_CHOROK = new Stat(85, 35, 5,
        2, 3, "연발 사격", 500, "연초록");

    // --- [제우스 진영 영웅 (보스)] ---
    public static final Stat COCO_MI = new Stat(110, 20, 10, 1, 2, "치유의 빛", 350, "코코미");
    public static final Stat HINU_HINU = new Stat(130, 35, 15, 2, 1, "신속", 400, "히누히누");
    public static final Stat YOON_IJE = new Stat(140, 30, 20, 1, 1, "지혜의 방패", 400, "윤이제");
    public static final Stat HARU_BI = new Stat(170, 45, 25, 1, 1, "거인의 힘", 450, "하루비");
    public static final Stat SI_MONG = new Stat(100, 55, 10, 3, 1, "암습", 600, "시몽");
    public static final Stat MA_RONI = new Stat(210, 30, 15, 1, 1, "왕의 위엄", 700, "마로니");

    // 스테이지 7: 최종 보스 (압도적인 사거리와 체력)
    public static final Stat GO_SEGU = new Stat(450, 60, 25, 1, 5, "천상의 화살", 1500, "고세구");

    // --- [일반 병사] ---
    // 공격 20 / 반격 10: 플레이어가 먼저 때려야 하는 이유를 명확히 함
    public static final Stat HADES_SOLDIER = new Stat(100, 20, 10, 1, 1, "일반 병사", 100, "하데스일반병사");
    public static final Stat ZEUS_SOLDIER = new Stat(100, 20, 10, 1, 1, "일반 병사", 100, "제우스일반병사");

    // --- [진영 공용 설정] ---
    public static final Color COLOR_HADES = Color.BLUE;
    public static final Color COLOR_ZEUS = Color.RED;

    // 하데스 영웅 선택 리스트
    public static final String[] NAMES_HADES = {"솜주먹", "키마", "챈나", "띵귤", "연초록"};
    public static final Stat[] STATS_HADES = {SOM_JUMEOK, KIMA, CHAN_NA, THING_GYUL, YEON_CHOROK};

    // 제우스 스테이지별 보스 리스트 (index 0~6 순서대로 스테이지 1~7 매칭)
    public static final String[] NAMES_ZEUS = {"데메테르", "헤스티아", "아테나", "아르테미스", "헤라", "아프로디테", "제우스"};
    public static final Stat[] STATS_ZEUS = {COCO_MI, HINU_HINU, YOON_IJE, HARU_BI, SI_MONG, MA_RONI, GO_SEGU};
}
