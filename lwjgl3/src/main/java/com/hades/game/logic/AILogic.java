package com.hades.game.logic;

import com.badlogic.gdx.utils.Array;
import com.hades.game.entities.Unit;
import com.badlogic.gdx.math.MathUtils;
import com.hades.game.screens.BattleScreen;
import com.hades.game.constants.GameConfig;
import com.hades.game.constants.SkillData;
import com.hades.game.view.GameUI;
import java.util.Comparator;

// Chess Olympus: HADES vs ZEUS - GDX 이터레이터 중첩 해결 및 지능형 AI
public class AILogic {

    private static final float DIFFICULTY_FACTOR = 1.0f;
    private static final Array<Integer> recentUnitIds = new Array<>();
    private static final Array<MoveCandidate> candidatesPool = new Array<>();

    private static class MoveCandidate {
        Unit actor;
        int x, y;
        float score;

        void set(Unit actor, int x, int y, float score) {
            this.actor = actor;
            this.x = x;
            this.y = y;
            this.score = score;
        }
    }

    static {
        for(int i = 0; i < 500; i++) candidatesPool.add(new MoveCandidate());
    }

    public static void processAITurn(Array<Unit> units, String aiTeam, TurnManager turnManager, Object screenObj) {
        try {
            String strategy = determineStrategy();
            System.out.println("[AI Thought] Strategy: " + strategy);

            MoveCandidate best = findGlobalBestMove(units, aiTeam, strategy);

            if (best != null && best.actor != null) {
                recordAction(best.actor.hashCode());
                checkAndReserveSkill(best.actor, best.x, best.y, units);
                best.actor.setPosition(best.x, best.y);

                String reserved = best.actor.stat.getReservedSkill();
                if (reserved != null && !reserved.equals("기본 공격")) {
                    GameUI ui = (screenObj instanceof BattleScreen) ? ((BattleScreen) screenObj).getGameUI() : null;
                    SkillManager.executeSkill(best.actor, reserved, units, ui, best.actor.team, screenObj);
                }

                if (screenObj instanceof BattleScreen) {
                    ((BattleScreen) screenObj).processMoveEnd(best.actor);
                }
            }
        } catch (Exception e) {
            System.err.println("[AI Error] " + e.getMessage());
            e.printStackTrace();
        } finally {
            turnManager.endTurn();
        }
    }

    private static String determineStrategy() {
        float roll = MathUtils.random(0f, 100f);
        if (roll < 50f) return "EFFICIENCY";
        if (roll < 85f) return "ASSASSIN";
        return "DEFENSIVE_COOP";
    }

    private static void recordAction(int unitHash) {
        recentUnitIds.add(unitHash);
        if (recentUnitIds.size > 2) recentUnitIds.removeIndex(0);
    }

    private static MoveCandidate findGlobalBestMove(Array<Unit> units, String aiTeam, String strategy) {
        int candidateIdx = 0;

        // libGDX Array의 중첩 이터레이터 에러 방지를 위해 인덱스 for문 사용
        for (int i = 0; i < units.size; i++) {
            Unit actor = units.get(i);
            if (actor == null || !actor.isAlive() || !aiTeam.equals(actor.team)) continue;

            for (int x = 0; x < GameConfig.BOARD_WIDTH; x++) {
                for (int y = 0; y < GameConfig.BOARD_HEIGHT; y++) {
                    if (!BoardManager.canMoveTo(actor, x, y, units)) continue;

                    float score = calculateFinalScore(actor, x, y, units, aiTeam, strategy);

                    if (candidateIdx < candidatesPool.size) {
                        candidatesPool.get(candidateIdx).set(actor, x, y, score);
                        candidateIdx++;
                    }
                }
            }
        }

        if (candidateIdx == 0) return null;

        // 정렬을 위해 임시 배열 크기 조정
        Array<MoveCandidate> activeCandidates = new Array<>();
        for(int i = 0; i < candidateIdx; i++) {
            activeCandidates.add(candidatesPool.get(i));
        }

        activeCandidates.sort((c1, c2) -> Float.compare(c2.score, c1.score));

        int poolLimit = Math.min(activeCandidates.size, 3);
        return activeCandidates.get(MathUtils.random(0, poolLimit - 1));
    }

    private static float calculateFinalScore(Unit actor, int tx, int ty, Array<Unit> units, String aiTeam, String strategy) {
        float score = 0f;
        SkillData.Skill mySkill = SkillData.get(actor.stat.skillName());
        int range = mySkill.range;

        if (recentUnitIds.contains(actor.hashCode(), false)) score -= 40000f;

        float potentialDamageTaken = 0;
        int threatCount = 0;

        // 내부 루프도 인덱스 for문으로 교체하여 안전성 확보
        for (int i = 0; i < units.size; i++) {
            Unit enemy = units.get(i);
            if (enemy == null || !enemy.isAlive() || aiTeam.equals(enemy.team)) continue;

            int dist = Math.abs(tx - enemy.gridX) + Math.abs(ty - enemy.gridY);

            if (dist <= enemy.stat.move() + enemy.stat.range()) {
                threatCount++;
                potentialDamageTaken += enemy.stat.atk();
            }

            if (dist <= range) {
                float targetValue = (enemy.unitClass == Unit.UnitClass.HERO) ? 50000f : 10000f;
                score += targetValue;

                int predictedDmg = (int)(actor.getPower(true) * mySkill.power);
                if (enemy.currentHp <= predictedDmg) score += 100000f;

                float hpPercent = (float) enemy.currentHp / enemy.stat.hp();
                score += (1.0f - hpPercent) * 20000f;
            }
        }

        if (actor.unitClass == Unit.UnitClass.HERO) {
            score -= (potentialDamageTaken * 15f);
            if (threatCount > 2) score -= 60000f;
        } else {
            score -= (potentialDamageTaken * 3f);
        }

        score += getForwardBias(aiTeam, ty);
        return score * DIFFICULTY_FACTOR;
    }

    private static void checkAndReserveSkill(Unit actor, int tx, int ty, Array<Unit> units) {
        String skillName = actor.stat.skillName();
        SkillData.Skill skill = SkillData.get(skillName);
        for (int i = 0; i < units.size; i++) {
            Unit enemy = units.get(i);
            if (enemy != null && enemy.isAlive() && !enemy.team.equals(actor.team)) {
                int dist = Math.abs(tx - enemy.gridX) + Math.abs(ty - enemy.gridY);
                if (dist <= skill.range) {
                    actor.stat.setReservedSkill(skillName);
                    break;
                }
            }
        }
    }

    private static float getForwardBias(String aiTeam, int ty) {
        if ("ZEUS".equalsIgnoreCase(aiTeam)) return ty * 500f;
        return (GameConfig.BOARD_HEIGHT - 1 - ty) * 500f;
    }
}
