
package com.hades.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.hades.game.screens.MenuScreen;


/*
 HadesGame 게임의 전체 생명주기를 관리하며, 각 화면(Screen) 사이를 연결하는 매니저 클래스
 ApplicationAdapter 대신 Game 클래스를 상속받아 여러 화면을 전환할 수 있는 기능을 가집니다.
 */
public class HadesGame extends Game {
    // 여러 화면에서 공용으로 사용할 SpriteBatch입니다.
    public SpriteBatch batch;
    public BitmapFont font; // 모든 화면에서 공유할 공용 폰트

    /* [메서드 설명] 게임이 실행될 때 최초 1회 호출됩니다. 초기 자원을 설정하고 첫 화면을 띄웁니다. */
    @Override
    public void create() {
        batch = new SpriteBatch();

        /* 근본적 해결: 복잡한 로직 대신 팩토리를 통해 폰트 획득 (단 1줄) */
        font = com.hades.game.utils.FontFactory.createKoreanFont(20, Color.WHITE);
        // 게임 실행 시 가장 먼저 홈 화면(MenuScreen)을 표시하도록 설정합니다.
        this.setScreen(new MenuScreen(this));
    }

    /* [메서드 설명] 매 프레임마다 호출되며, 현재 설정된 Screen의 render 메서드를 실행합니다. */
    @Override
    public void render() {
        super.render(); // 현재 설정된 Screen(MenuScreen 또는 BattleScreen)의 렌더링을 호출
    }

    /* [메서드 설명] 게임 종료 시 사용 중인 모든 자원을 메모리에서 해제합니다. */
    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (font != null) font.dispose(); // 공용 폰트 자원 해제
        // 현재 화면이 있다면 해당 화면의 자원도 해제합니다.
        if (getScreen() != null) getScreen().dispose();
    }
}
