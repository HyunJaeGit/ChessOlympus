package com.hades.game.screens.cutscene;

import java.util.HashMap;
import java.util.Map;

public class CutsceneManager {
    // 스테이지 번호를 키로 하여 컷씬 데이터를 보관하는 저장소입니다.
    private static final Map<Integer, CutsceneData> stageDataMap = new HashMap<>();

    static {
        // [Stage 0] 인트로 데이터 - 이 부분이 없어서 NullPointerException이 발생했습니다.
        stageDataMap.put(0, new CutsceneData(
            new String[]{"images/cutscene/intro-1.png"},
            new String[]{
                "올림포스의 꼭대기, 제우스는 명계의 왕 하데스를 반역자로 선포했다.",
                "신들의 전쟁에서 패배한 하데스는 깊은 연옥에 갇혔고,",
                "이제 영웅들은 빼앗긴 권능을 되찾기 위한 반격을 시작한다."
            }
        ));

        // [Stage 1] 아케론 - 스킬 봉인 상태
        stageDataMap.put(1, new CutsceneData(
            new String[]{"images/cutscene/stage-1.png"},
            new String[]{
                "지상으로 향하는 첫 번째 관문, 아케론.",
                "제우스의 인장에 의해 영웅들의 권능은 봉인된 상태입니다.",
                "오직 전략과 무력으로 이 관문을 돌파해야 합니다."
            }
        ));

        // [Stage 2] 타르타로스 - 스킬 각성
        stageDataMap.put(2, new CutsceneData(
            new String[]{"images/cutscene/stage-2.png"},
            new String[]{
                "첫 번째 인장이 파괴되며 영웅들의 고유 권능이 깨어났습니다!",
                "하지만 두 번째 관문, 타르타로스의 뜨거운 열기가 앞길을 막습니다.",
                "더욱 강력해진 적들을 상대로 각성한 힘을 시험하십시오."
            }
        ));

        // [Stage 3] 지혜의 회랑
        stageDataMap.put(3, new CutsceneData(
            new String[]{"images/cutscene/stage-3.png"},
            new String[]{
                "시련을 넘어 도착한 곳은 끝없는 지혜의 회랑.",
                "올림포스의 전술가들이 당신들의 의지를 시험하려 합니다.",
                "지혜로운 전술만이 이 회랑의 끝으로 인도할 것입니다."
            }
        ));

        // [Stage 4] 달빛의 숲
        stageDataMap.put(4, new CutsceneData(
            new String[]{"images/cutscene/stage-4.png"},
            new String[]{
                "회랑을 지나자 차가운 달빛이 쏟아지는 신비로운 숲이 나타납니다.",
                "어둠 속에서 날아오는 보이지 않는 위협을 경계하십시오.",
                "숲의 주인은 침입자를 절대 용서하지 않습니다."
            }
        ));

        // [Stage 5] 영혼의 안식처
        stageDataMap.put(5, new CutsceneData(
            new String[]{"images/cutscene/stage-5.png"},
            new String[]{
                "숲의 끝에서 마주한 곳은 고요한 영혼의 안식처.",
                "심연의 울림이 영웅들의 정신을 흔들어 놓습니다.",
                "망설임은 곧 죽음입니다. 앞만 보고 나아가십시오."
            }
        ));

        // [Stage 6] 황금 궁전
        stageDataMap.put(6, new CutsceneData(
            new String[]{"images/cutscene/stage-6.png"},
            new String[]{
                "마침내 올림포스의 황금 궁전이 눈앞에 모습을 드러냈습니다.",
                "제우스의 권능이 가장 강력하게 미치는 성역입니다.",
                "마지막 인장을 파괴해야만 신들의 왕에게 닿을 수 있습니다."
            }
        ));

        // [Stage 7] 제우스의 옥좌
        stageDataMap.put(7, new CutsceneData(
            new String[]{"images/cutscene/stage-7.png"},
            new String[]{
                "마침내 올림포스의 정점, 제우스의 옥좌 앞입니다.",
                "모든 인장을 파괴한 영웅들 앞에 신들의 왕이 위엄을 드러냅니다.",
                "\"하데스의 아이들이여, 너희의 반역은 여기서 끝이다.\"",
                "명계의 운명을 건 최후의 결전이 지금 시작됩니다."
            }
        ));
    }

    // 요청받은 스테이지 번호에 해당하는 데이터를 반환합니다.
    public static CutsceneData getStageData(int stage) {
        return stageDataMap.getOrDefault(stage, stageDataMap.get(1));
    }
    // MenuScreen에서 호출하는 메서드
    public static CutsceneData getIntroData() {
        return stageDataMap.get(0);
    }
    // 1스테이지 데이터는 빈번하게 호출되므로 별도 메서드로 제공합니다.
    public static CutsceneData getStage1Data() {
        return getStageData(1);
    }
}
