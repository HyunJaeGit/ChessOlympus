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
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.hades.game.HadesGame;
import com.hades.game.constants.GameConfig; // GameConfig 추가
import com.hades.game.constants.SkillData;
import com.hades.game.constants.UnitData;

// 클래스 역할: 진영 선택 후 플레이할 영웅을 선택하는 화면입니다.
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

        // GameConfig의 가상 해상도를 적용하여 비율을 고정합니다.
        this.stage = new Stage(new FitViewport(GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT));

        backgroundTexture = new Texture(Gdx.files.internal("images/background/main.png"));
        backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        // 반투명 블랙 배경 드로어블 생성
        createDialogBackground();

        initUI();
    }

    private void createDialogBackground() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0, 0, 0, 0.85f));
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
        root.setTransform(true); // 글자 확대 효과 시 레이아웃 깨짐 방지
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

            rowTable.addListener(new InputListener() {
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor from) {
                    nameLabel.setColor(Color.WHITE);
                    nameLabel.setFontScale(1.1f);
                }
                @Override
                public void exit(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor to) {
                    nameLabel.setColor(Color.LIGHT_GRAY);
                    nameLabel.setFontScale(1.0f);
                }
            });

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

    // 메서드 설명: SkillData 클래스와 연동하여 영웅의 상세 정보를 팝업창에 출력합니다.
    private void showGridPopup(final String name, final UnitData.Stat stat) {
        Window.WindowStyle windowStyle = new Window.WindowStyle(game.detailFont2, Color.WHITE, dialogBackground);
        final Dialog dialog = new Dialog("", windowStyle);

        Table content = dialog.getContentTable();
        content.pad(10);

        // [테스트용 코드] 이 한 줄로 테이블의 모든 셀 경계선이 보입니다.
        content.setDebug(true);

        // SkillData에서 해당 유닛의 스킬 정보 가져오기
        SkillData.Skill skill = SkillData.get(stat.skillName());

        // 영웅 이미지 처리
        String path = "images/character/" + name + ".png";
        if (Gdx.files.internal(path).exists()) {
            Image heroImg = new Image(new Texture(Gdx.files.internal(path)));
            heroImg.setScaling(Scaling.fit);
            content.add(heroImg).size(360, 480).padTop(10);
        } else {
            content.add().size(300, 400).padTop(10);
        }

        // 스탯 및 상세 설명 출력
        Label infoLabel = new Label(
            "[" + name + "]\n\n" +
                "HP: " + stat.hp() + " | ATK: " + stat.atk() + "\n" +
                "MOVE: " + stat.move() + " | RANGE: " + stat.range() + "\n\n" +
                "SKILL: " + skill.name + "\n" +
                "--------------------------\n" +
                skill.description,
            new Label.LabelStyle(game.detailFont2, Color.LIME)
        );
        infoLabel.setAlignment(com.badlogic.gdx.utils.Align.center);
        infoLabel.setWrap(true);
        content.add(infoLabel).center().width(360).pad(10).row();

        Table btnTable = new Table();
        Label startBtn = new Label("[ 전투 시작 ]", new Label.LabelStyle(game.detailFont2, Color.GOLD));
        startBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (backgroundMusic != null) backgroundMusic.stop();
                game.setScreen(new BattleScreen(game, selectedFaction, name, stat, 1));
                dialog.hide();
            }
        });

        Label closeBtn = new Label("[ 닫기 ]", new Label.LabelStyle(game.detailFont2, Color.LIGHT_GRAY));
        closeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                dialog.hide();
            }
        });

        btnTable.add(startBtn).padRight(50);
        btnTable.add(closeBtn);
        content.add(btnTable).colspan(2).center().padTop(20);

        dialog.show(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // SpriteBatch가 가상 해상도 카메라 시점을 따르도록 투영 행렬을 설정합니다.
        game.batch.setProjectionMatrix(stage.getViewport().getCamera().combined);

        game.batch.begin();
        // 배경을 가상 해상도 크기로 고정해서 그립니다.
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
