package com.hades.game.logic;

import com.badlogic.gdx.utils.Array;
import com.hades.game.entities.Unit;

// 격자판 내 유닛 간의 상호작용 및 물리적 제약 조건을 판정합니다.
public class BoardManager {

    // 특정 좌표에 존재하는 '살아있는' 유닛을 찾아 반환합니다.
    public static Unit getUnitAt(Array<Unit> units, int x, int y) {
        for (Unit unit : units) {
            // 좌표가 일치하고 실제로 살아있는 상태인 유닛만 반환
            if (unit.gridX == x && unit.gridY == y && unit.isAlive()) {
                return unit;
            }
        }
        return null;
    }

    // 유닛의 스킬 및 이동력에 맞춰 이동 가능 여부를 판정합니다.
    public static boolean canMoveTo(Unit unit, int targetX, int targetY, Array<Unit> units) {
        // 격자 범위를 벗어나면 이동 불가
        if (targetX < 0 || targetX >= 7 || targetY < 0 || targetY >= 8) return false;

        // 해당 칸에 이미 살아있는 유닛이 있다면 이동 불가
        if (getUnitAt(units, targetX, targetY) != null) return false;

        // [도약] 스킬 보유자 특수 이동 로직
        if ("도약".equals(unit.stat.skillName())) {
            int dx = Math.abs(unit.gridX - targetX);
            int dy = Math.abs(unit.gridY - targetY);
            return (dx == 2 && dy == 1) || (dx == 1 && dy == 2);
        }

        // 일반 이동: 맨해튼 거리 기반 판정
        int distance = Math.abs(unit.gridX - targetX) + Math.abs(unit.gridY - targetY);
        return distance <= unit.stat.move();
    }

    // 공격자의 사거리 내에 적절한 목표 대상이 있는지 판정합니다.
    public static boolean canAttack(Unit attacker, Unit target) {
        // 타겟이 없거나 이미 죽었다면 공격 불가
        if (target == null || !target.isAlive()) return false;

        // 같은 팀이면 공격 불가
        if (attacker.team.equals(target.team)) return false;

        int distance = Math.abs(attacker.gridX - target.gridX) + Math.abs(attacker.gridY - target.gridY);
        return distance <= attacker.stat.range();
    }

    // 사거리 내 적들 중 우선순위(왕 > 최저 체력)에 따라 타겟을 선정합니다.
    public static Unit findBestTargetInRange(Unit attacker, Array<Unit> units) {
        Unit bestTarget = null;
        int minHp = Integer.MAX_VALUE;

        for (Unit unit : units) {
            // 살아있는 적군인지 확인
            if (unit.isAlive() && !unit.team.equals(attacker.team)) {
                if (canAttack(attacker, unit)) {
                    // 1순위: 적의 왕(RULER) 타겟팅
                    if ("왕의 위엄".equals(unit.stat.skillName())) {
                        return unit;
                    }
                    // 2순위: 체력이 가장 낮은 적 선택
                    if (unit.currentHp < minHp) {
                        minHp = unit.currentHp;
                        bestTarget = unit;
                    }
                }
            }
        }
        return bestTarget;
    }
}
