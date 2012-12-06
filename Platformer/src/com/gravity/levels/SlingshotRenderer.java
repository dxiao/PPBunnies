package com.gravity.levels;

import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Vector2f;

import com.gravity.fauna.Player;
import com.gravity.geom.Rect;

public class SlingshotRenderer implements Renderer {
    
    private Player selfPlayer, otherPlayer;
    
    private static Image blueHandRight, blueHandLeft, yellowHandRight, yellowHandLeft;
    private static Image blueAura, yellowAura;
    private static Image blueArrow, yellowArrow;
    private Image aura, handRight, handLeft, arrow;
    private Color color;
    
    static {
        try {
            blueAura = new Image("./new-assets/bunny/force-field-blue.png");
            blueHandRight = new Image("./assets/HandAssets/HandRight.png");
            blueHandLeft = new Image("./assets/HandAssets/HandLeft.png");
            blueArrow = new Image("./assets/blueArrow2.png");
            yellowAura = new Image("./new-assets/bunny/force-field-yellow.png");
            yellowHandRight = new Image("./assets/HandAssets/HandRightYellow.png");
            yellowHandLeft = new Image("./assets/HandAssets/HandLeftYellow.png");
            yellowArrow = new Image("./assets/yellowArrow2.png");
        } catch (SlickException e) {
            e.printStackTrace();
        }
    }
    
    public SlingshotRenderer(Player selfPlayer, Player otherPlayer) {
        this.selfPlayer = selfPlayer;
        this.otherPlayer = otherPlayer;
        
        if (selfPlayer.getName().equals("pink")) {
            aura = blueAura;
            handRight = blueHandRight;
            handLeft = blueHandLeft;
            arrow = blueArrow;
            color = new Color(26, 106, 255);
        } else {
            aura = yellowAura;
            handRight = yellowHandRight;
            handLeft = yellowHandLeft;
            arrow = yellowArrow;
            color = new Color(255, 246, 0);
        }
    }

    @Override
    public void render(Graphics g, int offsetX, int offsetY) {
        g.pushTransform();
        g.translate(offsetX, offsetY);
        if (selfPlayer.slingshot) {
            Rect selfRect = selfPlayer.getPhysicalState().getRectangle();
            Rect otherRect = otherPlayer.getPhysicalState().getRectangle();
            
            g.setColor(color);
            
            dottedLine(g, selfRect.getCenter().x, selfRect.getCenter().y,
                       otherRect.getCenter().x, otherRect.getCenter().y);
            
            Vector2f delta = new Vector2f(
                    otherRect.getCenter().x - selfRect.getCenter().x,
                    otherRect.getCenter().y - selfRect.getCenter().y);
            aura.setRotation((float) delta.getTheta());
            g.drawImage(aura, otherRect.getCenter().x - aura.getWidth() / 2,
                        otherRect.getCenter().y - aura.getHeight() / 2);
            /*if (selfRect.getX() < otherRect.getX()) {
                dottedLine(g, selfRect.getCenter().x, selfRect.getCenter().y + 15,
                           otherRect.getCenter().x - 8, otherRect.getCenter().y + 15);
                g.drawImage(handRight, otherRect.getCenter().x - 15, otherRect.getCenter().y);
            } else {
                dottedLine(g, selfRect.getCenter().x, selfRect.getCenter().y + 15,
                           otherRect.getCenter().x + 10, otherRect.getCenter().y + 15);
                g.drawImage(handLeft, otherRect.getCenter().x - 15, otherRect.getCenter().y);
            }*/
        }
        g.popTransform();
    }
    
    public void dottedLine(Graphics g, float x1, float y1, float x2, float y2) {
        float wavelength = 30.0f;

        Vector2f origin = new Vector2f(x1, y1);
        Vector2f totalDelta = new Vector2f(x2 - x1, y2 - y1);
        // Make sure arrows don't intersect aura.
        totalDelta.sub(totalDelta.copy().normalise().scale(16f));

        float distance = totalDelta.length();
        int dots = (int) (distance / wavelength);
        Vector2f eachDelta = totalDelta.copy().scale(1.0f / dots);

        Vector2f position = origin.copy();
        arrow.setRotation((float) totalDelta.getTheta());
        for (int i = 0; i < dots; i++) {
            g.drawImage(arrow, position.x - yellowArrow.getWidth() / 2,
                        position.y - yellowArrow.getHeight() / 2);
            //g.fillOval(position.x - radius, position.y - radius,
            //           2 * radius, 2 * radius);
            position.add(eachDelta);
        }
    }

}
