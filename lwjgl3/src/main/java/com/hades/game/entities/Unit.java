package com.hades.game.entities;

import com.hades.game.constants.UnitData;

public class Unit {
    // 병과 구분을 위한 Enum 추가
    public enum UnitClass {
        HERO, SHIELD, KNIGHT, ARCHER, CHARIOT, SAINT
    }

    public static final int ALIVE = 1;
    public static final int DEAD = 0;

    public final String name;
    public final String team;
    public final UnitData.Stat stat;
    public final UnitClass unitClass; // 병과 저장 변수

    public int currentHp;
    public int gridX;
    public int gridY;
    public int status = ALIVE;

    // 생성자 수정: UnitClass를 인자로 받습니다.
    public Unit(String name, String team, UnitData.Stat stat, UnitClass unitClass, int x, int y) {
        this.name = name;
        this.team = team;
        this.stat = stat;
        this.unitClass = unitClass;
        this.currentHp = stat.hp();
        this.gridX = x;
        this.gridY = y;
    }

    public int getPower(boolean isMyTurn) {
        return isMyTurn ? stat.atk() : stat.counterAtk();
    }

    public boolean canReach(Unit target) {
        if (target == null) return false;
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
