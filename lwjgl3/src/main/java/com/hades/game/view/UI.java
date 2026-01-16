package com.hades.game.view;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.hades.game.HadesGame;

public class UI {

    // addHoverEffect : 마우스 호버시 색상 변화 메서드
    public static void addHoverEffect(final HadesGame game, final Actor actor, final Color normalColor, final Color hoverColor) {
        // 아래의 확장된 메서드를 호출하여 코드 중복을 방지합니다.
        addHoverEffect(game, actor, actor, normalColor, hoverColor);
    }

    // addHoverEffect 오버로딩 메서드 (마우스 호버 감지 대상과 바뀌는 색상의 대상이 다를때)
    public static void addHoverEffect(final HadesGame game, final Actor eventActor, final Actor targetActor, final Color normalColor, final Color hoverColor) {

        // 1. 마우스 오버 리스너: 감지는 eventActor가 하고, 색 변화는 targetActor가 수행
        eventActor.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor from) {
                targetActor.setColor(hoverColor);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor to) {
                targetActor.setColor(normalColor);
            }
        });

        // 2. 클릭 사운드 리스너
        eventActor.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.playClick();
            }
        });
    }
}
