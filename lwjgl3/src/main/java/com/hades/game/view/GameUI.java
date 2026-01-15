package com.hades.game.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.hades.game.HadesGame;
import com.hades.game.constants.GameConfig;
import com.hades.game.entities.Unit;

// 클래스 역할: 전투 화면의 정보창, 로그창, 유닛 상세 정보 등 모든 UI 렌더링을 전담합니다.
public class GameUI implements Disposable {
    private final HadesGame game;
    private Texture logInfoBg;
    private Texture stageInfoBg;
    private Texture unitInfoBg;
    private Texture timerBoxBg;

    public GameUI(HadesGame game) {
        this.game = game;
        loadResources();
    }

    private void loadResources() {
        String path = "images/background/";
        logInfoBg = new Texture(Gdx.files.internal(path + "log_info.png"));
        stageInfoBg = new Texture(Gdx.files.internal(path + "stage_info.png"));
        unitInfoBg = new Texture(Gdx.files.internal(path + "unit_info.png"));
        timerBoxBg = new Texture(Gdx.files.internal(path + "timer_box.png"));
    }

    // BattleScreen의 drawUIElements 내용을 이쪽으로 옮겼습니다.
    public void render(int stageLevel, String currentTurn, String playerTeam, Rectangle menuHitbox, Unit selectedUnit) {
        // 1. 스테이지 정보 표시
        game.batch.draw(stageInfoBg, 20, GameConfig.VIRTUAL_HEIGHT - 100, 240, 80);
        game.unitFont2.setColor(Color.WHITE);
        game.unitFont2.draw(game.batch, "STAGE " + stageLevel, 80, GameConfig.VIRTUAL_HEIGHT - 45);

        // 2. 턴 정보 표시
        game.unitFont2.setColor(currentTurn.equals(playerTeam) ? Color.LIME : Color.RED);
        game.unitFont2.draw(game.batch, currentTurn.equals(playerTeam) ? "YOUR TURN" : "ENEMY TURN", 40, GameConfig.VIRTUAL_HEIGHT - 110);

        // 3. 메뉴/타이머 박스 및 화면 모드 표시
        game.batch.draw(timerBoxBg, menuHitbox.x, menuHitbox.y, menuHitbox.width, menuHitbox.height);
        String screenModeText = Gdx.graphics.isFullscreen() ? "window" : "fullscreen";
        game.unitFont3.setColor(Color.valueOf("4FB9AF"));
        game.unitFont3.draw(game.batch, screenModeText, menuHitbox.x, menuHitbox.y + 42, menuHitbox.width, Align.center, false);

        // 4. 전투 로그창
        game.batch.draw(logInfoBg, 240, 20, GameConfig.VIRTUAL_WIDTH - 260, 200);

        // 5. 선택된 유닛 정보 상세창 (나중에 텍스트 추가 예정)
        if (selectedUnit != null) {
            game.batch.draw(unitInfoBg, 10, 20, 300, 400);
            renderUnitDetails(selectedUnit);
        }
    }

    // 오늘 업데이트 예정인 유닛 상세 정보 텍스트 렌더링 (미리 뼈대 작성)
    private void renderUnitDetails(Unit unit) {
        // TODO: unit.stat 등을 이용해 이름, HP, ATK 등을 unitInfoBg 위에 그리기
    }

    @Override
    public void dispose() {
        logInfoBg.dispose();
        stageInfoBg.dispose();
        unitInfoBg.dispose();
        timerBoxBg.dispose();
    }
}
