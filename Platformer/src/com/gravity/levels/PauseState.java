package com.gravity.levels;

import java.util.List;

import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Vector2f;

import com.google.common.collect.Lists;

public class PauseState extends CageSelectState {
    
    public static final int ID = 22;
    
    private MenuCage resumeCage;
    
    public PauseState() throws SlickException {
        super("Pause", "assets/pause.tmx", ID);
    }

    @Override
    public void enterCageState(MenuCage cage) {
        game.enterState(cage.getToState(), fadeOut, fadeIn);
    }
    
    @Override
    protected CagesAndRenderers constructCagesAndRenderers() {
        List<MenuCage> cages = Lists.newLinkedList();
        List<Renderer> renderers = Lists.newLinkedList();
        
        Vector2f resumeLoc = map.getSpecialLocation("resume");
        Vector2f mainMenuLoc = map.getSpecialLocation("mainmenu");

        CageRenderer resumeRend, mainMenuRend;
        try {
            resumeRend = new CageRenderer(resumeLoc.x, resumeLoc.y, "Resume");
            mainMenuRend = new CageRenderer(mainMenuLoc.x, mainMenuLoc.y, "Main Menu");
        } catch (SlickException e) {
            throw new RuntimeException(e);
        }

        resumeCage = new MenuCage(resumeRend.getRect(), MainMenuState.ID);
        MenuCage mainMenuCage = new MenuCage(mainMenuRend.getRect(), MainMenuState.ID);
        renderers.add(resumeRend);
        renderers.add(mainMenuRend);
        cages.add(resumeCage);
        cages.add(mainMenuCage);
        
        return new CagesAndRenderers(cages, renderers);
    }

    public void setGameplayState(GameplayState gameplayState) {
        resumeCage.setToState(gameplayState.getID());
    }

}
