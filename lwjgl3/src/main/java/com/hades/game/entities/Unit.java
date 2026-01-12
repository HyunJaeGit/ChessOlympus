package com.hades.game.entities;

import com.hades.game.constants.UnitData;

/**
 * Unit 클래스는 격자 위에 존재하는 개별 캐릭터의 실시간 상태를 관리합니다.
 */
public class Unit {
    public final String name;
    public final String team;      // "HADES" 또는 "ZEUS"
    public final UnitData.Stat stat; // 기본 스탯 정보

    public int currentHp;
    public int gridX;
    public int gridY;

    public Unit(String name, String team, UnitData.Stat stat, int x, int y) {
        this.name = name;
        this.team = team;
        this.stat = stat;
        this.currentHp = stat.hp();
        this.gridX = x;
        this.gridY = y;
    }

    /* 유닛의 위치를 변경할 때 사용하는 메서드입니다. */
    public void setPosition(int x, int y) {
        this.gridX = x;
        this.gridY = y;
    }
}
