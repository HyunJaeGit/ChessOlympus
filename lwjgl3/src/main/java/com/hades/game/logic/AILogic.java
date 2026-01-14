package com.hades.game.logic;

import com.badlogic.gdx.utils.Array;
import com.hades.game.entities.Unit;
import com.badlogic.gdx.math.MathUtils;
import com.hades.game.screens.BattleScreen;

/*
[클래스 역할] AI의 의사결정과 실행 단계를 처리합니다.
LibGDX Array의 중첩 이터레이터 에러를 방지하기 위해 모든 루프를 인덱스 방식으로 구현했습니다.
*/
public class AILogic {
    private static final int MAX_ITER = 50;

    /*
    [메서드 설명] AI 턴을 처리하는 메인 루프입니다.
    에러 발생 시 턴이 멈추지 않도록 예외 처리가 강화되었습니다.
    */
    public static void processAITurn(Array<Unit> units, String aiTeam, TurnManager turnManager, Object screenObj) {
        try {
            String strategy = getStrategy();
            System.out.println("\n=== [" + aiTeam + "] 전략: " + strategy + " ===");

            Unit bestActor = null;
            Unit bestTarget = null;

            // 전략별 유닛 탐색
            if ("암살형".equals(strategy)) {
                bestTarget = getWeakest(units, aiTeam);
                if (bestTarget != null) bestActor = getClosest(units, aiTeam, bestTarget);
            } else if ("희생형".equals(strategy)) {
                Unit bait = getBait(units, aiTeam);
                if (bait != null) {
                    bestTarget = getTarget(bait, units, aiTeam);
                    if (bestTarget != null) bestActor = getAlly(units, aiTeam, bait, bestTarget);
                }
            }

            // 위 전략에서 찾지 못했거나 '고효율'인 경우 전수 조사
            if (bestActor == null) {
                Object[] bestPair = getBest(units, aiTeam);
                bestActor = (Unit) bestPair[0];
                bestTarget = (Unit) bestPair[1];
            }

            // 최종 실행
            if (bestActor != null && bestTarget != null && bestActor.isAlive() && bestTarget.isAlive()) {
                System.out.println("[AI 결정] " + bestActor.name + " -> " + bestTarget.name + " 타겟팅");
                moveUnit(bestActor, bestTarget, units);

                if (screenObj instanceof BattleScreen) {
                    ((BattleScreen) screenObj).processAutoAttack(aiTeam);
                }
            } else {
                System.out.println("[AI 대기] 적절한 타겟이 없어 대기합니다.");
            }

        } catch (Exception e) {
            System.err.println("[AI 로직 에러] " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 어떤 상황에서도 턴은 종료되어야 게임이 멈추지 않습니다.
            turnManager.endTurn();
        }
    }

    // 인덱스 기반 for문을 사용하여 중첩 이터레이터 에러를 원천 차단합니다.
    private static Object[] getBest(Array<Unit> units, String aiTeam) {
        Unit bestA = null, bestT = null;
        float maxS = -99999f;
        int count = 0;

        for (int i = 0; i < units.size; i++) {
            Unit a = units.get(i);
            if (a == null || !a.isAlive() || !a.team.equals(aiTeam)) continue;

            for (int j = 0; j < units.size; j++) {
                if (++count > MAX_ITER) break;
                Unit t = units.get(j);
                if (t == null || !t.isAlive() || t.team.equals(aiTeam)) continue;

                if (canMove(a, t, units)) {
                    float s = evalAction(a, t, units);
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

    private static float evalAction(Unit actor, Unit target, Array<Unit> units) {
        int dist = Math.abs(actor.gridX - target.gridX) + Math.abs(actor.gridY - target.gridY);
        float score = 0;

        if (dist <= actor.stat.range()) {
            score += (target.currentHp <= actor.stat.atk()) ? (target.stat.value() * 5) : target.stat.value();
        } else {
            score += (10 - dist);
        }

        // 보스 보호 및 병사 진격 로직
        if (actor.stat.value() >= 1000) {
            if (dist > actor.stat.range()) score -= 10000f; // 보스는 사거리 밖이면 대기
            else score += 500f;
        } else {
            score += 2000f; // 병사 우선 진격
            if (target.stat.value() >= 1000) score += 500f;
        }

        return score;
    }

    // --- 인덱스 기반으로 교체된 보조 메서드들 ---

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

    private static Unit getClosest(Array<Unit> units, String aiTeam, Unit target) {
        Unit close = null;
        int minDist = Integer.MAX_VALUE;
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            if (u != null && u.isAlive() && u.team.equals(aiTeam)) {
                if (canMove(u, target, units)) {
                    int d = Math.abs(u.gridX - target.gridX) + Math.abs(u.gridY - target.gridY);
                    if (d < minDist) {
                        minDist = d;
                        close = u;
                    }
                }
            }
        }
        return close;
    }

    private static Unit getBait(Array<Unit> units, String aiTeam) {
        Unit candidate = null;
        float minR = 2.0f;
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            if (u != null && u.isAlive() && u.team.equals(aiTeam) && u.stat.value() < 1000) {
                float r = (float) u.currentHp / u.stat.hp();
                if (r < minR) {
                    minR = r;
                    candidate = u;
                }
            }
        }
        return candidate;
    }

    private static Unit getTarget(Unit actor, Array<Unit> units, String aiTeam) {
        Unit best = null;
        float maxS = -10000;
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            if (u != null && u.isAlive() && !u.team.equals(aiTeam)) {
                float s = evalAction(actor, u, units);
                if (s > maxS) {
                    maxS = s;
                    best = u;
                }
            }
        }
        return best;
    }

    private static Unit getAlly(Array<Unit> units, String aiTeam, Unit bait, Unit target) {
        Unit best = null;
        int minDist = Integer.MAX_VALUE;
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            if (u != null && u.isAlive() && u.team.equals(aiTeam) && u != bait && u.stat.value() < 1000) {
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

    private static boolean canMove(Unit actor, Unit target, Array<Unit> units) {
        int dx = Integer.compare(target.gridX, actor.gridX);
        int dy = Integer.compare(target.gridY, actor.gridY);
        return (dx != 0 && BoardManager.canMoveTo(actor, actor.gridX + dx, actor.gridY, units)) ||
            (dy != 0 && BoardManager.canMoveTo(actor, actor.gridX, actor.gridY + dy, units));
    }

    private static void moveUnit(Unit actor, Unit target, Array<Unit> units) {
        int dx = Integer.compare(target.gridX, actor.gridX);
        int dy = Integer.compare(target.gridY, actor.gridY);
        if (dx != 0 && BoardManager.canMoveTo(actor, actor.gridX + dx, actor.gridY, units))
            actor.setPosition(actor.gridX + dx, actor.gridY);
        else if (dy != 0 && BoardManager.canMoveTo(actor, actor.gridX, actor.gridY + dy, units))
            actor.setPosition(actor.gridX, actor.gridY + dy);
    }

    private static String getStrategy() {
        float roll = MathUtils.random(0f, 100f);
        if (roll < 20) return "암살형";
        if (roll < 50) return "희생형";
        return "고효율";
    }
}
