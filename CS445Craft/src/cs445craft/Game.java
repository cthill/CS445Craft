package cs445craft;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.newdawn.slick.opengl.TextureLoader;

public class Game {
    // resolution
    public static final int RES_WIDTH = 1024;
    public static final int RES_HEIGHT = 768;
    
    // game constants
    private static final float MOUSE_SENS = 0.09f;
    private static final float MOVEMENT_SPEED = .20f;
    private static final float NOCLIP_SPEED = MOVEMENT_SPEED * 5;
    private static final float GRAVITY = 0.025f;
    private static final float TERMINAL_VELOCITY = MOVEMENT_SPEED * 5;
    private static final float JUMP_SPEED = 0.40f;
    // player is 1.5 blocks tall, so yOffset = Chunk.CUBE_S * 1.5;
    private static final float PLAYER_HEIGHT = Voxel.BLOCK_SIZE * 1.5f;
    private static final float SIDE_COLLIDE_HEIGHT_FACTOR = 0.75f;
        
    // game variables
    private boolean noClip, lastSpaceState, lastVState, lastRState, lastLeftMouseState;
    private int worldX, worldZ, chunkI, chunkJ;
    private float yspeed;
    
    private final Random rand;
    private final TaskQueue taskQueue;
    private final WorldGenerator worldGen;
    private final World world;
    private final Camera camera;
    private final Screen screen;
    
    public Game() throws LWJGLException, IOException {        
        rand = new Random();
        int initialWorldSize = 3;
        
        // init camera and screen
        float center = initialWorldSize * Chunk.CHUNK_S * Voxel.BLOCK_SIZE / 2;
        float top = Chunk.CHUNK_H * Voxel.BLOCK_SIZE;
        camera = new Camera(center, top, center);
        screen = new Screen(RES_WIDTH, RES_HEIGHT, "CS445Craft", camera);
        
        // load texture
        TextureLoader.getTexture("png", new FileInputStream(new File("res/terrain.png")));
        
        // setup world and taskQueue
        worldGen = new WorldGenerator(rand.nextInt(), initialWorldSize);
        world = worldGen.getOrGenerate();
        taskQueue = new TaskQueue();
        
        // build the meshes and add the chunks to the screen
        world.getChunks().forEach(chunk -> {
           chunk.rebuildMesh();
        });
        screen.addObjects(world.getChunks());
        
        init();
    }
    
    private void init() {
        chunkI = -1;
        chunkJ = -1;
    }
    
    public void run() {
        Mouse.setGrabbed(true);
        
        while(true) {
            // listen for q key or closeRequested
            if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE) || Keyboard.isKeyDown(Keyboard.KEY_Q)) {
                screen.close();
                break;
            }
            if (screen.getCloseRequested()) {
                break;
            }
            
            // move mouse to center
            Mouse.setCursorPosition(0,0);
            
            // update world position
            updateWorldPos();
            
            // handle mouse events (mouse movement & clicks)
            mouseEvents();
            
            // handle all keyboard events
            keyboardEvents();

            
            // check if underwater
            checkPlayerStatus();

            // run one async task
            taskQueue.run(1);
            
            // draw frame
            screen.drawFrame();
        }
    }
    
    private void updateWorldPos() {
        boolean gridPositionUpdated = false;
        boolean chunkPositionUpdated = false;
        if (world.worldPosToBlockIndex(camera.x) != worldX) {
            gridPositionUpdated = true;
            worldX = world.worldPosToBlockIndex(camera.x);
        }

        if (world.worldPosToBlockIndex(camera.z) != worldZ) {
            gridPositionUpdated = true;
            worldZ = world.worldPosToBlockIndex(camera.z);
        }

        if (world.blockIndexToChunkNum(worldX) != chunkI) {
            chunkPositionUpdated = true;
            chunkI = world.blockIndexToChunkNum(worldX);
        }

        if (world.blockIndexToChunkNum(worldZ) != chunkJ) {
            chunkPositionUpdated = true;
            chunkJ = world.blockIndexToChunkNum(worldZ);
        }

        if (gridPositionUpdated || chunkPositionUpdated) {
            System.out.println("pos (" + worldX + "," + worldZ + ") chunk (" + chunkI + "," + chunkJ + ")");
        }

        if (chunkPositionUpdated) {
            List<Runnable> chunkGenerationTasks = worldGen.newChunkPosition(chunkI, chunkJ, screen);
            taskQueue.addTasks(chunkGenerationTasks);
        }
    }
    
    private void mouseEvents() {
        // look movement
        camera.incYaw(Mouse.getDX() * MOUSE_SENS);
        camera.incPitch(Mouse.getDY() * MOUSE_SENS);
        
        // listen for left click
        if (Mouse.isButtonDown(0)) {
            if (!lastLeftMouseState) {
                lastLeftMouseState = true;

                float clickX = (float) (Math.sin(Math.toRadians(camera.yaw)) * Math.cos(Math.toRadians(-camera.pitch)));
                float clickZ = (float) (Math.cos(Math.toRadians(camera.yaw)) * Math.cos(Math.toRadians(-camera.pitch)));
                float clickY = (float)  Math.sin(Math.toRadians(-camera.pitch));

                for (float amplitude = 0.5f; amplitude <= Voxel.BLOCK_SIZE * 2; amplitude++) {
                    float projectX = camera.x + amplitude * clickX;
                    float projectZ = camera.z - amplitude * clickZ;
                    float projectY = camera.y + amplitude * clickY;

                    Voxel.VoxelType clickedBlock = world.blockAt(projectX, projectY, projectZ);
                    if (clickedBlock == null || Voxel.isMineThrough(clickedBlock)) {
                        continue;
                    }

                    // check if block can be broken
                    if (Voxel.isBreakable(clickedBlock)) {
                        world.removeBlock(projectX, projectY, projectZ);
                        break;
                    } else {
                        break;
                    }
                } 
            }
        } else {
            lastLeftMouseState = false;
        }
    }
    
    private void keyboardEvents() {
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
                world.swapMeshes();
            }
        } else {
            lastRState = false;
        }

        worldMovement();
    }
    
    private void worldMovement() {
        float dx = 0.0f;
        float dz = 0.0f;
        float dy = yspeed;

        // gravity and jumping
        // listen for jump
        boolean blockBelow = world.solidBlockAt(camera.x, camera.y + dy - PLAYER_HEIGHT, camera.z) != null;
        boolean blockAbove = world.solidBlockAt(camera.x, camera.y + dy + 0.5f, camera.z) != null;

        if (noClip) {
            // skip gravity code if noclip is enabled
            if (Keyboard.isKeyDown(Keyboard.KEY_SPACE))
                camera.y += NOCLIP_SPEED;
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT))
                camera.y -= NOCLIP_SPEED;
            dy = 0;
            yspeed = 0;
        }  else if (blockBelow) {
            yspeed = 0;
            dy = 0;
            // prevent player from getting stuck in floor if they have high y speed
            camera.y = world.depthAt(camera.x, camera.y, camera.z) + PLAYER_HEIGHT + 0.75f;
            //camera.y = Math.round(camera.y - 0.5f);
        } else {
            if (blockAbove && yspeed > 0) {
                yspeed = 0;
            }

            // accelerate on the yaxis for the next frame
            yspeed -= GRAVITY;
        }

        if (yspeed < -TERMINAL_VELOCITY) {
            yspeed = -TERMINAL_VELOCITY;
        } else if (yspeed > TERMINAL_VELOCITY) {
            yspeed = TERMINAL_VELOCITY;
        }

        if (Keyboard.isKeyDown(Keyboard.KEY_SPACE) && blockBelow && !noClip) {
            if (!lastSpaceState) {
                lastSpaceState = true;
                yspeed = JUMP_SPEED;
            }
        } else {
            lastSpaceState = false;
        }
        
        float speed = MOVEMENT_SPEED;
        if (noClip) {
            speed = NOCLIP_SPEED;
        }
        
        // world movement            
        if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
            dx += speed * (float) Math.sin(Math.toRadians(camera.yaw));
            dz += -speed * (float) Math.cos(Math.toRadians(camera.yaw));
        } else if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
            dx += -speed * (float) Math.sin(Math.toRadians(camera.yaw));
            dz += speed * (float) Math.cos(Math.toRadians(camera.yaw));
        }

        if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
            dx += speed * (float)Math.sin(Math.toRadians(camera.yaw-90));
            dz += -speed * (float)Math.cos(Math.toRadians(camera.yaw-90));
        } else if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
            dx += -speed * (float)Math.sin(Math.toRadians(camera.yaw-90));
            dz += speed * (float)Math.cos(Math.toRadians(camera.yaw-90));
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
        float offsetY = PLAYER_HEIGHT * SIDE_COLLIDE_HEIGHT_FACTOR;
        boolean willCollideX = world.solidBlockAt(camera.x + dx + offsetX, camera.y - offsetY, camera.z) != null;
               willCollideX |= world.solidBlockAt(camera.x + dx + offsetX, camera.y - offsetY + Voxel.BLOCK_SIZE, camera.z) != null;
        boolean willCollideZ = world.solidBlockAt(camera.x, camera.y - offsetY, camera.z + dz + offsetZ) != null;
               willCollideZ |= world.solidBlockAt(camera.x, camera.y - offsetY + Voxel.BLOCK_SIZE, camera.z + dz + offsetZ) != null;

        // special case for when running directly into a corner
        boolean willCollideXZ = world.solidBlockAt(camera.x + dx + offsetX, camera.y - offsetY, camera.z + dz + offsetZ) != null;
               willCollideXZ |= world.solidBlockAt(camera.x + dx + offsetX, camera.y - offsetY + Voxel.BLOCK_SIZE, camera.z + dz + offsetZ) != null;

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
    }
    
    private void checkPlayerStatus() {
        Voxel.VoxelType blockAtCamera = world.blockAt(camera.x, camera.y, camera.z);
        
        // check if underwater
        if (blockAtCamera == Voxel.VoxelType.WATER) {
            screen.setTintColor(0.75f, 0.75f, 1.0f);
        } else {
            screen.setTintColor(1.0f, 1.0f, 1.0f);
        }
    }
}
