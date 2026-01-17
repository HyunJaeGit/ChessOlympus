package com.hades.game.logic;

import com.badlogic.gdx.utils.Array;
import com.hades.game.constants.GameConfig;
import com.hades.game.constants.UnitData;
import com.hades.game.entities.Unit;

// 스테이지 레벨에 맞춰 유닛 구성을 생성해주는 클래스입니다.
public class StageGenerator {

    public static Array<Unit> create(int stageLevel, String playerTeam, String heroName, UnitData.Stat heroStat) {
        Array<Unit> units = new Array<>();
        String aiTeam = playerTeam.equals("HADES") ? "ZEUS" : "HADES";

        // 1. 플레이어 유닛 배치
        setupPlayerUnits(units, playerTeam, heroName, heroStat);

        // 2. 적군(AI) 유닛 배치
        setupEnemyUnits(units, aiTeam, stageLevel);

        return units;
    }

    private static void setupPlayerUnits(Array<Unit> units, String team, String name, UnitData.Stat stat) {
        // 영웅은 항상 중앙(3, 0)
        units.add(new Unit(name, team, stat, name, Unit.UnitClass.HERO, 3, 0));

        // 기본 병사들 (고정 배치 혹은 로직에 따른 배치)
        units.add(new Unit("기병", team, UnitData.STAT_KNIGHT, UnitData.IMG_KNIGHT, Unit.UnitClass.KNIGHT, 0, 0));
        units.add(new Unit("궁병", team, UnitData.STAT_ARCHER, UnitData.IMG_ARCHER, Unit.UnitClass.ARCHER, 1, 0));
        units.add(new Unit("방패병", team, UnitData.STAT_SHIELD, UnitData.IMG_SHIELD, Unit.UnitClass.SHIELD, 2, 0));
        units.add(new Unit("방패병", team, UnitData.STAT_SHIELD, UnitData.IMG_SHIELD, Unit.UnitClass.SHIELD, 4, 0));
        units.add(new Unit("성녀", team, UnitData.STAT_SAINT, UnitData.IMG_SAINT, Unit.UnitClass.SAINT, 5, 0));
        units.add(new Unit("전차병", team, UnitData.STAT_CHARIOT, UnitData.IMG_CHARIOT, Unit.UnitClass.CHARIOT, 6, 0));
    }

    private static void setupEnemyUnits(Array<Unit> units, String team, int stageLevel) {
        int enemyRow = GameConfig.BOARD_HEIGHT - 1;

        // 보스(영웅) 결정
        int bossIdx = Math.min(stageLevel - 1, UnitData.STATS_ZEUS.length - 1);
        String bossName = UnitData.NAMES_ZEUS[bossIdx];
        units.add(new Unit(bossName, team, UnitData.STATS_ZEUS[bossIdx], bossName, Unit.UnitClass.HERO, 3, enemyRow));

        // 적 병졸 배치 (스테이지 레벨이 높아질수록 구성을 바꿀 수도 있음)
        units.add(new Unit("적 궁병", team, UnitData.STAT_ARCHER, UnitData.IMG_ARCHER, Unit.UnitClass.ARCHER, 0, enemyRow));
        units.add(new Unit("적 기병", team, UnitData.STAT_KNIGHT, UnitData.IMG_KNIGHT, Unit.UnitClass.KNIGHT, 1, enemyRow));
        units.add(new Unit("적 방패병", team, UnitData.STAT_SHIELD, UnitData.IMG_SHIELD, Unit.UnitClass.SHIELD, 2, enemyRow));
        units.add(new Unit("적 방패병", team, UnitData.STAT_SHIELD, UnitData.IMG_SHIELD, Unit.UnitClass.SHIELD, 4, enemyRow));
        units.add(new Unit("적 전차병", team, UnitData.STAT_CHARIOT, UnitData.IMG_CHARIOT, Unit.UnitClass.CHARIOT, 5, enemyRow));
        units.add(new Unit("적 성녀", team, UnitData.STAT_SAINT, UnitData.IMG_SAINT, Unit.UnitClass.SAINT, 6, enemyRow));
    }
}
