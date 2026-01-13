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

/**
 * [클래스 역할] 게임의 메인 메뉴 화면을 담당합니다.
 * 배경 이미지 출력, 진영 선택, 버튼 하이라이트 효과를 포함합니다.
 */
public class MenuScreen extends ScreenAdapter {
    private final HadesGame game;
    private Stage stage;
    private Color bgColor;
    private String selectedFaction = "HADES";
    private Label factionStatusLabel;
    private Texture backgroundTexture;

    public MenuScreen(HadesGame game) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport());

        // 배경 이미지 로드 (assets/images/background/main.png)
        backgroundTexture = new Texture(Gdx.files.internal("images/background/main.png"));
        this.bgColor = new Color(0.05f, 0.05f, 0.1f, 1);

        Gdx.input.setInputProcessor(stage);
        initUI();
    }

    // UI 구성 요소들을 생성하고 레이아웃을 배치합니다.
    private void initUI() {
        // 1. 메인 버튼들을 담을 테이블 (화면 중앙 정렬)
        Table mainTable = new Table();
        mainTable.setFillParent(true); // 화면 전체 크기로 설정
        mainTable.center();            // 내부 요소들을 중앙에 배치
        stage.addActor(mainTable);

        // --- 타이틀 및 버튼 배치 (mainTable 사용) ---

        // 1. 타이틀 및 부제목
        Label titleLabel = new Label("CHESS OLYMPUS", new Label.LabelStyle(game.titleFont, Color.GOLD));
        Label subtitleLabel = new Label("Hades VS Zeus", new Label.LabelStyle(game.subtitleFont, Color.LIGHT_GRAY));
        mainTable.add(titleLabel).padBottom(5).row();
        mainTable.add(subtitleLabel).padBottom(40).row();

        // 2. 진영 선택 상태창
        factionStatusLabel = new Label("선택된 진영: " + selectedFaction, new Label.LabelStyle(game.mainFont, Color.WHITE));
        mainTable.add(factionStatusLabel).padBottom(15).row();

        // 3. 하데스 진영 선택 버튼
        final Label hadesBtn = new Label("[ 하데스 진영 선택 ]", new Label.LabelStyle(game.mainFont, Color.VIOLET));
        addHoverEffect(hadesBtn, Color.VIOLET, Color.WHITE);
        hadesBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectedFaction = "HADES";
                bgColor.set(0.05f, 0.05f, 0.1f, 1);
                updateFactionStatus();
            }
        });
        mainTable.add(hadesBtn).padBottom(10).row();

        // 4. 제우스 진영 선택 버튼
        final Label zeusBtn = new Label("[ 제우스 진영 선택 ]", new Label.LabelStyle(game.mainFont, Color.YELLOW));
        addHoverEffect(zeusBtn, Color.YELLOW, Color.WHITE);
        zeusBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectedFaction = "ZEUS";
                bgColor.set(0.15f, 0.15f, 0.1f, 1);
                updateFactionStatus();
            }
        });
        mainTable.add(zeusBtn).padBottom(20).row();

        // 5. 게임 시작 버튼
        final Label startBtn = new Label("[ 게임 시작 ]", new Label.LabelStyle(game.mainFont, Color.CYAN));
        addHoverEffect(startBtn, Color.CYAN, Color.WHITE);
        startBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new BattleScreen(game, selectedFaction));
            }
        });
        mainTable.add(startBtn).padBottom(60).row();


        // 2. 하단 저작권 정보용 테이블 (별도로 생성하여 배치)
        Table bottomTable = new Table();
        bottomTable.setFillParent(true);
        bottomTable.bottom().padBottom(20); // 화면 하단에 딱 붙이고 위로 20px 여백
        stage.addActor(bottomTable);

        // 6. 하단 저작권 정보 (bottomTable에 추가)
        Label infoLabel = new Label(
            "비영리/비홍보용 팬게임이며, 수익창출 및 무단 수정 배포를 금지합니다.\n" +
                "모든 권리는 제작자 '데브케이'에 있습니다.\n" +
                "문의: fatking25@kakao.com",
            new Label.LabelStyle(game.detailFont, Color.GRAY)
        );
        infoLabel.setAlignment(com.badlogic.gdx.utils.Align.center); // 텍스트 중앙 정렬

        // 이제 버튼이 커져도 bottomTable은 독립되어 있어 절대 밀려나지 않습니다.
        bottomTable.add(infoLabel);
    }

    /**
     * [메서드 설명] 라벨에 마우스 오버 시 색상 변경 및 확대 효과를 부여합니다.
     */
    private void addHoverEffect(final Label label, final Color originalColor, final Color hoverColor) {
        label.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                label.setColor(hoverColor);     // 마우스 올리면 색상 변경
                label.setFontScale(1.1f);      // 살짝 확대
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                label.setColor(originalColor);  // 마우스 나가면 원복
                label.setFontScale(1.0f);      // 크기 원복
            }
        });
    }

    private void updateFactionStatus() {
        factionStatusLabel.setText("선택된 진영: " + selectedFaction);
        factionStatusLabel.setColor(selectedFaction.equals("HADES") ? Color.VIOLET : Color.YELLOW);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(bgColor.r, bgColor.g, bgColor.b, bgColor.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.batch.begin();
        // 배경을 60% 밝기로 설정하여 텍스트가 더 잘 보이게 함
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
