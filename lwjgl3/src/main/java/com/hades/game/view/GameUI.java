package com.hades.game.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
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

// 게임의 모든 UI(상단 정보, 로그, 하단 카드, 우측 스킬)를 렌더링하는 클래스입니다.
public class GameUI implements Disposable {
    private final HadesGame game;
    private Texture logInfoBgTex; // NinePatch용 텍스처
    private NinePatch logPatch;    // 테두리 보호를 위한 나인패치
    private Texture stageInfoBg;
    private Texture timerBoxBg;

    // 전투 로그 관리를 위한 내부 클래스
    private static class LogEntry {
        String text;
        Color color;
        LogEntry(String text, Color color) {
            this.text = text;
            this.color = color;
        }
    }

    private Array<LogEntry> battleLogs;
    private static final int MAX_LOGS = 10; // 확장 시 최대 표시 줄 수

    // --- [로그창 동적 레이아웃 설정] ---
    private final float LOG_AREA_X = 400;
    private final float LOG_AREA_Y = 10;
    private final float LOG_AREA_W = 800;
    private final float MIN_LOG_H = 150;  // 평상시 높이
    private final float MAX_LOG_H = 400;  // 마우스 호버 시 확장 높이
    private float currentLogHeight = MIN_LOG_H; // 현재 실시간 높이 (애니메이션용)
    private final float LERP_SPEED = 0.15f;    // 애니메이션 부드러움 정도
    private final float LOG_LINE_H = 25;       // 로그 줄 간격

    // 마우스 감지 영역 (기본 크기 기준)
    private final Rectangle logHitbox = new Rectangle(LOG_AREA_X, LOG_AREA_Y, LOG_AREA_W, MIN_LOG_H);

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
        // 신규 로그 이미지 로드 및 NinePatch 설정 (테두리 30px 보호)
        logInfoBgTex = new Texture(Gdx.files.internal(path + "log_info.png"));
        logPatch = new NinePatch(logInfoBgTex, 30, 30, 30, 30);

        stageInfoBg = new Texture(Gdx.files.internal(path + "stage_info.png"));
        timerBoxBg = new Texture(Gdx.files.internal(path + "timer_box.png"));
    }

    // 시스템 메시지용 (골드 색상)
    public void addLog(String message) {
        battleLogs.insert(0, new LogEntry(message, Color.GOLD));
        if (battleLogs.size > MAX_LOGS) battleLogs.removeIndex(battleLogs.size - 1);
    }

    // 전투 상황용 (진영별 색상 적용)
    public void addLog(String message, String unitTeam, String playerTeam) {
        Color logColor = Color.LIGHT_GRAY;
        if (unitTeam != null && !unitTeam.equals(playerTeam)) {
            logColor = Color.FIREBRICK; // 적군 로그는 붉은색
        }
        battleLogs.insert(0, new LogEntry(message, logColor));
        if (battleLogs.size > MAX_LOGS) battleLogs.removeIndex(battleLogs.size - 1);
    }

    public void render(int stageLevel, String currentTurn, String playerTeam, Rectangle menuHitbox, Unit selectedUnit, float mx, float my) {
        // 1. [상단 UI] 스테이지 및 턴 정보
        game.batch.draw(stageInfoBg, 20, GameConfig.VIRTUAL_HEIGHT - 80, 200, 60);
        game.unitFont2.setColor(Color.WHITE);
        game.unitFont2.draw(game.batch, "STAGE " + stageLevel, 60, GameConfig.VIRTUAL_HEIGHT - 40);

        game.unitFont2.setColor(currentTurn.equals(playerTeam) ? Color.LIME : Color.RED);
        game.unitFont2.draw(game.batch, currentTurn.equals(playerTeam) ? "YOUR TURN" : "ENEMY TURN", 40, GameConfig.VIRTUAL_HEIGHT - 110);

        // 2. [우측 상단 UI] 메뉴 버튼
        game.batch.draw(timerBoxBg, menuHitbox.x, menuHitbox.y + 10, menuHitbox.width - 10, menuHitbox.height - 14);
        String mode = Gdx.graphics.isFullscreen() ? "WINDOW" : "FULLSCREEN";
        game.unitFont3.draw(game.batch, mode, menuHitbox.x, menuHitbox.y + 40, menuHitbox.width, Align.center, false);

        // 3. [하단 중앙 UI] 배경 자동 확장 전투 로그
        renderExpandableLog(mx, my);

        // 4. [하단/우측 유닛 UI] 선택된 유닛 정보
        if (selectedUnit != null && selectedUnit.isAlive()) {
            renderUnitDetails(selectedUnit);
            if (selectedUnit.unitClass == Unit.UnitClass.HERO) {
                renderHeroSkills(selectedUnit, mx, my, playerTeam);
            }
        }
    }

    // 로그창 자동 확장 렌더링 로직
    private void renderExpandableLog(float mx, float my) {
        // 호버 상태에 따른 목표 높이 설정
        boolean isHovered = logHitbox.contains(mx, my);
        float targetH = isHovered ? MAX_LOG_H : MIN_LOG_H;
        int displayCount = isHovered ? 10 : 4;

        // 높이 애니메이션 (부드럽게 목표치에 도달)
        currentLogHeight = MathUtils.lerp(currentLogHeight, targetH, LERP_SPEED);

        // NinePatch 배경 그리기 (테두리 뭉개짐 방지)
        logPatch.draw(game.batch, LOG_AREA_X, LOG_AREA_Y, LOG_AREA_W, currentLogHeight);

        // 로그 텍스트 출력 (최신 로그가 하단에 위치)
        for (int i = 0; i < Math.min(battleLogs.size, displayCount); i++) {
            LogEntry entry = battleLogs.get(i);

            // i=0(최신)이 아래에 오도록 텍스트 로그 Y좌표 계산
            float logY = LOG_AREA_Y + 65 + (i * LOG_LINE_H);

            // 박스 현재 높이 내부에 있을 때만 그리기 (클리핑 효과)
            if (logY < LOG_AREA_Y + currentLogHeight - 30) {
                game.unitFont3.setColor(entry.color.r, entry.color.g, entry.color.b, isHovered ? 1.0f : 0.8f);
                game.unitFont3.draw(game.batch, entry.text, LOG_AREA_X + 55, logY);    // 로그 텍스트 x좌표 계산
            }
        }
        game.unitFont3.setColor(Color.WHITE);
    }

    // 영웅 유닛의 스킬 목록 표시
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

            if (!isPlayerUnit) {
                game.unitFont3.setColor(Color.WHITE);
            } else {
                if (!unit.stat.isSkillReady(skillName)) {
                    game.unitFont3.setColor(Color.GRAY);
                } else if (skillName.equals(reserved)) {
                    game.unitFont3.setColor(Color.YELLOW);
                } else {
                    game.unitFont3.setColor(Color.WHITE);
                }
            }

            game.unitFont3.draw(game.batch, (i + 1) + ". " + skillName, SKILL_X + 10, y - 5);

            if (rect.contains(mx, my)) {
                tooltipToDraw = skillName;
            }
        }

        if (tooltipToDraw != null) {
            renderSkillTooltip(tooltipToDraw, mx, my);
        }
        game.unitFont3.setColor(Color.WHITE);
    }

    // 스킬 마우스 호버 시 툴팁 표시
    private void renderSkillTooltip(String skillName, float mx, float my) {
        SkillData.Skill data = SkillData.get(skillName);
        if (data == null) return;

        float tw = 400;
        float th = 220;
        float tx = mx - tw - 20;
        float ty = my - 100;

        if (tx < 10) tx = mx + 20;

        // 툴팁 배경도 동일한 UI 스타일 적용
        game.batch.draw(logInfoBgTex, tx, ty, tw, th);

        float marginLeft = 60;
        float marginTop = 55;
        float lineSpacing = 35;
        float contentWidth = tw - (marginLeft * 2);
        float currentY = ty + th - marginTop;

        game.unitFont3.setColor(Color.CYAN);
        game.unitFont3.draw(game.batch, "[" + data.name + "]", tx + marginLeft, currentY);

        currentY -= lineSpacing;
        game.unitFont3.setColor(Color.ORANGE);
        game.unitFont3.draw(game.batch, "위력: " + (int)(data.power * 100) + "% | 사거리: " + data.range, tx + marginLeft, currentY);

        currentY -= lineSpacing;
        game.unitFont3.setColor(Color.WHITE);
        game.unitFont3.draw(game.batch, data.description, tx + marginLeft, currentY, contentWidth, Align.left, true);
    }

    // 클릭 시 스킬 선택 여부 반환
    public String getClickedSkill(float mx, float my, Unit unit) {
        if (unit == null) return null;
        Array<String> allSkills = unit.stat.getLearnedSkills();
        Array<String> visibleSkills = new Array<>();
        for (String s : allSkills) {
            if (!s.equals("기본 공격")) visibleSkills.add(s);
        }

        for (int i = 0; i < visibleSkills.size; i++) {
            String name = visibleSkills.get(i);
            Rectangle rect = new Rectangle(SKILL_X - 20, SKILL_Y - (i * SKILL_H) - 35, 250, 45);
            if (rect.contains(mx, my)) {
                if (!unit.stat.isSkillReady(name)) return null;
                return name;
            }
        }
        return null;
    }

    // 좌측 하단 유닛 상태 카드 정보 표시
    private void renderUnitDetails(Unit unit) {
        if (unit.portrait != null) game.batch.draw(unit.portrait, 10, 20, 300, 420);

        game.cardFont.setColor(Color.WHITE);
        game.cardFont.draw(game.batch, "HP : " + unit.currentHp + " / " + unit.stat.hp(), 55, 145);
        game.cardFont.draw(game.batch, "ATK: " + unit.stat.atk(), 55, 125);
        game.cardFont.draw(game.batch, "CTK: " + unit.stat.counterAtk(), 55, 105);
        game.cardFont.setColor(Color.SKY);
        game.cardFont.draw(game.batch, "RNG: " + unit.stat.range(), 200, 105);
        game.cardFont.setColor(Color.WHITE);
    }

    @Override
    public void dispose() {
        if (logInfoBgTex != null) logInfoBgTex.dispose();
        if (stageInfoBg != null) stageInfoBg.dispose();
        if (timerBoxBg != null) timerBoxBg.dispose();
    }
}
