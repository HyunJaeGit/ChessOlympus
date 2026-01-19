package com.hades.game.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.hades.game.HadesGame;

public class AudioManager {
    private final HadesGame game;
    private Music currentMusic;
    private String currentMusicPath;

    public AudioManager(HadesGame game) {
        this.game = game;
    }

    // 배경음악 재생 (실무에서는 경로를 넘겨 제어합니다)
    public void playBgm(String path) {
        // 1. 이미 재생 중인 곡이면 중복 재생 방지
        if (currentMusicPath != null && currentMusicPath.equals(path)) {
            if (currentMusic != null && !currentMusic.isPlaying()) {
                currentMusic.play();
            }
            return;
        }

        // 2. 기존 음악 정리
        stopBgm();

        // 3. 새 음악 로드 및 재생
        try {
            if (Gdx.files.internal(path).exists()) {
                currentMusic = Gdx.audio.newMusic(Gdx.files.internal(path));
                currentMusic.setLooping(true);
                currentMusic.setVolume(game.globalVolume);
                currentMusic.play();
                currentMusicPath = path;
            }
        } catch (Exception e) {
            Gdx.app.error("AudioManager", "BGM 로드 실패: " + path);
        }
    }

    public void stopBgm() {
        if (currentMusic != null) {
            currentMusic.stop();
            currentMusic.dispose();
            currentMusic = null;
            currentMusicPath = null;
        }
    }

    public void updateVolume(float volume) {
        if (currentMusic != null) {
            currentMusic.setVolume(volume);
        }
    }

    // 효과음 재생 (간단한 SFX용)
    public void playSfx(String path) {
        try {
            Sound sfx = Gdx.audio.newSound(Gdx.files.internal(path));
            sfx.play(game.globalVolume);
            // 실무에서는 Sound도 캐싱(Caching)하여 재사용하지만, 여기서는 기본 흐름만 보여줍니다.
        } catch (Exception e) {
            Gdx.app.error("AudioManager", "SFX 로드 실패: " + path);
        }
    }
}
