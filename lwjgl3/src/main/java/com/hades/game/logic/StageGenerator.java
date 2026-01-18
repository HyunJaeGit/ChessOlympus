package com.hades.game.logic;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.hades.game.constants.GameConfig;
import com.hades.game.constants.UnitData;
import com.hades.game.entities.Unit;

// Chess Olympus: HADES vs ZEUS - 스테이지 생성기
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
        // 영웅은 전략적 핵심이므로 항상 중앙 하단 (3, 0) 고정
        units.add(new Unit(name, team, stat, name, Unit.UnitClass.HERO, 3, 0));

        // 배치가 가능한 X 좌표 리스트 (영웅 자리인 3을 제외한 0, 1, 2, 4, 5, 6)
        Array<Integer> availableX = new Array<>(new Integer[]{0, 1, 2, 4, 5, 6});
        availableX.shuffle(); // 좌표 리스트를 무작위로 섞음

        // 섞인 좌표에 순서대로 일반 병사들 배치
        units.add(new Unit("기병", team, UnitData.STAT_KNIGHT, UnitData.IMG_KNIGHT, Unit.UnitClass.KNIGHT, availableX.pop(), 0));
        units.add(new Unit("궁병", team, UnitData.STAT_ARCHER, UnitData.IMG_ARCHER, Unit.UnitClass.ARCHER, availableX.pop(), 0));
        units.add(new Unit("방패병", team, UnitData.STAT_SHIELD, UnitData.IMG_SHIELD, Unit.UnitClass.SHIELD, availableX.pop(), 0));
        units.add(new Unit("방패병", team, UnitData.STAT_SHIELD, UnitData.IMG_SHIELD, Unit.UnitClass.SHIELD, availableX.pop(), 0));
        units.add(new Unit("성녀", team, UnitData.STAT_SAINT, UnitData.IMG_SAINT, Unit.UnitClass.SAINT, availableX.pop(), 0));
        units.add(new Unit("전차병", team, UnitData.STAT_CHARIOT, UnitData.IMG_CHARIOT, Unit.UnitClass.CHARIOT, availableX.pop(), 0));
    }

    private static void setupEnemyUnits(Array<Unit> units, String team, int stageLevel) {
        int enemyRow = GameConfig.BOARD_HEIGHT - 1;

        // 적군 진영에 맞는 보스 스탯과 이름 로드
        String[] enemyHeroNames = team.equals("HADES") ? UnitData.NAMES_HADES : UnitData.NAMES_ZEUS;
        UnitData.Stat[] enemyHeroStats = team.equals("HADES") ? UnitData.STATS_HADES : UnitData.STATS_ZEUS;

        int bossIdx = Math.min(stageLevel - 1, enemyHeroStats.length - 1);
        String bossName = enemyHeroNames[bossIdx];

        // 적군 보스는 항상 적진 중앙 (3, enemyRow) 고정
        units.add(new Unit(bossName, team, enemyHeroStats[bossIdx], bossName, Unit.UnitClass.HERO, 3, enemyRow));

        // 배치가 가능한 X 좌표 리스트 (보스 자리인 3을 제외)
        Array<Integer> availableX = new Array<>(new Integer[]{0, 1, 2, 4, 5, 6});
        availableX.shuffle(); // 적군 좌표도 무작위로 섞음

        // 섞인 좌표에 순서대로 적군 일반병 배치
        units.add(new Unit("적 궁병", team, UnitData.STAT_ARCHER, UnitData.IMG_ARCHER, Unit.UnitClass.ARCHER, availableX.pop(), enemyRow));
        units.add(new Unit("적 기병", team, UnitData.STAT_KNIGHT, UnitData.IMG_KNIGHT, Unit.UnitClass.KNIGHT, availableX.pop(), enemyRow));
        units.add(new Unit("적 방패병", team, UnitData.STAT_SHIELD, UnitData.IMG_SHIELD, Unit.UnitClass.SHIELD, availableX.pop(), enemyRow));
        units.add(new Unit("적 방패병", team, UnitData.STAT_SHIELD, UnitData.IMG_SHIELD, Unit.UnitClass.SHIELD, availableX.pop(), enemyRow));
        units.add(new Unit("적 전차병", team, UnitData.STAT_CHARIOT, UnitData.IMG_CHARIOT, Unit.UnitClass.CHARIOT, availableX.pop(), enemyRow));
        units.add(new Unit("적 성녀", team, UnitData.STAT_SAINT, UnitData.IMG_SAINT, Unit.UnitClass.SAINT, availableX.pop(), enemyRow));
    }
}
