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

// Chess Olympus: HADES vs ZEUS - 최종 성적표 및 명예의 전당 화면 클래스
// 게임 클리어 후 전체 기록을 보여주고 역대 최고 기록 유저를 기리는 역할을 합니다.
public class ScoreScreen extends ScreenAdapter {
    private final HadesGame game;
    private Stage stage;
    private boolean isExiting = false; // 중복 클릭으로 인한 화면 전환 오류 방지 플래그

    // 폰트 렌더링 에러를 방지하기 위해 스타일 객체를 미리 생성하여 보관합니다.
    private Label.LabelStyle subtitleStyle;
    private Label.LabelStyle mainStyle;
    private Label.LabelStyle mainGoldStyle;
    private Label.LabelStyle mainCyanStyle;
    private Label.LabelStyle detailStyle;
    private Label.LabelStyle unitStyle;

    // 클래스 생성자: 화면 전환 시 가장 먼저 실행됩니다.
    public ScoreScreen(HadesGame game) {
        this.game = game;
        // 가상 해상도 설정에 맞춰 스테이지를 초기화합니다.
        this.stage = new Stage(new FitViewport(GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT));

        initStyles();           // 1. 사용할 텍스트 스타일 정의
        game.runState.ensureRankInitialized(); // 2. 명예의 전당 데이터 검증 및 초기화
        initUI();               // 3. 화면 레이아웃 구성
        setupGlobalTouch();     // 4. 터치 이벤트 설정
    }

    // 텍스트 스타일 초기화 메서드: 폰트와 색상을 조합하여 스타일 객체를 만듭니다.
    private void initStyles() {
        subtitleStyle = new Label.LabelStyle(game.subtitleFont, Color.GOLD);
        mainStyle = new Label.LabelStyle(game.mainFont, Color.WHITE);
        mainGoldStyle = new Label.LabelStyle(game.mainFont, Color.GOLD);
        mainCyanStyle = new Label.LabelStyle(game.mainFont, Color.CYAN);
        detailStyle = new Label.LabelStyle(game.detailFont, Color.WHITE);
        unitStyle = new Label.LabelStyle(game.unitFont3, Color.GRAY);
    }

    // 메인 UI 배치 메서드: 화면의 전체적인 레이아웃(상단, 중앙, 하단)을 잡습니다.
    private void initUI() {
        Table root = new Table(); // 전체 화면을 덮는 루트 테이블
        root.setFillParent(true);
        root.top().pad(40); // 상단 여백을 주어 타이틀 배치 시작
        stage.addActor(root);

        // 이번 게임의 총 클리어 시간 합계를 계산합니다.
        float totalTime = 0;
        Map<Integer, Float> bestTimes = game.runState.stageBestTimes;
        for (float t : bestTimes.values()) totalTime += t;

        // [상단] 화면 제목 배치
        Label title = new Label("BATTLE REPORT", subtitleStyle);
        root.add(title).colspan(2).padBottom(10).row();

        // [상단] 총 클리어 타임 표시 및 애니메이션 적용
        Label totalLabel = new Label("TOTAL CLEAR TIME " + formatTime(totalTime), mainCyanStyle);
        totalLabel.addAction(Actions.forever(Actions.sequence(Actions.alpha(0.6f, 0.7f), Actions.alpha(1.0f, 0.7f))));
        root.add(totalLabel).colspan(2).padBottom(50).row();

        // [중앙] 가로 분할 테이블 (좌측 명예의 전당 / 우측 상세 스코어)
        Table contentTable = new Table();
        contentTable.add(createHallOfFameTable(totalTime)).top().padRight(80);
        contentTable.add(createScoreDetailTable(bestTimes)).top();
        root.add(contentTable).row();

        // [하단] 홈으로 돌아가기 안내 문구
        Label touchPrompt = new Label("ANYWHERE TOUCH TO HOME", unitStyle);
        root.add(touchPrompt).colspan(2).padTop(60);
    }

    // 좌측 명예의 전당 테이블 생성 메서드: TOP 3 기록 유저를 표시합니다.
    private Table createHallOfFameTable(float currentTotal) {
        Table hof = new Table();
        // 이번 회차 기록이 TOP 3 안에 드는지 확인하고 업데이트합니다.
        if (currentTotal > 0) updateTopRankings(currentTotal, game.runState.selectedHeroName);

        Label hofTitle = new Label("HALL OF FAME", mainGoldStyle);
        hof.add(hofTitle).padBottom(25).row();

        // 1위부터 3위까지 데이터를 한 줄씩 생성합니다.
        for (int i = 0; i < 3; i++) {
            float bTime = game.runState.topBestTimes[i];
            String bName = game.runState.topHeroNames[i];
            String timeStr = formatTime(bTime);
            String nameStr = (bName == null || bName.isEmpty() || bName.equals("---")) ? "EMPTY" : bName;

            // 순위별로 색상을 다르게 적용 (1위: CYAN, 2위: GOLD, 3위: SILVER 계열)
            Color rowColor = (i == 0) ? Color.CYAN : (i == 1 ? Color.GOLD : Color.LIGHT_GRAY);
            Label rankLbl = new Label((i + 1) + "ST " + timeStr + " " + nameStr,
                new Label.LabelStyle(game.detailFont, rowColor));

            hof.add(rankLbl).left().padBottom(15).row();
        }
        return hof;
    }

    // 우측 상세 스코어 테이블 생성 메서드: 1~7단계별 클리어 기록을 표시합니다.
    private Table createScoreDetailTable(Map<Integer, Float> bestTimes) {
        Table detail = new Table();
        detail.defaults().pad(5, 10, 5, 10);

        Label detailTitle = new Label("STAGE DETAILS", mainGoldStyle);
        detail.add(detailTitle).colspan(3).padBottom(20).row();

        // 각 스테이지(1~7)의 기록과 등급(Rank)을 행 단위로 배치합니다.
        for (int i = 1; i <= 7; i++) {
            float time = bestTimes.getOrDefault(i, 0f);
            String rank = getRank(time);

            detail.add(new Label("ST." + i, mainStyle)).left();
            detail.add(new Label(formatTime(time), new Label.LabelStyle(game.mainFont, Color.LIGHT_GRAY)));

            Label rankLabel = new Label(rank, new Label.LabelStyle(game.mainFont, getRankColor(rank)));
            detail.add(rankLabel).width(40).center().row();
        }
        return detail;
    }

    // 명예의 전당 데이터 갱신 메서드: 삽입 정렬 알고리즘을 사용하여 순위를 조정합니다.
    private void updateTopRankings(float newTime, String newName) {
        float[] times = game.runState.topBestTimes;
        String[] names = game.runState.topHeroNames;
        for (int i = 0; i < 3; i++) {
            // 기록이 없거나(0) 현재 기록이 기존 기록보다 빠를 경우
            if (times[i] <= 0 || newTime < times[i]) {
                // 하위 순위 기록들을 한 칸씩 뒤로 밀어냅니다.
                for (int j = 2; j > i; j--) {
                    times[j] = times[j - 1];
                    names[j] = names[j - 1];
                }
                // 새로운 기록을 해당 순위에 삽입합니다.
                times[i] = newTime;
                names[i] = newName;
                game.saveGame(); // 로컬 파일로 즉시 저장
                break;
            }
        }
    }

    // 전역 터치 이벤트 설정 메서드: 화면 어디든 누르면 메인 메뉴로 돌아갑니다.
    private void setupGlobalTouch() {
        stage.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (isExiting) return;
                isExiting = true;
                game.playClick();
                // 서서히 사라지는 효과와 함께 화면을 전환합니다.
                stage.addAction(Actions.sequence(
                    Actions.fadeOut(0.5f),
                    Actions.run(() -> game.setScreen(new MenuScreen(game)))
                ));
            }
        });
    }

    // 시간 변환 메서드: 초(second)를 MM:SS 포맷의 문자열로 바꿉니다.
    private String formatTime(float sec) {
        if (sec <= 0) return "00:00";
        return String.format("%02d:%02d", (int)(sec / 60), (int)(sec % 60));
    }

    // 등급 판정 메서드: 클리어 타임에 따라 S, A, B, F 등급을 반환합니다.
    private String getRank(float sec) {
        if (sec <= 0) return "F";
        if (sec < 90) return "S";
        if (sec < 180) return "A";
        return "B";
    }

    // 등급 색상 반환 메서드: 등급에 맞는 시각적 색상을 정의합니다.
    private Color getRankColor(String rank) {
        if ("S".equals(rank)) return Color.SKY;
        if ("A".equals(rank)) return Color.GOLD;
        if ("F".equals(rank)) return Color.GRAY;
        return Color.WHITE;
    }

    // 화면이 활성화될 때 입력 처리를 스테이지로 넘깁니다.
    @Override public void show() { Gdx.input.setInputProcessor(stage); }

    // 매 프레임마다 화면을 그리는 메서드입니다.
    @Override public void render(float d) {
        Gdx.gl.glClearColor(0.01f, 0.01f, 0.02f, 1); // 배경을 아주 어두운 남색으로 칠함
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(d); // 애니메이션 업데이트
        stage.draw(); // UI 그리기
    }

    // 창 크기가 변경될 때 뷰포트를 업데이트합니다.
    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }

    // 화면 종료 시 메모리 자원을 해제합니다.
    @Override public void dispose() { stage.dispose(); }
}
