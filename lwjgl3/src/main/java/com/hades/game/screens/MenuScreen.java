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
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.hades.game.HadesGame;

/**
 * 게임의 시작 화면을 담당하는 클래스입니다.
 * 플레이어는 여기서 진영(HADES/ZEUS)을 선택하고 게임을 시작할 수 있습니다.
 */
public class MenuScreen extends ScreenAdapter {
    private final HadesGame game;
    private Stage stage;
    private Color bgColor;
    private String selectedFaction = "HADES"; // 플레이어가 선택한 진영 (기본값 HADES)
    private Label factionStatusLabel;        // 현재 어떤 진영을 선택했는지 보여주는 라벨

    public MenuScreen(HadesGame game) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport());

        // 기본 배경색 (하데스 테마: 어두운 남보라)
        this.bgColor = new Color(0.05f, 0.05f, 0.1f, 1);

        // 입력을 Stage로 넘겨 버튼 클릭이 가능하게 합니다.
        Gdx.input.setInputProcessor(stage);

        initUI();
    }

    /**
     * [메서드 설명] Scene2D UI 구성 요소들을 생성하고 레이아웃을 배치합니다.
     */
    private void initUI() {
        Table table = new Table();
        table.setFillParent(true); // 테이블이 화면 전체를 채우도록 설정
        stage.addActor(table);

        // 1. 메인 타이틀
        Label.LabelStyle titleStyle = new Label.LabelStyle(game.detailFont, Color.GOLD);
        Label titleLabel = new Label("CHESS OLYMPUS : 하데스 vs 제우스", titleStyle);
        titleLabel.setFontScale(1.5f);
        table.add(titleLabel).padBottom(60).row();

        // 2. 진영 선택 상태창
        Label.LabelStyle statusStyle = new Label.LabelStyle(game.mainFont, Color.WHITE);
        factionStatusLabel = new Label("선택된 진영: " + selectedFaction, statusStyle);
        table.add(factionStatusLabel).padBottom(30).row();

        // 3. 진영 선택 버튼들 (HADES)
        Label.LabelStyle hadesStyle = new Label.LabelStyle(game.mainFont, Color.VIOLET);
        Label hadesBtn = new Label("[ 하데스 진영 선택 ]", hadesStyle);
        hadesBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectedFaction = "HADES";
                bgColor.set(0.05f, 0.05f, 0.1f, 1); // 배경색을 하데스 테마로 변경
                updateFactionStatus();
            }
        });
        table.add(hadesBtn).padBottom(15).row();

        // 4. 진영 선택 버튼들 (ZEUS)
        Label.LabelStyle zeusStyle = new Label.LabelStyle(game.mainFont, Color.YELLOW);
        Label zeusBtn = new Label("[ 제우스 진영 선택 ]", zeusStyle);
        zeusBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectedFaction = "ZEUS";
                bgColor.set(0.15f, 0.15f, 0.1f, 1); // 배경색을 제우스 테마로 변경
                updateFactionStatus();
            }
        });
        table.add(zeusBtn).padBottom(40).row();

        // 5. 게임 시작 버튼
        Label.LabelStyle startStyle = new Label.LabelStyle(game.mainFont, Color.CYAN);
        Label startBtn = new Label("[ 게임 시작 ]", startStyle);
        startBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // [리팩토링 핵심] 선택된 진영 데이터를 BattleScreen으로 전달하며 화면 전환
                game.setScreen(new BattleScreen(game, selectedFaction));
            }
        });
        table.add(startBtn).padBottom(80).row();

        // 6. 하단 저작권 및 안내 문구
        Label.LabelStyle infoStyle = new Label.LabelStyle(game.detailFont, Color.GRAY);
        Label infoLabel = new Label(
            "비영리/비홍보용 팬게임이며, 수익창출 및 무단 수정 배포를 금지합니다.\n" +
                "모든 권리는 제작자 '데브케이'에 있습니다.\n" +
                "문의: fatking25@kakao.com",
            infoStyle
        );
        infoLabel.setFontScale(0.8f);
        table.add(infoLabel);
    }

    // 진영이 바뀔 때마다 텍스트를 갱신
    private void updateFactionStatus() {
        factionStatusLabel.setText("선택된 진영: " + selectedFaction);
        factionStatusLabel.setColor(selectedFaction.equals("HADES") ? Color.VIOLET : Color.YELLOW);
    }

    @Override
    public void render(float delta) {
        // 배경색 적용
        Gdx.gl.glClearColor(bgColor.r, bgColor.g, bgColor.b, bgColor.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Stage 업데이트 및 드로잉
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        // 화면 크기가 변해도 UI가 깨지지 않도록 업데이트
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        // 리소스 해제
        stage.dispose();
    }
}
