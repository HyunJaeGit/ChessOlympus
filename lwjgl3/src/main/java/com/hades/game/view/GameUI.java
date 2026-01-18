package com.hades.game.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.hades.game.HadesGame;
import com.hades.game.constants.GameConfig;
import com.hades.game.constants.SkillData;
import com.hades.game.entities.Unit;

// Chess Olympus: HADES vs ZEUS - 전투 화면 UI 렌더링 클래스
public class GameUI implements Disposable {
    private final HadesGame game;

    // 리소스 관련 변수
    private Texture logInfoBgTex;
    private NinePatch logPatch;
    private Texture stageInfoBg;
    private Texture timerBoxBg;

    // HELP 버튼 관련 변수
    private final float HELP_BTN_W = 120;
    private final Rectangle helpBtnHitbox = new Rectangle();

    // 로그 시스템 관련 클래스
    private static class LogEntry {
        final Color color;
        final GlyphLayout layout;

        LogEntry(String text, Color color, com.badlogic.gdx.graphics.g2d.BitmapFont font) {
            this.color = color;
            this.layout = new GlyphLayout(font, text);
        }
    }

    private static class PendingLog {
        final String message;
        final Color color;
        PendingLog(String m, Color c) {
            this.message = m;
            this.color = c;
        }
    }

    private final Array<LogEntry> battleLogs = new Array<>();
    private final Array<PendingLog> pendingQueue = new Array<>();
    private static final int MAX_LOGS = 12;

    // 로그 영역 설정
    private final float LOG_AREA_X = 400;
    private final float LOG_AREA_Y = 10;
    private final float LOG_AREA_W = 800;
    private final float MIN_LOG_H = 150;
    private final float MAX_LOG_H = 400;
    private float currentLogHeight = MIN_LOG_H;
    private final float LERP_SPEED = 0.15f;
    private final float LOG_LINE_H = 28;

    private final Rectangle logHitbox = new Rectangle(LOG_AREA_X, LOG_AREA_Y, LOG_AREA_W, MIN_LOG_H);

    // 스킬 UI 배치 상수
    private final float SKILL_X = GameConfig.VIRTUAL_WIDTH - 280;
    private final float SKILL_Y = 500;
    private final float SKILL_H = 50;

    public GameUI(HadesGame game) {
        this.game = game;
        loadResources();
    }

    private void loadResources() {
        String path = "images/background/";
        logInfoBgTex = new Texture(Gdx.files.internal(path + "log_info.png"));
        logPatch = new NinePatch(logInfoBgTex, 30, 30, 30, 30);
        stageInfoBg = new Texture(Gdx.files.internal(path + "stage_info.png"));
        timerBoxBg = new Texture(Gdx.files.internal(path + "timer_box.png"));
    }

    public void addLog(String message) {
        addLog(message, "SYSTEM", "PLAYER");
    }

    public void addLog(String message, String unitTeam, String playerTeam) {
        Color logColor = Color.LIGHT_GRAY;
        if ("SYSTEM".equals(unitTeam)) logColor = Color.GOLD;
        else if (unitTeam != null && !unitTeam.equals(playerTeam)) logColor = Color.FIREBRICK;

        synchronized (pendingQueue) {
            pendingQueue.add(new PendingLog(message, logColor));
        }
    }

    private void flushLogs() {
        synchronized (pendingQueue) {
            if (pendingQueue.size == 0) return;
            for (int i = 0; i < pendingQueue.size; i++) {
                PendingLog p = pendingQueue.get(i);
                battleLogs.insert(0, new LogEntry(p.message, p.color, game.unitFont3));
                if (battleLogs.size > MAX_LOGS) battleLogs.removeIndex(battleLogs.size - 1);
            }
            pendingQueue.clear();
        }
    }

    // 메인 UI 렌더링
    public void render(int stageLevel, String currentTurn, String playerTeam, Rectangle menuHitbox, Unit selectedUnit, float mx, float my, boolean showHelp) {
        flushLogs();

        // 1. 상단 정보
        game.batch.draw(stageInfoBg, 20, GameConfig.VIRTUAL_HEIGHT - 80, 200, 60);
        game.unitFont2.setColor(Color.WHITE);
        game.unitFont2.draw(game.batch, "STAGE " + stageLevel, 60, GameConfig.VIRTUAL_HEIGHT - 40);

        game.unitFont2.setColor(currentTurn.equals(playerTeam) ? Color.LIME : Color.RED);
        game.unitFont2.draw(game.batch, currentTurn.equals(playerTeam) ? "YOUR TURN" : "ENEMY TURN", 40, GameConfig.VIRTUAL_HEIGHT - 110);

        // 2. 상단 버튼 (HELP & WINDOW)
        helpBtnHitbox.set(menuHitbox.x - HELP_BTN_W - 15, menuHitbox.y + 10, HELP_BTN_W, menuHitbox.height - 14);
        game.batch.draw(timerBoxBg, helpBtnHitbox.x, helpBtnHitbox.y, helpBtnHitbox.width, helpBtnHitbox.height);

        boolean isHelpHover = helpBtnHitbox.contains(mx, my);
        game.unitFont3.setColor(isHelpHover ? Color.GOLD : Color.WHITE);
        game.unitFont3.draw(game.batch, "HELP", helpBtnHitbox.x, helpBtnHitbox.y + 33, helpBtnHitbox.width, Align.center, false);

        game.batch.draw(timerBoxBg, menuHitbox.x, menuHitbox.y + 10, menuHitbox.width - 10, menuHitbox.height - 14);
        String mode = Gdx.graphics.isFullscreen() ? "WINDOW" : "FULLSCREEN";
        game.unitFont3.setColor(Color.WHITE);
        game.unitFont3.draw(game.batch, mode, menuHitbox.x, menuHitbox.y + 43, menuHitbox.width, Align.center, false);

        renderExpandableLog(mx, my);

        // 3. 유닛 정보 및 스킬
        if (selectedUnit != null && selectedUnit.isAlive()) {
            renderUnitDetails(selectedUnit);
            if (selectedUnit.unitClass == Unit.UnitClass.HERO) {
                renderHeroSkills(selectedUnit, mx, my, playerTeam);
            }
        }

        // 4. 도움말 오버레이
        if (showHelp) {
            renderHelpWindow();
        }
    }

    private void renderExpandableLog(float mx, float my) {
        boolean isHovered = logHitbox.contains(mx, my);
        float targetH = isHovered ? MAX_LOG_H : MIN_LOG_H;
        int displayCount = isHovered ? MAX_LOGS : 4;

        currentLogHeight = MathUtils.lerp(currentLogHeight, targetH, LERP_SPEED);
        logPatch.draw(game.batch, LOG_AREA_X, LOG_AREA_Y, LOG_AREA_W, currentLogHeight);

        for (int i = 0; i < Math.min(battleLogs.size, displayCount); i++) {
            LogEntry entry = battleLogs.get(i);
            float logY = LOG_AREA_Y + 65 + (i * LOG_LINE_H);
            if (logY < LOG_AREA_Y + currentLogHeight - 30) {
                game.unitFont3.setColor(entry.color.r, entry.color.g, entry.color.b, isHovered ? 1.0f : 0.7f);
                game.unitFont3.draw(game.batch, entry.layout, LOG_AREA_X + 55, logY);
            }
        }
        game.unitFont3.setColor(Color.WHITE);
    }

    private void renderHeroSkills(Unit unit, float mx, float my, String playerTeam) {
        Array<String> allSkills = unit.stat.getLearnedSkills();
        Array<String> visibleSkills = new Array<>();
        for (String s : allSkills) {
            if (!s.equals("기본 공격")) visibleSkills.add(s);
        }

        if (visibleSkills.size == 0) return;

        boolean isPlayerUnit = unit.team.equals(playerTeam);
        game.unitFont2.setColor(isPlayerUnit ? Color.GOLD : Color.FIREBRICK);
        game.unitFont2.draw(game.batch, isPlayerUnit ? "[ 보유 권능 ]" : "[ 적의 권능 ]", SKILL_X, SKILL_Y + 50);

        String reserved = unit.stat.getReservedSkill();
        String tooltipToDraw = null;

        for (int i = 0; i < visibleSkills.size; i++) {
            String skillName = visibleSkills.get(i);
            float y = SKILL_Y - (i * SKILL_H);
            Rectangle rect = new Rectangle(SKILL_X - 20, y - 35, 250, 45);

            game.batch.draw(timerBoxBg, rect.x, rect.y, rect.width, rect.height);

            if (!isPlayerUnit) game.unitFont3.setColor(Color.WHITE);
            else {
                if (!unit.stat.isSkillReady(skillName)) game.unitFont3.setColor(Color.GRAY);
                else if (skillName.equals(reserved)) game.unitFont3.setColor(Color.YELLOW);
                else game.unitFont3.setColor(Color.WHITE);
            }

            game.unitFont3.draw(game.batch, (i + 1) + ". " + skillName, SKILL_X + 10, y - 5);
            if (rect.contains(mx, my)) tooltipToDraw = skillName;
        }
        if (tooltipToDraw != null) renderSkillTooltip(tooltipToDraw, mx, my);
    }

    private void renderSkillTooltip(String skillName, float mx, float my) {
        SkillData.Skill data = SkillData.get(skillName);
        if (data == null) return;

        float tw = 400, th = 220;
        float tx = (mx - tw - 20 < 10) ? mx + 20 : mx - tw - 20;
        float ty = my - 100;

        game.batch.draw(logInfoBgTex, tx, ty, tw, th);
        float marginLeft = 60, marginTop = 55;
        float currentY = ty + th - marginTop;

        game.unitFont3.setColor(Color.CYAN);
        game.unitFont3.draw(game.batch, "[" + data.name + "]", tx + marginLeft, currentY);
        currentY -= 35;
        game.unitFont3.setColor(Color.ORANGE);
        game.unitFont3.draw(game.batch, "위력: " + (int)(data.power * 100) + "% | 사거리: " + data.range, tx + marginLeft, currentY);
        currentY -= 35;
        game.unitFont3.setColor(Color.WHITE);
        game.unitFont3.draw(game.batch, data.description, tx + marginLeft, currentY, tw - (marginLeft * 2), Align.left, true);
    }

    // 도움말 창 렌더링
    private void renderHelpWindow() {
        float winW = 850;
        float winH = 500;
        float winX = (GameConfig.VIRTUAL_WIDTH - winW) / 2;
        float winY = (GameConfig.VIRTUAL_HEIGHT - winH) / 2;

        game.batch.setColor(0, 0, 0, 0.9f);
        logPatch.draw(game.batch, winX, winY, winW, winH);
        game.batch.setColor(Color.WHITE);

        float textX = winX + 60;
        float startY = winY + winH - 60;

        game.unitFont2.setColor(Color.GOLD);
        game.unitFont2.draw(game.batch, "== GAME RULES & CONTROLS ==", textX, startY);

        game.unitFont3.setColor(Color.WHITE);
        String help = "\n[ 이동 및 공격 ]\n- 좌클릭으로 아군 선택 후 이동 타일 클릭\n- 이동 후 자동 공격\n\n[ 권능 사용 ]\n- 스킬 클릭하여 '장전' 후 이동\n\n[ 카메라 ]\n- 휠: 줌 | 우클릭 드래그: 이동 (떼면 복귀)";
        game.unitFont3.draw(game.batch, help, textX, startY - 40, winW - 120, Align.left, true);

        game.unitFont3.setColor(Color.GRAY);
        game.unitFont3.draw(game.batch, "(화면 아무 곳이나 클릭하여 닫기)", winX, winY + 50, winW, Align.center, false);
    }

    public String getClickedSkill(float mx, float my, Unit unit) {
        if (unit == null) return null;
        Array<String> allSkills = unit.stat.getLearnedSkills();
        for (int i = 0, j = 0; i < allSkills.size; i++) {
            String name = allSkills.get(i);
            if (name.equals("기본 공격")) continue;
            Rectangle rect = new Rectangle(SKILL_X - 20, SKILL_Y - (j * SKILL_H) - 35, 250, 45);
            if (rect.contains(mx, my)) {
                if (!unit.stat.isSkillReady(name)) return null;
                return name;
            }
            j++;
        }
        return null;
    }

    public boolean isHelpClicked(float mx, float my) {
        return helpBtnHitbox.contains(mx, my);
    }

    private void renderUnitDetails(Unit unit) {
        if (unit.portrait != null) game.batch.draw(unit.portrait, 10, 20, 300, 420);
        game.cardFont.setColor(Color.WHITE);
        game.cardFont.draw(game.batch, "HP : " + unit.currentHp + " / " + unit.stat.hp(), 55, 145);
        game.cardFont.draw(game.batch, "ATK: " + unit.stat.atk(), 55, 125);
        game.cardFont.draw(game.batch, "CTK: " + unit.stat.counterAtk(), 55, 105);
        game.cardFont.setColor(Color.SKY);
        game.cardFont.draw(game.batch, "RNG: " + unit.stat.range(), 200, 105);
    }

    @Override
    public void dispose() {
        if (logInfoBgTex != null) logInfoBgTex.dispose();
        if (stageInfoBg != null) stageInfoBg.dispose();
        if (timerBoxBg != null) timerBoxBg.dispose();
        battleLogs.clear();
    }
}
