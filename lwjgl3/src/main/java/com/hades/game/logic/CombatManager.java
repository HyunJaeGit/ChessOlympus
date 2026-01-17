package com.hades.game.logic;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.hades.game.constants.SkillData;
import com.hades.game.entities.Unit;
import com.hades.game.view.GameUI;

/**
 * Chess Olympus: HADES vs ZEUS
 * 전투 수치 계산, 자동 공격 순서 관리, 컴뱃 로그 생성을 전담하는 매니저 클래스입니다.
 * 폰트 엔진 에러 방지를 위해 로그를 합치지 않고 개별 전송하도록 수정되었습니다.
 */
public class CombatManager {
    private final GameUI gameUI;
    private final TurnManager turnManager;
    private final String playerTeam;
    private final DeathHandler deathHandler;

    // [최적화] 이제 StringBuilder를 통한 버퍼링을 사용하지 않습니다.
    // 줄바꿈(\n)이 폰트 엔진의 크래시를 유발할 수 있기 때문입니다.

    /**
     * 유닛의 사망 처리를 외부(BattleScreen 등)에서 처리할 수 있도록 연결하는 인터페이스입니다.
     */
    public interface DeathHandler {
        void onUnitDeath(Unit target);
    }

    public CombatManager(GameUI gameUI, TurnManager turnManager, String playerTeam, DeathHandler deathHandler) {
        this.gameUI = gameUI;
        this.turnManager = turnManager;
        this.playerTeam = playerTeam;
        this.deathHandler = deathHandler;
    }

    /**
     * 특정 진영의 모든 살아있는 유닛이 사거리 내 적을 자동으로 공격하도록 처리합니다.
     */
    public void processAutoAttack(Array<Unit> units, String team) {
        // libGDX Array의 중첩 반복 에러를 방지하기 위해 인덱스 기반 for문을 사용합니다.
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

        // 모든 공격이 끝난 후 성녀 유닛들의 치료 로직을 실행합니다.
        processAutoHeal(units, team);
    }

    /**
     * 공격자와 피격자 간의 실제 데미지 계산 및 로그 전송을 수행합니다.
     */
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

        String logEntry = (activeSkillName != null)
            ? String.format("%s -> %s [%s] %d데미지", attacker.name, target.name, activeSkillName, finalDamage)
            : String.format("%s -> %s %d데미지", attacker.name, target.name, finalDamage);
        sendToUI(logEntry, attacker.team);

        if (target.currentHp <= 0) {
            sendToUI(target.name + " 처치됨!", "SYSTEM");
            deathHandler.onUnitDeath(target);
            return;
        }

        // 반격 로직
        if (target.canReach(attacker)) {
            target.playAttackAnim(attacker.gridX, attacker.gridY);
            int counterDamage = target.getPower(turnManager.isMyTurn(target.team));
            attacker.takeDamage(counterDamage, Color.GOLD);

            // [수정] "ㄴ" 기호는 유니코드 범위 문제로 에러를 유발할 수 있어 ">"로 대체하거나
            // 폰트 팩토리 수정 후 안전하게 사용합니다. 여기서는 기호 대신 화살표를 사용합니다.
            sendToUI(" > " + target.name + " 반격! " + counterDamage + "데미지", target.team);

            if (attacker.currentHp <= 0) {
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

    /**
     * [수정] 줄바꿈 없이 UI에 즉시 로그를 전달합니다.
     */
    private void sendToUI(String msg, String team) {
        gameUI.addLog(msg, team, playerTeam);
    }
}
