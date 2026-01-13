package com.hades.game.logic;

import com.badlogic.gdx.utils.Array;
import com.hades.game.entities.Unit;
import com.badlogic.gdx.math.MathUtils;
import com.hades.game.screens.BattleScreen;

/* AI 의사결정 및 유닛 이동을 담당하는 클래스입니다. */
public class AILogic {
    private static final int MAX_ITER = 50;

    /* [메서드 설명] AI의 턴을 처리합니다. 전략을 선택하고 유닛을 이동시킨 후 협공을 요청합니다. */
    public static void processAITurn(Array<Unit> units, String aiTeam, TurnManager turnManager, Object screenObj) {
        String strategy = getStrategy();
        System.out.println("\n=== [" + aiTeam + "] 전략: " + strategy + " ===");

        Unit actor = null;
        Unit target = null;

        // 1. 전략별 유닛 선발
        if ("암살형".equals(strategy)) {
            target = getWeakest(units, aiTeam);
            if (target != null) actor = getClosest(units, aiTeam, target);
        } else if ("희생형".equals(strategy)) {
            Unit bait = getBait(units, aiTeam);
            if (bait != null) {
                target = getTarget(bait, units);
                if (target != null) actor = getAlly(units, aiTeam, bait, target);
            }
        }

        // 2. 기본 고효율 쌍 선발
        if (actor == null) {
            Object[] best = getBest(units, aiTeam);
            actor = (Unit) best[0];
            target = (Unit) best[1];
        }

        // 3. 이동 및 협공 실행
        if (actor != null && target != null) {
            moveUnit(actor, target, units);
            if (screenObj instanceof BattleScreen) {
                ((BattleScreen) screenObj).processAutoAttack(aiTeam);
            }
        }

        turnManager.endTurn();
    }

    /* 최적의 타겟을 찾는 루프입니다. 인덱스 기반으로 중첩 루프 에러를 방지합니다. */
    private static Object[] getBest(Array<Unit> units, String aiTeam) {
        Unit bestA = null, bestT = null;
        int maxS = -10000;
        int count = 0;

        for (int i = 0; i < units.size; i++) {
            Unit a = units.get(i);
            if (a == null || a.currentHp <= 0 || !a.team.equals(aiTeam)) continue;

            for (int j = 0; j < units.size; j++) {
                if (++count > MAX_ITER) break;

                Unit t = units.get(j);
                if (t == null || t.currentHp <= 0 || t.team.equals(aiTeam)) continue;

                if (canMove(a, t, units)) {
                    int s = evalAction(a, t, units);
                    if (s > maxS) { maxS = s; bestA = a; bestT = t; }
                }
            }
            if (count > MAX_ITER) break;
        }
        return new Object[]{bestA, bestT};
    }

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

    /* 가장 체력이 낮은 적을 찾습니다. 인덱스 루프를 사용하여 안정성을 높였습니다. */
    private static Unit getWeakest(Array<Unit> units, String aiTeam) {
        Unit weak = null;
        int minHp = Integer.MAX_VALUE;
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            if (u.currentHp > 0 && !u.team.equals(aiTeam)) {
                if (u.currentHp < minHp) { minHp = u.currentHp; weak = u; }
            }
        }
        return weak;
    }

    private static Unit getClosest(Array<Unit> units, String aiTeam, Unit target) {
        Unit close = null;
        int minDist = Integer.MAX_VALUE;
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            if (u.currentHp <= 0 || !u.team.equals(aiTeam)) continue;
            if (canMove(u, target, units)) {
                int d = Math.abs(u.gridX - target.gridX) + Math.abs(u.gridY - target.gridY);
                if (d < minDist) { minDist = d; close = u; }
            }
        }
        return close;
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

    private static Unit getBait(Array<Unit> units, String aiTeam) {
        Unit candidate = null;
        float minR = 2.0f;
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            if (u.currentHp > 0 && u.team.equals(aiTeam) && !"왕의 위엄".equals(u.stat.skillName())) {
                float r = (float) u.currentHp / u.stat.hp();
                if (r < minR) { minR = r; candidate = u; }
            }
        }
        return candidate;
    }

    private static Unit getTarget(Unit actor, Array<Unit> units) {
        Unit best = null;
        int maxS = -10000;
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            if (u.currentHp <= 0 || u.team.equals(actor.team)) continue;
            int s = evalAction(actor, u, units);
            if (s > maxS) { maxS = s; best = u; }
        }
        return best;
    }

    private static Unit getAlly(Array<Unit> units, String aiTeam, Unit bait, Unit target) {
        Unit best = null;
        int minDist = Integer.MAX_VALUE;
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            if (u.currentHp > 0 && u.team.equals(aiTeam) && u != bait && !"왕의 위엄".equals(u.stat.skillName())) {
                if (canMove(u, target, units)) {
                    int d = Math.abs(u.gridX - target.gridX) + Math.abs(u.gridY - target.gridY);
                    if (d < minDist) { minDist = d; best = u; }
                }
            }
        }
        return best;
    }

    private static String getStrategy() {
        float roll = MathUtils.random(0f, 100f);
        if (roll < 20) return "암살형";
        if (roll < 50) return "희생형";
        return "고효율";
    }
}
