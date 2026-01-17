package com.hades.game.logic;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.hades.game.constants.SkillData;
import com.hades.game.entities.Unit;
import com.hades.game.view.GameUI;

/**
 * Chess Olympus: HADES vs ZEUS
 * 전투 수치 계산, 자동 공격 순서 관리, 컴뱃 로그 압축 및 생성을 전담하는 매니저 클래스입니다.
 */
public class CombatManager {
    private final GameUI gameUI;
    private final TurnManager turnManager;
    private final String playerTeam;
    private final DeathHandler deathHandler;

    // 한 사이클(이동 후 발생하는 모든 액션) 동안의 로그를 모아두는 버퍼입니다.
    private StringBuilder turnLogBuffer = new StringBuilder();

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
     * @param units 전장에 있는 전체 유닛 리스트
     * @param team 현재 공격을 수행할 팀 (HADES 또는 ZEUS)
     */
    public void processAutoAttack(Array<Unit> units, String team) {
        // 1. 새로운 공격 사이클을 위해 로그 버퍼 초기화
        turnLogBuffer.setLength(0);

        // libGDX Array의 중첩 반복 에러를 방지하기 위해 인덱스 기반 for문을 사용합니다.
        for (int i = 0; i < units.size; i++) {
            Unit attacker = units.get(i);
            if (attacker != null && attacker.isAlive() && attacker.team.equals(team)) {
                // 기병 클래스는 범위 내 모든 적을 공격하고, 그 외 클래스는 가장 좋은 타겟 하나만 공격합니다.
                if (attacker.unitClass == Unit.UnitClass.KNIGHT) {
                    Array<Unit> targets = BoardManager.findAllTargetsInRange(attacker, units);
                    for (Unit target : targets) performAttack(attacker, target);
                } else {
                    Unit target = BoardManager.findBestTargetInRange(attacker, units);
                    if (target != null) performAttack(attacker, target);
                }
            }
        }

        // 모든 공격이 끝난 후 성녀 유닛들의 치료 로직을 실행합니다.
        processAutoHeal(units, team);

        // 2. 사이클 동안 모인 로그가 있다면 UI 로그창에 한 번에 출력합니다.
        if (turnLogBuffer.length() > 0) {
            gameUI.addLog(turnLogBuffer.toString().trim(), team, playerTeam);
        }
    }

    /**
     * 공격자와 피격자 간의 실제 데미지 계산, 권능(스킬) 체크, 반격 로직을 수행합니다.
     */
    public void performAttack(Unit attacker, Unit target) {
        if (attacker == null || target == null || !target.isAlive() || !attacker.isAlive()) return;

        // 공격 애니메이션 실행
        attacker.playAttackAnim(target.gridX, target.gridY);

        boolean isAttackerTurn = turnManager.isMyTurn(attacker.team);
        int damage = attacker.getPower(isAttackerTurn);
        float skillMultiplier = 1.0f;
        String activeSkillName = null;

        // 영웅(HERO) 클래스인 경우 예약된 권능이 있는지 확인하고 적용합니다.
        if (attacker.unitClass == Unit.UnitClass.HERO) {
            String reserved = attacker.stat.getReservedSkill();
            if (reserved != null && !reserved.equals("기본 공격")) {
                activeSkillName = reserved;
                skillMultiplier = SkillData.get(activeSkillName).power;
                addToBuffer("[권능] " + attacker.name + " [" + activeSkillName + "]!");
                attacker.stat.clearReservedSkill();
                attacker.stat.setSkillUsed(activeSkillName, true);
            } else if (!attacker.team.equals(playerTeam)) {
                // 적군 영웅(AI)은 보유한 기본 스킬을 권능으로 사용합니다.
                activeSkillName = attacker.stat.skillName();
                skillMultiplier = SkillData.get(activeSkillName).power;
                addToBuffer("[권능] " + attacker.name + " [" + activeSkillName + "]!");
            }
        }

        // 최종 데미지 계산 및 적용
        int finalDamage = (int)(damage * skillMultiplier);
        target.takeDamage(finalDamage, Color.RED);

        // 로그 기록 (버퍼에 저장)
        String logEntry = (activeSkillName != null)
            ? String.format("%s -> %s [%s] %d데미지", attacker.name, target.name, activeSkillName, finalDamage)
            : String.format("%s -> %s %d데미지", attacker.name, target.name, finalDamage);
        addToBuffer(logEntry);

        // 피격자 사망 체크
        if (target.currentHp <= 0) {
            addToBuffer(target.name + " 처치됨!");
            deathHandler.onUnitDeath(target);
            return;
        }

        // 반격 로직: 피격자가 공격자를 공격할 수 있는 거리라면 반격을 수행합니다.
        if (target.canReach(attacker)) {
            target.playAttackAnim(attacker.gridX, attacker.gridY);
            int counterDamage = target.getPower(turnManager.isMyTurn(target.team));
            attacker.takeDamage(counterDamage, Color.GOLD);
            addToBuffer(" ㄴ " + target.name + " 반격! " + counterDamage + "데미지");

            if (attacker.currentHp <= 0) {
                addToBuffer(attacker.name + " 처치됨!");
                deathHandler.onUnitDeath(attacker);
            }
        }
    }

    /**
     * 성녀(SAINT) 클래스 유닛이 인접한(거리 1) 아군 중 체력이 깎인 유닛을 자동으로 치료합니다.
     */
    public void processAutoHeal(Array<Unit> units, String team) {
        // [수정] libGDX Array의 중첩 반복 에러(#iterator nested) 해결을 위해 인덱스 for문을 사용합니다.
        for (int i = 0; i < units.size; i++) {
            Unit u = units.get(i);

            // 유닛이 성녀 클래스이고 살아있는 경우
            if (u.isAlive() && u.team.equals(team) && u.unitClass == Unit.UnitClass.SAINT) {
                // 다시 한 번 전수 조사를 통해 주변 아군을 찾습니다.
                for (int j = 0; j < units.size; j++) {
                    Unit ally = units.get(j);

                    // 살아있는 아군이며, 본인이 아니고, 인접한 칸(거리 1)에 있는 경우
                    if (ally.isAlive() && ally.team.equals(team) && ally != u) {
                        int dist = Math.abs(u.gridX - ally.gridX) + Math.abs(u.gridY - ally.gridY);
                        if (dist == 1 && ally.currentHp < ally.stat.hp()) {
                            // 최대 체력을 넘지 않는 선에서 15의 체력을 회복시킵니다.
                            ally.currentHp = Math.min(ally.stat.hp(), ally.currentHp + 15);
                            addToBuffer("[치료] " + u.name + " -> " + ally.name + "(+15)");
                        }
                    }
                }
            }
        }
    }

    /**
     * 전달받은 문자열을 줄바꿈과 함께 로그 버퍼에 추가하는 유틸리티 메서드입니다.
     */
    private void addToBuffer(String msg) {
        if (turnLogBuffer.length() > 0) turnLogBuffer.append("\n");
        turnLogBuffer.append(msg);
    }
}
