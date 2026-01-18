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
import com.hades.game.view.UI;

// Chess Olympus: HADES vs ZEUS - 영웅 강화 화면 (명계의 제단)
public class UpgradeScreen extends ScreenAdapter {
    private final HadesGame game;
    private final UnitData.Stat heroStat;
    private final String heroName;
    private final Stage stage;
    private final Texture background;
    private Texture heroTexture;

    private Label hpLabel, atkLabel, soulLabel, sealLabel, messageLabel, currentSkillLabel;
    private Table mainTable, skillSelectionTable;
    private Array<String> fixedSkillOptions;

    public UpgradeScreen(HadesGame game, String heroName, UnitData.Stat stat, int currentStage) {
        this.game = game;
        this.heroName = heroName;
        // [중요] 항상 전역 상태인 RunState의 스탯을 직접 참조하여 데이터 일관성 유지
        this.heroStat = (game.runState.heroStat != null) ? game.runState.heroStat : stat;
        this.stage = new Stage(new FitViewport(GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT));

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

        // 상단 재화 표시 바
        Table topBar = new Table();
        topBar.setBackground(UI.getColoredDrawable(0, 0, 0, 0.6f));
        topBar.pad(10, 40, 10, 40);

        soulLabel = new Label("영혼 파편: " + game.runState.soulFragments, new Label.LabelStyle(game.mainFont, Color.CYAN));
        sealLabel = new Label("올림포스 인장: " + game.runState.olympusSeals, new Label.LabelStyle(game.mainFont, Color.GOLD));

        topBar.add(soulLabel).padRight(50);
        topBar.add(sealLabel);
        mainTable.add(topBar).top().padTop(20).row();

        // 중앙 콘텐츠 (일러스트 및 강화 옵션)
        Table contentTable = new Table();
        if (heroTexture != null) {
            Image heroImg = new Image(heroTexture);
            heroImg.setScaling(Scaling.fit);
            contentTable.add(heroImg).size(420, 520).padRight(50);
        }

        Table rightSide = new Table().align(Align.left);
        rightSide.setBackground(UI.getColoredDrawable(0, 0, 0, 0.5f));
        rightSide.pad(30, 40, 30, 40);

        rightSide.add(new Label(heroName + "의 각성", new Label.LabelStyle(game.detailFont, Color.WHITE)))
            .colspan(2).left().padBottom(30).row();

        // 체력 강화
        hpLabel = new Label("최대 체력: " + heroStat.hp(), new Label.LabelStyle(game.detailFont, Color.WHITE));
        Label hpPlus = new Label("[+]", new Label.LabelStyle(game.detailFont, Color.CYAN));
        hpPlus.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (game.runState.soulFragments > 0) {
                    game.playClick();
                    game.runState.soulFragments--;
                    heroStat.setHp(heroStat.hp() + 30);
                    game.saveGame();
                    updateUI();
                } else {
                    showStatusMessage("영혼 파편이 부족합니다.");
                }
            }
        });

        // 공격력 강화
        atkLabel = new Label("공격력: " + heroStat.atk(), new Label.LabelStyle(game.detailFont, Color.WHITE));
        Label atkPlus = new Label("[+]", new Label.LabelStyle(game.detailFont, Color.CYAN));
        atkPlus.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (game.runState.soulFragments > 0) {
                    game.playClick();
                    game.runState.soulFragments--;
                    heroStat.setAtk(heroStat.atk() + 5);
                    game.saveGame();
                    updateUI();
                } else {
                    showStatusMessage("영혼 파편이 부족합니다.");
                }
            }
        });

        rightSide.add(hpLabel).left().width(280);
        rightSide.add(hpPlus).left().row();
        rightSide.add(atkLabel).left().width(280).padTop(15);
        rightSide.add(atkPlus).left().padTop(15).row();

        // 스킬 해제 버튼
        Label skillUnlockBtn = new Label("[ 랜덤 권능 봉인 해제 ]", new Label.LabelStyle(game.detailFont, Color.GOLD));
        skillUnlockBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (game.runState.olympusSeals > 0) {
                    game.playClick();
                    openSkillSelection();
                } else {
                    showStatusMessage("올림포스 인장이 부족합니다.");
                }
            }
        });
        UI.addHoverEffect(game, skillUnlockBtn, Color.GOLD, Color.WHITE);
        rightSide.add(skillUnlockBtn).colspan(2).left().padTop(50).row();

        currentSkillLabel = new Label("보유 권능: " + heroStat.getLearnedSkills().size + "개",
            new Label.LabelStyle(game.detailFont, Color.LIGHT_GRAY));
        rightSide.add(currentSkillLabel).colspan(2).left().padTop(10).row();

        contentTable.add(rightSide);
        mainTable.add(contentTable).center().expandY().row();

        messageLabel = new Label("", new Label.LabelStyle(game.detailFont, Color.YELLOW));
        mainTable.add(messageLabel).padBottom(15).row();

        // 나가기 버튼 (스테이지 맵으로 복귀)
        Label exitBtn = new Label("여정 계속하기", new Label.LabelStyle(game.mainFont, Color.WHITE));
        exitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.playClick();
                game.setScreen(new StageMapScreen(game));
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
        skillSelectionTable.setBackground(UI.getColoredDrawable(0, 0, 0, 0.92f));
        stage.addActor(skillSelectionTable);
    }

    private void openSkillSelection() {
        skillSelectionTable.clear();
        skillSelectionTable.setVisible(true);
        mainTable.setVisible(false);

        Label title = new Label("운명의 갈림길", new Label.LabelStyle(game.detailFont2, Color.GOLD));
        skillSelectionTable.add(title).colspan(2).padBottom(40).row();

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
                card.setBackground(UI.getColoredDrawable(0.1f, 0.1f, 0.15f, 0.9f));

                Label name = new Label(skill.name, new Label.LabelStyle(game.unitFont2, Color.GOLD));
                Label desc = new Label(skill.description, new Label.LabelStyle(game.unitFont3, Color.WHITE));
                desc.setWrap(true);
                desc.setAlignment(Align.center);

                card.add(name).padBottom(15).row();
                card.add(desc).width(240).padBottom(25).row();

                Label selectBtn = new Label("[ 수락 ]", new Label.LabelStyle(game.detailFont, Color.LIME));
                card.add(selectBtn);
                card.pad(30);

                card.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        game.playClick(1.2f);
                        heroStat.addSkill(skill.name);
                        game.runState.olympusSeals--;
                        game.saveGame();
                        fixedSkillOptions = null;
                        closeSkillSelection();
                        showStatusMessage(skill.name + " 권능 획득!");
                        updateUI();
                    }
                });

                UI.addHoverEffect(game, card, Color.valueOf("1A1A1A"), Color.valueOf("333333"));
                skillSelectionTable.add(card).pad(15).width(300).height(320);
            }
        }

        skillSelectionTable.row();
        Label cancelBtn = new Label("[ 돌아가기 ]", new Label.LabelStyle(game.detailFont, Color.GRAY));
        cancelBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                closeSkillSelection();
            }
        });
        skillSelectionTable.add(cancelBtn).colspan(2).padTop(40);
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
        soulLabel.setText("영혼 파편: " + game.runState.soulFragments);
        sealLabel.setText("올림포스 인장: " + game.runState.olympusSeals);
        currentSkillLabel.setText("보유 권능: " + heroStat.getLearnedSkills().size + "개");
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        game.batch.setProjectionMatrix(stage.getViewport().getCamera().combined);
        game.batch.begin();
        game.batch.setColor(0.7f, 0.7f, 0.7f, 1f);
        game.batch.draw(background, 0, 0, GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT);
        game.batch.setColor(Color.WHITE);
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
