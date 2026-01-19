package com.hades.game.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.Array;
import com.hades.game.HadesGame;
import com.hades.game.screens.BattleScreen;
import com.hades.game.entities.Unit;
import com.hades.game.constants.UnitData;

// Chess Olympus: HADES vs ZEUS - 전투 치트 관리
public class DebugManager {

    public static void handleBattleDebug(HadesGame game, Array<Unit> units, String aiTeam, java.util.function.Consumer<Unit> deathHandler) {

        // K 키: 적 보스 즉시 처치
        if (Gdx.input.isKeyJustPressed(Input.Keys.K)) {
            for (Unit u : units) {
                if (u.team.equals(aiTeam) && u.unitClass == Unit.UnitClass.HERO) {
                    u.currentHp = 0;
                    deathHandler.accept(u);
                    break;
                }
            }
        }

        // L 키: 즉시 7단계(제우스 전) 돌입
        if (Gdx.input.isKeyJustPressed(Input.Keys.L)) {
            game.playClick();

            // [수정] AudioManager를 통해 배경음악 정지
            game.audioManager.stopBgm();

            game.setScreen(new BattleScreen(
                game,
                "HADES",
                "솜주먹",
                UnitData.SOM_JUMEOK,
                7
            ));
        }
    }
}
