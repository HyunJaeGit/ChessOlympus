package com.hades.game.logic;

// TurnManager: 게임의 현재 턴 상태를 관리하고 진영을 교체함
public class TurnManager {
    private String currentTurn = "HADES"; // 시작 진영

    // 현재 누구의 턴인지 확인
    public String getCurrentTurn() {
        return currentTurn;
    }

    // 턴을 강제로 교체 (HADES <-> ZEUS)
    public void endTurn() {
        currentTurn = currentTurn.equals("HADES") ? "ZEUS" : "HADES";
        System.out.println("현재 턴: " + currentTurn);
    }

    // 클릭한 유닛이 현재 턴의 주인인지 확인
    public boolean isMyTurn(String team) {
        return currentTurn.equals(team);
    }
}
