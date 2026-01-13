package com.hades.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.hades.game.screens.MenuScreen;
import com.hades.game.utils.FontFactory;

/**
 * [클래스 역할] 게임의 메인 진입점이며 공용 자원(폰트, 배치)을 관리합니다.
 */
public class HadesGame extends Game {
    public SpriteBatch batch;
    public BitmapFont font;       // (기본 폰트)
    public BitmapFont mainFont;   // 갈무리
    public BitmapFont detailFont;
    public BitmapFont titleFont;
    public BitmapFont subtitleFont;

    @Override
    public void create() {
        batch = new SpriteBatch();

        /* [메서드 설명] FontFactory를 사용하여 필요한 모든 폰트를 안전하게 생성합니다. */
        // 메인 폰트 (갈무리)
        mainFont = FontFactory.createFont("Galmuri14", 18, Color.WHITE, 0);
        // 상세 폰트 (맑은 고딕 - 없으면 갈무리로 대체됨)
        detailFont = FontFactory.createFont("malgun", 14, Color.WHITE, 0);
        // 기존 코드와의 호환성을 위해 font 변수에도 할당
        font = mainFont;
        // 홈 타이틀 폰트
        titleFont = FontFactory.createFont("Galmuri14", 30, Color.GOLD, 2);
        // 홈 부제목 타이틀 폰트
        subtitleFont = FontFactory.createFont("Galmuri14", 22, Color.LIGHT_GRAY, 2);
        this.setScreen(new MenuScreen(this));
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (mainFont != null) mainFont.dispose();
        if (detailFont != null) detailFont.dispose();
        if (getScreen() != null) getScreen().dispose();
    }
}
