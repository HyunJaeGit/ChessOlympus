package com.hades.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.hades.game.HadesGame;
import com.hades.game.constants.GameConfig;
import com.hades.game.screens.cutscene.BaseCutsceneScreen;
import com.hades.game.screens.cutscene.CutsceneManager;

// Chess Olympus: HADES vs ZEUS - 스테이지 선택 및 진행 화면
// 월드 맵 내 노드를 선택하여 전투 단계로 진입하거나 게임 상태를 저장합니다.
public class StageMapScreen extends ScreenAdapter {
    private final HadesGame game;
    private final OrthographicCamera cam;
    private final Viewport viewport;
    private final ShapeRenderer shapeRenderer;

    // 리소스 영역
    private final Texture backgroundTexture;
    private final Texture nodeLocked, nodeCurrent, nodeClear;
    private final Texture infoWindowTex;
    private final Texture moveBtnTex;

    // 카메라 및 뷰 제어 변수
    private final Vector3 targetPos;
    private float targetZoom = 2.5f;
    private float stateTime = 0f;
    private final float moveSpeedFactor = 0.15f;
    private final float zoomSpeedFactor = 0.08f;

    // 맵 데이터 및 노드 설정
    private static final float MAP_WIDTH = 1280f;
    private static final float MAP_HEIGHT = 2560f;
    private final float[][] nodePositions = {
        {640, 300}, {450, 650}, {830, 950}, {640, 1300},
        {450, 1650}, {830, 1950}, {640, 2300}
    };
    private final String[] stageNames = {"아케론 강", "탄탈로스의 늪", "엘리시움", "타르타로스", "스틱스", "올림포스 관문", "제우스의 옥좌"};
    private final String[] stageBosses = {"보스: 데메테르", "보스: 헤스티아", "보스: 아테나", "보스: 아르테미스", "보스: 헤라", "보스: 아프로디테", "보스: 제우스"};

    // UI 인터랙션 영역
    private int selectedStageIndex = -1;
    private boolean isInfoWindowOpen = false;
    private final Rectangle infoWindowRect = new Rectangle();
    private final Rectangle startButtonRect = new Rectangle();
    private final Rectangle saveButtonRect = new Rectangle();
    private final Rectangle homeButtonRect = new Rectangle();
    private final Vector3 touchPoint = new Vector3();

    // 알림 메시지 변수
    private String saveMessage = "";
    private float saveMessageTimer = 0f;

    public StageMapScreen(HadesGame game) {
        this.game = game;
        this.cam = new OrthographicCamera();
        this.viewport = new ExtendViewport(MAP_WIDTH, GameConfig.VIRTUAL_HEIGHT, cam);
        this.shapeRenderer = new ShapeRenderer();

        // 그래픽 에셋 로드
        backgroundTexture = new Texture(Gdx.files.internal("images/background/stage_map_full.png"));
        nodeLocked = new Texture(Gdx.files.internal("images/ui/map/node_locked.png"));
        nodeCurrent = new Texture(Gdx.files.internal("images/ui/map/node_current.png"));
        nodeClear = new Texture(Gdx.files.internal("images/ui/map/node_clear.png"));
        infoWindowTex = new Texture(Gdx.files.internal("images/ui/map/info_panel.png"));
        moveBtnTex = new Texture(Gdx.files.internal("images/ui/map/move_icon.png"));

        // 현재 진행도(RunState)에 따른 초기 카메라 위치 설정
        int currentIdx = MathUtils.clamp(game.runState.currentStageLevel - 1, 0, nodePositions.length - 1);
        float startX = nodePositions[currentIdx][0];
        float startY = nodePositions[currentIdx][1];

        targetPos = new Vector3(startX, startY, 0);
        cam.position.set(startX, startY, 0);
        cam.zoom = 2.0f;

        // 하단 고정 UI 영역 설정
        saveButtonRect.set(MAP_WIDTH / 2f - 230f, 50f, 220f, 80f);
        homeButtonRect.set(MAP_WIDTH / 2f + 10f, 50f, 220f, 80f);
    }

    @Override
    public void show() {
        // 화면 진입 시 입력 처리기 활성화 및 배경음악 상태 체크
        setupInputProcessor();
        if (game.menuBgm != null && !game.menuBgm.isPlaying()) {
            game.menuBgm.setLooping(true);
            game.menuBgm.setVolume(game.globalVolume);
            game.menuBgm.play();
        }
    }

    // 터치 및 드래그, 스크롤 입력 로직 정의
    private void setupInputProcessor() {
        Gdx.input.setInputProcessor(new InputAdapter() {
            private boolean isMapMoved = false;
            private float dragDistance = 0;

            @Override
            public boolean scrolled(float amountX, float amountY) {
                if (isInfoWindowOpen) return true;
                targetPos.y -= amountY * 150f * cam.zoom;
                return true;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                isMapMoved = false;
                dragDistance = 0;
                return true;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                if (isInfoWindowOpen) return true;
                float deltaY = Gdx.input.getDeltaY();
                if (Math.abs(deltaY) > 2) {
                    targetPos.y += deltaY * cam.zoom;
                    dragDistance += Math.abs(deltaY);
                    isMapMoved = true;
                }
                return true;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                // 단순 클릭 시에만 processClick 실행 (드래그 시 클릭 판정 방지)
                if (!isMapMoved || dragDistance < 15) processClick(screenX, screenY);
                return true;
            }
        });
    }

    // 클릭 좌표에 따른 UI 상호작용 처리
    private void processClick(int screenX, int screenY) {
        touchPoint.set(screenX, screenY, 0);
        viewport.unproject(touchPoint);

        // 1. 저장하기 버튼: 현재 RunState 저장 및 메시지 표시
        if (saveButtonRect.contains(touchPoint.x, touchPoint.y)) {
            game.playClick();
            game.saveGame();
            saveMessage = "기록이 성좌에 새겨졌습니다.";
            saveMessageTimer = 2.0f;
            return;
        }

        // 2. 홈으로 버튼: 메인 메뉴로 복귀
        if (homeButtonRect.contains(touchPoint.x, touchPoint.y)) {
            game.playClick();
            game.setScreen(new MenuScreen(game));
            return;
        }

        // 3. 전투 시작 버튼: 컷신 및 전투 화면으로 진입 (음악 중단 포함)
        if (isInfoWindowOpen && startButtonRect.contains(touchPoint.x, touchPoint.y)) {
            game.playClick();
            // 배경음악 중복 방지를 위해 명시적 중단
            if (game.menuBgm != null) game.menuBgm.stop();

            int stageNum = selectedStageIndex + 1;
            game.setScreen(new BaseCutsceneScreen(
                game,
                CutsceneManager.getStageData(stageNum),
                new BattleScreen(game, game.runState.selectedFaction, game.runState.selectedHeroName, game.runState.heroStat, stageNum)
            ));
            return;
        }

        // 4. 노드 클릭: 스테이지 상세 정보창 열기
        boolean nodeClicked = false;
        for (int i = 0; i < nodePositions.length; i++) {
            float dist = touchPoint.dst(nodePositions[i][0], nodePositions[i][1], 0);
            if (dist < 100f) {
                if (i + 1 <= game.runState.currentStageLevel) {
                    selectedStageIndex = i;
                    isInfoWindowOpen = true;
                    targetZoom = 1.0f;
                    targetPos.set(nodePositions[i][0] + 180f, nodePositions[i][1], 0);
                    game.playClick();
                    nodeClicked = true;
                }
                break;
            }
        }

        // 5. 빈 공간 클릭: 정보창 닫기 및 카메라 줌 리셋
        if (!nodeClicked) {
            if (isInfoWindowOpen && !infoWindowRect.contains(touchPoint.x, touchPoint.y)) {
                isInfoWindowOpen = false;
                selectedStageIndex = -1;
                game.playClick();
            } else if (!isInfoWindowOpen) {
                targetZoom = 2.5f;
                targetPos.set(MAP_WIDTH / 2f, MAP_HEIGHT / 2f, 0);
                game.playClick();
            }
        }
    }

    @Override
    public void render(float delta) {
        stateTime += delta;
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 카메라 보간 및 위치 제한(Clamping) 처리
        float currentHalfW = (viewport.getWorldWidth() * cam.zoom) / 2f;
        float currentHalfH = (viewport.getWorldHeight() * cam.zoom) / 2f;
        float finalTargetX = MathUtils.clamp(targetPos.x, currentHalfW, MAP_WIDTH - currentHalfW);
        float finalTargetY = MathUtils.clamp(targetPos.y, currentHalfH, MAP_HEIGHT - currentHalfH);
        if (currentHalfW * 2 > MAP_WIDTH) finalTargetX = MAP_WIDTH / 2f;

        cam.position.x = MathUtils.lerp(cam.position.x, finalTargetX, moveSpeedFactor);
        cam.position.y = MathUtils.lerp(cam.position.y, finalTargetY, moveSpeedFactor);
        cam.zoom = Interpolation.smooth.apply(cam.zoom, targetZoom, zoomSpeedFactor);
        cam.update();

        // 1단계: 배경 연결선 렌더링
        shapeRenderer.setProjectionMatrix(cam.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(1, 1, 1, 0.2f);
        for (int i = 0; i < nodePositions.length - 1; i++) {
            shapeRenderer.line(nodePositions[i][0], nodePositions[i][1], nodePositions[i+1][0], nodePositions[i+1][1]);
        }
        shapeRenderer.end();

        // 2단계: 맵 배경 및 스테이지 노드 렌더링
        game.batch.setProjectionMatrix(cam.combined);
        game.batch.begin();
        game.batch.draw(backgroundTexture, 0, 0, MAP_WIDTH, MAP_HEIGHT);

        for (int i = 0; i < nodePositions.length; i++) {
            float x = nodePositions[i][0], y = nodePositions[i][1];
            Texture tex = (i + 1 < game.runState.currentStageLevel) ? nodeClear :
                (i + 1 == game.runState.currentStageLevel) ? nodeCurrent : nodeLocked;

            // 현재 진행해야 할 노드에 애니메이션 효과 적용
            float scale = (i + 1 == game.runState.currentStageLevel) ? 1.0f + (float)Math.sin(stateTime * 5f) * 0.1f : 1.0f;
            game.batch.draw(tex, x - (128*scale)/2f, y - (128*scale)/2f, 128*scale, 128*scale);
        }

        // 3단계: 시스템 버튼 및 정보창 렌더링
        drawMapButtons();

        if (isInfoWindowOpen && selectedStageIndex != -1) drawInfoWindow();

        // 저장 안내 메시지 페이드 아웃 처리
        if (saveMessageTimer > 0) {
            game.mainFont.setColor(0, 1, 0.8f, MathUtils.clamp(saveMessageTimer, 0, 1));
            game.mainFont.draw(game.batch, saveMessage, MAP_WIDTH / 2f - 200f, 180f);
            saveMessageTimer -= delta;
        }
        game.batch.end();
    }

    // 하단 시스템 제어 버튼 그리기
    private void drawMapButtons() {
        game.batch.setColor(0.2f, 0.2f, 0.2f, 0.8f);
        game.batch.draw(infoWindowTex, saveButtonRect.x, saveButtonRect.y, saveButtonRect.width, saveButtonRect.height);
        game.batch.setColor(Color.WHITE);
        game.detailFont.draw(game.batch, "저장하기", saveButtonRect.x + 45, saveButtonRect.y + 50);

        game.batch.setColor(0.2f, 0.2f, 0.2f, 0.8f);
        game.batch.draw(infoWindowTex, homeButtonRect.x, homeButtonRect.y, homeButtonRect.width, homeButtonRect.height);
        game.batch.setColor(Color.valueOf("7F8C8D"));
        game.detailFont.draw(game.batch, "홈으로", homeButtonRect.x + 60, homeButtonRect.y + 50);
        game.batch.setColor(Color.WHITE);
    }

    // 스테이지 상세 정보 패널 그리기
    private void drawInfoWindow() {
        float winW = 550f, winH = 380f;
        float winX = nodePositions[selectedStageIndex][0] + 120f;
        float winY = nodePositions[selectedStageIndex][1] - winH / 2f;
        infoWindowRect.set(winX, winY, winW, winH);

        game.batch.draw(infoWindowTex, winX, winY, winW, winH);
        game.detailFont2.draw(game.batch, "STAGE " + (selectedStageIndex + 1) + ": " + stageNames[selectedStageIndex], winX + 80, winY + winH - 100);
        game.detailFont.draw(game.batch, stageBosses[selectedStageIndex], winX + 80, winY + winH - 160);

        float btnW = 220f, btnH = 90f;
        float btnX = winX + (winW - btnW) / 2f;
        float btnY = winY + 50f;
        startButtonRect.set(btnX, btnY, btnW, btnH);
        game.batch.draw(moveBtnTex, btnX, btnY, btnW, btnH);
    }

    @Override
    public void resize(int width, int height) { viewport.update(width, height); }

    @Override
    public void hide() {
        // 스크린이 비활성화될 때 입력 프로세서 해제
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        backgroundTexture.dispose();
        nodeLocked.dispose();
        nodeCurrent.dispose();
        nodeClear.dispose();
        infoWindowTex.dispose();
        moveBtnTex.dispose();
        shapeRenderer.dispose();
    }
}
