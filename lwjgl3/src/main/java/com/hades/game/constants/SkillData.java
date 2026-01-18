package com.hades.game.constants;

import com.badlogic.gdx.utils.Array;
import java.util.HashMap;
import java.util.Map;

public class SkillData {
    public enum Shape { MANHATTAN, LINE, CROSS, SQUARE, GLOBAL }

    public static class Skill {
        public final String name;
        public final String description; // [추가] UI에서 출력하기 위해 반드시 필요합니다.
        public final float power;
        public final int range;
        public final boolean isAoE;
        public final Shape shape;

        public Skill(String name, String description, float power, int range, boolean isAoE, Shape shape) {
            this.name = name;
            this.description = description;
            this.power = power;
            this.range = range;
            this.isAoE = isAoE;
            this.shape = shape;
        }
    }

    private static final Map<String, Skill> skills = new HashMap<>();

    static {
        // --- [하데스 영웅 스킬] ---
        addSkill("연옥의 불꽃", "단일 대상에게 명계의 집중 화염 투하 (계수:3.0)", 3.0f, 1, false, Shape.MANHATTAN);
        addSkill("그림자 습격", "일직선 상의 적을 기습하여 강력한 타격 (계수:2.0)", 2.0f, 4, false, Shape.LINE);
        addSkill("지옥의 숨결", "주변 십자 범위 모든 적에게 업화 (계수:2.2)", 2.2f, 1, true, Shape.CROSS);
        addSkill("심연의 고리", "넓은 다이아몬드 범위의 적들을 잠식 (계수:1.3)", 1.3f, 3, true, Shape.MANHATTAN);
        addSkill("망자의 원한", "자신 주변 8칸에 원혼의 대폭발을 해방 (계수:2.5)", 2.5f, 1, true, Shape.SQUARE);

        // --- [제우스 영웅 스킬] ---
        addSkill("심판의 번개", "체력이 가장 낮은 적을 추격하는 벼락 (계수:1.5)", 1.0f, 99, false, Shape.GLOBAL);
        addSkill("매혹의 향기", "주변 8칸 아군을 성스러운 힘으로 치유 (계수:0.8)", 0.8f, 2, true, Shape.SQUARE);
        addSkill("여왕의 권위", "십자 방향 적들을 압박하여 위축시킴 (계수:1.1)", 1.1f, 2, true, Shape.CROSS);
        addSkill("달빛의 추격", "매우 긴 사거리에서 일직선 정밀 저격 (계수:1.3)", 1.3f, 6, false, Shape.LINE);
        addSkill("전략적 방벽", "공격과 동시에 피해를 줄이는 방어막 형성 (계수:1.0)", 1.0f, 1, false, Shape.MANHATTAN);
        addSkill("영겁의 화로", "지면을 불바다로 만들어 적의 접근 차단 (계수:1.2)", 1.2f, 3, true, Shape.MANHATTAN);
        addSkill("대지의 분노", "지진을 일으켜 주변 8칸 모든 적을 타격 (계수:0.8)", 0.8f, 2, true, Shape.SQUARE);
        addSkill("올림푸스의 가호", "성스러운 빛으로 아군을 대폭 회복 (계수:1.8)", 1.8f, 1, false, Shape.MANHATTAN);
    }

    private static void addSkill(String name, String desc, float power, int range, boolean isAoE, Shape shape) {
        skills.put(name, new Skill(name, desc, power, range, isAoE, shape));
    }

    // [복구] UpgradeScreen에서 랜덤 스킬 옵션을 뽑을 때 사용합니다.
    public static Array<String> getRandomSkills(int count, Array<String> learnedSkills) {
        Array<String> pool = new Array<>();
        for (String name : skills.keySet()) {
            // 이미 배운 스킬이 아니고, 보스 전용 스킬이 아닌 것들 중에서 선택
            if (!learnedSkills.contains(name, false) && !isBossSkill(name) && !name.equals("기본 공격")) {
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

    // 제우스 진영 영웅들이 사용하는 스킬인지 판별
    private static boolean isBossSkill(String name) {
        return name.contains("심판") || name.contains("매혹") || name.contains("여왕") ||
            name.contains("달빛") || name.contains("전략적") || name.contains("영겁") ||
            name.contains("대지") || name.contains("올림푸스");
    }

    public static Skill get(String skillName) {
        return skills.getOrDefault(skillName, new Skill("기본 공격", "적에게 타격", 1.0f, 1, false, Shape.MANHATTAN));
    }
}
