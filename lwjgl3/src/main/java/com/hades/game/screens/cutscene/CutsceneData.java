package com.hades.game.screens.cutscene;

// 각 스테이지의 이미지, 대사, 그리고 배경음악 경로를 담는 불변 데이터 객체입니다.
public record CutsceneData(String[] imagePaths, String[] scripts, String bgmPath) {
}
