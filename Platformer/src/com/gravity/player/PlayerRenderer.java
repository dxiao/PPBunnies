package com.gravity.player;

import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;

import com.gravity.root.Renderer;

public class PlayerRenderer implements Renderer {
	private Player player;
	private Image playerOneImage;
	private float x;
	private float y;

	public PlayerRenderer(Player player) {
		this.player = player;
		try {
            playerOneImage = new Image("./assets/TopHatGuy.png");
        } catch (SlickException e) {
            throw new RuntimeException(e);
        }

	}

	@Override
	public void render(Graphics g) {
		this.x = player.oldPosition.x;
		this.y = player.oldPosition.y + 512;
		g.drawImage(playerOneImage, this.x, this.y);
	}
}
