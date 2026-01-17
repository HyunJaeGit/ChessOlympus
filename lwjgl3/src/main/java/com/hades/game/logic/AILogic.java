package com.hades.game.logic;

import com.badlogic.gdx.utils.Array;
import com.hades.game.entities.Unit;
import com.badlogic.gdx.math.MathUtils;
import com.hades.game.screens.BattleScreen;
import com.hades.game.constants.GameConfig;
import com.hades.game.constants.SkillData;

public class AILogic {

    public static void processAITurn(Array<Unit> units, String aiTeam, TurnManager turnManager, Object screenObj) {
        try {
            String strategy = getStrategy();
            System.out.println("[AI Strategy] " + strategy); // AI_TEST_LOG

            Object[] bestMove = findBestMove(units, aiTeam, strategy);

            Unit actor = (Unit) bestMove[0];
            int targetX = (int) bestMove[1];
            int targetY = (int) bestMove[2];

            if (actor != null) {
                System.out.println("[AI Action] " + actor.name + " move to (" + targetX + "," + targetY + ")"); // AI_TEST_LOG
                actor.setPosition(targetX, targetY);
                finalizeAction(aiTeam, screenObj);
            } else {
                System.out.println("[AI Action] No movable units found."); // AI_TEST_LOG
            }

        } catch (Exception e) {
            System.err.println("[AI Error] Exception in AI Logic loop"); // AI_TEST_LOG
            e.printStackTrace();
        } finally {
            turnManager.endTurn();
        }
    }

    private static String getStrategy() {
        float roll = MathUtils.random(0f, 100f);
        // 내부 로직 비교를 위해 영문으로 전략명 변경
        if (roll < 70) return "EFFICIENCY";
        if (roll < 90) return "SACRIFICE";
        return "ASSASSIN";
    }

    private static Object[] findBestMove(Array<Unit> units, String aiTeam, String strategy) {
        Unit bestActor = null;
        Unit bestTargetUnit = null;
        int bestX = -1, bestY = -1;
        float maxScore = -999999f;

        for (int i = 0; i < units.size; i++) {
            Unit actor = units.get(i);
            if (actor == null || !actor.isAlive() || !actor.team.equals(aiTeam)) continue;

            for (int x = 0; x < GameConfig.BOARD_WIDTH; x++) {
                for (int y = 0; y < GameConfig.BOARD_HEIGHT; y++) {
                    if (BoardManager.canMoveTo(actor, x, y, units)) {
                        for (int j = 0; j < units.size; j++) {
                            Unit enemy = units.get(j);
                            if (enemy == null || !enemy.isAlive() || enemy.team.equals(aiTeam)) continue;

                            float score = calculateMoveScore(actor, x, y, enemy, units, strategy);
                            if (score > maxScore) {
                                maxScore = score;
                                bestActor = actor;
                                bestX = x;
                                bestY = y;
                                bestTargetUnit = enemy;
                            }
                        }
                    }
                }
            }
        }
        return new Object[]{bestActor, bestX, bestY, bestTargetUnit};
    }

    private static float calculateMoveScore(Unit actor, int tx, int ty, Unit target, Array<Unit> units, String strategy) {
        float score = 0;
        int dist = Math.abs(tx - target.gridX) + Math.abs(ty - target.gridY);
        boolean isActorHero = (actor.unitClass == Unit.UnitClass.HERO);
        boolean isTargetHero = (target.unitClass == Unit.UnitClass.HERO);

        // --- [1단계: 공용 판단 로직 및 병사 가중치] ---
        if (!isActorHero) {
            if (dist <= actor.stat.range() && target.currentHp <= actor.stat.atk()) {
                return 20000;
            }
            score += 3000;
        }

        // 전략 성향에 따른 기본 가중치 (getStrategy 영문명에 맞춤)
        switch (strategy) {
            case "EFFICIENCY":
                if (dist <= actor.stat.range()) score += 2000;
                if (dist > target.stat.range()) score += 1000;
                break;
            case "SACRIFICE":
                if (isTargetHero) score += (20 - dist) * 150;
                break;
            case "ASSASSIN":
                if (isTargetHero) score += 7000 - (dist * 200);
                break;
        }

        // --- [2단계: 영웅(보스) 전용 스킬 및 공격 지능] ---
        if (isActorHero) {
            String skillName = actor.stat.skillName();
            SkillData.Skill skill = SkillData.get(skillName);
            int expectedAtk = (int)(actor.stat.atk() * skill.power);
            int skillRange = skill.range;

            if (dist <= skillRange) {
                score += 18000;
                if (isTargetHero) score += 5000;
                if (target.currentHp <= expectedAtk) return 30000;
            }

            if (strategy.equals("EFFICIENCY")) {
                int currentDist = Math.abs(actor.gridX - target.gridX) + Math.abs(actor.gridY - target.gridY);
                if (skillRange >= 4 && currentDist <= skillRange) {
                    if (tx == actor.gridX && ty == actor.gridY) score += 12000;
                }
                if (currentDist > skillRange + 2) {
                    score -= 8000;
                } else {
                    if (skill.isAoE && dist <= skillRange) {
                        int enemiesHit = countNearbyEnemies(target.gridX, target.gridY, units, actor.team);
                        score += (enemiesHit * 3500);
                    }
                    if (dist == skillRange) score += 4000;
                }
            }
        }

        // --- [3단계: 위험도 감점] ---
        int danger = 0;
        for (Unit enemy : units) {
            if (enemy != null && enemy.isAlive() && !enemy.team.equals(actor.team)) {
                if (Math.abs(tx - enemy.gridX) + Math.abs(ty - enemy.gridY) <= enemy.stat.range()) {
                    danger++;
                }
            }
        }
        score -= (danger * (isActorHero ? 300 : 800));

        return score;
    }

    private static int countNearbyEnemies(int x, int y, Array<Unit> units, String myTeam) {
        int count = 0;
        for (Unit u : units) {
            if (u != null && u.isAlive() && !u.team.equals(myTeam)) {
                if (Math.abs(u.gridX - x) <= 1 && Math.abs(u.gridY - y) <= 1) {
                    count++;
                }
            }
        }
        return count;
    }

    private static void finalizeAction(String aiTeam, Object screenObj) {
        if (screenObj instanceof BattleScreen) {
            ((BattleScreen) screenObj).processAutoAttack(aiTeam);
        }
    }
}
