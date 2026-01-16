package com.hades.game.screens.cutscene;

// record를 사용하여 불변 데이터 객체를 심플하게 정의합니다.
public record CutsceneData(String[] imagePaths, String[] scripts) {
}
