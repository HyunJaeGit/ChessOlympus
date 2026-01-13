
package com.hades.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.hades.game.screens.MenuScreen;
import com.hades.game.utils.FontFactory;


/*
 HadesGame 게임의 전체 생명주기를 관리하며, 각 화면(Screen) 사이를 연결하는 매니저 클래스
 ApplicationAdapter 대신 Game 클래스를 상속받아 여러 화면을 전환할 수 있는 기능을 가집니다.
 */
public class HadesGame extends Game {
    // 여러 화면에서 공용으로 사용할 SpriteBatch입니다.
    public SpriteBatch batch;
    // 두 종류의 공용 폰트 선언
    public BitmapFont mainFont;   // 갈무리 (제목, 현재 턴 등 메인 UI용)
    public BitmapFont detailFont; // 맑은 고딕 (유닛 정보, 설명 등 상세 내용용)

    @Override
    public void create() {
        batch = new SpriteBatch();

        /* [핵심] FontFactory를 두 번 호출하여 서로 다른 폰트를 생성합니다. */
        // 1. 갈무리 폰트 생성
        mainFont = com.hades.game.utils.FontFactory.createFont("Galmuri14", 20, Color.WHITE, 1.0f);

        // 2. 맑은 고딕 생성
        detailFont = com.hades.game.utils.FontFactory.createFont("malgun", 16, Color.WHITE, 0);

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
        // 현재 화면이 있다면 해당 화면의 자원도 해제합니다.
        if (getScreen() != null) getScreen().dispose();
    }
}

