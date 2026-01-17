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
import com.hades.game.constants.SkillData;
import com.hades.game.entities.Unit;

// 게임의 모든 UI(상단 정보, 로그, 하단 카드, 우측 스킬)를 렌더링하는 클래스입니다.
public class GameUI implements Disposable {
    private final HadesGame game;
    private Texture logInfoBg;
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
    private static final int MAX_LOGS = 4; // 표시할 최대 로그 줄 수

    // --- [미세조정: 우측 스킬 목록 레이아웃] ---
    private final float SKILL_X = GameConfig.VIRTUAL_WIDTH - 280; // 스킬 목록 시작 X 좌표
    private final float SKILL_Y = 500;                            // 첫 번째 스킬 항목의 Y 좌표
    private final float SKILL_H = 50;                             // 스킬 항목 간의 세로 간격

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

    // 시스템 메시지용 (골드)
    public void addLog(String message) {
        battleLogs.insert(0, new LogEntry(message, Color.GOLD));
        if (battleLogs.size > MAX_LOGS) battleLogs.removeIndex(battleLogs.size - 1);
    }

    // 전투 상황용 (진영별 색상 적용)
    public void addLog(String message, String unitTeam, String playerTeam) {
        Color logColor = Color.LIGHT_GRAY;
        if (unitTeam != null && !unitTeam.equals(playerTeam)) {
            logColor = Color.FIREBRICK; // 적군 관련 로그는 붉은색
        }
        battleLogs.insert(0, new LogEntry(message, logColor));
        if (battleLogs.size > MAX_LOGS) battleLogs.removeIndex(battleLogs.size - 1);
    }

    public void render(int stageLevel, String currentTurn, String playerTeam, Rectangle menuHitbox, Unit selectedUnit, float mx, float my) {
        // 1. [상단 UI] 스테이지 레벨 및 현재 턴 정보
        game.batch.draw(stageInfoBg, 20, GameConfig.VIRTUAL_HEIGHT - 80, 200, 60);
        game.unitFont2.setColor(Color.WHITE);
        game.unitFont2.draw(game.batch, "STAGE " + stageLevel, 60, GameConfig.VIRTUAL_HEIGHT - 40);

        game.unitFont2.setColor(currentTurn.equals(playerTeam) ? Color.LIME : Color.RED);
        game.unitFont2.draw(game.batch, currentTurn.equals(playerTeam) ? "YOUR TURN" : "ENEMY TURN", 40, GameConfig.VIRTUAL_HEIGHT - 110);

        // 2. [우측 상단 UI] 설정/전체화면 메뉴 버튼
        game.batch.draw(timerBoxBg, menuHitbox.x, menuHitbox.y + 10, menuHitbox.width - 10, menuHitbox.height - 14);
        String mode = Gdx.graphics.isFullscreen() ? "WINDOW" : "FULLSCREEN";
        game.unitFont3.draw(game.batch, mode, menuHitbox.x, menuHitbox.y + 40, menuHitbox.width, Align.center, false);

        // 3. [하단 중앙 UI] 실시간 전투 로그 출력
        game.batch.draw(logInfoBg, 400, 10, 800, 240);
        for (int i = 0; i < battleLogs.size; i++) {
            LogEntry entry = battleLogs.get(i);
            game.unitFont3.setColor(entry.color);
            game.unitFont3.draw(game.batch, entry.text, 550, 170 - (i * 30)); // 30 간격으로 위에서 아래로 출력
        }
        game.unitFont3.setColor(Color.WHITE);

        // 4. [하단/우측 유닛 UI] 선택된 유닛이 존재하고 살아있을 때만 렌더링 (사망 시 에러 방지 핵심)
        if (selectedUnit != null && selectedUnit.isAlive()) {
            renderUnitDetails(selectedUnit); // 좌측 하단 유닛 카드(스탯) 표시

            // 영웅 유닛(HERO)이라면 진영에 관계없이 보유한 스킬 목록 표시
            if (selectedUnit.unitClass == Unit.UnitClass.HERO) {
                renderHeroSkills(selectedUnit, mx, my, playerTeam);
            }
        }
    }

    private void renderHeroSkills(Unit unit, float mx, float my, String playerTeam) {
        Array<String> allSkills = unit.stat.getLearnedSkills();
        Array<String> visibleSkills = new Array<>();
        for (String s : allSkills) {
            if (!s.equals("기본 공격")) visibleSkills.add(s); // 평타를 제외한 권능들만 추출
        }

        if (visibleSkills.size == 0) return;

        // 진영별 제목 연출 (우리팀: 골드/보유 권능, 적팀: 적색/적의 권능)
        boolean isPlayerUnit = unit.team.equals(playerTeam);
        game.unitFont2.setColor(isPlayerUnit ? Color.GOLD : Color.FIREBRICK);
        game.unitFont2.draw(game.batch, isPlayerUnit ? "[ 보유 권능 ]" : "[ 적의 권능 ]", SKILL_X, SKILL_Y + 50);

        String reserved = unit.stat.getReservedSkill();
        String tooltipToDraw = null;

        for (int i = 0; i < visibleSkills.size; i++) {
            String skillName = visibleSkills.get(i);
            float y = SKILL_Y - (i * SKILL_H);
            Rectangle rect = new Rectangle(SKILL_X - 20, y - 35, 250, 45); // 클릭 및 마우스 오버 판정 영역

            game.batch.draw(timerBoxBg, rect.x, rect.y, rect.width, rect.height);

            // 스킬 상태별 텍스트 색상 분기
            if (!isPlayerUnit) {
                game.unitFont3.setColor(Color.WHITE); // 적군은 상태 표시 불필요
            } else {
                if (!unit.stat.isSkillReady(skillName)) {
                    game.unitFont3.setColor(Color.GRAY);   // 사용 완료된 스킬
                } else if (skillName.equals(reserved)) {
                    game.unitFont3.setColor(Color.YELLOW); // 현재 장전(선택)된 스킬
                } else {
                    game.unitFont3.setColor(Color.WHITE);  // 사용 가능한 스킬
                }
            }

            game.unitFont3.draw(game.batch, (i + 1) + ". " + skillName, SKILL_X + 10, y - 5);

            // 마우스 오버 시 툴팁 대상 지정
            if (rect.contains(mx, my)) {
                tooltipToDraw = skillName;
            }
        }

        if (tooltipToDraw != null) {
            renderSkillTooltip(tooltipToDraw, mx, my); // 툴팁은 가장 마지막에 그려서 겹침 방지
        }
        game.unitFont3.setColor(Color.WHITE);
    }

    private void renderSkillTooltip(String skillName, float mx, float my) {
        SkillData.Skill data = SkillData.get(skillName);
        if (data == null) return;

        // --- [미세조정: 툴팁 박스 설정] ---
        float tw = 400; // 박스 너비
        float th = 220; // 박스 높이
        float tx = mx - tw - 20; // 마우스 왼쪽 배치
        float ty = my - 100;     // 마우스 높이 기준 중앙 정렬

        if (tx < 10) tx = mx + 20; // 화면 왼쪽 경계 침범 시 오른쪽으로 반전

        game.batch.draw(logInfoBg, tx, ty, tw, th);

        // --- [미세조정: 툴팁 내부 텍스트 위치] ---
        float marginLeft = 60;   // 왼쪽 장식 여백
        float marginTop = 55;    // 상단 장식 여백
        float lineSpacing = 35;  // 줄 간격
        float contentWidth = tw - (marginLeft * 2);
        float currentY = ty + th - marginTop;

        // 1. 스킬 이름
        game.unitFont3.setColor(Color.CYAN);
        game.unitFont3.draw(game.batch, "[" + data.name + "]", tx + marginLeft, currentY);

        // 2. 수치 정보 (위력/사거리)
        currentY -= lineSpacing;
        game.unitFont3.setColor(Color.ORANGE);
        game.unitFont3.draw(game.batch, "위력: " + (int)(data.power * 100) + "% | 사거리: " + data.range, tx + marginLeft, currentY);

        // 3. 상세 설명 (자동 줄바꿈 적용)
        currentY -= lineSpacing;
        game.unitFont3.setColor(Color.WHITE);
        game.unitFont3.draw(game.batch, data.description, tx + marginLeft, currentY, contentWidth, Align.left, true);
    }

    // 클릭 좌표를 기반으로 어떤 스킬이 눌렸는지 확인 (BattleScreen에서 호출)
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

    // 좌측 하단 유닛 상태 카드 렌더링
    private void renderUnitDetails(Unit unit) {
        if (unit.portrait != null) game.batch.draw(unit.portrait, 10, 20, 300, 420);

        // 카드 텍스트 위치 (X: 55, 200 / Y: 105, 125, 145 부근)
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
        logInfoBg.dispose();
        stageInfoBg.dispose();
        timerBoxBg.dispose();
    }
}
