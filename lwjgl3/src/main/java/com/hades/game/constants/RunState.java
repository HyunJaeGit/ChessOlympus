package com.hades.game.constants;

import java.io.Serializable;

// HADES vs ZEUS - 게임 진행 데이터를 저장하는 클래스
public class RunState implements Serializable {
    private static final long serialVersionUID = 1L;

    public String selectedHeroName = "";
    public String selectedFaction = "";
    public int currentStageLevel = 1;
    public UnitData.Stat heroStat;

    // [추가] 재화 및 게임 상태 데이터 통합
    public int soulFragments = 0;          // 영혼 파편
    public int olympusSeals = 0;           // 올림포스 인장
    public boolean isGameOver = false;

    public RunState() {
        // 기본 생성자
    }

    // [중요] 이 메서드가 있어야 MenuScreen의 에러가 사라집니다.
    public void reset() {
        this.selectedHeroName = "";
        this.selectedFaction = "HADES"; // 기본값 설정
        this.currentStageLevel = 1;
        this.heroStat = null;
        this.soulFragments = 0;
        this.olympusSeals = 0;
        this.isGameOver = false;
    }

    public void startNewRun(String name, UnitData.Stat baseStat, String faction) {
        this.selectedHeroName = name;
        this.selectedFaction = faction;
        this.currentStageLevel = 1;
        this.heroStat = new UnitData.Stat(baseStat); // 원본 보호를 위한 복사
        this.soulFragments = 0;
        this.olympusSeals = 0;
        this.isGameOver = false;
    }
}
