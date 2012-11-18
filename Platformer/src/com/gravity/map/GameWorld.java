package com.gravity.map;

import java.util.List;

import org.newdawn.slick.geom.Vector2f;
import org.newdawn.slick.tiled.Layer;

import com.gravity.geom.Rect;
import com.gravity.physics.Collidable;
import com.gravity.root.Renderer;

public interface GameWorld extends Renderer {

    public static final String TILES_LAYER_NAME = "collisions";
    public static final String SPIKES_LAYER_NAME = "spikes";
    public static final String PLAYERS_LAYER_NAME = "players";
    public static final String MARKERS_LAYER_NAME = "level markers";
    public static final String BOUNCYS_LAYER_NAME = "bouncys";
    public static final String FINISH_MARKER_NAME = "finish";
    public static final String DISAPPEARING_LAYER_NAME = "disappearing";

    public static final Vector2f PLAYER_ONE_DEFAULT_STARTPOS = new Vector2f(256, 512);
    public static final Vector2f PLAYER_TWO_DEFAULT_STARTPOS = new Vector2f(224, 512);

    /** Initializes the GameWorld. Must be called before this object is used otherwise. */
    public void initialize();

    /** Get the height of this map, in pixels */
    public int getHeight();

    /** Get the width of this map, in pixels */
    public int getWidth();

    /** Return a list of entities for use in collision detection that do not wish to be notified of collisions */
    public List<Collidable> getTerrainEntitiesNoCalls();

    /** Return a list of entities for use in collision detection that wish to be notified of collisions */
    public List<Collidable> getTerrainEntitiesCallColls();

    /** Return a list of player start positions, in order from first to nth player */
    public List<Vector2f> getPlayerStartPositions();

    /** Return the goal rectangle */
    public Rect getFinishRect();

    /** Return the layer with the given name */
    public Layer getLayer(String name);

}
