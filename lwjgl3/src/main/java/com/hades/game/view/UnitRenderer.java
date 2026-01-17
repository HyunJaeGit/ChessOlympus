package com.hades.game.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.hades.game.entities.Unit;
import com.hades.game.logic.IsoUtils;

// 게임 화면에 배치된 유닛의 이미지, 그림자, 체력바 등을 실제로 그려주는 클래스입니다.
public class UnitRenderer {

    private SpriteBatch batch;
    private ShapeRenderer shape;
    private BitmapFont font;
    private String playerTeam;
    private GlyphLayout layout; // 텍스트의 가로 길이를 계산하기 위한 도구

    // 마우스 클릭 판정을 위한 히트박스 크기 설정
    private static final float HITBOX_W = 30f;
    private static final float HITBOX_H = 50f;

    public UnitRenderer(SpriteBatch batch, ShapeRenderer shape, BitmapFont unitFont, String playerTeam) {
        this.batch = batch;
        this.shape = shape;
        this.font = unitFont;
        this.playerTeam = playerTeam;
        this.layout = new GlyphLayout();
    }

    // 유닛의 발밑 그림자와 선택 효과를 먼저 그립니다. (바닥에 깔리는 요소)
    public void renderShadow(Unit unit, Unit selectedUnit) {
        // 그리드 좌표를 화면상의 픽셀 좌표로 변환
        Vector2 screenPos = IsoUtils.gridToScreen(unit.gridX, unit.gridY);
        boolean isSelected = (unit == selectedUnit);

        // 기본 타원형 그림자 그리기
        drawShadow(screenPos);

        // 선택된 유닛이라면 발밑에 강조 효과(링) 추가
        if (isSelected) {
            drawSelectionRing(unit, screenPos, true);
        }
    }

    // 유닛의 몸통(이미지)과 UI 요소(체력바, 이름)를 그립니다.
    public void renderBody(Unit unit, Unit selectedUnit) {
        Vector2 screenPos = IsoUtils.gridToScreen(unit.gridX, unit.gridY);
        // 유닛 객체가 내부적으로 가지고 있는 텍스처(fieldTexture) 참조
        Texture currentTexture = unit.fieldTexture;
        boolean isSelected = (unit == selectedUnit);

        // 유닛 이미지 출력
        drawUnitBody(currentTexture, screenPos);
        // 체력바 및 이름표 출력
        drawStatusUI(unit, screenPos, isSelected);
    }

    // 해당 유닛이 현재 마우스 위치에 있는지 확인 (클릭 판정)
    public boolean isMouseInsideHitbox(Unit unit, float mx, float my) {
        Vector2 pos = IsoUtils.gridToScreen(unit.gridX, unit.gridY);
        // 중앙 하단을 기점으로 가로 HITBOX_W, 세로 HITBOX_H 영역 계산
        return mx >= pos.x - (HITBOX_W / 2) && mx <= pos.x + (HITBOX_W / 2) &&
            my >= pos.y && my <= pos.y + HITBOX_H;
    }

    // 선택된 유닛의 발밑에 빛나는 타원 효과를 그립니다.
    private void drawSelectionRing(Unit unit, Vector2 pos, boolean isSelected) {
        // ShapeRenderer를 쓰기 위해 진행 중인 batch를 잠시 멈춤
        if (batch.isDrawing()) batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND); // 반투명 효과 활성화
        shape.begin(ShapeRenderer.ShapeType.Filled);

        Color glowColor = new Color(1, 1, 1, 0.2f);
        shape.setColor(glowColor);

        // 이중 타원을 그려서 은은한 오라 효과 표현
        shape.ellipse(pos.x - 35, pos.y - 17, 70, 35);
        shape.setColor(new Color(1, 1, 1, 0.1f));
        shape.ellipse(pos.x - 45, pos.y - 22, 90, 45);

        shape.end();
        batch.begin(); // 다시 이미지 렌더링을 위해 batch 시작
    }

    // 모든 유닛 공통 발밑 그림자
    private void drawShadow(Vector2 pos) {
        if (batch.isDrawing()) batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shape.begin(ShapeRenderer.ShapeType.Filled);

        // 약간 푸르스름한 어두운 색상으로 설정
        shape.setColor(new Color(0.7f, 0.7f, 0.75f, 0.33f));

        shape.ellipse(pos.x - 25, pos.y - 10, 50, 20);
        shape.end();

        batch.begin();
    }

    // 유닛의 실제 이미지를 화면에 그리는 로직
    private void drawUnitBody(Texture tex, Vector2 pos) {
        float targetWidth = 64f; // 기본 가로폭 고정
        float aspectRatio = (float) tex.getHeight() / tex.getWidth();
        float targetHeight = targetWidth * aspectRatio; // 비율에 맞춰 높이 자동 계산

        // 유닛의 발밑이 그리드 중앙에 오도록 x축은 절반만큼 왼쪽으로 밀어서 그림
        batch.draw(tex, pos.x - (targetWidth / 2f), pos.y, targetWidth, targetHeight);
    }

    // 체력바와 이름을 그리는 상단 UI 로직
    private void drawStatusUI(Unit unit, Vector2 pos, boolean isSelected) {
        boolean isAlly = unit.team.equals(playerTeam);
        // 아군은 초록색, 적군은 빨간색
        Color teamColor = isAlly ? Color.GREEN : Color.RED;

        float hpBarY = pos.y + 70f; // 유닛 머리 위 높이
        float hpBarWidth = 40f;
        float hpBarHeight = 5f;

        if (batch.isDrawing()) batch.end();

        shape.begin(ShapeRenderer.ShapeType.Filled);
        // 배경 검은색 바
        shape.setColor(Color.BLACK);
        shape.rect(pos.x - (hpBarWidth / 2f), hpBarY, hpBarWidth, hpBarHeight);

        // 현재 체력 비율 계산 (Record의 hp() 호출)
        float hpPercent = (float) unit.currentHp / unit.stat.hp();
        if (hpPercent > 0) {
            shape.setColor(teamColor);
            // 남은 체력만큼만 가로 길이를 채움
            shape.rect(pos.x - (hpBarWidth / 2f), hpBarY, hpBarWidth * hpPercent, hpBarHeight);
        }
        shape.end();

        batch.begin();

        // 선택된 유닛일 경우에만 이름을 표시
        if (isSelected) {
            font.setColor(Color.YELLOW);
            layout.setText(font, unit.name);
            // 텍스트 가로 길이를 계산하여 중앙 정렬
            font.draw(batch, unit.name, pos.x - (layout.width / 2f), hpBarY + 35f);
        }
    }

    public void dispose() {
        // 외부에서 받은 자원들이므로 직접 소멸시키지는 않으나 필요한 경우 추가 로직 작성 가능
    }
}
