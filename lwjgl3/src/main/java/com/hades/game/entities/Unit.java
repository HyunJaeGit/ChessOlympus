package com.hades.game.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;
import com.hades.game.constants.UnitData;

public class Unit implements Disposable {
    // 병과 구분을 위한 Enum
    public enum UnitClass {
        HERO, SHIELD, KNIGHT, ARCHER, CHARIOT, SAINT
    }

    public static final int ALIVE = 1;
    public static final int DEAD = 0;

    public final String name;
    public final String team;
    public final UnitData.Stat stat;
    public final UnitClass unitClass;

    // 카드용 고화질 일러스트와 필드용 체스말 이미지를 분리하여 저장합니다.
    public final Texture portrait;     // UI 카드용 일러스트
    public final Texture fieldTexture;  // 체스판 위에 올라가는 유닛 이미지

    public int currentHp;
    public int gridX;
    public int gridY;
    public int status = ALIVE;

    // 생성자: 두 폴더에서 각각의 이미지를 로드합니다.
    public Unit(String name, String team, UnitData.Stat stat, UnitClass unitClass, int x, int y) {
        this.name = name;
        this.team = team;
        this.stat = stat;
        this.unitClass = unitClass;
        this.currentHp = stat.hp();
        this.gridX = x;
        this.gridY = y;

        // 1. 일러스트 로드 (images/character 폴더)
        String portraitPath = "images/character/" + stat.imageName() + ".png";
        this.portrait = new Texture(Gdx.files.internal(portraitPath));

        // 2. 체스말 유닛 이미지 로드 (images/units 폴더)
        // 파일명이 동일하므로 경로만 다르게 설정합니다.
        String fieldPath = "images/units/" + stat.imageName() + ".png";
        this.fieldTexture = new Texture(Gdx.files.internal(fieldPath));
    }

    // 공격력 또는 반격력 반환
    public int getPower(boolean isMyTurn) {
        return isMyTurn ? stat.atk() : stat.counterAtk();
    }

    // 사거리 내에 타겟이 있는지 확인
    public boolean canReach(Unit target) {
        if (target == null) return false;
        int dist = Math.abs(this.gridX - target.gridX) + Math.abs(this.gridY - target.gridY);
        return dist <= this.stat.range();
    }

    // 생존 여부 확인
    public boolean isAlive() {
        return status == ALIVE && currentHp > 0;
    }

    // 위치 갱신
    public void setPosition(int x, int y) {
        this.gridX = x;
        this.gridY = y;
    }

    // 메모리 해제: 생성된 두 개의 텍스처를 모두 제거합니다.
    @Override
    public void dispose() {
        if (portrait != null) {
            portrait.dispose();
        }
        if (fieldTexture != null) {
            fieldTexture.dispose();
        }
    }
}
