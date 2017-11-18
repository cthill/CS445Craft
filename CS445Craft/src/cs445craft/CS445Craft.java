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
        Mouse.setCursorPosition(0,0);
        Mouse.setGrabbed(true);
        
        boolean noClip = false;
        float noClipSpeedFactor = 5.0f;
        float mouseSens = 0.09f;
        float speed = .20f;
        float gravity = 0.025f;
        float terminalVelocity = speed * 5;
        float jumpSpeed = 0.40f;
        float yspeed = 0.0f;
        
        // player is 1.5 blocks tall, so yOffset = Chunk.CUBE_S * 1.5;
        float playerHeight = Voxel.BLOCK_SIZE * 1.5f;
        float sideCollideHeightFactor = 0.75f;
        boolean lastSpaceState = false; // last state of the spacebar (true == pressed)
        boolean lastVState = false;
        boolean lastRState = false;
        boolean lastMouseState = false;
        
        int chunki = 0;
        int chunkj = 0;
        int indexx = 0;
        int indexz = 0;
        
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
            
            if (Mouse.isButtonDown(0)) {
                if (!lastMouseState) {
                    lastMouseState = true;
                    for (float amplitude = 0.5f; amplitude <= Voxel.BLOCK_SIZE * 2; amplitude++) {
                        float clickX = camera.x - (float) (amplitude * Math.sin(Math.toRadians(camera.yaw)) * Math.cos(Math.toRadians(camera.pitch)));
                        float clickZ = camera.z + (float) (amplitude * Math.cos(Math.toRadians(camera.yaw)) * Math.cos(Math.toRadians(camera.pitch)));
                        
                        float clickY = camera.y + (float) (amplitude * Math.sin(Math.toRadians(camera.pitch)));
                        
                        if (w.blockAt(clickX, clickY, clickZ)) {
                            w.removeBlock(clickX, clickY, clickZ);
                            break;
                        }
                    } 
                }
            } else {
                lastMouseState = false;
            }
            
            boolean updated = false;
            if (w.worldPosToBlockIndex(camera.x) != indexx) {
                updated = true;
                indexx = w.worldPosToBlockIndex(camera.x);
            }
            
            if (w.worldPosToBlockIndex(camera.z) != indexz) {
                updated = true;
                indexz = w.worldPosToBlockIndex(camera.z);
            }
            
            if (w.blockIndexToChunkNum(indexx) != chunki) {
                updated = true;
                chunki = w.blockIndexToChunkNum(indexx);
            }
            
            if (w.blockIndexToChunkNum(indexz) != chunkj) {
                updated = true;
                chunkj = w.blockIndexToChunkNum(indexz);
            }
            
            if (updated) {
                System.out.println("New pos (" + indexx + "," + indexz + ") chunk (" + chunki + "," + chunkj + ")");
            }
            
            if (Keyboard.isKeyDown(Keyboard.KEY_V)) {
                if (!lastVState) {
                    lastVState = true;
                    if (!noClip) {
                        noClip = true;
                        speed *= noClipSpeedFactor;
                    } else {
                        noClip = false;
                        speed /= noClipSpeedFactor;
                    }
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
            
            float dx = 0.0f;
            float dz = 0.0f;
            
            float dy = yspeed;
   
            // gravity and jumping
            // listen for jump
            boolean blockBelow = w.solidBlockAt(camera.x, camera.y + dy + playerHeight, camera.z);
            boolean blockAbove = w.solidBlockAt(camera.x, camera.y + dy - 0.5f, camera.z);
            
            if (noClip) {
                // skip gravity code if noclip is enabled
                if (Keyboard.isKeyDown(Keyboard.KEY_SPACE))
                    camera.y -= speed;
                if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT))
                    camera.y += speed;
                dy = 0;
                yspeed = 0;
            }  else if (blockBelow) {
                yspeed = 0;
                dy = 0;
                // prevent player from getting stuck in floor if they have high y speed
                float depth = w.depthAt(camera.x, camera.y, camera.z);
                camera.y = World.CHUNK_H * Voxel.BLOCK_SIZE - depth * Voxel.BLOCK_SIZE - playerHeight - .75f;
                //camera.y = Math.round(camera.y - 0.5f);
            } else {
                if (blockAbove && yspeed < 0) {
                    yspeed = 0;
                }
                
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
            
            float offsetX, offsetZ, offsetXZ;
            offsetX = offsetZ = 0.0f;
            offsetXZ = Voxel.BLOCK_SIZE / 3.75f;
            
            if (dx > 0) {
                offsetX = offsetXZ;
            } else if (dx < 0) {
                offsetX = -offsetXZ;
            }
            
            if (dz > 0) {
                offsetZ = offsetXZ;
            } else if (dz < 0) {
                offsetZ = -offsetXZ;
            }

            // player is ~2 blocks tall, so we must check side collision twice on each axis
            float offsetY = playerHeight * sideCollideHeightFactor;
            boolean willCollideX = w.solidBlockAt(camera.x + dx + offsetX, camera.y + offsetY, camera.z);
                   willCollideX |= w.solidBlockAt(camera.x + dx + offsetX, camera.y + offsetY - Voxel.BLOCK_SIZE, camera.z);
            boolean willCollideZ = w.solidBlockAt(camera.x, camera.y + offsetY, camera.z + dz + offsetZ);
                   willCollideZ |= w.solidBlockAt(camera.x, camera.y + offsetY - Voxel.BLOCK_SIZE, camera.z + dz + offsetZ);
                   
            // special case for when running directly into a corner
            boolean willCollideXZ = w.solidBlockAt(camera.x + dx + offsetX, camera.y + offsetY, camera.z + dz + offsetZ);
                   willCollideXZ |= w.solidBlockAt(camera.x + dx + offsetX, camera.y + offsetY - Voxel.BLOCK_SIZE, camera.z + dz + offsetZ);
                   
            if (willCollideXZ && !willCollideX && !willCollideZ && !noClip) {
                dx = 0;
                dz = 0;
            }
                    
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
            s = new Screen(1024, 768, "CS445Craft", c);
            
            int worldSize = 5;
            
            w = new World(worldSize);
            Chunk[][] chunks = w.getChunks();
            for (int i = 0; i < worldSize; i++) {
                for (int j = 0; j < worldSize; j++) {
                    s.addObject(chunks[i][j]);
                }
            }
            
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
