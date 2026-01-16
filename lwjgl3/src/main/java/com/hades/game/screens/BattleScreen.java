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

// 실제 턴제 전투가 이루어지는 메인 게임 화면 클래스입니다.
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

        // 전투 시작 로그 출력
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

        units = new Array<>();
        turnManager = new TurnManager();
        setupBattleUnits();
    }

    // 플레이어와 AI의 유닛들을 전장에 배치합니다.
    private void setupBattleUnits() {
        units.clear();
        // 플레이어 진영 (HADES)
        units.add(new Unit(heroName, playerTeam, heroStat, Unit.UnitClass.HERO, 3, 0));
        units.add(new Unit("궁병", playerTeam, UnitData.CLASS_ARCHER, Unit.UnitClass.ARCHER, 0, 0));
        units.add(new Unit("기병", playerTeam, UnitData.CLASS_KNIGHT, Unit.UnitClass.KNIGHT, 1, 0));
        units.add(new Unit("방패병1", playerTeam, UnitData.CLASS_SHIELD, Unit.UnitClass.SHIELD, 2, 0));
        units.add(new Unit("방패병2", playerTeam, UnitData.CLASS_SHIELD, Unit.UnitClass.SHIELD, 4, 0));
        units.add(new Unit("전차병", playerTeam, UnitData.CLASS_CHARIOT, Unit.UnitClass.CHARIOT, 5, 0));
        units.add(new Unit("성녀", playerTeam, UnitData.CLASS_SAINT, Unit.UnitClass.SAINT, 6, 0));

        // AI 진영 (ZEUS)
        int enemyRow = GameConfig.BOARD_HEIGHT - 1;
        int bossIdx = Math.min(stageLevel - 1, UnitData.STATS_ZEUS.length - 1);
        units.add(new Unit(UnitData.NAMES_ZEUS[bossIdx], aiTeam, UnitData.STATS_ZEUS[bossIdx], Unit.UnitClass.HERO, 3, enemyRow));
        units.add(new Unit("적 궁병", aiTeam, UnitData.CLASS_ARCHER, Unit.UnitClass.ARCHER, 0, enemyRow));
        units.add(new Unit("적 기병", aiTeam, UnitData.CLASS_KNIGHT, Unit.UnitClass.KNIGHT, 1, enemyRow));
        units.add(new Unit("적 방패병1", aiTeam, UnitData.CLASS_SHIELD, Unit.UnitClass.SHIELD, 2, enemyRow));
        units.add(new Unit("적 방패병2", aiTeam, UnitData.CLASS_SHIELD, Unit.UnitClass.SHIELD, 4, enemyRow));
        units.add(new Unit("적 전차병", aiTeam, UnitData.CLASS_CHARIOT, Unit.UnitClass.CHARIOT, 5, enemyRow));
        units.add(new Unit("적 성녀", aiTeam, UnitData.CLASS_SAINT, Unit.UnitClass.SAINT, 6, enemyRow));
    }

    @Override
    public void render(float delta) {
        update(delta);
        cleanupDeadUnits();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.batch.setProjectionMatrix(stage.getViewport().getCamera().combined);
        shape.setProjectionMatrix(stage.getViewport().getCamera().combined);

        // 1. 배경 출력
        game.batch.begin();
        game.batch.draw(battleBg, 0, 0, GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT);
        game.batch.end();

        // 2. 맵 타일 렌더링
        mapRenderer.drawTiles(hoveredGrid, selectedUnit, units);

        // 3. 사거리 하이라이트 표시
        if (!gameOver && selectedUnit != null) {
            mapRenderer.drawRangeOverlays(selectedUnit);
        }

        // 4. 유닛 렌더링 (그림자 -> 본체 순서)
        game.batch.begin();
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            if (u.isAlive()) unitRenderer.renderShadow(u, selectedUnit);
        }
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            if (u.isAlive()) unitRenderer.renderBody(u, selectedUnit);
        }

        // 하단 UI 렌더링
        gameUI.render(stageLevel, turnManager.getCurrentTurn(), playerTeam, menuHitbox, selectedUnit);
        game.batch.end();

        // 5. 게임 종료 시 결과 메뉴 출력
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

    // 마우스 입력을 통한 유닛 선택 및 이동을 처리합니다.
    private void handleInput() {
        if (gameOver) return;
        Vector2 touchPos = new Vector2(Gdx.input.getX(), Gdx.input.getY());
        stage.getViewport().unproject(touchPos);
        float mx = touchPos.x;
        float my = touchPos.y;

        // 전체화면 토글 버튼 클릭 확인
        if (Gdx.input.justTouched() && menuHitbox.contains(mx, my)) {
            game.playClick();
            toggleFullscreen();
            return;
        }

        if (!turnManager.getCurrentTurn().equals(playerTeam) || aiBusy) return;
        hoveredGrid = IsoUtils.screenToGrid(mx, my);

        if (Gdx.input.justTouched()) {
            if (selectedUnit != null) {
                int tx = (int) hoveredGrid.x;
                int ty = (int) hoveredGrid.y;
                // 선택된 유닛 이동 처리
                if (tx >= 0 && ty >= 0 && selectedUnit.team.equals(playerTeam) && BoardManager.canMoveTo(selectedUnit, tx, ty, units)) {
                    selectedUnit.setPosition(tx, ty);
                    processAutoAttack(playerTeam);
                    selectedUnit = null;
                    aiBusy = true;
                    turnManager.endTurn();
                    return;
                }
            }
            // 유닛 선택 처리
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

    // 이동 후 사거리 내 적이 있으면 자동으로 공격합니다.
    public void processAutoAttack(String team) {
        for (int i = 0; i < units.size; i++) {
            Unit attacker = units.get(i);
            if (attacker != null && attacker.isAlive() && attacker.team.equals(team)) {
                if (attacker.unitClass == Unit.UnitClass.KNIGHT) {
                    Array<Unit> targets = BoardManager.findAllTargetsInRange(attacker, units);
                    for (int j = 0; j < targets.size; j++) {
                        performAttack(attacker, targets.get(j));
                    }
                } else {
                    Unit target = BoardManager.findBestTargetInRange(attacker, units);
                    if (target != null) performAttack(attacker, target);
                }
            }
        }
        processAutoHeal(team);
    }

    // 성녀 클래스의 주변 아군 자동 치료 로직입니다.
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
                            gameUI.addLog(u.name + "가 " + ally.name + "를 치료함(+15)");
                        }
                    }
                }
            }
        }
    }

    // 공격과 반격 로직을 처리합니다.
    public void performAttack(Unit attacker, Unit target) {
        if (attacker == null || target == null || !target.isAlive() || !attacker.isAlive()) return;

        boolean isAttackerTurn = turnManager.isMyTurn(attacker.team);
        int damage = attacker.getPower(isAttackerTurn);
        target.currentHp -= damage;

        gameUI.addLog(attacker.name + " -> " + target.name + " [" + damage + " 데미지]");

        if (target.currentHp <= 0) {
            target.currentHp = 0;
            gameUI.addLog(target.name + " 처치됨!");
            handleDeath(target);
            return;
        }

        // 반격 가능한 거리라면 반격 수행
        if (target.canReach(attacker)) {
            int counterDamage = target.getPower(turnManager.isMyTurn(target.team));
            attacker.currentHp -= counterDamage;
            gameUI.addLog(target.name + "의 반격! [" + counterDamage + " 데미지]");

            if (attacker.currentHp <= 0) {
                attacker.currentHp = 0;
                gameUI.addLog(attacker.name + " 처치됨!");
                handleDeath(attacker);
            }
        }
    }

    // 유닛 사망 처리 및 승리/패배 조건을 체크합니다.
    private void handleDeath(Unit target) {
        target.status = Unit.DEAD;
        boolean isEnemyBoss = target.team.equals(aiTeam) && target.unitClass == Unit.UnitClass.HERO;
        boolean isPlayerHero = target.team.equals(playerTeam) && target.unitClass == Unit.UnitClass.HERO;

        if (isEnemyBoss) {
            gameOver = true;
            gameUI.addLog("승리! 적의 수장을 물리쳤습니다.");
            showGameOverMenu(true);
        } else if (isPlayerHero) {
            gameOver = true;
            gameUI.addLog("패배... 하데스의 영웅이 전사했습니다.");
            showGameOverMenu(false);
        }
    }

    // 게임 종료 시 결과 팝업 메뉴를 생성합니다.
    private void showGameOverMenu(boolean isVictory) {
        Table table = new Table();
        table.setFillParent(true);
        table.center();

        String resultText = isVictory ? "VICTORY!" : "DEFEAT...";
        Label titleLabel = new Label(resultText, new Label.LabelStyle(game.titleFont, isVictory ? Color.GOLD : Color.FIREBRICK));
        table.add(titleLabel).padBottom(60).row();

        // 승리 시 다음 스테이지 버튼
        if (isVictory && stageLevel < 7) {
            Label nextBtn = new Label("[ NEXT STAGE ]", new Label.LabelStyle(game.mainFont, Color.valueOf("4FB9AF")));
            nextBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    game.setScreen(new BattleScreen(game, playerTeam, heroName, heroStat, stageLevel + 1));
                }
            });
            UI.addHoverEffect(game, nextBtn, Color.valueOf("4FB9AF"), Color.WHITE);
            table.add(nextBtn).padBottom(20).row();
        }
        // 패배 시 재시도 버튼
        else if (!isVictory) {
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

        // 메뉴 화면으로 돌아가기 버튼
        Label homeBtn = new Label("[ BACK TO MENU ]", new Label.LabelStyle(game.mainFont, Color.LIGHT_GRAY));
        homeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MenuScreen(game));
            }
        });
        UI.addHoverEffect(game, homeBtn, Color.LIGHT_GRAY, Color.WHITE);
        table.add(homeBtn);

        stage.addActor(table);
        Gdx.input.setInputProcessor(stage);
    }

    // 사망한 유닛을 리스트에서 제거하고 선택 상태를 해제합니다.
    private void cleanupDeadUnits() {
        // 리스트 삭제 시 인덱스 꼬임을 방지하기 위해 역순으로 반복합니다.
        for (int i = units.size - 1; i >= 0; i--) {
            if (units.get(i).status == Unit.DEAD) {
                // 삭제되는 유닛이 현재 선택된 유닛이라면 선택 해제
                if (selectedUnit == units.get(i)) selectedUnit = null;
                units.removeIndex(i);
            }
        }
    }

    // 전체화면과 창 모드를 서로 전환합니다.
    private void toggleFullscreen() {
        if (Gdx.graphics.isFullscreen()) {
            Gdx.graphics.setWindowedMode((int) GameConfig.VIRTUAL_WIDTH, (int) GameConfig.VIRTUAL_HEIGHT);
        } else {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        }
    }

    // 게임 종료 시 화면을 어둡게 덮는 반투명 오버레이를 그립니다.
    private void drawGameOverOverlay() {
        Gdx.gl.glEnable(GL20.GL_BLEND); // 투명도 조절을 위해 블렌딩 활성화
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0, 0, 0, 0.7f); // 70% 농도의 검은색
        shape.rect(0, 0, GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT);
        shape.end();
    }

    // 창 크기가 변경될 때 뷰포트를 업데이트하여 비율을 유지합니다.
    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    // 메모리 누수 방지를 위해 사용된 리소스들을 해제합니다.
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
