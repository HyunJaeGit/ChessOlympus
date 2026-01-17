package com.hades.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.hades.game.HadesGame;
import com.hades.game.constants.GameConfig;
import com.hades.game.screens.cutscene.BaseCutsceneScreen;
import com.hades.game.screens.cutscene.CutsceneManager;
import com.hades.game.view.UI; // 전역 UI 클래스 임포트

public class MenuScreen extends ScreenAdapter {
    private final HadesGame game;
    private Stage stage;
    private Texture backgroundTexture;

    private Label volStatusLabel;
    private Label screenBtn;
    private int volumeStep;

    private final Color COLOR_GOLD = Color.valueOf("D4AF37");
    private final Color COLOR_MAIN = Color.valueOf("E0E0E0");
    private final Color COLOR_SUB  = Color.valueOf("7F8C8D");
    private final Color COLOR_POINT = Color.valueOf("4FB9AF");

    public MenuScreen(HadesGame game) {
        this.game = game;
        this.volumeStep = (int)(game.globalVolume * 10);
        this.stage = new Stage(new FitViewport(GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT));
        this.backgroundTexture = new Texture(Gdx.files.internal("images/background/main.png"));

        initUI();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        if (game.battleBgm != null) game.battleBgm.stop();
        if (game.menuBgm != null && !game.menuBgm.isPlaying()) {
            game.menuBgm.setVolume(game.globalVolume);
            game.menuBgm.play();
        }
    }

    private void initUI() {
        Table mainTable = new Table();
        mainTable.setFillParent(true);
        mainTable.top().padTop(100);
        stage.addActor(mainTable);

        // 1. 타이틀 섹션
        mainTable.add(new Label("CHESS OLYMPUS", new Label.LabelStyle(game.titleFont, COLOR_GOLD))).padBottom(5).row();
        mainTable.add(new Label("HADES VS ZEUS", new Label.LabelStyle(game.subtitleFont, COLOR_SUB))).padBottom(60).row();

        // 2. 중간 메뉴 그룹 (Compact Table)
        Table menuGroup = new Table();

        // --- BGM 설정 행 ---
        Table volumeRow = new Table();
        volStatusLabel = new Label((volumeStep * 10) + "%", new Label.LabelStyle(game.mainFont, COLOR_POINT));
        Label volDown = new Label(" - ", new Label.LabelStyle(game.mainFont, COLOR_MAIN));
        Label volUp = new Label(" + ", new Label.LabelStyle(game.mainFont, COLOR_MAIN));

        UI.addHoverEffect(game, volDown, COLOR_MAIN, COLOR_GOLD);
        UI.addHoverEffect(game, volUp, COLOR_MAIN, COLOR_GOLD);

        // 볼륨 클릭 리스너 추가
        volUp.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (volumeStep < 10) { volumeStep++; syncVolume(); game.playClick(1.2f); }
            }
        });
        volDown.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (volumeStep > 0) { volumeStep--; syncVolume(); game.playClick(0.8f); }
            }
        });

        volumeRow.add(new Label("BGM", new Label.LabelStyle(game.mainFont, COLOR_MAIN))).padRight(20);
        volumeRow.add(volDown).padRight(10);
        volumeRow.add(volStatusLabel).width(80).align(Align.center);
        volumeRow.add(volUp).padLeft(10);
        menuGroup.add(volumeRow).padBottom(15).row();

        // --- 화면 모드 버튼 행 (문제 해결 포인트) ---
        screenBtn = new Label(Gdx.graphics.isFullscreen() ? "WINDOW" : "FULLSCREEN",
            new Label.LabelStyle(game.detailFont2, COLOR_POINT));
        UI.addHoverEffect(game, screenBtn, COLOR_POINT, COLOR_MAIN);
        // 클릭 리스너 추가
        screenBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.playClick(1.0f);
                toggleFullscreen();
            }
        });
        menuGroup.add(screenBtn).padBottom(15).row();

        // --- 게임 시작 버튼 ---
        Label startBtn = new Label("GAME START",
            new Label.LabelStyle(game.detailFont2, COLOR_MAIN));
        UI.addHoverEffect(game, startBtn, COLOR_MAIN, COLOR_GOLD);
        startBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.playClick(1.0f);

                // 게임 시작 시 메뉴 배경음악을 정지하여 음악 중첩 방지
                if (game.menuBgm != null) {
                    game.menuBgm.stop();
                }

                game.setScreen(new BaseCutsceneScreen(
                    game,
                    CutsceneManager.getIntroData(),
                    new HeroSelectionScreen(game, "HADES", game.menuBgm)
                ));
            }
        });
        menuGroup.add(startBtn).padBottom(15).row();

        // --- 종료 버튼 ---
        Label exitBtn = new Label("EXIT GAME",
            new Label.LabelStyle(game.detailFont2, COLOR_SUB));
        UI.addHoverEffect(game, exitBtn, COLOR_SUB, Color.FIREBRICK);
        exitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) { Gdx.app.exit(); }
        });
        menuGroup.add(exitBtn).row();

        mainTable.add(menuGroup).center();

        // 3. 하단 저작권 정보
        Table bottomTable = new Table();
        bottomTable.setFillParent(true);
        bottomTable.bottom().padBottom(30);
        stage.addActor(bottomTable);

        Label infoLabel = new Label(
            "비영리/비상업용 팬게임이며 수익창출/2차창작을 금지합니다.\n " +
                "모든 권리는 제작자 '데브케이'에 있습니다." +
                "\n문의 : fatking25@kakao.com",
            new Label.LabelStyle(game.detailFont, new Color(0.4f, 0.4f, 0.4f, 1f))
        );
        infoLabel.setAlignment(Align.center);
        bottomTable.add(infoLabel);
    }

    private void toggleFullscreen() {
        if (Gdx.graphics.isFullscreen()) {
            Gdx.graphics.setWindowedMode((int) GameConfig.VIRTUAL_WIDTH, (int) GameConfig.VIRTUAL_HEIGHT);
            screenBtn.setText("전체화면 전환");
        } else {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
            screenBtn.setText("창모드 전환");
        }
    }

    private void syncVolume() {
        game.updateVolume(volumeStep / 10f);
        volStatusLabel.setText((volumeStep * 10) + "%");
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        game.batch.setProjectionMatrix(stage.getViewport().getCamera().combined);
        game.batch.begin();
        game.batch.setColor(0.4f, 0.4f, 0.4f, 1f);
        game.batch.draw(backgroundTexture, 0, 0, GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT);
        game.batch.setColor(Color.WHITE);
        game.batch.end();
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int w, int h) { stage.getViewport().update(w, h, true); }

    @Override
    public void hide() { Gdx.input.setInputProcessor(null); }

    @Override
    public void dispose() {
        stage.dispose();
        backgroundTexture.dispose();
    }
}
