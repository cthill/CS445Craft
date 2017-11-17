/***************************************************************
* file: CS445Craft.java
* author: CS445 Group 42^3
* class: CS 445 – Computer Graphics
*
* assignment: Final Project
* date last modified: 10/16/2017
*
* purpose: This is the main class in the program. It instantiates
* the screen, the camera, and collects keyboard/mouse input, checks
* for collisions.
* 
****************************************************************/
package cs445craft;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class CS445Craft {
    private static World w;
    
    /**
    * method: coordsToChunk
    * purpose: return the chunk a pair of xy coordinates occupies. In the future,
    * this method will support mulitple chunks.
    **/
    private static Chunk coordsToChunk(float x, float z) {
        // TODO: allow for more chunks
        return w.coordsToChunk(x, z);
    }
    
    /**
    * method: run
    * purpose: The main event loop of the game. Collects user input, moves camera,
    * and checks for collision. It requires a Screen and Camera object.
    **/
    private static void run(Screen screen, Camera camera) {
        Mouse.setGrabbed(true);
        
        boolean noClip = false;
        float mouseSens = 0.09f;
        float speed = .75f;
        float gravity = 0.025f;
        float terminalVelocity = speed * 15;
        float jumpSpeed = 0.40f;
        float yspeed = 0.0f;
        
        // player is 1.5 blocks tall, so yOffset = Chunk.CUBE_S * 1.5;
        float playerHeight = Chunk.BLOCK_SIZE * 1.5f;
//        float yOffsetSideCollide = Chunk.BLOCK_SIZE / 2.0f;
        boolean lastSpaceState = false; // last state of the spacebar (true == pressed)
        boolean lastVState = false;
        boolean lastRState = false;
        
        while(true) {
            // listen for q key
            if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE) || Keyboard.isKeyDown(Keyboard.KEY_Q)) {
                screen.close();
                break;
            }
            
            // listen for close requested
            if (screen.getCloseRequested()) {
                break;
            }
            
            if (Keyboard.isKeyDown(Keyboard.KEY_V)) {
                if (!lastVState) {
                    lastVState = true;
                    noClip = !noClip;
                }
            } else {
                lastVState = false;
            }
            
            if (Keyboard.isKeyDown(Keyboard.KEY_R)) {
                if (!lastRState) {
                    lastRState = true;
                    w.swapMeshes();
                }
            } else {
                lastRState = false;
            }
            
            //Chunk currentChunk = coordsToChunk(camera.x, camera.z);
            float dx = 0.0f;
            float dz = 0.0f;
            
            float dy = yspeed;
   
            // gravity and jumping
            // listen for jump
            boolean blockBelow = w.blockAt(camera.x, camera.y + dy + playerHeight, camera.z);
            boolean blockAbove = w.blockAt(camera.x, camera.y + dy, camera.z);
            
            if (noClip) {
                // skip gravity code if noclip is enabled
                if (Keyboard.isKeyDown(Keyboard.KEY_SPACE))
                    camera.y -= speed;
                if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT))
                    camera.y += speed;
                dy = 0;
                yspeed = 0;
            } else if (blockAbove || blockBelow) {
                // prevent player from getting stuck in floor if they have high y speed
                float wouldBeY = camera.y + dy;
                
                float nearestSnapY = Math.round(camera.y);
                
                float overshoot = ((float) Math.floor(dy)) / 2.0f;
                float rounded = Math.round(camera.y + dy);
                camera.y = rounded - overshoot - playerHeight;
                
                // set yspeed and dy to 0 
                yspeed = 0;
                dy = 0;
            } else {                
                // accelerate on the yaxis for the next frame
                yspeed += gravity;
                if (yspeed > terminalVelocity) {
                    yspeed = terminalVelocity;
                }
            }
            
            if (Keyboard.isKeyDown(Keyboard.KEY_SPACE) && blockBelow && !noClip) {
                if (!lastSpaceState) {
                    lastSpaceState = true;
                    yspeed = -jumpSpeed;
                }
            } else {
                lastSpaceState = false;
            }
            

            // world movement
            // listen for movement keys
            if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
                dx += -speed * (float) Math.sin(Math.toRadians(camera.yaw));
                dz += speed * (float) Math.cos(Math.toRadians(camera.yaw));
            } else if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
                dx += speed * (float) Math.sin(Math.toRadians(camera.yaw));
                dz += -speed * (float) Math.cos(Math.toRadians(camera.yaw));
            }
            
            if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
                dx += -speed * (float)Math.sin(Math.toRadians(camera.yaw-90));
                dz += speed * (float)Math.cos(Math.toRadians(camera.yaw-90));
            } else if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
                dx += speed * (float)Math.sin(Math.toRadians(camera.yaw-90));
                dz += -speed * (float)Math.cos(Math.toRadians(camera.yaw-90));
            }
            
            float offsetX, offsetZ;
            offsetX = offsetZ = 0.0f;

            // player is 2 blocks tall, so we must check side collision twice on each axis
            boolean willCollideX = w.blockAt(camera.x + dx + offsetX, camera.y + playerHeight, camera.z + offsetZ);
                   willCollideX |= w.blockAt(camera.x + dx + offsetX, camera.y + playerHeight - 1.0f, camera.z + offsetZ);
                   willCollideX |= w.blockAt(camera.x + dx + offsetX, camera.y + playerHeight - 2.0f, camera.z + offsetZ);
            
            boolean willCollideZ = w.blockAt(camera.x + offsetX, camera.y + playerHeight, camera.z + dz + offsetZ);
                   willCollideZ |= w.blockAt(camera.x + offsetX, camera.y + playerHeight - 1.0f, camera.z + dz + offsetZ);
                   willCollideZ |= w.blockAt(camera.x + offsetX, camera.y + playerHeight - 2.0f, camera.z + dz + offsetZ);
                    
            if (willCollideX && !noClip) {
                float overshoot = (float) Math.floor(dx) / 2.0f;
                if (Math.abs(overshoot) > 0) {
                    camera.x = Math.round(camera.x + dx - overshoot);
                }
                dx = 0;
            }
            if (willCollideZ && !noClip) {
                float overshoot = (float) Math.floor(dz) / 2.0f;
                if (Math.abs(overshoot) > 0) {
                    camera.z = Math.round(camera.z + dz - overshoot);
                }
                dz = 0;
            }
            
            // world movement
            camera.x += dx;
            camera.z += dz;
            camera.y += dy;
            
            // look movement
            camera.incYaw(Mouse.getDX() * mouseSens);
            camera.incPitch(Mouse.getDY() * mouseSens);

            // draw frame
            screen.drawFrame();
        }
    }
    
    /**
    * method: main
    * purpose: Start the main program by instantiating a Camera, a Screen,
    * and calling run().
    **/
    public static void main(String[] args) {
        Screen s;
        try {
            Camera c = new Camera(0,0,0);
            //s = new Screen(1024, 768, "CS445Craft", c);
            s = new Screen(1024, 768, "CS445Craft", c);
            w = new World(5);
            s.addObject(w);
            
            float center = w.getWorldSize() / 2;
            c.x = -center;
            c.z = -center;
            
            run(s, c);
        } catch (LWJGLException ex) {
            Logger.getLogger(CS445Craft.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CS445Craft.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
