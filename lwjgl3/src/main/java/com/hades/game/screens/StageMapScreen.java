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
    private float targetZoom = 2.5f; // 카메라가 도달할 목표 줌 (클수록 멀리 보임)
    private final float moveSpeedFactor = 0.12f; // 카메라 위치 보간 속도 (Lerp)
    private final float zoomSpeedFactor = 0.08f; // 카메라 줌 보간 속도 (Lerp)
    private float stateTime = 0f; // 애니메이션(노드 펄스 효과 등)을 위한 누적 시간

    // 맵 및 노드 설정
    private static final float MAP_WIDTH = 1280f; // 전체 맵 가로 크기
    private static final float MAP_HEIGHT = 2560f; // 전체 맵 세로 크기 (수직형)
    private final float[][] nodePositions = { // 각 스테이지 노드의 월드 좌표 {x, y}
        {640, 300}, {450, 650}, {830, 950}, {640, 1300},
        {450, 1650}, {830, 1950}, {640, 2300}
    };
    private final String[] stageNames = {"아케론 강", "탄탈로스의 늪", "엘리시움", "타르타로스", "스틱스", "올림포스 관문", "제우스의 옥좌"};
    private final String[] stageBosses = {"보스: 데메테르", "보스: 헤스티아", "보스: 아테나", "보스: 아르테미스", "보스: 헤라", "보스: 아프로디테", "보스: 제우스"};

    // 상태 제어 및 클릭 판정 영역
    private int selectedStageIndex = -1; // 현재 선택(클릭)된 스테이지 인덱스
    private boolean isInfoWindowOpen = false; // 정보 창 표시 여부
    private final Rectangle infoWindowRect = new Rectangle(); // 정보 창 터치 무시용 영역
    private final Rectangle startButtonRect = new Rectangle(); // 전투 시작 버튼 영역
    private final Rectangle saveButtonRect = new Rectangle(); // 저장하기 버튼 영역
    private final Rectangle homeButtonRect = new Rectangle(); // 홈으로 버튼 영역
    private final Vector3 touchPoint = new Vector3(); // 터치 좌표 변환용 벡터

    // 알림 메시지 (기록 완료, 잠금 메시지 등)
    private String saveMessage = ""; // 화면에 표시할 메시지 텍스트
    private float saveMessageTimer = 0f; // 메시지 표시 유지 시간 (0보다 크면 표시)

    public StageMapScreen(HadesGame game) {
        this.game = game;
        this.cam = new OrthographicCamera();
        // 맵의 가로폭에 맞춰 뷰포트 설정
        this.viewport = new ExtendViewport(MAP_WIDTH, GameConfig.VIRTUAL_HEIGHT, cam);
        this.shapeRenderer = new ShapeRenderer();

        // 이미지 로드
        backgroundTexture = new Texture(Gdx.files.internal("images/background/stage_map_full.png"));
        nodeLocked = new Texture(Gdx.files.internal("images/ui/map/node_locked.png"));
        nodeCurrent = new Texture(Gdx.files.internal("images/ui/map/node_current.png"));
        nodeClear = new Texture(Gdx.files.internal("images/ui/map/node_clear.png"));
        infoWindowTex = new Texture(Gdx.files.internal("images/ui/map/info_panel.png"));
        moveBtnTex = new Texture(Gdx.files.internal("images/ui/map/move_icon.png"));

        // 시작 시 카메라 위치 설정: 현재 진행 중인 스테이지 레벨을 중앙에 배치
        int currentIdx = MathUtils.clamp(game.runState.currentStageLevel - 1, 0, nodePositions.length - 1);
        targetPos.set(nodePositions[currentIdx][0], nodePositions[currentIdx][1], 0);
        cam.position.set(targetPos.x, targetPos.y, 0);
        cam.zoom = 2.0f;
        targetZoom = 2.0f;
    }

    @Override
    public void show() {
        setupInputProcessor();
    }

    // 터치 및 드래그, 스크롤 입력 처리 설정
    private void setupInputProcessor() {
        Gdx.input.setInputProcessor(new InputAdapter() {
            private float dragDistance = 0;

            @Override
            public boolean scrolled(float amountX, float amountY) {
                if (isInfoWindowOpen) return true;
                // 마우스 휠 스크롤 시 카메라 목표 높이 조절
                targetPos.y -= amountY * 250f * cam.zoom;
                return true;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                dragDistance = 0; // 드래그 누적 거리 초기화
                return true;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                if (isInfoWindowOpen) return true;
                // 드래그 시 카메라 위치 직접 이동 (손가락 움직임 반영)
                float deltaY = Gdx.input.getDeltaY();
                targetPos.y += deltaY * cam.zoom;
                dragDistance += Math.abs(deltaY);
                return true;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                // 짧은 터치(드래그가 적을 때)만 클릭으로 간주
                if (dragDistance < 15) {
                    processClick(screenX, screenY);
                }
                return true;
            }
        });
    }

    // 클릭 시 좌표 판정 및 로직 수행
    private void processClick(int screenX, int screenY) {
        touchPoint.set(screenX, screenY, 0);
        viewport.unproject(touchPoint); // 화면 좌표를 게임 월드 좌표로 변환

        if (checkSystemButtons()) return; // 하단 버튼 클릭 시 스테이지 클릭 무시

        // 정보 창의 '전투 시작' 버튼 클릭 판정
        if (isInfoWindowOpen && startButtonRect.contains(touchPoint.x, touchPoint.y)) {
            game.playClick();
            game.setScreen(new BaseCutsceneScreen(game,
                CutsceneManager.getStageData(selectedStageIndex + 1),
                new BattleScreen(game, game.runState.selectedFaction, game.runState.selectedHeroName, game.runState.heroStat, selectedStageIndex + 1)));
            return;
        }

        // 맵 상의 노드 클릭 판정
        int clickedNode = -1;
        for (int i = 0; i < nodePositions.length; i++) {
            if (touchPoint.dst(nodePositions[i][0], nodePositions[i][1], 0) < 80f) {
                clickedNode = i;
                break;
            }
        }

        if (clickedNode != -1) {
            // 현재 진행 가능한 스테이지인지 확인
            if (clickedNode + 1 <= game.runState.currentStageLevel) {
                selectedStageIndex = clickedNode;
                isInfoWindowOpen = true;
                targetZoom = 1.5f; // 선택 시 확대
                targetPos.set(nodePositions[clickedNode][0] + 180f, nodePositions[clickedNode][1], 0); // 정보창을 고려한 옆 이동
                game.playClick();
            } else {
                // 잠긴 스테이지 클릭 시 알림
                saveMessage = "운명이 아직 이 성좌를 허락하지 않았습니다.";
                saveMessageTimer = 2.0f;
            }
        } else {
            // 노드 외부 클릭 시 정보 창 닫기 및 줌 복구
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

    // 하단 시스템 버튼(기록하기, 홈으로) 영역 및 클릭 체크
    private boolean checkSystemButtons() {
        float viewH = viewport.getWorldHeight() * cam.zoom;
        float bW = 150f * cam.zoom;
        float bH = 50f * cam.zoom;
        float bY = cam.position.y - (viewH / 2f) + 50f * cam.zoom; // 화면 하단 고정 좌표

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

        // 카메라 가용 범위 계산 및 보간 이동
        float currentHalfW = (viewport.getWorldWidth() * cam.zoom) / 2f;
        float currentHalfH = (viewport.getWorldHeight() * cam.zoom) / 2f;

        cam.zoom = MathUtils.lerp(cam.zoom, targetZoom, zoomSpeedFactor);

        // 카메라가 맵 밖으로 나가지 않도록 고정
        targetPos.x = MathUtils.clamp(targetPos.x, currentHalfW, MAP_WIDTH - currentHalfW);
        targetPos.y = MathUtils.clamp(targetPos.y, currentHalfH, MAP_HEIGHT - currentHalfH);
        if (currentHalfW * 2 > MAP_WIDTH) targetPos.x = MAP_WIDTH / 2f;

        cam.position.x = MathUtils.lerp(cam.position.x, targetPos.x, moveSpeedFactor);
        cam.position.y = MathUtils.lerp(cam.position.y, targetPos.y, moveSpeedFactor);
        cam.update();

        game.batch.setProjectionMatrix(cam.combined);
        game.batch.begin();

        // 1. 배경 드로우
        game.batch.draw(backgroundTexture, 0, 0, MAP_WIDTH, MAP_HEIGHT);

        // 2. 스테이지 노드 드로우 (현재 스테이지는 펄스 효과 적용)
        for (int i = 0; i < nodePositions.length; i++) {
            Texture tex = (i + 1 < game.runState.currentStageLevel) ? nodeClear :
                (i + 1 == game.runState.currentStageLevel) ? nodeCurrent : nodeLocked;
            float pulse = (i + 1 == game.runState.currentStageLevel) ? 1.0f + (float)Math.sin(stateTime * 4f) * 0.05f : 1.0f;
            game.batch.draw(tex, nodePositions[i][0] - 64 * pulse, nodePositions[i][1] - 64 * pulse, 128 * pulse, 128 * pulse);
        }

        // 3. 시스템 UI(하단 버튼) 드로우
        drawSystemUI();

        // 4. 스테이지 상세 정보창 드로우
        if (isInfoWindowOpen) drawInfoWindow();

        // 5. 알림 메시지 드로우 (시간 초과 시 자동 소멸)
        if (saveMessageTimer > 0) {
            float alpha = MathUtils.clamp(saveMessageTimer, 0, 1f);
            game.mainFont.setColor(0, 1f, 0.9f, alpha);
            game.mainFont.getData().setScale(cam.zoom * 0.75f);

            // 1. 텍스트 박스(레이아웃) 생성 및 문구 너비 계산
            // 이 박스는 눈에 보이지 않지만, 문구의 실제 가로/세로 길이를 측정
            com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout();
            layout.setText(game.mainFont, saveMessage);

            // 2. 계산된 박스 크기를 바탕으로 정중앙 좌표 산출
            // (카메라 중심) - (박스 너비의 절반) = 완벽한 좌우 중앙
            float textX = cam.position.x - (layout.width / 2);
            // (카메라 중심) + (박스 높이의 절반) = 완벽한 상하 중앙
            float textY = cam.position.y + (layout.height / 2);

            // 3. 그리기
            game.mainFont.draw(game.batch, saveMessage, textX, textY);

            saveMessageTimer -= delta;
            game.mainFont.setColor(Color.WHITE);
            game.mainFont.getData().setScale(1.0f);
        }

        game.batch.end();
    }

    // 하단 시스템 UI 그리기 및 텍스트 설정
    private void drawSystemUI() {
        float bW = 150f * cam.zoom;
        float bH = 50f * cam.zoom;
        float viewH = viewport.getWorldHeight() * cam.zoom;
        float bY = cam.position.y - (viewH / 2f) + 50f * cam.zoom;

        // 버튼 배경 (반투명 블랙)
        game.batch.setColor(0, 0, 0, 0.8f);
        game.batch.draw(infoWindowTex, cam.position.x - 160f * cam.zoom, bY, bW, bH);
        game.batch.draw(infoWindowTex, cam.position.x + 10f * cam.zoom, bY, bW, bH);
        game.batch.setColor(Color.WHITE);

        // 버튼 텍스트
        // 폰트 크기 동적 조절
        game.detailFont.getData().setScale(cam.zoom * 0.8f);

        game.detailFont.setColor(Color.LIGHT_GRAY);
        game.detailFont.draw(game.batch, "기록하기", cam.position.x - 125f * cam.zoom, bY + 35f * cam.zoom);

        game.detailFont.setColor(Color.LIGHT_GRAY);
        game.detailFont.draw(game.batch, "홈으로", cam.position.x + 50f * cam.zoom, bY + 35f * cam.zoom);

        // 폰트 상태 복구
        game.detailFont.setColor(Color.WHITE);
        game.detailFont.getData().setScale(1.0f);
    }

    // 선택된 스테이지의 상세 정보 패널 드로우
    private void drawInfoWindow() {
        float winW = 550f, winH = 400f;
        float winX = nodePositions[selectedStageIndex][0] + 120f;
        float winY = nodePositions[selectedStageIndex][1] - winH / 2f;
        infoWindowRect.set(winX, winY, winW, winH);

        game.batch.draw(infoWindowTex, winX, winY, winW, winH);

        float padding = 70f;
        // 스테이지 정보 텍스트 (순서: 스테이지 번호 -> 이름 -> 보스)
        game.detailFont2.draw(game.batch, "STAGE " + (selectedStageIndex + 1), winX + padding, winY + winH - 100);
        game.mainFont.getData().setScale(0.7f);
        game.mainFont.draw(game.batch, stageNames[selectedStageIndex], winX + padding, winY + winH - 150);
        game.mainFont.getData().setScale(1.0f);

        game.detailFont.setColor(Color.GOLDENROD);
        game.detailFont.draw(game.batch, stageBosses[selectedStageIndex], winX + padding, winY + winH - 210);
        game.detailFont.setColor(Color.WHITE);

        // 전투 시작(이동) 버튼 이미지 영역 설정
        startButtonRect.set(winX + (winW - 280) / 2f, winY + 50, 280, 90);
        game.batch.draw(moveBtnTex, startButtonRect.x, startButtonRect.y, startButtonRect.width, startButtonRect.height);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
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
