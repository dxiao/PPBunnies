package com.gravity.map;

import java.util.List;
import java.util.Set;

import org.newdawn.slick.geom.Vector2f;

import com.google.common.collect.Sets;
import com.gravity.fauna.Player;
import com.gravity.levels.GameplayControl;

public class Checkpoint {

    private final List<Vector2f> restartPositions;
    private final GameplayControl controller;

    private boolean passed;
    private final Set<Player> playersPassed;

    public Checkpoint(GameplayControl controller, List<Vector2f> restartPositions) {
        this.controller = controller;
        this.restartPositions = restartPositions;

        passed = false;
        playersPassed = Sets.newHashSet();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Checkpoint [restartPositions=");
        builder.append(restartPositions);
        builder.append(", passed=");
        builder.append(passed);
        builder.append(", playersPassed=");
        builder.append(playersPassed);
        builder.append("]");
        return builder.toString();
    }

    public void playerPassed(Player player) {
        playersPassed.add(player);
        if (!passed && playersPassed.size() == restartPositions.size()) {
            System.out.println("Checkpoint passed.");
            passed = true;
            controller.newStartPositions(restartPositions);
        }
    }

}
