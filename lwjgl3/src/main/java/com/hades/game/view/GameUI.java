package com.hades.game.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
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

    private Array<String> battleLogs;
    private static final int MAX_LOGS = 4; // 화면에 표시될 전투 로그 최대 줄 수

    public GameUI(HadesGame game) {
        this.game = game;
        this.battleLogs = new Array<>();
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

        // 3. 우측 상단 메뉴 박스 (창 모드 텍스트)
        float menuHitboxHeight = menuHitbox.height - 14;
        float menuHitboxwidth = menuHitbox.width - 10;
        float adjustedY = menuHitbox.y + 10;

        game.batch.draw(timerBoxBg, menuHitbox.x, adjustedY, menuHitboxwidth, menuHitboxHeight);
        String screenModeText = Gdx.graphics.isFullscreen() ? "WINDOW" : "FULLSCREEN";
        game.unitFont3.setColor(Color.WHITE);
        game.unitFont3.draw(game.batch, screenModeText, menuHitbox.x, menuHitbox.y + 45, menuHitbox.width, Align.center, false);

        // 4. 전투 로그창 섹션
        float logBgX = 400;
        float logBgY = 10;
        float logWidth = 800;
        float logHeight = 240;

        game.batch.draw(logInfoBg, logBgX, logBgY, logWidth, logHeight);

        float textStartX = logBgX + 150;

        for (int i = 0; i < battleLogs.size; i++) {
            String logLine = battleLogs.get(i);

            // 로그 내용에 따른 색상 분기
            if (logLine.contains("ZEUS") || logLine.contains("적 ") || logLine.contains("패배")) {
                game.unitFont3.setColor(Color.FIREBRICK);
            } else if (logLine.contains("HADES") || logLine.contains("승리")) {
                game.unitFont3.setColor(Color.LIME);
            } else {
                game.unitFont3.setColor(Color.LIGHT_GRAY);
            }

            float textY = logBgY + 160 - (i * 30);
            game.unitFont3.draw(game.batch, logLine, textStartX, textY);
        }

        // 5. 유닛 정보창 (좌측 하단)
        if (selectedUnit != null) {
            renderUnitDetails(selectedUnit);
        }
    }

    private void renderUnitDetails(Unit unit) {
        float uiX = 10;
        float uiY = 20;

        // 배경
        game.batch.draw(unitInfoBg, uiX, uiY, 300, 420);

        // 일러스트
        if (unit.portrait != null) {
            game.batch.draw(unit.portrait, uiX + 65, uiY + 148, 160, 175);
        }

        // 이름
        game.unitFont3.setColor(unit.unitClass == Unit.UnitClass.HERO ? Color.GOLD : Color.WHITE);
        game.unitFont3.draw(game.batch, unit.name, uiX + 60, uiY + 355);

        // 스탯 정보
        game.cardFont.setColor(Color.WHITE);
        game.cardFont.draw(game.batch, "HP : " + unit.currentHp + " / " + unit.stat.hp(), uiX + 45, uiY + 105);
        game.cardFont.draw(game.batch, "ATK: " + unit.stat.atk() + " / CRT: " + unit.stat.counterAtk(), uiX + 45, uiY + 85);
        game.cardFont.draw(game.batch, "RNG: " + unit.stat.range() + " 칸", uiX + 45, uiY + 65);
    }

    @Override
    public void dispose() {
        if (logInfoBg != null) logInfoBg.dispose();
        if (stageInfoBg != null) stageInfoBg.dispose();
        if (unitInfoBg != null) unitInfoBg.dispose();
        if (timerBoxBg != null) timerBoxBg.dispose();
    }
}
