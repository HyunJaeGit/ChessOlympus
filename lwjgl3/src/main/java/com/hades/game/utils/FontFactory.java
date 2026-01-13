package com.hades.game.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

/**
 * [클래스 역할] 맑은 고딕, 갈무리 등 다양한 외부 폰트 생성을 전담하며 한글 출력을 지원합니다.
 */
public class FontFactory {

    /**
     * [메서드 설명] 지정된 경로의 폰트 파일을 읽어 한글 지원 BitmapFont를 생성합니다.
     * @param fontName "malgun" 또는 "Galmuri14" 등 폰트 파일명
     * @param size 폰트 크기
     * @param color 폰트 색상
     * @param border 테두리 두께 (0이면 테두리 없음)
     * @return 설정이 완료된 BitmapFont 객체
     */
    /* [클래스 역할] 테두리와 그림자 효과가 적용된 한글 BitmapFont를 생성하는 유틸리티 클래스입니다. */
    public static BitmapFont createFont(String fontName, int size, Color color, float border) {
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

        /* [설명] 테두리(Border) 설정: 글자의 외곽선을 그려 배경과 분리합니다. */
        if (border > 0) {
            param.borderWidth = border;
            param.borderColor = Color.BLACK;
        }

        /* 그림자(Shadow) 설정: 글자 뒤에 어두운 그림자를 깔아 입체감을 줍니다. */
        // 테두리가 있을 때 그림자까지 있으면 가독성이 비약적으로 상승합니다.
        if (size > 20) { // 타이틀처럼 큰 글씨에는 더 짙은 그림자 적용
            param.shadowOffsetX = 3;
            param.shadowOffsetY = 3;
            param.shadowColor = new Color(0, 0, 0, 0.8f); // 80% 투명한 검정
        } else { // 작은 글씨에는 얇은 그림자 적용
            param.shadowOffsetX = 1;
            param.shadowOffsetY = 1;
            param.shadowColor = new Color(0, 0, 0, 0.6f);
        }

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
