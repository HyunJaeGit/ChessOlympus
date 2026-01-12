package com.hades.game.entities;

import com.hades.game.constants.UnitData;

/**
 * [클래스 역할] 격자 위에 존재하는 개별 캐릭터의 실시간 상태(HP, 위치)를 관리합니다.
 * UnitData.Stat을 통해 공격력(atk)과 반격력(counterAtk)을 모두 보유합니다.
 */
public class Unit {
    public final String name;
    public final String team;      // "HADES" 또는 "ZEUS"
    public final UnitData.Stat stat; // [데이터 포함] hp, atk, counterAtk, move, range 등

    public int currentHp;
    public int gridX;
    public int gridY;


    // 유닛을 생성할 때 기본 스탯 정보(stat)를 주입받습니다.
    public Unit(String name, String team, UnitData.Stat stat, int x, int y) {
        this.name = name;
        this.team = team;
        this.stat = stat;
        this.currentHp = stat.hp(); // 생성 시 최대 체력으로 설정
        this.gridX = x;
        this.gridY = y;
    }

    // [메서드 설명] 유닛의 격자 좌표를 변경합니다.
    public void setPosition(int x, int y) {
        this.gridX = x;
        this.gridY = y;
    }
}
