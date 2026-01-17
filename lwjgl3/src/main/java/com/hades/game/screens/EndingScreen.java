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

public class EndingScreen extends ScreenAdapter {
    private final HadesGame game;
    private Stage stage;

    public EndingScreen(HadesGame game) {
        this.game = game;
        this.stage = new Stage(new FitViewport(GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT));
        initUI();
    }

    private void initUI() {
        Table table = new Table();
        table.setFillParent(true);

        // 엔딩 타이틀 및 승리 메시지
        Label title = new Label("THE END...", new Label.LabelStyle(game.titleFont, Color.GOLD));
        Label msg = new Label("5명의 영웅들은 지옥의 질서를 다시 세웠습니다.", new Label.LabelStyle(game.mainFont, Color.WHITE));

        // 제작자 인사말 추가
        String thanksText = "플레이해주셔서 감사합니다.\n" +
            "다음에 더 재밌는 팬게임으로 찾아오겠습니다.\n" +
            "무수나 하데스 관련해서 원하시는 장르나 소재 있으시면 피드백 주세요.\n" +
            "문의사항/피드백 : 데브케이(fatking25@kakao.com)";
        Label thanksMsg = new Label(thanksText, new Label.LabelStyle(game.unitFont, Color.LIGHT_GRAY));
        thanksMsg.setAlignment(com.badlogic.gdx.utils.Align.center); // 텍스트 중앙 정렬

        // 홈으로 버튼
        Label homeBtn = new Label("[ 홈으로 돌아가기 ]", new Label.LabelStyle(game.mainFont, Color.WHITE));
        // 호버 효과 추가
        homeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.playClick();

                // 모든 음악 정지 및 초기화
                if (game.battleBgm != null) game.battleBgm.stop();
                if (game.currentBgm != null) {
                    game.currentBgm.stop();
                    game.currentBgm.dispose();
                    game.currentBgm = null;
                }

                // 메뉴 배경음악 재생 후 메인 화면으로 이동
                if (game.menuBgm != null) {
                    game.menuBgm.setVolume(game.globalVolume);
                    if (!game.menuBgm.isPlaying()) game.menuBgm.play();
                }

                game.setScreen(new MenuScreen(game));
            }
        });

        // 테이블에 요소 배치
        table.add(title).padBottom(20).row();
        table.add(msg).padBottom(60).row();
        table.add(thanksMsg).padBottom(70).row(); // 제작자 인사 추가
        table.add(homeBtn);

        stage.addActor(table);
        Gdx.input.setInputProcessor(stage);
    }
    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act();
        stage.draw();
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
