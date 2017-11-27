/***************************************************************
* file: Camera.java
* author: CS445 Group 42^3
* class: CS 445 – Computer Graphics
*
* assignment: Final Project
* date last modified: 10/08/2017
*
* purpose: This class holds the camera state: position, pitch angle,
* and yaw angle.
* 
****************************************************************/
package cs445craft;

import static org.lwjgl.opengl.GL11.glRotatef;
import static org.lwjgl.opengl.GL11.glTranslatef;

public class Camera {
    private static final float MIN_PITCH = -90.0f;
    private static final float MAX_PITCH = 90.0f;
    public float x,y,z, yaw, pitch;
    
    public Camera(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = 180.0f;
        this.pitch = 45.0f;
    }
    
    /**
    * method: incYaw
    * purpose: add a float value to the yaw angle of the camera (in degrees).
    * Positive values yaw right, negative values yaw left.
    **/
    public void incYaw(float yaw) {
        this.yaw += yaw;
    }
    
    /**
    * method: incPitch
    * purpose: add a float value to the pitch angle of the camera (in degrees).
    * Positive values pitch up, negative values pitch down.
    **/
    public void incPitch(float pitch) {
        this.pitch -= pitch;
        if (this.pitch < MIN_PITCH) {
            this.pitch = MIN_PITCH;
        } else if (this.pitch > MAX_PITCH) {
            this.pitch = MAX_PITCH;
        }
    }
    
    /**
    * method: lookThrough
    * purpose: uses glRotatef and glTranslatef to "look through" the camera. Call
    * on each frame.
    **/
    public void lookThrough() {
        glRotatef(pitch, 1.0f, 0.0f, 0.0f);
        glRotatef(yaw, 0.0f, 1.0f, 0.0f);
        glTranslatef(-x, -y, -z);
    }
}
