package com.hades.game.gwt;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.hades.game.HadesGame;

/** Launches the GWT application. */
public class GwtLauncher extends GwtApplication {
    @Override
    public GwtApplicationConfiguration getConfig () {
        // 고정된 가상 해상도 값을 사용하거나 비율을 유지하는 설정이 안정적입니다.
        GwtApplicationConfiguration cfg = new GwtApplicationConfiguration(1280, 720); // 예시 해상도
        cfg.antialiasing = true; // 웹 환경에서 계단 현상 방지
        return cfg;
    }

        @Override
        public ApplicationListener createApplicationListener () {
            return new HadesGame();
        }
}
