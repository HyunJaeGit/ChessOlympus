package com.hades.game.logic;

import com.badlogic.gdx.utils.Array;
import com.hades.game.entities.Unit;
import com.badlogic.gdx.math.MathUtils;
import com.hades.game.screens.BattleScreen;

/**
 * AI의 의사결정과 실행 단계를 명확히 분리하여
 * 중첩 Iterator 에러 및 데이터 충돌을 방지하는 클래스입니다.
 */
public class AILogic {
    private static final int MAX_ITER = 50;

    // AI 턴 처리: 탐색 -> 결정 -> 실행 순서를 엄격히 따름
    public static void processAITurn(Array<Unit> units, String aiTeam, TurnManager turnManager, Object screenObj) {
        String strategy = getStrategy();
        System.out.println("\n=== [" + aiTeam + "] 전략: " + strategy + " ===");

        // [1단계] 탐색 및 결정 (Thinking Phase)
        Unit bestActor = null;
        Unit bestTarget = null;

        if ("암살형".equals(strategy)) {
            bestTarget = getWeakest(units, aiTeam);
            if (bestTarget != null) bestActor = getClosest(units, aiTeam, bestTarget);
        } else if ("희생형".equals(strategy)) {
            Unit bait = getBait(units, aiTeam);
            if (bait != null) {
                bestTarget = getTarget(bait, units);
                if (bestTarget != null) bestActor = getAlly(units, aiTeam, bait, bestTarget);
            }
        }

        // 특정 전략에서 후보를 못 찾았다면 기본 고효율 쌍 탐색
        if (bestActor == null) {
            Object[] bestPair = getBest(units, aiTeam);
            bestActor = (Unit) bestPair[0];
            bestTarget = (Unit) bestPair[1];
        }

        // [2단계] 최종 유효성 검증 (Validation Phase)
        // 행동할 유닛과 대상이 모두 존재하고 살아있는지 확인합니다.
        if (bestActor != null && bestTarget != null && bestActor.isAlive() && bestTarget.isAlive()) {

            // [3단계] 실행 (Action Phase)
            System.out.println("[AI 결정] " + bestActor.name + " -> " + bestTarget.name + " 타겟팅");

            // 유닛을 타겟 방향으로 1칸 이동시킵니다.
            moveUnit(bestActor, bestTarget, units);

            // 이동 후, BattleScreen의 자동 공격 로직을 호출하여 전투를 처리합니다.
            if (screenObj instanceof BattleScreen) {
                ((BattleScreen) screenObj).processAutoAttack(aiTeam);
            }
        } else {
            // 행동할 대상을 찾지 못한 경우에 대한 로그 (버그 추적용)
            System.out.println("[AI 대기] 이번 턴에 행동할 적절한 타겟을 찾지 못했습니다.");
        }

        // [중요] 어떤 상황에서도 AI 턴이 끝났음을 TurnManager에 알려 턴이 꼬이는 것을 방지합니다.
        turnManager.endTurn();
    }

    // 전수 조사를 통해 가장 효율적인 공격 유닛과 대상 선정 (중첩 인덱스 루프)
    private static Object[] getBest(Array<Unit> units, String aiTeam) {
        Unit bestA = null, bestT = null;
        int maxS = -10000;
        int count = 0;

        for (int i = 0; i < units.size; i++) {
            Unit a = units.get(i);
            if (a == null || !a.isAlive() || !a.team.equals(aiTeam)) continue;

            for (int j = 0; j < units.size; j++) {
                if (++count > MAX_ITER) break;
                Unit t = units.get(j);
                if (t == null || !t.isAlive() || t.team.equals(aiTeam)) continue;

                if (canMove(a, t, units)) {
                    int s = evalAction(a, t, units);
                    if (s > maxS) {
                        maxS = s;
                        bestA = a;
                        bestT = t;
                    }
                }
            }
        }
        return new Object[]{bestA, bestT};
    }

    // 행동의 가치를 평가하여 점수 반환
    private static int evalAction(Unit actor, Unit target, Array<Unit> units) {
        int dist = Math.abs(actor.gridX - target.gridX) + Math.abs(actor.gridY - target.gridY);
        int val = target.stat.value();
        if ("왕의 위엄".equals(target.stat.skillName())) val = 1500;

        int score;
        if (dist <= actor.stat.range()) {
            score = (target.currentHp <= actor.stat.atk()) ? (val * 10) : val;
        } else {
            score = Math.max(0, 10 - dist) + (val / 10);
            if ("왕의 위엄".equals(actor.stat.skillName())) score -= 3000;
        }
        return score;
    }

    // 체력이 가장 낮은 적 탐색
    private static Unit getWeakest(Array<Unit> units, String aiTeam) {
        Unit weak = null;
        int minHp = Integer.MAX_VALUE;
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            if (u != null && u.isAlive() && !u.team.equals(aiTeam)) {
                if (u.currentHp < minHp) {
                    minHp = u.currentHp;
                    weak = u;
                }
            }
        }
        return weak;
    }

    // 타겟과 가장 가까운 아군 탐색
    private static Unit getClosest(Array<Unit> units, String aiTeam, Unit target) {
        Unit close = null;
        int minDist = Integer.MAX_VALUE;
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            if (u == null || !u.isAlive() || !u.team.equals(aiTeam)) continue;
            if (canMove(u, target, units)) {
                int d = Math.abs(u.gridX - target.gridX) + Math.abs(u.gridY - target.gridY);
                if (d < minDist) {
                    minDist = d;
                    close = u;
                }
            }
        }
        return close;
    }

    // 이동 가능 여부 판정
    private static boolean canMove(Unit actor, Unit target, Array<Unit> units) {
        int dx = Integer.compare(target.gridX, actor.gridX);
        int dy = Integer.compare(target.gridY, actor.gridY);
        return (dx != 0 && BoardManager.canMoveTo(actor, actor.gridX + dx, actor.gridY, units)) ||
            (dy != 0 && BoardManager.canMoveTo(actor, actor.gridX, actor.gridY + dy, units));
    }

    // 타겟 방향으로 유닛 이동 실행
    private static void moveUnit(Unit actor, Unit target, Array<Unit> units) {
        int dx = Integer.compare(target.gridX, actor.gridX);
        int dy = Integer.compare(target.gridY, actor.gridY);
        if (dx != 0 && BoardManager.canMoveTo(actor, actor.gridX + dx, actor.gridY, units))
            actor.setPosition(actor.gridX + dx, actor.gridY);
        else if (dy != 0 && BoardManager.canMoveTo(actor, actor.gridX, actor.gridY + dy, units))
            actor.setPosition(actor.gridX, actor.gridY + dy);
    }

    // 체력이 낮은 아군 미끼 탐색
    private static Unit getBait(Array<Unit> units, String aiTeam) {
        Unit candidate = null;
        float minR = 2.0f;
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            if (u != null && u.isAlive() && u.team.equals(aiTeam) && !"왕의 위엄".equals(u.stat.skillName())) {
                float r = (float) u.currentHp / u.stat.hp();
                if (r < minR) {
                    minR = r;
                    candidate = u;
                }
            }
        }
        return candidate;
    }

    // 특정 유닛에게 가장 적합한 공격 대상 탐색
    private static Unit getTarget(Unit actor, Array<Unit> units) {
        Unit best = null;
        int maxS = -10000;
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            if (u == null || !u.isAlive() || u.team.equals(actor.team)) continue;
            int s = evalAction(actor, u, units);
            if (s > maxS) {
                maxS = s;
                best = u;
            }
        }
        return best;
    }

    // 협공을 위한 아군 탐색
    private static Unit getAlly(Array<Unit> units, String aiTeam, Unit bait, Unit target) {
        Unit best = null;
        int minDist = Integer.MAX_VALUE;
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            if (u != null && u.isAlive() && u.team.equals(aiTeam) && u != bait && !"왕의 위엄".equals(u.stat.skillName())) {
                if (canMove(u, target, units)) {
                    int d = Math.abs(u.gridX - target.gridX) + Math.abs(u.gridY - target.gridY);
                    if (d < minDist) {
                        minDist = d;
                        best = u;
                    }
                }
            }
        }
        return best;
    }

    // 이번 턴의 성향 결정
    private static String getStrategy() {
        float roll = MathUtils.random(0f, 100f);
        if (roll < 20) return "암살형";
        if (roll < 50) return "희생형";
        return "고효율";
    }
}
