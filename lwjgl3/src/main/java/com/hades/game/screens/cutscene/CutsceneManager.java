package com.hades.game.screens.cutscene;

public class CutsceneManager {
    // 컷씬에 필요한 데이터 묶음 클래스
    public static class CutsceneData {
        public final String[] imagePaths;
        public final String[] texts;

        public CutsceneData(String[] imagePaths, String[] texts) {
            this.imagePaths = imagePaths;
            this.texts = texts;
        }
    }

    // 인트로 컷씬 데이터
    public static CutsceneData getIntroData() {
        return new CutsceneData(
            new String[]{
                "images/cutscene/intro-1.png",
                "images/cutscene/intro-2.png",
                "images/cutscene/intro-3.png",
                "images/cutscene/intro-4.png"
            },
            new String[]{
                "제우스의 강력한 권능으로 인해,\n세상의 모든 생명은 영생의 저주를 받았다.",
                "음지로 추락한 하데스.\n그들은 모든 권능을 잃고 지옥에서 여정을 시작한다.",
                "\"그들은 제우스에게 복수하기 위해서라면\"\n제우스와 그들을 지키는 올림포스의 신들을 이겨야 한다.",
                "권능을 되찾기 위해 선택한 여정...\n올림포스를 향한 그들의 여정이 시작된다."
            }
        );
    }

    // 스테이지 1 진입 데이터
    public static CutsceneData getStage1Data() {
        return new CutsceneData(
            new String[]{"images/cutscene/stage1_intro.png"},
            new String[]{"지상으로 향하는 첫 번째 관문, 아케론.\n데메테르가 차가운 겨울 꽃으로 길을 막아섰다."}
        );
    }

    // 추가 스테이지 데이터를 이곳에 계속 정의하면 됩니다.
}
