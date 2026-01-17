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
// 리팩터링: Unit의 animOffset과 hitTimer를 사용하여 공격/피격 연출을 표현합니다.
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

    public void renderShadow(Unit unit, Unit selectedUnit) {
        Vector2 screenPos = IsoUtils.gridToScreen(unit.gridX, unit.gridY);
        boolean isSelected = (unit == selectedUnit);

        drawShadow(screenPos);

        if (isSelected) {
            drawSelectionRing(unit, screenPos, true);
        }
    }

    public void renderBody(Unit unit, Unit selectedUnit) {
        Vector2 screenPos = IsoUtils.gridToScreen(unit.gridX, unit.gridY);

        // 애니메이션 오프셋 적용
        float drawX = screenPos.x + unit.animOffset.x;
        float drawY = screenPos.y + unit.animOffset.y;

        Texture currentTexture = unit.fieldTexture;
        boolean isSelected = (unit == selectedUnit);

        // 피격 시 빨간색 깜빡임 적용
        if (unit.hitTimer > 0) {
            batch.setColor(Color.RED);
        } else {
            batch.setColor(Color.WHITE);
        }

        drawUnitBody(currentTexture, drawX, drawY);
        batch.setColor(Color.WHITE);

        // 체력바와 이름 출력
        drawStatusUI(unit, drawX, drawY, isSelected);

        // 데미지 팝업 텍스트 출력
        drawDamagePopups(unit, screenPos);
    }

    // 데미지 숫자를 화면에 그리는 로직
    private void drawDamagePopups(Unit unit, Vector2 basePos) {
        for (Unit.DamageText dt : unit.damageTexts) {
            // 텍스트 색상에 투명도(alpha) 적용
            font.setColor(dt.color.r, dt.color.g, dt.color.b, dt.alpha);

            // 데미지 수치 앞에 '-' 기호 추가하여 출력
            String displayTxt = "-" + dt.text;
            layout.setText(font, displayTxt);

            // 유닛의 원래 위치(basePos) 기준, dt가 가진 오프셋만큼 떨어진 곳에 그림
            font.draw(batch, displayTxt,
                basePos.x - (layout.width / 2f),
                basePos.y + dt.offsetPos.y);
        }
        // 폰트 색상 원복 (다른 UI 영향 방지)
        font.setColor(Color.WHITE);
    }

    public boolean isMouseInsideHitbox(Unit unit, float mx, float my) {
        Vector2 pos = IsoUtils.gridToScreen(unit.gridX, unit.gridY);
        // 클릭 판정은 애니메이션과 상관없이 고정된 그리드 위치를 기준으로 함
        return mx >= pos.x - (HITBOX_W / 2) && mx <= pos.x + (HITBOX_W / 2) &&
            my >= pos.y && my <= pos.y + HITBOX_H;
    }

    private void drawSelectionRing(Unit unit, Vector2 pos, boolean isSelected) {
        if (batch.isDrawing()) batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shape.begin(ShapeRenderer.ShapeType.Filled);

        Color glowColor = new Color(1, 1, 1, 0.2f);
        shape.setColor(glowColor);

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

    // 매개변수를 Vector2 대신 직접 x, y로 받도록 변경 (오프셋 적용 위함)
    private void drawUnitBody(Texture tex, float x, float y) {
        float targetWidth = 64f;
        float aspectRatio = (float) tex.getHeight() / tex.getWidth();
        float targetHeight = targetWidth * aspectRatio;

        batch.draw(tex, x - (targetWidth / 2f), y, targetWidth, targetHeight);
    }

    // 매개변수를 Vector2 대신 직접 x, y로 받도록 변경
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

        // 선택했을 때 유닛 이름을 출력
        if (isSelected) {
            font.setColor(Color.YELLOW);
            layout.setText(font, unit.name);
            font.draw(batch, unit.name, x - (layout.width / 2f), hpBarY + 35f);
        }
    }

    public void dispose() { }
}
