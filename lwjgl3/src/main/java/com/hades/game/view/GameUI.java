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
    private Texture timerBoxBg;

    // 로그 텍스트와 색상을 함께 저장하기 위한 내부 클래스
    private static class LogEntry {
        String text;
        Color color;
        LogEntry(String text, Color color) {
            this.text = text;
            this.color = color;
        }
    }

    private Array<LogEntry> battleLogs;
    private static final int MAX_LOGS = 4;

    private final float SKILL_X = GameConfig.VIRTUAL_WIDTH - 280;
    private final float SKILL_Y = 500;
    private final float SKILL_H = 50;

    public GameUI(HadesGame game) {
        this.game = game;
        this.battleLogs = new Array<>();
        loadResources();
    }

    private void loadResources() {
        String path = "images/background/";
        logInfoBg = new Texture(Gdx.files.internal(path + "log_info.png"));
        stageInfoBg = new Texture(Gdx.files.internal(path + "stage_info.png"));
        timerBoxBg = new Texture(Gdx.files.internal(path + "timer_box.png"));
    }

    // 일반 시스템 메시지용 (황금색)
    public void addLog(String message) {
        battleLogs.insert(0, new LogEntry(message, Color.GOLD));
        if (battleLogs.size > MAX_LOGS) battleLogs.removeIndex(battleLogs.size - 1);
    }

    // 전투 행동 로그용 (팀에 따라 색상 자동 결정)
    public void addLog(String message, String unitTeam, String playerTeam) {
        Color logColor = Color.LIGHT_GRAY;
        // 유닛의 팀이 플레이어 팀과 다르면 적군으로 간주하여 빨간색 적용
        if (unitTeam != null && !unitTeam.equals(playerTeam)) {
            logColor = Color.FIREBRICK;
        }
        battleLogs.insert(0, new LogEntry(message, logColor));
        if (battleLogs.size > MAX_LOGS) battleLogs.removeIndex(battleLogs.size - 1);
    }

    public void render(int stageLevel, String currentTurn, String playerTeam, Rectangle menuHitbox, Unit selectedUnit) {
        // 1. 스테이지 및 턴 정보
        game.batch.draw(stageInfoBg, 20, GameConfig.VIRTUAL_HEIGHT - 80, 200, 60);
        game.unitFont2.setColor(Color.WHITE);
        game.unitFont2.draw(game.batch, "STAGE " + stageLevel, 60, GameConfig.VIRTUAL_HEIGHT - 40);

        game.unitFont2.setColor(currentTurn.equals(playerTeam) ? Color.LIME : Color.RED);
        game.unitFont2.draw(game.batch, currentTurn.equals(playerTeam) ? "YOUR TURN" : "ENEMY TURN", 40, GameConfig.VIRTUAL_HEIGHT - 110);

        // 2. 우측 상단 메뉴
        game.batch.draw(timerBoxBg, menuHitbox.x, menuHitbox.y + 10, menuHitbox.width - 10, menuHitbox.height - 14);
        String mode = Gdx.graphics.isFullscreen() ? "WINDOW" : "FULLSCREEN";
        game.unitFont3.draw(game.batch, mode, menuHitbox.x, menuHitbox.y + 45, menuHitbox.width, Align.center, false);

        // 3. 전투 로그 (GameUI가 색상을 직접 관리)
        game.batch.draw(logInfoBg, 400, 10, 800, 240);
        for (int i = 0; i < battleLogs.size; i++) {
            LogEntry entry = battleLogs.get(i);
            game.unitFont3.setColor(entry.color);
            game.unitFont3.draw(game.batch, entry.text, 550, 170 - (i * 30));
        }
        game.unitFont3.setColor(Color.WHITE);

        // 4. 유닛 상세 정보
        if (selectedUnit != null) {
            renderUnitDetails(selectedUnit);
            if (selectedUnit.team.equals(playerTeam) && selectedUnit.unitClass == Unit.UnitClass.HERO) {
                renderHeroSkills(selectedUnit);
            }
        }
    }

    private void renderHeroSkills(Unit unit) {
        Array<String> allSkills = unit.stat.getLearnedSkills();
        Array<String> visibleSkills = new Array<>();
        for (String s : allSkills) {
            if (!s.equals("기본 공격")) visibleSkills.add(s);
        }

        if (visibleSkills.size == 0) return;

        game.unitFont2.setColor(Color.GOLD);
        game.unitFont2.draw(game.batch, "[ 보유 권능 ]", SKILL_X, SKILL_Y + 50);

        boolean isUsed = unit.stat.isSkillUsed();
        String reserved = unit.stat.getReservedSkill();

        for (int i = 0; i < visibleSkills.size; i++) {
            String skillName = visibleSkills.get(i);
            float y = SKILL_Y - (i * SKILL_H);
            game.batch.draw(timerBoxBg, SKILL_X - 20, y - 35, 250, 45); // 스킬 목록 UI

            if (isUsed) game.unitFont3.setColor(Color.GRAY);
            else if (skillName.equals(reserved)) game.unitFont3.setColor(Color.YELLOW);
            else game.unitFont3.setColor(Color.WHITE);

            game.unitFont3.draw(game.batch, (i + 1) + ". " + skillName, SKILL_X, y);
        }
        game.unitFont3.setColor(Color.WHITE);
    }

    public String getClickedSkill(float mx, float my, Unit unit) {
        if (unit == null || unit.stat.isSkillUsed()) return null;
        Array<String> allSkills = unit.stat.getLearnedSkills();
        Array<String> visibleSkills = new Array<>();
        for (String s : allSkills) {
            if (!s.equals("기본 공격")) visibleSkills.add(s);
        }

        for (int i = 0; i < visibleSkills.size; i++) {
            Rectangle rect = new Rectangle(SKILL_X - 10, SKILL_Y - (i * SKILL_H) - 35, 250, 45);
            if (rect.contains(mx, my)) return visibleSkills.get(i);
        }
        return null;
    }

    private void renderUnitDetails(Unit unit) {
        if (unit.portrait != null) game.batch.draw(unit.portrait, 10, 20, 300, 420);
        game.cardFont.setColor(Color.WHITE);
        game.cardFont.draw(game.batch, "HP : " + unit.currentHp + " / " + unit.stat.hp(), 55, 145);
        game.cardFont.draw(game.batch, "ATK: " + unit.stat.atk(), 55, 125);
        game.cardFont.draw(game.batch, "CRK: " + unit.stat.counterAtk(), 55, 105);
    }

    @Override
    public void dispose() {
        logInfoBg.dispose();
        stageInfoBg.dispose();
        timerBoxBg.dispose();
    }
}
