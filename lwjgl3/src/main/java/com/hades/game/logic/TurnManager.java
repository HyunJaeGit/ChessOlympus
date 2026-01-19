package com.hades.game.logic;

import com.hades.game.screens.BattleScreen;
import com.hades.game.entities.Unit;
import com.badlogic.gdx.utils.Array;

// TurnManager: 턴 교체 시 영웅 생존 여부를 전수 조사하여 게임 종료를 판정
public class TurnManager {
    private String currentTurn = "HADES";
    private BattleScreen battleScreen;

    public void setBattleScreen(BattleScreen screen) {
        this.battleScreen = screen;
    }

    public String getCurrentTurn() {
        return currentTurn;
    }

    public void endTurn() {
        if (battleScreen == null) return;

        // 1. 턴 교체 전 영웅 생존 여부 전수 조사
        Array<Unit> units = battleScreen.getUnits();
        Unit deadHeroCandidate = null;
        boolean zeusHeroAlive = false;
        boolean hadesHeroAlive = false;

        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            if (u.unitClass == Unit.UnitClass.HERO) {
                if (u.isAlive()) {
                    if (u.team.equals("ZEUS")) zeusHeroAlive = true;
                    if (u.team.equals("HADES")) hadesHeroAlive = true;
                } else {
                    // 리스트에는 있지만 죽은 상태인 영웅을 보관
                    deadHeroCandidate = u;
                }
            }
        }

        // 2. 영웅 중 하나라도 죽었다면 게임 종료 처리
        if (!zeusHeroAlive || !hadesHeroAlive) {
            System.out.println("[TurnManager] Hero death detected. Triggering GameOver Sequence.");

            // 죽은 영웅 객체가 리스트에 남아있다면 그 객체를 전달
            if (deadHeroCandidate != null) {
                battleScreen.handleDeath(deadHeroCandidate);
            } else {
                // 만약 리스트에서도 이미 삭제된 경우라면, 생존한 영웅의 반대 상황으로 판정
                // 이 상황은 보통 발생하지 않지만 안전을 위해 로그를 남깁니다.
                System.out.println("[TurnManager] Hero object missing, forcing check.");
                // 임시 객체 생성이 안 되므로, BattleScreen에 강제 종료용 메서드를 호출하거나
                // units 리스트의 아무 유닛이나 넣어 로직만 태웁니다.
                if (units.size > 0) battleScreen.handleDeath(units.get(0));
            }
            return;
        }

        // 3. 게임 오버가 아닐 때만 턴 교체
        if (battleScreen.isGameOver()) return;

        currentTurn = currentTurn.equals("HADES") ? "ZEUS" : "HADES";
        System.out.println("현재 턴: " + currentTurn);
    }

    public boolean isMyTurn(String team) {
        return currentTurn.equals(team);
    }
}
