package com.hades.game.logic;

import com.badlogic.gdx.utils.Array;
import com.hades.game.constants.SkillData;
import com.hades.game.entities.Unit;
import com.hades.game.view.GameUI;

public class SkillManager {

    public static void executeSkill(Unit caster, String skillName, Array<Unit> units, GameUI gameUI, String playerTeam) {
        if (caster.team.equals(playerTeam) && !caster.stat.isSkillReady(skillName)) return;

        SkillData.Skill data = SkillData.get(skillName);
        if (data == null) return;

        gameUI.addLog("권능 해방!! [" + skillName + "]", caster.team, playerTeam);

        // 스킬별 전용 메서드 배당
        switch (skillName) {
            case "심판의 번개":
                executeJudgmentLightning(caster, data, units, gameUI, playerTeam);
                break;
            case "매혹의 향기":
            case "올림푸스의 가호":
                executeHealSkill(caster, data, units, gameUI, playerTeam);
                break;
            case "그림자 습격":
            case "달빛의 추격":
                executeLineSkill(caster, data, units, gameUI, playerTeam);
                break;
            default:
                // 일반적인 범위 판정 스킬들 (십자, 사각형, 다이아몬드)
                executeAreaSkill(caster, data, units, gameUI, playerTeam);
                break;
        }

        if (caster.team.equals(playerTeam)) {
            caster.stat.setSkillUsed(skillName, true);
            caster.stat.clearReservedSkill();
        }
    }

    // [전용 메서드 1] 심판의 번개: 체력 가장 낮은 적 타격
    private static void executeJudgmentLightning(Unit caster, SkillData.Skill data, Array<Unit> units, GameUI gameUI, String playerTeam) {
        Unit target = null;
        int minHp = Integer.MAX_VALUE;
        for (Unit u : units) {
            if (u.isAlive() && !u.team.equals(caster.team) && u.currentHp < minHp) {
                minHp = u.currentHp;
                target = u;
            }
        }
        if (target != null) {
            caster.playAttackAnim(target.gridX, target.gridY);
            applyEffect(target, (int)(caster.stat.atk() * data.power), false, gameUI, caster.team, playerTeam);
        }
    }

    // [전용 메서드 2] 일직선(LINE) 스킬 처리
    private static void executeLineSkill(Unit caster, SkillData.Skill data, Array<Unit> units, GameUI gameUI, String playerTeam) {
        for (Unit u : units) {
            if (u.isAlive() && !u.team.equals(caster.team)) {
                int dx = Math.abs(caster.gridX - u.gridX);
                int dy = Math.abs(caster.gridY - u.gridY);
                // X축이나 Y축이 같고 사거리 이내일 때
                if ((dx == 0 || dy == 0) && (dx + dy <= data.range)) {
                    caster.playAttackAnim(u.gridX, u.gridY);
                    applyEffect(u, (int)(caster.stat.atk() * data.power), false, gameUI, caster.team, playerTeam);
                    if (!data.isAoE) break;
                }
            }
        }
    }

    // [전용 메서드 3] 치유 계열 메서드
    private static void executeHealSkill(Unit caster, SkillData.Skill data, Array<Unit> units, GameUI gameUI, String playerTeam) {
        for (Unit u : units) {
            if (u.isAlive() && u.team.equals(caster.team)) {
                if (checkShape(caster, u, data)) {
                    applyEffect(u, (int)(caster.stat.atk() * data.power), true, gameUI, caster.team, playerTeam);
                    if (!data.isAoE) break;
                }
            }
        }
    }

    // [전용 메서드 4] 일반 범위기(Area) 메서드
    private static void executeAreaSkill(Unit caster, SkillData.Skill data, Array<Unit> units, GameUI gameUI, String playerTeam) {
        for (Unit u : units) {
            if (u.isAlive() && !u.team.equals(caster.team)) {
                if (checkShape(caster, u, data)) {
                    caster.playAttackAnim(u.gridX, u.gridY);
                    applyEffect(u, (int)(caster.stat.atk() * data.power), false, gameUI, caster.team, playerTeam);
                    if (!data.isAoE) break;
                }
            }
        }
    }

    // 모양 판정 유틸리티
    private static boolean checkShape(Unit c, Unit t, SkillData.Skill data) {
        int dx = Math.abs(c.gridX - t.gridX);
        int dy = Math.abs(c.gridY - t.gridY);
        int dist = dx + dy;
        switch (data.shape) {
            case CROSS: return (dx == 0 || dy == 0) && dist <= data.range;
            case SQUARE: return dx <= data.range && dy <= data.range;
            case MANHATTAN: return dist <= data.range;
            default: return dist <= data.range;
        }
    }

    private static void applyEffect(Unit t, int val, boolean heal, GameUI ui, String ct, String pt) {
        if (heal) {
            t.currentHp = Math.min(t.stat.hp(), t.currentHp + val);
            ui.addLog(t.name + " 체력 " + val + " 회복", ct, pt);
        } else {
            t.playHitAnim();
            t.currentHp -= val;
            ui.addLog(t.name + "에게 " + val + " 피해", ct, pt);
            if (t.currentHp <= 0) {
                t.currentHp = 0;
                t.status = Unit.DEAD;
            }
        }
    }
}
