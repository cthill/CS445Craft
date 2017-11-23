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
import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;

public class Chunk extends Drawable {
    public static final int CHUNK_S = 16;
    public static final int CHUNK_H = 90;
    public static final int NUM_BLOCKS = CHUNK_S * CHUNK_H * CHUNK_S;
    
    private World world;
    public int i, j;
    public float chunkX, chunkY, chunkZ;
    private VoxelType[][][] blocks;
    
    private boolean excludeHidden;
    private int numVisibleFaces;
    private int numVisibleFacesTranslucent;
    
    private int VBOVertexHandle;
    private int VBOTextureHandle;
    private int VBOVertexHandleTranslucent;
    private int VBOTextureHandleTranslucent;
    
    public Chunk(World world, int i, int j) {
        this.world = world;
        this.i = i;
        this.j = j;
        
        chunkX = i * CHUNK_S * Voxel.BLOCK_SIZE;
        chunkZ = j * CHUNK_S * Voxel.BLOCK_SIZE;
        chunkY = 0.0f;
        
        blocks = new VoxelType[CHUNK_S][CHUNK_H][CHUNK_S];
        
        excludeHidden = true;
    }
    
    public void copyBlocks(VoxelType[][][] wBlocks, int sx, int lx, int sy, int ly, int sz, int lz) {
        if (lx > CHUNK_S)
            lx = CHUNK_S;
        if (ly > CHUNK_H)
            ly = CHUNK_H;
        if (lz > CHUNK_S)
            lz = CHUNK_S;
        
        int x = 0;
        for (int ix = sx; ix < sx + lx; ix++) {
            int y = 0;
            for (int iy = sy; iy < sy + ly; iy++) {
                int z = 0;
                for (int iz = sz; iz < sz + lz; iz++) {
                    this.blocks[x][y][z] = wBlocks[ix][iy][iz];
                    z++;
                }
                y++;
            }
            x++;
        }
    }
        
    public void removeBlock(int x, int y, int z) {
        VoxelType v = safeLookup(x, y, z);
        if (v != null) {
            // break block
            blocks[x][y][z] = null;
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
                // lookup adjacent chunk and rebuild the mesh
                Chunk adjacent = world.findAdjacentChunk(this, xDir, 0);
                if (adjacent != null)
                    adjacent.rebuildMesh();
            }
            
            if (zDir != 0) {
                // lookup adjacent chunk and rebuild the mesh
                Chunk adjacent = world.findAdjacentChunk(this, 0, zDir);
                if (adjacent != null)
                    adjacent.rebuildMesh();
            }
        }
    }

    public VoxelType blockAt(int x, int y, int z) {
        return safeLookup(x, y, z);
    }
    
    public VoxelType solidBlockAt(int x, int y, int z) {
        VoxelType block = safeLookup(x, y, z);
        if (!Voxel.isSolid(block)) {
            return null;
        }
        else 
        return block;
    }
    
    public int depthAt(int x, int y, int z) {
        for (int i = y; i >= 0; i--) {
            VoxelType block = safeLookup(x, i, z);
            if (block != null && Voxel.isSolid(block)) {
                return i;
            }
        }
        
        return CHUNK_H;
    }
    
    /**
    * method: safeLookup()
    * purpose: Returns VoxelType in the x,y,z position in the blocks array. Does
    * bounds checking.
    **/
    private VoxelType safeLookup(int x, int y, int z) {
        if (
            x >= 0 && x < CHUNK_S &&
            y >= 0 && y < CHUNK_H &&
            z >= 0 && z < CHUNK_S
        ) {
            return blocks[x][y][z];
        }
        
        return null;
    }
    
    /**
    * method: draw()
    * purpose: Use vertex buffers to draw the pre-built textured mesh to the
    * screen.
    **/
    public void draw() {
        glPushMatrix();
        glTranslatef(chunkX, chunkY, chunkZ);
        glBindBuffer(GL_ARRAY_BUFFER, VBOVertexHandle);
        glVertexPointer(3, GL_FLOAT, 0, 0L);
        glBindBuffer(GL_ARRAY_BUFFER, VBOTextureHandle);
        glBindTexture(GL_TEXTURE_2D, 1);
        glTexCoordPointer(2, GL_FLOAT, 0, 0L);
        glDrawArrays(GL_QUADS, 0, numVisibleFaces * 4);
        
        glPopMatrix();
    }
    
    public void drawTranslucent() {
        glPushMatrix();
        glTranslatef(chunkX, chunkY, chunkZ);
        
        glBindBuffer(GL_ARRAY_BUFFER, VBOVertexHandleTranslucent);
        glVertexPointer(3, GL_FLOAT, 0, 0L);
        glBindBuffer(GL_ARRAY_BUFFER, VBOTextureHandleTranslucent);
        glBindTexture(GL_TEXTURE_2D, 1);
        glTexCoordPointer(2, GL_FLOAT, 0, 0L);
        glDrawArrays(GL_QUADS, 0, numVisibleFacesTranslucent * 4);
        
        glPopMatrix();
    }
    
    public float gridDistanceTo(int i, int j) {
        return (float) Math.sqrt(Math.pow(this.i - i, 2) + Math.pow(this.j - j, 2));
    }
    
    public float distanceTo(float x, float y, float z) {
        return (float) Math.sqrt(Math.pow(chunkX + CHUNK_S - x, 2) + Math.pow(chunkY + CHUNK_H - y, 2) + Math.pow(chunkZ + CHUNK_S - z, 2));
    }

    /**
    * method: swapMesh()
    * purpose: flip the excludeHidden flag and then rebuild the mesh.
    **/
    public void swapMesh() {
        excludeHidden = !excludeHidden;
        rebuildMesh();
    }
    
    public float getX() {
        return chunkX;
    }
    
    public float getY() {
        return chunkY;
    }
    
    public float getZ() {
        return chunkZ;
    }
    
    /**
    * method: rebuildMesh()
    * purpose: Loop over the blocks array to build a 3d mesh of the chunk to
    * render to the screen. If the excludeHidden flag is set, faces that can not
    * be see will not be included in the mesh.
    **/
    public void rebuildMesh() {        
        int floatsPerFacePosition = 3 * 4;
        int floatsPerFaceTexture = 2 * 4;        

        int writeIndexPosition = 0;
        int writeIndexTexture = 0;
        float[] positionData = new float[NUM_BLOCKS * 6 * floatsPerFacePosition];
        float[] textureData = new float[NUM_BLOCKS * 6 * floatsPerFaceTexture];
        
        int writeIndexPositionTranslucent = 0;
        int writeIndexTextureTranslucent = 0;
        float[] positionDataTranslucent = new float[NUM_BLOCKS * 6 * floatsPerFacePosition];
        float[] textureDataTranslucent = new float[NUM_BLOCKS * 6 * floatsPerFaceTexture];
        
        for (int x = 0; x < CHUNK_S; x++) {
            for (int z = 0; z < CHUNK_S; z++) {
                for (int y = 0; y < CHUNK_H; y++) {
                    VoxelType voxelType = blocks[x][y][z];
                    
                    // null is used for empty cells
                    if (voxelType == null) {
                        continue;
                    }
                    
                    float blockX = (float) (x * Voxel.BLOCK_SIZE);
                    float blockY = (float) (y * Voxel.BLOCK_SIZE);
                    float blockZ = (float) (z * Voxel.BLOCK_SIZE);
                    
                    // check if block is transparent
                    boolean translucentTexture = Voxel.isTranslucent(voxelType);
                    boolean partiallyTransparent = Voxel.isPartiallyTransparent(voxelType);

                    boolean[] faceVisible = new boolean[] { true, true, true, true, true, true };
                    if (excludeHidden) {
                        VoxelType above = lookupTraverseChunks(x, y + 1, z);
                        VoxelType below = lookupTraverseChunks(x, y - 1, z);
                        VoxelType front = lookupTraverseChunks(x, y, z - 1);
                        VoxelType back = lookupTraverseChunks(x, y, z + 1);
                        VoxelType left = lookupTraverseChunks(x - 1, y, z);
                        VoxelType right = lookupTraverseChunks(x + 1, y, z);
                        
                        // compute faces that can not be seen
                        faceVisible = new boolean[] {
                            shouldDrawFace(voxelType, above),
                            //the extra && y > 0 is here so we don't draw the bottom faces of the world's bottom voxels
                            shouldDrawFace(voxelType, below) && y > 0,
                            shouldDrawFace(voxelType, front),
                            shouldDrawFace(voxelType, back),
                            shouldDrawFace(voxelType, left),
                            shouldDrawFace(voxelType, right)
                        };
                    }
                    
                    // compute total number of visible faces
                    int cubeFaces = 0;
                    for (int i = 0; i < faceVisible.length; i++) {
                        if (faceVisible[i]) {
                            cubeFaces++;
                        }
                    }

                    if (cubeFaces == 0) {
                        continue;
                    }
                    
                    // write cube position vertex data and texture vertex data
                    if (!translucentTexture) {
                        writeIndexPosition += Voxel.getVertices(positionData, writeIndexPosition, faceVisible, voxelType, blockX, blockY, blockZ);
                        writeIndexTexture += Voxel.getTextureVertices(textureData, writeIndexTexture, faceVisible, voxelType);
                    } else {
                        writeIndexPositionTranslucent += Voxel.getVertices(positionDataTranslucent, writeIndexPositionTranslucent, faceVisible, voxelType, blockX, blockY, blockZ);
                        writeIndexTextureTranslucent += Voxel.getTextureVertices(textureDataTranslucent, writeIndexTextureTranslucent, faceVisible, voxelType);
                    }
                }
            }
        }
        numVisibleFaces = writeIndexPosition / floatsPerFacePosition;
        numVisibleFacesTranslucent = writeIndexPositionTranslucent / floatsPerFaceTexture;
        
        VBOVertexHandle = glGenBuffers();
        VBOTextureHandle = glGenBuffers();
        VBOVertexHandleTranslucent = glGenBuffers();
        VBOTextureHandleTranslucent = glGenBuffers();
        
        FloatBuffer VertexPosition = BufferUtils.createFloatBuffer(writeIndexPosition);
        FloatBuffer VertexTexture = BufferUtils.createFloatBuffer(writeIndexTexture);
        FloatBuffer VertexPositionTranslucent = BufferUtils.createFloatBuffer(writeIndexPositionTranslucent);
        FloatBuffer VertexTextureTranslucent = BufferUtils.createFloatBuffer(writeIndexTextureTranslucent);
        
        ((FloatBuffer) VertexPosition.clear()).put(positionData, 0, writeIndexPosition).flip();
        ((FloatBuffer) VertexTexture.clear()).put(textureData, 0, writeIndexTexture).flip();
        ((FloatBuffer) VertexPositionTranslucent.clear()).put(positionDataTranslucent, 0, writeIndexPositionTranslucent).flip();
        ((FloatBuffer) VertexTextureTranslucent.clear()).put(textureDataTranslucent, 0, writeIndexTextureTranslucent).flip();
        
        glBindBuffer(GL_ARRAY_BUFFER, VBOVertexHandle);
        glBufferData(GL_ARRAY_BUFFER, VertexPosition, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        
        glBindBuffer(GL_ARRAY_BUFFER, VBOTextureHandle);
        glBufferData(GL_ARRAY_BUFFER, VertexTexture, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        
        glBindBuffer(GL_ARRAY_BUFFER, VBOVertexHandleTranslucent);
        glBufferData(GL_ARRAY_BUFFER, VertexPositionTranslucent, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        
        glBindBuffer(GL_ARRAY_BUFFER, VBOTextureHandleTranslucent);
        glBufferData(GL_ARRAY_BUFFER, VertexTextureTranslucent, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
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
    
    public VoxelType lookupTraverseChunks(int x, int y, int z) {
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
        
        // not traversing chunk boundary, lookup block in this chunk
        if (xDir == 0 && zDir == 0) {
            return blocks[x][y][z];
        }
        
        // traversing chunk boundary, lookup adjacent chunk
        Chunk adjacentChunk = world.findAdjacentChunk(this, xDir, zDir);
        if (adjacentChunk == null) {
            // if we are the last chunk, return bedrock so we don't have to draw the side face of the chunk
            return VoxelType.BEDROCK;
        }

        // lookup block in adjacent chunk
        // reason for using Math.floorMod instead of regular mod:
        // https://stackoverflow.com/questions/4412179/best-way-to-make-javas-modulus-behave-like-it-should-with-negative-numbers/25830153#25830153
        return adjacentChunk.safeLookup(Math.floorMod(x, CHUNK_S), y, Math.floorMod(z, CHUNK_S));
    }
}
