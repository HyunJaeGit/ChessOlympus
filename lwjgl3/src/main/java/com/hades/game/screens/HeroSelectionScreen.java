package com.hades.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.hades.game.HadesGame;
import com.hades.game.constants.GameConfig;
import com.hades.game.constants.HeroStoryManager;
import com.hades.game.constants.SkillData;
import com.hades.game.constants.UnitData;
import com.hades.game.view.UI;

// 영웅 선택 및 상세 스토리 팝업을 관리하는 클래스입니다.
public class HeroSelectionScreen extends ScreenAdapter {
    private final HadesGame game;
    private final String selectedFaction;
    private Stage stage;
    private Texture backgroundTexture;
    private TextureRegionDrawable dialogBackground;
    private final Music backgroundMusic;

    public HeroSelectionScreen(HadesGame game, String faction, Music music) {
        this.game = game;
        this.backgroundMusic = music;
        this.selectedFaction = faction;

        this.stage = new Stage(new FitViewport(GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT));

        backgroundTexture = new Texture(Gdx.files.internal("images/background/main.png"));
        backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        createDialogBackground();
        initUI();
    }

    private void createDialogBackground() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0, 0, 0, 0.9f));
        pixmap.fill();
        dialogBackground = new TextureRegionDrawable(new TextureRegion(new Texture(pixmap)));
        pixmap.dispose();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    private void initUI() {
        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        Label title = new Label("CHOOSE YOUR HERO", new Label.LabelStyle(game.subtitleFont, Color.GOLD));
        root.add(title).padBottom(50).row();

        Table listTable = new Table();
        String[] names = selectedFaction.equals("HADES") ? UnitData.NAMES_HADES : UnitData.NAMES_ZEUS;
        final UnitData.Stat[] stats = selectedFaction.equals("HADES") ? UnitData.STATS_HADES : UnitData.STATS_ZEUS;

        for (int i = 0; i < names.length; i++) {
            final String name = names[i];
            final UnitData.Stat stat = stats[i];

            final Label nameLabel = new Label(name, new Label.LabelStyle(game.mainFont, Color.LIGHT_GRAY));
            nameLabel.setTouchable(Touchable.disabled);

            Table rowTable = new Table();
            rowTable.setTouchable(Touchable.enabled);
            rowTable.add(nameLabel).center().pad(10, 100, 10, 100);

            UI.addHoverEffect(game, rowTable, nameLabel, Color.LIGHT_GRAY, Color.WHITE);

            rowTable.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    game.playClick();
                    showGridPopup(name, stat);
                }
            });

            listTable.add(rowTable).fillX().row();
        }
        root.add(listTable).center();
    }

    private void showGridPopup(final String name, final UnitData.Stat stat) {
        Window.WindowStyle windowStyle = new Window.WindowStyle(game.detailFont2, Color.WHITE, dialogBackground);
        final Dialog dialog = new Dialog("", windowStyle) {
            @Override
            protected void result(Object object) {
                // 기본 result 동작을 막아 버튼 클릭 이벤트가 커스텀 리스너에서 처리되게 함
            }
        };

        Table mainTable = dialog.getContentTable();
        mainTable.pad(30);

        HeroStoryManager.HeroStory story = HeroStoryManager.get(name);

        // 좌측 섹션: 일러스트
        Table leftSection = new Table();
        String path = "images/character/" + name + ".png";
        if (Gdx.files.internal(path).exists()) {
            Texture charTex = new Texture(Gdx.files.internal(path));
            charTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            Image heroImg = new Image(charTex);
            heroImg.setScaling(Scaling.fit);
            leftSection.add(heroImg).size(400, 550);
        }
        mainTable.add(leftSection).padRight(40).top();

        // 우측 섹션: 정보 및 스토리
        Table rightSection = new Table();
        rightSection.align(Align.topLeft);

        Label nameLabel = new Label(name, new Label.LabelStyle(game.subtitleFont, Color.WHITE));
        rightSection.add(nameLabel).align(Align.left).padBottom(5).row();

        Label subTitleLabel = new Label(story.title(), new Label.LabelStyle(game.unitFont2, Color.LIME));
        rightSection.add(subTitleLabel).align(Align.left).padBottom(20).row();

        // 스테이지 1 권능 봉인 표시
        Label skillTitle = new Label("고유 권능: (봉인됨)", new Label.LabelStyle(game.unitFont2, Color.GRAY));
        rightSection.add(skillTitle).align(Align.left).padBottom(15).row();

        // 상세 스토리
        Label storyLabel = new Label(story.description(), new Label.LabelStyle(game.detailFont, Color.LIGHT_GRAY));
        storyLabel.setWrap(true);
        storyLabel.setAlignment(Align.left);
        rightSection.add(storyLabel).width(450).padBottom(30).row();

        // 기본 스탯 정보 테이블
        Table statTable = new Table();
        Label.LabelStyle statStyle = new Label.LabelStyle(game.unitFont3, Color.WHITE);

        statTable.add(new Label("체력(HP): " + stat.hp(), statStyle)).padRight(20);
        statTable.add(new Label("공격(ATK): " + stat.atk(), statStyle)).padRight(20);
        statTable.add(new Label("이동(MOV): " + stat.move(), statStyle)).row();

        statTable.add(new Label("사거리(RAN): " + stat.range(), statStyle)).padTop(10).padRight(20);
        statTable.add(new Label("반격력(CTK): " + stat.counterAtk(), statStyle)).padTop(10);

        rightSection.add(statTable).align(Align.left).padBottom(40).row();

        // 버튼 영역 (감지 영역 확장을 위해 Table 활용)
        Table btnTable = new Table();

        // 1. 전투 시작 버튼 컨테이너 생성
        Table startBtnCont = new Table();
        startBtnCont.setTouchable(Touchable.enabled);
        // 기본 색상을 LIME으로 설정 (Style에서 가져온 색상과 일치)
        final Label startBtnLabel = new Label("전투 시작", new Label.LabelStyle(game.detailFont, Color.LIME));
        startBtnCont.add(startBtnLabel).pad(15, 30, 15, 30);

        // 2. 닫기 버튼 컨테이너 생성
        Table closeBtnCont = new Table();
        closeBtnCont.setTouchable(Touchable.enabled);
        // 기본 색상을 WHITE로 설정
        final Label closeBtnLabel = new Label("닫기", new Label.LabelStyle(game.detailFont, Color.WHITE));
        closeBtnCont.add(closeBtnLabel).pad(15, 30, 15, 30);

        // UI.addHoverEffect 호출 시 첫 번째 색상(normalColor)을 Label의 초기 색상과 반드시 일치시킵니다.
        // 마우스가 나갈 때(exit) 이 normalColor로 돌아가기 때문입니다.
        UI.addHoverEffect(game, startBtnCont, startBtnLabel, Color.LIME, Color.WHITE);
        UI.addHoverEffect(game, closeBtnCont, closeBtnLabel, Color.WHITE, Color.GRAY);

        // 클릭 리스너 등록
        startBtnCont.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.playClick();
                if (backgroundMusic != null) backgroundMusic.stop();
                game.setScreen(new com.hades.game.screens.cutscene.BaseCutsceneScreen(
                    game,
                    com.hades.game.screens.cutscene.CutsceneManager.getStage1Data(),
                    new BattleScreen(game, selectedFaction, name, stat, 1)
                ));
                dialog.hide();
            }
        });

        closeBtnCont.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.playClick();
                dialog.hide();
            }
        });

        // 레이아웃 배치
        btnTable.add(startBtnCont).padRight(30);
        btnTable.add(closeBtnCont);

        rightSection.add(btnTable).expandY().bottom().right();
        mainTable.add(rightSection).expandY().fillY();

        dialog.show(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.batch.setProjectionMatrix(stage.getViewport().getCamera().combined);
        game.batch.begin();
        game.batch.draw(backgroundTexture, 0, 0, GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT);
        game.batch.end();

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
        backgroundTexture.dispose();
        if(dialogBackground != null) dialogBackground.getRegion().getTexture().dispose();
    }
}
