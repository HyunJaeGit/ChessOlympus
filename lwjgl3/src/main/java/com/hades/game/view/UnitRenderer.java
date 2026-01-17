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
    private GlyphLayout layout;

    private static final float HITBOX_W = 30f;
    private static final float HITBOX_H = 50f;

    public UnitRenderer(SpriteBatch batch, ShapeRenderer shape, BitmapFont unitFont, String playerTeam) {
        this.batch = batch;
        this.shape = shape;
        this.font = unitFont;
        this.playerTeam = playerTeam;
        this.layout = new GlyphLayout();
    }

    /**
     * 유닛의 발밑 그림자와 선택 링을 렌더링합니다.
     */
    public void renderShadow(Unit unit, Unit selectedUnit) {
        Vector2 screenPos = IsoUtils.gridToScreen(unit.gridX, unit.gridY);
        boolean isSelected = (unit == selectedUnit);

        drawShadow(screenPos);

        if (isSelected) {
            drawSelectionRing(unit, screenPos, true);
        }
    }

    /**
     * 유닛의 본체, 애니메이션, 상태 UI, 데미지 팝업을 순서대로 렌더링합니다.
     */
    public void renderBody(Unit unit, Unit selectedUnit) {
        Vector2 screenPos = IsoUtils.gridToScreen(unit.gridX, unit.gridY);

        // 공격 애니메이션 등으로 발생하는 위치 오프셋을 적용합니다.
        float drawX = screenPos.x + unit.animOffset.x;
        float drawY = screenPos.y + unit.animOffset.y;

        Texture currentTexture = unit.fieldTexture;
        boolean isSelected = (unit == selectedUnit);

        // 피격 시 빨간색 깜빡임 연출 적용
        if (unit.hitTimer > 0) {
            batch.setColor(Color.RED);
        } else {
            batch.setColor(Color.WHITE);
        }

        drawUnitBody(currentTexture, drawX, drawY);
        batch.setColor(Color.WHITE); // 색상 원복

        // 체력바와 유닛 이름 표시
        drawStatusUI(unit, drawX, drawY, isSelected);

        // 데미지 팝업 텍스트 표시
        drawDamagePopups(unit, screenPos);
    }

    /**
     * [핵심 수정] 데미지 숫자를 화면에 그립니다.
     * libGDX Array의 중첩 반복 에러를 방지하기 위해 일반 for문을 사용합니다.
     */
    private void drawDamagePopups(Unit unit, Vector2 basePos) {
        // 향상된 for문(Iterator) 사용 시 네이티브 크래시 위험이 있어 인덱스 for문으로 교체
        for (int i = 0; i < unit.damageTexts.size; i++) {
            Unit.DamageText dt = unit.damageTexts.get(i);

            // 텍스트 색상 및 투명도 설정
            font.setColor(dt.color.r, dt.color.g, dt.color.b, dt.alpha);

            String displayTxt = "-" + dt.text;
            layout.setText(font, displayTxt);

            // 유닛 위치 기준으로 연출 좌표(dt.offsetPos)에 텍스트 렌더링
            font.draw(batch, displayTxt,
                basePos.x - (layout.width / 2f),
                basePos.y + dt.offsetPos.y);
        }
        // 폰트 설정 초기화
        font.setColor(Color.WHITE);
    }

    public boolean isMouseInsideHitbox(Unit unit, float mx, float my) {
        Vector2 pos = IsoUtils.gridToScreen(unit.gridX, unit.gridY);
        return mx >= pos.x - (HITBOX_W / 2) && mx <= pos.x + (HITBOX_W / 2) &&
            my >= pos.y && my <= pos.y + HITBOX_H;
    }

    private void drawSelectionRing(Unit unit, Vector2 pos, boolean isSelected) {
        if (batch.isDrawing()) batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(new Color(1, 1, 1, 0.2f));
        shape.ellipse(pos.x - 35, pos.y - 17, 70, 35);
        shape.setColor(new Color(1, 1, 1, 0.1f));
        shape.ellipse(pos.x - 45, pos.y - 22, 90, 45);
        shape.end();

        batch.begin();
    }

    private void drawShadow(Vector2 pos) {
        if (batch.isDrawing()) batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(new Color(0.7f, 0.7f, 0.75f, 0.33f));
        shape.ellipse(pos.x - 25, pos.y - 10, 50, 20);
        shape.end();

        batch.begin();
    }

    private void drawUnitBody(Texture tex, float x, float y) {
        float targetWidth = 64f;
        float aspectRatio = (float) tex.getHeight() / tex.getWidth();
        float targetHeight = targetWidth * aspectRatio;
        batch.draw(tex, x - (targetWidth / 2f), y, targetWidth, targetHeight);
    }

    private void drawStatusUI(Unit unit, float x, float y, boolean isSelected) {
        boolean isAlly = unit.team.equals(playerTeam);
        Color teamColor = isAlly ? Color.GREEN : Color.RED;

        float hpBarY = y + 70f;
        float hpBarWidth = 40f;
        float hpBarHeight = 5f;

        if (batch.isDrawing()) batch.end();

        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(Color.BLACK);
        shape.rect(x - (hpBarWidth / 2f), hpBarY, hpBarWidth, hpBarHeight);

        float hpPercent = (float) unit.currentHp / unit.stat.hp();
        if (hpPercent > 0) {
            shape.setColor(teamColor);
            shape.rect(x - (hpBarWidth / 2f), hpBarY, hpBarWidth * hpPercent, hpBarHeight);
        }
        shape.end();

        batch.begin();

        if (isSelected) {
            font.setColor(Color.YELLOW);
            layout.setText(font, unit.name);
            font.draw(batch, unit.name, x - (layout.width / 2f), hpBarY + 35f);
        }
    }

    public void dispose() { }
}
