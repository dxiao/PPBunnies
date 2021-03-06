package com.gravity.levels;

import java.util.Collection;
import java.util.List;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Vector2f;
import org.newdawn.slick.state.BasicGameState;
import org.newdawn.slick.state.StateBasedGame;
import org.newdawn.slick.state.transition.FadeInTransition;
import org.newdawn.slick.state.transition.FadeOutTransition;
import org.newdawn.slick.tiled.Layer;
import org.newdawn.slick.tiled.Tile;
import org.newdawn.slick.tiled.TiledMapPlus;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.gravity.camera.Camera;
import com.gravity.camera.PanningCamera;
import com.gravity.camera.PlayerStalkingCamera;
import com.gravity.fauna.Player;
import com.gravity.fauna.PlayerKeyboardController;
import com.gravity.fauna.PlayerRenderer;
import com.gravity.fauna.WallofDeath;
import com.gravity.geom.Rect;
import com.gravity.geom.Rect.Side;
import com.gravity.map.LevelFinishZone;
import com.gravity.map.TileType;
import com.gravity.map.TileWorld;
import com.gravity.map.TileWorldRenderer;
import com.gravity.map.tiles.CompositeTileRenderer;
import com.gravity.map.tiles.DisappearingTileController;
import com.gravity.map.tiles.FadeOutTileRenderer;
import com.gravity.map.tiles.FallingTile;
import com.gravity.map.tiles.MovingEntity;
import com.gravity.map.tiles.PlayerKeyedTile;
import com.gravity.map.tiles.StandardTileRenderer;
import com.gravity.map.tiles.TileRenderer;
import com.gravity.map.tiles.TransitionTileRenderer;
import com.gravity.physics.Collidable;
import com.gravity.physics.CollisionEngine;
import com.gravity.physics.GravityPhysics;
import com.gravity.physics.LayeredCollisionEngine;
import com.gravity.physics.PhysicalState;
import com.gravity.physics.PhysicsFactory;
import com.gravity.root.GameSounds;
import com.gravity.root.GameSounds.Event;
import com.gravity.root.GameWinState;
import com.gravity.root.PauseState;
import com.gravity.root.PlatformerGame;
import com.gravity.root.RestartGameplayState;
import com.gravity.root.SlideTransition;

public class GameplayState extends BasicGameState implements GameplayControl, Resetable {

    final int ID;

    public static final String PANNING_CAMERA = "panning";
    public static final String STALKING_CAMERA = "stalking";

    @Override
    public int getID() {
        return ID;
    }

    protected TileWorld map;
    protected Player playerA, playerB;
    protected RenderList renderers;
    protected PlayerKeyboardController controllerA, controllerB;
    protected List<UpdateCycling> updaters;
    protected CollisionEngine collider;
    protected GameContainer container;
    protected StateBasedGame game;
    protected GravityPhysics gravityPhysics;
    protected LevelFinishZone finish;
    protected Player finishedPlayer;
    protected Camera camera;
    protected WallofDeath wallofDeath;

    protected boolean finished = false;
    protected boolean done = false;

    protected final List<Resetable> resetableTiles = Lists.newArrayList();
    private final String levelName;
    protected String winText;

    private static final float MIN_SLINGSHOT_DISTANCE = 5f;

    public GameplayState(LevelInfo info) throws SlickException {
        ID = info.stateId;
        this.levelName = info.title;
        map = new TileWorld(levelName, new TiledMapPlus(info.mapfile), this);
        winText = info.victoryText;
    }

    @Override
    public void init(GameContainer container, StateBasedGame game) throws SlickException {
        System.err.println(">>>Loading level " + levelName);
        this.container = container;
        this.game = game;
        map.initialize();
        reloadGame();
    }

    public void reloadGame() {
        finished = false;
        System.err.println(">>>Processing level " + levelName);
        pauseRender();
        pauseUpdate();

        collider = new LayeredCollisionEngine();
        updaters = Lists.newLinkedList();
        renderers = new RenderList();
        resetableTiles.clear();

        for (DisappearingTileController controller : map.reinitializeDisappearingLayers(collider)) {
            updaters.add(controller);
        }

        gravityPhysics = PhysicsFactory.createDefaultGravityPhysics(collider);

        // Map initialization
        for (Collidable c : map.getTerrainEntitiesCallColls()) {
            collider.addCollidable(c, LayeredCollisionEngine.FLORA_LAYER);
        }
        for (Collidable c : map.getTerrainEntitiesNoCalls()) {
            collider.addCollidable(c, LayeredCollisionEngine.FLORA_LAYER);
        }
        finish = new LevelFinishZone(map.getFinishRect(), this);
        collider.addCollidable(finish, LayeredCollisionEngine.FLORA_LAYER);
        finishedPlayer = null;
        System.out.println("Got finish zone at: " + finish + " for map " + map);
        updaters.addAll(map.getTriggeredTexts());
        updaters.addAll(map.getTriggeredImages());
        Collection<List<MovingEntity>> movingColls = map.getMovingCollMap().values();
        for (List<MovingEntity> l : movingColls) {
            updaters.addAll(l);
        }
        PauseTextRenderer ptr = new PauseTextRenderer(this);
        renderers.add(ptr, RenderList.FLOATING);
        updaters.add(ptr);

        // Player initialization
        List<Vector2f> playerPositions = map.getPlayerStartPositions();
        Preconditions.checkArgument(playerPositions.size() == 2,
                "Invalid number of player start positions: expected 2, got " + playerPositions.size());
        playerA = new Player(this, gravityPhysics, "pink", playerPositions.get(0));
        playerB = new Player(this, gravityPhysics, "yellow", playerPositions.get(1));
        updaters.add(playerA);
        updaters.add(playerB);
        renderers.add(new TileWorldRenderer(map), RenderList.TERRA);
        renderers.add(new PlayerRenderer(playerA), RenderList.FAUNA);
        renderers.add(new PlayerRenderer(playerB), RenderList.FAUNA);
        renderers.add(new SlingshotRenderer(playerA, playerB), RenderList.SLINGSHOT);
        renderers.add(new SlingshotRenderer(playerB, playerA), RenderList.SLINGSHOT);
        collider.addCollidable(playerA, LayeredCollisionEngine.FAUNA_LAYER);
        collider.addCollidable(playerB, LayeredCollisionEngine.FAUNA_LAYER);
        //@formatter:off
        controllerA = new PlayerKeyboardController(playerA)
                .setLeft(Input.KEY_A).setRight(Input.KEY_D)
                .setJump(Input.KEY_W).setMisc(Input.KEY_TAB);
        controllerB = new PlayerKeyboardController(playerB)
                .setLeft(Input.KEY_LEFT).setRight(Input.KEY_RIGHT)
                .setJump(Input.KEY_UP).setMisc(Input.KEY_SPACE);
        //@formatter:on

        // Map-tile construction
        TiledMapPlus tiledMap = map.map;
        Layer pkLayer = tiledMap.getLayer(TileWorld.PLAYERKEYED_LAYER_NAME);
        if (pkLayer != null) {
            pkLayer.visible = false;
            TileRenderer rendererDelegate = new StandardTileRenderer(tiledMap, TileType.PLAYER_KEYED_UNSET);
            //@formatter:off
            TileRenderer rendererDelegateWarningYellow = new CompositeTileRenderer(
                    new TransitionTileRenderer(tiledMap, TileType.PLAYER_KEYED_YELLOW, TileType.PLAYER_KEYED_WARNING),
                    new FadeOutTileRenderer(tiledMap, TileType.PLAYER_KEYED_WARNING),
                    0.5f);
            TileRenderer rendererDelegateWarningBlue = new CompositeTileRenderer(
                    new TransitionTileRenderer(tiledMap, TileType.PLAYER_KEYED_BLUE, TileType.PLAYER_KEYED_WARNING),
                    new FadeOutTileRenderer(tiledMap, TileType.PLAYER_KEYED_WARNING),
                    0.5f);
            TileRenderer rendererDelegateWarningBoth = new CompositeTileRenderer(
                    new TransitionTileRenderer(tiledMap, TileType.PLAYER_KEYED_UNSET, TileType.PLAYER_KEYED_WARNING),
                    new FadeOutTileRenderer(tiledMap, TileType.PLAYER_KEYED_WARNING),
                    0.5f);
            //@formatter:on
            TileRenderer rendererDelegateYellow = new TransitionTileRenderer(tiledMap, TileType.PLAYER_KEYED_UNSET, TileType.PLAYER_KEYED_YELLOW);
            TileRenderer rendererDelegateBlue = new TransitionTileRenderer(tiledMap, TileType.PLAYER_KEYED_UNSET, TileType.PLAYER_KEYED_BLUE);
            try {
                for (Tile tile : pkLayer.getTiles()) {
                    PlayerKeyedTile pkTile = new PlayerKeyedTile(new Rect(tile.x * 32, tile.y * 32, 32, 32), collider, rendererDelegate,
                            rendererDelegateYellow, rendererDelegateBlue, rendererDelegateWarningYellow, rendererDelegateWarningBlue,
                            rendererDelegateWarningBoth, pkLayer, tile.x, tile.y);
                    resetableTiles.add(pkTile);
                    updaters.add(pkTile);
                    collider.addCollidable(pkTile, LayeredCollisionEngine.FLORA_LAYER);
                    renderers.add(pkTile, RenderList.TERRA);
                }
            } catch (SlickException e) {
                throw new RuntimeException("Unable to make keyedplayertile", e);
            }
        }

        Layer fallSpike = tiledMap.getLayer(TileWorld.FALLING_SPIKE_LAYER_NAME);
        if (fallSpike != null) {
            fallSpike.visible = false;
            TileRenderer rd = new StandardTileRenderer(tiledMap, TileType.SPIKE);
            try {
                for (Tile tile : fallSpike.getTiles()) {
                    FallingTile fsTile = new FallingTile(this, new Rect(tile.x * 32, tile.y * 32, 32, 32).grow(-3 * TileWorld.TILE_MARGIN),
                            3 * TileWorld.TILE_MARGIN, rd);
                    updaters.add(fsTile);
                    collider.addCollidable(fsTile, LayeredCollisionEngine.FALLING_LAYER);
                    renderers.add(fsTile, RenderList.TERRA);
                }
            } catch (SlickException e) {
                throw new RuntimeException("Unable to make falling tile", e);
            }
        }

        // Camera initialization
        float panX = Math.min(playerA.getPhysicalState().getPosition().x, playerB.getPhysicalState().getPosition().x);
        panX = Math.max(0, panX - 300);
        PanningCamera pancam = new PanningCamera(2000, new Vector2f(panX, 0), new Vector2f(0.035f, 0), new Vector2f(map.getWidth()
                - PlatformerGame.WIDTH, 0), PlatformerGame.WIDTH, PlatformerGame.HEIGHT);
        camera = pancam;
        if (map.map.getMapProperty("camera", PANNING_CAMERA).equals(STALKING_CAMERA)) {
            camera = new PlayerStalkingCamera(PlatformerGame.WIDTH, PlatformerGame.HEIGHT, new Vector2f(0, 0), new Vector2f(map.getWidth(),
                    map.getHeight()), playerA, playerB);
        }
        updaters.add(pancam);

        // Wall of death initialization
        String wallVelStr;
        if ((wallVelStr = map.map.getMapProperty("wallofdeath", null)) != null) {
            float wallVel = 0.035f;
            try {
                wallVel = Float.parseFloat(wallVelStr);
            } catch (NumberFormatException e) {
                System.err.println("Could not format wall of death velocity, using default (0.035) instead.");
            }
            wallofDeath = new WallofDeath(2000, panX + 32, wallVel, Lists.newArrayList(playerA, playerB), this, PlatformerGame.HEIGHT);
            updaters.add(wallofDeath);
            renderers.add(wallofDeath, RenderList.FAUNA);
        }

        unpauseRender();
        unpauseUpdate();
    }

    @Override
    public void render(GameContainer container, StateBasedGame game, Graphics g) throws SlickException {
        Vector2f offset = camera.getViewport().getPosition();
        g.setAntiAlias(true);

        renderers.render(g, (int) offset.x, (int) offset.y);
    }

    @Override
    public void update(GameContainer container, StateBasedGame game, int delta) throws SlickException {
        for (UpdateCycling uc : updaters) {
            uc.startUpdate(delta);
        }
        collider.update(delta);
        for (UpdateCycling uc : updaters) {
            uc.finishUpdate(delta);
        }

        // Tell player when to die if off the screen
        checkDeath(playerA);
        checkDeath(playerB);

        float xOffset = camera.getViewport().getX();
        // Prevent player from going off right side
        checkRightSide(playerA, xOffset);
        checkRightSide(playerB, xOffset);

        // if both bunnies did not collide with win box this turn, reset
        finishedPlayer = null;
    }

    private void checkDeath(Player player) {
        Vector2f pos = player.getPhysicalState().getPosition();
        Rect r = camera.getViewport();
        if (pos.x + r.getX() + 32 < 0 || pos.y + r.getY() > r.getHeight() + 32) {
            // if (pos.x + offsetX2 + 32 < 0) {
            GameSounds.playSoundFor(Event.FELL_OFF_MAP);
            playerDies(player);
        }
    }

    private void checkRightSide(Player player, float offsetX2) {
        PhysicalState state = player.getPhysicalState();
        player.setPhysicalState(new PhysicalState(state.getRectangle().translateTo(
                Math.min(state.getRectangle().getX(), -offsetX2 + PlatformerGame.WIDTH - 32), state.getRectangle().getY()), state.velX, state.velY,
                state.accX, state.accY, state.surfaceVelX));
    }

    @Override
    public void keyPressed(int key, char c) {
        if (!controllerA.handleKeyPress(key)) {
            controllerB.handleKeyPress(key);
        }
        if (c == '*' && PlatformerGame.CHEATS_ENABLED) { // HACK: testing purposes only REMOVE FOR RELEASE
            stateWin();
        }
    }

    @Override
    public void keyReleased(int key, char c) {
        if (!controllerA.handleKeyRelease(key) && !controllerB.handleKeyRelease(key)) {
            if ((key == Input.KEY_ESCAPE) && canPause()) {
                PauseState pause = (PauseState) (game.getState(PauseState.ID));
                pause.setGameplayState(this);
                game.enterState(PauseState.ID, new SlideTransition(game.getState(PauseState.ID), Side.BOTTOM, 1000), null);
            }
        }
    }

    public boolean canPause() {
        return true;
    }

    @Override
    public void playerDies(Player player) {
        reset();
        RestartGameplayState pts = (RestartGameplayState) (game.getState(RestartGameplayState.ID));
        pts.setToState(this);
        game.enterState(RestartGameplayState.ID, new FadeOutTransition(Color.red.darker(), 300), null);
    }

    @Override
    public void playerHitSpikes(Player player) {
        // swapPlayerControls(Control.getById(rand.nextInt(Control.size())));
        playerDies(player);
        System.out.println("Player " + player.toString() + " hit spikes.");
    }

    /**
     * 
     */
    @Override
    public void specialMoveSlingshot(Player slingshoter, float strength) {
        // check for minimum distance between players
        if (playerA.getPhysicalState().getPosition().sub(playerB.getPhysicalState().getPosition()).length() < MIN_SLINGSHOT_DISTANCE) {
            GameSounds.playSoundFor(Event.NO_SLING);
        } else {
            if (slingshoter == playerA) {
                playerB.slingshotMe(strength, playerA.getPhysicalState().getPosition().sub(playerB.getPhysicalState().getPosition()));
            } else if (slingshoter == playerB) {
                playerA.slingshotMe(strength, playerB.getPhysicalState().getPosition().sub(playerA.getPhysicalState().getPosition()));
            } else {
                throw new RuntimeException("Who the **** called this method?");
                // Now now, Kevin, we don't use that kind of language in these parts. -xiao ^_^
            }
        }
    }

    @Override
    public void playerFinishes(Player player) {
        if (finishedPlayer == null) {
            finishedPlayer = player;
        } else if (finishedPlayer != player) {
            stateWin();
        }
    }

    protected void stateWin() {
        done = true;
        if (!finished) {
            reset();
            map.reset();
            finished = true;
            ((GameWinState) game.getState(GameWinState.ID)).setWinText(winText);
            game.enterState(GameWinState.ID, new FadeOutTransition(), new FadeInTransition());
            GameSounds.playSoundFor(Event.CAGE_SLAM);
        }
    }

    @Override
    public void newStartPositions(List<Vector2f> startPositions) {
        Preconditions.checkArgument(startPositions.size() == 2);
        map.setStartPositions(startPositions);
    }

    @Override
    public void reset() {
        collider.stop();
        for (Resetable r : resetableTiles) {
            r.reset();
        }
    }

    public boolean isFinished() {
        return done;
    }
}
