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
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.hades.game.HadesGame;

public class MenuScreen extends ScreenAdapter {
    private final HadesGame game;
    private Stage stage;
    private Color bgColor;
    private Texture backgroundTexture;
    private com.badlogic.gdx.audio.Music backgroundMusic; // 음악 객체 추가
    private Label volStatusLabel; // 현재 볼륨을 숫자로 보여줄 라벨

    public MenuScreen(HadesGame game) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport());

        // 배경 이미지 로드
        backgroundTexture = new Texture(Gdx.files.internal("images/background/main.png"));
        this.bgColor = new Color(0.05f, 0.05f, 0.1f, 1);

        // 배경음악 설정
        initMusic();

        Gdx.input.setInputProcessor(stage);
        initUI();
    }

    // 음악 파일을 로드하고 무한 반복 재생을 설정합니다.
    private void initMusic() {
        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("music/bgm.mp3"));
        backgroundMusic.setLooping(true); // 무한 반복
        backgroundMusic.setVolume(0.2f);  // 볼륨 (0.0 ~ 1.0)
        backgroundMusic.play();
    }

    private void initUI() {
        // 메인 버튼들을 담을 테이블
        Table mainTable = new Table();
        mainTable.setFillParent(true);
        mainTable.center();
        stage.addActor(mainTable);

        // 1. 타이틀 (부제목 Zeus 제거 및 컨셉에 맞춰 수정)
        Label titleLabel = new Label("CHESS OLYMPUS", new Label.LabelStyle(game.titleFont, Color.GOLD));
        Label subtitleLabel = new Label("HADES VS ZEUS", new Label.LabelStyle(game.subtitleFont, Color.LIGHT_GRAY));

        mainTable.add(titleLabel).padBottom(5).row();
        mainTable.add(subtitleLabel).padBottom(40).row();

        // 1-1. 볼륨 조절 영역 (Slider 대신 텍스트 버튼 방식 - 스킨 파일이 없을 경우 대비)
        Table volumeTable = new Table();
        Label volLabel = new Label("BGM VOLUME", new Label.LabelStyle(game.mainFont, Color.WHITE));
        Label volUp = new Label(" [ + ] ", new Label.LabelStyle(game.mainFont, Color.LIGHT_GRAY));
        Label volDown = new Label(" [ - ] ", new Label.LabelStyle(game.mainFont, Color.LIGHT_GRAY));
        volStatusLabel = new Label("20%", new Label.LabelStyle(game.mainFont, Color.WHITE));

        addHoverEffect(volDown, Color.LIGHT_GRAY, Color.WHITE);
        addHoverEffect(volUp, Color.LIGHT_GRAY, Color.WHITE);

        // 볼륨 업 클릭 이벤트
        volUp.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                float nextVol = Math.min(1f, backgroundMusic.getVolume() + 0.1f);
                backgroundMusic.setVolume(nextVol);
                updateVolLabel();
            }
        });

        // 볼륨 다운 클릭 이벤트
        volDown.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                float nextVol = Math.max(0f, backgroundMusic.getVolume() - 0.1f);
                backgroundMusic.setVolume(nextVol);
                updateVolLabel();
            }
        });

        volumeTable.add(volLabel).padRight(5);
        volumeTable.add(volDown).padRight(5); // [-] 버튼
        volumeTable.add(volStatusLabel).width(60); // 숫자 (중앙 정렬 유지용 width)
        volumeTable.add(volUp).padLeft(5);   // [+] 버튼
        mainTable.add(volumeTable).padBottom(30).row();

        // 2. 게임 시작 버튼
        final Label startBtn = new Label("[ 게임 시작 ]", new Label.LabelStyle(game.mainFont, Color.CYAN));
        addHoverEffect(startBtn, Color.CYAN, Color.GOLD);
        startBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // 영웅 선택 화면으로 바로 이동
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
                Gdx.app.exit();
            }
        });
        mainTable.add(exitBtn).padBottom(60).row();


        // 하단 저작권 정보용 테이블
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
    // 볼륨 업데이트
    private void updateVolLabel() {
        int volPercent = Math.round(backgroundMusic.getVolume() * 100);
        volStatusLabel.setText(volPercent + "%");
    }

    // 라벨에 마우스 오버 시 색상 변경 및 확대 효과
    private void addHoverEffect(final Label label, final Color originalColor, final Color hoverColor) {
        label.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                label.setColor(hoverColor);
                label.setFontScale(1.05f);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                label.setColor(originalColor);
                label.setFontScale(1.0f);
            }
        });
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(bgColor.r, bgColor.g, bgColor.b, bgColor.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.batch.begin();
        game.batch.setColor(0.6f, 0.6f, 0.6f, 1f);
        game.batch.draw(backgroundTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
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
    }
}
