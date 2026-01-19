package com.hades.game.logic;

import com.badlogic.gdx.utils.Array;
import com.hades.game.entities.Unit;
import com.badlogic.gdx.math.MathUtils;
import com.hades.game.screens.BattleScreen;
import com.hades.game.constants.GameConfig;
import com.hades.game.constants.SkillData;

// AI의 의사결정과 행동을 관리하는 클래스입니다.
public class AILogic {

    public static void processAITurn(Array<Unit> units, String aiTeam, TurnManager turnManager, Object screenObj) {
        try {
            String strategy = getStrategy();
            System.out.println("[AI Strategy] " + strategy);

            Object[] bestMove = findGlobalBestMove(units, aiTeam, strategy);
            Unit actor = (Unit) bestMove[0];
            int targetX = (int) bestMove[1];
            int targetY = (int) bestMove[2];

            if (actor != null) {
                if (actor.unitClass == Unit.UnitClass.HERO) {
                    checkAndReserveSkill(actor, targetX, targetY, units);
                }

                System.out.println("[AI Action] " + actor.name + " move to (" + targetX + "," + targetY + ")");
                actor.setPosition(targetX, targetY);

                if (screenObj instanceof BattleScreen) {
                    ((BattleScreen) screenObj).processMoveEnd(actor);
                }
            }
        } catch (Exception e) {
            System.err.println("[AI Error] Logical exception");
            e.printStackTrace();
        } finally {
            turnManager.endTurn();
        }
    }

    private static String getStrategy() {
        float roll = MathUtils.random(0f, 100f);
        if (roll < 60) return "EFFICIENCY";
        if (roll < 85) return "SACRIFICE";
        return "ASSASSIN";
    }

    private static Object[] findGlobalBestMove(Array<Unit> units, String aiTeam, String strategy) {
        Unit bestActor = null;
        int bestX = -1, bestY = -1;
        float maxScore = -999999f;

        for (int i = 0; i < units.size; i++) {
            Unit actor = units.get(i);
            if (actor == null || !actor.isAlive() || !actor.team.equals(aiTeam)) continue;

            for (int x = 0; x < GameConfig.BOARD_WIDTH; x++) {
                for (int y = 0; y < GameConfig.BOARD_HEIGHT; y++) {
                    if (BoardManager.canMoveTo(actor, x, y, units)) {
                        float totalScore = 0;

                        // 1. 적군과의 상호작용 점수 (전체 적 대상 합산)
                        for (int j = 0; j < units.size; j++) {
                            Unit enemy = units.get(j);
                            if (enemy == null || !enemy.isAlive() || enemy.team.equals(aiTeam)) continue;
                            totalScore += calculateMoveScore(actor, x, y, enemy, units, strategy);
                        }

                        // 2. 군단 형성 및 대형 유지 (좌우 방황 방지)
                        int nearbyAllies = 0;
                        for (int k = 0; k < units.size; k++) {
                            Unit ally = units.get(k);
                            if (ally != null && ally.isAlive() && ally.team.equals(aiTeam) && ally != actor) {
                                // 인접한 아군이 있다면 가산점
                                if (Math.abs(x - ally.gridX) <= 1 && Math.abs(y - ally.gridY) <= 1) {
                                    nearbyAllies++;
                                }
                            }
                        }
                        totalScore += (nearbyAllies * 3000);

                        // 3. 전진 의지 및 중앙 점유 (X축 방황 억제)
                        // 중앙(X=4)에 가까울수록, 그리고 상대 진영(Y 감소 방향)으로 전진할수록 소폭 가산점
                        totalScore += (5 - Math.abs(x - 4)) * 500;

                        // 제자리 유지 관성 (의미 없는 좌우 이동 억제)
                        if (x == actor.gridX && y == actor.gridY) totalScore += 2000;

                        if (totalScore > maxScore) {
                            maxScore = totalScore;
                            bestActor = actor;
                            bestX = x;
                            bestY = y;
                        }
                    }
                }
            }
        }
        return new Object[]{bestActor, bestX, bestY};
    }

    private static float calculateMoveScore(Unit actor, int tx, int ty, Unit target, Array<Unit> units, String strategy) {
        float score = 0;
        int dist = Math.abs(tx - target.gridX) + Math.abs(ty - target.gridY);
        int myRange = actor.stat.range();

        if (actor.unitClass == Unit.UnitClass.HERO) {
            myRange = SkillData.get(actor.stat.skillName()).range;
        }

        // 1. 공격적 가치 (UnitData.Stat.value 반영)
        if (dist <= myRange) {
            // 적 처치 시 해당 적의 가치만큼 보너스
            if (target.currentHp <= actor.stat.atk()) {
                score += 20000 + (target.stat.value() * 20);
            } else {
                // 공격 가능한 위치라면 대상의 가치에 비례해 점수 부여
                score += 5000 + (target.stat.value() * 5);
            }
            if (dist == myRange) score += 4000; // 사거리 끝 유지
        } else {
            // 전진 유도 가중치: 적의 가치가 높을수록 더 적극적으로 다가감
            score += (25 - dist) * (target.stat.value() * 0.5f);
        }

        // 2. 위험도 및 협공 회피 (내 가치 보호)
        float danger = 0;
        int attackers = 0;
        for (int i = 0; i < units.size; i++) {
            Unit enemy = units.get(i);
            if (enemy == null || !enemy.isAlive() || enemy.team.equals(actor.team)) continue;
            int reach = enemy.stat.move() + enemy.stat.range();
            if (Math.abs(tx - enemy.gridX) + Math.abs(ty - enemy.gridY) <= reach) {
                danger += enemy.stat.atk();
                attackers++;
            }
        }

        // 협공 시 내 유닛 가치에 비례해 대폭 감점 (고가치 유닛 보호)
        if (attackers >= 2) {
            score -= (danger * 2.0f) + (actor.stat.value() * 10);
        } else {
            score -= (danger * 0.5f);
        }

        // 3. 전략별 특화
        if (strategy.equals("SACRIFICE") && actor.unitClass != Unit.UnitClass.HERO) {
            // 저가치 유닛(병사)이 몸빵을 하도록 전진 점수 추가
            score += (1000 - actor.stat.value()) * 10;
        }

        // 4. 고립 방지 (범위 2칸)
        boolean isSafe = false;
        for (int i = 0; i < units.size; i++) {
            Unit ally = units.get(i);
            if (ally != null && ally.isAlive() && ally.team.equals(actor.team) && ally != actor) {
                if (Math.abs(tx - ally.gridX) <= 2 && Math.abs(ty - ally.gridY) <= 2) {
                    isSafe = true;
                    break;
                }
            }
        }
        if (!isSafe) score -= 20000;

        return score;
    }

    private static void checkAndReserveSkill(Unit actor, int tx, int ty, Array<Unit> units) {
        String skillName = actor.stat.skillName();
        SkillData.Skill skill = SkillData.get(skillName);
        for (int i = 0; i < units.size; i++) {
            Unit enemy = units.get(i);
            if (enemy != null && enemy.isAlive() && !enemy.team.equals(actor.team)) {
                if (Math.abs(tx - enemy.gridX) + Math.abs(ty - enemy.gridY) <= skill.range) {
                    actor.stat.setReservedSkill(skillName);
                    break;
                }
            }
        }
    }
}
