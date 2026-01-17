package com.hades.game.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.Array;
import com.hades.game.HadesGame;
import com.hades.game.screens.BattleScreen;
import com.hades.game.entities.Unit;
import com.hades.game.constants.UnitData;

// 전투 중 치트 기능을 중앙에서 관리하는 클래스입니다.
public class DebugManager {

    // 전투 중 치트키 입력 처리
    public static void handleBattleDebug(HadesGame game, Array<Unit> units, String aiTeam, java.util.function.Consumer<Unit> deathHandler) {

        // K 키: 적 보스 즉시 처치
        if (Gdx.input.isKeyJustPressed(Input.Keys.K)) {
            for (Unit u : units) {
                // 적 팀(AI)의 영웅 유닛(보스)을 찾아 체력을 0으로 만듭니다.
                if (u.team.equals(aiTeam) && u.unitClass == Unit.UnitClass.HERO) {
                    u.currentHp = 0;
                    // BattleScreen의 handleDeath 로직을 실행하여 승리/엔딩 컷씬 흐름을 태웁니다.
                    deathHandler.accept(u);
                    break;
                }
            }
        }

        // L 키: 즉시 7단계(제우스 전)로 돌입
        if (Gdx.input.isKeyJustPressed(Input.Keys.L)) {
            game.playClick();
            // 현재 배틀 BGM이 있다면 정지
            if (game.currentBgm != null) {
                game.currentBgm.stop();
                game.currentBgm.dispose();
                game.currentBgm = null;
            }

            // 7스테이지로 즉시 이동 (기본 영웅 데이터는 솜주먹 사용)
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
