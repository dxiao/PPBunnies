package com.gravity.map;

import java.util.Collection;

import com.gravity.fauna.Player;
import com.gravity.geom.Rect;
import com.gravity.levels.GameplayControl;
import com.gravity.physics.Collidable;
import com.gravity.physics.RectCollision;

public class LevelFinishZone extends StaticCollidable {
    private final GameplayControl control;

    public LevelFinishZone(Rect shape, GameplayControl control) {
        super(shape);
        this.control = control;
    }

    @Override
    public void handleCollisions(float ticks, Collection<RectCollision> collisions) {
        for (RectCollision coll : collisions) {
            if (coll.getOtherEntity(this) instanceof Player) {
                control.playerFinishes((Player) coll.getOtherEntity(this));
            }
        }
    }

    @Override
    public boolean causesCollisionsWith(Collidable other) {
        return false;
    }

    @Override
    public String toString() {
        return "LevelFinishZone [" + super.toString() + "]";
    }

}
