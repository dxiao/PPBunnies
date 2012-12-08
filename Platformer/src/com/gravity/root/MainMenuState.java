package com.gravity.root;

import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;

import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Vector2f;

import com.google.common.collect.Lists;
import com.gravity.levels.CageRenderer;
import com.gravity.levels.LevelInfo;
import com.gravity.levels.MenuCage;
import com.gravity.levels.Renderer;

/**
 * The main menu loop.
 * 
 * @author dxiao
 */
public class MainMenuState extends CageSelectState {

    static public final int ID = 50;

    private final LevelInfo[] levels;

    public MainMenuState(LevelInfo[] levels) throws SlickException {
        super(new LevelInfo("Main Menu", "assets/mainmenu2.tmx", ID));
        this.levels = levels;
    }

    @Override
    public void reloadGame() {
        GameSounds.playMenuMusic();
        super.reloadGame();
    }

    @Override
    protected CagesAndRenderers constructCagesAndRenderers() {
        List<MenuCage> cages = Lists.newLinkedList();
        List<Renderer> renderers = Lists.newLinkedList();

        Vector2f quitLoc = map.getQuitLocation();
        Vector2f optLoc = map.getOptionsLocation();
        Vector2f helpLoc = map.getHelpLocation();

        MenuCage quitCage = new MenuCage(game, quitLoc.x, quitLoc.y, GameQuitState.ID);
        MenuCage optCage = new MenuCage(game, optLoc.x, optLoc.y, CreditsState.ID);
        MenuCage helpCage = new MenuCage(game, helpLoc.x, helpLoc.y, PlatformerGame.TUTORIAL1);

        CageRenderer quitRend, optRend, helpRend, levelRend;
        try {
            quitRend = new CageRenderer(quitCage, "Quit Game");
            optRend = new CageRenderer(optCage, "Credits");
            helpRend = new CageRenderer(helpCage, "Help!");
        } catch (SlickException e) {
            throw new RuntimeException(e);
        }

        renderers.add(quitRend);
        renderers.add(optRend);
        renderers.add(helpRend);
        cages.add(quitCage);
        cages.add(optCage);
        cages.add(helpCage);

        SortedSet<Vector2f> levelLocs = map.getLevelLocations();
        LinkedList<Vector2f> reversedLevelLocs = Lists.newLinkedList();
        for (Vector2f loc : levelLocs) {
            reversedLevelLocs.addFirst(loc);
        }
        int i = levelLocs.size();
        for (Vector2f loc : reversedLevelLocs) {
            i--;
            LevelInfo info = null;
            for (LevelInfo level : levels) {
                if (level.levelOrder == i) {
                    info = level;
                    break;
                }
            }
            if (info == null) {
                continue;
            }
            MenuCage levelCage = new MenuCage(game, loc.x, loc.y, info.stateId);
            try {
                levelRend = new CageRenderer(levelCage, info.title);
            } catch (SlickException e) {
                throw new RuntimeException(e);
            }
            renderers.add(levelRend);
            cages.add(levelCage);
        }

        return new CagesAndRenderers(cages, renderers);
    }

    @Override
    protected void stateWin() {
        ((GameWinState) game.getState(GameWinState.ID)).setWinText(winText);
        game.enterState(GameWinState.ID);
    }

}
