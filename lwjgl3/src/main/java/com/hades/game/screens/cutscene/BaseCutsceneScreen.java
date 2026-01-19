package com.hades.game.screens.cutscene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.hades.game.HadesGame;
import com.hades.game.constants.GameConfig;

// Chess Olympus: HADES vs ZEUS - 게임의 서사를 보여주는 컷씬 화면
public class BaseCutsceneScreen extends ScreenAdapter {
    private final HadesGame game;
    private final Stage stage;
    private final Screen nextScreen;
    private final CutsceneData data;

    private Image displayImage;
    private Label storyLabel;
    private Texture[] textures;

    private int currentSceneIndex = 0;
    private String[] currentWords;
    private String currentDisplayText = "";
    private int wordIndex = 0;
    private float timeCount = 0;
    private final float wordSpeed = 0.15f; // 타이핑 속도 설정

    public BaseCutsceneScreen(HadesGame game, CutsceneData data, Screen nextScreen) {
        this.game = game;
        this.data = data;
        this.nextScreen = nextScreen;
        this.stage = new Stage(new FitViewport(GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT));

        // 데이터에 정의된 이미지 경로 로드
        this.textures = new Texture[data.imagePaths().length];
        for (int i = 0; i < data.imagePaths().length; i++) {
            this.textures[i] = new Texture(Gdx.files.internal(data.imagePaths()[i]));
        }

        initUI();
        updateScene();
    }

    private void initUI() {
        // 배경 이미지 설정
        displayImage = new Image(textures[0]);
        displayImage.setFillParent(true);
        displayImage.setColor(0.6f, 0.6f, 0.6f, 1f);
        stage.addActor(displayImage);

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // 하단 대사창 설정
        Label.LabelStyle style = new Label.LabelStyle(game.detailFont2, Color.WHITE);
        storyLabel = new Label("", style);
        storyLabel.setAlignment(Align.center);
        storyLabel.setWrap(true);

        root.bottom().add(storyLabel).width(GameConfig.VIRTUAL_WIDTH * 0.8f).padBottom(100f);
    }

    private void updateScene() {
        if (currentSceneIndex < data.scripts().length) {
            if (currentSceneIndex < textures.length) {
                displayImage.setDrawable(new TextureRegionDrawable(textures[currentSceneIndex]));
            }

            // 타이핑 효과 준비
            currentWords = data.scripts()[currentSceneIndex].split(" ");
            currentDisplayText = "";
            wordIndex = 0;
            timeCount = 0;
            storyLabel.setText("");
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 타이핑 로직
        if (currentWords != null && wordIndex < currentWords.length) {
            timeCount += delta;
            if (timeCount >= wordSpeed) {
                currentDisplayText += (wordIndex == 0 ? "" : " ") + currentWords[wordIndex];
                storyLabel.setText(currentDisplayText);
                wordIndex++;
                timeCount = 0;
            }
        }

        // 입력 처리
        if (Gdx.input.justTouched()) {
            game.playClick();
            if (currentWords != null && wordIndex < currentWords.length) {
                wordIndex = currentWords.length;
                storyLabel.setText(data.scripts()[currentSceneIndex]);
            } else {
                currentSceneIndex++;
                if (currentSceneIndex < data.scripts().length) {
                    updateScene();
                } else {
                    // 모든 컷씬 종료 후 다음 화면 이동 전 음악은 유지하거나 정지 (전투 배경음이 새로 재생됨)
                    game.setScreen(nextScreen);
                }
            }
        }

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        // [수정] AudioManager를 사용하여 컷씬 BGM 재생
        if (data.bgmPath() != null) {
            game.audioManager.playBgm(data.bgmPath());
        }
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
        for (Texture tex : textures) {
            if (tex != null) tex.dispose();
        }
    }
}
