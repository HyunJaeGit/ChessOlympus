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
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.hades.game.HadesGame;
import com.hades.game.constants.GameConfig;

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

        // 제작자 인사말
        String thanksText = "플레이해주셔서 감사합니다.\n" +
            "다음에 더 재밌는 팬게임으로 찾아오겠습니다.\n" +
            "무수나 하데스 관련해서 원하시는 장르나 소재 있으시면 피드백 주세요.\n" +
            "문의사항/피드백 : 데브케이(fatking25@kakao.com)";
        Label thanksMsg = new Label(thanksText, new Label.LabelStyle(game.unitFont, Color.LIGHT_GRAY));
        thanksMsg.setAlignment(com.badlogic.gdx.utils.Align.center);

        // 홈 버튼
        Label homeBtn = new Label("[ 홈으로 돌아가기 ]", new Label.LabelStyle(game.mainFont, Color.WHITE));
        homeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.playClick();

                // [수정] AudioManager를 사용하여 모든 배경음악 정지
                game.audioManager.stopBgm();

                // [수정] 메뉴 배경음악 재생 후 메인 화면으로 이동 (파일명은 music/menu_theme.mp3로 가정)
                game.audioManager.playBgm("music/ending.mp3");

                game.setScreen(new MenuScreen(game));
            }
        });

        table.add(title).padBottom(20).row();
        table.add(msg).padBottom(60).row();
        table.add(thanksMsg).padBottom(70).row();
        table.add(homeBtn);

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
