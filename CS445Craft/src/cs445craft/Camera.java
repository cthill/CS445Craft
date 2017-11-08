/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cs445craft;

import static org.lwjgl.opengl.GL11.*;

/**
 *
 * @author cthill
 */
public class Camera {
    protected Vector3f position, look;
    protected float yaw, pitch;
    
    public Camera(float x, float y, float z) {
        position = new Vector3f(x, y, z);
        look = new Vector3f(0.0f, 15.0f, 0.0f);
    }
    
    public void incYaw(float yaw) {
        this.yaw += yaw;
    }
    
    public void incPitch(float pitch) {
        this.pitch -= pitch;
    }
    
    public void move(float dist) {
        float xOffset = dist * (float) Math.sin(Math.toRadians(yaw));
        float zOffset = dist * (float) Math.cos(Math.toRadians(yaw));
        position.x -= xOffset;
        position.z += zOffset;
    }
    
    public void strafe(float dist) {
        float xOffset = dist * (float)Math.sin(Math.toRadians(yaw-90));
        float zOffset = dist * (float)Math.cos(Math.toRadians(yaw-90));
        position.x += xOffset;
        position.z -= zOffset;
    }
    
    public void elevate(float dist) {
        position.y += dist;
    }
    
    public void lookThrough() {
        glRotatef(pitch, 1.0f, 0.0f, 0.0f);
        glRotatef(yaw, 0.0f, 1.0f, 0.0f);
        glTranslatef(position.x, position.y, position.z);
    }
}
