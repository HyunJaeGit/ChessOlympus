package com.hades.game.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ObjectMap;
import com.hades.game.entities.Unit;
import com.hades.game.logic.IsoUtils;

/* 유닛 이미지, 그림자, 선택 링, 체력바 및 이름 렌더링 담당 */
public class UnitRenderer {

    private SpriteBatch batch;
    private ShapeRenderer shape;
    private BitmapFont font;
    private ObjectMap<String, Texture> unitTextures;
    private Texture defaultTexture;
    private String playerTeam;
    private GlyphLayout layout;

    // 타일 크기 대비 클릭 감지 영역 설정
    private static final float HITBOX_W = 30f;
    private static final float HITBOX_H = 50f;

    public UnitRenderer(SpriteBatch batch, ShapeRenderer shape, BitmapFont unitFont, String playerTeam) {
        this.batch = batch;
        this.shape = shape;
        this.font = unitFont;
        this.unitTextures = new ObjectMap<>();
        this.defaultTexture = new Texture("libgdx.png");
        this.playerTeam = playerTeam;
        this.layout = new GlyphLayout();
    }

    /* 1단계: 유닛 발밑 그림자 및 선택 표시링 출력 */
    public void renderShadow(Unit unit, Unit selectedUnit) {
        Vector2 screenPos = IsoUtils.gridToScreen(unit.gridX, unit.gridY);
        boolean isSelected = (unit == selectedUnit);

        drawShadow(screenPos);

        if (isSelected) {
            drawSelectionRing(unit, screenPos, true);
        }
    }

    /* 2단계: 유닛 본체 이미지 및 상단 상태 UI 출력 */
    public void renderBody(Unit unit, Unit selectedUnit) {
        Vector2 screenPos = IsoUtils.gridToScreen(unit.gridX, unit.gridY);
        Texture currentTexture = getTextureForUnit(unit);
        boolean isSelected = (unit == selectedUnit);

        drawUnitBody(currentTexture, screenPos);
        drawStatusUI(unit, screenPos, isSelected);
    }

    /* 마우스 좌표의 유닛 충돌 범위(Hitbox) 포함 여부 판정 */
    public boolean isMouseInsideHitbox(Unit unit, float mx, float my) {
        Vector2 pos = IsoUtils.gridToScreen(unit.gridX, unit.gridY);
        return mx >= pos.x - (HITBOX_W / 2) && mx <= pos.x + (HITBOX_W / 2) &&
            my >= pos.y && my <= pos.y + HITBOX_H;
    }

    // 유닛 선택 시 팀별 색상(앤티크 골드 / 다크 레드) 링 출력
    private void drawSelectionRing(Unit unit, Vector2 pos, boolean isSelected) {
        if (batch.isDrawing()) batch.end();

        Gdx.gl.glLineWidth(2);
        shape.begin(ShapeRenderer.ShapeType.Line);

        // 아군: 앤티크 골드, 적군: 깊은 와인색
        Color allyColor = Color.valueOf("D4AF37");
        Color enemyColor = Color.valueOf("800000");

        Color highlightColor = unit.team.equals(playerTeam) ? allyColor : enemyColor;
        shape.setColor(highlightColor);
        shape.ellipse(pos.x - 30, pos.y - 15, 60, 30);
        shape.end();

        batch.begin();
    }

    // 유닛명 기준 텍스트 로드 및 캐싱 처리
    private Texture getTextureForUnit(Unit unit) {
        String imgKey = unit.stat.imageName();
        String fileName = "images/units/" + imgKey + ".png";

        if (!unitTextures.containsKey(imgKey)) {
            try {
                Texture tex = new Texture(fileName);
                tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                unitTextures.put(imgKey, tex);
            } catch (Exception e) {
                return defaultTexture;
            }
        }
        return unitTextures.get(imgKey);
    }

    // 발밑 반투명 검은색 타원형 그림자 출력
    private void drawShadow(Vector2 pos) {
        if (batch.isDrawing()) batch.end();

        shape.begin(ShapeRenderer.ShapeType.Filled);

        // 단순 검정(0,0,0) 대신 배경과 어우러지는 짙은 네이비/퍼플 톤 그림자
        // 투명도를 0.3f -> 0.45f로 살짝 높여 무게감을 줌
        shape.setColor(new Color(0.05f, 0.05f, 0.12f, 0.15f));

        shape.ellipse(pos.x - 25, pos.y - 10, 50, 20);
        shape.end();

        batch.begin();
    }

    // 유닛 텍스처 출력 및 위치 보정
    private void drawUnitBody(Texture tex, Vector2 pos) {
        float targetWidth = 64f;
        float aspectRatio = (float) tex.getHeight() / tex.getWidth();
        float targetHeight = targetWidth * aspectRatio;

        batch.draw(tex, pos.x - (targetWidth / 2f), pos.y, targetWidth, targetHeight);
    }

    // 체력바 및 선택 유닛 이름 출력
    private void drawStatusUI(Unit unit, Vector2 pos, boolean isSelected) {
        boolean isAlly = unit.team.equals(playerTeam);
        Color teamColor = isAlly ? Color.GREEN : Color.RED;

        float hpBarY = pos.y + 70f;
        float hpBarWidth = 40f;
        float hpBarHeight = 5f;

        if (batch.isDrawing()) batch.end();

        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(Color.BLACK);
        shape.rect(pos.x - (hpBarWidth / 2f), hpBarY, hpBarWidth, hpBarHeight);

        float hpPercent = (float) unit.currentHp / unit.stat.hp();
        if (hpPercent > 0) {
            shape.setColor(teamColor);
            shape.rect(pos.x - (hpBarWidth / 2f), hpBarY, hpBarWidth * hpPercent, hpBarHeight);
        }
        shape.end();

        batch.begin();

        if (isSelected) {
            font.setColor(Color.YELLOW);
            layout.setText(font, unit.name);
            font.draw(batch, unit.name, pos.x - (layout.width / 2f), hpBarY + 25f);
        }
    }

    // 로드된 텍스처 자원 해제
    public void dispose() {
        for (Texture tex : unitTextures.values()) {
            if (tex != null) tex.dispose();
        }
        unitTextures.clear();
        if (defaultTexture != null) defaultTexture.dispose();
    }
}
