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
    private Label currentSkillLabel;

    private Table mainTable;
    private Table skillSelectionTable;

    // 리세마라 방지를 위해 이번 회차에 생성된 스킬 목록을 저장합니다.
    private Array<String> fixedSkillOptions;

    public UpgradeScreen(HadesGame game, String heroName, UnitData.Stat stat, int currentStage) {
        this.game = game;
        this.heroName = heroName;
        this.heroStat = stat;
        this.currentStage = currentStage;
        this.stage = new Stage(new FitViewport(GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT));

        // 배경 이미지를 upgrade.png로 변경하였습니다.
        this.background = new Texture(Gdx.files.internal("images/background/upgrade.png"));

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

        // 1. 상단 재화 표시 (배경과의 대비를 위해 어두운 배경 바 추가 고려 가능)
        Table topBar = new Table();
        topBar.setBackground(UI.getColoredDrawable(0, 0, 0, 0.6f)); // 상단 바 가독성을 위해 반투명 배경 추가
        topBar.pad(10, 40, 10, 40);

        soulLabel = new Label("영혼 파편: " + game.soulFragments, new Label.LabelStyle(game.mainFont, Color.CYAN));
        sealLabel = new Label("올림포스 인장: " + game.olympusSeals, new Label.LabelStyle(game.mainFont, Color.GOLD));

        topBar.add(soulLabel).padRight(50);
        topBar.add(sealLabel);
        mainTable.add(topBar).top().padTop(20).row();

        // 2. 중앙 레이아웃
        Table contentTable = new Table();
        if (heroTexture != null) {
            Image heroImg = new Image(heroTexture);
            heroImg.setScaling(Scaling.fit);
            contentTable.add(heroImg).size(420, 520).padRight(50);
        }

        // 스탯 정보 영역: 배경이 화려하므로 반투명 패널을 깔아 가독성 확보
        Table rightSide = new Table().align(Align.left);
        rightSide.setBackground(UI.getColoredDrawable(0, 0, 0, 0.5f)); // 가독성 핵심: 어두운 패널 추가
        rightSide.pad(30, 40, 30, 40);

        rightSide.add(new Label(heroName + "의 각성 (파편 소모)", new Label.LabelStyle(game.detailFont, Color.WHITE))).padBottom(30).row();

        // 체력 강화 로직
        hpLabel = new Label("최대 체력: " + heroStat.hp(), new Label.LabelStyle(game.detailFont, Color.WHITE));
        Label hpPlus = new Label("[+] ", new Label.LabelStyle(game.detailFont, Color.CYAN));
        hpPlus.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (game.soulFragments > 0) {
                    game.playClick();
                    game.soulFragments--;
                    heroStat.setHp(heroStat.hp() + 10);
                    updateUI();
                } else {
                    showStatusMessage("영혼 파편이 부족합니다.");
                }
            }
        });

        // 공격력 강화 로직
        atkLabel = new Label("공격력: " + heroStat.atk(), new Label.LabelStyle(game.detailFont, Color.WHITE));
        Label atkPlus = new Label("[+]", new Label.LabelStyle(game.detailFont, Color.CYAN));
        atkPlus.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (game.soulFragments > 0) {
                    game.playClick();
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
        rightSide.add(atkLabel).left().padTop(15);
        rightSide.add(atkPlus).padLeft(20).padTop(15).row();

        // 권능 봉인 해제 버튼
        Label skillUnlockBtn = new Label("[ 랜덤 권능 봉인 해제 ]", new Label.LabelStyle(game.detailFont, Color.GOLD));
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
        rightSide.add(skillUnlockBtn).padTop(50).row();

        currentSkillLabel = new Label("보유 권능: " + heroStat.getLearnedSkills().size + "개", new Label.LabelStyle(game.detailFont, Color.LIGHT_GRAY));
        rightSide.add(currentSkillLabel).padTop(10).row();

        contentTable.add(rightSide);
        mainTable.add(contentTable).center().expandY().row();

        messageLabel = new Label("", new Label.LabelStyle(game.detailFont, Color.YELLOW));
        mainTable.add(messageLabel).padBottom(15).row();

        // 3. 하단 여정 계속하기 버튼
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
        mainTable.add(exitBtn).bottom().padBottom(30);

        createSkillSelectionPopup();
    }

    private void createSkillSelectionPopup() {
        skillSelectionTable = new Table();
        skillSelectionTable.setFillParent(true);
        skillSelectionTable.setVisible(false);
        skillSelectionTable.setBackground(UI.getColoredDrawable(0, 0, 0, 0.92f)); // 팝업 시 배경을 더 어둡게 처리
        stage.addActor(skillSelectionTable);
    }

    private void openSkillSelection() {
        skillSelectionTable.clear();
        skillSelectionTable.setVisible(true);
        mainTable.setVisible(false);

        Label title = new Label("운명의 갈림길: 두 가지 권능 중 하나를 선택하십시오", new Label.LabelStyle(game.detailFont2, Color.GOLD));
        skillSelectionTable.add(title).colspan(2).padBottom(60).row();

        if (fixedSkillOptions == null) {
            fixedSkillOptions = SkillData.getRandomSkills(2, heroStat.getLearnedSkills());
        }

        if (fixedSkillOptions.size == 0) {
            Label emptyLabel = new Label("더 이상 습득할 수 있는 권능이 없습니다.", new Label.LabelStyle(game.mainFont, Color.WHITE));
            skillSelectionTable.add(emptyLabel).colspan(2).padBottom(20).row();
        } else {
            for (final String sName : fixedSkillOptions) {
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
                        fixedSkillOptions = null;
                        closeSkillSelection();
                        showStatusMessage(skill.name + " 권능이 영혼에 깃들었습니다.");
                        updateUI();
                    }
                });

                UI.addHoverEffect(game, card, Color.valueOf("1A1A1A"), Color.valueOf("2A2A2A"));
                skillSelectionTable.add(card).pad(20).width(350).height(400);
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

        // 배경 이미지를 약간 어둡게 그려서 UI가 돋보이게 합니다. (색상 값 0.7f 적용)
        game.batch.setColor(0.7f, 0.7f, 0.7f, 1f);
        game.batch.draw(background, 0, 0, GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT);
        game.batch.setColor(Color.WHITE); // 다음 렌더링을 위해 색상 초기화

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
