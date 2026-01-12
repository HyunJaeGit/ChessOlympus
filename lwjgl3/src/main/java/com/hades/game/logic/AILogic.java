package com.hades.game.logic;

import com.badlogic.gdx.utils.Array;
import com.hades.game.entities.Unit;
import com.badlogic.gdx.math.MathUtils;

/**
 * [클래스 역할]
 * 팀의 전략(암살, 희생, 고효율)에 따라 최적의 이동 요원을 선발합니다.
 * 킹은 팀의 패배 조건이므로 최대한 사리며, 일반 병사들이 전선을 형성하도록 제어합니다.
 */
public class AILogic {

    /**
     * AI의 전체 턴 흐름을 제어합니다.
     */
    public static void processAITurn(Array<Unit> units, String aiTeam, TurnManager turnManager) {
        String strategy = rollTeamStrategy();
        System.out.println("\n=== [" + aiTeam + "] AI 전략 수립: " + strategy + " ===");

        Unit actor = null;
        Unit target = null;

        // 1. 전략 기조에 따른 요원 및 타겟 선발
        if ("암살형".equals(strategy)) {
            target = findWeakestEnemy(units, aiTeam);
            if (target != null) actor = findMovableAllyClosestTo(units, aiTeam, target);
        }
        else if ("희생형".equals(strategy)) {
            Unit bait = findSacrificialUnit(units, aiTeam);
            if (bait != null) {
                target = findBestTargetForUnit(bait, units);
                if (target != null) {
                    System.out.println("[AI 희생] 미끼: " + bait.name + " -> 자리를 고수하며 [" + target.name + "] 유인.");
                    actor = findOtherMovableAlly(units, aiTeam, bait, target);
                }
            }
        }

        // 2. 특정 전략 요원 선발 실패 시, 가장 효율적인 이동 쌍 선발 (킹은 여기서도 후순위)
        if (actor == null) {
            Object[] pair = findBestMovableEfficiencyPair(units, aiTeam);
            actor = (Unit) pair[0];
            target = (Unit) pair[1];
            if (actor != null) strategy = "고효율(전환)";
        }

        // 3. 실행: 좌표 변화가 보장된 이동 수행
        if (actor != null && target != null) {
            executeMoveByStrategy(actor, target, units, strategy);
            processTeamAutoAttack(units, aiTeam);
        } else {
            System.out.println("[AI] 이동 가능한 적절한 유닛이 없습니다.");
        }

        turnManager.endTurn();
    }

    /**
     * [메서드 설명] 킹이 함부로 전진하지 않도록 가치 평가를 엄격히 제한합니다.
     */
    private static int evaluateAction(Unit actor, Unit target, Array<Unit> units) {
        int score = 0;
        int dist = Math.abs(actor.gridX - target.gridX) + Math.abs(actor.gridY - target.gridY);
        int targetValue = target.stat.value();

        // 적 킹은 반드시 잡아야 할 1순위 타겟
        if ("왕의 위엄".equals(target.stat.skillName())) targetValue = 1500;

        if (dist <= actor.stat.range()) {
            // 사거리 내 적이 있다면 공격 가치 계산
            score = (target.currentHp <= actor.stat.atk()) ? (targetValue * 10) : targetValue;
        } else {
            // 사거리 밖의 적을 향해 전진할 때의 점수
            score = Math.max(0, 10 - dist) + (targetValue / 10);

            /* [핵심] 아군 킹의 돌발 행동 방지 */
            if ("왕의 위엄".equals(actor.stat.skillName())) {
                score -= 3000; // 킹이 직접 전진하는 것에 대해 매우 큰 패널티 부여
                if ((float)actor.currentHp / actor.stat.hp() < 0.6f) score -= 5000; // 체력이 깎이면 더 사림
            }
        }

        // 아군 킹 주변의 적을 처단하여 보호하는 로직
        Unit myKing = findKing(actor.team, units);
        if (myKing != null && myKing != actor && myKing != target) {
            int distToKing = Math.abs(target.gridX - myKing.gridX) + Math.abs(target.gridY - myKing.gridY);
            if (distToKing <= 3) score += 500; // 킹을 위협하는 적에 대한 반격 우선순위
        }

        return score;
    }

    /**
     * [메서드 설명] 킹을 제외한 일반 유닛 중 타겟과 가까운 유닛을 우선 선발합니다.
     */
    private static Unit findMovableAllyClosestTo(Array<Unit> units, String aiTeam, Unit target) {
        Unit closest = null; int minDist = Integer.MAX_VALUE;
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            if (u == null || u.currentHp <= 0 || !u.team.equals(aiTeam)) continue;

            // 킹은 전진 요원에서 원칙적으로 제외
            if ("왕의 위엄".equals(u.stat.skillName())) continue;

            if (willActuallyMove(u, target, units)) {
                int d = Math.abs(u.gridX - target.gridX) + Math.abs(u.gridY - target.gridY);
                if (d < minDist) { minDist = d; closest = u; }
            }
        }
        // 아군이 킹밖에 없다면 킹이 스스로 움직임
        return (closest != null) ? closest : findKing(aiTeam, units);
    }

    /**
     * [메서드 설명] 두 유닛 간의 좌표 이동이 실제로 발생하는지 체크합니다.
     */
    private static boolean willActuallyMove(Unit actor, Unit target, Array<Unit> units) {
        int oldX = actor.gridX, oldY = actor.gridY;
        int dx = Integer.compare(target.gridX, actor.gridX);
        int dy = Integer.compare(target.gridY, actor.gridY);
        int nextX = oldX, nextY = oldY;

        if (dx != 0 && BoardManager.canMoveTo(actor, oldX + dx, oldY, units)) nextX += dx;
        else if (dy != 0 && BoardManager.canMoveTo(actor, oldX, oldY + dy, units)) nextY += dy;

        return (nextX != oldX || nextY != oldY);
    }

    /**
     * [메서드 설명] 중첩 이터레이터 에러 방지를 위한 인덱스 기반의 쌍 선발 로직입니다.
     */
    private static Object[] findBestMovableEfficiencyPair(Array<Unit> units, String aiTeam) {
        Unit bestA = null, bestT = null; int maxS = -10000;
        for (int i = 0; i < units.size; i++) {
            Unit a = units.get(i);
            if (a == null || a.currentHp <= 0 || !a.team.equals(aiTeam)) continue;
            for (int j = 0; j < units.size; j++) {
                Unit t = units.get(j);
                if (t == null || t.currentHp <= 0 || t.team.equals(aiTeam)) continue;

                if (willActuallyMove(a, t, units)) {
                    int s = evaluateAction(a, t, units);
                    if (s > maxS) { maxS = s; bestA = a; bestT = t; }
                }
            }
        }
        return new Object[]{bestA, bestT};
    }

    private static void executeMoveByStrategy(Unit actor, Unit target, Array<Unit> units, String strategy) {
        int oldX = actor.gridX, oldY = actor.gridY;
        System.out.print("[AI 선발] 요원: " + actor.name + " (" + strategy + ") -> ");

        // 킹이고 체력이 낮으며 적이 너무 가까우면 도망 로직 시도 (추가 생존본능)
        int dist = Math.abs(actor.gridX - target.gridX) + Math.abs(actor.gridY - target.gridY);
        if ("왕의 위엄".equals(actor.stat.skillName()) && (float)actor.currentHp / actor.stat.hp() < 0.4f && dist < 3) {
            System.out.print("위험 감지! 후퇴 시도 -> ");
            moveAwayFrom(actor, target, units);
        } else {
            tryMoveToward(actor, target, units);
        }
        System.out.println("좌표 변경: (" + oldX + "," + oldY + ") -> (" + actor.gridX + "," + actor.gridY + ")");
    }

    // --- 유틸리티 메서드들 ---

    private static String rollTeamStrategy() {
        float roll = MathUtils.random(0f, 100f);
        if (roll < 20) return "암살형";
        if (roll < 50) return "희생형";
        return "고효율";
    }

    private static Unit findKing(String team, Array<Unit> units) {
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            if (u != null && u.team.equals(team) && "왕의 위엄".equals(u.stat.skillName())) return u;
        }
        return null;
    }

    private static Unit findWeakestEnemy(Array<Unit> units, String aiTeam) {
        Unit weakest = null; int minHp = Integer.MAX_VALUE;
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            if (u != null && u.currentHp > 0 && !u.team.equals(aiTeam)) {
                if (u.currentHp < minHp) { minHp = u.currentHp; weakest = u; }
            }
        }
        return weakest;
    }

    private static Unit findOtherMovableAlly(Array<Unit> units, String aiTeam, Unit bait, Unit target) {
        Unit best = null; int minDist = Integer.MAX_VALUE;
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            if (u != null && u.currentHp > 0 && u.team.equals(aiTeam) && u != bait) {
                if ("왕의 위엄".equals(u.stat.skillName())) continue;
                if (willActuallyMove(u, target, units)) {
                    int d = Math.abs(u.gridX - target.gridX) + Math.abs(u.gridY - target.gridY);
                    if (d < minDist) { minDist = d; best = u; }
                }
            }
        }
        return best;
    }

    private static Unit findSacrificialUnit(Array<Unit> units, String aiTeam) {
        Unit candidate = null; float minRatio = 2.0f;
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            if (u != null && u.currentHp > 0 && u.team.equals(aiTeam)) {
                if ("왕의 위엄".equals(u.stat.skillName())) continue;
                float r = (float) u.currentHp / u.stat.hp();
                if (r < minRatio) { minRatio = r; candidate = u; }
            }
        }
        return candidate;
    }

    private static Unit findBestTargetForUnit(Unit actor, Array<Unit> units) {
        Unit best = null; int maxS = -10000;
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            if (u == null || u.currentHp <= 0 || u.team.equals(actor.team)) continue;
            int s = evaluateAction(actor, u, units);
            if (s > maxS) { maxS = s; best = u; }
        }
        return best;
    }

    private static void tryMoveToward(Unit actor, Unit target, Array<Unit> units) {
        int dx = Integer.compare(target.gridX, actor.gridX);
        int dy = Integer.compare(target.gridY, actor.gridY);
        if (dx != 0 && BoardManager.canMoveTo(actor, actor.gridX + dx, actor.gridY, units)) actor.setPosition(actor.gridX + dx, actor.gridY);
        else if (dy != 0 && BoardManager.canMoveTo(actor, actor.gridX, actor.gridY + dy, units)) actor.setPosition(actor.gridX, actor.gridY + dy);
    }

    private static void moveAwayFrom(Unit actor, Unit target, Array<Unit> units) {
        int bestX = actor.gridX, bestY = actor.gridY;
        int maxD = Math.abs(actor.gridX - target.gridX) + Math.abs(actor.gridY - target.gridY);
        int[][] dirs = {{1,0}, {-1,0}, {0,1}, {0,-1}};
        for (int[] d : dirs) {
            int tx = actor.gridX + d[0], ty = actor.gridY + d[1];
            if (BoardManager.canMoveTo(actor, tx, ty, units)) {
                int dist = Math.abs(tx - target.gridX) + Math.abs(ty - target.gridY);
                if (dist > maxD) { maxD = dist; bestX = tx; bestY = ty; }
            }
        }
        actor.setPosition(bestX, bestY);
    }

    private static void processTeamAutoAttack(Array<Unit> units, String team) {
        System.out.println("[협공] " + team + " 진영 자동 사격!");
        for (int i = 0; i < units.size; i++) {
            Unit attacker = units.get(i);
            if (attacker == null || attacker.currentHp <= 0 || !attacker.team.equals(team)) continue;
            Unit target = findBestTargetInRange(attacker, units);
            if (target != null) performAttack(attacker, target, units);
        }
    }

    private static Unit findBestTargetInRange(Unit attacker, Array<Unit> units) {
        Unit best = null; int minHp = Integer.MAX_VALUE;
        for (int i = 0; i < units.size; i++) {
            Unit t = units.get(i);
            if (t != null && t.currentHp > 0 && !t.team.equals(attacker.team)) {
                if (BoardManager.canAttack(attacker, t)) {
                    if (t.currentHp < minHp) { minHp = t.currentHp; best = t; }
                }
            }
        }
        return best;
    }

    private static void performAttack(Unit attacker, Unit target, Array<Unit> units) {
        target.currentHp -= attacker.stat.atk();
        System.out.println(" -> [공격] " + attacker.name + " -> " + target.name + " (HP: " + Math.max(0, target.currentHp) + ")");
        if (target.currentHp <= 0) {
            System.out.println(" -> [전사] " + target.name + " 퇴장.");
            units.removeValue(target, true);
        }
    }
}
