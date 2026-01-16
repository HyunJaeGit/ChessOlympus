package com.hades.game.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.hades.game.HadesGame;
import com.hades.game.constants.GameConfig;
import com.hades.game.entities.Unit;

public class GameUI implements Disposable {
    private final HadesGame game;
    private Texture logInfoBg;
    private Texture stageInfoBg;
    private Texture unitInfoBg;
    private Texture timerBoxBg;
    private ShapeRenderer debugShape; // 디버그용 그리기 도구

    private Array<String> battleLogs;
    private static final int MAX_LOGS = 5; // 화면에 표시될 전투 로그 최대 줄 수

    public GameUI(HadesGame game) {
        this.game = game;
        this.battleLogs = new Array<>();
        this.debugShape = new ShapeRenderer(); // 초기화
        loadResources();
    }

    private void loadResources() {
        String path = "images/background/";
        logInfoBg = new Texture(Gdx.files.internal(path + "log_info.png"));
        stageInfoBg = new Texture(Gdx.files.internal(path + "stage_info.png"));
        unitInfoBg = new Texture(Gdx.files.internal(path + "unit_info.png"));
        timerBoxBg = new Texture(Gdx.files.internal(path + "timer_box.png"));
    }

    public void addLog(String message) {
        battleLogs.insert(0, message); // 새 로그를 맨 위(0번)에 삽입
        if (battleLogs.size > MAX_LOGS) {
            battleLogs.removeIndex(battleLogs.size - 1);
        }
    }

    public void render(int stageLevel, String currentTurn, String playerTeam, Rectangle menuHitbox, Unit selectedUnit) {
        // 1. 스테이지 정보 (좌측 상단)
        game.batch.draw(stageInfoBg, 20, GameConfig.VIRTUAL_HEIGHT - 80, 200, 60);
        game.unitFont2.setColor(Color.WHITE);
        game.unitFont2.draw(game.batch, "STAGE " + stageLevel, 60, GameConfig.VIRTUAL_HEIGHT - 40);

        // 2. 턴 정보
        game.unitFont2.setColor(currentTurn.equals(playerTeam) ? Color.LIME : Color.RED);
        game.unitFont2.draw(game.batch, currentTurn.equals(playerTeam) ? "YOUR TURN" : "ENEMY TURN", 40, GameConfig.VIRTUAL_HEIGHT - 110);

        // 3. 우측 상단 메뉴 박스
        float menuHitboxHeight = menuHitbox.height - 10; // 메뉴 히트박스를 원래 높이보다 20픽셀 줄임
        float menuHitboxwidth = menuHitbox.height - 10; // 메뉴 히트박스를 원래 높이보다 20픽셀 줄임
        float adjustedY = menuHitbox.y + 10; // 위로 10픽셀 이동 (취향에 따라 조정)

        game.batch.draw(timerBoxBg, menuHitbox.x, adjustedY, menuHitboxwidth, menuHitboxHeight);
        String screenModeText = Gdx.graphics.isFullscreen() ? "WINDOW" : "FULLSCREEN";
        game.unitFont3.setColor(Color.WHITE);
        game.unitFont3.draw(game.batch, screenModeText, menuHitbox.x, menuHitbox.y + 45, menuHitbox.width, Align.center, false);

        // 4. 전투 로그창 섹션
        // 창의 위치와 크기
        float logBgX = 400; // 로그창 배경의 시작 X
        float logBgY = 15;  // 로그창 배경의 시작 Y
        float logWidth = 600; // 로그창의 가로 길이 (고정)
        float logHeight = 200; // 로그창의 세로 길이 (고정)

        // 배경 그리기 (이제 logX를 바꿔도 크기가 변하지 않습니다)
        game.batch.draw(logInfoBg, logBgX, logBgY, logWidth, logHeight);

        // 전투 로그 텍스트 위치 설정
        float textStartX = logBgX + 150; // 배경 시작점에서 오른쪽으로 50픽셀 안쪽부터 글자 시작
        game.unitFont3.setColor(Color.LIGHT_GRAY); // 텍스트 컬러

        // 전투 로그 텍스트 출력
        for (int i = 0; i < battleLogs.size; i++) {
            String logLine = battleLogs.get(i);
            // 로그 내용에 적 팀 이름(ZEUS)이나 "적"이라는 단어가 있으면 빨간색으로 표시
            // playerTeam이 HADES라면, ZEUS가 들어간 로그는 적의 행동입니다.
            if (logLine.contains("ZEUS") || logLine.contains("적 ") || logLine.contains("패배")) {
                game.unitFont3.setColor(Color.FIREBRICK); // 적의 행동은 어두운 빨강
            } else if (logLine.contains("HADES") || logLine.contains("승리")) {
                game.unitFont3.setColor(Color.LIME); // 내 행동 중 강조할 것은 라임색 (선택사항)
            } else {
                game.unitFont3.setColor(Color.LIGHT_GRAY); // 기본 로그는 밝은 회색
            }
            float textY = logBgY + 160 - (i * 30);
            game.unitFont3.draw(game.batch, logLine, textStartX, textY);
        }

        // 5. 유닛 정보창 (좌측 하단)
        if (selectedUnit != null) {
            renderUnitDetails(selectedUnit);
        }

        // 6. 레이아웃 디버그 테두리 그리기 (테스트용)
        // 주의: batch.end() 이후에 호출해야 도형이 이미지 위로 덮어씌워집니다.
        game.batch.end();
        drawDebugFrames(menuHitbox, selectedUnit);
        game.batch.begin();
    }

    // 테스트용 영역 시각화 메서드 (테스트 끝나면 비활성화)
    private void drawDebugFrames(Rectangle menuHitbox, Unit selectedUnit) {
        debugShape.setProjectionMatrix(game.batch.getProjectionMatrix());
        debugShape.begin(ShapeRenderer.ShapeType.Line);

        // 1 & 2 섹션: 스테이지 및 턴 정보 (노란색)
        debugShape.setColor(Color.YELLOW);
        debugShape.rect(20, GameConfig.VIRTUAL_HEIGHT - 120, 240, 110);

        // 3 섹션: 메뉴 박스 (하늘색)
        debugShape.setColor(Color.CYAN);
        debugShape.rect(menuHitbox.x, menuHitbox.y, menuHitbox.width, menuHitbox.height);

        // 4 섹션: 전투 로그창 (빨간색 - 텍스트 시작점 포함)
        debugShape.setColor(Color.RED);
        float logBgX = 400;
        float logBgY = 15;
        float logWidth = 900;
        float logHeight = 200;
        debugShape.rect(logBgX, logBgY, logWidth, logHeight); // 배경 영역
        debugShape.line(logBgX + 150, logBgY, logBgX + 150, logBgY + logHeight); // 텍스트 시작선(textStartX)

        // 5 섹션: 유닛 정보창 (녹색)
        if (selectedUnit != null) {
            debugShape.setColor(Color.GREEN);
            debugShape.rect(10, 20, 300, 420);
        }

        debugShape.end();
    }
    // 유닛 카드 상세정보
    private void renderUnitDetails(Unit unit) {
        float uiX = 10;
        float uiY = 20;
        game.batch.draw(unitInfoBg, uiX, uiY, 300, 420);

        // 유닛 카드 상세정보(이름)
        game.unitFont3.setColor(unit.unitClass == Unit.UnitClass.HERO ? Color.GOLD : Color.WHITE);
        game.unitFont3.draw(game.batch, unit.name, uiX + 60, uiY + 355);

        // 스탯 나열 (Y값을 20씩 깎으면서 줄바꿈 효과)
        game.cardFont.setColor(Color.WHITE);
        game.cardFont.draw(game.batch, "HP : " + unit.currentHp + " / " + unit.stat.hp(), uiX + 45, uiY + 105);
        // 2. ATK(공격력) / CRT(반격력) 표시
        game.cardFont.draw(game.batch, "ATK: " + unit.stat.atk() + " / CRT: " + unit.stat.counterAtk(), uiX + 45, uiY + 85);
        game.cardFont.draw(game.batch, "RNG: " + unit.stat.range() + " 칸", uiX + 45, uiY + 65);

    }

    @Override
    public void dispose() {
        logInfoBg.dispose();
        stageInfoBg.dispose();
        unitInfoBg.dispose();
        timerBoxBg.dispose();
        if (debugShape != null) debugShape.dispose(); // 레이아웃 디버그용 그리기 도구 메모리 청소
    }
}
