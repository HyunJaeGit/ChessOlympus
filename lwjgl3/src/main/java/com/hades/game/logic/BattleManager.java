package com.hades.game.logic;

import com.badlogic.gdx.utils.Array;
import com.hades.game.entities.Unit;
import com.hades.game.view.GameUI;

// Chess Olympus: HADES vs ZEUS - 전투 규칙 집행 매니저
// 일반 공격, 반격, 자동 힐링 등 전투의 기본 규칙을 집행하며 연출을 트리거합니다.
public class BattleManager {

    // [핵심] 해당 팀 유닛들의 자동 공격 시퀀스를 실행합니다.
    public static void processAutoAttack(String team, Array<Unit> units, TurnManager turnManager, GameUI gameUI, String playerTeam) {
        for (Unit attacker : units) {
            if (attacker.isAlive() && attacker.team.equals(team)) {
                // 기사(KNIGHT) 클래스는 사거리 내 모든 적을 광역 공격합니다.
                if (attacker.unitClass == Unit.UnitClass.KNIGHT) {
                    Array<Unit> targets = BoardManager.findAllTargetsInRange(attacker, units);
                    for (Unit t : targets) performAttack(attacker, t, turnManager, gameUI, playerTeam);
                } else {
                    // 그 외 일반 유닛은 사거리 내 가장 효율적인 타겟 하나를 공격합니다.
                    Unit target = BoardManager.findBestTargetInRange(attacker, units);
                    if (target != null) performAttack(attacker, target, turnManager, gameUI, playerTeam);
                }
            }
        }
        // 공격 시퀀스 종료 후 해당 팀의 성녀(SAINT) 치료 로직을 실행합니다.
        processAutoHeal(team, units, gameUI, playerTeam);
    }

    // 개별 유닛 간의 공격 및 반격 로직을 처리합니다. (애니메이션 및 로그 포함)
    public static void performAttack(Unit attacker, Unit target, TurnManager turnManager, GameUI gameUI, String playerTeam) {
        if (attacker == null || target == null || !target.isAlive() || !attacker.isAlive()) return;

        // [연출] 공격자 도약 및 피격자 깜빡임 애니메이션
        attacker.playAttackAnim(target.gridX, target.gridY);
        target.playHitAnim();

        boolean isAttackerTurn = turnManager.isMyTurn(attacker.team);
        int damage = attacker.getPower(isAttackerTurn);

        target.currentHp -= damage;
        // 로그 출력 시 공격자의 팀 정보를 전달하여 GameUI에서 색상을 판단하게 합니다.
        gameUI.addLog(attacker.name + " -> " + target.name + " " + damage + " 데미지", attacker.team, playerTeam);

        if (target.currentHp <= 0) {
            target.currentHp = 0;
            target.status = Unit.DEAD;
            gameUI.addLog(target.name + " 처치됨!", attacker.team, playerTeam);
            return;
        }

        // 반격 판정: 피격자가 공격자를 때릴 수 있는 거리라면 즉시 반격합니다.
        if (target.canReach(attacker)) {
            target.playAttackAnim(attacker.gridX, attacker.gridY);
            attacker.playHitAnim();

            int counterDamage = target.getPower(turnManager.isMyTurn(target.team));
            attacker.currentHp -= counterDamage;
            gameUI.addLog(target.name + "의 반격! " + counterDamage + " 데미지", target.team, playerTeam);

            if (attacker.currentHp <= 0) {
                attacker.currentHp = 0;
                attacker.status = Unit.DEAD;
                gameUI.addLog(attacker.name + " 처치됨!", target.team, playerTeam);
            }
        }
    }

    // 성녀(SAINT) 유닛의 주변 아군 자동 치료 로직입니다.
    public static void processAutoHeal(String team, Array<Unit> units, GameUI gameUI, String playerTeam) {
        for (Unit u : units) {
            if (u.isAlive() && u.team.equals(team) && u.unitClass == Unit.UnitClass.SAINT) {
                for (Unit ally : units) {
                    if (ally.isAlive() && ally.team.equals(team) && ally != u) {
                        int dist = Math.abs(u.gridX - ally.gridX) + Math.abs(u.gridY - ally.gridY);
                        // 인접한 1칸 내의 부상당한 아군 치료
                        if (dist == 1 && ally.currentHp < ally.stat.hp()) {
                            ally.currentHp = Math.min(ally.stat.hp(), ally.currentHp + 15);
                            gameUI.addLog(u.name + "가 " + ally.name + "를 치료함(+15)", u.team, playerTeam);
                        }
                    }
                }
            }
        }
    }
}
