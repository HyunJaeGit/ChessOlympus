package com.hades.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.hades.game.HadesGame;
import com.hades.game.constants.GameConfig;

// 클래스 역할: HadesGame에 로드된 전역 BGM을 제어하며 메인 메뉴 인터페이스를 제공합니다.
public class MenuScreen extends ScreenAdapter {
    private final HadesGame game;
    private Stage stage;
    private Texture backgroundTexture;

    private Label volStatusLabel;
    private Label screenBtn;
    private int volumeStep = 1; // 기본 볼륨 10%

    private final Color COLOR_GOLD = Color.valueOf("D4AF37");
    private final Color COLOR_MAIN = Color.valueOf("E0E0E0");
    private final Color COLOR_SUB  = Color.valueOf("7F8C8D");
    private final Color COLOR_POINT = Color.valueOf("4FB9AF");

    public MenuScreen(HadesGame game) {
        this.game = game;
        this.stage = new Stage(new FitViewport(GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT));
        this.backgroundTexture = new Texture(Gdx.files.internal("images/background/main.png"));

        initUI();
    }

    // 화면이 나타날 때 음악 제어
    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);

        // [음악 제어] 배틀 음악 끄고 메뉴 음악 재생
        if (game.battleBgm != null) game.battleBgm.stop();
        if (game.menuBgm != null && !game.menuBgm.isPlaying()) {
            game.menuBgm.setVolume(volumeStep / 10f);
            game.menuBgm.play();
        }
    }

    private void initUI() {
        Table mainTable = new Table();
        mainTable.setFillParent(true);
        mainTable.center();
        stage.addActor(mainTable);

        // 1. 타이틀
        mainTable.add(new Label("CHESS OLYMPUS", new Label.LabelStyle(game.titleFont, COLOR_GOLD))).padBottom(5).row();
        mainTable.add(new Label("HADES VS ZEUS", new Label.LabelStyle(game.subtitleFont, COLOR_SUB))).padBottom(40).row();

        // 2. 볼륨 조절
        Table volumeTable = new Table();
        volStatusLabel = new Label((volumeStep * 10) + "%", new Label.LabelStyle(game.mainFont, COLOR_POINT));
        Label volDown = new Label(" [ - ] ", new Label.LabelStyle(game.mainFont, COLOR_MAIN));
        Label volUp = new Label(" [ + ] ", new Label.LabelStyle(game.mainFont, COLOR_MAIN));

        addHoverEffect(volDown, COLOR_MAIN, COLOR_GOLD);
        addHoverEffect(volUp, COLOR_MAIN, COLOR_GOLD);

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

        volumeTable.add(new Label("BGM ", new Label.LabelStyle(game.mainFont, COLOR_MAIN)));
        volumeTable.add(volStatusLabel).width(80).align(Align.center);
        volumeTable.add(volDown).padRight(5);
        volumeTable.add(volUp).padLeft(5);
        mainTable.add(volumeTable).padBottom(20).row();

        // 3. 화면 모드 버튼
        screenBtn = new Label(Gdx.graphics.isFullscreen() ? "[ 창모드 전환 ]" : "[ 전체화면 전환 ]", new Label.LabelStyle(game.mainFont, COLOR_POINT));
        addHoverEffect(screenBtn, COLOR_POINT, COLOR_MAIN);
        screenBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) { game.playClick(1.0f); toggleFullscreen(); }
        });
        mainTable.add(screenBtn).padBottom(30).row();

        // 4. 게임 시작
        Label startBtn = new Label("[ 게임 시작 ]", new Label.LabelStyle(game.mainFont, COLOR_MAIN));
        addHoverEffect(startBtn, COLOR_MAIN, COLOR_GOLD);
        startBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.playClick(1.0f);
                // [수정] 음악 객체 전달 방식 변경 (필요 시 game.menuBgm 전달)
                game.setScreen(new HeroSelectionScreen(game, "HADES", game.menuBgm));
            }
        });
        mainTable.add(startBtn).padBottom(20).row();

        // 5. 종료
        Label exitBtn = new Label("[ 게임 종료 ]", new Label.LabelStyle(game.mainFont, COLOR_SUB));
        addHoverEffect(exitBtn, COLOR_SUB, Color.FIREBRICK);
        exitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) { Gdx.app.exit(); }
        });
        mainTable.add(exitBtn).padBottom(60).row();

        // 하단 저작권 정보
        Table bottomTable = new Table();
        bottomTable.setFillParent(true);
        bottomTable.bottom().padBottom(20);
        stage.addActor(bottomTable);

        Label infoLabel = new Label(
            "비영리/비홍보용 팬게임이며, 수익창출 및 무단 수정 배포를 금지합니다.\n" +
                "모든 권리는 제작자 '데브케이'에 있습니다.\n" +
                "문의 : fatking25@kakao.com",
            new Label.LabelStyle(game.detailFont, Color.valueOf("555555"))
        );
        infoLabel.setAlignment(Align.center);
        bottomTable.add(infoLabel);
    }

    private void toggleFullscreen() {
        if (Gdx.graphics.isFullscreen()) {
            Gdx.graphics.setWindowedMode((int) GameConfig.VIRTUAL_WIDTH, (int) GameConfig.VIRTUAL_HEIGHT);
            screenBtn.setText("[ 전체화면 전환 ]");
        } else {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
            screenBtn.setText("[ 창모드 전환 ]");
        }
    }

    private void syncVolume() {
        if (game.menuBgm != null) {
            game.menuBgm.setVolume(volumeStep / 10f);
        }
        volStatusLabel.setText((volumeStep * 10) + "%");
    }

    private void addHoverEffect(final Label label, final Color original, final Color hover) {
        label.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor from) { label.setColor(hover); }
            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor to) { label.setColor(original); }
        });
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
        // [주의] game.menuBgm은 HadesGame에서 dispose하므로 여기선 하지 않습니다.
    }
}
