package com.hades.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music; // [추가] 배경음악용
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.hades.game.screens.MenuScreen;
import com.hades.game.utils.FontFactory;

/**
 * [클래스 역할] 게임의 메인 진입점이며 공용 자원(폰트, 사운드, BGM)을 관리합니다.
 */
public class HadesGame extends Game {

    public SpriteBatch batch;
    public BitmapFont font, mainFont, detailFont, titleFont, subtitleFont, unitFont, detailFont2, unitFont2, unitFont3, cardFont;
    public Sound clickSound;

    // [추가] 배경음악 관리용 객체
    public Music menuBgm;   // 메뉴 화면 음악 (bgm.mp3)
    public Music battleBgm; // 전투 화면 음악 (bgm-battle.mp3)

    // 전역 볼륨 설정 (0.0 ~ 1.0)
    public float globalVolume = 0.1f; // 초기 음량 10% (원하는 대로 조절)

    private void loadBackgroundMusic() {
        if (Gdx.files.internal("music/bgm.mp3").exists()) {
            menuBgm = Gdx.audio.newMusic(Gdx.files.internal("music/bgm.mp3"));
            menuBgm.setLooping(true);
            // 전역 볼륨 적용
            menuBgm.setVolume(globalVolume);
        }

        if (Gdx.files.internal("music/bgm-battle.mp3").exists()) {
            battleBgm = Gdx.audio.newMusic(Gdx.files.internal("music/bgm-battle.mp3"));
            battleBgm.setLooping(true);
            // 전역 볼륨 적용
            battleBgm.setVolume(globalVolume);
        }
    }

    // 볼륨 일괄 업데이트 메서드
    public void updateVolume(float volume) {
        this.globalVolume = volume;
        if (menuBgm != null) menuBgm.setVolume(volume);
        if (battleBgm != null) battleBgm.setVolume(volume);
    }

    @Override
    public void create() {
        batch = new SpriteBatch();

        // 폰트 자원 생성
        titleFont = FontFactory.createFont("Galmuri14", 66, Color.GOLD, 6.0f, Color.BLACK, new Color(0,0,0,0.5f));
        subtitleFont = FontFactory.createFont("Galmuri14", 44, Color.LIGHT_GRAY, 4.0f);
        mainFont = FontFactory.createFont("Galmuri14", 36, Color.WHITE, 2.5f);
        detailFont = FontFactory.createFont("KERISBAEUM_L", 28, Color.WHITE, 2.0f);
        unitFont = FontFactory.createFont("KERISBAEUM_L", 20, Color.WHITE, 2.0f);
        unitFont2 = FontFactory.createFont("Galmuri14", 22, Color.WHITE, 2.0f);
        cardFont = FontFactory.createFont("KERISBAEUM_L", 16, Color.WHITE, 2.0f);
        unitFont3 = FontFactory.createFont("KERISBAEUM_L", 18, Color.WHITE, 2.0f);
        detailFont2 = FontFactory.createFont("KERISBAEUM_L", 32, Color.WHITE, 2.0f);
        font = mainFont;

        // 효과음 로드
        String soundPath = "music/click.wav";
        if (Gdx.files.internal(soundPath).exists()) {
            try {
                clickSound = Gdx.audio.newSound(Gdx.files.internal(soundPath));
            } catch (Exception e) {
                Gdx.app.error("HadesGame", "Sound 로딩 실패: " + e.getMessage());
            }
        }

        // 배경음악 로드 및 설정
        loadBackgroundMusic();

        // 초기 화면 설정 (MenuScreen에서 menuBgm 재생 시작 예정)
        this.setScreen(new MenuScreen(this));
    }

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
        // 폰트 및 그래픽 자원 해제
        if (batch != null) batch.dispose();
        if (mainFont != null) mainFont.dispose();
        if (detailFont != null) detailFont.dispose();
        if (unitFont != null) unitFont.dispose();
        if (titleFont != null) titleFont.dispose();
        if (subtitleFont != null) subtitleFont.dispose();

        // 사운드 및 음악 자원 해제
        if (clickSound != null) clickSound.dispose();
        if (menuBgm != null) {
            menuBgm.stop();
            menuBgm.dispose();
        }
        if (battleBgm != null) {
            battleBgm.stop();
            battleBgm.dispose();
        }

        if (getScreen() != null) getScreen().dispose();
    }
}
