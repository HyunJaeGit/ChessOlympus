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
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.hades.game.HadesGame;
import com.hades.game.constants.GameConfig;
import com.hades.game.constants.SkillData;
import com.hades.game.constants.UnitData;
import com.hades.game.entities.Unit;
import com.hades.game.logic.*;
import com.hades.game.view.GameUI;
import com.hades.game.view.MapRenderer;
import com.hades.game.view.UnitRenderer;
import com.hades.game.view.UI;

// Chess Olympus: HADES vs ZEUS - 메인 전투 화면
// 유닛의 이동, 공격, 턴 관리 및 승패 판정을 처리합니다.
public class BattleScreen extends ScreenAdapter {
    private final HadesGame game;
    private ShapeRenderer shape;
    private final Stage stage;
    private CameraManager cameraManager;
    private Array<Unit> units;
    private Vector2 hoveredGrid = new Vector2(-1, -1);
    private Unit selectedUnit = null;
    private TurnManager turnManager;
    private CombatManager combatManager;
    private MapRenderer mapRenderer;
    private UnitRenderer unitRenderer;
    private GameUI gameUI;
    private boolean showHelp = false; // 도움말 창 활성화 상태를 저장하는 변수

    private Texture battleBg;
    private Texture tileTop;

    private final String playerTeam;
    private final String aiTeam;
    private final String heroName;
    private final UnitData.Stat heroStat;
    private final int stageLevel;

    private float aiDelay = 0;
    private boolean aiBusy = false;
    private boolean gameOver = false;

    private final float MENU_W = 180;
    private final float MENU_H = 60;
    private final Rectangle menuHitbox;

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
        gameUI.addLog("STAGE " + stageLevel + " 전투 시작!", "SYSTEM", playerTeam);
    }

    // [show] 화면이 나타날 때 실행되는 메서드
    @Override
    public void show() {
        // 스테이지의 입력 프로세서를 설정합니다.
        Gdx.input.setInputProcessor(stage);

        // [추가] 휠 스크롤 입력을 CameraManager와 연결
        stage.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public boolean scrolled(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, float amountX, float amountY) {
                cameraManager.handleScroll(amountY);
                return true;
            }
        });

        // 1. CutsceneManager에서 현재 스테이지 레벨에 해당하는 데이터
        com.hades.game.screens.cutscene.CutsceneData data =
            com.hades.game.screens.cutscene.CutsceneManager.getStageData(stageLevel);

        if (data != null && data.bgmPath() != null) {
            // 2. 현재 로드된 음악과 스테이지 데이터의 음악 경로가 다른지 확인
            if (game.battleBgm == null || !Gdx.files.internal(data.bgmPath()).path().equals(data.bgmPath())) {

                // 기존에 사용하던 음악 객체가 있다면 메모리에서 해제(dispose)합니다. (메모리 누수 방지)
                if (game.battleBgm != null) {
                    game.battleBgm.dispose();
                }

                // 새로운 경로의 음악 파일을 생성하여 game.battleBgm 바구니에 담습니다.
                game.battleBgm = Gdx.audio.newMusic(Gdx.files.internal(data.bgmPath()));
            }

            // 다른 음악을 모두 정지(stop)시키고
            // 새로운 배틀 음악만 깔끔하게 재생하여 소리 겹침 현상을 방지합니다.
            game.playMusic(game.battleBgm);
        }
    }

    private void loadResources() {
        battleBg = new Texture(Gdx.files.internal("images/background/battle_background.png"));
        tileTop = new Texture(Gdx.files.internal("images/background/tile_top.png"));
    }

    private void init() {
        shape = new ShapeRenderer();
        mapRenderer = new MapRenderer(shape, game.batch, tileTop);
        unitRenderer = new UnitRenderer(game.batch, shape, game.battleFont, playerTeam);
        gameUI = new GameUI(game);
        // stage의 카메라를 OrthographicCamera로 캐스팅하여 매니저에 넘겨줍니다.
        cameraManager    = new CameraManager((OrthographicCamera) stage.getCamera());

        if (heroStat != null) {
            heroStat.resetSkillStatus();
            heroStat.clearReservedSkill();
        }

        turnManager = new TurnManager();
        // TurnManager가 BattleScreen을 감시
        turnManager.setBattleScreen(this);
        combatManager = new CombatManager(gameUI, turnManager, playerTeam, this::handleDeath);
        units = StageGenerator.create(stageLevel, playerTeam, heroName, heroStat);
    }

    @Override
    public void render(float delta) {
        // 카메라 상태(줌 보간 등) 업데이트
        cameraManager.update();

        // 우클릭 드래그시 화면이동 처리
        if (Gdx.input.isButtonPressed(com.badlogic.gdx.Input.Buttons.RIGHT)) {
            cameraManager.handlePan(Gdx.input.getDeltaX(), Gdx.input.getDeltaY());
        } else {
            // [추가] 우클릭을 떼면 복귀 로직 활성화
            cameraManager.stopPanning();
        }

        for (Unit u : units) u.update(delta);

        // [수정] 업데이트 로직을 먼저 수행하여 사망 판정(handleDeath)이 cleanup보다 먼저 일어나게 함
        update(delta);

        // [수정] 게임 오버가 아닐 때만 유닛 삭제를 진행하여, 영웅 사망 판정 시 리스트에 유닛이 남아있도록 보장함
        if (!gameOver) {
            cleanupDeadUnits();
        }

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

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

        for (Unit u : units) if (u.isAlive()) unitRenderer.renderShadow(u, selectedUnit);   // 1. 모든 유닛의 그림자 먼저 렌더링
        for (Unit u : units) if (u.isAlive()) unitRenderer.renderBody(u, selectedUnit);     // 2. 모든 유닛의 본체와 체력바 렌더링
        for (Unit u : units) {
            if (u.isAlive()) unitRenderer.renderSpeechBubble(u);    // 3. 말풍선을 가장 마지막에 렌더링 (유닛 본체에 가려지지 않도록 분리)
        }
        // 4. GameUI.java -> UI 렌더링
        gameUI.render(stageLevel, turnManager.getCurrentTurn(), playerTeam, menuHitbox, selectedUnit, mx, my, showHelp);
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
                    try {
                        AILogic.processAITurn(units, aiTeam, turnManager, this);
                    } catch (Exception e) {
                        turnManager.endTurn();
                    }
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
        // 디버그 툴 처리
        com.hades.game.utils.DebugManager.handleBattleDebug(game, units, aiTeam, this::handleDeath);

        // 게임 오버 상태면 입력 무시
        if (gameOver) return;

        // 마우스 좌표를 게임 월드 좌표로 변환 (Unproject)
        Vector2 touchPos = new Vector2(Gdx.input.getX(), Gdx.input.getY());
        stage.getViewport().unproject(touchPos);
        float mx = touchPos.x;
        float my = touchPos.y;

        // 클릭 발생 시 처리
        if (Gdx.input.justTouched()) {

            // 1. [우선순위 1] HELP 버튼 클릭 체크
            if (gameUI.isHelpClicked(mx, my)) {
                game.playClick();
                showHelp = !showHelp; // 도움말 창 상태 반전 (토글)
                return; // UI를 클릭했으므로 아래 로직 무시
            }

            // 2. [우선순위 2] 도움말 창이 켜져 있을 때 화면 어디든 누르면 닫기
            if (showHelp) {
                showHelp = false;
                return;
            }

            // 3. [우선순위 3] 화면 모드 버튼 (WINDOW/FULLSCREEN) 클릭 체크
            if (menuHitbox.contains(mx, my)) {
                game.playClick();
                toggleFullscreen();
                return;
            }

            // 4. [우선순위 4] 권능(스킬) UI 클릭 체크 (아군 영웅이 선택되어 있을 때)
            if (selectedUnit != null && selectedUnit.team.equals(playerTeam) && selectedUnit.unitClass == Unit.UnitClass.HERO) {
                String clickedSkill = gameUI.getClickedSkill(mx, my, selectedUnit);
                if (clickedSkill != null) {
                    String currentReserved = selectedUnit.stat.getReservedSkill();

                    if (clickedSkill.equals(currentReserved)) {
                        // 이미 장전된 스킬을 다시 누르면 취소
                        selectedUnit.stat.clearReservedSkill();
                        gameUI.addLog(clickedSkill + " 장전 취소", "SYSTEM", playerTeam);
                    } else {
                        // 새로운 스킬 장전
                        selectedUnit.stat.setReservedSkill(clickedSkill);
                        gameUI.addLog(clickedSkill + " 장전됨! 이동 시 발동.", selectedUnit.team, playerTeam);
                    }
                    game.playClick(1.1f);
                    return;
                }
            }
        }

        // 아군 턴이 아니거나 AI가 행동 중이면 조작 불가
        if (!turnManager.getCurrentTurn().equals(playerTeam) || aiBusy) return;

        // 마우스가 위치한 그리드 좌표 계산 (호버링 효과용)
        hoveredGrid = IsoUtils.screenToGrid(mx, my);

        // 실제 필드 내 유닛 클릭 및 이동 처리
        if (Gdx.input.justTouched()) {

            // 유닛 이동 처리: 유닛이 선택되어 있고, 클릭한 타일이 이동 가능할 때
            if (selectedUnit != null) {
                int tx = (int) hoveredGrid.x;
                int ty = (int) hoveredGrid.y;
                if (tx >= 0 && ty >= 0 && selectedUnit.team.equals(playerTeam) && BoardManager.canMoveTo(selectedUnit, tx, ty, units)) {
                    selectedUnit.setPosition(tx, ty); // 유닛 이동
                    processMoveEnd(selectedUnit);    // 이동 후 교전 처리 등
                    selectedUnit = null;             // 선택 해제
                    aiBusy = true;                   // AI 턴으로 넘어가기 전 대기 상태
                    turnManager.endTurn();           // 턴 종료
                    return;
                }
            }

            // 유닛 선택 처리: 필드 위의 유닛을 클릭했을 때
            Unit clickedUnit = null;
            for (Unit u : units) {
                if (u.isAlive() && unitRenderer.isMouseInsideHitbox(u, mx, my)) {
                    clickedUnit = u;
                    game.playClick();
                    break;
                }
            }
            selectedUnit = clickedUnit; // 클릭한 유닛을 선택 유닛으로 지정 (없으면 null)
        }
    }

    public void processMoveEnd(Unit unit) {
        String reserved = unit.stat.getReservedSkill();
        if (reserved != null && !reserved.equals("기본 공격")) {
            executeHeroSkill(unit, reserved);
        }
        combatManager.processAutoAttack(units, unit.team);
    }
    // 영웅 유닛의 특수 권능(스킬)을 실행합니다.
    private void executeHeroSkill(Unit hero, String skillName) {
        // 기술 발동 시 유닛 머리 위에 기술명을 외치는 말풍선 생성
        hero.say(skillName + "!!");

        // 직접 루프를 돌지 않고 통합 매니저인 SkillManager에 모든 로직을 위임합니다.
        // 이 메서드 하나로 사거리 판정, 공격/치유 분기, 하데스 스킬 횟수 차감이 모두 처리됩니다.
        SkillManager.executeSkill(hero, skillName, units, gameUI, playerTeam);

        // 기술 사용 후 예약된 스킬 상태 초기화 (SkillManager 내부에서도 처리하지만 이중 안전장치로 유지)
        hero.stat.clearReservedSkill();
    }

    // [handleDeath] 유닛이 사망할 때마다 호출되어 승패를 판정하는 핵심 로직입니다.
    private void handleDeath(Unit target) {
        // [추가] 이미 게임 오버라면 중복 처리를 방지
        if (gameOver) return;

        target.status = Unit.DEAD; // 유닛 상태를 사망으로 변경

        // 사망한 유닛이 적군 영웅(보스)인지, 플레이어 영웅인지 체크
        boolean isEnemyBoss = target.team.equals(aiTeam) && target.unitClass == Unit.UnitClass.HERO;
        boolean isPlayerHero = target.team.equals(playerTeam) && target.unitClass == Unit.UnitClass.HERO;

        // 1. 승리 또는 패배 조건이 충족된 경우
        if (isEnemyBoss || isPlayerHero) {
            // [핵심] 모든 배경 음악을 정지시켜 중복 재생을 방지하고 정적을 만듭니다.
            game.playMusic(null);
            gameOver = true; // 게임 오버 플래그 활성화 (render의 업데이트 중지)

            // [수정] 즉시 결과 창을 띄움
            if (isEnemyBoss) {
                // 마지막 7스테이지라면 엔딩 컷신으로 연결
                if (stageLevel == 7) {
                    game.setScreen(new com.hades.game.screens.cutscene.BaseCutsceneScreen(
                        game, com.hades.game.screens.cutscene.CutsceneManager.getStageData(8), new EndingScreen(game)
                    ));
                } else {
                    gameUI.addLog("승리! 적의 수장을 물리쳤습니다.", "SYSTEM", playerTeam);
                    showGameOverMenu(true); // 승리 메뉴 표시
                }
            } else {
                gameUI.addLog("패배... 하데스의 영웅이 전사했습니다.", "SYSTEM", playerTeam);
                showGameOverMenu(false); // 패배 메뉴 표시
            }
        }
    }

    // [showGameOverMenu] 전투 결과에 따라 화면 중앙에 UI를 띄워주는 메서드입니다.
    private void showGameOverMenu(boolean isVictory) {
        Table table = new Table();
        table.setFillParent(true); // 테이블을 화면 전체 크기로 설정
        table.center(); // 중앙 정렬

        // 결과 타이틀 (승리: 골드색, 패배: 붉은색)
        Label titleLabel = new Label(isVictory ? "VICTORY!" : "DEFEAT...",
            new Label.LabelStyle(game.titleFont, isVictory ? Color.GOLD : Color.FIREBRICK));
        table.add(titleLabel).padBottom(50).row();

        // 승리했을 경우 보상 및 다음 단계 처리
        if (isVictory) {
            // 랜덤 보상 계산 및 저장 (영혼 파편 1~3개)
            int rewardSouls = (int)(Math.random() * 3) + 1;
            game.runState.soulFragments += rewardSouls;
            game.runState.olympusSeals += 1;

            // 현재 클리어한 스테이지가 최고 기록보다 높으면 진행도 갱신
            if (game.runState.currentStageLevel <= stageLevel) {
                game.runState.currentStageLevel = stageLevel + 1;
            }
            game.saveGame(); // 변경된 상태 세이브 파일에 기록

            Label rewardLabel = new Label("보상: 영혼 파편 +" + rewardSouls + ", 인장 +1", new Label.LabelStyle(game.mainFont, Color.CYAN));
            table.add(rewardLabel).padBottom(30).row();

            // [버튼] 강화 화면(업그레이드)으로 이동
            Label upgradeBtn = new Label("[ 명계의 제단으로 ]", new Label.LabelStyle(game.mainFont, Color.valueOf("4FB9AF")));
            upgradeBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    game.playClick();
                    game.setScreen(new UpgradeScreen(game, heroName, game.runState.heroStat, stageLevel));
                }
            });
            UI.addHoverEffect(game, upgradeBtn, Color.valueOf("4FB9AF"), Color.WHITE);
            table.add(upgradeBtn).padBottom(20).row();
        }
        // 패배했을 경우 리트라이 옵션 제공
        else {
            Label retryBtn = new Label("[ RE-TRY ]", new Label.LabelStyle(game.mainFont, Color.WHITE));
            retryBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    game.playClick();

                    // [핵심] 리트라이 시 현재 음악을 완전히 멈춰야 새로운 화면의 show()에서 처음부터 다시 틀어줍니다.
                    if (game.battleBgm != null) {
                        game.battleBgm.stop();
                    }

                    // 동일한 데이터로 배틀 스크린 재시작
                    game.setScreen(new BattleScreen(game, playerTeam, heroName, heroStat, stageLevel));
                }
            });
            UI.addHoverEffect(game, retryBtn, Color.WHITE, Color.GOLD);
            table.add(retryBtn).padBottom(20).row();
        }

        // [공통 버튼] 맵 화면으로 돌아가기
        Label homeBtn = new Label("[ GO TO MAP ]", new Label.LabelStyle(game.mainFont, Color.LIGHT_GRAY));
        homeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.playClick();
                // 로딩 화면을 거쳐 스테이지 맵으로 복귀
                game.setScreen(new LoadingScreen(game, new StageMapScreen(game)));
            }
        });
        UI.addHoverEffect(game, homeBtn, Color.LIGHT_GRAY, Color.WHITE);
        table.add(homeBtn).padBottom(20).row();

        // [공통 버튼] 메인 타이틀로 복귀
        Label titleBtn = new Label("[ RETURN HOME ]", new Label.LabelStyle(game.mainFont, Color.valueOf("7F8C8D")));
        titleBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.playClick();
                game.setScreen(new MenuScreen(game));
            }
        });
        UI.addHoverEffect(game, titleBtn, Color.valueOf("7F8C8D"), Color.WHITE);
        table.add(titleBtn).padBottom(10);

        // 준비된 테이블을 스테이지에 추가하고 입력 권한을 스테이지로 넘깁니다.
        stage.addActor(table);
        Gdx.input.setInputProcessor(stage);
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
    }

    // 게임오버 확인
    public boolean isGameOver() {
        return gameOver;
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
        if (battleBg != null) battleBg.dispose();
        if (tileTop != null) tileTop.dispose();
    }
}
