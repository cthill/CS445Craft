/***************************************************************
* file: Chunk.java
* author: CS445 Group 42^3
* class: CS 445 – Computer Graphics
*
* assignment: Final Project
* date last modified: 10/28/2017
*
* purpose: This class defines one 30x30x90 chunk of voxels. The class uses a 3d
* array of the enum VoxelType to keep track of which blocks are in each cell.
* Null entries indicate empty cells. This class is also responsible for building
* a mesh and rendering that mesh using VBOs.
* 
****************************************************************/

package cs445craft;

import cs445craft.Voxel.VoxelType;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;

public class Chunk extends Drawable {
    public static final int CHUNK_S = 30;
    public static final int CHUNK_H = 90;
    public static final int NUM_BLOCKS = CHUNK_S * CHUNK_H * CHUNK_S;
    
    private final World world;
    public int indexI, indexJ;
    public float chunkX, chunkY, chunkZ;
    private final VoxelType[][][] blocks;
    
    private int numFaces, numFacesTranslucent, tempNumFaces, tempNumFacesTranslucent;
    
    private final int VBOHandle;
    private final int VBOHandleTranslucent;
    
    int writeIndex;
    int writeIndexTranslucent;
    float[] tempMeshData;
    float[] tempMeshDataTranslucent;
    
    private boolean dirty, generated, built;
    
    public Chunk(World world, int indexI, int indexJ) {
        this.world = world;
        this.indexI = indexI;
        this.indexJ = indexJ;
        
        chunkX = indexI * CHUNK_S * Voxel.BLOCK_SIZE;
        chunkZ = indexJ * CHUNK_S * Voxel.BLOCK_SIZE;
        chunkY = 0.0f;
        
        /*
        The ordering of the blocks array is [y][x][z]. The reason for doing this
        is because the WorldGenerator was easier to design if we iterate first on
        the y-axis and then on the x and z axiis. If we iterate in the same way
        the array is structured, we get better cache locality and hopefully better
        performance.
        */
        blocks = new VoxelType[CHUNK_H][CHUNK_S][CHUNK_S];

        VBOHandle = glGenBuffers();
        VBOHandleTranslucent = glGenBuffers();
    }
    
    /**
    * method: getDirty()
    * purpose: Returns the state of the dirty flag. Chunks are marked as "dirty"
    * when the mesh has been modified and needs to be rebuilt. The rebuildMesh()
    * methods clears the dirty flag.
    * 
    * The Game.java class checks for dirty chunks and adds them to a queue to be
    * rebuilt.
    **/
    public boolean getDirty() {
        return dirty;
    }
    
    /**
    * method: setDirty()
    * purpose: Mark this chunk as dirty so its mesh will be rebuilt. The chunk
    * becomes "dirty" when a block is broken or a new adjacent chunk is created
    * by the WorldGenerator.
    **/
    public void setDirty() {
        this.dirty = true;
    }
    
    /**
    * method: getGenerated()
    * purpose: Returns the state of the generated flag. The generated flag is set
    * to true when the WorldGenerator has generated and filled the contents of this
    * chunk.
    **/
    public boolean getGenerated() {
        return generated;
    }
    
    /**
    * method: setGenerated()
    * purpose: Sets the generated flag to true. This method is called by the WorldGenerator
    * after the contents of this chunk have been generated.
    * chunk.
    **/
    public void setGenerated() {
        this.generated = true;
    }
    
    
    /**
    * method: copyBlocks()
    * purpose: Copy a 3d array of VoxelType into this instance's blocks array.
    **/
    public void copyBlocks(VoxelType[][][] wBlocks, int sx, int lx, int sy, int ly, int sz, int lz) {
        if (ly > CHUNK_H)
            ly = CHUNK_H;
        if (lx > CHUNK_S)
            lx = CHUNK_S;
        if (lz > CHUNK_S)
            lz = CHUNK_S;
        
        int y = 0;
            for (int iy = sy; iy < sy + ly; iy++) {
            int x = 0;
            for (int ix = sx; ix < sx + lx; ix++) {
                int z = 0;
                for (int iz = sz; iz < sz + lz; iz++) {
                    this.blocks[y][x][z] = wBlocks[iy][ix][iz];
                    z++;
                }
                x++;
            }
            y++;
        }
    }

    /**
    * method: voxelLookupSafe()
    * purpose: Returns VoxelType in the x,y,z position in the blocks array. Does
    * bounds checking.
    **/
    private VoxelType voxelLookupSafe(int x, int y, int z) {
        if (
            y >= 0 && y < CHUNK_H &&
            x >= 0 && x < CHUNK_S &&
            z >= 0 && z < CHUNK_S
        ) {
            return blocks[y][x][z];
        }
        
        return null;
    }
    
    /**
    * method: blockAt()
    * purpose: Returns the VoxelType at a given x,y,z in the blocks array.
    * 
    **/
    public VoxelType blockAt(int x, int y, int z) {
        return voxelLookupSafe(x, y, z);
    }
    
    /**
    * method: solidBlockAt()
    * purpose: Returns the VoxelType at a given x,y,z in the blocks array. Return
    * null if the voxel at that location is not solid. Uses Voxel.isSolid() to
    * determine if the voxel is solid.
    * 
    **/
    public VoxelType solidBlockAt(int x, int y, int z) {
        VoxelType block = voxelLookupSafe(x, y, z);
        if (!Voxel.isSolid(block)) {
            return null;
        }
        return block;
    }
    
    /**
    * method: depthAt()
    * purpose: Returns the y-value of the nearest solid voxel to a given x, y, z
    * in the blocks array. Useful for collision checking.
    * 
    **/
    public int depthAt(int x, int y, int z) {
        for (int i = y; i >= 0; i--) {
            VoxelType block = voxelLookupSafe(x, i, z);
            if (block != null && Voxel.isSolid(block)) {
                return i;
            }
        }
        
        return CHUNK_H;
    }
    
    /**
    * method: traverseChunks()
    * purpose: Returns the chunk at a given x,y,z location in the blocks array.
    * traverseChunks(0,0,0) will return this chunk. traverseChunks(CHUNK_S,0,0)
    * will return the adjacent chunk in the positive x direction.
    **/
    public Chunk traverseChunks(int x, int y, int z) {
        if (y < 0 || y > CHUNK_H - 1) {
            return null;
        }
        
        int xDir = 0;
        int zDir = 0;

        if (x < 0)
            xDir = -1;
        else if (x > CHUNK_S - 1)
            xDir = 1;

        if (z < 0)
            zDir = -1;
        else if (z > CHUNK_S -1)
            zDir = 1;
        
        // not traversing chunk boundary
        if (xDir == 0 && zDir == 0) {
            return this;
        }
        
        // traversing chunk boundary, lookup adjacent chunk
        return world.findAdjacentChunk(this, xDir, zDir);
    }
    
    /**
    * method: voxelLookupTraverseChunks()
    * purpose: Lookup a VoxelType at a given x, y, z position in the blocks array.
    * Traverses chunk boundaries if out-of-bounds values are provided. Allows user
    * to provide a default value if a boundary is crossed but there is no adjacent
    * chunk.
    **/
    public VoxelType voxelLookupTraverseChunks(int x, int y, int z, VoxelType defaultIfNullChunk) {
        Chunk lookupChunk = traverseChunks(x, y, z);
        if (lookupChunk == null) {
            return defaultIfNullChunk;
        }
        
        int wrappedX = Math.floorMod(x, CHUNK_S);
        int wrappedZ = Math.floorMod(z, CHUNK_S);
        
        return lookupChunk.blocks[y][wrappedX][wrappedZ]; //voxelLookupSafe(wrappedX, y, wrappedZ);
    }
    
    /**
    * method: voxelLookupTraverseChunks()
    * purpose: Lookup a VoxelType at a given x, y, z position in the blocks array.
    * Traverses chunk boundaries if out-of-bounds values are provided. Returns null
    * if a boundary is crossed but there is no adjacent chunk.
    **/
    public VoxelType voxelLookupTraverseChunks(int x, int y, int z) {
        return voxelLookupTraverseChunks(x, y, z, null);
    }
    
    /**
    * method: breakBlock()
    * purpose: Remove a voxel at a given x, y, z position in the blocks array and
    * rebuild the mesh. If a block is broken at a chunk boundary, the adjacent chunk
    * is marked as dirty so it will also be rebuilt. If the above voxels satisfy
    * the Voxel.breakIfSupportRemoved() function, they will also be removed.
    * 
    **/
    public void breakBlock(int x, int y, int z) {
        VoxelType v = voxelLookupSafe(x, y, z);
        if (v != null) {
            // check if block above requires support (like cactus or snow)
            VoxelType above = voxelLookupSafe(x, y + 1, z);
            if (Voxel.breakIfSupportRemoved(above)) {
                breakBlock(x, y + 1, z);
            }
            
            // break block and fix the mesh
            blocks[y][x][z] = null;
            rebuildMesh();
            copyMeshToVBO();
            
            // check if breaking block at chunk boundary
            int xDir = 0;
            int zDir = 0;
            
            if (x == 0)
                xDir = -1;
            else if (x == CHUNK_S - 1)
                xDir = 1;

            if (z == 0)
                zDir = -1;
            else if (z == CHUNK_S - 1)
                zDir = 1;

            if (xDir != 0) {
                // lookup adjacent chunk and mark it as dirty so the mesh will be rebuilt
                Chunk adjacent = world.findAdjacentChunk(this, xDir, 0);
                if (adjacent != null) {
                    adjacent.setDirty();
                }
            }
            
            if (zDir != 0) {
                // lookup adjacent chunk and mark it as dirty so the mesh will be rebuilt
                Chunk adjacent = world.findAdjacentChunk(this, 0, zDir);
                if (adjacent != null) {
                    adjacent.setDirty();
                }
            }
        }
    }
    
    /**
    * method: rebuildMesh()
    * purpose: Loop over the blocks array to build a 3d mesh of the chunk to
    * render to the screen. Faces that can not be see will not be included in
    * the mesh.
    * 
    * This method builds two meshes: one for opaque voxels and another for translucent
    * voxels. This is because the screen needs to draw the opaque meshes before
    * the translucent ones.
    * 
    * Mesh data is rendered to temporary float arrays. The copyMeshToVBO() copies
    * the data out of the temporary array to create the VBOs. The reason for a two
    * step generation process is so that rebuildMesh() can be done on a background
    * thread to prevent lag/stuttering. However copyMeshToVBO() must be done on
    * the main thread because that thread has the OpenGL context.
    **/
    public void rebuildMesh() {
        // create a timer
        ThreadMXBean threadTimer = ManagementFactory.getThreadMXBean();
        long start = threadTimer.getCurrentThreadCpuTime();
        
        // reset number of faces
        tempNumFaces = 0;
        tempNumFacesTranslucent = 0;
        
        // compute number of floats to be written
        int floatsPerFacePosition = 3 * 4;
        int floatsPerFaceTexture = 2 * 4;
        int floatsPerFace = floatsPerFacePosition + floatsPerFaceTexture;        
        int totalFloats = NUM_BLOCKS * 6 * floatsPerFace;

        // create float arrays to hold data
        writeIndex = 0;
        writeIndexTranslucent = 0;
        tempMeshData = new float[totalFloats];
        tempMeshDataTranslucent = new float[totalFloats];

        // loop over each block in this chunk
        for (int y = 0; y < CHUNK_H; y++) {
            for (int x = 0; x < CHUNK_S; x++) {
                for (int z = 0; z < CHUNK_S; z++) {
                    VoxelType voxelType = blocks[y][x][z];
                    
                    // null is used for empty cells
                    if (voxelType == null) {
                        continue;
                    }
                    
                    // lookup all 6 adjacent voxels, traversing chunk boundaries if needed.
                    VoxelType above = voxelLookupTraverseChunks(x, y + 1, z, VoxelType.BEDROCK);
                    VoxelType below = voxelLookupTraverseChunks(x, y - 1, z, VoxelType.BEDROCK);
                    VoxelType front = voxelLookupTraverseChunks(x, y, z - 1, VoxelType.BEDROCK);
                    VoxelType back  = voxelLookupTraverseChunks(x, y, z + 1, VoxelType.BEDROCK);
                    VoxelType left  = voxelLookupTraverseChunks(x - 1, y, z, VoxelType.BEDROCK);
                    VoxelType right = voxelLookupTraverseChunks(x + 1, y, z, VoxelType.BEDROCK);

                    // compute faces that can not be seen
                    boolean[] faceVisible = new boolean[6];
                    faceVisible[Voxel.FACE_TOP]    = shouldDrawFace(voxelType, above);
                    //the extra & y > 0 is here so we don't draw the bottom faces of the world's bottom voxels
                    faceVisible[Voxel.FACE_BOTTOM] = y > 0 && shouldDrawFace(voxelType, below);
                    faceVisible[Voxel.FACE_FRONT]  = shouldDrawFace(voxelType, front);
                    faceVisible[Voxel.FACE_BACK]   = shouldDrawFace(voxelType, back);
                    faceVisible[Voxel.FACE_LEFT]   = shouldDrawFace(voxelType, left);
                    faceVisible[Voxel.FACE_RIGHT]  = shouldDrawFace(voxelType, right);

                    // don't draw the top and bottom faces of cross type objects
                    faceVisible[Voxel.FACE_BOTTOM] &= !Voxel.isCrossType(voxelType);
                    faceVisible[Voxel.FACE_TOP] &= !Voxel.isCrossType(voxelType);

                    // check if texture is translucent (like water or glass)
                    boolean translucentTexture = Voxel.isTranslucent(voxelType);
                    
                    // translate x,y,z indices to OpenGL coordinates
                    float glX = (float) (x * Voxel.BLOCK_SIZE);
                    float glY = (float) (y * Voxel.BLOCK_SIZE);
                    float glZ = (float) (z * Voxel.BLOCK_SIZE);
                    
                    // loop over the faces and write them to the buffers
                    for (int face = 0; face < 6; face++) {
                        if (faceVisible[face]) {
                            if (translucentTexture) {
                                // write to the translucent buffer
                                Voxel.writeFaceVertices(tempMeshDataTranslucent, writeIndexTranslucent, face, voxelType, glX, glY, glZ);
                                writeIndexTranslucent += floatsPerFace;
                                tempNumFacesTranslucent++;
                            } else {
                                // write to the opaque buffer
                                Voxel.writeFaceVertices(tempMeshData, writeIndex, face, voxelType, glX, glY, glZ);
                                writeIndex += floatsPerFace;
                                tempNumFaces++;
                            }
                        }
                    }
                }
            }
            // yeild after each vertical layer so that mesh building doesn't cause stuttering
            Thread.yield();
        }
        
        // print out how long it took to build the mesh
        System.out.println("MeshBuild " + indexI + "," + indexJ + " in " + (threadTimer.getCurrentThreadCpuTime() - start) / 1000000000.0);
    }
    
    /**
    * method: copyMeshToVBO()
    * purpose: Copy the data in the tempMeshData and tempMeshDataTranslucent arrays
    * to VBOs. This must be done only after rebuildMesh() is called.
    **/
    public void copyMeshToVBO() {
        // make sure rebuildMesh() was called first.
        if (tempMeshData == null || tempMeshDataTranslucent == null) {
            return;
        }
        
        // create timer
        ThreadMXBean threadTimer = ManagementFactory.getThreadMXBean();
        long start = threadTimer.getCurrentThreadCpuTime();
        
        // create FloatBuffers
        FloatBuffer vertexVBO = BufferUtils.createFloatBuffer(writeIndex);
        FloatBuffer vertexTranslucentVBO = BufferUtils.createFloatBuffer(writeIndexTranslucent);
        
        // copy float data to FloatBuffers
        ((FloatBuffer) vertexVBO.clear()).put(tempMeshData, 0, writeIndex).flip();
        ((FloatBuffer) vertexTranslucentVBO.clear()).put(tempMeshDataTranslucent, 0, writeIndexTranslucent).flip();
        
        // Load FloatBuffers data into VBOs
        glBindBuffer(GL_ARRAY_BUFFER, VBOHandle);
        glBufferData(GL_ARRAY_BUFFER, vertexVBO, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, VBOHandleTranslucent);
        glBufferData(GL_ARRAY_BUFFER, vertexTranslucentVBO, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        
        // copy over number of faces
        numFaces = tempNumFaces;
        numFacesTranslucent = tempNumFacesTranslucent;
        
        // clear out temp buffers
        tempMeshData = null;
        tempMeshDataTranslucent = null;
        
        // print how long it took
        System.out.println("MeshVBOCopy " + indexI + "," + indexJ + " in " + (threadTimer.getCurrentThreadCpuTime() - start) / 1000000000.0);
        
        // set the flags
        dirty = false;
        built = true;
    }
    
    /**
    * method: shouldDrawFace()
    * purpose: Examines two adjacent VoxelTypes and determines if the
    * mating face of the fist Voxel should be drawn.
    **/
    private boolean shouldDrawFace(VoxelType v, VoxelType adjacent) {
        if (adjacent == null) {
            return true;
        }
        
        if (Voxel.isPartiallyTransparent(v) || Voxel.isPartiallyTransparent(adjacent)) {
            return true;
        }
        
        if (Voxel.isTranslucent(adjacent) && v != adjacent) {
            return true;
        }
        
        return false;
    }
    
    /**
    * method: draw()
    * purpose: Draw the pre-built textured mesh of opaque voxels to the screen.
    **/
    @Override
    public void draw() {
        drawVBO(VBOHandle, numFaces);
    }
    
    /**
    * method: drawTranslucent()
    * purpose: Draw the pre-built textured mesh of translucent voxels to the screen.
    **/
    @Override
    public void drawTranslucent() {
        drawVBO(VBOHandleTranslucent, numFacesTranslucent);
    }
    
    /**
    * method: drawVBO()
    * purpose: Draw a given VBO to the screen.
    **/
    private void drawVBO(int VBO, int faces) {
        if (!built) {
            return;
        }
        
        glEnableClientState(GL_VERTEX_ARRAY);
        glEnableClientState (GL_TEXTURE_COORD_ARRAY);
        glPushMatrix();
        glTranslatef(chunkX, chunkY, chunkZ);
        
        glBindBuffer(GL_ARRAY_BUFFER, VBO);
        glBindTexture(GL_TEXTURE_2D, 1);
        
        // Using interleved VBO for better performance
        // (V,V,V,T,T)
        int stride = 5 * 4;
        glVertexPointer(3, GL_FLOAT, stride, 0);
        glTexCoordPointer(2, GL_FLOAT, stride, 3 * 4);

        glDrawArrays(GL_QUADS, 0, faces * 4);
        
        glPopMatrix();
        glDisableClientState(GL_VERTEX_ARRAY);
        glDisableClientState (GL_TEXTURE_COORD_ARRAY);
    }
    
    /**
    * method: distanceTo()
    * purpose: Calculate the 3d distance from the center of this chunk to some given
    * point. These coordinates are in OpenGL space, relative to the OpenGl origin.
    **/
    @Override
    public float distanceTo(float x, float y, float z) {
        return (float) Math.sqrt(Math.pow(chunkX + CHUNK_S - x, 2) + Math.pow(chunkY + CHUNK_H - y, 2) + Math.pow(chunkZ + CHUNK_S - z, 2));
    }
    
    /**
    * method: getX()
    * purpose: Get the x coordinate of this chunk, in OpenGL space, relative to
    * the OpenGl origin.
    **/
    @Override
    public float getX() {
        return chunkX;
    }
    
    
    /**
    * method: getY()
    * purpose: Get the y coordinate of this chunk, in OpenGL space, relative to
    * the OpenGl origin.
    **/
    @Override
    public float getY() {
        return chunkY;
    }
    
    /**
    * method: getZ()
    * purpose: Get the z coordinate of this chunk, in OpenGL space, relative to
    * the OpenGl origin.
    **/
    @Override
    public float getZ() {
        return chunkZ;
    }
}
