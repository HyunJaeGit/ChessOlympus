package com.hades.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.hades.game.screens.MenuScreen;
import com.hades.game.utils.FontFactory;

// 게임의 메인 진입점이며 공용 자원(폰트, 사운드, BGM)을 관리합니다.
public class HadesGame extends Game {

    public SpriteBatch batch;
    public BitmapFont font, mainFont, detailFont, titleFont, subtitleFont,
        unitFont, detailFont2, unitFont2, unitFont3, cardFont, battleFont;
    public Sound clickSound;
    // 배경음악 관리용 객체
    public Music menuBgm;
    public Music battleBgm;
    // 스테이지별 동적 배경음악을 관리하기 위한 변수
    public Music currentBgm;

    public int soulFragments = 0; // 보유한 영혼 파편
    public int olympusSeals = 0;  // 보유한 올림포스 인장

    // 전역 볼륨 설정 (0.0 ~ 1.0)
    public float globalVolume = 0.1f;

    private void loadBackgroundMusic() {
        if (Gdx.files.internal("music/bgm.mp3").exists()) {
            menuBgm = Gdx.audio.newMusic(Gdx.files.internal("music/bgm.mp3"));
            menuBgm.setLooping(true);
            menuBgm.setVolume(globalVolume);
        }

        if (Gdx.files.internal("music/bgm-battle.mp3").exists()) {
            battleBgm = Gdx.audio.newMusic(Gdx.files.internal("music/bgm-battle.mp3"));
            battleBgm.setLooping(true);
            battleBgm.setVolume(globalVolume);
        }
    }

    // 볼륨 일괄 업데이트 메서드
    public void updateVolume(float volume) {
        this.globalVolume = volume;
        if (menuBgm != null) menuBgm.setVolume(volume);
        if (battleBgm != null) battleBgm.setVolume(volume);
        // 현재 재생 중인 스테이지 음악에도 볼륨 적용
        if (currentBgm != null) currentBgm.setVolume(volume);
    }

    @Override
    public void create() {
        batch = new SpriteBatch();

        titleFont = FontFactory.createFont("Galmuri14", 66, Color.GOLD, 6.0f, Color.BLACK, new Color(0,0,0,0.5f));
        subtitleFont = FontFactory.createFont("Galmuri14", 44, Color.LIGHT_GRAY, 4.0f);
        mainFont = FontFactory.createFont("Galmuri14", 36, Color.WHITE, 2.5f);
        detailFont = FontFactory.createFont("Galmuri14", 24, Color.WHITE, 2.0f);
        detailFont2 = FontFactory.createFont("Galmuri14", 32, Color.WHITE, 2.0f);
        unitFont = FontFactory.createFont("KERISBAEUM_B", 20, Color.WHITE, 2.0f);
        unitFont2 = FontFactory.createFont("Galmuri14", 22, Color.WHITE, 2.0f);
        unitFont3 = FontFactory.createFont("Galmuri14", 16, Color.WHITE, 2.0f);
        cardFont = FontFactory.createFont("Galmuri14", 14, Color.WHITE, 2.0f);
        battleFont = FontFactory.createFont("KERISBAEUM_B", 14, Color.WHITE, 2.0f);
        font = mainFont;

        String soundPath = "music/click.ogg";
        if (Gdx.files.internal(soundPath).exists()) {
            try {
                clickSound = Gdx.audio.newSound(Gdx.files.internal(soundPath));
                Gdx.app.log("HadesGame", "Sound loaded: " + soundPath);
            } catch (Exception e) {
                Gdx.app.error("HadesGame", "Critical: Failed to load sound - " + e.getMessage());
            }
        }

        loadBackgroundMusic();
        this.setScreen(new MenuScreen(this));
    }

    public void playClick(float pitch) {
        if (clickSound != null) {
            clickSound.play(globalVolume, pitch, 0);
        }
    }

    public void playClick() {
        playClick(1.0f);
    }

    @Override
    public void dispose() {
        if (titleFont != null) titleFont.dispose();
        if (subtitleFont != null) subtitleFont.dispose();
        if (mainFont != null) mainFont.dispose();
        if (detailFont != null) detailFont.dispose();
        if (detailFont2 != null) detailFont2.dispose();
        if (unitFont != null) unitFont.dispose();
        if (unitFont2 != null) unitFont2.dispose();
        if (unitFont3 != null) unitFont3.dispose();
        if (cardFont != null) cardFont.dispose();

        if (clickSound != null) clickSound.dispose();
        if (menuBgm != null) {
            menuBgm.stop();
            menuBgm.dispose();
        }
        if (battleBgm != null) {
            battleBgm.stop();
            battleBgm.dispose();
        }
        // 동적 배경음악 자원 해제
        if (currentBgm != null) {
            currentBgm.stop();
            currentBgm.dispose();
        }

        if (getScreen() != null) getScreen().dispose();
    }
}
