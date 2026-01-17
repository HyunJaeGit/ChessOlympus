package com.hades.game.logic;

import com.badlogic.gdx.utils.Array;
import com.hades.game.constants.SkillData;
import com.hades.game.entities.Unit;
import com.hades.game.view.GameUI;

// 영웅의 권능(스킬) 발동, 사거리 판정, 데미지 계산을 전담하며 연출을 트리거하는 매니저 클래스
public class SkillManager {

    // [핵심] 스킬 발동 시 사거리 내 적을 찾아 데미지 및 연출 트리거
    public static void executeSkill(Unit caster, String skillName, Array<Unit> units, GameUI gameUI, String playerTeam) {
        SkillData.Skill data = SkillData.get(skillName);
        if (data == null) return;

        gameUI.addLog("권능 해방!! [" + skillName + "]", caster.team, playerTeam);
        int damage = (int) (caster.stat.atk() * data.power);

        // 모든 유닛을 순회하며 적군이고 사거리 내에 있는지 확인
        for (Unit target : units) {
            if (target != null && target.isAlive() && !target.team.equals(caster.team)) {
                int dist = Math.abs(caster.gridX - target.gridX) + Math.abs(caster.gridY - target.gridY);

                if (dist <= data.range) {
                    // [연출 추가] 스킬 사용자가 대상을 향해 짧게 도약 (시각적 피드백)
                    caster.playAttackAnim(target.gridX, target.gridY);

                    applySkillDamage(target, damage, gameUI, caster.team, playerTeam);

                    // 광역 스킬이 아니면 첫 번째 대상만 타격 후 종료
                    if (!data.isAoE) break;
                }
            }
        }
    }

    // 실제 체력을 깎고 피격 애니메이션을 실행합니다.
    private static void applySkillDamage(Unit target, int damage, GameUI gameUI, String casterTeam, String playerTeam) {
        // [연출 추가] 피격된 유닛 깜빡임
        target.playHitAnim();

        target.currentHp -= damage;
        gameUI.addLog(target.name + "에게 " + damage + "의 피해!", casterTeam, playerTeam);

        if (target.currentHp <= 0) {
            target.currentHp = 0;
            target.status = Unit.DEAD;
            gameUI.addLog(target.name + " 처치됨!", casterTeam, playerTeam);
        }
    }

    // 특정 스킬의 사거리 내에 유효한 타겟이 있는지 미리 체크
    public static boolean isAnyTargetInRange(Unit hero, String skillName, Array<Unit> units) {
        SkillData.Skill data = SkillData.get(skillName);
        if (data == null) return false;

        for (Unit target : units) {
            if (target.isAlive() && !target.team.equals(hero.team)) {
                int dist = Math.abs(hero.gridX - target.gridX) + Math.abs(hero.gridY - target.gridY);
                if (dist <= data.range) return true;
            }
        }
        return false;
    }
}
