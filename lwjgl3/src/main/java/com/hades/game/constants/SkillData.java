package com.hades.game.constants;

import java.util.HashMap;
import java.util.Map;

public class SkillData {
    // 스킬 정보를 담는 내부 클래스
    public static class Skill {
        public final String name;
        public final String description;
        public final float power; // 대미지 계수나 지속 시간 등

        public Skill(String name, String description, float power) {
            this.name = name;
            this.description = description;
            this.power = power;
        }
    }

    // 모든 스킬을 이름을 키값으로 저장하는 저장소
    private static final Map<String, Skill> skills = new HashMap<>();

    static {
        // 하데스 진영 스킬
        addSkill("지옥의 숨결", "전방의 모든 적에게 강력한 화염 피해를 입힙니다.", 1.5f);
        addSkill("삼두견의 포효", "주변 적들을 공포에 빠뜨려 3초간 기절시킵니다.", 3.0f);

        // 제우스 진영 스킬
        addSkill("심판의 번개", "가장 강력한 적에게 거대한 낙뢰를 떨어뜨립니다.", 2.5f);
        addSkill("올림푸스의 가호", "자신과 주변 아군의 방어력을 일시적으로 높입니다.", 1.2f);
    }

    private static void addSkill(String name, String description, float power) {
        skills.put(name, new Skill(name, description, power));
    }

    // 스킬 이름을 넣으면 스킬 객체를 반환하는 메서드
    public static Skill get(String skillName) {
        return skills.getOrDefault(skillName, new Skill("기본 공격", "적에게 물리 타격을 입힙니다.", 1.0f));
    }
}
