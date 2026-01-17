package com.hades.game.constants;

import com.badlogic.gdx.utils.Array;
import java.util.HashMap;
import java.util.Map;

public class SkillData {
    public static class Skill {
        public final String name;
        public final String description;
        public final float power;
        public final int range;
        public final boolean isAoE;

        public Skill(String name, String description, float power, int range, boolean isAoE) {
            this.name = name;
            this.description = description;
            this.power = power;
            this.range = range;
            this.isAoE = isAoE;
        }
    }

    private static final Map<String, Skill> skills = new HashMap<>();

    static {
        // --- [하데스 진영 권능] ---
        addSkill("지옥의 숨결", "주변 1칸 모든 적에게 화염 피해 (계수:1.2)", 1.2f, 1, true);
        addSkill("삼두견의 포효", "주변 2칸 모든 적에게 피해 (계수:0.8)", 0.8f, 2, true);
        addSkill("연옥의 불꽃", "단일 대상에게 강력한 일격 (계수:1.7)", 1.7f, 1, false);
        addSkill("그림자 습격", "먼 거리의 적을 기습 (계수:1.0)", 1.0f, 3, false);
        addSkill("영혼 흡수", "피해를 입히고 체력 일부 회복 (계수:0.9)", 0.9f, 1, false);
        addSkill("심연의 고리", "주변 3칸 내 적들에게 약한 피해 (계수:0.6)", 0.6f, 3, true);
        addSkill("망자의 원한", "자신 주변에 강력한 저주 폭발 (계수:1.4)", 1.4f, 1, true);

        // --- [제우스 진영: 7단계 보스 전용 권능] ---
        // 1. 제우스: 압도적인 맵 전체 타격
        addSkill("심판의 번개", "전장의 모든 적 중 하나에게 번개 투하 (계수:1.8)", 1.8f, 99, false);

        // 2. 아프로디테: 공격과 동시에 주변 치유
        addSkill("매혹의 향기", "적 타격 시 주변 아군 치유 (계수:1.2)", 1.2f, 2, true);

        // 3. 헤라: 이동 봉쇄 및 약화
        addSkill("여왕의 권위", "공격 시 대상의 이동력을 0으로 고정 (계수:1.3)", 1.3f, 2, false);

        // 4. 아르테미스: 원거리 관통 저격
        addSkill("달빛의 추격", "원거리의 적을 꿰뚫는 저격 (계수:1.5)", 1.5f, 4, false);

        // 5. 아테나: 완벽한 수비와 반격
        addSkill("전략적 방벽", "공격 후 자신의 피해를 최소화 (계수:1.1)", 1.1f, 1, false);

        // 6. 헤스티아: 불바다 생성 (지속딜 컨셉)
        addSkill("영겁의 화로", "공격 지점 주변에 화염 지대 생성 (계수:1.4)", 1.4f, 2, true);

        // 7. 데메테르: 대지의 속박 (범위 감속)
        addSkill("대지의 속박", "주변 적들의 발을 묶는 대지의 힘 (계수:1.0)", 1.0f, 2, true);

        // 보조용
        addSkill("올림푸스의 가호", "자신 주변 아군 치유 (계수:1.2)", 1.2f, 1, true);
    }

    private static void addSkill(String name, String description, float power, int range, boolean isAoE) {
        skills.put(name, new Skill(name, description, power, range, isAoE));
    }

    public static Array<String> getRandomSkills(int count, Array<String> learnedSkills) {
        Array<String> pool = new Array<>();
        for (String name : skills.keySet()) {
            // 필터링: 이미 배움 / 보스 전용 키워드 / 기본 공격 제외
            if (!learnedSkills.contains(name, false) &&
                !isBossSkill(name) &&
                !name.equals("기본 공격")) {
                pool.add(name);
            }
        }
        pool.shuffle();
        Array<String> result = new Array<>();
        for (int i = 0; i < Math.min(count, pool.size); i++) {
            result.add(pool.get(i));
        }
        return result;
    }

    // 보스 전용 스킬인지 판별하는 헬퍼 메서드
    private static boolean isBossSkill(String name) {
        return name.contains("심판") || name.contains("매혹") || name.contains("여왕") ||
            name.contains("달빛") || name.contains("전략적") || name.contains("영겁") ||
            name.contains("대지") || name.contains("올림푸스");
    }

    public static Skill get(String skillName) {
        return skills.getOrDefault(skillName, new Skill("기본 공격", "적에게 타격", 1.0f, 1, false));
    }
}
