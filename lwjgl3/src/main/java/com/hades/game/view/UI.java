package com.hades.game.view;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.hades.game.HadesGame;

public class UI {

    // // [추가된 메서드] 특정 색상의 사각형 Drawable을 생성합니다. (팝업 배경 등에 사용)
    public static Drawable getColoredDrawable(float r, float g, float b, float a) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(r, g, b, a));
        pixmap.fill();

        // // 메모리 누수 방지를 위해 Pixmap은 Texture 생성 후 바로 dispose 합니다.
        Texture texture = new Texture(pixmap);
        TextureRegionDrawable drawable = new TextureRegionDrawable(texture);
        pixmap.dispose();

        return drawable;
    }

    // // addHoverEffect : 마우스 호버 시 색상 변화 메서드
    public static void addHoverEffect(final HadesGame game, final Actor actor, final Color normalColor, final Color hoverColor) {
        // // 아래의 확장된 메서드를 호출하여 코드 중복을 방지합니다.
        addHoverEffect(game, actor, actor, normalColor, hoverColor);
    }

    // // addHoverEffect 오버로딩 메서드 (마우스 호버 감지 대상과 바뀌는 색상의 대상이 다를 때)
    public static void addHoverEffect(final HadesGame game, final Actor eventActor, final Actor targetActor, final Color normalColor, final Color hoverColor) {

        // // 1. 마우스 오버 리스너: 감지는 eventActor가 하고, 색 변화는 targetActor가 수행
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

        // // 2. 클릭 사운드 리스너
        eventActor.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.playClick();
            }
        });
    }
}
