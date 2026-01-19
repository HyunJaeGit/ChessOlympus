package com.hades.game.logic;

import com.badlogic.gdx.utils.Array;
import com.hades.game.entities.Unit;
import com.badlogic.gdx.math.MathUtils;
import com.hades.game.screens.BattleScreen;
import com.hades.game.constants.GameConfig;
import com.hades.game.constants.SkillData;
import com.hades.game.view.GameUI;

// Chess Olympus AI: LibGDX Array Iterator 중첩 에러 방지 버전
public class AILogic {

    private static final float DIFFICULTY_FACTOR = 1.0f;
    private static final Array<Integer> recentUnitIds = new Array<>();
    private static final Array<MoveCandidate> candidatesPool = new Array<>();
    private static final Array<MoveCandidate> activeCandidates = new Array<>();

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
        for(int i = 0; i < 1000; i++) candidatesPool.add(new MoveCandidate());
    }

    public static void processAITurn(Array<Unit> units, String aiTeam, TurnManager turnManager, Object screenObj) {
        try {
            String strategy = determineStrategy();
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
        } finally {
            turnManager.endTurn();
        }
    }

    private static String determineStrategy() {
        float roll = MathUtils.random(0f, 100f);
        if (roll < 40f) return "EFFICIENCY";
        if (roll < 70f) return "ASSASSIN";
        return "DEFENSIVE";
    }

    private static void recordAction(int unitHash) {
        recentUnitIds.add(unitHash);
        if (recentUnitIds.size > 2) recentUnitIds.removeIndex(0);
    }

    // [수정] 향상된 for문(Iterator)을 모두 제거하고 인덱스 루프로 변경
    private static MoveCandidate findGlobalBestMove(Array<Unit> units, String aiTeam, String strategy) {
        int candidateIdx = 0;
        activeCandidates.clear();

        int halfBoard = GameConfig.BOARD_HEIGHT / 2;
        boolean isPlayerInMyTerritory = false;
        Unit mostVulnerableAlly = null;
        float lowestHpRatio = 1.1f;

        // 1. 상황 파악 루프 (인덱스 사용)
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            if (u == null || !u.isAlive()) continue;

            if (!u.team.equals(aiTeam)) {
                if (u.gridY >= halfBoard) isPlayerInMyTerritory = true;
            } else {
                if (isUnitUnderThreat(u, units, aiTeam)) {
                    float hpRatio = (float) u.currentHp / u.stat.hp();
                    if (hpRatio < lowestHpRatio) {
                        lowestHpRatio = hpRatio;
                        mostVulnerableAlly = u;
                    }
                }
            }
        }

        // 2. 전체 탐색 루프 (인덱스 사용)
        for (int i = 0; i < units.size; i++) {
            Unit actor = units.get(i);
            if (actor == null || !actor.isAlive() || !aiTeam.equals(actor.team)) continue;

            if (actor.unitClass == Unit.UnitClass.HERO && !isPlayerInMyTerritory) {
                if (!isUnitUnderThreat(actor, units, aiTeam) && !canHitEnemyFrom(actor, actor.gridX, actor.gridY, units)) {
                    continue;
                }
            }

            float unitBasePenalty = recentUnitIds.contains(actor.hashCode(), false) ? -30000f : 0f;

            for (int x = 0; x < GameConfig.BOARD_WIDTH; x++) {
                for (int y = 0; y < GameConfig.BOARD_HEIGHT; y++) {
                    if (!BoardManager.canMoveTo(actor, x, y, units)) continue;

                    float score = calculateFinalScore(actor, x, y, units, aiTeam, strategy) + unitBasePenalty;

                    if (mostVulnerableAlly != null && actor != mostVulnerableAlly) {
                        int distToAlly = Math.abs(x - mostVulnerableAlly.gridX) + Math.abs(y - mostVulnerableAlly.gridY);
                        if (distToAlly <= 5 && canHitEnemyFrom(actor, x, y, units)) {
                            score += (6 - distToAlly) * 5000f;
                        }
                    }

                    if (candidateIdx < candidatesPool.size) {
                        MoveCandidate c = candidatesPool.get(candidateIdx);
                        c.set(actor, x, y, score);
                        activeCandidates.add(c);
                        candidateIdx++;
                    }
                }
            }
        }

        if (activeCandidates.size == 0) return null;
        activeCandidates.sort((c1, c2) -> Float.compare(c2.score, c1.score));
        int poolLimit = Math.min(activeCandidates.size, 3);
        return activeCandidates.get(MathUtils.random(0, poolLimit - 1));
    }

    private static float calculateFinalScore(Unit actor, int tx, int ty, Array<Unit> units, String aiTeam, String strategy) {
        float score = 0f;
        SkillData.Skill mySkill = SkillData.get(actor.stat.skillName());
        int halfBoard = GameConfig.BOARD_HEIGHT / 2;
        boolean isOwnTerritory = ty >= halfBoard;

        float potentialDamageTaken = 0;
        int threatCount = 0;

        // 인덱스 루프 사용
        for (int i = 0; i < units.size; i++) {
            Unit enemy = units.get(i);
            if (enemy == null || !enemy.isAlive() || aiTeam.equals(enemy.team)) continue;

            int distToMe = Math.abs(tx - enemy.gridX) + Math.abs(ty - enemy.gridY);
            if (distToMe <= (enemy.stat.move() + enemy.stat.range())) {
                threatCount++;
                potentialDamageTaken += enemy.stat.atk();
            }

            float targetValue = (enemy.unitClass == Unit.UnitClass.HERO) ? 15000f : 8000f;
            if (strategy.equals("ASSASSIN") && enemy.unitClass == Unit.UnitClass.HERO) targetValue += 10000f;

            if (canHit(mySkill, tx, ty, enemy.gridX, enemy.gridY, actor.stat.range())) {
                score += targetValue;
                if (enemy.currentHp <= (int)(actor.getPower(true) * mySkill.power)) score += 20000f;
            }
        }

        if (threatCount >= 2) score += strategy.equals("ASSASSIN") ? -40000f : -100000f;
        if (!isOwnTerritory) score += (actor.unitClass == Unit.UnitClass.HERO) ? -120000f : -40000f;

        float survivalWeight = strategy.equals("DEFENSIVE") ? 20f : 8f;
        score -= (potentialDamageTaken * survivalWeight);

        return score * DIFFICULTY_FACTOR;
    }

    private static boolean canHitEnemyFrom(Unit actor, int tx, int ty, Array<Unit> units) {
        SkillData.Skill skill = SkillData.get(actor.stat.skillName());
        for (int i = 0; i < units.size; i++) {
            Unit enemy = units.get(i);
            if (enemy != null && enemy.isAlive() && !enemy.team.equals(actor.team)) {
                if (canHit(skill, tx, ty, enemy.gridX, enemy.gridY, actor.stat.range())) return true;
            }
        }
        return false;
    }

    private static boolean canHit(SkillData.Skill skill, int cx, int cy, int tx, int ty, int unitRange) {
        int dx = Math.abs(cx - tx);
        int dy = Math.abs(cy - ty);
        int dist = dx + dy;
        int effectiveRange = Math.max(skill.range, unitRange);
        if (dist == 0 || dist > effectiveRange) return false;
        switch (skill.shape) {
            case CROSS: case LINE: return (dx == 0 || dy == 0);
            case SQUARE: return (dx <= effectiveRange && dy <= effectiveRange);
            default: return dist <= effectiveRange;
        }
    }

    private static boolean isUnitUnderThreat(Unit unit, Array<Unit> units, String aiTeam) {
        for (int i = 0; i < units.size; i++) {
            Unit enemy = units.get(i);
            if (enemy != null && enemy.isAlive() && !enemy.team.equals(aiTeam)) {
                int dist = Math.abs(enemy.gridX - unit.gridX) + Math.abs(enemy.gridY - unit.gridY);
                if (dist <= (enemy.stat.move() + enemy.stat.range())) return true;
            }
        }
        return false;
    }

    private static void checkAndReserveSkill(Unit actor, int tx, int ty, Array<Unit> units) {
        SkillData.Skill skill = SkillData.get(actor.stat.skillName());
        for (int i = 0; i < units.size; i++) {
            Unit enemy = units.get(i);
            if (enemy != null && enemy.isAlive() && !enemy.team.equals(actor.team)) {
                if (canHit(skill, tx, ty, enemy.gridX, enemy.gridY, actor.stat.range())) {
                    actor.stat.setReservedSkill(actor.stat.skillName());
                    break;
                }
            }
        }
    }
}
