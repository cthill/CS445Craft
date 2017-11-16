/***************************************************************
* file: CS445Craft.java
* author: CS445 Group 42^3
* class: CS 445 – Computer Graphics
*
* assignment: Final Project
* date last modified: 10/08/2017
*
* purpose: This is the main class in the program. It instantiates
* the screen, the camera, and collects keyboard/mouse input
* 
****************************************************************/
package cs445craft;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class CS445Craft {
    private static Chunk c;
    
    /**
    * method: main
    * purpose: Start the main program by instantiating a Camera, a Screen,
    * and calling run().
    **/
    public static void main(String[] args) {
        Screen s;
        try {
            Camera c = new Camera(0,0,0);
            s = new Screen(1024, 768, "CS445Craft", c);
            init(s);
            run(s, c);
        } catch (LWJGLException ex) {
            Logger.getLogger(CS445Craft.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static void init(Screen screen) {
        // TODO: allow for more chunks
        c = new Chunk(-30,0,-30);
        screen.addObject(c);
    }
    
    private static Chunk coordsToChunk(float x, float z) {
        return c;
    }
    
    /**
    * method: run
    * purpose: The main event loop of the game. Collects user input and requires
    * a Screen and Camera object.
    **/
    private static void run(Screen screen, Camera camera) {
        Mouse.setGrabbed(true);
         
        float mouseSens = 0.09f;
        float speed = .20f;
        float gravity = 0.025f;
        float terminalVelocity = speed * 5;
        float jumpSpeed = speed * 2;
        float yspeed = 0.0f;
        // player is 2 blocks tall, so yOffset = Chunk.CUBE_S * 2
        float yOffset = Chunk.CUBE_S * 2.0f;
        float yOffsetSideCollide = Chunk.CUBE_S * 1.75f;
        boolean lastSpaceState = false; // last state of the spacebar (true == pressed)
        
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
            
            
            Chunk currentChunk = coordsToChunk(camera.x, camera.z);
            float dx = 0.0f;
            float dz = 0.0f;
            float dy = 0.0f;
   
            // gravity and jumping
            // listen for jump
            boolean blockBelow = currentChunk.blockAt(camera.x, camera.y + yspeed + yOffset, camera.z);
            boolean blockAbove = currentChunk.blockAt(camera.x, camera.y + yspeed, camera.z);
            
            if (blockBelow) {
                yspeed = 0;
                dy = 0;
                // prevent player from getting stuck in floor if they have high y speed
                float depth = currentChunk.depthAt(camera.x, camera.y, camera.z);
                camera.y = Chunk.CHUNK_S * Chunk.CUBE_S - depth * Chunk.CUBE_S - yOffset - .75f;
            } else if (blockAbove) {
                yspeed = 0;
            } else {
                dy += yspeed;
                yspeed += gravity;
                if (yspeed > terminalVelocity) {
                    yspeed = terminalVelocity;
                }
            }
            
            if (Keyboard.isKeyDown(Keyboard.KEY_SPACE) && blockBelow && !lastSpaceState) {
                lastSpaceState = true;
                yspeed = -jumpSpeed;
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
            
            // player is 2 blocks tall, so we must check side collision twice on each axis
            boolean willCollideX = currentChunk.blockAt(camera.x + dx, camera.y + yOffsetSideCollide, camera.z);
            willCollideX = willCollideX || currentChunk.blockAt(camera.x + dx, camera.y, camera.z);
            
            boolean willCollideZ = currentChunk.blockAt(camera.x, camera.y + yOffsetSideCollide, camera.z + dz);
            willCollideZ = willCollideZ || currentChunk.blockAt(camera.x, camera.y, camera.z + dz);
                    
            if (willCollideX) {
                dx = 0;
            }
            if (willCollideZ) {
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
}
