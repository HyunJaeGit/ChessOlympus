package com.hades.game.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;
import com.hades.game.constants.UnitData;

public class Unit implements Disposable {
    public enum UnitClass {
        HERO, SHIELD, KNIGHT, ARCHER, CHARIOT, SAINT
    }

    public static final int ALIVE = 1;
    public static final int DEAD = 0;

    public final String name;
    public final String team;
    public final UnitData.Stat stat;
    public final UnitClass unitClass;

    public final Texture portrait;      // images/character/ 폴더의 일러스트
    public final Texture fieldTexture; // images/units/ 폴더의 체스말

    public int currentHp;
    public int gridX;
    public int gridY;
    public int status = ALIVE;

    public Unit(String name, String team, UnitData.Stat stat, String imageKey, UnitClass unitClass, int x, int y) {
        this.name = name;
        this.team = team;
        this.stat = stat;
        this.unitClass = unitClass;
        this.currentHp = stat.hp();
        this.gridX = x;
        this.gridY = y;

        // 1. 일러스트 로드 (images/character/)
        // imageKey가 "고세구"라면 "images/character/고세구.png"를 찾습니다.
        String portraitPath = "images/character/" + imageKey + ".png";
        this.portrait = loadSafeTexture(portraitPath);

        // 2. 체스말 로드 (images/units/)
        String fieldFileName;
        if (unitClass == UnitClass.HERO) {
            fieldFileName = imageKey; // 예: 고세구
        } else {
            String prefix = team.equalsIgnoreCase("HADES") ? "하데스" : "제우스";
            fieldFileName = prefix + imageKey; // 예: 하데스기마병
        }

        String fieldPath = "images/units/" + fieldFileName + ".png";
        this.fieldTexture = loadSafeTexture(fieldPath);
    }

    // 파일 로드 실패 시 게임이 꺼지지 않도록 처리하는 안전 메서드
    private Texture loadSafeTexture(String path) {
        try {
            if (Gdx.files.internal(path).exists()) {
                Texture tex = new Texture(Gdx.files.internal(path));
                tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                return tex;
            } else {
                // 파일이 없으면 콘솔에 에러를 찍고 기본 로고 이미지를 반환합니다.
                Gdx.app.error("Unit", "파일을 찾을 수 없습니다: " + path);
                return new Texture(Gdx.files.internal("libgdx.png"));
            }
        } catch (Exception e) {
            Gdx.app.error("Unit", "예외 발생: " + path);
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
        // portrait와 fieldTexture가 동일 파일일 수 있으므로 중복 해제 방지
        if (fieldTexture != null && fieldTexture != portrait) {
            fieldTexture.dispose();
        }
    }
}
