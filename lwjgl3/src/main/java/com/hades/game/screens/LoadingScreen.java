package com.hades.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.hades.game.HadesGame;

// Chess Olympus: HADES vs ZEUS - 로딩 화면
// 화면 전환 시 배경음악 중첩을 방지하고 리소스 정리 시간을 벌어주는 정거장 역할을 합니다.
public class LoadingScreen extends ScreenAdapter {
    private final HadesGame game;
    private final ScreenAdapter nextScreen; // 다음에 이동할 화면
    private float waitTimer = 0f;
    private final float TARGET_TIME = 0.6f; // 로딩 화면 유지 시간 (0.6초)

    // 생성자: 게임 인스턴스와 다음에 보여줄 스크린을 인자로 받습니다.
    public LoadingScreen(HadesGame game, ScreenAdapter nextScreen) {
        this.game = game;
        this.nextScreen = nextScreen;
    }

    @Override
    public void show() {
        // [핵심] 로딩 시작 시 모든 배경음악을 강제로 정지하여 중첩을 방지합니다.
        if (game.menuBgm != null) game.menuBgm.stop();
        if (game.battleBgm != null) game.battleBgm.stop();

        // 입력 프로세서를 null로 설정하여 로딩 중 조작을 방지합니다.
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void render(float delta) {
        // 1. 화면 초기화 (검은색)
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 2. 시간 계산
        waitTimer += delta;

        // 3. 텍스트 출력
        game.batch.begin();
        // 화면 중앙 좌표 계산하여 "Loading..." 출력
        String text = "Loading...";
        float x = (Gdx.graphics.getWidth() / 2f) - 50f;
        float y = (Gdx.graphics.getHeight() / 2f);
        game.mainFont.draw(game.batch, text, x, y);
        game.batch.end();

        // 4. 설정한 시간이 지나면 다음 화면으로 전환
        if (waitTimer >= TARGET_TIME) {
            game.setScreen(nextScreen);
        }
    }

    @Override
    public void hide() {
        // 로딩이 끝나면 리소스 점유를 해제하거나 필요한 처리를 합니다.
    }

    @Override
    public void dispose() {
        // 로딩 스크린 자체 리소스가 있다면 여기서 해제합니다.
    }
}
