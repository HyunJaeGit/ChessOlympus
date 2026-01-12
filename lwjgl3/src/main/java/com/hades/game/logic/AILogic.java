package com.hades.game.logic;

import com.badlogic.gdx.utils.Array;
import com.hades.game.entities.Unit;
import com.hades.game.constants.UnitData;

/**
 * [클래스 역할]
 * 유닛의 생존 여부와 가치를 판단하여 최선의 수를 결정합니다.
 * 죽은 유닛이나 가치가 없는 대상은 연산에서 즉시 제외하여 효율성과 안정성을 높였습니다.
 */
public class AILogic {

    /**
     * AI의 턴을 처리합니다.
     * 리스트 순회 중 유닛이 제거되어도 에러가 나지 않도록 인덱스 방식을 사용합니다.
     */
    public static void processAITurn(Array<Unit> units, String aiTeam, TurnManager turnManager) {
        System.out.println("=== " + aiTeam + " AI 전략 연산 시작 ===");

        Unit bestActor = null;
        Unit bestTarget = null;
        int maxScore = -1;

        // [안전] 향상된 for문 대신 인덱스(i)를 사용하여 리스트 변동 시 발생하는 에러를 방지합니다.
        for (int i = 0; i < units.size; i++) {
            Unit actor = units.get(i);

            // 1. 행동 유닛 검사: 죽었거나 우리 팀이 아니면 스킵
            if (actor == null || actor.currentHp <= 0 || !actor.team.equals(aiTeam)) continue;

            for (int j = 0; j < units.size; j++) {
                Unit target = units.get(j);

                // 2. 타겟 유닛 검사: 죽었거나 같은 팀이면 스킵 (사용자님 제안 반영)
                if (target == null || target.currentHp <= 0 || target.team.equals(aiTeam)) continue;

                int score = evaluateAction(actor, target, units);
                if (score > maxScore) {
                    maxScore = score;
                    bestActor = actor;
                    bestTarget = target;
                }
            }
        }

        // 3. 연산된 최선의 수 실행
        if (bestActor != null && bestTarget != null && maxScore > 0) {
            executeFinalAction(bestActor, bestTarget, units);
        } else {
            System.out.println("[AI] 현재 공격하거나 이동할 수 있는 유효한 타겟이 없습니다.");
        }

        System.out.println("=== " + aiTeam + " 턴 종료 ===");
        turnManager.endTurn();
    }

    /**
     * 특정 유닛들 간의 행동 가치를 평가합니다.
     */
    private static int evaluateAction(Unit actor, Unit target, Array<Unit> units) {
        if (actor.currentHp <= 0 || target.currentHp <= 0) return 0;

        int score = 0;
        int dist = Math.abs(actor.gridX - target.gridX) + Math.abs(actor.gridY - target.gridY);

        // [수정] 타겟이 '왕'이라면 데이터상의 가치와 상관없이 엄청난 보너스 부여
        int targetRealValue = target.stat.value();
        if ("왕의 위엄".equals(target.stat.skillName())) {
            targetRealValue = 1000; // 적 왕은 무조건 1순위 타겟
        }

        if (dist <= actor.stat.range()) {
            // 공격 상황: 적 왕을 죽일 수 있다면 최고 점수
            score = (target.currentHp <= actor.stat.atk()) ? (targetRealValue * 10) : targetRealValue;
        } else {
            // 이동 상황: 적에게 다가가는 보너스
            score = Math.max(0, 10 - dist) + (targetRealValue / 10);

            // [추가] 자신의 가치가 너무 높으면(예: 왕) 전방으로 나가는 것에 감점 부여
            // "왕의 위엄" 스킬을 가진 유닛은 공격하러 나가는 점수를 깎습니다.
            if ("왕의 위엄".equals(actor.stat.skillName())) {
                score -= 500; // 왕은 웬만하면 제자리에 있게 함
            }
        }

        // 우리 왕 보호 보너스 (기존 로직 유지)
        Unit myKing = findKing(actor.team, units);
        if (myKing != null && myKing != target) {
            int distToKing = Math.abs(target.gridX - myKing.gridX) + Math.abs(target.gridY - myKing.gridY);
            if (distToKing <= 2) score += 150;
        }

        return score;
    }

    /**
     * 실제 공격 또는 이동 명령을 내립니다.
     */
    private static void executeFinalAction(Unit actor, Unit target, Array<Unit> units) {
        int dist = Math.abs(actor.gridX - target.gridX) + Math.abs(actor.gridY - target.gridY);
        if (dist <= actor.stat.range()) {
            performAttack(actor, target, units);
        } else {
            tryMoveToward(actor, target, units);
        }
    }

    /**
     * 유닛 공격을 수행하고 체력이 0이 되면 리스트에서 제거합니다.
     */
    private static void performAttack(Unit attacker, Unit target, Array<Unit> units) {
        target.currentHp -= attacker.stat.atk();
        System.out.println("[AI 공격] " + attacker.name + " -> " + target.name + " (잔여 HP: " + Math.max(0, target.currentHp) + ")");

        if (target.currentHp <= 0) {
            System.out.println("[전사] " + target.name + "이(가) 전장을 떠납니다.");
            units.removeValue(target, true);
        }
    }

    /**
     * 타겟을 향해 한 칸 이동합니다.
     */
    private static void tryMoveToward(Unit actor, Unit target, Array<Unit> units) {
        int nextX = actor.gridX, nextY = actor.gridY;

        if ("도약".equals(actor.stat.skillName())) {
            int[][] leapOffsets = {{2,1}, {2,-1}, {-2,1}, {-2,-1}, {1,2}, {1,-2}, {-1,2}, {-1,-2}};
            int minD = Integer.MAX_VALUE;
            for (int[] o : leapOffsets) {
                int tx = actor.gridX + o[0], ty = actor.gridY + o[1];
                if (BoardManager.canMoveTo(actor, tx, ty, units)) {
                    int d = Math.abs(tx - target.gridX) + Math.abs(ty - target.gridY);
                    if (d < minD) { minD = d; nextX = tx; nextY = ty; }
                }
            }
        } else {
            int dx = Integer.compare(target.gridX, actor.gridX);
            int dy = Integer.compare(target.gridY, actor.gridY);
            if (dx != 0 && BoardManager.canMoveTo(actor, actor.gridX + dx, actor.gridY, units)) nextX += dx;
            else if (dy != 0 && BoardManager.canMoveTo(actor, actor.gridX, actor.gridY + dy, units)) nextY += dy;
        }

        if (nextX != actor.gridX || nextY != actor.gridY) {
            actor.setPosition(nextX, nextY);
            System.out.println("[AI 이동] " + actor.name + " -> (" + nextX + "," + nextY + ")");
        }
    }

    private static boolean canLeapAnywhere(Unit actor, Array<Unit> units) {
        int[][] leapOffsets = {{2,1}, {2,-1}, {-2,1}, {-2,-1}, {1,2}, {1,-2}, {-1,2}, {-1,-2}};
        for (int[] o : leapOffsets) {
            if (BoardManager.canMoveTo(actor, actor.gridX + o[0], actor.gridY + o[1], units)) return true;
        }
        return false;
    }

    private static Unit findKing(String team, Array<Unit> units) {
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            if (u != null && u.team.equals(team) && "왕의 위엄".equals(u.stat.skillName())) return u;
        }
        return null;
    }

    /**
     * [신규 메서드] 사거리 내의 적들 중 누구를 쏠지 결정합니다.
     */
    private static Unit findBestTargetInRange(Unit attacker, Array<Unit> units) {
        Unit bestTarget = null;
        int minHp = Integer.MAX_VALUE;

        for (int i = 0; i < units.size; i++) {
            Unit target = units.get(i);
            if (target == null || target.currentHp <= 0 || target.team.equals(attacker.team)) continue;

            // BoardManager를 통해 사거리 체크
            if (BoardManager.canAttack(attacker, target)) {
                // 적 왕이면 즉시 타겟팅 (최우선순위)
                if ("왕의 위엄".equals(target.stat.skillName())) return target;

                // 그 외엔 체력이 가장 적은 적 우선
                if (target.currentHp < minHp) {
                    minHp = target.currentHp;
                    bestTarget = target;
                }
            }
        }
        return bestTarget;
    }
}
