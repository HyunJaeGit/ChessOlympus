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

    // 팝업 가독성을 위해 어두운 반투명 배경을 생성합니다.
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

    // 영웅 선택 리스트 UI를 초기화합니다.
    private void initUI() {
        Table root = new Table();   // 테이블 생성
        root.setFillParent(true);   // 테이블 크기를 부모(stage)의 크기에 맞춤
        stage.addActor(root);       // 생성한 테이블을 그리고 감지

        Label title = new Label("CHOOSE YOUR HERO", new Label.LabelStyle(game.subtitleFont, Color.GOLD));
        root.add(title).padBottom(50).row();

        Table listTable = new Table();
        String[] names = selectedFaction.equals("HADES") ? UnitData.NAMES_HADES : UnitData.NAMES_ZEUS;
        final UnitData.Stat[] stats = selectedFaction.equals("HADES") ? UnitData.STATS_HADES : UnitData.STATS_ZEUS;

        for (int i = 0; i < names.length; i++) {
            final String name = names[i];
            final UnitData.Stat stat = stats[i];

            // 1. 라벨 생성
            final Label nameLabel = new Label(name, new Label.LabelStyle(game.mainFont, Color.LIGHT_GRAY));
            nameLabel.setTouchable(Touchable.disabled);

            UI.addHoverEffect(game, nameLabel, Color.LIGHT_GRAY, Color.WHITE);

            // 2. 라벨을 담을 테이블 생성
            Table rowTable = new Table();
            rowTable.setTouchable(Touchable.enabled);
            rowTable.add(nameLabel).center().pad(10, 100, 10, 100); // 라벨 정렬(중앙, 여백)

            // 3. addHoverEffect 메서드 적용 (rowTable 호버시 nameLabel 색상 변경)
            UI.addHoverEffect(game, rowTable, nameLabel, Color.LIGHT_GRAY, Color.WHITE);

            // 4. 테이블 클릭 시 팝업을 띄우는 리스너 추가
            rowTable.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    showGridPopup(name, stat);
                }
            });

            listTable.add(rowTable).fillX().row();
        }
        root.add(listTable).center();
    }

    // 좌측 이미지, 우측 스토리 구조의 상세 정보 팝업을 출력합니다.
    private void showGridPopup(final String name, final UnitData.Stat stat) {
        Window.WindowStyle windowStyle = new Window.WindowStyle(game.detailFont2, Color.WHITE, dialogBackground);
        final Dialog dialog = new Dialog("", windowStyle);

        Table mainTable = dialog.getContentTable();
        mainTable.pad(30);
        mainTable.debug(); // 시맨틱 구조 확인용 가이드 라인 (테스트용 코드)

        SkillData.Skill skill = SkillData.get(stat.skillName());

        // --- 좌측 섹션: 일러스트 ---
        Table leftSection = new Table();
        String path = "images/character/" + name + ".png";
        if (Gdx.files.internal(path).exists()) {
            Texture charTex = new Texture(Gdx.files.internal(path));
            charTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            Image heroImg = new Image(charTex);
            heroImg.setScaling(Scaling.fit);
            leftSection.add(heroImg).size(400, 550);
        }
        mainTable.add(leftSection).padRight(40);

        // --- 우측 섹션: 정보 및 스토리 ---
        Table rightSection = new Table();
        rightSection.align(Align.top);
        rightSection.pad(20);

        Label nameLabel = new Label(name, new Label.LabelStyle(game.subtitleFont, Color.WHITE));
        rightSection.add(nameLabel).padBottom(5).row();

        Label subTitleLabel = new Label("연옥의 기사", new Label.LabelStyle(game.unitFont2, Color.LIME));
        rightSection.add(subTitleLabel).padBottom(30).row();

        Label skillTitle = new Label("고유 권능: " + skill.name, new Label.LabelStyle(game.unitFont2, Color.GOLD));
        rightSection.add(skillTitle).padBottom(20).row();

        String heroStory = "죽음의 신 하데스에게 충성하는 이 기사는 연옥의 불꽃을 다룹니다.\n" +
            "그의 손에 쥐어진 검은 영혼을 베어 넘기며,\n" +
            "적들에게 영원한 안식을 선사할 것입니다.";

        Label storyLabel = new Label(heroStory, new Label.LabelStyle(game.detailFont, Color.LIGHT_GRAY));
        storyLabel.setWrap(true);
        storyLabel.setAlignment(Align.left);
        rightSection.add(storyLabel).width(450).padBottom(40).row();

        // --- 버튼 영역 ---
        Table btnTable = new Table();

        final Label startBtn = new Label("전투 시작", new Label.LabelStyle(game.detailFont, Color.GOLD));
        startBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (backgroundMusic != null) backgroundMusic.stop();
                game.setScreen(new BattleScreen(game, selectedFaction, name, stat, 1));
                dialog.hide();
            }
        });

        final Label closeBtn = new Label("닫기", new Label.LabelStyle(game.detailFont, Color.WHITE));
        closeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                dialog.hide();
            }
        });
        // UI.java 의 addHoverEffect 메서드로 UI 효과 적용
        UI.addHoverEffect(game, startBtn, Color.GOLD, Color.WHITE);
        UI.addHoverEffect(game, closeBtn, Color.WHITE, Color.LIGHT_GRAY);

        btnTable.add(startBtn).padRight(40);
        btnTable.add(closeBtn);
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
        if (backgroundMusic != null) backgroundMusic.dispose();
        if(dialogBackground != null) dialogBackground.getRegion().getTexture().dispose();
    }
}
