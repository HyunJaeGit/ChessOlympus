package com.hades.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.hades.game.HadesGame;
import com.hades.game.constants.GameConfig;
import java.util.Map;

// Chess Olympus: HADES vs ZEUS - 컴팩트 결과 화면 클래스
// 데이터 로드 시의 정합성을 보장하며, 화면에 모든 요소가 알맞게 들어가도록 구성되었습니다.
public class ScoreScreen extends ScreenAdapter {
    private final HadesGame game;
    private Stage stage;
    private boolean isExiting = false;

    // 폰트 크기를 줄인 스타일 객체들을 캐싱하여 메모리 안정성과 시각적 편안함을 제공합니다.
    private Label.LabelStyle titleStyle;
    private Label.LabelStyle totalStyle;
    private Label.LabelStyle categoryStyle;
    private Label.LabelStyle contentStyle;
    private Label.LabelStyle exitStyle;

    // 클래스 생성자: 스타일 초기화 및 UI 구성을 시작합니다.
    public ScoreScreen(HadesGame game) {
        this.game = game;
        this.stage = new Stage(new FitViewport(GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT));

        initStyles(); // 스타일 초기화
        game.runState.ensureRankInitialized(); // 명예의 전당 데이터 검증
        initUI(); // 레이아웃 배치
        setupGlobalTouch(); // 터치 이벤트 설정
    }

    // 스타일 초기화 메서드: 폰트 크기를 낮추어 화면이 잘리지 않게 조정합니다.
    private void initStyles() {
        titleStyle = new Label.LabelStyle(game.subtitleFont, Color.GOLD);
        totalStyle = new Label.LabelStyle(game.mainFont, Color.CYAN);
        categoryStyle = new Label.LabelStyle(game.unitFont2, Color.GOLD);
        contentStyle = new Label.LabelStyle(game.unitFont3, Color.WHITE);
        exitStyle = new Label.LabelStyle(game.cardFont, Color.GRAY);
    }

    // 메인 UI 배치 메서드: RunState의 안전한 합산 메서드를 사용하여 총점을 표시합니다.
    private void initUI() {
        Table root = new Table();
        root.setFillParent(true);
        root.center(); // 요소를 중앙에 밀집시킴
        stage.addActor(root);

        // [중요] 세이브 데이터 타입 오류를 방지하기 위해 RunState의 안전한 합산 메서드 호출
        float totalTime = game.runState.getTotalClearTime();

        // 헤더 영역 배치
        root.add(new Label("체스올림푸스 기록", titleStyle)).padBottom(5).row();

        Label totalLabel = new Label("TOTAL CLEAR TIME " + formatTime(totalTime), totalStyle);
        root.add(totalLabel).padBottom(30).row();

        // 컨텐츠 영역 (좌우 분할) 배치
        Table contentTable = new Table();
        contentTable.add(createHallOfFameTable()).top().padRight(50); // 등록 기능 없이 단순 출력
        contentTable.add(createScoreDetailTable(game.runState.stageBestTimes)).top();

        root.add(contentTable).padBottom(40).row();

        // 하단 안내 문구 배치
        Label touchPrompt = new Label("TOUCH TO HOME", exitStyle);
        touchPrompt.addAction(Actions.forever(Actions.sequence(Actions.alpha(0.5f, 1f), Actions.alpha(1f, 1f))));
        root.add(touchPrompt);
    }

    // 좌측 명예의 전당 테이블 생성 메서드: RunState에 고정된 공식 기록만 출력합니다.
    private Table createHallOfFameTable() {
        Table hof = new Table();
        hof.add(new Label("HALL OF FAME", categoryStyle)).padBottom(15).row();

        // 개발자가 RunState.java에 정의한 TOP 3 기록을 순회하며 출력합니다.
        for (int i = 0; i < 3; i++) {
            float bTime = game.runState.topBestTimes[i];
            String bName = game.runState.topHeroNames[i];
            String nameStr = (bName == null || bName.isEmpty() || bName.equals("---")) ? "EMPTY" : bName;

            Color rowColor = (i == 0) ? Color.CYAN : (i == 1 ? Color.GOLD : Color.LIGHT_GRAY);
            Label rankLbl = new Label((i + 1) + "ST " + formatTime(bTime) + " " + nameStr,
                new Label.LabelStyle(game.unitFont3, rowColor));

            hof.add(rankLbl).left().padBottom(10).row();
        }
        return hof;
    }

    // 우측 상세 스코어 테이블 생성 메서드: 스테이지별 기록을 컴팩트하게 표시합니다.
    private Table createScoreDetailTable(Map<Integer, Float> bestTimes) {
        Table detail = new Table();
        detail.add(new Label("STAGE RECORD", categoryStyle)).colspan(3).padBottom(15).row();

        for (int i = 1; i <= 7; i++) {
            Object rawValue = bestTimes.get(i);
            float time = (rawValue instanceof Number) ? ((Number) rawValue).floatValue() : 0f;
            String rank = getRank(time);

            detail.add(new Label("ST." + i, contentStyle)).left().padRight(15);
            detail.add(new Label(formatTime(time), new Label.LabelStyle(game.unitFont3, Color.LIGHT_GRAY))).padRight(15);

            Label rankLabel = new Label(rank, new Label.LabelStyle(game.unitFont3, getRankColor(rank)));
            detail.add(rankLabel).row();
        }
        return detail;
    }

    // 터치 이벤트 설정 메서드: 화면 클릭 시 메인 메뉴로 안전하게 전환합니다.
    private void setupGlobalTouch() {
        stage.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (isExiting) return;
                isExiting = true;
                game.playClick();
                stage.addAction(Actions.sequence(
                    Actions.fadeOut(0.4f),
                    Actions.run(() -> game.setScreen(new MenuScreen(game)))
                ));
            }
        });
    }

    // 시간 포맷 변환 메서드: 초 단위를 분:초 형태의 문자열로 변환합니다.
    private String formatTime(float sec) {
        if (sec <= 0) return "00:00";
        return String.format("%02d:%02d", (int)(sec / 60), (int)(sec % 60));
    }

    // 등급 판정 메서드: 클리어 타임에 따라 랭크 등급을 결정합니다.
    private String getRank(float sec) {
        if (sec <= 0) return "F";
        if (sec < 90) return "S";
        if (sec < 180) return "A";
        return "B";
    }

    // 등급 색상 반환 메서드: 각 등급에 어울리는 색상을 반환합니다.
    private Color getRankColor(String rank) {
        if ("S".equals(rank)) return Color.SKY;
        if ("A".equals(rank)) return Color.GOLD;
        return Color.WHITE;
    }

    // 화면 표시 시 입력 프로세서를 설정합니다.
    @Override public void show() { Gdx.input.setInputProcessor(stage); }

    // 프레임 렌더링 메서드: 배경색을 칠하고 스테이지를 그립니다.
    @Override public void render(float d) {
        Gdx.gl.glClearColor(0.01f, 0.01f, 0.02f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(d); stage.draw();
    }

    // 화면 크기 조절 시 뷰포트를 업데이트합니다.
    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }

    // 화면 해제 시 자원을 정리합니다.
    @Override public void dispose() { stage.dispose(); }
}
