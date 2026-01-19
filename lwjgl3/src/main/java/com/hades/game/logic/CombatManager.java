package com.hades.game.logic;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.hades.game.constants.SkillData;
import com.hades.game.entities.Unit;
import com.hades.game.view.GameUI;

// Chess Olympus: HADES vs ZEUS
// 전투 수치 계산, 자동 공격 순서 관리, 컴뱃 로그 생성을 전담하는 매니저 클래스입니다.
public class CombatManager {
    private final GameUI gameUI;
    private final TurnManager turnManager;
    private final String playerTeam;
    private final DeathHandler deathHandler;

    // 유닛의 사망 처리를 외부에서 처리할 수 있도록 연결하는 인터페이스입니다.
    public interface DeathHandler {
        void onUnitDeath(Unit target);
    }

    public CombatManager(GameUI gameUI, TurnManager turnManager, String playerTeam, DeathHandler deathHandler) {
        this.gameUI = gameUI;
        this.turnManager = turnManager;
        this.playerTeam = playerTeam;
        this.deathHandler = deathHandler;
    }

    // 특정 진영의 모든 살아있는 유닛이 사거리 내 적을 자동으로 공격하도록 처리합니다.
    public void processAutoAttack(Array<Unit> units, String team) {
        for (int i = 0; i < units.size; i++) {
            Unit attacker = units.get(i);
            if (attacker != null && attacker.isAlive() && attacker.team.equals(team)) {
                if (attacker.unitClass == Unit.UnitClass.KNIGHT) {
                    Array<Unit> targets = BoardManager.findAllTargetsInRange(attacker, units);
                    for (int j = 0; j < targets.size; j++) {
                        performAttack(attacker, targets.get(j));
                    }
                } else {
                    Unit target = BoardManager.findBestTargetInRange(attacker, units);
                    if (target != null) performAttack(attacker, target);
                }
            }
        }
        processAutoHeal(units, team);
    }

    // 공격자와 피격자 간의 실제 데미지 계산 및 로그 전송을 수행합니다.
    public void performAttack(Unit attacker, Unit target) {
        if (attacker == null || target == null || !target.isAlive() || !attacker.isAlive()) return;

        attacker.playAttackAnim(target.gridX, target.gridY);

        boolean isAttackerTurn = turnManager.isMyTurn(attacker.team);
        int damage = attacker.getPower(isAttackerTurn);
        float skillMultiplier = 1.0f;
        String activeSkillName = null;

        if (attacker.unitClass == Unit.UnitClass.HERO) {
            String reserved = attacker.stat.getReservedSkill();
            if (reserved != null && !reserved.equals("기본 공격")) {
                activeSkillName = reserved;
                skillMultiplier = SkillData.get(activeSkillName).power;
                sendToUI("[권능] " + attacker.name + " [" + activeSkillName + "]!", attacker.team);
                attacker.stat.clearReservedSkill();
                attacker.stat.setSkillUsed(activeSkillName, true);
            } else if (!attacker.team.equals(playerTeam)) {
                activeSkillName = attacker.stat.skillName();
                skillMultiplier = SkillData.get(activeSkillName).power;
                sendToUI("[권능] " + attacker.name + " [" + activeSkillName + "]!", attacker.team);
            }
        }

        int finalDamage = (int)(damage * skillMultiplier);
        target.takeDamage(finalDamage, Color.RED);

        // 로그 생성 시 "데미지" 키워드 앞에 공백을 주어 GameUI에서 인식하기 쉽게 만듭니다.
        String logEntry = (activeSkillName != null)
            ? String.format("%s -> %s [%s] %d 데미지", attacker.name, target.name, activeSkillName, finalDamage)
            : String.format("%s -> %s %d 데미지", attacker.name, target.name, finalDamage);
        sendToUI(logEntry, attacker.team);

        if (target.currentHp <= 0) {
            target.currentHp = 0;
            target.status = Unit.DEAD;
            sendToUI(target.name + " 처치됨!", "SYSTEM");
            deathHandler.onUnitDeath(target);
            return;
        }

        // 반격 로직
        if (target.canReach(attacker)) {
            target.playAttackAnim(attacker.gridX, attacker.gridY);
            int counterDamage = target.getPower(turnManager.isMyTurn(target.team));
            attacker.takeDamage(counterDamage, Color.GOLD);
            // 반격 로그에도 "데미지" 키워드 포함
            sendToUI(" > " + target.name + " 반격! " + counterDamage + " 데미지", target.team);

            if (attacker.currentHp <= 0) {
                attacker.currentHp = 0;
                attacker.status = Unit.DEAD;
                sendToUI(attacker.name + " 처치됨!", "SYSTEM");
                deathHandler.onUnitDeath(attacker);
            }
        }
    }

    public void processAutoHeal(Array<Unit> units, String team) {
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);
            if (u.isAlive() && u.team.equals(team) && u.unitClass == Unit.UnitClass.SAINT) {
                for (int j = 0; j < units.size; j++) {
                    Unit ally = units.get(j);
                    if (ally.isAlive() && ally.team.equals(team) && ally != u) {
                        int dist = Math.abs(u.gridX - ally.gridX) + Math.abs(u.gridY - ally.gridY);
                        if (dist == 1 && ally.currentHp < ally.stat.hp()) {
                            ally.currentHp = Math.min(ally.stat.hp(), ally.currentHp + 15);
                            sendToUI("[치료] " + u.name + " -> " + ally.name + "(+15)", team);
                        }
                    }
                }
            }
        }
    }

    private void sendToUI(String msg, String team) {
        gameUI.addLog(msg, team, playerTeam);
    }
}
