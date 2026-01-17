package com.hades.game.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array; // 추가
import com.badlogic.gdx.utils.Disposable;
import com.hades.game.constants.UnitData;

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

    // [추가] 데미지 텍스트 리스트
    public Array<DamageText> damageTexts = new Array<>();
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

        // 3. [추가] 데미지 텍스트 상태 업데이트
        for (int i = damageTexts.size - 1; i >= 0; i--) {
            DamageText dt = damageTexts.get(i);
            dt.update(delta);
            if (dt.timer <= 0) damageTexts.removeIndex(i);
        }
    }

    public void playAttackAnim(int targetX, int targetY) {
        attackAnimTimer = ATTACK_DURATION;
        float dx = targetX - gridX;
        float dy = targetY - gridY;
        attackDir.set(dx * 1.5f, -dy).nor();
    }

    public void playHitAnim() {
        hitTimer = 0.15f;
    }

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

    public int getPower(boolean isMyTurn) {
        return isMyTurn ? stat.atk() : stat.counterAtk();
    }

    public boolean canReach(Unit target) {
        if (target == null) return false;
        int dist = Math.abs(this.gridX - target.gridX) + Math.abs(this.gridY - target.gridY);
        return dist <= this.stat.range();
    }

    public boolean isAlive() {
        return status == ALIVE && currentHp > 0;
    }

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
