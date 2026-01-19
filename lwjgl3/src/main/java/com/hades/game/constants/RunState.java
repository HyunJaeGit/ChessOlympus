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

    // 데이터 안전 초기화 및 공식 명예의 전당 기록 박제 메서드
    public void ensureRankInitialized() {
        if (topHeroNames == null || topHeroNames.length < 3) {
            topHeroNames = new String[3];
            topBestTimes = new float[3];
        }

        // 로컬에 저장하는 명예의전당 순위(더미)
        if (topHeroNames[0] == null || topHeroNames[0].equals("---") || topHeroNames[0].isEmpty()) {
            topHeroNames[0] = "데브케이";
            topBestTimes[0] = 320f;
            topHeroNames[1] = "무수하데스발닦개";
            topBestTimes[1] = 465f;
            topHeroNames[2] = "제우스도화이팅";
            topBestTimes[2] = 615f;
        }

        // 세이브 로드 시 Map의 데이터가 null이 되지 않도록 보장합니다.
        if (stageBestTimes == null) {
            stageBestTimes = new HashMap<>();
        }
    }

    // 세이브 파일로부터 로드된 데이터를 합산할 때 사용할 안전한 총점 계산 메서드
    public float getTotalClearTime() {
        float total = 0;
        if (stageBestTimes == null) return 0;

        // JSON 로드 시 Integer 키가 String으로, Float이 Double로 변환되는 현상을 방어합니다.
        for (Object value : stageBestTimes.values()) {
            if (value instanceof Number) {
                total += ((Number) value).floatValue();
            }
        }
        return total;
    }

    // 게임 오버 또는 클리어 시 현재 회차 데이터 리셋 메서드
    public void reset() {
        this.selectedHeroName = "";
        this.selectedFaction = "HADES";
        this.currentStageLevel = 1;
        this.heroStat = null;
        this.soulFragments = 0;
        this.olympusSeals = 0;
        this.isGameOver = false;
        if (this.stageBestTimes != null) {
            this.stageBestTimes.clear();
        }
    }

    // 새로운 게임 시작 시 초기화 메서드
    public void startNewRun(String name, UnitData.Stat baseStat, String faction) {
        this.selectedHeroName = name;
        this.selectedFaction = faction;
        this.currentStageLevel = 1;
        this.heroStat = new UnitData.Stat(baseStat);
        this.soulFragments = 0;
        this.olympusSeals = 0;
        this.isGameOver = false;
        if (this.stageBestTimes == null) {
            this.stageBestTimes = new HashMap<>();
        } else {
            this.stageBestTimes.clear();
        }
        ensureRankInitialized();
    }
}
