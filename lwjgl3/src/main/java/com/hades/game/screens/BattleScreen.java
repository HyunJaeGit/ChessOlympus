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

// 클래스 역할: 실제 전투가 이루어지는 화면으로 유닛 배치, 전투 로직, 승패 판정을 담당합니다.
public class BattleScreen extends ScreenAdapter {
    private final HadesGame game;
    private ShapeRenderer shape; // 도형(타일, 하이라이트 등)을 그리기 위한 도구
    private Stage stage; // UI 요소(버튼, 레이블)를 관리하는 무대
    private Array<Unit> units; // 전장에 존재하는 모든 유닛 리스트
    private Vector2 hoveredGrid = new Vector2(-1, -1); // 마우스가 올라가 있는 그리드 좌표
    private Unit selectedUnit = null; // 현재 플레이어가 선택한 유닛
    private TurnManager turnManager; // 누구의 차례인지 관리하는 객체
    private MapRenderer mapRenderer; // 타일 맵을 화면에 그리는 역할
    private UnitRenderer unitRenderer; // 유닛 이미지를 화면에 그리는 역할
    private GameUI gameUI; // 화면 상단/하단의 정보 UI 표시

    private Texture battleBg; // 전투 배경 이미지
    private Texture tileTop; // 타일 윗면 이미지

    private final String playerTeam; // 플레이어 팀 이름 (HADES)
    private final String aiTeam; // 적 팀 이름 (ZEUS)
    private final String heroName; // 선택한 영웅 이름
    private final UnitData.Stat heroStat; // 영웅의 능력치
    private int stageLevel; // 현재 스테이지 번호

    private float aiDelay = 0; // AI 행동 사이의 시간 지연용 변수
    private boolean aiBusy = false; // AI가 계산/행동 중인지 여부
    private boolean gameOver = false; // 게임 종료 상태 여부

    private final float MENU_W = 180; // 화면 모드 버튼 너비
    private final float MENU_H = 60; // 화면 모드 버튼 높이
    private Rectangle menuHitbox; // 화면 모드 버튼의 클릭 영역

    public BattleScreen(HadesGame game, String playerTeam, String heroName, UnitData.Stat heroStat, int stageLevel) {
        this.game = game;
        this.playerTeam = playerTeam;
        this.heroName = heroName;
        this.heroStat = heroStat;
        this.stageLevel = stageLevel;
        this.aiTeam = playerTeam.equals("HADES") ? "ZEUS" : "HADES";

        // 화면 크기에 맞게 가상 뷰포트 설정
        this.stage = new Stage(new FitViewport(GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT));

        // 화면 우측 상단에 버튼 클릭 영역 설정
        this.menuHitbox = new Rectangle(
            GameConfig.VIRTUAL_WIDTH - MENU_W - 20,
            GameConfig.VIRTUAL_HEIGHT - MENU_H - 20,
            MENU_W,
            MENU_H
        );

        loadResources();
        init();
    }

    // 화면이 처음 나타날 때 실행되는 메서드 (입력 처리 및 음악 설정)
    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage); // 입력을 stage가 받도록 설정

        // 메뉴 음악은 멈추고 전투 음악을 전역 볼륨으로 재생
        if (game.menuBgm != null && game.menuBgm.isPlaying()) game.menuBgm.stop();
        if (game.battleBgm != null && !game.battleBgm.isPlaying()) {
            game.battleBgm.setVolume(game.globalVolume);
            game.battleBgm.play();
        }
    }

    // 이미지 파일 로드
    private void loadResources() {
        battleBg = new Texture(Gdx.files.internal("images/background/battle_background.png"));
        tileTop = new Texture(Gdx.files.internal("images/background/tile_top.png"));
    }

    // 각종 렌더러 및 게임 로직 객체 초기화
    private void init() {
        shape = new ShapeRenderer();
        mapRenderer = new MapRenderer(shape, game.batch, tileTop);
        unitRenderer = new UnitRenderer(game.batch, shape, game.unitFont, playerTeam);
        gameUI = new GameUI(game);

        units = new Array<>();
        turnManager = new TurnManager();
        setupBattleUnits();
    }

    // 유닛들을 초기 위치에 배치 (7인 체제)
    private void setupBattleUnits() {
        units.clear();

        // 플레이어(HADES) 진영 유닛 생성 및 추가
        units.add(new Unit(heroName, playerTeam, heroStat, Unit.UnitClass.HERO, 3, 0));
        units.add(new Unit("궁병", playerTeam, UnitData.CLASS_ARCHER, Unit.UnitClass.ARCHER, 0, 0));
        units.add(new Unit("기병", playerTeam, UnitData.CLASS_KNIGHT, Unit.UnitClass.KNIGHT, 1, 0));
        units.add(new Unit("방패병1", playerTeam, UnitData.CLASS_SHIELD, Unit.UnitClass.SHIELD, 2, 0));
        units.add(new Unit("방패병2", playerTeam, UnitData.CLASS_SHIELD, Unit.UnitClass.SHIELD, 4, 0));
        units.add(new Unit("전차병", playerTeam, UnitData.CLASS_CHARIOT, Unit.UnitClass.CHARIOT, 5, 0));
        units.add(new Unit("성녀", playerTeam, UnitData.CLASS_SAINT, Unit.UnitClass.SAINT, 6, 0));

        // 적(ZEUS) 진영 유닛 생성 (스테이지 레벨에 따라 보스 변경)
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

    // 매 프레임마다 화면을 그리는 메인 렌더링 메서드
    @Override
    public void render(float delta) {
        update(delta); // 데이터 갱신
        cleanupDeadUnits(); // 죽은 유닛 제거

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 1. 배경 그리기
        // SpriteBatch: 이미지(텍스처)를 모아서 화면에 한꺼번에 그리는 메서드
        // ShapeRenderer: 선, 사각형, 원 등 수학적 계산이 필요한 '도형'을 직접 그리는 메서드
        // setProjectionMatrix: 그리기 도구에게 현재 카메라가 보고 있는 '좌표계'를 전달하는 메서드
        game.batch.setProjectionMatrix(stage.getViewport().getCamera().combined);
        shape.setProjectionMatrix(stage.getViewport().getCamera().combined);
        game.batch.begin();
        game.batch.draw(battleBg, 0, 0, GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT);
        game.batch.end();

        // 2. 맵 타일 및 이동 범위 그리기
        mapRenderer.drawTiles(hoveredGrid, selectedUnit, units);

        // 3. 유닛 공격 사거리 가이드 표시
        if (!gameOver && selectedUnit != null) {
            mapRenderer.drawRangeOverlays(selectedUnit);
        }

        // 4. 유닛 및 UI 그리기
        game.batch.begin();
        // 그림자를 먼저 모두 그리고 몸체를 그려서 겹침 문제 방지
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            if (u.isAlive()) unitRenderer.renderShadow(u, selectedUnit);
        }
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            if (u.isAlive()) unitRenderer.renderBody(u, selectedUnit);
        }
        // 상단 UI 정보 렌더링
        gameUI.render(stageLevel, turnManager.getCurrentTurn(), playerTeam, menuHitbox, selectedUnit);
        game.batch.end();

        // 5. 게임 종료 시 오버레이 및 메뉴 표시
        if (gameOver) {
            drawGameOverOverlay();
            stage.act();
            stage.draw();
        }
    }

    // 게임 논리 상태 갱신 (입력 처리 및 AI 판단)
    private void update(float delta) {
        if (gameOver) return;

        // 플레이어 차례인 경우
        if (turnManager.getCurrentTurn().equals(playerTeam)) {
            aiBusy = false;
            aiDelay = 0;
            handleInput();
        }
        // AI 차례인 경우
        else {
            aiDelay += delta;
            if (aiDelay >= 1.0f) { // 1초 대기 후 행동
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

    // 사용자 마우스/터치 입력 처리
    private void handleInput() {
        if (gameOver) return;

        Vector2 touchPos = new Vector2(Gdx.input.getX(), Gdx.input.getY());
        stage.getViewport().unproject(touchPos); // 화면 좌표를 게임 가상 좌표로 변환
        float mx = touchPos.x;
        float my = touchPos.y;

        // 화면 모드 전환 버튼 클릭 체크
        if (Gdx.input.justTouched() && menuHitbox.contains(mx, my)) {
            game.playClick(1.0f);
            toggleFullscreen();
            return;
        }

        if (!turnManager.getCurrentTurn().equals(playerTeam) || aiBusy) return;

        // 마우스 위치의 그리드 좌표 계산
        hoveredGrid = IsoUtils.screenToGrid(mx, my);

        if (Gdx.input.justTouched()) {
            // 유닛 이동 처리
            if (selectedUnit != null) {
                int tx = (int) hoveredGrid.x;
                int ty = (int) hoveredGrid.y;
                if (tx >= 0 && ty >= 0 && selectedUnit.team.equals(playerTeam) && BoardManager.canMoveTo(selectedUnit, tx, ty, units)) {
                    selectedUnit.setPosition(tx, ty); // 위치 변경
                    processAutoAttack(playerTeam); // 이동 후 자동 공격 실행
                    selectedUnit = null;
                    aiBusy = true;
                    turnManager.endTurn(); // 턴 종료
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

    // 해당 팀의 모든 유닛이 사거리 내 적을 자동 공격
    public void processAutoAttack(String team) {
        for (int i = 0; i < units.size; i++) {
            Unit attacker = units.get(i);
            if (attacker != null && attacker.isAlive() && attacker.team.equals(team)) {
                // 기병은 광역 공격
                if (attacker.unitClass == Unit.UnitClass.KNIGHT) {
                    Array<Unit> targets = BoardManager.findAllTargetsInRange(attacker, units);
                    for (int j = 0; j < targets.size; j++) {
                        performAttack(attacker, targets.get(j));
                    }
                }
                // 그 외 병과는 단일 타겟 공격
                else {
                    Unit target = BoardManager.findBestTargetInRange(attacker, units);
                    if (target != null) performAttack(attacker, target);
                }
            }
        }
        processAutoHeal(team); // 공격 후 치료 단계
    }

    // 성녀 유닛의 주변 아군 자동 치료
    private void processAutoHeal(String team) {
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            if (u.isAlive() && u.team.equals(team) && u.unitClass == Unit.UnitClass.SAINT) {
                for (int j = 0; j < units.size; j++) {
                    Unit ally = units.get(j);
                    if (ally.isAlive() && ally.team.equals(team) && ally != u) {
                        int dist = Math.abs(u.gridX - ally.gridX) + Math.abs(u.gridY - ally.gridY);
                        // 인접한(거리 1) 체력이 깎인 아군 치료
                        if (dist == 1 && ally.currentHp < ally.stat.hp()) {
                            ally.currentHp = Math.min(ally.stat.hp(), ally.currentHp + 15);
                        }
                    }
                }
            }
        }
    }

    // 실제 데미지 계산 및 반격 로직
    public void performAttack(Unit attacker, Unit target) {
        if (attacker == null || target == null || !target.isAlive() || !attacker.isAlive()) return;

        boolean isAttackerTurn = turnManager.isMyTurn(attacker.team);
        target.currentHp -= attacker.getPower(isAttackerTurn); // 공격

        // 수비자가 죽지 않았다면 반격 체크
        if (target.currentHp <= 0) {
            target.currentHp = 0;
            handleDeath(target);
            return;
        }

        if (target.canReach(attacker)) {
            attacker.currentHp -= target.getPower(turnManager.isMyTurn(target.team)); // 반격
            if (attacker.currentHp <= 0) {
                attacker.currentHp = 0;
                handleDeath(attacker);
            }
        }
    }

    // 유닛 사망 시 승리/패배 조건 확인
    private void handleDeath(Unit target) {
        target.status = Unit.DEAD;
        boolean isEnemyBoss = target.team.equals(aiTeam) && target.unitClass == Unit.UnitClass.HERO;
        boolean isPlayerHero = target.team.equals(playerTeam) && target.unitClass == Unit.UnitClass.HERO;

        if (isEnemyBoss) {
            gameOver = true;
            showGameOverMenu(true); // 승리
        } else if (isPlayerHero) {
            gameOver = true;
            showGameOverMenu(false); // 패배
        }
    }

    // 결과 화면(VICTORY/DEFEAT) UI 생성
    private void showGameOverMenu(boolean isVictory) {
        Table table = new Table();
        table.setFillParent(true);
        table.center();

        String resultText = isVictory ? "VICTORY!" : "DEFEAT...";
        Label titleLabel = new Label(resultText, new Label.LabelStyle(game.titleFont, isVictory ? Color.GOLD : Color.FIREBRICK));
        table.add(titleLabel).padBottom(60).row();

        // 승리 시 다음 스테이지 버튼 (7스테이지 미만일 때만)
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
        }
        // 패배 시 재시도 버튼
        else if (!isVictory) {
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

        // 메인 메뉴로 돌아가기 버튼
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

    // 죽은 유닛을 units 리스트에서 실제로 제거
    private void cleanupDeadUnits() {
        for (int i = units.size - 1; i >= 0; i--) {
            if (units.get(i).status == Unit.DEAD) {
                if (selectedUnit == units.get(i)) selectedUnit = null;
                units.removeIndex(i);
            }
        }
    }

    // 전체화면과 창모드 전환
    private void toggleFullscreen() {
        if (Gdx.graphics.isFullscreen()) {
            Gdx.graphics.setWindowedMode((int) GameConfig.VIRTUAL_WIDTH, (int) GameConfig.VIRTUAL_HEIGHT);
        } else {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        }
    }

    // 게임 종료 시 화면을 어둡게 만드는 반투명 검은 사각형 그리기
    private void drawGameOverOverlay() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0, 0, 0, 0.7f);
        shape.rect(0, 0, GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT);
        shape.end();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    // 메모리 해제
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
