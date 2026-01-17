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

// 게임의 서사를 보여주는 컷씬 화면을 담당하는 클래스입니다.
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

        // 데이터에 정의된 이미지 경로들을 순차적으로 로드합니다.
        this.textures = new Texture[data.imagePaths().length];
        for (int i = 0; i < data.imagePaths().length; i++) {
            this.textures[i] = new Texture(Gdx.files.internal(data.imagePaths()[i]));
        }

        initUI();
        updateScene();
    }

    private void initUI() {
        // 배경 이미지 설정 (이미지 배경을 어둡게 처리하여 텍스트 가독성 확보)
        displayImage = new Image(textures[0]);
        displayImage.setFillParent(true);
        displayImage.setColor(0.6f, 0.6f, 0.6f, 1f);
        stage.addActor(displayImage);

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // 하단 대사창 레이블 설정
        Label.LabelStyle style = new Label.LabelStyle(game.detailFont2, Color.WHITE);
        storyLabel = new Label("", style);
        storyLabel.setAlignment(Align.center);
        storyLabel.setWrap(true);

        root.bottom().add(storyLabel).width(GameConfig.VIRTUAL_WIDTH * 0.8f).padBottom(100f);
    }

    private void updateScene() {
        // 현재 인덱스에 맞는 대사와 이미지를 준비합니다.
        if (currentSceneIndex < data.scripts().length) {
            if (currentSceneIndex < textures.length) {
                displayImage.setDrawable(new TextureRegionDrawable(textures[currentSceneIndex]));
            }

            // 문장을 단어 단위로 분리하여 타이핑 효과 준비
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

        // 한 단어씩 나타나는 타이핑 로직
        if (currentWords != null && wordIndex < currentWords.length) {
            timeCount += delta;
            if (timeCount >= wordSpeed) {
                currentDisplayText += (wordIndex == 0 ? "" : " ") + currentWords[wordIndex];
                storyLabel.setText(currentDisplayText);
                wordIndex++;
                timeCount = 0;
            }
        }

        // 터치/클릭 입력 처리
        if (Gdx.input.justTouched()) {
            game.playClick();
            if (currentWords != null && wordIndex < currentWords.length) {
                // 아직 글자가 나오는 중이라면 즉시 전체 문장 표시
                wordIndex = currentWords.length;
                storyLabel.setText(data.scripts()[currentSceneIndex]);
            } else {
                // 문장이 완성된 상태라면 다음 장면으로 전환
                currentSceneIndex++;
                if (currentSceneIndex < data.scripts().length) {
                    updateScene();
                } else {
                    // 모든 컷씬이 끝났으므로 다음 화면(주로 전투)으로 이동
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

        // 컷씬 데이터에 지정된 BGM 재생
        if (data.bgmPath() != null) {
            // 현재 재생 중인 메뉴 음악 등이 있다면 정지 및 제거
            if (game.currentBgm != null) {
                game.currentBgm.stop();
                game.currentBgm.dispose();
            }
            // 새로운 스테이지 배경음악 로드
            game.currentBgm = Gdx.audio.newMusic(Gdx.files.internal(data.bgmPath()));
            game.currentBgm.setLooping(true);
            game.currentBgm.setVolume(game.globalVolume); // 메인 메뉴에서 설정한 볼륨 적용
            game.currentBgm.play();
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
        // 배경음악은 다음 화면에서도 계속 재생되어야 하므로 여기서 dispose하지 않습니다.
    }
}
