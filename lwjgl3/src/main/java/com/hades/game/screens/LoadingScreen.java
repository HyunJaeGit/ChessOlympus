package com.hades.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.hades.game.HadesGame;

// Chess Olympus: HADES vs ZEUS - 로딩 화면
public class LoadingScreen extends ScreenAdapter {
    private final HadesGame game;
    private final ScreenAdapter nextScreen;
    private float waitTimer = 0f;
    private final float TARGET_TIME = 0.6f;

    public LoadingScreen(HadesGame game, ScreenAdapter nextScreen) {
        this.game = game;
        this.nextScreen = nextScreen;
    }

    @Override
    public void show() {
        // [수정] 로딩 시작 시 AudioManager를 통해 모든 배경음악 정지
        game.audioManager.stopBgm();

        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        waitTimer += delta;

        game.batch.begin();
        String text = "Loading...";
        float x = (Gdx.graphics.getWidth() / 2f) - 50f;
        float y = (Gdx.graphics.getHeight() / 2f);
        game.mainFont.draw(game.batch, text, x, y);
        game.batch.end();

        if (waitTimer >= TARGET_TIME) {
            game.setScreen(nextScreen);
        }
    }

    @Override
    public void hide() {}

    @Override
    public void dispose() {}
}
