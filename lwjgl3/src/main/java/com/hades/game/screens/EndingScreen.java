package com.hades.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.hades.game.HadesGame;
import com.hades.game.constants.GameConfig;
import com.hades.game.view.UI;

// Chess Olympus: HADES vs ZEUS - 게임 엔딩 화면
public class EndingScreen extends ScreenAdapter {
    private final HadesGame game;
    private Stage stage;

    public EndingScreen(HadesGame game) {
        this.game = game;
        this.stage = new Stage(new FitViewport(GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT));
        initUI();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    private void initUI() {
        Table table = new Table();
        table.setFillParent(true);

        // 엔딩 타이틀 및 승리 메시지
        Label title = new Label("THE END...", new Label.LabelStyle(game.titleFont, Color.GOLD));
        Label msg = new Label("5명의 영웅들은 지옥의 질서를 다시 세웠다.", new Label.LabelStyle(game.mainFont, Color.WHITE));

        // [추가] 기록 확인 버튼
        Label scoreBtn = new Label("[ 명예의전당 (기록확인) ]", new Label.LabelStyle(game.mainFont, Color.CYAN));
        UI.addHoverEffect(game, scoreBtn, Color.CYAN, Color.WHITE);
        scoreBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.playClick();
                game.setScreen(new ScoreScreen(game)); // 기록 화면으로 이동
            }
        });

        // 홈 버튼
        Label homeBtn = new Label("[ 홈으로 돌아가기 ]", new Label.LabelStyle(game.mainFont, Color.WHITE));
        UI.addHoverEffect(game, homeBtn, Color.WHITE, Color.GOLD);
        homeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.playClick();
                game.audioManager.stopBgm();
                game.audioManager.playBgm("music/ending.mp3");
                game.setScreen(new MenuScreen(game));
            }
        });

        // 제작자 인사말
        String thanksText = "플레이해주셔서 감사합니다.\n" +
            "다음에 더 재밌는 팬게임으로 찾아오겠습니다.\n" +
            "문의사항/피드백 : 데브케이(fatking25@kakao.com)";
        Label thanksMsg = new Label(thanksText, new Label.LabelStyle(game.unitFont, Color.LIGHT_GRAY));
        thanksMsg.setAlignment(Align.center);

        table.add(title).padBottom(20).row();
        table.add(msg).padBottom(60).row();
        table.add(scoreBtn).padBottom(30).row(); // 기록 확인 버튼 배치
        table.add(homeBtn).padBottom(70).row();
        table.add(thanksMsg);

        stage.addActor(table);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
