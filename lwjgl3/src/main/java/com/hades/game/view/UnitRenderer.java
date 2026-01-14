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

// 클래스 역할: 유닛의 이미지, 체력바, 이름을 렌더링하며 마우스 오버 판정을 수행합니다.
public class UnitRenderer {

    private SpriteBatch batch;
    private ShapeRenderer shape;
    private BitmapFont font;
    private ObjectMap<String, Texture> unitTextures;
    private Texture defaultTexture;
    private String playerTeam;
    private GlyphLayout layout;

    // 히트박스 설정 상수: 타일 크기에 맞춰 클릭 영역을 지정합니다.
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

    // 메서드 설명: 유닛과 UI 요소들을 순서대로 그립니다.
    // 여기서 사용되는 mouseX, mouseY는 BattleScreen에서 이미 Unproject된 가상 좌표여야 합니다.
    public void render(Unit unit, Unit selectedUnit) {
        Vector2 screenPos = IsoUtils.gridToScreen(unit.gridX, unit.gridY);
        Texture currentTexture = getTextureForUnit(unit);

        // 중요: BattleScreen의 handleInput에서 계산된 hoveredGrid를 통해 오버 여부를 판단하는 것이 가장 정확하지만,
        // 여기서는 유닛의 개별 히트박스 범위를 기준으로 판정합니다.
        // 현재 BattleScreen의 render 루프 내 batch.setProjectionMatrix 덕분에 screenPos는 가상 해상도 기준입니다.

        boolean isSelected = (unit == selectedUnit);

        // 히트박스 판정용 마우스 좌표는 BattleScreen에서 stage.getViewport().unproject를 거친 값을 사용해야 정확합니다.
        // 여기서는 시각적 효과를 위해 unit의 상태를 BattleScreen으로부터 받아오는 구조가 더 안전합니다.
        // 일단 기존 로직의 좌표 불일치 문제를 해결하기 위해 렌더링에 집중합니다.

        drawShadow(screenPos);

        // 유닛 발밑 하이라이트
        if (isSelected) {
            drawSelectionRing(unit, screenPos, true);
        }

        drawUnitBody(currentTexture, screenPos);

        // 체력바 및 이름은 항상 유닛 머리 위에 고정
        drawStatusUI(unit, screenPos, isSelected);
    }

    // 메서드 설명: 가상 해상도 좌표(mx, my)를 받아 유닛의 클릭 영역 내에 있는지 판정합니다.
    public boolean isMouseInsideHitbox(Unit unit, float mx, float my) {
        Vector2 pos = IsoUtils.gridToScreen(unit.gridX, unit.gridY);
        // pos.x는 타일의 가로 중앙, pos.y는 타일의 바닥점입니다.
        return mx >= pos.x - (HITBOX_W / 2) && mx <= pos.x + (HITBOX_W / 2) &&
            my >= pos.y && my <= pos.y + HITBOX_H;
    }

    // 유닛 발밑에 팀 색상 링을 그립니다.
    private void drawSelectionRing(Unit unit, Vector2 pos, boolean isSelected) {
        batch.end();
        Gdx.gl.glLineWidth(2);
        shape.begin(ShapeRenderer.ShapeType.Line);
        Color highlightColor = unit.team.equals(playerTeam) ? Color.CYAN : Color.ORANGE;
        shape.setColor(highlightColor);
        // 타일의 2:1 비율에 맞춰 타원형으로 그림
        shape.ellipse(pos.x - 30, pos.y - 15, 60, 30);
        shape.end();
        batch.begin();
    }

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

    private void drawShadow(Vector2 pos) {
        batch.end();
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(new Color(0, 0, 0, 0.3f));
        shape.ellipse(pos.x - 25, pos.y - 10, 50, 20);
        shape.end();
        batch.begin();
    }

    private void drawUnitBody(Texture tex, Vector2 pos) {
        float targetWidth = 64f;
        float aspectRatio = (float) tex.getHeight() / tex.getWidth();
        float targetHeight = targetWidth * aspectRatio;
        // 유닛의 발이 타일 중앙 pos.y에 오도록 draw (pos.x는 중앙 정렬)
        batch.draw(tex, pos.x - (targetWidth / 2f), pos.y, targetWidth, targetHeight);
    }

    private void drawStatusUI(Unit unit, Vector2 pos, boolean isSelected) {
        boolean isAlly = unit.team.equals(playerTeam);
        Color teamColor = isAlly ? Color.GREEN : Color.RED;

        // 유닛 높이에 맞춰 체력바 위치 조정 (보통 유닛 이미지 위쪽)
        float hpBarY = pos.y + 70f;
        float hpBarWidth = 40f;
        float hpBarHeight = 5f;

        batch.end();
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

        // 선택되었을 때만 이름 표시 (또는 마우스 오버 시)
        if (isSelected) {
            font.setColor(Color.YELLOW);
            layout.setText(font, unit.name);
            font.draw(batch, unit.name, pos.x - (layout.width / 2f), hpBarY + 25f);
        }
    }

    public void dispose() {
        for (Texture tex : unitTextures.values()) {
            if (tex != null) tex.dispose();
        }
        unitTextures.clear();
        if (defaultTexture != null) defaultTexture.dispose();
    }
}
