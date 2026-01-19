package com.hades.game.logic;

import com.badlogic.gdx.utils.Array;
import com.hades.game.entities.Unit;

public class BoardManager {

    public static Unit getUnitAt(Array<Unit> units, int x, int y) {
        for (Unit unit : units) {
            if (unit.gridX == x && unit.gridY == y && unit.isAlive()) return unit;
        }
        return null;
    }

    public static boolean canMoveTo(Unit unit, int targetX, int targetY, Array<Unit> units) {
        if (targetX < 0 || targetX >= 7 || targetY < 0 || targetY >= 8) return false;
        if (getUnitAt(units, targetX, targetY) != null) return false;

        int dx = Math.abs(unit.gridX - targetX);
        int dy = Math.abs(unit.gridY - targetY);

        // 1. 기병 (KNIGHT): L자 이동 (영웅의 도약 스킬을 병과 로직으로 이전)
        if (unit.unitClass == Unit.UnitClass.KNIGHT) {
            return (dx == 2 && dy == 1) || (dx == 1 && dy == 2);
        }

        // 2. 전차병 (CHARIOT): 상하좌우 직선 3칸 이동
        if (unit.unitClass == Unit.UnitClass.CHARIOT) {
            boolean isStraight = (dx == 0 || dy == 0);
            return isStraight && (dx + dy <= 3);
        }

        // 3. 일반 이동 (영웅 및 기타 병과): 맨해튼 거리 기반
        int distance = dx + dy;
        return distance <= unit.stat.move();
    }

    // 공격 사거리
    public static boolean canAttack(Unit attacker, Unit target) {
        if (target == null || !target.isAlive() || attacker.team.equals(target.team)) return false;

        int dx = Math.abs(attacker.gridX - target.gridX);
        int dy = Math.abs(attacker.gridY - target.gridY);
        int dist = dx + dy;

        // 1. 기병 (KNIGHT): 기병은 예외적으로 주변 8칸(대각선 포함)을 모두 타격 (유연성 유지)
        if (attacker.unitClass == Unit.UnitClass.KNIGHT) {
            return dx <= 1 && dy <= 1 && dist > 0;
        }

        // 2. 모든 일반 유닛 및 영웅 (궁병 포함)
        // [핵심] 동일 선상(X축 또는 Y축이 같음)에 있어야 하며, 거리가 사거리 이내여야 함
        boolean isStraight = (dx == 0 || dy == 0);

        if (isStraight && dist > 0 && dist <= attacker.stat.range()) {
            return true;
        }

        return false;
    }

    // 광역 공격을 위해 주변 모든 적을 반환하는 메서드 추가
    public static Array<Unit> findAllTargetsInRange(Unit attacker, Array<Unit> units) {
        Array<Unit> targets = new Array<>();
        for (int i = 0; i < units.size; i++) {
            Unit unit = units.get(i);
            if (canAttack(attacker, unit)) {
                targets.add(unit);
            }
        }
        return targets;
    }

    // 게임룰에 따라 사거리 범위 내 타겟 우선순위 설정
    public static Unit findBestTargetInRange(Unit attacker, Array<Unit> units) {
        Unit bestTarget = null;
        int minDistance = Integer.MAX_VALUE;
        int minHp = Integer.MAX_VALUE;
        int minCounterAtk = Integer.MAX_VALUE;

        for (int i = 0; i < units.size; i++) {
            Unit unit = units.get(i);

            // canAttack 내부에서 이미 '십자가 형태'인지 체크함
            if (unit.isAlive() && !unit.team.equals(attacker.team) && canAttack(attacker, unit)) {
                int dist = Math.abs(attacker.gridX - unit.gridX) + Math.abs(attacker.gridY - unit.gridY);
                int currentHp = unit.currentHp;
                int counterAtk = unit.stat.counterAtk();

                boolean shouldReplace = false;
                if (bestTarget == null) {
                    shouldReplace = true;
                } else {
                    // 1순위: 가장 가까운 적 (직선 거리 기준)
                    if (dist < minDistance) {
                        shouldReplace = true;
                    } else if (dist == minDistance) {
                        // 2순위: 체력이 낮은 적
                        if (currentHp < minHp) {
                            shouldReplace = true;
                        } else if (currentHp == minHp) {
                            // 3순위: 반격 데미지가 낮은 적
                            if (counterAtk < minCounterAtk) {
                                shouldReplace = true;
                            }
                        }
                    }
                }

                if (shouldReplace) {
                    bestTarget = unit;
                    minDistance = dist;
                    minHp = currentHp;
                    minCounterAtk = counterAtk;
                }
            }
        }
        return bestTarget;
    }
}
