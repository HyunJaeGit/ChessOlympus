package com.hades.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.hades.game.constants.RunState;
import com.hades.game.screens.MenuScreen;
import com.hades.game.utils.FontFactory;
import com.hades.game.utils.AudioManager; // 추가

// Chess Olympus: HADES vs ZEUS - 메인 게임 클래스
public class HadesGame extends Game {

    public SpriteBatch batch;
    public BitmapFont font, mainFont, detailFont, titleFont, subtitleFont,
        unitFont, detailFont2, unitFont2, unitFont3, cardFont, battleFont;

    public Sound clickSound;
    public float globalVolume = 0.1f;

    // 오디오 전담 관리 매니저
    public AudioManager audioManager;

    // 게임 진행 상태 데이터
    public RunState runState = new RunState();

    @Override
    public void create() {
        batch = new SpriteBatch();

        // 폰트 초기화
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

        // 클릭 효과음 로드
        String soundPath = "music/click.ogg";
        if (Gdx.files.internal(soundPath).exists()) {
            try {
                clickSound = Gdx.audio.newSound(Gdx.files.internal(soundPath));
            } catch (Exception e) {
                Gdx.app.error("HadesGame", "Sound load error: " + e.getMessage());
            }
        }

        // 오디오 매니저 초기화
        audioManager = new AudioManager(this);

        // 첫 화면 설정
        this.setScreen(new MenuScreen(this));
    }

    // 볼륨 설정 변경 시 호출되는 메서드
    public void updateVolume(float volume) {
        this.globalVolume = volume;
        if (audioManager != null) {
            audioManager.updateVolume(volume);
        }
    }

    // 클릭 효과음 재생 (피치 조절 가능)
    public void playClick(float pitch) {
        if (clickSound != null) {
            clickSound.play(globalVolume, pitch, 0);
        }
    }

    // 클릭 효과음 기본 재생
    public void playClick() {
        playClick(1.0f);
    }

    // 저장된 게임 데이터 로드
    public void loadGame() {
        com.badlogic.gdx.files.FileHandle file = Gdx.files.local("save/run_data.json");
        if (file.exists()) {
            com.badlogic.gdx.utils.Json json = new com.badlogic.gdx.utils.Json();
            this.runState = json.fromJson(RunState.class, file.readString());
            Gdx.app.log("SAVE_SYSTEM", "불러오기 완료");
        }
    }

    // 현재 게임 데이터 저장
    public void saveGame() {
        com.badlogic.gdx.utils.Json json = new com.badlogic.gdx.utils.Json();
        Gdx.files.local("save/run_data.json").writeString(json.prettyPrint(runState), false);
        Gdx.app.log("SAVE_SYSTEM", "저장 완료");
    }

    @Override
    public void dispose() {
        // 그래픽 및 폰트 자원 해제
        if (batch != null) batch.dispose();
        if (titleFont != null) titleFont.dispose();
        if (subtitleFont != null) subtitleFont.dispose();
        if (mainFont != null) mainFont.dispose();
        if (detailFont != null) detailFont.dispose();
        if (detailFont2 != null) detailFont2.dispose();
        if (unitFont != null) unitFont.dispose();
        if (unitFont2 != null) unitFont2.dispose();
        if (unitFont3 != null) unitFont3.dispose();
        if (cardFont != null) cardFont.dispose();
        if (battleFont != null) battleFont.dispose();

        // 사운드 자원 해제
        if (clickSound != null) clickSound.dispose();
        if (audioManager != null) audioManager.stopBgm();

        // 현재 스크린 해제
        if (getScreen() != null) getScreen().dispose();
    }
}
