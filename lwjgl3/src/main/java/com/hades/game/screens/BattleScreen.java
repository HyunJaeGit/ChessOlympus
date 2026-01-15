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

// 클래스 역할: 전투 로직을 제어하며, 승리/패배 시 재도전 및 메뉴 이동 UI와 전용 BGM 재생을 담당합니다.
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
    private GameUI gameUI; // UI 전담 클래스 추가

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
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        // BGM 전환: 메뉴 중지, 배틀 시작
        if (game.menuBgm != null && game.menuBgm.isPlaying()) game.menuBgm.stop();
        if (game.battleBgm != null && !game.battleBgm.isPlaying()) game.battleBgm.play();
    }

    @Override
    public void hide() {
        if (game.battleBgm != null) game.battleBgm.stop();
        Gdx.input.setInputProcessor(null);
    }

    private void loadResources() {
        // 배경과 타일만 유지, 상세 UI는 GameUI 내부에서 로드
        battleBg = new Texture(Gdx.files.internal("images/background/battle_background.png"));
        tileTop = new Texture(Gdx.files.internal("images/background/tile_top.png"));
    }

    private void init() {
        shape = new ShapeRenderer();
        mapRenderer = new MapRenderer(shape, game.batch, tileTop);
        unitRenderer = new UnitRenderer(game.batch, shape, game.unitFont, playerTeam);
        gameUI = new GameUI(game); // UI 인스턴스 초기화

        units = new Array<>();
        turnManager = new TurnManager();
        setupBattleUnits();
    }

    private void setupBattleUnits() {
        units.clear();
        // 플레이어 영웅 및 부대 배치
        units.add(new Unit(heroName, playerTeam, heroStat, 3, 0));
        for (int x = 0; x < GameConfig.BOARD_WIDTH; x++) {
            if (x == 3) continue;
            units.add(new Unit("케로", playerTeam, UnitData.HADES_SOLDIER, x, 0));
        }

        // 적 부대 배치 (스테이지 레벨에 따른 보스 설정)
        int enemyRow = GameConfig.BOARD_HEIGHT - 1;
        int bossIdx = Math.min(stageLevel - 1, UnitData.STATS_ZEUS.length - 1);
        units.add(new Unit(UnitData.NAMES_ZEUS[bossIdx], aiTeam, UnitData.STATS_ZEUS[bossIdx], 3, enemyRow));
        for (int x = 0; x < GameConfig.BOARD_WIDTH; x++) {
            if (x == 3) continue;
            units.add(new Unit("병사", aiTeam, UnitData.ZEUS_SOLDIER, x, enemyRow));
        }
    }

    @Override
    public void render(float delta) {
        update(delta);
        cleanupDeadUnits();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.batch.setProjectionMatrix(stage.getViewport().getCamera().combined);
        shape.setProjectionMatrix(stage.getViewport().getCamera().combined);

        // 1. 배경 렌더링
        game.batch.begin();
        game.batch.draw(battleBg, 0, 0, GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT);
        game.batch.end();

        // 2. 맵 타일 렌더링
        mapRenderer.drawTiles(hoveredGrid);
        if (!gameOver && selectedUnit != null) {
            mapRenderer.drawRangeOverlays(selectedUnit, units);
        }

        // 3. 유닛 렌더링 (그림자 -> 바디 순서)
        game.batch.begin();
        for (Unit u : units) { if (u.isAlive()) unitRenderer.renderShadow(u, selectedUnit); }
        for (Unit u : units) { if (u.isAlive()) unitRenderer.renderBody(u, selectedUnit); }

        // 4. UI 전담 렌더링 (GameUI로 위임)
        gameUI.render(stageLevel, turnManager.getCurrentTurn(), playerTeam, menuHitbox, selectedUnit);
        game.batch.end();

        // 5. 게임오버 오버레이 및 Stage(버튼 등)
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
            // AI 턴 처리
            aiDelay += delta;
            if (aiDelay >= 1.0f) {
                if (aiBusy) {
                    try { AILogic.processAITurn(units, aiTeam, turnManager, this); }
                    catch (Exception e) { turnManager.endTurn(); }
                    aiBusy = false;
                    aiDelay = 0;
                    selectedUnit = null;
                } else { aiBusy = true; }
            }
        }
    }

    private void handleInput() {
        if (gameOver) return;

        Vector2 touchPos = new Vector2(Gdx.input.getX(), Gdx.input.getY());
        stage.getViewport().unproject(touchPos);
        float mx = touchPos.x;
        float my = touchPos.y;

        // 전체화면 토글 버튼 처리
        if (Gdx.input.justTouched() && menuHitbox.contains(mx, my)) {
            game.playClick(1.0f);
            toggleFullscreen();
            return;
        }

        if (!turnManager.getCurrentTurn().equals(playerTeam) || aiBusy) return;

        hoveredGrid = IsoUtils.screenToGrid(mx, my);

        if (Gdx.input.justTouched()) {
            // 유닛 이동 처리
            if (selectedUnit != null) {
                int tx = (int) hoveredGrid.x;
                int ty = (int) hoveredGrid.y;
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
                    break;
                }
            }
            selectedUnit = clickedUnit;
        }
    }

    private void toggleFullscreen() {
        if (Gdx.graphics.isFullscreen()) {
            Gdx.graphics.setWindowedMode((int) GameConfig.VIRTUAL_WIDTH, (int) GameConfig.VIRTUAL_HEIGHT);
        } else {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        }
    }

    private void drawGameOverOverlay() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0, 0, 0, 0.7f);
        shape.rect(0, 0, GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT);
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    public void processAutoAttack(String team) {
        // 향상된 for문(for : units) 대신 인덱스 for문을 사용하여 반복자 충돌 방지
        for (int i = 0; i < units.size; i++) {
            Unit attacker = units.get(i);

            // 유닛이 살아있고 해당 팀일 때만 공격 시도
            if (attacker != null && attacker.isAlive() && attacker.team.equals(team)) {
                // 내부에서 다시 units를 검색해도 이제 안전합니다.
                Unit target = BoardManager.findBestTargetInRange(attacker, units);
                if (target != null) {
                    performAttack(attacker, target);
                }
            }
        }
    }

    public void performAttack(Unit attacker, Unit target) {
        if (attacker == null || target == null || !target.isAlive() || !attacker.isAlive()) return;

        // 1. 공격 주도자의 타격 (항상 현재 턴인 유닛이 호출함)
        boolean isAttackerTurn = turnManager.isMyTurn(attacker.team);
        int atkDmg = attacker.getPower(isAttackerTurn);
        target.currentHp -= atkDmg;

        // TODO: 전투 로그 추가 (예: gameUI.addLog(attacker.name + "의 공격! " + atkDmg + " 데미지"))

        // 2. 타겟 사망 확인
        if (target.currentHp <= 0) {
            target.currentHp = 0;
            handleDeath(target);
            return; // 사망 시 반격 불가
        }

        // 3. 수비자의 반격 (조건: 타겟이 생존해 있고 공격자가 내 사거리 안인가?)
        if (target.canReach(attacker)) {
            // 반격자는 현재 턴의 주인이 아니므로 false가 전달되어 counterAtk가 적용됨
            int counterDmg = target.getPower(turnManager.isMyTurn(target.team));
            attacker.currentHp -= counterDmg;

            // TODO: 전투 로그 추가 (예: gameUI.addLog(target.name + "의 반격! " + counterDmg + " 데미지"))

            if (attacker.currentHp <= 0) {
                attacker.currentHp = 0;
                handleDeath(attacker);
            }
        }
    }

    private void handleDeath(Unit target) {
        target.status = Unit.DEAD;

        // 보스 사망 시 승리, 플레이어 영웅 사망 시 패배
        boolean isEnemyBoss = target.team.equals(aiTeam) && !target.stat.skillName().equals("일반 병사");
        boolean isPlayerHero = target.team.equals(playerTeam) && target.name.equals(heroName);

        if (isEnemyBoss) {
            gameOver = true;
            showGameOverMenu(true);
        } else if (isPlayerHero) {
            gameOver = true;
            showGameOverMenu(false);
        }
    }

    private void showGameOverMenu(boolean isVictory) {
        Table table = new Table();
        table.setFillParent(true);
        table.center();

        String resultText = isVictory ? "VICTORY!" : "DEFEAT...";
        Label titleLabel = new Label(resultText, new Label.LabelStyle(game.titleFont, isVictory ? Color.GOLD : Color.FIREBRICK));
        table.add(titleLabel).padBottom(60).row();

        if (isVictory && stageLevel < 7) {
            Label nextBtn = new Label("[ NEXT STAGE ]", new Label.LabelStyle(game.mainFont, Color.valueOf("4FB9AF")));
            nextBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    game.playClick(1.0f);
                    game.setScreen(new BattleScreen(game, playerTeam, heroName, heroStat, stageLevel + 1));
                }
            });
            table.add(nextBtn).padBottom(20).row();
        } else if (!isVictory) {
            Label retryBtn = new Label("[ RE-TRY ]", new Label.LabelStyle(game.mainFont, Color.WHITE));
            retryBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    game.playClick(1.0f);
                    game.setScreen(new BattleScreen(game, playerTeam, heroName, heroStat, stageLevel));
                }
            });
            table.add(retryBtn).padBottom(20).row();
        }

        Label homeBtn = new Label("[ BACK TO MENU ]", new Label.LabelStyle(game.mainFont, Color.LIGHT_GRAY));
        homeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.playClick(1.0f);
                game.setScreen(new MenuScreen(game));
            }
        });
        table.add(homeBtn);
        stage.addActor(table);
    }

    private void cleanupDeadUnits() {
        for (int i = units.size - 1; i >= 0; i--) {
            if (units.get(i).status == Unit.DEAD) {
                if (selectedUnit == units.get(i)) selectedUnit = null;
                units.removeIndex(i);
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

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
