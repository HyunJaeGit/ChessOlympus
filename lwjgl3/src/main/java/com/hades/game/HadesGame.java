package com.hades.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.hades.game.constants.RunState; // 외부 패키지의 RunState를 임포트
import com.hades.game.constants.UnitData;
import com.hades.game.screens.MenuScreen;
import com.hades.game.utils.FontFactory;

public class HadesGame extends Game {

    public SpriteBatch batch;
    public BitmapFont font, mainFont, detailFont, titleFont, subtitleFont,
        unitFont, detailFont2, unitFont2, unitFont3, cardFont, battleFont;

    public Sound clickSound;
    public Music menuBgm;
    public Music battleBgm;
    public Music currentBgm;

    public float globalVolume = 0.1f;

    // [수정] 내부 클래스 RunState 정의를 삭제하고 외부 클래스를 사용합니다.
    public RunState runState = new RunState();

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
            } catch (Exception e) {
                Gdx.app.error("HadesGame", "Sound load error: " + e.getMessage());
            }
        }

        loadBackgroundMusic();
        this.setScreen(new MenuScreen(this));
    }

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

    public void updateVolume(float volume) {
        this.globalVolume = volume;
        if (menuBgm != null) menuBgm.setVolume(volume);
        if (battleBgm != null) battleBgm.setVolume(volume);
        if (currentBgm != null) currentBgm.setVolume(volume);
    }

    public void playClick(float pitch) {
        if (clickSound != null) clickSound.play(globalVolume, pitch, 0);
    }

    public void playClick() {
        playClick(1.0f);
    }

    // [수정] 외부 RunState 클래스 타입으로 로드합니다.
    public void loadGame() {
        com.badlogic.gdx.files.FileHandle file = Gdx.files.local("save/run_data.json");
        if (file.exists()) {
            com.badlogic.gdx.utils.Json json = new com.badlogic.gdx.utils.Json();
            this.runState = json.fromJson(RunState.class, file.readString());
            Gdx.app.log("SAVE_SYSTEM", "불러오기 완료");
        }
    }

    public void saveGame() {
        com.badlogic.gdx.utils.Json json = new com.badlogic.gdx.utils.Json();
        Gdx.files.local("save/run_data.json").writeString(json.prettyPrint(runState), false);
        Gdx.app.log("SAVE_SYSTEM", "저장 완료");
    }

    @Override
    public void dispose() {
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

        if (clickSound != null) clickSound.dispose();
        if (menuBgm != null) menuBgm.dispose();
        if (battleBgm != null) battleBgm.dispose();
        if (currentBgm != null) currentBgm.dispose();

        if (getScreen() != null) getScreen().dispose();
    }
}
