package com.hades.game.logic;

import com.badlogic.gdx.utils.Array;
import com.hades.game.entities.Unit;

/* AILogic 클래스: 적군(AI) 유닛들의 의사결정과 행동 로직을 전담하여 처리하는 클래스입니다. */
public class AILogic {

    /**
     * AI 팀의 모든 유닛이 각자의 턴 행동을 수행하도록 지시하는 메인 메서드입니다.
     * @param units 현재 게임에 존재하는 모든 유닛 리스트
     * @param aiTeam AI가 조종할 팀 이름 (예: "ZEUS")
     */
    public static void processAITurn(Array<Unit> units, String aiTeam) {
        for (Unit unit : units) {
            // AI 팀 소속 유닛만 골라서 행동 시작
            if (unit.team.equals(aiTeam)) {
                Unit target = findNearestEnemy(unit, units);

                if (target != null) {
                    executeAction(unit, target, units);
                }
            }
        }
    }

    /**
     * 특정 유닛을 기준으로 가장 가까운 거리에 있는 적군 유닛을 찾아 반환합니다.
     * 2차원 격자에서의 맨해튼 거리(Manhattan Distance) 계산법을 사용합니다.
     */
    private static Unit findNearestEnemy(Unit actor, Array<Unit> units) {
        Unit nearest = null;
        int minDistance = Integer.MAX_VALUE;

        for (Unit enemy : units) {
            // 팀이 다른 경우에만 적으로 간주하여 거리 측정
            if (!enemy.team.equals(actor.team)) {
                int distance = Math.abs(actor.gridX - enemy.gridX) + Math.abs(actor.gridY - enemy.gridY);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = enemy;
                }
            }
        }
        return nearest;
    }

    /**
     * 대상(Target)과의 거리에 따라 이동할지 공격할지 결정하고 실행합니다.
     */
    private static void executeAction(Unit actor, Unit target, Array<Unit> units) {
        int distance = Math.abs(actor.gridX - target.gridX) + Math.abs(actor.gridY - target.gridY);

        // [판단] 적이 사거리 안에 있다면 즉시 공격
        if (distance <= actor.stat.range()) {
            performAttack(actor, target, units);
        }
        // [판단] 적이 멀리 있다면 적의 위치 방향으로 한 칸 이동 시도
        else {
            approachTarget(actor, target, units);
        }
    }

    /**
     * 적의 좌표와 현재 좌표를 비교하여 적에게 가까워지는 방향으로 좌표를 한 칸 수정합니다.
     */
    private static void approachTarget(Unit actor, Unit target, Array<Unit> units) {
        int nextX = actor.gridX;
        int nextY = actor.gridY;

        if (actor.gridX < target.gridX) nextX++;
        else if (actor.gridX > target.gridX) nextX--;

        if (actor.gridY < target.gridY) nextY++;
        else if (actor.gridY > target.gridY) nextY--;

        // 이동하려는 칸이 비어있는지 BoardManager를 통해 확인 후 이동
        if (BoardManager.canMoveTo(actor, nextX, nextY, units)) {
            actor.setPosition(nextX, nextY);
        }
    }

    /**
     * 공격을 수행하여 대상의 HP를 깎고, HP가 0 이하가 되면 전장에서 제거합니다.
     */
    private static void performAttack(Unit attacker, Unit target, Array<Unit> units) {
        target.currentHp -= attacker.stat.atk();
        if (target.currentHp <= 0) {
            units.removeValue(target, true);
        }
    }
}
