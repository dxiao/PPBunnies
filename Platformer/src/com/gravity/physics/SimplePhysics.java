package com.gravity.physics;

public class SimplePhysics extends AbstractPhysics implements Physics {
    
    public SimplePhysics(CollisionEngine collisionEngine) {
        super(collisionEngine);
    }
    
    @Override
    public PhysicalState computePhysics(Entity entity, PhysicalState state, float ticks) {
        return state.fastForward(ticks);
    }
    
    @Override
    public PhysicalState handleCollision(Entity entity, PhysicalState state, Collision[] collisions) {
        return state.killMovement();
    }
    
    @Override
    public PhysicalState rehandleCollision(Entity entity, PhysicalState state, Collision[] collisions) {
        System.err.println("WARNING: Rehandling collision for entity " + entity + " at " + state + " stupidly");
        return state.killMovement();
    }
    
}