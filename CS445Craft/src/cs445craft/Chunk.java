/***************************************************************
* file: Chunk.java
* author: CS445 Group 42^3
* class: CS 445 – Computer Graphics
*
* assignment: Final Project
* date last modified: 10/16/2017
*
* purpose: This class defines one 30x30x30 chunk of voxels. The
* contents of the chunk are randomly generated using simplex noise.
* The class uses a 3d array of the enum VoxelType to keep track of
* which blocks are in each cell. null entries indicate empty cells.
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
    public static final int CHUNK_S = 16;
    public static final int CHUNK_H = 90;
    public static final int NUM_BLOCKS = CHUNK_S * CHUNK_H * CHUNK_S;
    
    private final World world;
    public int indexI, indexJ;
    public float chunkX, chunkY, chunkZ;
    private final VoxelType[][][] blocks;
    
    private int numFaces;
    private int numFacesTranslucent;
    
    private final int VBOHandle;
    private final int VBOHandleTranslucent;
    
    private boolean dirty, generated, built;
    
    public Chunk(World world, int indexI, int indexJ) {
        this.world = world;
        this.indexI = indexI;
        this.indexJ = indexJ;
        
        chunkX = indexI * CHUNK_S * Voxel.BLOCK_SIZE;
        chunkZ = indexJ * CHUNK_S * Voxel.BLOCK_SIZE;
        chunkY = 0.0f;
        
        // ordering: y, x, z
        blocks = new VoxelType[CHUNK_H][CHUNK_S][CHUNK_S];

        VBOHandle = glGenBuffers();
        VBOHandleTranslucent = glGenBuffers();
    }
    
    public boolean getDirty() {
        return dirty;
    }
    
    public void setDirty() {
        this.dirty = true;
    }
    
    public boolean getGenerated() {
        return generated;
    }
    
    public void setGenerated() {
        this.generated = true;
    }
    
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
    
    public VoxelType blockAt(int x, int y, int z) {
        return voxelLookupSafe(x, y, z);
    }
    
    public VoxelType solidBlockAt(int x, int y, int z) {
        VoxelType block = voxelLookupSafe(x, y, z);
        if (!Voxel.isSolid(block)) {
            return null;
        }
        return block;
    }
    
    public int depthAt(int x, int y, int z) {
        for (int i = y; i >= 0; i--) {
            VoxelType block = voxelLookupSafe(x, i, z);
            if (block != null && Voxel.isSolid(block)) {
                return i;
            }
        }
        
        return CHUNK_H;
    }
    
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
    
    public VoxelType voxelLookupTraverseChunks(int x, int y, int z, VoxelType defaultIfNull) {
        Chunk lookupChunk = traverseChunks(x, y, z);
        if (lookupChunk == null) {
            return defaultIfNull;
        }
        
        int wrappedX = Math.floorMod(x, CHUNK_S);
        int wrappedZ = Math.floorMod(z, CHUNK_S);
        
        return lookupChunk.blocks[y][wrappedX][wrappedZ]; //voxelLookupSafe(wrappedX, y, wrappedZ);
    }
    
    public VoxelType voxelLookupTraverseChunks(int x, int y, int z) {
        return voxelLookupTraverseChunks(x, y, z, null);
    }
    
    

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
                    //adjacent.setDirty();
                    adjacent.rebuildMesh();
                }
            }
            
            if (zDir != 0) {
                // lookup adjacent chunk and mark it as dirty so the mesh will be rebuilt
                Chunk adjacent = world.findAdjacentChunk(this, 0, zDir);
                if (adjacent != null) {
                    //adjacent.setDirty();
                    adjacent.rebuildMesh();
                }
            }
        }
    }
    
    /**
    * method: rebuildMesh()
    * purpose: Loop over the blocks array to build a 3d mesh of the chunk to
    * render to the screen. Faces that can not be see will not be included in
    * the mesh.
    **/
    public void rebuildMesh() {
        ThreadMXBean threadTimer = ManagementFactory.getThreadMXBean();
        long start = threadTimer.getCurrentThreadCpuTime();
        
        // reset number of faces
        numFaces = 0;
        numFacesTranslucent = 0;
        
        // compute number of floats to be written
        int floatsPerFacePosition = 3 * 4;
        int floatsPerFaceTexture = 2 * 4;
        int floatsPerFace = floatsPerFacePosition + floatsPerFaceTexture;        
        int totalFloats = NUM_BLOCKS * 6 * floatsPerFace;

        // create float arrays to hold data
        int writeIndex = 0;
        int writeIndexTranslucent = 0;
        float[] tempMeshData = new float[totalFloats];
        float[] tempMeshDataTranslucent = new float[totalFloats];

        // loop over each block in this chunk
        for (int y = 0; y < CHUNK_H; y++) {
            for (int x = 0; x < CHUNK_S; x++) {
                for (int z = 0; z < CHUNK_S; z++) {
                    VoxelType voxelType = blocks[y][x][z];
                    
                    // null is used for empty cells
                    if (voxelType == null) {
                        continue;
                    }
                    
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
                    
                    // loop over the faces and write them to the buffers
                    for (int face = 0; face < 6; face++) {
                        if (faceVisible[face]) {
                            if (translucentTexture) {
                                Voxel.writeFaceVertices(tempMeshDataTranslucent, writeIndexTranslucent, face, voxelType, x, y, z);
                                writeIndexTranslucent += floatsPerFace;
                                numFacesTranslucent++;
                            } else {
                                Voxel.writeFaceVertices(tempMeshData, writeIndex, face, voxelType, x, y, z);
                                writeIndex += floatsPerFace;
                                numFaces++;
                            }
                        }
                    }
                }
            }
        }

        // create VBOs
        FloatBuffer vertexVBO = BufferUtils.createFloatBuffer(writeIndex);
        FloatBuffer vertexTranslucentVBO = BufferUtils.createFloatBuffer(writeIndexTranslucent);
        
        // copy float data to VBOs
        ((FloatBuffer) vertexVBO.clear()).put(tempMeshData, 0, writeIndex).flip();
        ((FloatBuffer) vertexTranslucentVBO.clear()).put(tempMeshDataTranslucent, 0, writeIndexTranslucent).flip();
        
        // Bind the VBOs
        glBindBuffer(GL_ARRAY_BUFFER, VBOHandle);
        glBufferData(GL_ARRAY_BUFFER, vertexVBO, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, VBOHandleTranslucent);
        glBufferData(GL_ARRAY_BUFFER, vertexTranslucentVBO, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        
        // set the flags
        dirty = false;
        built = true;
        
        System.out.println("Mesh " + indexI + "," + indexJ + " in " + (threadTimer.getCurrentThreadCpuTime() - start) / 1000000000.0);
    }
    
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
    * purpose: Use vertex buffers to draw the pre-built textured mesh to the
    * screen.
    **/
    @Override
    public void draw() {
        drawVBO(VBOHandle, numFaces);
    }
    
    @Override
    public void drawTranslucent() {
        drawVBO(VBOHandleTranslucent, numFacesTranslucent);
    }
    
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
    
    @Override
    public float distanceTo(float x, float y, float z) {
        return (float) Math.sqrt(Math.pow(chunkX + CHUNK_S - x, 2) + Math.pow(chunkY + CHUNK_H - y, 2) + Math.pow(chunkZ + CHUNK_S - z, 2));
    }
    
    @Override
    public float getX() {
        return chunkX;
    }
    
    @Override
    public float getY() {
        return chunkY;
    }
    
    @Override
    public float getZ() {
        return chunkZ;
    }
}
