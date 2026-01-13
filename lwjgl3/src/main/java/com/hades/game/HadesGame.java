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

        // 1. 홈 타이틀: 가장 크고 화려하게 (금색 + 검정 테두리 3px + 그림자)
        titleFont = FontFactory.createFont("Galmuri14", 36, Color.GOLD, 3.0f);
        // 2. 홈 부제목: 차분하지만 선명하게 (밝은 회색 + 테두리 2px)
        subtitleFont = FontFactory.createFont("Galmuri14", 22, Color.LIGHT_GRAY, 2.0f);
        // 3. 메인 메뉴 버튼: 클릭 가독성을 위해 테두리 추가 (흰색 + 테두리 1.5px)
        mainFont = FontFactory.createFont("Galmuri14", 18, Color.WHITE, 1.5f);
        // 4. 상세 정보/저작권: 작은 글씨도 배경에 묻히지 않게 (회색/흰색 + 테두리 1px)
        detailFont = FontFactory.createFont("malgun", 14, Color.WHITE, 1.0f);
        // 기존 코드 호환용 할당
        font = mainFont;
        /* [설명] 모든 자원이 준비되면 메뉴 화면으로 전환합니다. */
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
