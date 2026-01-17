package com.hades.game.screens.cutscene;

import java.util.HashMap;
import java.util.Map;

// 각 스테이지별 컷씬 데이터를 관리하는 매니저 클래스입니다.
public class CutsceneManager {
    private static final Map<Integer, CutsceneData> stageDataMap = new HashMap<>();

    static {
        // [Stage 0] 인트로: 게임의 시작을 알리는 서사입니다.
        stageDataMap.put(0, new CutsceneData(
            new String[]{
                "images/cutscene/intro-1.png",
                "images/cutscene/intro-2.png",
                "images/cutscene/intro-3.png",
                "images/cutscene/intro-4.png"
            },
            new String[]{
                "905년 전,\n 올림포스의 꼭대기...",
                "제우스는 지옥의 왕 하데스를 반역자로 선포했다.",
                "신들의 전쟁에서 패배한 하데스는 완전한 음지에 갇혔고,",
                "지옥의 영웅들은 빼앗긴 권능을 되찾기 위한 반격을 시작한다."
            },
            "music/bgm.mp3"
        ));

        // [Stage 1] 아케론 - 보스: 데메테르
        stageDataMap.put(1, new CutsceneData(
            new String[]{"images/cutscene/stage-1.png"},
            new String[]{
                "지상으로 향하는 첫 관문, 아케론 강.",
                "풍요의 여신 '데메테르'의 결계가 \n영웅들의 힘을 억누르고 있습니다.",
                "권능을 봉인하고있는 데메테르를 굴복시켜야 합니다."
            },
            "music/stage1.mp3"
        ));

        // [Stage 2] 타르타로스 - 보스: 헤스티아
        stageDataMap.put(2, new CutsceneData(
            new String[]{"images/cutscene/stage-2.png"},
            new String[]{
                "첫 번째 결계를 부쉈지만, \n'헤스티아'가 타르타로스를 지키고 있습니다.",
                "그녀가 일으킨 뜨거운 화염의 장벽이 \n앞길을 막아섭니다.",
                "이제 해방된 지옥의 권능으로 \n불길을 잠재우고 전진하십시오!"
            },
            "music/stage3.mp3"
        ));

        // [Stage 3] 지혜의 회랑 - 보스: 아테나
        stageDataMap.put(3, new CutsceneData(
            new String[]{"images/cutscene/stage-3.png"},
            new String[]{
                "정적만이 흐르는 지혜의 회랑에 도달했습니다.",
                "전쟁과 지혜의 여신 \n'아테나'가 완벽한 전술로 당신들을 기다립니다.",
                "무력만으로는 그녀를 넘을 수 없습니다. \n지혜로운 수 싸움이 필요합니다."
            },
            "music/stage3.mp3"
        ));

        // [Stage 4] 달빛의 숲 - 보스: 아르테미스
        stageDataMap.put(4, new CutsceneData(
            new String[]{"images/cutscene/stage-4.png"},
            new String[]{
                "차가운 달빛이 비치는 신비로운 숲에 진입했습니다.",
                "달빛의 사냥꾼 \n'아르테미스'의 화살이 어둠 속에서 번뜩입니다.",
                "숲의 주인인 그녀는 \n침입자에게 단 한 치의 틈도 허용하지 않습니다."
            },
            "music/stage4.mp3"
        ));

        // [Stage 5] 영혼의 안식처 - 보스: 헤라
        stageDataMap.put(5, new CutsceneData(
            new String[]{"images/cutscene/stage-5.png"},
            new String[]{
                "고요한 영혼의 안식처, \n이곳의 주인은 신들의 여왕 '헤라'입니다.",
                "그녀의 압도적인 위압감이 영웅들의 마음을 뒤흔듭니다.",
                "망설이지 마십시오. \n왕좌로 향하는 길은 이제 얼마 남지 않았습니다."
            },
            "music/stage5.mp3"
        ));

        // [Stage 6] 황금 궁전 - 보스: 아프로디테
        stageDataMap.put(6, new CutsceneData(
            new String[]{"images/cutscene/stage-6.png"},
            new String[]{
                "올림포스의 심장, \n화려한 황금 궁전에 들어섰습니다.",
                "아름다움 속에 치명적인 독을 감춘 \n'아프로디테'가 앞길을 막아섭니다.",
                "그녀의 유혹적인 권능을 뿌리치고 \n신들의 왕에게 도전하십시오."
            },
            "music/stage6.mp3"
        ));

        // [Stage 7] 제우스의 옥좌 - 보스: 제우스
        stageDataMap.put(7, new CutsceneData(
            new String[]{"images/cutscene/stage-7.png"},
            new String[]{
                "올림포스의 가장 높은 곳, \n마침내 신들의 왕 '제우스'와 마주합니다.",
                "분노에 찬 번개가 하늘을 뒤덮습니다.",
                "\"지옥의 무리여, 너희의 반란은 여기서 끝이다.\"",
                "지옥의 운명을 결정지을... \n최후의 전쟁이 지금 시작됩니다!"
            },
            "music/stage7.mp3"
        ));

        // [Stage 8] 엔딩
        stageDataMap.put(8, new CutsceneData(
            new String[]{
                "images/background/ending-1.png",
                "images/background/ending-2.png",
                "images/background/ending-3.png",
                "images/background/ending-4.png"
            },
            new String[]{
                // CUTSCENE 1 — 무너진 하늘
                "올림포스의 옥좌가 갈라진다. 번개는 더 이상 내려오지 않는다.\n" +
                    "제우스는 무릎을 꿇고, 부서진 계단에 손을 짚는다.\n" +
                "세상은 멈춰 있었다. 이제, 다시 흐르려 한다.",

                // CUTSCENE 2 — 마지막 판단
                "지옥의 문이 열린다. 어둠은 바람처럼 스며든다.\n" +
                "탄생이 있다면, 끝도 있어야 한다.\n" +
                "빛은 폭발하지 않는다. 조용히 사라진다.",

                // CUTSCENE 3 — 다시 움직이는 세계
                "구름이 흐르고, 바람이 분다.\n" +
                "어딘가에서는 새로운 생명이 태어나고, 누군가는 눈을 감는다.\n" +
                "생명력이 넘치는 땅은 머무는 곳이 아니라, 지나가는 곳이 된다.",

                // CUTSCENE 4
                "전쟁은 지옥의 승리로 끝났다.\n" +
                "하지만, 다섯은 지배하지 않고, 다시 자신의 자리로 돌아갔다.\n" +
                "태어나는 것과, 떠나는 것..."
            },
            "music/ending.mp3"
        ));
    }

    // 요청받은 스테이지 번호에 해당하는 데이터를 반환합니다.
    public static CutsceneData getStageData(int stage) {
        return stageDataMap.getOrDefault(stage, stageDataMap.get(1));
    }

    // 메인 메뉴에서 인트로 시작 시 호출됩니다.
    public static CutsceneData getIntroData() {
        return stageDataMap.get(0);
    }

    // 영웅 선택 화면에서 1스테이지 진입 시 호출됩니다.
    public static CutsceneData getStage1Data() {
        return getStageData(1);
    }
}
