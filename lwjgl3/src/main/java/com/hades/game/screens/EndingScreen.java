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

// 게임 엔딩 화면
public class EndingScreen extends ScreenAdapter {
    private final HadesGame game;
    private Stage stage;

    public EndingScreen(HadesGame game) {
        this.game = game;
        this.stage = new Stage(new FitViewport(GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT));
        initUI();
    }

    // 화면이 활성화될 때마다 실행되며, 입력 프로세서를 스테이지로 고정합니다.
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

        // 홈
        Label homeBtn = new Label("[ 홈으로 돌아가기 ]", new Label.LabelStyle(game.mainFont, Color.WHITE));
        homeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.playClick();

                // 모든 음악 정지 및 리소스 해제
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

        // 테이블 배치: 타이틀, 메시지, 인사말, 버튼 순으로 구성
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

        // stage.act()에 delta를 전달해야 클릭 판정이 정확하게 일어납니다.
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        // 화면 크기 조정 시 뷰포트를 업데이트하여 터치 좌표 오차를 방지합니다.
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
