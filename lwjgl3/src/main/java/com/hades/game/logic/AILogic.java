package com.hades.game.logic;

import com.badlogic.gdx.utils.Array;
import com.hades.game.entities.Unit;
import com.badlogic.gdx.math.MathUtils;
import com.hades.game.HadesGame; /* [수정] HadesGame 참조를 위한 import 추가 */

public class AILogic {

    /* [메서드 설명] AI의 턴 로직을 총괄합니다. 전략 수립 -> 요원 선발 -> 이동 -> 협공 순으로 진행됩니다. */
    public static void processAITurn(Array<Unit> units, String aiTeam, TurnManager turnManager, HadesGame game) {
        String strategy = rollTeamStrategy();
        System.out.println("\n=== [" + aiTeam + "] AI 전략 수립: " + strategy + " ===");

        Unit actor = null;
        Unit target = null;

        // 1. 전략 기조에 따른 요원 및 타겟 선발
        if ("암살형".equals(strategy)) {
            target = findWeakestEnemy(units, aiTeam);
            if (target != null) actor = findMovableAllyClosestTo(units, aiTeam, target);
        } else if ("희생형".equals(strategy)) {
            Unit bait = findSacrificialUnit(units, aiTeam);
            if (bait != null) {
                target = findBestTargetForUnit(bait, units);
                if (target != null) {
                    actor = findOtherMovableAlly(units, aiTeam, bait, target);
                }
            }
        }

        // 2. 전략 요원 선발 실패 시, 가장 효율적인 이동 쌍 선발
        if (actor == null) {
            Object[] pair = findBestMovableEfficiencyPair(units, aiTeam);
            actor = (Unit) pair[0];
            target = (Unit) pair[1];
        }

        // 3. 실행: 좌표 이동 후 HadesGame의 전역 협공 로직 호출
        if (actor != null && target != null) {
            executeMove(actor, target, units);
            game.processAutoAttack(aiTeam); /* HadesGame의 중앙 룰을 사용하여 협공 실행 */
        } else {
            System.out.println("[AI] 이동 가능한 유닛이 없어 대기합니다.");
        }

        turnManager.endTurn();
    }

    /* [메서드 설명] 선택된 요원을 목표 방향으로 이동시킵니다. 킹의 생존 본능(도망) 로직이 포함되어 있습니다. */
    private static void executeMove(Unit actor, Unit target, Array<Unit> units) {
        int oldX = actor.gridX, oldY = actor.gridY;
        int dist = Math.abs(actor.gridX - target.gridX) + Math.abs(actor.gridY - target.gridY);

        /* 아군 킹이 위험(체력 40% 미만 & 적 인접)하면 도망칩니다. */
        if ("왕의 위엄".equals(actor.stat.skillName()) && (float) actor.currentHp / actor.stat.hp() < 0.4f && dist < 3) {
            moveAwayFrom(actor, target, units);
        } else {
            tryMoveToward(actor, target, units);
        }
        System.out.println("[AI 이동] " + actor.name + ": (" + oldX + "," + oldY + ") -> (" + actor.gridX + "," + actor.gridY + ")");
    }

    /* [메서드 설명] 특정 행동의 가치를 점수로 환산합니다. 킹이 전진하는 것에는 큰 패널티를 줍니다. */
    private static int evaluateAction(Unit actor, Unit target, Array<Unit> units) {
        int score; /* [수정] Redundant initializer 경고 해결을 위해 선언만 수행 */
        int dist = Math.abs(actor.gridX - target.gridX) + Math.abs(actor.gridY - target.gridY);
        int targetValue = target.stat.value();

        if ("왕의 위엄".equals(target.stat.skillName())) targetValue = 1500; /* 적 킹은 최우선 제거 대상 */

        if (dist <= actor.stat.range()) {
            score = (target.currentHp <= actor.stat.atk()) ? (targetValue * 10) : targetValue;
        } else {
            score = Math.max(0, 10 - dist) + (targetValue / 10);
            /* 아군 킹의 돌발 전진 억제 */
            if ("왕의 위엄".equals(actor.stat.skillName())) score -= 3000;
        }

        /* 아군 킹 주변의 적을 처단하는 것에 가산점 */
        Unit myKing = findKing(actor.team, units);
        if (myKing != null && myKing != actor && myKing != target) {
            int distToKing = Math.abs(target.gridX - myKing.gridX) + Math.abs(target.gridY - myKing.gridY);
            if (distToKing <= 3) score += 500;
        }
        return score;
    }

// --- 유틸리티 및 탐색 메서드 ---

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

    private static Unit findMovableAllyClosestTo(Array<Unit> units, String aiTeam, Unit target) {
        Unit closest = null; int minDist = Integer.MAX_VALUE;
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            if (u == null || u.currentHp <= 0 || !u.team.equals(aiTeam) || "왕의 위엄".equals(u.stat.skillName())) continue;
            if (willActuallyMove(u, target, units)) {
                int d = Math.abs(u.gridX - target.gridX) + Math.abs(u.gridY - target.gridY);
                if (d < minDist) { minDist = d; closest = u; }
            }
        }
        return (closest != null) ? closest : findKing(aiTeam, units);
    }

    private static boolean willActuallyMove(Unit actor, Unit target, Array<Unit> units) {
        int oldX = actor.gridX, oldY = actor.gridY;
        int dx = Integer.compare(target.gridX, actor.gridX);
        int dy = Integer.compare(target.gridY, actor.gridY);
        int nextX = oldX, nextY = oldY;

        if (dx != 0 && BoardManager.canMoveTo(actor, oldX + dx, oldY, units)) nextX += dx;
        else if (dy != 0 && BoardManager.canMoveTo(actor, oldX, oldY + dy, units)) nextY += dy;

        return (nextX != oldX || nextY != oldY);
    }

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

    private static void tryMoveToward(Unit actor, Unit target, Array<Unit> units) {
        int dx = Integer.compare(target.gridX, actor.gridX);
        int dy = Integer.compare(target.gridY, actor.gridY);
        if (dx != 0 && BoardManager.canMoveTo(actor, actor.gridX + dx, actor.gridY, units)) actor.setPosition(actor.gridX + dx, actor.gridY);
        else if (dy != 0 && BoardManager.canMoveTo(actor, actor.gridX, actor.gridY + dy, units)) actor.setPosition(actor.gridX, actor.gridY + dy);
    }

    private static void moveAwayFrom(Unit actor, Unit target, Array<Unit> units) {
        int bestX = actor.gridX, bestY = actor.gridY;
        int maxD = Math.abs(actor.gridX - target.gridX) + Math.abs(actor.gridY - target.gridY);
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : dirs) {
            int tx = actor.gridX + d[0], ty = actor.gridY + d[1];
            if (BoardManager.canMoveTo(actor, tx, ty, units)) {
                int dist = Math.abs(tx - target.gridX) + Math.abs(ty - target.gridY);
                if (dist > maxD) { maxD = dist; bestX = tx; bestY = ty; }
            }
        }
        actor.setPosition(bestX, bestY);
    }

    private static Unit findSacrificialUnit(Array<Unit> units, String aiTeam) {
        Unit candidate = null; float minRatio = 2.0f;
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            if (u != null && u.currentHp > 0 && u.team.equals(aiTeam) && !"왕의 위엄".equals(u.stat.skillName())) {
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
}
