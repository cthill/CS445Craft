/***************************************************************
* file: Game.java
* author: CS445 Group 42^3
* class: CS 445 – Computer Graphics
*
* assignment: Final Project
* date last modified: 10/28/2017
*
* purpose: This class manages game state, interfaces with all of
* the other classes, polls for input, and handles game physics.
* 
****************************************************************/

package cs445craft;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.newdawn.slick.opengl.TextureLoader;

public class Game {
    // game constants
    public static final int RES_WIDTH = 1280;
    public static final int RES_HEIGHT = 720;
    public static final int ASYNC_TASKS_PER_FRAME = 2;
    private static final boolean DYNAMIC_WORLD_GENERATION = true;
    private static int CHUNK_GENERATION_BOUNDARY = 3;
    private static final int INITIAL_WORLD_SIZE = 4;
    
    // physics constants
    private static final float MOUSE_SENS = 0.09f;
    private static final float MOVEMENT_SPEED = .20f;
    private static final float NOCLIP_SPEED = MOVEMENT_SPEED * 10;
    private static final float GRAVITY = 0.025f;
    private static final float TERMINAL_VELOCITY = MOVEMENT_SPEED * 5;
    private static final float JUMP_SPEED = 0.40f;
    private static final float PLAYER_HEIGHT = Voxel.BLOCK_SIZE * 1.5f;
    private static final float SIDE_COLLIDE_HEIGHT_FACTOR = 0.75f;
        
    // game state variables
    private boolean noClip, lastSpaceState, lastVState, lastLeftMouseState, lastUpState, lastDownState;
    private int worldX, worldZ, chunkI, chunkJ;
    private float yspeed;
    
    // game objects
    private final Random rand;
    private final WorldGenerator worldGen;
    private final World world;
    private final Camera camera;
    private final Screen screen;
    
    // useful data structures
    private final Queue<Runnable> taskQueue;
    private final Set<Chunk> scheduledForRebuild;
    private final BlockingQueue<Chunk> ungeneratedChunkQueue, unbuiltChunkQueue, builtChunkQueue;
    private final Thread chunkGenerator, meshBuilder;
    
    public Game() throws LWJGLException, IOException {        
        // init camera and screen
        float center = INITIAL_WORLD_SIZE * Chunk.CHUNK_S * Voxel.BLOCK_SIZE / 2;
        float top = Chunk.CHUNK_H * Voxel.BLOCK_SIZE;
        camera = new Camera(center, top, center);
        screen = new Screen(RES_WIDTH, RES_HEIGHT, "CS445Craft", camera);
        
        // load texture
        TextureLoader.getTexture("png", new FileInputStream(new File("res/terrain.png")));
        
        // setup world, taskQueue, and screen
        rand = new Random();
        worldGen = new WorldGenerator(rand.nextInt(), INITIAL_WORLD_SIZE);
        world = worldGen.getOrGenerate();
        screen.addObjects(world.getChunks());
        
        // setup data structures
        taskQueue = new LinkedList<>();
        scheduledForRebuild = new HashSet<>();
        ungeneratedChunkQueue = new LinkedBlockingQueue<>();
        chunkGenerator = new ChunkGenerator(ungeneratedChunkQueue);
        unbuiltChunkQueue = new LinkedBlockingQueue<>();
        builtChunkQueue = new LinkedBlockingQueue<>();
        meshBuilder = new MeshBuilder(unbuiltChunkQueue, builtChunkQueue);
                
        init();
    }
    
    /**
    * method: init()
    * purpose: set initial values of game state variables
    **/
    private void init() {
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        Mouse.setGrabbed(true);
        chunkI = -1;
        chunkJ = -1;
    }
    
    /**
    * method: run()
    * purpose: This method is the main game loop. It is responsible for four things:
    *  1. Starting and stopping the background threads (chunkGenerator and meshBuilder).
    *  2. Performing all the per-frame operations like input polling, collision checking, etc.
    *  3. Looking for dirty chunks and adding them to a queue to be rebuilt by the meshBuilder
    **/
    public void run() {
        try {
            // start the chunkGenerator and meshBuilder background threads
            chunkGenerator.start();
            meshBuilder.start();

            // main game loop
            while(true) {
                // listen for q key or closeRequested
                if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE) || Keyboard.isKeyDown(Keyboard.KEY_Q)) {
                    screen.close();
                    break;
                }
                if (screen.getCloseRequested()) {
                    break;
                }
                
                // check if player entered a new voxel/chunk
                updateWorldPos();
                // handle mouse events
                mouseEvents();
                // handle keyboard events
                keyboardEvents();
                // do world movement and collision detection
                worldMovement();
                // check misc player status
                checkPlayerStatus();

                /*
                Poll the taskQueue for async tasks that need to be completed. We
                limit the number of async tasks that will be run per frame to
                ASYNC_TASKS_PER_FRAME so that there are no studders or lag during
                gameplay.
                */
                for (int i = 0; i < ASYNC_TASKS_PER_FRAME; i++) {
                    Runnable task = taskQueue.poll();
                    if (task == null) {
                        break;
                    }
                    task.run();
                }

                // draw one frame frame
                screen.drawFrame();

                /*
                find all chunks that are:
                  1. active (within the screen's draw distance)
                  2. generated (the WorldGenerator has filled them with voxels)
                  3. dirty (the mesh needs to be rebuilt)
                  4. not already scheduled to be rebuilt
                
                Take these chunks and add them to the unbuiltChunkQueue. This will
                cause the MeshBuilder thread to rebuild the chunk mesh in the
                background.
                */
                world.getChunks().stream().filter(chunk -> chunk.getActive() && chunk.getGenerated() && chunk.getDirty() && !scheduledForRebuild.contains(chunk)).forEach(chunk -> {
                    scheduledForRebuild.add(chunk);
                    taskQueue.add(() -> {
                        unbuiltChunkQueue.add(chunk);
                    });
                });

                /*
                Once the MeshBuilder thread is finished building the mesh, it will
                place the chunk in the builtChunkQueue. Now we need to take the
                chunk and generate an async task to copy the new mesh data into
                a VBO for OpenGL to use. We can't do this in the MeshBuilder thread
                because it dones't have a GL context.
                */
                while (!builtChunkQueue.isEmpty()) {
                    Chunk chunk = builtChunkQueue.poll();
                    taskQueue.add(() -> {
                        chunk.copyMeshToVBO();
                        scheduledForRebuild.remove(chunk);
                    });
                }
            }
        } finally {
            ((ChunkGenerator) chunkGenerator).terminate();
            ((MeshBuilder) meshBuilder).terminate();
        }
    }
    
    /**
    * method: updateWorldPos()
    * purpose: Use the camera's x,y,z position (in OpenGL space) to determine
    * if the player has entered a new voxel on the grid or a new chunk. If the
    * player has entered a new chunk, let the world generator know so it can
    * create new chunks if needed.
    **/
    private void updateWorldPos() {
        boolean gridPositionUpdated = false;
        boolean chunkPositionUpdated = false;
        
        // Check if x position in voxel grid has changed
        if (world.glCoordToVoxelGridLocation(camera.x) != worldX) {
            gridPositionUpdated = true;
            worldX = world.glCoordToVoxelGridLocation(camera.x);
        }

        // Check if z position in voxel grid has chanded
        if (world.glCoordToVoxelGridLocation(camera.z) != worldZ) {
            gridPositionUpdated = true;
            worldZ = world.glCoordToVoxelGridLocation(camera.z);
        }

        // Check if in new chunk
        if (world.voxelGridLocationToChunkNum(worldX) != chunkI) {
            chunkPositionUpdated = true;
            chunkI = world.voxelGridLocationToChunkNum(worldX);
        }

        // Check if in new chunk
        if (world.voxelGridLocationToChunkNum(worldZ) != chunkJ) {
            chunkPositionUpdated = true;
            chunkJ = world.voxelGridLocationToChunkNum(worldZ);
        }

        // If the position changed, print it out
        if (gridPositionUpdated || chunkPositionUpdated) {
            System.out.println("pos (" + worldX + "," + worldZ + ") chunk (" + chunkI + "," + chunkJ + ")");
        }

        // If the player entered a new chunk, have the world generator create new chunks (if needed)
        // Then, add the new chunks to the ungeneratedChunkQueue so they will be filled by the ChunkGenerator
        if (chunkPositionUpdated && DYNAMIC_WORLD_GENERATION) {
            List<Chunk> newChunks = worldGen.createNewChunksIfNeeded(chunkI, chunkJ, CHUNK_GENERATION_BOUNDARY, screen);
            ungeneratedChunkQueue.addAll(newChunks);
        }
    }
    
    /**
    * method: mouseEvents()
    * purpose: Handle mouse movement and click events. When a player left clicks,
    * check if there is a breakable voxel under the cursor, up to a maximum distance.
    * If so, break it.
    **/
    private void mouseEvents() {
        // mouse movement
        camera.incYaw(Mouse.getDX() * MOUSE_SENS);
        camera.incPitch(Mouse.getDY() * MOUSE_SENS);
        Mouse.setCursorPosition(0,0);
        
        // listen for left click
        if (Mouse.isButtonDown(0)) {
            if (!lastLeftMouseState) {
                lastLeftMouseState = true;

                // compute x,y,z vector of direction camera is facing
                float clickX = (float) (Math.sin(Math.toRadians(camera.yaw)) * Math.cos(Math.toRadians(-camera.pitch)));
                float clickZ = (float) (Math.cos(Math.toRadians(camera.yaw)) * Math.cos(Math.toRadians(-camera.pitch)));
                float clickY = (float)  Math.sin(Math.toRadians(-camera.pitch));

                // scan along that vector for breakable blocks
                for (float amplitude = 0.5f; amplitude <= Voxel.BLOCK_SIZE * 2; amplitude++) {
                    float projectX = camera.x + amplitude * clickX;
                    float projectZ = camera.z - amplitude * clickZ;
                    float projectY = camera.y + amplitude * clickY;

                    // Continue loop if there is no block or the block can be mined through (like water)
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
    
    /**
    * method: keyboardEvents()
    * purpose: Handle keyboard events not related to world movement.
    *  1. press 'v' to toggle noclip mode.
    *  2. press up to increase the draw distance
    *  3. press down to decrease the draw distance
    **/
    private void keyboardEvents() {
        if (Keyboard.isKeyDown(Keyboard.KEY_V)) {
            if (!lastVState) {
                lastVState = true;
                noClip = !noClip;
            }
        } else {
            lastVState = false;
        }
        
        if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {
            if (!lastUpState) {
                lastUpState = true;
                screen.incDrawDist(Chunk.CHUNK_S * Voxel.BLOCK_SIZE);
                CHUNK_GENERATION_BOUNDARY++;
            }
        } else {
            lastUpState = false;
        }
        
        if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
            if (!lastDownState) {
                lastDownState = true;
                screen.incDrawDist(-Chunk.CHUNK_S * Voxel.BLOCK_SIZE);
                CHUNK_GENERATION_BOUNDARY--;
            }
        } else {
            lastDownState = false;
        }
    }
    
    /**
    * method: worldMovement()
    * purpose: Handle world movement, including collision checking
    **/
    private void worldMovement() {
        float dx = 0.0f;
        float dz = 0.0f;
        float dy = yspeed;

        // check for blocks above and below
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
            /*
            prevent player from getting stuck in floor if they have high y speed
            by setting camera.y to the depth of the world at that point
            */
            camera.y = world.depthAt(camera.x, camera.y, camera.z) + PLAYER_HEIGHT + 0.75f;
        } else {
            if (blockAbove && yspeed > 0) {
                yspeed = 0;
            }

            // accelerate on the yaxis for the next frame
            yspeed -= GRAVITY;
        }

        // don't let player fall too fast
        if (yspeed < -TERMINAL_VELOCITY) {
            yspeed = -TERMINAL_VELOCITY;
        } else if (yspeed > TERMINAL_VELOCITY) {
            yspeed = TERMINAL_VELOCITY;
        }

        // listen for jump key
        if (Keyboard.isKeyDown(Keyboard.KEY_SPACE) && blockBelow && !noClip) {
            if (!lastSpaceState) {
                lastSpaceState = true;
                yspeed = JUMP_SPEED;
            }
        } else {
            lastSpaceState = false;
        }
        
        // increase speed if noclip is enabled
        float speed = MOVEMENT_SPEED;
        if (noClip) {
            speed = NOCLIP_SPEED;
        }
        
        // world movement keys        
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

        /*
        add an offset to the player's x and z coordinates so they can't get too
        close to the blocks.
        */
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

        // perform appropriate action if player will collide with a block and noclip is off
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
    
    /**
    * method: checkPlayerStatus()
    * purpose: Check various player status attributes like if they are underwater
    **/
    private void checkPlayerStatus() {
        Voxel.VoxelType blockAtCamera = world.blockAt(camera.x, camera.y, camera.z);
        
        if (blockAtCamera == Voxel.VoxelType.WATER) {
            // if underwater, tint the screen blue
            screen.setTintColor(0.75f, 0.75f, 1.0f);
        } else {
            screen.setTintColor(1.0f, 1.0f, 1.0f);
        }
    }
    
    
    /**
    * class: ChunkGenerator
    * purpose: This class extends the Thread class and allows us to generate chunks
    * on a background thread. This is to prevent lag/stuttering because chunk
    * generation is an expensive process.
    **/
    private class ChunkGenerator extends Thread {
        private boolean done;
        private final BlockingQueue<Chunk> ungeneratedChunkQueue;
        
        ChunkGenerator(BlockingQueue<Chunk> ungenQueue) {
            this.done = false;
            this.ungeneratedChunkQueue = ungenQueue;
        }
        
        /**
        * class: terminate()
        * purpose: Terminate this thread
        **/
        public void terminate() {
            done = true;
        }

        /**
        * class: run()
        * purpose: Run the thread. It will continually poll the ungeneratedChunkQueue
        * looking for new chunks that need to be filled with randomly generated
        * content. When it's done, it will mark the new and adjacent chunks as
        * dirty so they will be rebuilt.
        **/
        @Override
        public void run() {
            while (!done) {
                Chunk chunkToFill = ungeneratedChunkQueue.poll();
                if (chunkToFill == null) {
                    continue;
                }
                
                // generate chunk and set it dirty so the mesh will be rebuilt
                worldGen.fillChunkGenerateRandom(chunkToFill);
                chunkToFill.setDirty();
                
                world.findAllAdjacentChunks(chunkToFill).forEach(adjChunk -> {
                    // set adjacent chunks as dirty so they will be rebuild as well
                    adjChunk.setDirty();
                });
            }
        }
    }
    
    /**
    * class: MeshBuilder
    * purpose: This class extends the Thread class and allows us to build meshes
    * on a background thread. This is to prevent lag/stuttering because mesh
    * building is an expensive process.
    **/
    private class MeshBuilder extends Thread {
        private boolean done;
        private final BlockingQueue<Chunk> unbuiltMeshQueue;
        private final BlockingQueue<Chunk> builtMeshQueue;
        
        MeshBuilder(BlockingQueue<Chunk> unbuiltQueue, BlockingQueue<Chunk> builtQueue) {
            this.done = false;
            this.unbuiltMeshQueue = unbuiltQueue;
            this.builtMeshQueue = builtQueue;
        }
        
        /**
        * class: terminate()
        * purpose: Terminate this thread
        **/
        public void terminate() {
            done = true;
        }
        
        /**
        * class: run()
        * purpose: Run the thread. It will continually poll the unbuiltMeshQueue
        * looking for new meshes that need to be built. Once a mesh is built,
        * it is added to the builtMeshQueue.
        **/
        @Override
        public void run() {
            while (!done) {
                Chunk chunkToBuild = unbuiltMeshQueue.poll();
                if (chunkToBuild == null) {
                    continue;
                }
                
                chunkToBuild.rebuildMesh();
                
                builtMeshQueue.add(chunkToBuild);
            }
        }
    }
}
