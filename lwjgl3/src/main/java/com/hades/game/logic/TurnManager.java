package com.hades.game.logic;

import com.hades.game.screens.BattleScreen;

// Chess Olympus: HADES vs ZEUS
// TurnManager: 게임의 현재 턴 상태를 관리하고 진영을 교체함
public class TurnManager {
    private String currentTurn = "HADES"; // 시작 진영
    private BattleScreen battleScreen;     // 게임 오버 상태 확인용 참조

    // BattleScreen 인스턴스를 등록하여 상태를 감시할 수 있게 합니다.
    public void setBattleScreen(BattleScreen screen) {
        this.battleScreen = screen;
    }

    // 현재 누구의 턴인지 확인
    public String getCurrentTurn() {
        return currentTurn;
    }

    // 턴을 강제로 교체 (HADES <-> ZEUS)
    public void endTurn() {
        // [Gatekeeper 로직] 게임이 오버되었다면 턴을 교체하지 않고 중단합니다.
        if (battleScreen != null && battleScreen.isGameOver()) {
            System.out.println("[TurnManager] 게임 종료 상태이므로 턴 교체를 중단합니다.");
            return;
        }

        currentTurn = currentTurn.equals("HADES") ? "ZEUS" : "HADES";
        System.out.println("현재 턴: " + currentTurn);
    }

    // 클릭한 유닛이 현재 턴의 주인인지 확인
    public boolean isMyTurn(String team) {
        return currentTurn.equals(team);
    }
}
