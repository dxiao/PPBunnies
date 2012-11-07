package com.gravity.physics;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.gravity.entity.Entity;
import com.gravity.entity.PhysicallyStateful;
import com.gravity.geom.Rect;
import com.gravity.geom.Rect.Side;

/**
 * A Physics simulator which assumes gravity, but no bouncing.
 * 
 * @author xiao, predrag
 * 
 */
public class GravityPhysics implements Physics {

    private final CollisionEngine collisionEngine;
    private final float gravity;
    private final float backstep;
    private final float offsetGroundCheck;

    GravityPhysics(CollisionEngine collisionEngine, float gravity, float backstep, float offsetGroundCheck) {
        Preconditions.checkArgument(backstep <= 0f, "Backstep has to be non-positive.");
        this.collisionEngine = collisionEngine;
        this.gravity = gravity;
        this.backstep = backstep;
        this.offsetGroundCheck = offsetGroundCheck;
    }

    public boolean isOnGround(Entity entity) {
        Rect collider = entity.getPhysicalState().getRectangle().translate(0, offsetGroundCheck);
        List<Collidable> collisions = collisionEngine.collisionsInLayer(0f, collider, LayeredCollisionEngine.FLORA_LAYER);

        for (Collidable c : collisions) {
            c.handleCollisions(0f, Lists.newArrayList(new RectCollision(entity, c, 0f, null, null)));
        }
        return !collisions.isEmpty();
    }

    @Override
    public PhysicalState computePhysics(Entity entity) {
        if (isOnGround(entity)) {
            return entity.getPhysicalState();
        } else {
            return entity.getPhysicalState().addAcceleration(0f, gravity);
        }
    }

    @Override
    public PhysicalState handleCollision(Entity entity, Collection<RectCollision> collisions) {
        PhysicalState state = entity.getPhysicalState();
        float velX = state.velX;
        float velY = state.velY;
        float accX = state.accX;
        float accY = state.accY;
        for (RectCollision c : collisions) {
            EnumSet<Side> sides = c.getMyCollisions(entity);
            Preconditions.checkArgument(sides != null, "Collision passed did not involve entity: " + entity + ", " + c);

            if (Side.isSimpleSet(sides)) {
                if (sides.contains(Side.TOP)) {
                    velY = Math.max(velY, 0);
                    accY = Math.max(accY, 0);
                }
                if (sides.contains(Side.LEFT)) {
                    velX = Math.max(velX, 0);
                    accX = Math.max(accX, 0);
                }
                if (sides.contains(Side.BOTTOM)) {
                    velY = Math.min(velY, 0);
                    accY = Math.min(accY, 0);
                }
                if (sides.contains(Side.RIGHT)) {
                    velX = Math.min(velX, 0);
                    accX = Math.min(accX, 0);
                }
            } else {
                velX = 0;
                velY = 0;
                accX = 0;
                accY = 0;
            }
        }
        return new PhysicalState(entity.getRect(0), velX, velY, accX, accY);
    }

    @Override
    public PhysicalState rehandleCollision(PhysicallyStateful entity, Collection<RectCollision> collisions) {
        System.err.println("Warning: rehandling collisions for: " + entity);
        return entity.getPhysicalState().snapshot(backstep);
    }
}