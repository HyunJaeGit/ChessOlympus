package com.hades.game.logic;

import com.badlogic.gdx.utils.Array;
import com.hades.game.entities.Unit;
import com.badlogic.gdx.math.MathUtils;
import com.hades.game.screens.BattleScreen;
import com.hades.game.constants.GameConfig;

public class AILogic {

    public static void processAITurn(Array<Unit> units, String aiTeam, TurnManager turnManager, Object screenObj) {
        try {
            Unit hero = getHero(units, aiTeam);
            Unit threat = (hero != null) ? getNearestEnemy(hero, units) : null;
            boolean heroActed = false;

            // --- [1단계] 영웅의 판단 로직 ---
            if (hero != null && hero.isAlive() && threat != null) {
                int distToThreat = Math.abs(hero.gridX - threat.gridX) + Math.abs(hero.gridY - threat.gridY);

                if (distToThreat <= hero.stat.range() + 1) {
                    float heroRoll = MathUtils.random(0f, 100f);

                    if (heroRoll < 50) { // 후퇴
                        moveAwayFrom(hero, threat, units);
                        heroActed = true;
                    } else if (heroRoll < 80) { // 엄호 대기
                        // // 영웅이 대기를 선택하면 행동 완료(heroActed)를 false로 두어 병사들이 움직일 기회를 줍니다.
                        heroActed = false;
                    } else { // 반격 돌진
                        moveUnit(hero, threat, units);
                        heroActed = true;
                    }

                    // // 영웅이 이동이나 공격을 실행했다면 즉시 턴을 종료합니다.
                    if (heroActed) {
                        finalizeAction(aiTeam, screenObj);
                        return;
                    }
                }
            }

            // --- [2단계] 일반 유닛 혹은 영웅 대기 시의 행동 결정 ---
            String strategy = getStrategy();
            Unit bestActor = null;
            Unit bestTarget = null;

            // // 병사 전용 필터를 해제하여(false), 병사가 없으면 영웅이라도 최선의 행동을 하도록 변경했습니다.
            Object[] pair = getBestByFilter(units, aiTeam, false);
            bestActor = (Unit) pair[0];
            bestTarget = (Unit) pair[1];

            if (bestActor != null && bestTarget != null) {
                System.out.println("[AI 결정] " + bestActor.name + " -> " + bestTarget.name);
                moveUnit(bestActor, bestTarget, units);
                finalizeAction(aiTeam, screenObj);
            } else {
                System.out.println("[AI 대기] 모든 유닛 이동 불가");
            }

        } catch (Exception e) {
            System.err.println("[AI 에러] " + e.getMessage());
        } finally {
            turnManager.endTurn();
        }
    }

    // // 기병의 L자 이동 등 모든 병과의 이동 규칙을 전수 조사하여 최적의 칸을 찾는 메서드
    private static void moveUnit(Unit actor, Unit target, Array<Unit> units) {
        int bestX = actor.gridX;
        int bestY = actor.gridY;
        // // 현재 거리보다 더 가까워지는 칸을 찾기 위한 기준값
        int minDistance = Math.abs(actor.gridX - target.gridX) + Math.abs(actor.gridY - target.gridY);

        for (int x = 0; x < GameConfig.BOARD_WIDTH; x++) {
            for (int y = 0; y < GameConfig.BOARD_HEIGHT; y++) {
                // // BoardManager를 통해 유닛별(기병, 전차 등) 고유 이동 가능 여부를 확인합니다.
                if (BoardManager.canMoveTo(actor, x, y, units)) {
                    int dist = Math.abs(x - target.gridX) + Math.abs(y - target.gridY);
                    if (dist < minDistance) {
                        minDistance = dist;
                        bestX = x;
                        bestY = y;
                    }
                }
            }
        }
        actor.setPosition(bestX, bestY);
    }

    private static boolean canMove(Unit actor, Unit target, Array<Unit> units) {
        // // 단순히 한 칸 옆이 아니라, 실제로 갈 수 있는 유효한 칸이 하나라도 있는지 확인합니다.
        for (int x = 0; x < GameConfig.BOARD_WIDTH; x++) {
            for (int y = 0; y < GameConfig.BOARD_HEIGHT; y++) {
                if (BoardManager.canMoveTo(actor, x, y, units)) return true;
            }
        }
        return false;
    }

    private static void finalizeAction(String aiTeam, Object screenObj) {
        if (screenObj instanceof BattleScreen) {
            ((BattleScreen) screenObj).processAutoAttack(aiTeam);
        }
    }

    private static void moveAwayFrom(Unit actor, Unit threat, Array<Unit> units) {
        int dx = Integer.compare(actor.gridX, threat.gridX);
        int dy = Integer.compare(actor.gridY, threat.gridY);
        int targetX = actor.gridX + (dx != 0 ? (dx > 0 ? 1 : -1) : 0);
        int targetY = actor.gridY + (dy != 0 ? (dy > 0 ? 1 : -1) : 0);

        if (BoardManager.canMoveTo(actor, targetX, targetY, units)) {
            actor.setPosition(targetX, targetY);
        }
    }

    private static String getStrategy() {
        float roll = MathUtils.random(0f, 100f);
        if (roll < 20) return "암살형";
        if (roll < 50) return "희생형";
        return "고효율";
    }

    private static Unit getHero(Array<Unit> units, String aiTeam) {
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            if (u != null && u.isAlive() && u.team.equals(aiTeam) && u.unitClass == Unit.UnitClass.HERO) return u;
        }
        return null;
    }

    private static Unit getNearestEnemy(Unit actor, Array<Unit> units) {
        Unit nearest = null;
        int minDist = Integer.MAX_VALUE;
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            if (u != null && u.isAlive() && !u.team.equals(actor.team)) {
                int d = Math.abs(actor.gridX - u.gridX) + Math.abs(actor.gridY - u.gridY);
                if (d < minDist) { minDist = d; nearest = u; }
            }
        }
        return nearest;
    }

    private static Object[] getBestByFilter(Array<Unit> units, String aiTeam, boolean isSoldierOnly) {
        Unit bestA = null, bestT = null;
        float maxS = -99999f;
        for (int i = 0; i < units.size; i++) {
            Unit a = units.get(i);
            if (a == null || !a.isAlive() || !a.team.equals(aiTeam)) continue;
            if (isSoldierOnly && a.unitClass == Unit.UnitClass.HERO) continue;

            for (int j = 0; j < units.size; j++) {
                Unit t = units.get(j);
                if (t == null || !t.isAlive() || t.team.equals(aiTeam)) continue;
                if (canMove(a, t, units)) {
                    float s = evalAction(a, t, units);
                    if (s > maxS) { maxS = s; bestA = a; bestT = t; }
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
            score += (20 - dist);
        }
        return score;
    }

    // --- 전략 보조 메서드들 (기존 유지) ---
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
                if (soldierOnly && u.unitClass == Unit.UnitClass.HERO) continue;
                if (canMove(u, target, units)) {
                    int d = Math.abs(u.gridX - target.gridX) + Math.abs(u.gridY - target.gridY);
                    if (d < minDist) { minDist = d; close = u; }
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
            if (u != null && u.isAlive() && u.team.equals(aiTeam) && u.unitClass != Unit.UnitClass.HERO) {
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
                if (soldierOnly && u.unitClass == Unit.UnitClass.HERO) continue;
                if (canMove(u, target, units)) {
                    int d = Math.abs(u.gridX - target.gridX) + Math.abs(u.gridY - target.gridY);
                    if (d < minDist) { minDist = d; best = u; }
                }
            }
        }
        return best;
    }
}
