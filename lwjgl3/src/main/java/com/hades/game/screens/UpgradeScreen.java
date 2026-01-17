package com.hades.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.hades.game.HadesGame;
import com.hades.game.constants.GameConfig;
import com.hades.game.constants.SkillData;
import com.hades.game.constants.UnitData;
import com.hades.game.screens.cutscene.BaseCutsceneScreen;
import com.hades.game.screens.cutscene.CutsceneManager;
import com.hades.game.view.UI;

// 영웅의 능력치를 강화하고 새로운 권능(스킬)을 해금하는 화면입니다.
public class UpgradeScreen extends ScreenAdapter {
    private final HadesGame game;
    private final UnitData.Stat heroStat;
    private final String heroName;
    private final int currentStage;
    private final Stage stage;
    private final Texture background;
    private Texture heroTexture;

    private Label hpLabel;
    private Label atkLabel;
    private Label soulLabel;
    private Label sealLabel;
    private Label messageLabel;
    private Label currentSkillLabel; // 갱신을 위해 필드로 분리

    private Table mainTable;
    private Table skillSelectionTable;

    public UpgradeScreen(HadesGame game, String heroName, UnitData.Stat stat, int currentStage) {
        this.game = game;
        this.heroName = heroName;
        this.heroStat = stat;
        this.currentStage = currentStage;
        this.stage = new Stage(new FitViewport(GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT));
        this.background = new Texture(Gdx.files.internal("images/background/main.png"));

        String path = "images/character/" + heroName + ".png";
        if (Gdx.files.internal(path).exists()) {
            this.heroTexture = new Texture(Gdx.files.internal(path));
            this.heroTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }

        initUI();
    }

    private void initUI() {
        mainTable = new Table();
        mainTable.setFillParent(true);
        stage.addActor(mainTable);

        // 1. 상단 재화 표시
        Table topBar = new Table();
        soulLabel = new Label("영혼 파편: " + game.soulFragments, new Label.LabelStyle(game.mainFont, Color.CYAN));
        sealLabel = new Label("올림포스 인장: " + game.olympusSeals, new Label.LabelStyle(game.mainFont, Color.GOLD));
        topBar.add(soulLabel).padRight(50);
        topBar.add(sealLabel);
        mainTable.add(topBar).top().padTop(30).row();

        // 2. 중앙 레이아웃
        Table contentTable = new Table();
        if (heroTexture != null) {
            Image heroImg = new Image(heroTexture);
            heroImg.setScaling(Scaling.fit);
            contentTable.add(heroImg).size(400, 500).padRight(60);
        }

        Table rightSide = new Table().align(Align.left);
        rightSide.add(new Label(heroName + "의 각성", new Label.LabelStyle(game.subtitleFont, Color.WHITE))).padBottom(40).row();

        hpLabel = new Label("최대 체력: " + heroStat.hp(), new Label.LabelStyle(game.mainFont, Color.LIGHT_GRAY));
        Label hpPlus = new Label(" [+] ", new Label.LabelStyle(game.mainFont, Color.LIME));
        hpPlus.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (game.soulFragments > 0) {
                    game.soulFragments--;
                    heroStat.setHp(heroStat.hp() + 10);
                    updateUI();
                } else {
                    showStatusMessage("영혼 파편이 부족합니다.");
                }
            }
        });

        atkLabel = new Label("공격력: " + heroStat.atk(), new Label.LabelStyle(game.mainFont, Color.LIGHT_GRAY));
        Label atkPlus = new Label(" [+] ", new Label.LabelStyle(game.mainFont, Color.LIME));
        atkPlus.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (game.soulFragments > 0) {
                    game.soulFragments--;
                    heroStat.setAtk(heroStat.atk() + 2);
                    updateUI();
                } else {
                    showStatusMessage("영혼 파편이 부족합니다.");
                }
            }
        });

        rightSide.add(hpLabel).left();
        rightSide.add(hpPlus).padLeft(20).row();
        rightSide.add(atkLabel).left().padTop(20);
        rightSide.add(atkPlus).padLeft(20).padTop(20).row();

        Label skillUnlockBtn = new Label("[ 랜덤 권능 봉인 해제 ]", new Label.LabelStyle(game.mainFont, Color.GOLD));
        skillUnlockBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (game.olympusSeals > 0) {
                    game.playClick();
                    openSkillSelection();
                } else {
                    showStatusMessage("올림포스 인장이 부족합니다.");
                }
            }
        });
        UI.addHoverEffect(game, skillUnlockBtn, Color.GOLD, Color.WHITE);
        rightSide.add(skillUnlockBtn).padTop(60).row();

        // 현재 보유한 스킬 개수 표시
        currentSkillLabel = new Label("보유 권능: " + heroStat.getLearnedSkills().size + "개", new Label.LabelStyle(game.detailFont, Color.GRAY));
        rightSide.add(currentSkillLabel).padTop(10).row();

        contentTable.add(rightSide);
        mainTable.add(contentTable).center().expandY().row();

        messageLabel = new Label("", new Label.LabelStyle(game.detailFont, Color.YELLOW));
        mainTable.add(messageLabel).padBottom(20).row();

        Label exitBtn = new Label("여정 계속하기", new Label.LabelStyle(game.mainFont, Color.WHITE));
        exitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.playClick();
                int nextStage = currentStage + 1;
                if (nextStage <= 7) {
                    game.setScreen(new BaseCutsceneScreen(game, CutsceneManager.getStageData(nextStage),
                        new BattleScreen(game, "HADES", heroName, heroStat, nextStage)));
                } else {
                    game.setScreen(new MenuScreen(game));
                }
            }
        });
        UI.addHoverEffect(game, exitBtn, Color.WHITE, Color.GOLD);
        mainTable.add(exitBtn).bottom().padBottom(40);

        createSkillSelectionPopup();
    }

    private void createSkillSelectionPopup() {
        skillSelectionTable = new Table();
        skillSelectionTable.setFillParent(true);
        skillSelectionTable.setVisible(false);
        skillSelectionTable.setBackground(UI.getColoredDrawable(0, 0, 0, 0.9f));
        stage.addActor(skillSelectionTable);
    }

    private void openSkillSelection() {
        skillSelectionTable.clear();
        skillSelectionTable.setVisible(true);
        mainTable.setVisible(false);

        Label title = new Label("운명의 갈림길: 두 가지 권능 중 하나를 선택하십시오", new Label.LabelStyle(game.detailFont2, Color.GOLD));
        skillSelectionTable.add(title).colspan(2).padBottom(60).row();

        // SkillData에서 이미 배운 스킬을 제외한 2개의 랜덤 스킬 추출
        Array<String> randomOptions = SkillData.getRandomSkills(2, heroStat.getLearnedSkills());

        if (randomOptions.size == 0) {
            Label emptyLabel = new Label("더 이상 습득할 수 있는 권능이 없습니다.", new Label.LabelStyle(game.mainFont, Color.WHITE));
            skillSelectionTable.add(emptyLabel).colspan(2).padBottom(20).row();
        } else {
            for (final String sName : randomOptions) {
                final SkillData.Skill skill = SkillData.get(sName);

                Table card = new Table();
                card.setBackground(UI.getColoredDrawable(0.05f, 0.05f, 0.1f, 0.95f));

                Label name = new Label(skill.name, new Label.LabelStyle(game.mainFont, Color.GOLD));
                Label desc = new Label(skill.description, new Label.LabelStyle(game.unitFont3, Color.WHITE));
                desc.setWrap(true);
                desc.setAlignment(Align.center);

                card.add(name).padBottom(20).row();
                card.add(desc).width(280).padBottom(40).row();

                Label selectBtn = new Label("[ 수락 ]", new Label.LabelStyle(game.mainFont, Color.LIME));
                card.add(selectBtn);
                card.pad(50);

                card.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        game.playClick(1.2f);
                        heroStat.addSkill(skill.name);
                        game.olympusSeals--;

                        closeSkillSelection();
                        showStatusMessage(skill.name + " 권능이 영혼에 깃들었습니다.");
                        updateUI();
                    }
                });

                UI.addHoverEffect(game, card, Color.valueOf("1A1A1A"), Color.valueOf("2A2A2A"));
                skillSelectionTable.add(card).pad(20).width(200).height(200); // 권능 표시 레이어 크기
            }
        }

        skillSelectionTable.row();
        Label cancelBtn = new Label("[ 돌아가기 ]", new Label.LabelStyle(game.mainFont, Color.GRAY));
        cancelBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                closeSkillSelection();
            }
        });
        skillSelectionTable.add(cancelBtn).colspan(2).padTop(60);
    }

    private void closeSkillSelection() {
        skillSelectionTable.setVisible(false);
        mainTable.setVisible(true);
    }

    private void showStatusMessage(String text) {
        messageLabel.setText(text);
        messageLabel.getColor().a = 1;
        messageLabel.clearActions();
        messageLabel.addAction(Actions.sequence(Actions.delay(2f), Actions.fadeOut(1f)));
    }

    private void updateUI() {
        hpLabel.setText("최대 체력: " + heroStat.hp());
        atkLabel.setText("공격력: " + heroStat.atk());
        soulLabel.setText("영혼 파편: " + game.soulFragments);
        sealLabel.setText("올림포스 인장: " + game.olympusSeals);
        currentSkillLabel.setText("보유 권능: " + heroStat.getLearnedSkills().size + "개");
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        game.batch.setProjectionMatrix(stage.getViewport().getCamera().combined);
        game.batch.begin();
        game.batch.draw(background, 0, 0, GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT);
        game.batch.end();
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void show() { Gdx.input.setInputProcessor(stage); }

    @Override
    public void dispose() {
        stage.dispose();
        background.dispose();
        if (heroTexture != null) heroTexture.dispose();
    }
}
