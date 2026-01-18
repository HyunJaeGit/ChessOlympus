package com.hades.game.logic;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

// 카메라의 확대/축소 및 이동을 전담하는 매니저
public class CameraManager {
    private final OrthographicCamera camera;
    private float targetZoom = 1.0f;
    private final float minZoom = 0.5f;
    private final float maxZoom = 2.0f;
    private final float lerpSpeed = 0.15f; // 줌 부드러움 정도

    // 복귀할 중앙 좌표와 상태 제어 변수
    private final Vector2 originPos = new Vector2();
    private boolean isPanning = false;

    public CameraManager(OrthographicCamera camera) {
        this.camera = camera;
        // 1. 생성 시점의 카메라 위치(화면 중앙)를 복귀 지점으로 저장합니다.
        this.originPos.set(camera.position.x, camera.position.y);
    }

    // 마우스 휠 입력 처리
    public void handleScroll(float amountY) {
        targetZoom += amountY * 0.15f;
        targetZoom = MathUtils.clamp(targetZoom, minZoom, maxZoom);
    }

    // 우클릭 드래그 화면 이동 처리
    public void handlePan(float deltaX, float deltaY) {
        isPanning = true; // 드래그가 시작됨을 알림
        // 카메라의 줌 수치를 곱해줘야 줌 상태에 상관없이 일정한 속도로 움직입니다.
        camera.position.x -= deltaX * camera.zoom;
        camera.position.y += deltaY * camera.zoom;
    }

    // 드래그가 멈췄을 때 호출할 메서드
    public void stopPanning() {
        isPanning = false;
    }

    // 매 프레임 업데이트 (BattleScreen의 render 메서드에서 호출)
    public void update() {
        // 1. 선형 보간(Lerp)을 사용하여 줌을 부드럽게 변경합니다.
        camera.zoom = MathUtils.lerp(camera.zoom, targetZoom, lerpSpeed);

        // 2. 드래그 중이 아닐 때만 원래 위치(originPos)로 부드럽게 복귀시킵니다.
        if (!isPanning) {
            // 위치 복귀 속도는 0.1f 정도로 설정 (숫자가 작을수록 천천히 돌아옴)
            camera.position.x = MathUtils.lerp(camera.position.x, originPos.x, 0.1f);
            camera.position.y = MathUtils.lerp(camera.position.y, originPos.y, 0.1f);
        }

        camera.update();
    }
}
