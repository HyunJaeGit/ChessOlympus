package com.hades.game.constants;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

// Chess Olympus: HADES vs ZEUS - 게임 진행 데이터를 저장하는 클래스
public class RunState implements Serializable {
    private static final long serialVersionUID = 1L;

    public String selectedHeroName = "";
    public String selectedFaction = "";
    public int currentStageLevel = 1;
    public UnitData.Stat heroStat;

    public int soulFragments = 0;
    public int olympusSeals = 0;
    public boolean isGameOver = false;

    // 회차별 기록 (현재 플레이 중인 기록)
    public Map<Integer, Float> stageBestTimes = new HashMap<>();

    // 명예의 전당 1~3위 데이터 (모든 유저에게 공유될 공식 기록 포함)
    public float[] topBestTimes = new float[3];    // 기록 시간
    public String[] topHeroNames = new String[3];  // 유저 닉네임

    public RunState() { }

    // 데이터 안전 초기화 및 공식 명예의 전당 기록 박제
    public void ensureRankInitialized() {
        if (topHeroNames == null || topHeroNames.length < 3) {
            topHeroNames = new String[3];
            topBestTimes = new float[3];
        }

        // 로컬에 저장된 기록이 없거나 초기 상태일 때 공식 기록(더미)을 주입합니다.
        // 이 데이터는 배포 시 모든 유저에게 동일하게 보여집니다.
        if (topHeroNames[0] == null || topHeroNames[0].equals("---") || topHeroNames[0].isEmpty()) {
            // 1위: 데브케이 (05:20)
            topHeroNames[0] = "데브케이";
            topBestTimes[0] = 320f;

            // 2위: 데브케이 (07:45)
            topHeroNames[1] = "데브케이";
            topBestTimes[1] = 465f;

            // 3위: 데브케이 (10:15)
            topHeroNames[2] = "데브케이";
            topBestTimes[2] = 615f;
        }
    }

    // 게임 오버 또는 클리어 시 현재 회차 데이터 리셋
    public void reset() {
        this.selectedHeroName = "";
        this.selectedFaction = "HADES";
        this.currentStageLevel = 1;
        this.heroStat = null;
        this.soulFragments = 0;
        this.olympusSeals = 0;
        this.isGameOver = false;
        this.stageBestTimes.clear(); // 회차 기록만 삭제 (명예의 전당은 유지됨)
    }

    // 새로운 게임 시작 시 초기화
    public void startNewRun(String name, UnitData.Stat baseStat, String faction) {
        this.selectedHeroName = name;
        this.selectedFaction = faction;
        this.currentStageLevel = 1;
        this.heroStat = new UnitData.Stat(baseStat);
        this.soulFragments = 0;
        this.olympusSeals = 0;
        this.isGameOver = false;
        this.stageBestTimes.clear();
        ensureRankInitialized(); // 시작 시 명예의 전당 데이터 체크 및 주입
    }
}
