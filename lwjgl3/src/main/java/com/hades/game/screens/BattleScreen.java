package com.hades.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.hades.game.HadesGame;
import com.hades.game.constants.GameConfig;
import com.hades.game.constants.SkillData;
import com.hades.game.constants.UnitData;
import com.hades.game.entities.Unit;
import com.hades.game.logic.AILogic;
import com.hades.game.logic.BoardManager;
import com.hades.game.logic.IsoUtils;
import com.hades.game.logic.TurnManager;
import com.hades.game.view.GameUI;
import com.hades.game.view.MapRenderer;
import com.hades.game.view.UnitRenderer;
import com.hades.game.view.UI;

// 실제 전투가 이루어지는 메인 스크린 클래스
public class BattleScreen extends ScreenAdapter {
    private final HadesGame game;
    private ShapeRenderer shape;
    private Stage stage;
    private Array<Unit> units;
    private Vector2 hoveredGrid = new Vector2(-1, -1);
    private Unit selectedUnit = null;
    private TurnManager turnManager;
    private MapRenderer mapRenderer;
    private UnitRenderer unitRenderer;
    private GameUI gameUI;

    private Texture battleBg;
    private Texture tileTop;

    private final String playerTeam;
    private final String aiTeam;
    private final String heroName;
    private final UnitData.Stat heroStat;
    private int stageLevel;

    private float aiDelay = 0;
    private boolean aiBusy = false;
    private boolean gameOver = false;

    private final float MENU_W = 180;
    private final float MENU_H = 60;
    private Rectangle menuHitbox;

    public BattleScreen(HadesGame game, String playerTeam, String heroName, UnitData.Stat heroStat, int stageLevel) {
        this.game = game;
        this.playerTeam = playerTeam;
        this.heroName = heroName;
        this.heroStat = heroStat;
        this.stageLevel = stageLevel;
        this.aiTeam = playerTeam.equals("HADES") ? "ZEUS" : "HADES";

        this.stage = new Stage(new FitViewport(GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT));
        this.menuHitbox = new Rectangle(
            GameConfig.VIRTUAL_WIDTH - MENU_W - 20,
            GameConfig.VIRTUAL_HEIGHT - MENU_H - 20,
            MENU_W,
            MENU_H
        );

        loadResources();
        init();
        gameUI.addLog("STAGE " + stageLevel + " 전투 시작!");
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        if (game.menuBgm != null && game.menuBgm.isPlaying()) game.menuBgm.stop();
        if (game.battleBgm != null && !game.battleBgm.isPlaying()) {
            game.battleBgm.setVolume(game.globalVolume);
            game.battleBgm.play();
        }
    }

    private void loadResources() {
        battleBg = new Texture(Gdx.files.internal("images/background/battle_background.png"));
        tileTop = new Texture(Gdx.files.internal("images/background/tile_top.png"));
    }

    private void init() {
        shape = new ShapeRenderer();
        mapRenderer = new MapRenderer(shape, game.batch, tileTop);
        unitRenderer = new UnitRenderer(game.batch, shape, game.unitFont, playerTeam);
        gameUI = new GameUI(game);

        if (heroStat != null) {
            heroStat.resetSkillStatus();
            heroStat.clearReservedSkill();
        }

        units = new Array<>();
        turnManager = new TurnManager();
        setupBattleUnits();
    }

    private void setupBattleUnits() {
        units.clear();
        units.add(new Unit(heroName, playerTeam, heroStat, heroName, Unit.UnitClass.HERO, 3, 0));
        units.add(new Unit("기병", playerTeam, UnitData.STAT_KNIGHT, UnitData.IMG_KNIGHT, Unit.UnitClass.KNIGHT, 0, 0));
        units.add(new Unit("궁병", playerTeam, UnitData.STAT_ARCHER, UnitData.IMG_ARCHER, Unit.UnitClass.ARCHER, 1, 0));
        units.add(new Unit("방패병", playerTeam, UnitData.STAT_SHIELD, UnitData.IMG_SHIELD, Unit.UnitClass.SHIELD, 2, 0));
        units.add(new Unit("방패병", playerTeam, UnitData.STAT_SHIELD, UnitData.IMG_SHIELD, Unit.UnitClass.SHIELD, 4, 0));
        units.add(new Unit("성녀", playerTeam, UnitData.STAT_SAINT, UnitData.IMG_SAINT, Unit.UnitClass.SAINT, 5, 0));
        units.add(new Unit("전차병", playerTeam, UnitData.STAT_CHARIOT, UnitData.IMG_CHARIOT, Unit.UnitClass.CHARIOT, 6, 0));

        int enemyRow = GameConfig.BOARD_HEIGHT - 1;
        int bossIdx = Math.min(stageLevel - 1, UnitData.STATS_ZEUS.length - 1);
        String bossName = UnitData.NAMES_ZEUS[bossIdx];

        units.add(new Unit(bossName, aiTeam, UnitData.STATS_ZEUS[bossIdx], bossName, Unit.UnitClass.HERO, 3, enemyRow));
        units.add(new Unit("적 궁병", aiTeam, UnitData.STAT_ARCHER, UnitData.IMG_ARCHER, Unit.UnitClass.ARCHER, 0, enemyRow));
        units.add(new Unit("적 기병", aiTeam, UnitData.STAT_KNIGHT, UnitData.IMG_KNIGHT, Unit.UnitClass.KNIGHT, 1, enemyRow));
        units.add(new Unit("적 방패병", aiTeam, UnitData.STAT_SHIELD, UnitData.IMG_SHIELD, Unit.UnitClass.SHIELD, 2, enemyRow));
        units.add(new Unit("적 방패병", aiTeam, UnitData.STAT_SHIELD, UnitData.IMG_SHIELD, Unit.UnitClass.SHIELD, 4, enemyRow));
        units.add(new Unit("적 전차병", aiTeam, UnitData.STAT_CHARIOT, UnitData.IMG_CHARIOT, Unit.UnitClass.CHARIOT, 5, enemyRow));
        units.add(new Unit("적 성녀", aiTeam, UnitData.STAT_SAINT, UnitData.IMG_SAINT, Unit.UnitClass.SAINT, 6, enemyRow));
    }

    @Override
    public void render(float delta) {
        update(delta);
        cleanupDeadUnits();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 정밀 마우스 좌표 계산 (ViewPort unproject 적용)
        Vector2 touchPos = new Vector2(Gdx.input.getX(), Gdx.input.getY());
        stage.getViewport().unproject(touchPos);
        float mx = touchPos.x;
        float my = touchPos.y;

        game.batch.setProjectionMatrix(stage.getViewport().getCamera().combined);
        shape.setProjectionMatrix(stage.getViewport().getCamera().combined);

        game.batch.begin();
        game.batch.draw(battleBg, 0, 0, GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT);
        game.batch.end();

        mapRenderer.drawTiles(hoveredGrid, selectedUnit, units);

        if (!gameOver && selectedUnit != null && selectedUnit.team.equals(playerTeam)) {
            String reserved = selectedUnit.stat.getReservedSkill();
            if (reserved != null) {
                mapRenderer.drawSkillRange(selectedUnit, SkillData.get(reserved).range);
            } else {
                mapRenderer.drawRangeOverlays(selectedUnit);
            }
        }

        game.batch.begin();
        for (Unit u : units) if (u.isAlive()) unitRenderer.renderShadow(u, selectedUnit);
        for (Unit u : units) if (u.isAlive()) unitRenderer.renderBody(u, selectedUnit);

        // GameUI 렌더링 시 정밀 좌표 mx, my를 전달하여 툴팁 판정 문제를 해결
        gameUI.render(stageLevel, turnManager.getCurrentTurn(), playerTeam, menuHitbox, selectedUnit, mx, my);
        game.batch.end();

        if (gameOver) {
            drawGameOverOverlay();
            stage.act();
            stage.draw();
        }
    }

    private void update(float delta) {
        if (gameOver) return;
        if (turnManager.getCurrentTurn().equals(playerTeam)) {
            aiBusy = false;
            aiDelay = 0;
            handleInput();
        } else {
            aiDelay += delta;
            if (aiDelay >= 1.0f) {
                if (aiBusy) {
                    try { AILogic.processAITurn(units, aiTeam, turnManager, this); }
                    catch (Exception e) { turnManager.endTurn(); }
                    aiBusy = false;
                    aiDelay = 0;
                    selectedUnit = null;
                } else {
                    aiBusy = true;
                }
            }
        }
    }

    private void handleInput() {
        com.hades.game.utils.DebugManager.handleBattleDebug(game, units, aiTeam, this::handleDeath);

        if (gameOver) return;
        Vector2 touchPos = new Vector2(Gdx.input.getX(), Gdx.input.getY());
        stage.getViewport().unproject(touchPos);
        float mx = touchPos.x;
        float my = touchPos.y;

        if (Gdx.input.justTouched()) {
            if (menuHitbox.contains(mx, my)) {
                game.playClick();
                toggleFullscreen();
                return;
            }

            if (selectedUnit != null && selectedUnit.team.equals(playerTeam) && selectedUnit.unitClass == Unit.UnitClass.HERO) {
                String clickedSkill = gameUI.getClickedSkill(mx, my, selectedUnit);
                if (clickedSkill != null) {
                    String currentReserved = selectedUnit.stat.getReservedSkill();

                    if (clickedSkill.equals(currentReserved)) {
                        selectedUnit.stat.clearReservedSkill();
                        gameUI.addLog(clickedSkill + " 장전 취소", "SYSTEM", playerTeam);
                    } else {
                        selectedUnit.stat.setReservedSkill(clickedSkill);
                        gameUI.addLog(clickedSkill + " 장전됨! 이동 시 발동.", selectedUnit.team, playerTeam);
                    }
                    game.playClick(1.1f);
                    return;
                }
            }
        }

        if (!turnManager.getCurrentTurn().equals(playerTeam) || aiBusy) return;
        hoveredGrid = IsoUtils.screenToGrid(mx, my);

        if (Gdx.input.justTouched()) {
            if (selectedUnit != null) {
                int tx = (int) hoveredGrid.x;
                int ty = (int) hoveredGrid.y;
                if (tx >= 0 && ty >= 0 && selectedUnit.team.equals(playerTeam) && BoardManager.canMoveTo(selectedUnit, tx, ty, units)) {
                    selectedUnit.setPosition(tx, ty);

                    processMoveEnd(selectedUnit);

                    selectedUnit = null;
                    aiBusy = true;
                    turnManager.endTurn();
                    return;
                }
            }
            Unit clickedUnit = null;
            for (Unit u : units) {
                if (u.isAlive() && unitRenderer.isMouseInsideHitbox(u, mx, my)) {
                    clickedUnit = u;
                    game.playClick();
                    break;
                }
            }
            selectedUnit = clickedUnit;
        }
    }

    public void processMoveEnd(Unit unit) {
        String reserved = unit.stat.getReservedSkill();

        // [1순위] 영웅의 권능 처리 (하데스 or AI 보스)
        if (reserved != null && !reserved.equals("기본 공격")) {
            if (isAnyTargetInRange(unit, reserved)) {
                executeActiveSkill(unit, reserved); // 스킬 발동
                unit.stat.setSkillUsed(reserved, true);
            } else {
                // 사거리가 안 닿을 때 (플레이어에게만 알림 전송)
                if (unit.team.equals(playerTeam)) {
                    gameUI.addLog("사거리 내 적이 없어 " + reserved + " 취소", "SYSTEM", playerTeam);
                }
            }
            unit.stat.clearReservedSkill(); // 장전 해제
        }

        // [2순위] 이후 해당 진영 전체 자동 공격 (병사들의 평타 타임)
        processAutoAttack(unit.team);
    }

    // 특정 스킬의 사거리 내에 적이 존재하는지 판별합니다.
    private boolean isAnyTargetInRange(Unit hero, String skillName) {
        SkillData.Skill data = SkillData.get(skillName);
        for (Unit target : units) {
            if (target.isAlive() && !target.team.equals(hero.team)) {
                int dist = Math.abs(hero.gridX - target.gridX) + Math.abs(hero.gridY - target.gridY);
                if (dist <= data.range) return true;
            }
        }
        return false;
    }

    private void executeActiveSkill(Unit hero, String skillName) {
        SkillData.Skill data = SkillData.get(skillName);
        gameUI.addLog("권능 해방!! [" + skillName + "]", hero.team, playerTeam);
        int damage = (int)(hero.stat.atk() * data.power);
        for (int i = 0; i < units.size; i++) {
            Unit target = units.get(i);
            if (target != null && target.isAlive() && !target.team.equals(hero.team)) {
                int dist = Math.abs(hero.gridX - target.gridX) + Math.abs(hero.gridY - target.gridY);
                if (dist <= data.range) {
                    target.currentHp -= damage;
                    gameUI.addLog(target.name + "에게 " + damage + "의 피해!", hero.team, playerTeam);
                    if (target.currentHp <= 0) {
                        target.currentHp = 0;
                        target.status = Unit.DEAD;
                        gameUI.addLog(target.name + " 처치됨!", hero.team, playerTeam);
                        handleDeath(target);
                    }
                    if (!data.isAoE) break;
                }
            }
        }
        processAutoHeal(hero.team);
    }

    public void processAutoAttack(String team) {
        for (int i = 0; i < units.size; i++) {
            Unit attacker = units.get(i);
            if (attacker != null && attacker.isAlive() && attacker.team.equals(team)) {
                if (attacker.unitClass == Unit.UnitClass.KNIGHT) {
                    Array<Unit> targets = BoardManager.findAllTargetsInRange(attacker, units);
                    for (int j = 0; j < targets.size; j++) performAttack(attacker, targets.get(j));
                } else {
                    Unit target = BoardManager.findBestTargetInRange(attacker, units);
                    if (target != null) performAttack(attacker, target);
                }
            }
        }
        processAutoHeal(team);
    }

    private void processAutoHeal(String team) {
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            if (u.isAlive() && u.team.equals(team) && u.unitClass == Unit.UnitClass.SAINT) {
                for (int j = 0; j < units.size; j++) {
                    Unit ally = units.get(j);
                    if (ally.isAlive() && ally.team.equals(team) && ally != u) {
                        int dist = Math.abs(u.gridX - ally.gridX) + Math.abs(u.gridY - ally.gridY);
                        if (dist == 1 && ally.currentHp < ally.stat.hp()) {
                            ally.currentHp = Math.min(ally.stat.hp(), ally.currentHp + 15);
                            gameUI.addLog(u.name + "가 " + ally.name + "를 치료함(+15)", u.team, playerTeam);
                        }
                    }
                }
            }
        }
    }

    public void performAttack(Unit attacker, Unit target) {
        // 공격자와 방어자의 생존 여부 및 존재 확인
        if (attacker == null || target == null || !target.isAlive() || !attacker.isAlive()) return;

        boolean isAttackerTurn = turnManager.isMyTurn(attacker.team);
        int damage = attacker.getPower(isAttackerTurn); // 기본 능력치 기반 공격력 계산
        float skillMultiplier = 1.0f;
        String activeSkillName = null;

        // --- 영웅 유닛 전용 권능/진노 발동 판단 ---
        if (attacker.unitClass == Unit.UnitClass.HERO) {
            if (!attacker.team.equals(playerTeam)) {
                // [AI 보스] 설정된 전용 권능을 매 공격 시 자동으로 사용
                activeSkillName = attacker.stat.skillName();
                SkillData.Skill skill = SkillData.get(activeSkillName);
                skillMultiplier = skill.power;

                gameUI.addLog("[권능] " + attacker.name + ": [" + activeSkillName + "]!", attacker.team, playerTeam);
            } else {
                // [플레이어 하데스] UI에서 선택하여 '장전된(Reserved)' 스킬이 있을 때만 발동
                String reserved = attacker.stat.getReservedSkill();
                if (reserved != null && !reserved.equals("기본 공격")) {
                    activeSkillName = reserved;
                    SkillData.Skill skill = SkillData.get(activeSkillName);
                    skillMultiplier = skill.power;

                    gameUI.addLog("[권능] " + attacker.name + ": [" + activeSkillName + "]!", attacker.team, playerTeam);

                    // 사용 완료된 스킬 소모 처리 및 쿨타임 관리
                    attacker.stat.clearReservedSkill();
                    attacker.stat.setSkillUsed(activeSkillName, true);
                }
            }
        }

        // 최종 데미지 적용 및 로그 출력
        int finalDamage = (int)(damage * skillMultiplier);
        target.currentHp -= finalDamage;

        if (activeSkillName != null) {
            gameUI.addLog(attacker.name + " -> " + target.name + " [" + activeSkillName + "] " + finalDamage + " 데미지", attacker.team, playerTeam);
        } else {
            gameUI.addLog(attacker.name + " -> " + target.name + " " + finalDamage + " 데미지", attacker.team, playerTeam);
        }

        // --- 처치 판정 및 반격 로직 ---
        if (target.currentHp <= 0) {
            target.currentHp = 0;
            gameUI.addLog(target.name + " 처치됨!", attacker.team, playerTeam);
            handleDeath(target);
            return;
        }

        // 방어자의 사거리 내에 공격자가 있다면 즉시 반격 실행
        if (target.canReach(attacker)) {
            int counterDamage = target.getPower(turnManager.isMyTurn(target.team));
            attacker.currentHp -= counterDamage;
            gameUI.addLog(target.name + "의 반격! " + counterDamage + " 데미지", target.team, playerTeam);

            if (attacker.currentHp <= 0) {
                attacker.currentHp = 0;
                gameUI.addLog(attacker.name + " 처치됨!", target.team, playerTeam);
                handleDeath(attacker);
            }
        }
    }



    private void handleDeath(Unit target) {
        target.status = Unit.DEAD;
        boolean isEnemyBoss = target.team.equals(aiTeam) && target.unitClass == Unit.UnitClass.HERO;
        boolean isPlayerHero = target.team.equals(playerTeam) && target.unitClass == Unit.UnitClass.HERO;

        if (isEnemyBoss) {
            gameOver = true;
            if (stageLevel == 7) {
                game.setScreen(new com.hades.game.screens.cutscene.BaseCutsceneScreen(
                    game,
                    com.hades.game.screens.cutscene.CutsceneManager.getStageData(8),
                    new EndingScreen(game)
                ));
            } else {
                gameUI.addLog("승리! 적의 수장을 물리쳤습니다.");
                showGameOverMenu(true);
            }
        } else if (isPlayerHero) {
            gameOver = true;
            gameUI.addLog("패배... 하데스의 영웅이 전사했습니다.");
            showGameOverMenu(false);
        }
    }

    private void showGameOverMenu(boolean isVictory) {
        Table table = new Table();
        table.setFillParent(true);
        table.center();
        Label titleLabel = new Label(isVictory ? "VICTORY!" : "DEFEAT...", new Label.LabelStyle(game.titleFont, isVictory ? Color.GOLD : Color.FIREBRICK));
        table.add(titleLabel).padBottom(60).row();

        if (isVictory) {
            int rewardSouls = (int)(Math.random() * 3) + 1;
            game.soulFragments += rewardSouls;
            game.olympusSeals += 1;
            Label rewardLabel = new Label("보상: 영혼 파편 +" + rewardSouls + ", 인장 +1", new Label.LabelStyle(game.mainFont, Color.CYAN));
            table.add(rewardLabel).padBottom(20).row();
            Label upgradeBtn = new Label("[ 명계의 제단으로 ]", new Label.LabelStyle(game.mainFont, Color.valueOf("4FB9AF")));
            upgradeBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    game.playClick();
                    game.setScreen(new UpgradeScreen(game, heroName, heroStat, stageLevel));
                }
            });
            UI.addHoverEffect(game, upgradeBtn, Color.valueOf("4FB9AF"), Color.WHITE);
            table.add(upgradeBtn).padBottom(20).row();
        } else {
            Label retryBtn = new Label("[ RE-TRY ]", new Label.LabelStyle(game.mainFont, Color.WHITE));
            retryBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    game.setScreen(new BattleScreen(game, playerTeam, heroName, heroStat, stageLevel));
                }
            });
            UI.addHoverEffect(game, retryBtn, Color.WHITE, Color.GOLD);
            table.add(retryBtn).padBottom(20).row();
        }
        Label homeBtn = new Label("[ BACK TO MENU ]", new Label.LabelStyle(game.mainFont, Color.LIGHT_GRAY));
        homeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.playClick();
                resetMusicToHome();
                game.setScreen(new MenuScreen(game));
            }
        });
        UI.addHoverEffect(game, homeBtn, Color.LIGHT_GRAY, Color.WHITE);
        table.add(homeBtn);
        stage.addActor(table);
        Gdx.input.setInputProcessor(stage);
    }

    private void resetMusicToHome() {
        if (game.battleBgm != null) game.battleBgm.stop();
        if (game.menuBgm != null) {
            game.menuBgm.setVolume(game.globalVolume);
            game.menuBgm.play();
        }
    }

    private void cleanupDeadUnits() {
        for (int i = units.size - 1; i >= 0; i--) {
            if (units.get(i).status == Unit.DEAD) {
                if (selectedUnit == units.get(i)) selectedUnit = null;
                units.removeIndex(i);
            }
        }
    }

    private void toggleFullscreen() {
        if (Gdx.graphics.isFullscreen()) Gdx.graphics.setWindowedMode((int) GameConfig.VIRTUAL_WIDTH, (int) GameConfig.VIRTUAL_HEIGHT);
        else Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
    }

    private void drawGameOverOverlay() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0, 0, 0, 0.7f);
        shape.rect(0, 0, GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT);
        shape.end();
    }

    @Override
    public void resize(int width, int height) { stage.getViewport().update(width, height, true); }

    @Override
    public void dispose() {
        if (shape != null) shape.dispose();
        if (unitRenderer != null) unitRenderer.dispose();
        if (stage != null) stage.dispose();
        if (gameUI != null) gameUI.dispose();
        battleBg.dispose();
        tileTop.dispose();
    }
}
