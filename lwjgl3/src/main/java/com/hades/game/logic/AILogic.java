package com.hades.game.logic;

import com.badlogic.gdx.utils.Array;
import com.hades.game.entities.Unit;
import com.badlogic.gdx.math.MathUtils;
import com.hades.game.screens.BattleScreen;
import com.hades.game.constants.GameConfig;
import com.hades.game.constants.SkillData;
import com.hades.game.view.GameUI;

/**
 * Chess Olympus AI: 구원(Rescue) 및 전술적 지원 로직 통합본
 * 아군이 공격받을 때 가장 가까운 유닛이 우선적으로 대응합니다.
 */
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

    private static MoveCandidate findGlobalBestMove(Array<Unit> units, String aiTeam, String strategy) {
        int candidateIdx = 0;
        activeCandidates.clear();

        // 1. 전장 상황 파악
        int halfBoard = GameConfig.BOARD_HEIGHT / 2;
        boolean isPlayerInMyTerritory = false;
        Unit unitUnderAttack = null; // 가장 심하게 위협받는 아군

        for (Unit u : units) {
            if (!u.isAlive()) continue;
            if (!u.team.equals(aiTeam)) {
                if (u.gridY >= halfBoard) isPlayerInMyTerritory = true; // ZEUS 상단 기준
            } else {
                // 아군이 적의 사거리(이동+사거리) 안에 있는지 체크
                if (isUnitUnderThreat(u, units, aiTeam)) {
                    unitUnderAttack = u; // 이 유닛을 구원 대상으로 설정
                }
            }
        }

        for (int i = 0; i < units.size; i++) {
            Unit actor = units.get(i);
            if (actor == null || !actor.isAlive() || !aiTeam.equals(actor.team)) continue;

            // [보스 제한] 플레이어가 중앙을 넘지 않았고 보스 자신이 위험하지 않으면 대기
            if (actor.unitClass == Unit.UnitClass.HERO && !isPlayerInMyTerritory) {
                if (!isPotentialThreat(actor, units, aiTeam) && !canAttackFromCurrent(actor, units)) {
                    continue;
                }
            }

            float unitBasePenalty = recentUnitIds.contains(actor.hashCode(), false) ? -30000f : 0f;

            for (int x = 0; x < GameConfig.BOARD_WIDTH; x++) {
                for (int y = 0; y < GameConfig.BOARD_HEIGHT; y++) {
                    if (!BoardManager.canMoveTo(actor, x, y, units)) continue;

                    float score = calculateFinalScore(actor, x, y, units, aiTeam, strategy) + unitBasePenalty;

                    // [구원 로직] 맞고 있는 아군과 맨해튼 거리가 가까운 유닛에게 보너스 점수
                    if (unitUnderAttack != null && actor != unitUnderAttack) {
                        int distToAlly = Math.abs(x - unitUnderAttack.gridX) + Math.abs(y - unitUnderAttack.gridY);
                        // 가까울수록(거리가 작을수록) 높은 가산점
                        score += (15 - distToAlly) * 3000f;
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

        for (Unit enemy : units) {
            if (!enemy.isAlive() || aiTeam.equals(enemy.team)) continue;

            int distToMe = Math.abs(tx - enemy.gridX) + Math.abs(ty - enemy.gridY);
            if (distToMe <= (enemy.stat.move() + enemy.stat.range())) {
                threatCount++;
                potentialDamageTaken += enemy.stat.atk();
            }

            float targetValue = (enemy.unitClass == Unit.UnitClass.HERO) ? 15000f : 8000f;
            if (canHit(mySkill, tx, ty, enemy.gridX, enemy.gridY, actor.stat.range())) {
                score += targetValue;
                if (enemy.currentHp <= (int)(actor.getPower(true) * mySkill.power)) score += 20000f;
            }
        }

        // 포위 회피 및 진영 유지
        if (threatCount >= 2) score += strategy.equals("ASSASSIN") ? -40000f : -100000f;
        if (!isOwnTerritory) score += (actor.unitClass == Unit.UnitClass.HERO) ? -100000f : -40000f;

        // 생존 가중치
        float survivalWeight = strategy.equals("DEFENSIVE") ? 15f : 5f;
        score -= (potentialDamageTaken * survivalWeight);

        return score * DIFFICULTY_FACTOR;
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
        for (Unit enemy : units) {
            if (enemy.isAlive() && !enemy.team.equals(aiTeam)) {
                int dist = Math.abs(enemy.gridX - unit.gridX) + Math.abs(enemy.gridY - unit.gridY);
                if (dist <= (enemy.stat.move() + enemy.stat.range())) return true;
            }
        }
        return false;
    }

    private static boolean isPotentialThreat(Unit boss, Array<Unit> units, String aiTeam) {
        return isUnitUnderThreat(boss, units, aiTeam);
    }

    private static boolean canAttackFromCurrent(Unit actor, Array<Unit> units) {
        SkillData.Skill skill = SkillData.get(actor.stat.skillName());
        for (Unit enemy : units) {
            if (enemy.isAlive() && !enemy.team.equals(actor.team)) {
                if (canHit(skill, actor.gridX, actor.gridY, enemy.gridX, enemy.gridY, actor.stat.range())) return true;
            }
        }
        return false;
    }

    private static void checkAndReserveSkill(Unit actor, int tx, int ty, Array<Unit> units) {
        SkillData.Skill skill = SkillData.get(actor.stat.skillName());
        for (Unit enemy : units) {
            if (enemy.isAlive() && !enemy.team.equals(actor.team)) {
                if (canHit(skill, tx, ty, enemy.gridX, enemy.gridY, actor.stat.range())) {
                    actor.stat.setReservedSkill(actor.stat.skillName());
                    break;
                }
            }
        }
    }
}
