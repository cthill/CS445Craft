/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cs445craft;

import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glTranslatef;

/**
 *
 * @author cthill
 */
public abstract class Drawable {
    protected float x, y, z;
    public Drawable(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public abstract void draw();
}
