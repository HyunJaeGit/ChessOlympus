package com.hades.game.view;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ObjectMap;
import com.hades.game.entities.Unit;
import com.hades.game.logic.IsoUtils;

// [클래스 역할] 유닛의 이름과 매칭되는 이미지를 로드하고, 체력바(상단) 및 이름표(하단)를 포함한 유닛의 모든 외형을 그립니다.
public class UnitRenderer {

    private SpriteBatch batch;
    private ShapeRenderer shape;
    private BitmapFont font;
    private ObjectMap<String, Texture> unitTextures;
    private Texture defaultTexture;
    private String playerTeam; // 아군 팀 이름을 저장

    public UnitRenderer(SpriteBatch batch, ShapeRenderer shape, BitmapFont font, String playerTeam) {
        this.batch = batch;
        this.shape = shape;
        this.font = font;
        this.unitTextures = new ObjectMap<>();
        this.defaultTexture = new Texture("libgdx.png");
        this.playerTeam = playerTeam; // 초기화 시 아군 팀 설정
    }

    // 유닛의 시각적 요소(그림자 -> 본체 -> 상태 UI)를 순서대로 그립니다.
    public void render(Unit unit) {
        Vector2 screenPos = IsoUtils.gridToScreen(unit.gridX, unit.gridY);
        Texture currentTexture = getTextureForUnit(unit);

        drawShadow(screenPos);
        drawUnitBody(currentTexture, screenPos);
        drawStatusUI(unit, screenPos);
    }

    // 유닛 이름을 기반으로 images/units/ 폴더에서 텍스처를 찾아 반환합니다.
    private Texture getTextureForUnit(Unit unit) {
        String fileName = "images/units/" + unit.name + ".png";

        if (!unitTextures.containsKey(unit.name)) {
            try {
                Texture tex = new Texture(fileName);
                tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                unitTextures.put(unit.name, tex);
            } catch (Exception e) {
                System.err.println("[Asset Error] 파일을 찾을 수 없음: " + fileName);
                return defaultTexture;
            }
        }
        return unitTextures.get(unit.name);
    }

    // 유닛 발밑에 타원형 그림자를 그립니다.
    private void drawShadow(Vector2 pos) {
        batch.end();
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(new Color(0, 0, 0, 0.3f));
        shape.ellipse(pos.x - 25, pos.y - 8, 50, 16);
        shape.end();
        batch.begin();
    }

    // 유닛 이미지를 중앙 정렬하여 렌더링합니다.
    private void drawUnitBody(Texture tex, Vector2 pos) {
        float targetWidth = 50f;
        float aspectRatio = (float) tex.getHeight() / tex.getWidth();
        float targetHeight = targetWidth * aspectRatio;

        batch.draw(tex, pos.x - (targetWidth / 2f), pos.y, targetWidth, targetHeight);
    }

    // 머리 위에는 체력바를, 발밑에는 이름을 팀 색상에 맞춰 표시합니다.
    // 아군이면 녹색, 적군이면 적색으로 체력바와 이름을 그립니다.
    private void drawStatusUI(Unit unit, Vector2 pos) {
        boolean isAlly = unit.team.equals(playerTeam);
        Color teamColor = isAlly ? Color.GREEN : Color.RED;

        float hpBarWidth = 40f;
        float hpBarHeight = 5f;
        float hpBarYOffset = 70f;

        batch.end();
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(Color.DARK_GRAY);
        shape.rect(pos.x - (hpBarWidth / 2f), pos.y + hpBarYOffset, hpBarWidth, hpBarHeight);

        float hpPercent = (float) unit.currentHp / unit.stat.hp();
        if (hpPercent > 0) {
            shape.setColor(teamColor);
            shape.rect(pos.x - (hpBarWidth / 2f), pos.y + hpBarYOffset, hpBarWidth * hpPercent, hpBarHeight);
        }
        shape.end();
        batch.begin();

        font.getData().setScale(0.5f);
        float nameYOffset = -15f;
        font.setColor(teamColor);
        font.draw(batch, unit.name, pos.x - 15f, pos.y + nameYOffset);
        font.getData().setScale(1.0f);
    }

    // [메서드 설명] 로드된 모든 텍스처 자원을 해제하여 메모리 누수를 방지합니다.
    public void dispose() {
        for (Texture tex : unitTextures.values()) {
            if (tex != null) tex.dispose();
        }
        unitTextures.clear();
        if (defaultTexture != null) defaultTexture.dispose();
    }
}
