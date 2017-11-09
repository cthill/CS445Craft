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

import static org.lwjgl.opengl.GL11.*;

public class Camera {
    protected Vector3f position;
    protected float yaw, pitch;
    
    public Camera(float x, float y, float z) {
        position = new Vector3f(x, y, z);
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
    }
    
    /**
    * method: move
    * purpose: move the camera forward or backwards in the 3d world by the given
    * distance. Positive values move forward, negative values move backward.
    **/
    public void move(float dist) {
        float xOffset = dist * (float) Math.sin(Math.toRadians(yaw));
        float zOffset = dist * (float) Math.cos(Math.toRadians(yaw));
        position.x -= xOffset;
        position.z += zOffset;
    }
    
    /**
    * method: strafe
    * purpose: strafe the camera in the 3d world by the given distance. Positive
    * values strafe right, negative values strafe left.
    **/
    public void strafe(float dist) {
        float xOffset = dist * (float)Math.sin(Math.toRadians(yaw-90));
        float zOffset = dist * (float)Math.cos(Math.toRadians(yaw-90));
        position.x += xOffset;
        position.z -= zOffset;
    }
    
    /**
    * method: elevate
    * purpose: change the camera's elevation by the given float distance. Positive
    * values increase elevation, negative values decrease elevation.
    **/
    public void elevate(float dist) {
        position.y += dist;
    }
    
    /**
    * method: lookThrough
    * purpose: uses glRotatef and glTranslatef to "look through" the camera. Call
    * on each frame.
    **/
    public void lookThrough() {
        glRotatef(pitch, 1.0f, 0.0f, 0.0f);
        glRotatef(yaw, 0.0f, 1.0f, 0.0f);
        glTranslatef(position.x, position.y, position.z);
    }
}
