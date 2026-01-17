package com.hades.game.logic;

import com.badlogic.gdx.utils.Array;
import com.hades.game.entities.Unit;
import com.hades.game.view.GameUI;

// 일반 공격, 반격, 자동 힐링 등 전투의 기본 규칙을 집행하며 연출을 트리거하는 매니저 클래스입니다.
public class BattleManager {

    // [핵심] 해당 팀 유닛들의 자동 공격 시퀀스를 실행
    public static void processAutoAttack(String team, Array<Unit> units, TurnManager turnManager, GameUI gameUI, String playerTeam) {
        for (Unit attacker : units) {
            if (attacker.isAlive() && attacker.team.equals(team)) {
                if (attacker.unitClass == Unit.UnitClass.KNIGHT) {
                    Array<Unit> targets = BoardManager.findAllTargetsInRange(attacker, units);
                    for (Unit t : targets) performAttack(attacker, t, turnManager, gameUI, playerTeam);
                } else {
                    Unit target = BoardManager.findBestTargetInRange(attacker, units);
                    if (target != null) performAttack(attacker, target, turnManager, gameUI, playerTeam);
                }
            }
        }
        processAutoHeal(team, units, gameUI, playerTeam);
    }

    // 개별 유닛 간의 공격 및 반격 로직 (애니메이션 트리거 포함)
    public static void performAttack(Unit attacker, Unit target, TurnManager turnManager, GameUI gameUI, String playerTeam) {
        if (attacker == null || target == null || !target.isAlive() || !attacker.isAlive()) return;

        // [연출 추가] 공격자 도약 및 피격자 깜빡임
        attacker.playAttackAnim(target.gridX, target.gridY);
        target.playHitAnim();

        boolean isAttackerTurn = turnManager.isMyTurn(attacker.team);
        int damage = attacker.getPower(isAttackerTurn);

        target.currentHp -= damage;
        gameUI.addLog(attacker.name + " -> " + target.name + " " + damage + " 데미지", attacker.team, playerTeam);

        if (target.currentHp <= 0) {
            target.currentHp = 0;
            target.status = Unit.DEAD;
            gameUI.addLog(target.name + " 처치됨!", attacker.team, playerTeam);
            return;
        }

        // 반격 판정
        if (target.canReach(attacker)) {
            // [연출 추가] 반격 시에도 피격자(원래 공격자) 깜빡임 및 반격자(원래 피격자) 도약
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

    // 성녀 유닛의 주변 아군 자동 치료
    public static void processAutoHeal(String team, Array<Unit> units, GameUI gameUI, String playerTeam) {
        for (Unit u : units) {
            if (u.isAlive() && u.team.equals(team) && u.unitClass == Unit.UnitClass.SAINT) {
                for (Unit ally : units) {
                    if (ally.isAlive() && ally.team.equals(team) && ally != u) {
                        int dist = Math.abs(u.gridX - ally.gridX) + Math.abs(u.gridY - ally.gridY);
                        if (dist == 1 && ally.currentHp < ally.stat.hp()) {
                            // [연출 추가] 치료받는 유닛도 피격과 구분되는 시각적 피드백이 있으면 좋으나 우선 생략하거나 깜빡임 재활용 가능
                            ally.currentHp = Math.min(ally.stat.hp(), ally.currentHp + 15);
                            gameUI.addLog(u.name + "가 " + ally.name + "를 치료함(+15)", u.team, playerTeam);
                        }
                    }
                }
            }
        }
    }
}
