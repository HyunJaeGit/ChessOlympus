package com.hades.game.constants;

import com.badlogic.gdx.utils.Array;
import java.util.HashMap;
import java.util.Map;

public class SkillData {
    // 스킬 정보를 담는 내부 클래스
    public static class Skill {
        public final String name;
        public final String description;
        public final float power; // 대미지 계수
        public final int range;   // 발동 범위
        public final boolean isAoE; // 광역 공격 여부

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
        // 하데스 권능: 플레이어가 업그레이드 화면에서 획득 가능한 목록
        addSkill("지옥의 숨결", "주변 1칸 모든 적에게 화염 피해 (계수:1.8)", 1.8f, 1, true);
        addSkill("삼두견의 포효", "주변 2칸 모든 적에게 피해 (계수:1.2)", 1.2f, 2, true);
        addSkill("연옥의 불꽃", "단일 대상에게 강력한 일격 (계수:2.5)", 2.5f, 1, false);
        addSkill("그림자 습격", "먼 거리의 적을 기습 (계수:1.5)", 1.5f, 3, false);
        addSkill("영혼 흡수", "피해를 입히고 체력 일부 회복 (계수:1.3)", 1.3f, 1, false);
        addSkill("심연의 고리", "주변 3칸 내 적들에게 약한 피해 (계수:0.8)", 0.8f, 3, true);
        addSkill("망자의 원한", "자신 주변에 강력한 저주 폭발 (계수:2.0)", 2.0f, 1, true);

        // 제우스 진영 권능: AI 전용 (랜덤 목록에서 제외됨)
        addSkill("심판의 번개", "체력이 높은 적 타격", 2.5f, 99, false);
        addSkill("올림푸스의 가호", "자신 주변 아군 치유", 1.5f, 1, true);
    }

    private static void addSkill(String name, String description, float power, int range, boolean isAoE) {
        skills.put(name, new Skill(name, description, power, range, isAoE));
    }

    // 중복 및 특정 키워드(적 전용)를 제외한 랜덤 권능 리스트 반환
    public static Array<String> getRandomSkills(int count, Array<String> learnedSkills) {
        Array<String> pool = new Array<>();
        for (String name : skills.keySet()) {
            // 1. 이미 배운 스킬 리스트에 포함되어 있지 않아야 함
            // 2. 적 전용 키워드(심판, 올림푸스)를 포함하지 않아야 함
            // 3. '기본 공격'이 아니어야 함
            if (!learnedSkills.contains(name, false) &&
                !name.contains("심판") &&
                !name.contains("올림푸스") &&
                !name.equals("기본 공격")) {
                pool.add(name);
            }
        }

        // 목록을 섞어서 무작위성 확보
        pool.shuffle();

        Array<String> result = new Array<>();
        // 요청받은 count(2개) 만큼 결과 배열에 담기
        for (int i = 0; i < Math.min(count, pool.size); i++) {
            result.add(pool.get(i));
        }
        return result;
    }

    public static Skill get(String skillName) {
        return skills.getOrDefault(skillName, new Skill("기본 공격", "적에게 타격", 1.0f, 1, false));
    }
}
