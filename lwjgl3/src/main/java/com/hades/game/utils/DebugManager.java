package com.hades.game.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.Array;
import com.hades.game.HadesGame;
import com.hades.game.screens.BattleScreen;
import com.hades.game.entities.Unit;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.hades.game.constants.UnitData;

// Chess Olympus: HADES vs ZEUS - 전투 치트 관리 (조합 단축키 시스템)
public class DebugManager {

    public static void handleBattleDebug(HadesGame game, Array<Unit> units, String aiTeam, java.util.function.Consumer<Unit> deathHandler) {

        // 1. 조합키를 위한 'Control' 또는 'Shift' 상태 확인
        boolean isControlPressed = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT);
        boolean isShiftPressed = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);

        // [치트 1] Ctrl + K : 적 보스 즉시 처치 (승리 테스트용)
        if (isControlPressed && Gdx.input.isKeyJustPressed(Input.Keys.K)) {
            System.out.println("[CHEAT] Instant Win Activated");
            for (int i = 0; i < units.size; i++) {
                Unit u = units.get(i);
                if (u != null && u.isAlive() && u.team.equals(aiTeam) && u.unitClass == Unit.UnitClass.HERO) {
                    u.currentHp = 0;
                    deathHandler.accept(u);
                    break;
                }
            }
        }

        // [치트 2] Ctrl + Shift + L : 즉시 최종 7단계 돌입 (보스전 테스트용)
        // 세 개의 키를 조합하여 더 안전하게 보호합니다.
        if (isControlPressed && isShiftPressed && Gdx.input.isKeyJustPressed(Input.Keys.L)) {
            System.out.println("[CHEAT] Warp to Stage 7");
            game.playClick();
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

    // 텍스트 설계도에 있던 '시멘틱 디버그 프레임' 메서드 예시 (UI 영역 확인용)
    public static void drawDebugRect(ShapeRenderer shape, float x, float y, float width, float height, com.badlogic.gdx.graphics.Color color) {
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(color);
        shape.rect(x, y, width, height);
        shape.end();
    }
}
