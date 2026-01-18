package com.hades.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.hades.game.HadesGame;
import com.hades.game.constants.GameConfig;
import com.hades.game.screens.cutscene.BaseCutsceneScreen;
import com.hades.game.screens.cutscene.CutsceneManager;
import com.hades.game.view.UI;

public class MenuScreen extends ScreenAdapter {
    private final HadesGame game;
    private final Stage stage; // final 추가
    private final Texture backgroundTexture; // final 추가

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
        mainTable.center(); // 전체를 중앙에 배치
        stage.addActor(mainTable);

        // 1. 타이틀 섹션 (위쪽으로 배치)
        mainTable.add(new Label("CHESS OLYMPUS", new Label.LabelStyle(game.titleFont, COLOR_GOLD))).padBottom(10).row();
        mainTable.add(new Label("HADES VS ZEUS", new Label.LabelStyle(game.subtitleFont, COLOR_SUB))).padBottom(50).row();

        // 2. 중간 메뉴 그룹 (글씨 크기를 detailFont로 하향 조정하여 겹침 방지)
        Table menuGroup = new Table();

        // --- 여정 계속하기 ---
        boolean hasSave = Gdx.files.local("save/run_data.json").exists();
        Label continueBtn = new Label("CONTINUE", new Label.LabelStyle(game.detailFont, hasSave ? COLOR_GOLD : Color.DARK_GRAY));

        if (hasSave) {
            UI.addHoverEffect(game, continueBtn, COLOR_GOLD, Color.WHITE);
            continueBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    game.playClick();
                    game.loadGame();
                    game.setScreen(new StageMapScreen(game));
                }
            });
        }
        menuGroup.add(continueBtn).padBottom(20).row(); // 패딩을 늘려 간격 확보

        // --- 새로운 여정 ---
        Label startBtn = new Label("NEW JOURNEY", new Label.LabelStyle(game.detailFont, COLOR_MAIN));
        UI.addHoverEffect(game, startBtn, COLOR_MAIN, COLOR_GOLD);
        startBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.playClick();
                if (Gdx.files.local("save/run_data.json").exists()) {
                    showOverwriteWarning();
                } else {
                    startNewGame();
                }
            }
        });
        menuGroup.add(startBtn).padBottom(20).row();

        // --- BGM 설정 ---
        Table volumeRow = new Table();
        volStatusLabel = new Label((volumeStep * 10) + "%", new Label.LabelStyle(game.detailFont, COLOR_POINT));
        Label volDown = new Label(" - ", new Label.LabelStyle(game.detailFont, COLOR_MAIN));
        Label volUp = new Label(" + ", new Label.LabelStyle(game.detailFont, COLOR_MAIN));

        UI.addHoverEffect(game, volDown, COLOR_MAIN, COLOR_GOLD);
        UI.addHoverEffect(game, volUp, COLOR_MAIN, COLOR_GOLD);

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

        volumeRow.add(new Label("BGM", new Label.LabelStyle(game.detailFont, COLOR_MAIN))).padRight(20);
        volumeRow.add(volDown).padRight(15);
        volumeRow.add(volStatusLabel).width(100).align(Align.center);
        volumeRow.add(volUp).padLeft(15);
        menuGroup.add(volumeRow).padBottom(20).row();

        // --- 화면 모드 ---
        screenBtn = new Label(Gdx.graphics.isFullscreen() ? "WINDOW" : "FULLSCREEN", new Label.LabelStyle(game.detailFont, COLOR_POINT));
        UI.addHoverEffect(game, screenBtn, COLOR_POINT, COLOR_MAIN);
        screenBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.playClick();
                toggleFullscreen();
            }
        });
        menuGroup.add(screenBtn).padBottom(20).row();

        // --- 종료 ---
        Label exitBtn = new Label("EXIT GAME", new Label.LabelStyle(game.detailFont, COLOR_SUB));
        UI.addHoverEffect(game, exitBtn, COLOR_SUB, Color.FIREBRICK);
        exitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) { Gdx.app.exit(); }
        });
        menuGroup.add(exitBtn).row();

        mainTable.add(menuGroup).center();

        // 3. 하단 저작권 (조금 더 아래로 내림)
        Table bottomTable = new Table();
        bottomTable.setFillParent(true);
        bottomTable.bottom().padBottom(20);
        stage.addActor(bottomTable);

        Label infoLabel = new Label("""
            비영리/비상업용 팬게임이며 수익창출/2차창작을 금지합니다.
            모든 권리는 제작자 '데브케이'에 있습니다.
            문의 : fatking25@kakao.com""",
            new Label.LabelStyle(game.detailFont, new Color(0.5f, 0.5f, 0.5f, 0.8f)) // 가독성을 위해 색상 살짝 조정
        );
        infoLabel.setAlignment(Align.center);
        infoLabel.setFontScale(0.8f); // 저작권 텍스트 크기 자체 축소
        bottomTable.add(infoLabel);
    }

    private void showOverwriteWarning() {
        // 1. 윈도우 스타일 (배경 없이 폰트만 설정)
        Window.WindowStyle windowStyle = new Window.WindowStyle(game.detailFont2, Color.WHITE, null);

        Dialog dialog = new Dialog("", windowStyle) {
            @Override
            public void draw(com.badlogic.gdx.graphics.g2d.Batch batch, float parentAlpha) {
                // [효과 1] 전체 화면 어둡게 만들기 (Dim)
                batch.setColor(0, 0, 0, 0.7f); // 70% 투명도
                // 흰색 픽셀이나 배경 텍스처를 활용해 전체를 덮습니다.
                batch.draw(backgroundTexture, 0, 0, GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT);

                // [효과 2] 다이얼로그 그림자 (Drop Shadow)
                // 본체보다 약간 오른쪽 아래에 검은색 사각형을 먼저 그립니다.
                batch.setColor(0, 0, 0, 0.5f);
                batch.draw(backgroundTexture, getX() + 10, getY() - 10, getWidth(), getHeight());

                // [효과 3] 다이얼로그 실제 배경 (어두운 남색/검정)
                batch.setColor(0.05f, 0.05f, 0.1f, 0.95f);
                batch.draw(backgroundTexture, getX(), getY(), getWidth(), getHeight());

                // [효과 4] 금색 테두리 (Border) - 배경 위를 얇게 덮음
                batch.setColor(COLOR_GOLD);
                // 가로선 상/하
                batch.draw(backgroundTexture, getX(), getY(), getWidth(), 2);
                batch.draw(backgroundTexture, getX(), getY() + getHeight() - 2, getWidth(), 2);
                // 세로선 좌/우
                batch.draw(backgroundTexture, getX(), getY(), 2, getHeight());
                batch.draw(backgroundTexture, getX() + getWidth() - 2, getY(), 2, getHeight());

                batch.setColor(Color.WHITE); // 색상 초기화
                super.draw(batch, parentAlpha);
            }
        };

        // 기존 UI 구성은 그대로 유지
        dialog.getContentTable().pad(50);
        dialog.text("기존의 여정 기록이 존재합니다.\n새로 시작하면 이전 기록은 영원히 소멸합니다.\n계속하시겠습니까?",
            new Label.LabelStyle(game.detailFont, Color.WHITE));

        Table buttonTable = dialog.getButtonTable();
        buttonTable.padBottom(40);

        Label okBtn = new Label("기록 삭제 및 시작", new Label.LabelStyle(game.detailFont, COLOR_GOLD));
        UI.addHoverEffect(game, okBtn, COLOR_GOLD, Color.WHITE);
        okBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                dialog.hide();
                startNewGame();
            }
        });

        Label cancelBtn = new Label("돌아가기", new Label.LabelStyle(game.detailFont, COLOR_SUB));
        UI.addHoverEffect(game, cancelBtn, COLOR_SUB, Color.WHITE);
        cancelBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                dialog.hide();
            }
        });

        buttonTable.add(okBtn).padRight(50);
        buttonTable.add(cancelBtn);

        dialog.show(stage);
    }

    private void startNewGame() {
        if (game.menuBgm != null) game.menuBgm.stop();
        game.runState.reset();
        game.setScreen(new BaseCutsceneScreen(
            game,
            CutsceneManager.getIntroData(),
            new HeroSelectionScreen(game, "HADES", game.menuBgm)
        ));
    }

    private void toggleFullscreen() {
        if (Gdx.graphics.isFullscreen()) {
            Gdx.graphics.setWindowedMode((int) GameConfig.VIRTUAL_WIDTH, (int) GameConfig.VIRTUAL_HEIGHT);
            screenBtn.setText("FULLSCREEN");
        } else {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
            screenBtn.setText("WINDOW");
        }
    }

    private void syncVolume() {
        game.updateVolume(volumeStep / 10f);
        volStatusLabel.setText((volumeStep * 10) + "%");
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.batch.setProjectionMatrix(stage.getViewport().getCamera().combined);
        game.batch.begin();
        // 배경을 조금 더 어둡게 처리하여 글자 가독성 확보 (0.4f -> 0.3f)
        game.batch.setColor(0.3f, 0.3f, 0.3f, 1f);
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
