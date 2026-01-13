package com.hades.game.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

/**
 * 폰트 생성 로직을 전담하는 유틸리티 클래스입니다.
 * 복잡한 유니코드 루프와 설정을 캡슐화하여 코드 가독성을 높입니다.
 */
public class FontFactory {

    /**
     * [메서드 설명] 한글 전체 범위를 포함한 BitmapFont를 생성합니다.
     * @param size 폰트 크기
     * @param color 폰트 색상
     * @return 설정이 완료된 BitmapFont 객체
     */
    public static BitmapFont createKoreanFont(int size, Color color) {
        String fontPath = "fonts/malgun.ttf";

        if (!Gdx.files.internal(fontPath).exists()) {
            System.err.println("[경고] 폰트 파일을 찾을 수 없어 기본 폰트를 반환합니다.");
            return new BitmapFont();
        }

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal(fontPath));
        FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();

        param.size = size;
        param.color = color;
        param.incremental = true; // 메모리 최적화 및 렉 방지

        // 한글 유니코드 전체 범위 설정
        StringBuilder sb = new StringBuilder();
        sb.append(FreeTypeFontGenerator.DEFAULT_CHARS);
        for (char c = 0xAC00; c <= 0xD7A3; c++) {
            sb.append(c);
        }
        param.characters = sb.toString();

        BitmapFont font = generator.generateFont(param);
        generator.dispose();

        return font;
    }
}
