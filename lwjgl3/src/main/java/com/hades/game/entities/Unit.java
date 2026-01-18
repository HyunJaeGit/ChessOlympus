package com.hades.game.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils; // 추가
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.hades.game.constants.UnitData;

// Chess Olympus: HADES vs ZEUS
// 유닛의 데이터와 시각적 애니메이션 상태를 관리하는 클래스입니다.
public class Unit implements Disposable {
    public enum UnitClass {
        HERO, SHIELD, KNIGHT, ARCHER, CHARIOT, SAINT
    }

    // [데미지 팝업을 위한 내부 보조 클래스]
    public static class DamageText {
        public String text;
        public Vector2 offsetPos; // 유닛 위치 기준 상대 좌표
        public float timer;       // 남은 시간
        public float alpha = 1f;  // 투명도
        public Color color;

        public DamageText(int amount, Color color) {
            this.text = String.valueOf(amount);
            this.offsetPos = new Vector2(0, 70); // 머리 위 적당한 높이에서 시작
            this.timer = 0.8f;                   // 0.8초 동안 표시
            this.color = color.cpy();
        }

        public void update(float delta) {
            timer -= delta;
            offsetPos.y += 40 * delta;       // 매 프레임 위로 떠오름
            alpha = Math.max(0, timer / 0.8f); // 시간에 따라 서서히 사라짐
        }
    }

    public static final int ALIVE = 1;
    public static final int DEAD = 0;

    public final String name;
    public final String team;
    public final UnitData.Stat stat;
    public final UnitClass unitClass;
    public final Texture portrait;
    public final Texture fieldTexture;

    public int currentHp;
    public int gridX;
    public int gridY;
    public int status = ALIVE;

    // --- 애니메이션 변수 ---
    public Vector2 animOffset = new Vector2(0, 0);
    public float hitTimer = 0;
    private float attackAnimTimer = 0;
    private Vector2 attackDir = new Vector2(0, 0);
    private final float ATTACK_DURATION = 0.2f;

    // [추가] 하이라이트용 시각적 투명도 변수
    public float visualAlpha = 1.0f; // 실제 렌더링에 반영될 현재 투명도
    public float targetAlpha = 1.0f; // 목표로 하는 투명도 (선택 유무에 따라 0.4f 또는 1.0f)

    // [추가] 데미지 텍스트 리스트
    public Array<DamageText> damageTexts = new Array<>();

    // // [추가] 말풍선(Speech Bubble) 관련 변수
    public String speechText = null;
    public float speechTimer = 0;
    private final float SPEECH_DURATION = 1.5f; // 말풍선 표시 시간
    // ----------------------

    public Unit(String name, String team, UnitData.Stat stat, String imageKey, UnitClass unitClass, int x, int y) {
        this.name = name;
        this.team = team;
        this.stat = stat;
        this.unitClass = unitClass;
        this.currentHp = stat.hp();
        this.gridX = x;
        this.gridY = y;

        String portraitPath = "images/character/" + imageKey + ".png";
        this.portrait = loadSafeTexture(portraitPath);

        String fieldFileName = (unitClass == UnitClass.HERO) ? imageKey : (team.equalsIgnoreCase("HADES") ? "하데스" : "제우스") + imageKey;
        String fieldPath = "images/units/" + fieldFileName + ".png";
        this.fieldTexture = loadSafeTexture(fieldPath);
    }

    // 데미지를 입었을 때 호출 (전투 로직에서 호출)
    public void takeDamage(int amount, Color color) {
        this.currentHp -= amount;
        if (this.currentHp < 0) this.currentHp = 0;

        // 팝업 생성
        damageTexts.add(new DamageText(amount, color));
        // 피격 빨간색 깜빡임 실행
        playHitAnim();
    }

    // // 유닛이 말풍선 대사를 하도록 설정하는 메서드
    public void say(String text) {
        this.speechText = text;
        this.speechTimer = SPEECH_DURATION;
    }

    // 매 프레임 유닛의 상태와 애니메이션을 업데이트합니다.
    public void update(float delta) {
        // 1. 피격 깜빡임 타이머
        if (hitTimer > 0) hitTimer -= delta;

        // 2. 공격 도약 애니메이션
        if (attackAnimTimer > 0) {
            attackAnimTimer -= delta;
            float progress = (ATTACK_DURATION - attackAnimTimer) / ATTACK_DURATION;
            float curve = (float) Math.sin(progress * Math.PI);
            float jumpDist = 8f;
            animOffset.set(attackDir.x * jumpDist * curve, attackDir.y * jumpDist * curve);
        } else {
            animOffset.set(0, 0);
        }

        // // [추가] 말풍선 타이머 업데이트
        if (speechTimer > 0) {
            speechTimer -= delta;
            if (speechTimer <= 0) speechText = null;
        }

        // 3. 데미지 텍스트 업데이트 (인덱스 기반 루프로 에러 방지)
        for (int i = 0; i < damageTexts.size; i++) {
            DamageText dt = damageTexts.get(i);
            dt.update(delta);
            if (dt.timer <= 0) {
                damageTexts.removeIndex(i);
                i--; // 인덱스 조정
            }
        }

        // 4. [추가] 하이라이트 투명도 보간 (Lerp)
        // visualAlpha가 targetAlpha를 향해 매 프레임 부드럽게 변하도록 합니다.
        visualAlpha = MathUtils.lerp(visualAlpha, targetAlpha, 0.15f);
    }

    // 공격 시 타겟 방향으로 살짝 점프하는 연출을 시작합니다.
    public void playAttackAnim(int targetX, int targetY) {
        attackAnimTimer = ATTACK_DURATION;
        float dx = targetX - gridX;
        float dy = targetY - gridY;
        attackDir.set(dx * 1.5f, -dy).nor();
    }

    // 피격 시 짧은 시간 동안 유닛을 빨간색으로 표시하도록 설정합니다.
    public void playHitAnim() {
        hitTimer = 0.15f;
    }

    // 리소스를 안전하게 불러오며 파일이 없을 경우 기본 이미지를 반환합니다.
    private Texture loadSafeTexture(String path) {
        try {
            if (Gdx.files.internal(path).exists()) {
                Texture tex = new Texture(Gdx.files.internal(path));
                tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                return tex;
            } else {
                return new Texture(Gdx.files.internal("libgdx.png"));
            }
        } catch (Exception e) {
            return new Texture(Gdx.files.internal("libgdx.png"));
        }
    }

    // 공격자의 턴 여부에 따라 공격력 혹은 반격력을 반환합니다.
    public int getPower(boolean isMyTurn) {
        return isMyTurn ? stat.atk() : stat.counterAtk();
    }

    // 대상 유닛이 자신의 사거리 안에 있는지 체크합니다.
    public boolean canReach(Unit target) {
        if (target == null) return false;
        int dist = Math.abs(this.gridX - target.gridX) + Math.abs(this.gridY - target.gridY);
        return dist <= this.stat.range();
    }

    // 유닛이 현재 전장에서 살아있는 상태인지 확인합니다.
    public boolean isAlive() {
        return status == ALIVE && currentHp > 0;
    }

    // 유닛의 그리드 좌표를 설정합니다.
    public void setPosition(int x, int y) {
        this.gridX = x;
        this.gridY = y;
    }

    @Override
    public void dispose() {
        if (portrait != null) portrait.dispose();
        if (fieldTexture != null && fieldTexture != portrait) fieldTexture.dispose();
    }
}
