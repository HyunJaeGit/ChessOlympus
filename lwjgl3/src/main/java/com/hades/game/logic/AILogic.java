package com.hades.game.logic;

import com.badlogic.gdx.utils.Array;
import com.hades.game.entities.Unit;

/**
 * [클래스 역할]
 * AILogic 클래스는 적군(AI) 유닛들의 의사결정을 담당합니다.
 * 행동 가능한 유닛을 찾을 때까지 리스트를 순회하며,
 * 한 유닛이 공격 또는 이동 중 하나의 행동을 완료하면 턴을 마칩니다.
 */
public class AILogic {

    /**
     * AI 팀의 유닛들을 순차적으로 확인하여 행동 가능한 첫 번째 유닛을 실행시킵니다.
     * @param units 전장의 모든 유닛 리스트
     * @param aiTeam AI 진영 이름 (ZEUS)
     * @param turnManager 턴 종료를 처리할 객체
     */
    public static void processAITurn(Array<Unit> units, String aiTeam, TurnManager turnManager) {
        System.out.println("=== " + aiTeam + " AI 유닛 탐색 시작 ===");

        boolean actionTaken = false;

        // 1. AI 팀 유닛들을 하나씩 검사
        for (Unit unit : units) {
            if (unit.team.equals(aiTeam)) {
                Unit target = findNearestEnemy(unit, units);

                if (target != null) {
                    // 2. 이 유닛이 공격이나 이동을 할 수 있는지 확인하고 실행
                    if (tryExecuteUnitAction(unit, target, units)) {
                        actionTaken = true;
                        break; // [중요] 한 명이라도 행동을 완료했다면 AI 턴 종료
                    }
                }
            }
        }

        if (!actionTaken) {
            System.out.println("[AI 대기] 현재 행동 가능한 유닛이 아무도 없습니다.");
        }

        // 3. 턴 매니저를 통해 다음 턴으로 교체
        System.out.println("=== " + aiTeam + " AI 행동 완료 및 턴 종료 ===");
        turnManager.endTurn();
    }

    /**
     * 선택된 유닛이 규칙(공격 우선 -> 이동 순)에 따라 행동을 시도합니다.
     * @return 행동을 실제로 수행했는지 여부
     */
    private static boolean tryExecuteUnitAction(Unit actor, Unit target, Array<Unit> units) {
        int distance = Math.abs(actor.gridX - target.gridX) + Math.abs(actor.gridY - target.gridY);

        // [행동 1순위] 사거리 내에 있다면 즉시 공격
        if (distance <= actor.stat.range()) {
            performAttack(actor, target, units);
            return true;
        }

        // [행동 2순위] 공격이 불가능하면 이동 시도
        return tryMoveToward(actor, target, units);
    }

    /**
     * 유닛의 스킬 규칙(일반/도약)에 맞춰 이동 가능한 칸을 찾아 이동합니다.
     */
    private static boolean tryMoveToward(Unit actor, Unit target, Array<Unit> units) {
        int nextX = actor.gridX;
        int nextY = actor.gridY;

        // [도약 유닛 규칙] L자 이동 경로 탐색
        if (actor.stat.skillName().equals("도약")) {
            int[][] leapOffsets = {{2,1}, {2,-1}, {-2,1}, {-2,-1}, {1,2}, {1,-2}, {-1,2}, {-1,-2}};
            int minDistance = Integer.MAX_VALUE;
            boolean found = false;

            for (int[] offset : leapOffsets) {
                int tx = actor.gridX + offset[0];
                int ty = actor.gridY + offset[1];
                if (BoardManager.canMoveTo(actor, tx, ty, units)) {
                    int dist = Math.abs(tx - target.gridX) + Math.abs(ty - target.gridY);
                    if (dist < minDistance) {
                        minDistance = dist;
                        nextX = tx; nextY = ty;
                        found = true;
                    }
                }
            }
            if (!found) return false; // 도약 가능한 칸이 없음
        }
        // [일반 유닛 규칙] 상하좌우 우회 탐색
        else {
            int dirX = (target.gridX > actor.gridX) ? 1 : (target.gridX < actor.gridX ? -1 : 0);
            int dirY = (target.gridY > actor.gridY) ? 1 : (target.gridY < actor.gridY ? -1 : 0);

            if (dirX != 0 && BoardManager.canMoveTo(actor, actor.gridX + dirX, actor.gridY, units)) {
                nextX = actor.gridX + dirX;
            } else if (dirY != 0 && BoardManager.canMoveTo(actor, actor.gridX, actor.gridY + dirY, units)) {
                nextY = actor.gridY + dirY;
            } else {
                return false; // 이동 가능한 경로가 막힘
            }
        }

        // 실제 좌표 업데이트 및 로그 출력
        actor.setPosition(nextX, nextY);
        System.out.println("[AI 이동] " + actor.name + " -> (" + nextX + ", " + nextY + ")");
        return true;
    }

    /**
     * 대상의 HP를 깎고 전사 여부를 판정합니다.
     */
    private static void performAttack(Unit attacker, Unit target, Array<Unit> units) {
        target.currentHp -= attacker.stat.atk();
        System.out.println("[AI 공격] " + attacker.name + " -> " + target.name + " (남은 HP: " + target.currentHp + ")");
        if (target.currentHp <= 0) {
            units.removeValue(target, true);
            System.out.println("[사망] " + target.name + "이(가) 제거되었습니다.");
        }
    }

    /**
     * 가장 가까운 적군 유닛을 반환합니다.
     */
    private static Unit findNearestEnemy(Unit actor, Array<Unit> units) {
        Unit nearest = null;
        int minDistance = Integer.MAX_VALUE;
        for (Unit enemy : units) {
            if (!enemy.team.equals(actor.team)) {
                int dist = Math.abs(actor.gridX - enemy.gridX) + Math.abs(actor.gridY - enemy.gridY);
                if (dist < minDistance) {
                    minDistance = dist;
                    nearest = enemy;
                }
            }
        }
        return nearest;
    }
}
