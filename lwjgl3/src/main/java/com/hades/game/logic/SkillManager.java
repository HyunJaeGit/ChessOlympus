package com.hades.game.logic;

import com.badlogic.gdx.utils.Array;
import com.hades.game.constants.SkillData;
import com.hades.game.entities.Unit;
import com.hades.game.view.GameUI;
import com.hades.game.screens.BattleScreen;

// SkillManager: 모든 권능(스킬)의 실행, 효과 판정, 상태 변화를 관리하는 통합 클래스
public class SkillManager {

    // 메인 실행부: 애니메이션 트리거와 함께 스킬 효과를 집행
    public static void executeSkill(Unit caster, String skillName, Array<Unit> units, GameUI gameUI, String playerTeam, Object screenObj) {
        // 이미 사용한 스킬인지 체크 (1회성 권능 제한)
        if (caster.team.equals(playerTeam) && !caster.stat.isSkillReady(skillName)) return;

        SkillData.Skill data = SkillData.get(skillName);
        if (data == null) return;

        gameUI.addLog("권능 해방!! [" + skillName + "]", caster.team, playerTeam);

        // 1. [특수 효과 사전 처리] 시전자 본인에게 적용되는 버프 등
        applyCasterBuff(caster, skillName, gameUI, playerTeam);

        // 2. [타겟팅 및 실행] 스킬 타입별 분기
        if (skillName.equals("심판의 번개")) {
            executeJudgmentLightning(caster, data, units, gameUI, playerTeam, screenObj);
        } else if (isHealSkill(skillName)) {
            executeHealSkill(caster, data, units, gameUI, playerTeam);
        } else if (data.shape == SkillData.Shape.LINE) {
            executeLineSkill(caster, data, units, gameUI, playerTeam, screenObj);
        } else {
            executeAreaSkill(caster, data, units, gameUI, playerTeam, screenObj);
        }

        // 3. [소모 처리] 사용 완료 후 기록
        caster.stat.setSkillUsed(skillName, true);
        caster.stat.clearReservedSkill();
    }

    // 시전자 본인 버프 (아테나 방벽 등 설명에 따른 특수 구현)
    private static void applyCasterBuff(Unit caster, String skillName, GameUI ui, String pt) {
        if (skillName.equals("전략적 방벽")) {
            // 아테나: 최대 체력의 20%만큼 보호막(현재 체력 증가) 부여
            int shieldAmount = (int)(caster.stat.hp() * 0.2f);
            caster.currentHp += shieldAmount;
            ui.addLog(caster.name + "가 성스러운 방벽을 세웠습니다! (보호막 +" + shieldAmount + ")", caster.team, pt);
        }
    }

    // 제우스 전용: 심판의 번개 (전체 범위 중 최저 체력 저격)
    private static void executeJudgmentLightning(Unit caster, SkillData.Skill data, Array<Unit> units, GameUI ui, String pt, Object screen) {
        Unit target = null;
        int minHp = Integer.MAX_VALUE;
        for (Unit u : units) {
            if (u.isAlive() && !u.team.equals(caster.team) && u.currentHp < minHp) {
                minHp = u.currentHp;
                target = u;
            }
        }
        if (target != null) {
            target.playHitAnim();
            applyEffect(target, (int)(caster.stat.atk() * data.power), false, ui, caster.team, pt, screen, data.name);
        }
    }

    // 아군 치유 스킬 (매혹의 향기, 올림푸스의 가호 등)
    private static void executeHealSkill(Unit caster, SkillData.Skill data, Array<Unit> units, GameUI ui, String pt) {
        for (Unit u : units) {
            if (u.isAlive() && u.team.equals(caster.team)) {
                if (checkShape(caster, u, data)) {
                    applyEffect(u, (int)(caster.stat.atk() * data.power), true, ui, caster.team, pt, null, data.name);
                    if (!data.isAoE) return;
                }
            }
        }
    }

    // 일직선 공격 스킬 (그림자 습격, 달빛의 추격)
    private static void executeLineSkill(Unit caster, SkillData.Skill data, Array<Unit> units, GameUI ui, String pt, Object screen) {
        for (Unit u : units) {
            if (u.isAlive() && !u.team.equals(caster.team)) {
                int dx = Math.abs(caster.gridX - u.gridX);
                int dy = Math.abs(caster.gridY - u.gridY);
                if ((dx == 0 || dy == 0) && (dx + dy <= data.range)) {
                    caster.playAttackAnim(u.gridX, u.gridY);
                    applyEffect(u, (int)(caster.stat.atk() * data.power), false, ui, caster.team, pt, screen, data.name);
                    if (!data.isAoE) return;
                }
            }
        }
    }

    // 일반 범위기 (대지의 분노, 여왕의 권위, 지옥의 숨결 등)
    private static void executeAreaSkill(Unit caster, SkillData.Skill data, Array<Unit> units, GameUI ui, String pt, Object screen) {
        for (Unit u : units) {
            if (u.isAlive() && !u.team.equals(caster.team)) {
                if (checkShape(caster, u, data)) {
                    caster.playAttackAnim(u.gridX, u.gridY);
                    applyEffect(u, (int)(caster.stat.atk() * data.power), false, ui, caster.team, pt, screen, data.name);
                    if (!data.isAoE) return;
                }
            }
        }
    }

    // 최종 효과 적용 및 사망 판정 연동
    private static void applyEffect(Unit t, int val, boolean heal, GameUI ui, String ct, String pt, Object screen, String skillName) {
        if (heal) {
            // 치유: 설명에 맞춰 최대 체력을 약간 넘길 수 있도록 보정
            t.currentHp = Math.min(t.stat.hp() + 100, t.currentHp + val);
            ui.addLog(t.name + " 체력 " + val + " 회복", ct, pt);
        } else {
            t.playHitAnim();

            // 상태 이상 효과 예시: 여왕의 권위 사용 시 추가 압박 피해
            int finalDamage = val;
            if (skillName.equals("여왕의 권위")) {
                ui.addLog(t.name + "가 여왕의 위엄에 압도되었습니다!", ct, pt);
                finalDamage += 20;
            }

            t.currentHp -= finalDamage;
            ui.addLog(t.name + "에게 " + finalDamage + " 피해", ct, pt);

            if (t.currentHp <= 0) {
                t.currentHp = 0;
                t.status = Unit.DEAD;
                // 영웅 사망 시 즉시 게임 오버 시퀀스 트리거
                if (screen instanceof BattleScreen) {
                    ((BattleScreen) screen).handleDeath(t);
                }
            }
        }
    }

    // 기하학적 범위 판정 유틸리티
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

    private static boolean isHealSkill(String name) {
        return name.contains("치유") || name.contains("향기") || name.contains("가호");
    }
}
