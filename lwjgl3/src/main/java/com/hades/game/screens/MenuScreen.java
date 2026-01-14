package com.hades.game.screens;

import com.badlogic.gdx.Gdx;
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
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.hades.game.HadesGame;
import com.hades.game.constants.GameConfig; // GameConfig 추가

// 클래스 역할: 메인 메뉴 화면을 담당하며 UI 이벤트, 배경음악, 효과음을 처리합니다.
public class MenuScreen extends ScreenAdapter {
    private final HadesGame game;
    private Stage stage;
    private Color bgColor;
    private Texture backgroundTexture;
    private com.badlogic.gdx.audio.Music backgroundMusic;
    private Label volStatusLabel;

    // 볼륨 연산 오차 방지를 위해 0~10 단계의 정수형 변수로 상태를 관리합니다.
    private int volumeStep = 2;

    public MenuScreen(HadesGame game) {
        this.game = game;

        // 수정: HadesGame.VIRTUAL_WIDTH 대신 GameConfig를 참조합니다.
        this.stage = new Stage(new FitViewport(GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT));

        this.backgroundTexture = new Texture(Gdx.files.internal("images/background/main.png"));
        this.bgColor = new Color(0.05f, 0.05f, 0.1f, 1);

        initMusic();
        Gdx.input.setInputProcessor(stage);
        initUI();
    }

    // 메서드 설명: 배경음악 리소스를 로드하고 초기 볼륨을 설정합니다.
    private void initMusic() {
        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("music/bgm.mp3"));
        backgroundMusic.setLooping(true);
        backgroundMusic.setVolume(volumeStep / 10f);
        backgroundMusic.play();
    }

    // 메서드 설명: 메뉴 화면의 UI 요소를 배치하고 이벤트를 연결합니다.
    private void initUI() {
        Table mainTable = new Table();
        mainTable.setFillParent(true);
        mainTable.setTransform(true);
        mainTable.center();
        stage.addActor(mainTable);

        // 1. 타이틀 영역
        Label titleLabel = new Label("CHESS OLYMPUS", new Label.LabelStyle(game.titleFont, Color.GOLD));
        Label subtitleLabel = new Label("HADES VS ZEUS", new Label.LabelStyle(game.subtitleFont, Color.LIGHT_GRAY));
        mainTable.add(titleLabel).padBottom(5).row();
        mainTable.add(subtitleLabel).padBottom(40).row();

        // 1-1. 볼륨 조절 영역
        Table volumeTable = new Table();
        Label volLabel = new Label("BGM VOLUME", new Label.LabelStyle(game.mainFont, Color.WHITE));
        Label volUp = new Label(" [ + ] ", new Label.LabelStyle(game.mainFont, Color.LIGHT_GRAY));
        Label volDown = new Label(" [ - ] ", new Label.LabelStyle(game.mainFont, Color.LIGHT_GRAY));
        volStatusLabel = new Label((volumeStep * 10) + "%", new Label.LabelStyle(game.mainFont, Color.WHITE));

        addHoverEffect(volDown, Color.LIGHT_GRAY, Color.WHITE);
        addHoverEffect(volUp, Color.LIGHT_GRAY, Color.WHITE);

        volUp.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (volumeStep < 10) {
                    volumeStep++;
                    syncVolume();
                    game.playClick(1.2f);
                }
            }
        });

        volDown.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (volumeStep > 0) {
                    volumeStep--;
                    syncVolume();
                    game.playClick(0.8f);
                }
            }
        });

        volumeTable.add(volLabel).padRight(5);
        volumeTable.add(volDown).padRight(5);
        volumeTable.add(volStatusLabel).width(60);
        volumeTable.add(volUp).padLeft(5);
        mainTable.add(volumeTable).padBottom(30).row();

        // 2. 게임 시작 버튼
        final Label startBtn = new Label("[ 게임 시작 ]", new Label.LabelStyle(game.mainFont, Color.CYAN));
        addHoverEffect(startBtn, Color.CYAN, Color.GOLD);
        startBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.playClick(1.0f);
                game.setScreen(new HeroSelectionScreen(game, "HADES", backgroundMusic));
            }
        });
        mainTable.add(startBtn).padBottom(20).row();

        // 3. 종료 버튼
        final Label exitBtn = new Label("[ 게임 종료 ]", new Label.LabelStyle(game.mainFont, Color.LIGHT_GRAY));
        addHoverEffect(exitBtn, Color.LIGHT_GRAY, Color.RED);
        exitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.playClick(0.7f);
                Gdx.app.exit();
            }
        });
        mainTable.add(exitBtn).padBottom(60).row();

        // 4. 하단 정보 영역
        Table bottomTable = new Table();
        bottomTable.setFillParent(true);
        bottomTable.bottom().padBottom(20);
        stage.addActor(bottomTable);

        Label infoLabel = new Label(
            "비영리/비홍보용 팬게임이며, 수익창출 및 무단 수정 배포를 금지합니다.\n" +
                "모든 권리는 제작자 '데브케이'에 있습니다.\n" +
                "문의: fatking25@kakao.com",
            new Label.LabelStyle(game.detailFont, Color.GRAY)
        );
        infoLabel.setAlignment(com.badlogic.gdx.utils.Align.center);
        bottomTable.add(infoLabel);
    }

    // 메서드 설명: 정수형 단계를 실제 음악 볼륨과 UI에 동기화합니다.
    private void syncVolume() {
        float nextVol = volumeStep / 10f;
        backgroundMusic.setVolume(nextVol);
        volStatusLabel.setText((volumeStep * 10) + "%");
    }

    // 메서드 설명: 마우스 오버 시 색상 및 크기 효과를 부여합니다.
    private void addHoverEffect(final Label label, final Color originalColor, final Color hoverColor) {
        label.setOrigin(com.badlogic.gdx.utils.Align.center);
        label.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                label.setColor(hoverColor);
                label.setScale(1.1f);
            }
            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                label.setColor(originalColor);
                label.setScale(1.0f);
            }
        });
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(bgColor.r, bgColor.g, bgColor.b, bgColor.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.batch.setProjectionMatrix(stage.getViewport().getCamera().combined);
        game.batch.begin();
        game.batch.setColor(0.6f, 0.6f, 0.6f, 1f);
        // 수정: GameConfig의 상수를 사용하여 배경을 그립니다.
        game.batch.draw(backgroundTexture, 0, 0, GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT);
        game.batch.setColor(Color.WHITE);
        game.batch.end();

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        stage.dispose();
        if (backgroundTexture != null) backgroundTexture.dispose();
        if (backgroundMusic != null) backgroundMusic.dispose();
    }
}
