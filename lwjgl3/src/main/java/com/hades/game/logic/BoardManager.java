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

    public static boolean canAttack(Unit attacker, Unit target) {
        if (target == null || !target.isAlive() || attacker.team.equals(target.team)) return false;

        int dx = Math.abs(attacker.gridX - target.gridX);
        int dy = Math.abs(attacker.gridY - target.gridY);

        // 기병 (KNIGHT): 주변 8칸 모두 공격 가능 (대각선 포함 거리 1)
        if (attacker.unitClass == Unit.UnitClass.KNIGHT) {
            return dx <= 1 && dy <= 1 && !(dx == 0 && dy == 0);
        }
        // 궁병 (ARCHER)
        if (attacker.unitClass == Unit.UnitClass.ARCHER) {
            boolean isStraight = (dx == 0 || dy == 0); // 직선 확인
            return isStraight && (dx + dy <= attacker.stat.range()); // 직선 거리 3칸 이내
        }

        // 일반 공격: 상하좌우 맨해튼 거리 기반
        return (dx + dy) <= attacker.stat.range();
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

        // 비교를 위한 기준값들
        int minDistance = Integer.MAX_VALUE;
        int minHp = Integer.MAX_VALUE;
        int minCounterAtk = Integer.MAX_VALUE;

        for (int i = 0; i < units.size; i++) {
            Unit unit = units.get(i);

            // 사거리 내에 있는 살아있는 적군인지 확인
            if (unit.isAlive() && !unit.team.equals(attacker.team) && canAttack(attacker, unit)) {
                int dist = Math.abs(attacker.gridX - unit.gridX) + Math.abs(attacker.gridY - unit.gridY);
                int currentHp = unit.currentHp;
                int counterAtk = unit.stat.counterAtk();

                // 타겟 교체 여부 결정
                boolean shouldReplace = false;

                if (bestTarget == null) {
                    shouldReplace = true;
                } else {
                    // 1순위: 가장 가까운 적
                    if (dist < minDistance) {
                        shouldReplace = true;
                    } else if (dist == minDistance) {
                        // 2순위: 거리도 같으면 현재 체력이 더 낮은 적
                        if (currentHp < minHp) {
                            shouldReplace = true;
                        } else if (currentHp == minHp) {
                            // 3순위: 체력까지 같으면 반격 데미지가 더 낮은 적
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
