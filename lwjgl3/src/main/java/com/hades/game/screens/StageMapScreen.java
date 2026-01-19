package com.hades.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.hades.game.HadesGame;
import com.hades.game.constants.GameConfig;
import com.hades.game.screens.cutscene.BaseCutsceneScreen;
import com.hades.game.screens.cutscene.CutsceneManager;

// Chess Olympus: HADES vs ZEUS - 스테이지 선택 및 맵 이동 화면
public class StageMapScreen extends ScreenAdapter {
    private final HadesGame game;
    private final OrthographicCamera cam;
    private final Viewport viewport;
    private final ShapeRenderer shapeRenderer;

    // 리소스 이미지
    private final Texture backgroundTexture; // 수직형 전체 맵 배경
    private final Texture nodeLocked, nodeCurrent, nodeClear; // 스테이지 노드 상태별 아이콘
    private final Texture infoWindowTex; // 스테이지 정보 창 배경 패널
    private final Texture moveBtnTex; // 전투 시작(이동) 버튼 이미지

    // 카메라 및 이동 제어 수치
    private final Vector3 targetPos = new Vector3(); // 카메라가 부드럽게 이동할 목표 지점
    private float targetZoom = 2.5f; // 카메라가 도달할 목표 줌
    private final float moveSpeedFactor = 0.12f; // 카메라 위치 보간 속도
    private final float zoomSpeedFactor = 0.08f; // 카메라 줌 보간 속도
    private float stateTime = 0f; // 애니메이션을 위한 누적 시간

    // 맵 및 노드 설정
    private static final float MAP_WIDTH = 1280f;
    private static final float MAP_HEIGHT = 2560f;
    private final float[][] nodePositions = {
        {640, 300}, {450, 650}, {830, 950}, {640, 1300},
        {450, 1650}, {830, 1950}, {640, 2300}
    };
    private final String[] stageNames = {"아케론 강", "탄탈로스의 늪", "엘리시움", "타르타로스", "스틱스", "올림포스 관문", "제우스의 옥좌"};
    private final String[] stageBosses = {"보스: 데메테르", "보스: 헤스티아", "보스: 아테나", "보스: 아르테미스", "보스: 헤라", "보스: 아프로디테", "보스: 제우스"};

    // 상태 제어 및 클릭 판정 영역
    private int selectedStageIndex = -1;
    private boolean isInfoWindowOpen = false;
    private final Rectangle infoWindowRect = new Rectangle();
    private final Rectangle startButtonRect = new Rectangle();
    private final Rectangle saveButtonRect = new Rectangle();
    private final Rectangle homeButtonRect = new Rectangle();
    private final Vector3 touchPoint = new Vector3();

    // 알림 메시지
    private String saveMessage = "";
    private float saveMessageTimer = 0f;

    public StageMapScreen(HadesGame game) {
        this.game = game;
        this.cam = new OrthographicCamera();
        this.viewport = new ExtendViewport(MAP_WIDTH, GameConfig.VIRTUAL_HEIGHT, cam);
        this.shapeRenderer = new ShapeRenderer();

        backgroundTexture = new Texture(Gdx.files.internal("images/background/stage_map_full.png"));
        nodeLocked = new Texture(Gdx.files.internal("images/ui/map/node_locked.png"));
        nodeCurrent = new Texture(Gdx.files.internal("images/ui/map/node_current.png"));
        nodeClear = new Texture(Gdx.files.internal("images/ui/map/node_clear.png"));
        infoWindowTex = new Texture(Gdx.files.internal("images/ui/map/info_panel.png"));
        moveBtnTex = new Texture(Gdx.files.internal("images/ui/map/move_icon.png"));

        int currentIdx = MathUtils.clamp(game.runState.currentStageLevel - 1, 0, nodePositions.length - 1);
        targetPos.set(nodePositions[currentIdx][0], nodePositions[currentIdx][1], 0);
        cam.position.set(targetPos.x, targetPos.y, 0);
        cam.zoom = 2.0f;
        targetZoom = 2.0f;
    }

    @Override
    public void show() {
        setupInputProcessor();
        // [수정] AudioManager를 통해 music/ 폴더 내 맵 테마 재생
        game.audioManager.playBgm("music/map.mp3");
    }

    private void setupInputProcessor() {
        Gdx.input.setInputProcessor(new InputAdapter() {
            private float dragDistance = 0;

            @Override
            public boolean scrolled(float amountX, float amountY) {
                if (isInfoWindowOpen) return true;
                targetPos.y -= amountY * 250f * cam.zoom;
                return true;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                dragDistance = 0;
                return true;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                if (isInfoWindowOpen) return true;
                float deltaY = Gdx.input.getDeltaY();
                targetPos.y += deltaY * cam.zoom;
                dragDistance += Math.abs(deltaY);
                return true;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                if (dragDistance < 15) {
                    processClick(screenX, screenY);
                }
                return true;
            }
        });
    }

    private void processClick(int screenX, int screenY) {
        touchPoint.set(screenX, screenY, 0);
        viewport.unproject(touchPoint);

        if (checkSystemButtons()) return;

        // 전투 시작 버튼 클릭 시: 컷씬으로 넘어가기 전 기존 음악 정지
        if (isInfoWindowOpen && startButtonRect.contains(touchPoint.x, touchPoint.y)) {
            game.playClick();

            // [수정] AudioManager를 통해 배경음악 정지
            game.audioManager.stopBgm();

            game.setScreen(new BaseCutsceneScreen(game,
                CutsceneManager.getStageData(selectedStageIndex + 1),
                new BattleScreen(game, game.runState.selectedFaction, game.runState.selectedHeroName, game.runState.heroStat, selectedStageIndex + 1)));
            return;
        }

        int clickedNode = -1;
        for (int i = 0; i < nodePositions.length; i++) {
            if (touchPoint.dst(nodePositions[i][0], nodePositions[i][1], 0) < 80f) {
                clickedNode = i;
                break;
            }
        }

        if (clickedNode != -1) {
            if (clickedNode + 1 <= game.runState.currentStageLevel) {
                selectedStageIndex = clickedNode;
                isInfoWindowOpen = true;
                targetZoom = 1.5f;
                targetPos.set(nodePositions[clickedNode][0] + 180f, nodePositions[clickedNode][1], 0);
                game.playClick();
            } else {
                saveMessage = "운명이 아직 이 성좌를 허락하지 않았습니다.";
                saveMessageTimer = 2.0f;
            }
        } else {
            if (isInfoWindowOpen) {
                if (!infoWindowRect.contains(touchPoint.x, touchPoint.y)) {
                    isInfoWindowOpen = false;
                    game.playClick();
                }
            } else if (targetZoom < 2.0f) {
                targetZoom = 2.5f;
                targetPos.set(MAP_WIDTH / 2f, cam.position.y, 0);
                game.playClick();
            }
        }
    }

    private boolean checkSystemButtons() {
        float viewH = viewport.getWorldHeight() * cam.zoom;
        float bW = 150f * cam.zoom;
        float bH = 50f * cam.zoom;
        float bY = cam.position.y - (viewH / 2f) + 50f * cam.zoom;

        saveButtonRect.set(cam.position.x - 160f * cam.zoom, bY, bW, bH);
        homeButtonRect.set(cam.position.x + 10f * cam.zoom, bY, bW, bH);

        if (saveButtonRect.contains(touchPoint.x, touchPoint.y)) {
            game.saveGame();
            saveMessage = "성좌의 기록이 보존되었습니다.";
            saveMessageTimer = 2.0f;
            game.playClick();
            return true;
        }
        if (homeButtonRect.contains(touchPoint.x, touchPoint.y)) {
            game.playClick();
            // [수정] 홈으로 이동 시에도 음악 정지하여 메뉴 음악과 겹침 방지
            game.audioManager.stopBgm();
            game.setScreen(new MenuScreen(game));
            return true;
        }
        return false;
    }

    @Override
    public void render(float delta) {
        stateTime += delta;
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 줌 및 카메라 좌표 보간 업데이트
        cam.zoom = MathUtils.lerp(cam.zoom, targetZoom, zoomSpeedFactor);

        float currentHalfW = (viewport.getWorldWidth() * cam.zoom) / 2f;
        float currentHalfH = (viewport.getWorldHeight() * cam.zoom) / 2f;

        float clampedX = MathUtils.clamp(targetPos.x, currentHalfW, MAP_WIDTH - currentHalfW);
        float clampedY = MathUtils.clamp(targetPos.y, currentHalfH, MAP_HEIGHT - currentHalfH);

        if (currentHalfW * 2 > MAP_WIDTH) clampedX = MAP_WIDTH / 2f;

        cam.position.x = MathUtils.lerp(cam.position.x, clampedX, moveSpeedFactor);
        cam.position.y = MathUtils.lerp(cam.position.y, clampedY, moveSpeedFactor);
        cam.update();

        game.batch.setProjectionMatrix(cam.combined);
        game.batch.begin();

        // 1. 배경 드로우
        game.batch.draw(backgroundTexture, 0, 0, MAP_WIDTH, MAP_HEIGHT);

        // 2. 스테이지 노드 드로우
        for (int i = 0; i < nodePositions.length; i++) {
            Texture tex = (i + 1 < game.runState.currentStageLevel) ? nodeClear :
                (i + 1 == game.runState.currentStageLevel) ? nodeCurrent : nodeLocked;

            float pulse = (i + 1 == game.runState.currentStageLevel) ? 1.0f + (float)Math.sin(stateTime * 4f) * 0.05f : 1.0f;
            game.batch.draw(tex, nodePositions[i][0] - 64 * pulse, nodePositions[i][1] - 64 * pulse, 128 * pulse, 128 * pulse);
        }

        // 3. 시스템 UI 드로우
        drawSystemUI();

        // 4. 스테이지 상세 정보창 드로우
        if (isInfoWindowOpen) drawInfoWindow();

        // 5. 알림 메시지 드로우
        if (saveMessageTimer > 0) {
            float alpha = MathUtils.clamp(saveMessageTimer, 0, 1f);
            game.mainFont.setColor(0, 1f, 0.9f, alpha);
            game.mainFont.getData().setScale(cam.zoom * 0.75f);

            com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout();
            layout.setText(game.mainFont, saveMessage);

            float textX = cam.position.x - (layout.width / 2);
            float textY = cam.position.y + (layout.height / 2);

            game.mainFont.draw(game.batch, saveMessage, textX, textY);

            saveMessageTimer -= delta;
            game.mainFont.setColor(Color.WHITE);
            game.mainFont.getData().setScale(1.0f);
        }

        game.batch.end();
    }

    private void drawSystemUI() {
        float bW = 150f * cam.zoom;
        float bH = 50f * cam.zoom;
        float viewH = viewport.getWorldHeight() * cam.zoom;
        float bY = cam.position.y - (viewH / 2f) + 50f * cam.zoom;

        game.batch.setColor(0, 0, 0, 0.8f);
        game.batch.draw(infoWindowTex, cam.position.x - 160f * cam.zoom, bY, bW, bH);
        game.batch.draw(infoWindowTex, cam.position.x + 10f * cam.zoom, bY, bW, bH);
        game.batch.setColor(Color.WHITE);

        game.detailFont.getData().setScale(cam.zoom * 0.8f);
        game.detailFont.setColor(Color.LIGHT_GRAY);
        game.detailFont.draw(game.batch, "기록하기", cam.position.x - 125f * cam.zoom, bY + 35f * cam.zoom);
        game.detailFont.draw(game.batch, "홈으로", cam.position.x + 50f * cam.zoom, bY + 35f * cam.zoom);

        game.detailFont.setColor(Color.WHITE);
        game.detailFont.getData().setScale(1.0f);
    }

    private void drawInfoWindow() {
        float winW = 550f, winH = 400f;
        float winX = nodePositions[selectedStageIndex][0] + 120f;
        float winY = nodePositions[selectedStageIndex][1] - winH / 2f;
        infoWindowRect.set(winX, winY, winW, winH);

        game.batch.draw(infoWindowTex, winX, winY, winW, winH);

        float padding = 70f;
        game.detailFont2.draw(game.batch, "STAGE " + (selectedStageIndex + 1), winX + padding, winY + winH - 100);
        game.mainFont.getData().setScale(0.7f);
        game.mainFont.draw(game.batch, stageNames[selectedStageIndex], winX + padding, winY + winH - 150);
        game.mainFont.getData().setScale(1.0f);

        game.detailFont.setColor(Color.GOLDENROD);
        game.detailFont.draw(game.batch, stageBosses[selectedStageIndex], winX + padding, winY + winH - 210);
        game.detailFont.setColor(Color.WHITE);

        startButtonRect.set(winX + (winW - 280) / 2f, winY + 50, 280, 90);
        game.batch.draw(moveBtnTex, startButtonRect.x, startButtonRect.y, startButtonRect.width, startButtonRect.height);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override
    public void hide() {
        // 화면 전환 시 입력 프로세서 해제
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
