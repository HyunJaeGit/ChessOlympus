package com.hades.game.entities;

import com.hades.game.constants.UnitData;

public class Unit {
    public static final int ALIVE = 1;
    public static final int DEAD = 0;

    public final String name;
    public final String team;
    public final UnitData.Stat stat;

    public int currentHp;
    public int gridX;
    public int gridY;
    public int status = ALIVE;

    public Unit(String name, String team, UnitData.Stat stat, int x, int y) {
        this.name = name;
        this.team = team;
        this.stat = stat;
        this.currentHp = stat.hp();
        this.gridX = x;
        this.gridY = y;
    }

    // [추가] 상황(내 턴 여부)에 따른 공격력을 반환합니다.
    // 내 턴이면 ATK, 상대 턴이면 C-ATK를 반환하여 '반격 페널티'를 구현합니다.
    public int getPower(boolean isMyTurn) {
        if (isMyTurn) {
            return stat.atk(); // 능동 공격 (스킬 확장 가능)
        } else {
            return stat.counterAtk(); // 반격 (순수 데미지)
        }
    }

    // [추가] 대상이 내 사거리 안에 있는지 확인합니다. (반격 가능 여부 판단)
    public boolean canReach(Unit target) {
        if (target == null) return false;
        // 맨해튼 거리 계산: |x1 - x2| + |y1 - y2|
        int dist = Math.abs(this.gridX - target.gridX) + Math.abs(this.gridY - target.gridY);
        return dist <= this.stat.range();
    }

    public boolean isAlive() {
        return status == ALIVE && currentHp > 0;
    }

    public void setPosition(int x, int y) {
        this.gridX = x;
        this.gridY = y;
    }
}
