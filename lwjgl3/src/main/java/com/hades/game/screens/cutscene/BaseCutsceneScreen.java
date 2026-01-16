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

public class BaseCutsceneScreen extends ScreenAdapter {
    private final HadesGame game;
    private final Stage stage;
    private final Screen nextScreen;
    private final CutsceneData data; // record 타입 참조

    private Image displayImage;
    private Label storyLabel;
    private Texture[] textures;

    private int currentSceneIndex = 0;
    private String[] currentWords;
    private String currentDisplayText = "";
    private int wordIndex = 0;
    private float timeCount = 0;
    private final float wordSpeed = 0.3f; // 타이핑 속도 조정

    public BaseCutsceneScreen(HadesGame game, CutsceneData data, Screen nextScreen) {
        this.game = game;
        this.data = data;
        this.nextScreen = nextScreen;
        this.stage = new Stage(new FitViewport(GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT));

        // record의 imagePaths() 메서드로 접근
        this.textures = new Texture[data.imagePaths().length];
        for (int i = 0; i < data.imagePaths().length; i++) {
            this.textures[i] = new Texture(Gdx.files.internal(data.imagePaths()[i]));
        }

        initUI();
        updateScene();
    }

    private void initUI() {
        displayImage = new Image(textures[0]);
        displayImage.setFillParent(true);
        // 배경 이미지를 약간 어둡게 하여 글자가 잘 보이게 합니다.
        displayImage.setColor(0.5f, 0.5f, 0.5f, 1f);
        stage.addActor(displayImage);

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        Label.LabelStyle style = new Label.LabelStyle(game.detailFont2, Color.WHITE);
        storyLabel = new Label("", style);
        storyLabel.setAlignment(Align.center);
        storyLabel.setWrap(true);

        root.bottom().add(storyLabel).width(GameConfig.VIRTUAL_WIDTH * 0.8f).padBottom(100f);
    }

    private void updateScene() {
        // record의 scripts() 메서드로 접근
        if (currentSceneIndex < data.scripts().length) {
            // 이미지가 여러 장일 경우에 대비한 로직 (현재는 1장)
            if (currentSceneIndex < textures.length) {
                displayImage.setDrawable(new TextureRegionDrawable(textures[currentSceneIndex]));
            }

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

        if (Gdx.input.justTouched()) {
            game.playClick();
            // 아직 타이핑 중이라면 전체 문장을 바로 보여줌
            if (currentWords != null && wordIndex < currentWords.length) {
                wordIndex = currentWords.length;
                storyLabel.setText(data.scripts()[currentSceneIndex]);
            } else {
                // 문장이 끝났다면 다음 장면으로
                currentSceneIndex++;
                if (currentSceneIndex < data.scripts().length) {
                    updateScene();
                } else {
                    // 모든 장면 종료 시 다음 화면으로 전환
                    game.setScreen(nextScreen);
                }
            }
        }

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void show() { Gdx.input.setInputProcessor(stage); }

    @Override
    public void hide() { Gdx.input.setInputProcessor(null); }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
        for (Texture tex : textures) if (tex != null) tex.dispose();
    }
}
