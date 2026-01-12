package com.hades.game.logic;

import com.badlogic.gdx.utils.Array;
import com.hades.game.entities.Unit;

// BoardManager: 격자판 내 유닛 간의 상호작용 및 물리적 제약 조건을 판정함
public class BoardManager {

    // 특정 좌표(x, y)에 존재하는 유닛 객체를 찾아 반환함
    public static Unit getUnitAt(Array<Unit> units, int x, int y) {
        for (Unit unit : units) {
            if (unit.gridX == x && unit.gridY == y) return unit;
        }
        return null;
    }

    // 유닛의 스킬에 맞춰 이동 가능 여부를 판정함
    public static boolean canMoveTo(Unit unit, int targetX, int targetY, Array<Unit> units) {
        // 공통 제약: 격자 밖이나 이미 유닛이 있는 칸은 이동 불가
        if (targetX < 0 || targetX >= 7 || targetY < 0 || targetY >= 8) return false;
        if (getUnitAt(units, targetX, targetY) != null) return false;

        // [도약] 스킬 보유자 (민지, 코코미) 특수 로직
        if (unit.stat.skillName().equals("도약")) {
            int dx = Math.abs(unit.gridX - targetX);
            int dy = Math.abs(unit.gridY - targetY);

            // L자 형태: (가로2, 세로1) 또는 (가로1, 세로2)
            return (dx == 2 && dy == 1) || (dx == 1 && dy == 2);
        }

        // 일반 유닛: 맨해튼 거리 기반 이동
        int distance = Math.abs(unit.gridX - targetX) + Math.abs(unit.gridY - targetY);
        return distance <= unit.stat.move();
    }

    // 공격자의 사거리 내에 목표 대상이 있는지 판정함
    public static boolean canAttack(Unit attacker, Unit target) {
        // 같은 팀이면 공격 불가
        if (attacker.team.equals(target.team)) return false;

        // 공격자와 대상 사이의 맨해튼 거리 계산
        int distance = Math.abs(attacker.gridX - target.gridX) + Math.abs(attacker.gridY - target.gridY);

        // 유닛의 사거리 스탯 이내인지 확인
        return distance <= attacker.stat.range();
    }

}
