package com.hades.game.constants;

import java.util.HashMap;
import java.util.Map;

public class HeroStoryManager {
    public record HeroStory(String title, String description) {}

    private static final Map<String, HeroStory> stories = new HashMap<>();

    static {
        // 하데스 진영 영웅 스토리
        stories.put("솜주먹", new HeroStory(
            "연옥의 기사",
            "하데스에게 가장 충성스러운 기사.\n제우스의 봉인으로 인해 주먹에 깃든 연옥의 불꽃이 억제된 상태입니다."));
        stories.put("키마", new HeroStory(
            "그림자 추적자",
            "지옥의 틈새를 누비던 자.\n명계의 밑바닥으로 추락하며 고유의 기동력을 잃어버렸습니다."));
        stories.put("챈나", new HeroStory(
            "심연의 자객",
            "어둠 속에서 적을 처단하던 자.\n현재는 권능이 묶여 그림자 속에 숨는 것조차 버겁습니다."));
        stories.put("띵귤", new HeroStory(
            "망자의 인도자",
            "영혼들을 안식으로 이끌던 자.\n제우스가 선포한 '영생의 독재'로 인해 인도의 힘을 상실했습니다."));
        stories.put("연초록", new HeroStory(
            "영혼의 사수",
            "한 치의 오차도 없는 화살을 쏘던 자.\n달빛 아래서 사냥하던 감각이 봉인 속에 잠들어 있습니다."));
    }

    public static HeroStory get(String name) {
        // 등록되지 않은 이름(일반병 등)일 경우 기본값 리턴
        return stories.getOrDefault(name, new HeroStory("이름 없는 병사", "올림포스의 전쟁에 동원된 영혼입니다."));
    }
}
