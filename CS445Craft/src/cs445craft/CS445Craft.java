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
    private static Chunk c;
    
    /**
    * method: init
    * purpose: create the chunks and add them to the screen so they can be drawn.
    **/
    private static void init(Screen screen) throws IOException {
        // TODO: allow for more chunks
        c = new Chunk(-30,0,-30);
        screen.addObject(c);
    }
    
    /**
    * method: coordsToChunk
    * purpose: return the chunk a pair of xy coordinates occupies. In the future,
    * this method will support mulitple chunks.
    **/
    private static Chunk coordsToChunk(float x, float z) {
        // TODO: allow for more chunks
        return c;
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
        float speed = .20f;
        float gravity = 0.025f;
        float terminalVelocity = speed * 5;
        float jumpSpeed = speed * 2;
        float yspeed = 0.0f;
        
        // player is 2 blocks tall, so yOffset = Chunk.CUBE_S * 2
        float yOffset = Chunk.BLOCK_S * 2.0f;
        float yOffsetSideCollide = Chunk.BLOCK_S * 1.75f;
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
                    c.swapMesh();
                }
            } else {
                lastRState = false;
            }
            
            Chunk currentChunk = coordsToChunk(camera.x, camera.z);
            float dx = 0.0f;
            float dz = 0.0f;
            float dy = 0.0f;
   
            // gravity and jumping
            // listen for jump
            boolean blockBelow = currentChunk.blockAt(camera.x, camera.y + yspeed + yOffset, camera.z);
            boolean blockAbove = currentChunk.blockAt(camera.x, camera.y + yspeed, camera.z);
            
            if (noClip) {
                // skip gravity code if noclip is enabled
                if (Keyboard.isKeyDown(Keyboard.KEY_SPACE))
                    camera.y -= speed;
                if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT))
                    camera.y += speed;
                dy = 0;
                yspeed = 0;
            } else if (blockBelow) {
                yspeed = 0;
                dy = 0;
                // prevent player from getting stuck in floor if they have high y speed
                float depth = currentChunk.depthAt(camera.x, camera.y, camera.z);
                camera.y = Chunk.CHUNK_S * Chunk.BLOCK_S - depth * Chunk.BLOCK_S - yOffset - .75f;
                //camera.y = Math.round(camera.y - 0.5f);
            } else if (blockAbove) {
                yspeed = 0;
            } else {
                dy += yspeed;
                yspeed += gravity;
                if (yspeed > terminalVelocity) {
                    yspeed = terminalVelocity;
                }
            }
            
            if (Keyboard.isKeyDown(Keyboard.KEY_SPACE) && blockBelow) {
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
            boolean willCollideX = currentChunk.blockAt(camera.x + dx + offsetX, camera.y + yOffsetSideCollide, camera.z + offsetZ);
            willCollideX = willCollideX || currentChunk.blockAt(camera.x + dx + offsetX, camera.y, camera.z + offsetZ);
            
            boolean willCollideZ = currentChunk.blockAt(camera.x + offsetX, camera.y + yOffsetSideCollide, camera.z + dz + offsetZ);
            willCollideZ = willCollideZ || currentChunk.blockAt(camera.x + offsetX, camera.y, camera.z + dz + offsetZ);
                    
            if (willCollideX && !noClip) {
                dx = 0;
            }
            if (willCollideZ && !noClip) {
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
            s = new Screen(640, 480, "CS445Craft", c);
            init(s);
            run(s, c);
        } catch (LWJGLException ex) {
            Logger.getLogger(CS445Craft.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CS445Craft.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
