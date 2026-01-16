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

        SkillData.Skill skill = SkillData.get(stat.skillName());
        // HeroStoryManager에서 해당 유닛의 칭호와 설명 데이터를 가져옵니다.
        HeroStoryManager.HeroStory story = HeroStoryManager.get(name);

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

        // 매니저에서 가져온 칭호(title) 적용
        Label subTitleLabel = new Label(story.title(), new Label.LabelStyle(game.unitFont2, Color.LIME));
        rightSection.add(subTitleLabel).padBottom(30).row();

        // 스테이지 1에서는 스킬이 봉인된 것으로 표시 (기획 반영)
        String skillText = "고유 권능: (봉인됨)";
        Color skillColor = Color.GRAY;

        // stageLevel 변수가 이 클래스에 선언되어 있다고 가정하거나, 1단계면 무조건 봉인
        // 만약 stageLevel > 1 이라면 skill.name을 보여주는 로직으로 확장 가능합니다.
        Label skillTitle = new Label(skillText, new Label.LabelStyle(game.unitFont2, skillColor));
        rightSection.add(skillTitle).padBottom(20).row();

        // 매니저에서 가져온 상세 스토리(description) 적용
        Label storyLabel = new Label(story.description(), new Label.LabelStyle(game.detailFont, Color.LIGHT_GRAY));
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
                // 컷씬 화면으로 먼저 전환 후 전투로 이동 (기존 로직 유지하며 목적지 설정)
                game.setScreen(new com.hades.game.screens.cutscene.BaseCutsceneScreen(
                    game,
                    com.hades.game.screens.cutscene.CutsceneManager.getStage1Data(),
                    new BattleScreen(game, selectedFaction, name, stat, 1)
                ));
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
