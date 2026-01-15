package com.hades.game.logic;

import com.badlogic.gdx.utils.Array;
import com.hades.game.entities.Unit;
import com.badlogic.gdx.math.MathUtils;
import com.hades.game.screens.BattleScreen;

/*
[클래스 역할] AI의 의사결정과 실행 단계를 처리합니다.
1턴 1유닛 규칙을 준수하며, 영웅의 상위 판단 로직과 병사의 확률적 전략을 결합했습니다.
*/
public class AILogic {
    private static final int MAX_ITER = 50;

    /*
    [메서드 설명] AI 턴의 메인 컨트롤러입니다.
    영웅의 위기 상황을 먼저 체크한 후, 안전할 경우 병사들에게 행동권을 넘깁니다.
    */
    public static void processAITurn(Array<Unit> units, String aiTeam, TurnManager turnManager, Object screenObj) {
        try {
            Unit hero = getHero(units, aiTeam);
            Unit threat = (hero != null) ? getNearestEnemy(hero, units) : null;
            boolean heroActed = false;

            // --- [1단계] 영웅의 상위 판단 (사거리 + 1 내에 적 존재 시) ---
            if (hero != null && hero.isAlive() && threat != null) {
                int distToThreat = Math.abs(hero.gridX - threat.gridX) + Math.abs(hero.gridY - threat.gridY);

                // 적이 영웅의 사거리 근방까지 접근한 위기 상황
                if (distToThreat <= hero.stat.range() + 1) {
                    float heroRoll = MathUtils.random(0f, 100f);
                    System.out.println("[영웅 위기] " + hero.name + " 근처 적 감지! (거리: " + distToThreat + ")");

                    if (heroRoll < 50) { // 50% 확률: 후퇴
                        System.out.println("[영웅 판단] 50% 확률: 안전을 위해 후퇴합니다.");
                        moveAwayFrom(hero, threat, units);
                        heroActed = true;
                    } else if (heroRoll < 80) { // 30% 확률: 엄호 대기
                        System.out.println("[영웅 판단] 30% 확률: 제자리에서 아군의 엄호를 기다립니다.");
                        // 아무것도 하지 않고 대기 (시간 제한에 의해 턴 종료 유도)
                        heroActed = true;
                    } else { // 20% 확률: 반격 돌진
                        System.out.println("[영웅 판단] 20% 확률: 위협을 직접 제거하기 위해 돌진합니다!");
                        moveUnit(hero, threat, units);
                        heroActed = true;
                    }

                    // 영웅이 직접 행동(이동/대기/공격)을 결정했다면 턴의 메인 액션을 종료합니다.
                    if (heroActed) {
                        finalizeAction(aiTeam, screenObj);
                        return;
                    }
                }
            }

            // --- [2단계] 평시 상황 (영웅 안전 시) ---
            // 영웅은 행동 후보에서 제외하고, 병사들만 확률적 전략에 따라 움직입니다.
            String strategy = getStrategy();
            Unit bestActor = null;
            Unit bestTarget = null;

            // 전략별 병사(Value < 1000) 탐색
            if ("암살형".equals(strategy)) {
                bestTarget = getWeakest(units, aiTeam);
                if (bestTarget != null) bestActor = getClosest(units, aiTeam, bestTarget, true);
            } else if ("희생형".equals(strategy)) {
                Unit bait = getBait(units, aiTeam);
                if (bait != null) {
                    bestTarget = getTarget(bait, units, aiTeam);
                    if (bestTarget != null) bestActor = getAlly(units, aiTeam, bait, bestTarget, true);
                }
            }

            // 전략에서 배우를 못 찾았거나 '고효율'인 경우 전수 조사 (병사 전용)
            if (bestActor == null) {
                Object[] soldierPair = getBestByFilter(units, aiTeam, true);
                bestActor = (Unit) soldierPair[0];
                bestTarget = (Unit) soldierPair[1];
            }

            // 최종 실행 (병사가 움직임)
            if (bestActor != null && bestTarget != null) {
                System.out.println("[AI 결정] " + bestActor.name + " -> " + bestTarget.name + " (" + strategy + ")");
                moveUnit(bestActor, bestTarget, units);
                finalizeAction(aiTeam, screenObj);
            } else {
                System.out.println("[AI 대기] 움직일 수 있는 병사가 없어 대기합니다.");
            }

        } catch (Exception e) {
            System.err.println("[AI 로직 에러] " + e.getMessage());
        } finally {
            // 시간 제한이 있더라도 명시적으로 턴을 종료하여 흐름을 유지합니다.
            turnManager.endTurn();
        }
    }

    // 행동 후 자동 공격을 처리하는 공통 메서드
    private static void finalizeAction(String aiTeam, Object screenObj) {
        if (screenObj instanceof BattleScreen) {
            ((BattleScreen) screenObj).processAutoAttack(aiTeam);
        }
    }

    // 적과 반대 방향으로 한 칸 이동합니다 (후퇴).
    private static void moveAwayFrom(Unit actor, Unit threat, Array<Unit> units) {
        int dx = Integer.compare(actor.gridX, threat.gridX);
        int dy = Integer.compare(actor.gridY, threat.gridY);

        // 적의 반대 방향 좌표 (멀어지는 방향)
        int targetX = actor.gridX + (dx != 0 ? (dx > 0 ? 1 : -1) : 0);
        int targetY = actor.gridY + (dy != 0 ? (dy > 0 ? 1 : -1) : 0);

        if (BoardManager.canMoveTo(actor, targetX, targetY, units)) {
            actor.setPosition(targetX, targetY);
        }
    }

    // 유저님의 확률 로직: 전략을 결정합니다.
    private static String getStrategy() {
        float roll = MathUtils.random(0f, 100f);
        if (roll < 20) return "암살형";
        if (roll < 50) return "희생형";
        return "고효율";
    }

    // 이번 팀의 영웅(King) 유닛을 찾습니다.
    private static Unit getHero(Array<Unit> units, String aiTeam) {
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            if (u != null && u.isAlive() && u.team.equals(aiTeam) && u.stat.value() >= 1000) return u;
        }
        return null;
    }

    // 유닛과 가장 가까운 적을 찾습니다.
    private static Unit getNearestEnemy(Unit actor, Array<Unit> units) {
        Unit nearest = null;
        int minDist = Integer.MAX_VALUE;
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            if (u != null && u.isAlive() && !u.team.equals(actor.team)) {
                int d = Math.abs(actor.gridX - u.gridX) + Math.abs(actor.gridY - u.gridY);
                if (d < minDist) {
                    minDist = d;
                    nearest = u;
                }
            }
        }
        return nearest;
    }

    // 필터(병사 전용 여부)에 따라 최선의 유닛 쌍을 찾습니다.
    private static Object[] getBestByFilter(Array<Unit> units, String aiTeam, boolean isSoldierOnly) {
        Unit bestA = null, bestT = null;
        float maxS = -99999f;
        for (int i = 0; i < units.size; i++) {
            Unit a = units.get(i);
            if (a == null || !a.isAlive() || !a.team.equals(aiTeam)) continue;
            if (isSoldierOnly && a.stat.value() >= 1000) continue;

            for (int j = 0; j < units.size; j++) {
                Unit t = units.get(j);
                if (t == null || !t.isAlive() || t.team.equals(aiTeam)) continue;
                if (canMove(a, t, units)) {
                    float s = evalAction(a, t, units);
                    if (s > maxS) {
                        maxS = s; bestA = a; bestT = t;
                    }
                }
            }
        }
        return new Object[]{bestA, bestT};
    }

    // 액션 효율성을 평가합니다.
    private static float evalAction(Unit actor, Unit target, Array<Unit> units) {
        int dist = Math.abs(actor.gridX - target.gridX) + Math.abs(actor.gridY - target.gridY);
        float score = 0;
        if (dist <= actor.stat.range()) {
            score += (target.currentHp <= actor.stat.atk()) ? (target.stat.value() * 5) : target.stat.value();
        } else {
            score += (20 - dist);
        }
        return score;
    }

    // --- 전략 보조 메서드들 ---

    private static Unit getWeakest(Array<Unit> units, String aiTeam) {
        Unit weak = null;
        int minHp = Integer.MAX_VALUE;
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            if (u != null && u.isAlive() && !u.team.equals(aiTeam)) {
                if (u.currentHp < minHp) { minHp = u.currentHp; weak = u; }
            }
        }
        return weak;
    }

    private static Unit getClosest(Array<Unit> units, String aiTeam, Unit target, boolean soldierOnly) {
        Unit close = null;
        int minDist = Integer.MAX_VALUE;
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            if (u != null && u.isAlive() && u.team.equals(aiTeam)) {
                if (soldierOnly && u.stat.value() >= 1000) continue;
                int d = Math.abs(u.gridX - target.gridX) + Math.abs(u.gridY - target.gridY);
                if (d < minDist && canMove(u, target, units)) { minDist = d; close = u; }
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
                if (r < minR) { minR = r; candidate = u; }
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
                if (s > maxS) { maxS = s; best = u; }
            }
        }
        return best;
    }

    private static Unit getAlly(Array<Unit> units, String aiTeam, Unit bait, Unit target, boolean soldierOnly) {
        Unit best = null;
        int minDist = Integer.MAX_VALUE;
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            if (u != null && u.isAlive() && u.team.equals(aiTeam) && u != bait) {
                if (soldierOnly && u.stat.value() >= 1000) continue;
                if (canMove(u, target, units)) {
                    int d = Math.abs(u.gridX - target.gridX) + Math.abs(u.gridY - target.gridY);
                    if (d < minDist) { minDist = d; best = u; }
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
}
