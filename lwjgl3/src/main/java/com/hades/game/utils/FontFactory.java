package com.hades.game.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

// 클래스 역할: 테두리와 그림자 효과가 적용된 한글 BitmapFont를 생성하는 유틸리티 클래스입니다.
public class FontFactory {

    // 메서드 설명: 4개의 인자만 들어올 경우 실행되며, 기본 테두리(검정)와 기본 그림자를 적용합니다.
    public static BitmapFont createFont(String fontName, int size, Color color, float border) {
        // 내부적으로 아래의 6개 인자 메서드를 호출하여 코드 중복을 방지합니다.
        return createFont(fontName, size, color, border, Color.BLACK, new Color(0, 0, 0, 0.6f));
    }

    // 메서드 설명: 6개의 인자를 받아 테두리 색상과 그림자 색상을 정밀하게 설정하여 폰트를 생성합니다.
    public static BitmapFont createFont(String fontName, int size, Color color, float border, Color borderColor, Color shadowColor) {
        String fontPath = "fonts/" + fontName + ".ttf";

        if (!Gdx.files.internal(fontPath).exists()) {
            System.err.println("[경고] " + fontPath + " 파일을 찾을 수 없습니다. 기본 폰트를 사용합니다.");
            return new BitmapFont();
        }

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal(fontPath));
        FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();

        param.size = size;
        param.color = color;
        param.incremental = true; // 필요한 글자만 동적으로 생성 (메모리 절약)

        // 텍스트 필터 설정: 글자가 깨지는 것을 방지하고 부드럽게 표현합니다.
        param.minFilter = Texture.TextureFilter.Linear;
        param.magFilter = Texture.TextureFilter.Linear;

        // 테두리 설정: 전달받은 borderColor가 있으면 적용하고, 없으면 검은색을 기본값으로 사용합니다.
        if (border > 0) {
            param.borderWidth = border;
            param.borderColor = (borderColor != null) ? borderColor : Color.BLACK;
        }

        // 그림자 설정: 전달받은 shadowColor가 투명이 아닐 때만 적용합니다.
        if (shadowColor != null && !shadowColor.equals(Color.CLEAR)) {
            param.shadowColor = shadowColor;
            // 가독성을 위해 크기에 따라 그림자 거리(Offset) 자동 조절
            int offset = (size > 30) ? 3 : 1;
            param.shadowOffsetX = offset;
            param.shadowOffsetY = offset;
        }

        // 한글 유니코드 전체 범위 설정
        StringBuilder sb = new StringBuilder();
        sb.append(FreeTypeFontGenerator.DEFAULT_CHARS);
        for (char c = 0xAC00; c <= 0xD7A3; c++) {
            sb.append(c);
        }
        param.characters = sb.toString();

        BitmapFont font = generator.generateFont(param);
        generator.dispose(); // 리소스 해제

        return font;
    }
}
