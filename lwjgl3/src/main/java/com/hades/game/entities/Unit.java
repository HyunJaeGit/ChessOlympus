package com.hades.game.entities;

import com.hades.game.constants.UnitData;

/**
 * [클래스 역할] 격자 위에 존재하는 개별 캐릭터의 실시간 상태(HP, 위치)를 관리합니다.
 * 상태 플래그(status)를 통해 시스템 안전성을 확보합니다.
 */
public class Unit {
    // 상태 상수 정의 (현업에서는 의미 없는 숫자 대신 이름을 부여합니다)
    public static final int ALIVE = 1;
    public static final int DEAD = 0;

    public final String name;
    public final String team;      // "HADES" 또는 "ZEUS"
    public final UnitData.Stat stat;

    public int currentHp;
    public int gridX;
    public int gridY;

    /**
     * 유닛의 생존 상태를 나타냅니다.
     * 1 (ALIVE): 정상 활동 가능
     * 0 (DEAD): 사망하여 리스트에서 제거 대기 중
     */
    public int status = ALIVE;

    public Unit(String name, String team, UnitData.Stat stat, int x, int y) {
        this.name = name;
        this.team = team;
        this.stat = stat;
        this.currentHp = stat.hp();
        this.gridX = x;
        this.gridY = y;
    }

    /**
     * [메서드 설명] 유닛이 현재 전장에서 활동 가능한 상태인지 확인합니다.
     * @return 체력이 있고 상태가 ALIVE이면 true
     */
    public boolean isAlive() {
        return status == ALIVE && currentHp > 0;
    }

    public void setPosition(int x, int y) {
        this.gridX = x;
        this.gridY = y;
    }
}
