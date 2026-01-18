package com.hades.game.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
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

    public static class DamageText {
        public String text;
        public Vector2 offsetPos;
        public float timer;
        public float alpha = 1f;
        public Color color;

        public DamageText(int amount, Color color) {
            this.text = String.valueOf(amount);
            this.offsetPos = new Vector2(0, 70);
            this.timer = 0.8f;
            this.color = color.cpy();
        }

        public void update(float delta) {
            timer -= delta;
            offsetPos.y += 40 * delta;
            alpha = Math.max(0, timer / 0.8f);
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

    public Vector2 animOffset = new Vector2(0, 0);
    public float hitTimer = 0;
    private float attackAnimTimer = 0;
    private Vector2 attackDir = new Vector2(0, 0);
    private final float ATTACK_DURATION = 0.2f;

    public float visualAlpha = 1.0f;
    public float targetAlpha = 1.0f;

    public Array<DamageText> damageTexts = new Array<>();

    public String speechText = null;
    public float speechTimer = 0;
    private final float SPEECH_DURATION = 1.5f;

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

    // [추가] 이 유닛이 영웅 클래스인지 확인하는 헬퍼 메서드
    public boolean isHero() {
        return this.unitClass == UnitClass.HERO;
    }

    // [추가] 특정 리스트 내에서 아군 혹은 적군 영웅이 죽었는지 확인하는 정적 메서드
    public boolean isHeroDead(Array<Unit> units) {
        if (this.isHero() && this.currentHp <= 0) return true;
        return false;
    }

    public void takeDamage(int amount, Color color) {
        this.currentHp -= amount;
        if (this.currentHp < 0) {
            this.currentHp = 0;
            this.status = DEAD; // [강화] 체력이 0이 되면 즉시 상태 변경
        }
        damageTexts.add(new DamageText(amount, color));
        playHitAnim();
    }

    public void say(String text) {
        this.speechText = text;
        this.speechTimer = SPEECH_DURATION;
    }

    public void update(float delta) {
        if (hitTimer > 0) hitTimer -= delta;

        if (attackAnimTimer > 0) {
            attackAnimTimer -= delta;
            float progress = (ATTACK_DURATION - attackAnimTimer) / ATTACK_DURATION;
            float curve = (float) Math.sin(progress * Math.PI);
            float jumpDist = 8f;
            animOffset.set(attackDir.x * jumpDist * curve, attackDir.y * jumpDist * curve);
        } else {
            animOffset.set(0, 0);
        }

        if (speechTimer > 0) {
            speechTimer -= delta;
            if (speechTimer <= 0) speechText = null;
        }

        for (int i = 0; i < damageTexts.size; i++) {
            DamageText dt = damageTexts.get(i);
            dt.update(delta);
            if (dt.timer <= 0) {
                damageTexts.removeIndex(i);
                i--;
            }
        }

        visualAlpha = MathUtils.lerp(visualAlpha, targetAlpha, 0.15f);
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
