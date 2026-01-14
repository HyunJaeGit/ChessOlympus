package com.hades.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.hades.game.constants.GameConfig; // [추가] 설정값 참조
import com.hades.game.screens.MenuScreen;
import com.hades.game.utils.FontFactory;

/**
 * [클래스 역할] 게임의 메인 진입점이며 공용 자원(폰트, 사운드)을 관리합니다.
 * 해상도 설정은 GameConfig를 참조하여 중앙 집중식으로 관리합니다.
 */
public class HadesGame extends Game {
    /* [수정] 직접 정의했던 VIRTUAL_WIDTH/HEIGHT 상수를 삭제했습니다.
       앞으로는 GameConfig.VIRTUAL_WIDTH 형식을 사용합니다. */

    public SpriteBatch batch;
    public BitmapFont font, mainFont, detailFont, titleFont, subtitleFont, unitFont, detailFont2;
    public Sound clickSound;

    @Override
    public void create() {
        batch = new SpriteBatch();

        /* [메서드 설명] FontFactory를 통해 각기 다른 스타일의 폰트 자원을 생성합니다. */
        titleFont = FontFactory.createFont("Galmuri14", 66, Color.GOLD, 6.0f, Color.BLACK, new Color(0,0,0,0.5f));
        subtitleFont = FontFactory.createFont("Galmuri14", 44, Color.LIGHT_GRAY, 4.0f);
        mainFont = FontFactory.createFont("Galmuri14", 36, Color.WHITE, 2.5f);
        detailFont = FontFactory.createFont("KERISBAEUM_L", 28, Color.WHITE, 2.0f);
        unitFont = FontFactory.createFont("KERISBAEUM_L", 22, Color.WHITE, 2.0f);
        detailFont2 = FontFactory.createFont("KERISBAEUM_L", 32, Color.WHITE, 2.0f);
        font = mainFont;

        String soundPath = "music/click.wav";
        if (Gdx.files.internal(soundPath).exists()) {
            try {
                clickSound = Gdx.audio.newSound(Gdx.files.internal(soundPath));
            } catch (Exception e) {
                Gdx.app.error("HadesGame", "Sound 로딩 실패: " + e.getMessage());
            }
        }

        this.setScreen(new MenuScreen(this));
    }

    /**
     * [메서드 설명] 효과음 재생을 처리하며, 재생 실패 시 시스템 오류를 방지합니다.
     * @param pitch 재생 속도 및 높낮이 조절
     */
    public void playClick(float pitch) {
        if (clickSound != null) {
            long id = clickSound.play(1.0f, pitch, 0);
            if (id == -1) {
                Gdx.app.error("SoundSystem", "재생 슬롯 부족");
            }
        }
    }

    public void playClick() {
        playClick(1.0f);
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (mainFont != null) mainFont.dispose();
        if (detailFont != null) detailFont.dispose();
        if (unitFont != null) unitFont.dispose();
        if (titleFont != null) titleFont.dispose();
        if (subtitleFont != null) subtitleFont.dispose();
        if (clickSound != null) clickSound.dispose();

        /* [설명] 현재 설정된 스크린의 자원도 함께 해제합니다. */
        if (getScreen() != null) getScreen().dispose();
    }
}
