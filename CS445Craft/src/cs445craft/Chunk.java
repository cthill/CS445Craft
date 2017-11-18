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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureLoader;

public class Chunk extends Drawable {
    public float chunkX, chunkY, chunkZ;
    private int chunkSize;
    private int chunkHeight;
    private int numBlocks;
    private VoxelType[][][] blocks;
    
    private boolean excludeHidden;
    private int numVisibleFaces;
    private int numVisibleFacesTranslucent;
    
    private Texture texture;
    private int VBOVertexHandle;
    private int VBOTextureHandle;
    private int VBOVertexHandleTranslucent;
    private int VBOTextureHandleTranslucent;
    
    public Chunk(float chunkX, float chunkY, float chunkZ, int chunkSize, int chunkHeight) throws IOException {
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;
        this.chunkSize = chunkSize;
        this.chunkHeight = chunkHeight;
        this.numBlocks = chunkSize * chunkHeight * chunkSize;
        excludeHidden = true;
        
        blocks = new VoxelType[chunkSize][chunkHeight][chunkSize];
        texture = TextureLoader.getTexture("png", new FileInputStream(new File("res/terrain.png")));
    }
    
    public void copyBlocks(VoxelType[][][] wBlocks, int sx, int lx, int sy, int ly, int sz, int lz) {
        if (lx > chunkSize)
            lx = chunkSize;
        if (ly > chunkHeight)
            ly = chunkHeight;
        if (lz > chunkSize)
            lz = chunkSize;
        
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
        if (v != null && Voxel.isBreakable(v)) {
            blocks[x][y][z] = null;
        }
    }

    public boolean blockAt(int x, int y, int z) {
        VoxelType block = safeLookup(x, y, z);
        return block != null;
    }
    
    public boolean solidBlockAt(int x, int y, int z) {
        VoxelType block = safeLookup(x, y, z);
        return block != null && Voxel.isSolid(block); 
    }
    
    public float depthAt(int x, int y, int z) {
        for (int i = y; i >= 0; i--) {
            VoxelType block = safeLookup(x, i, z);
            if (block != null && Voxel.isSolid(block)) {
                return i;
            }
        }
        
        return chunkHeight;
    }
    
    /**
    * method: safeLookup()
    * purpose: Returns VoxelType in the x,y,z position in the blocks array. Does
    * bounds checking.
    **/
    private VoxelType safeLookup(int x, int y, int z) {
        if (
            x >= 0 && x < chunkSize &&
            y >= 0 && y < chunkHeight &&
            z >= 0 && z < chunkSize
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
        glBindBuffer(GL_ARRAY_BUFFER, VBOVertexHandle);
        glVertexPointer(3, GL_FLOAT, 0, 0L);
        glBindBuffer(GL_ARRAY_BUFFER, VBOTextureHandle);
        glBindTexture(GL_TEXTURE_2D, 1);
        glTexCoordPointer(2, GL_FLOAT, 0, 0L);
        glDrawArrays(GL_QUADS, 0, numVisibleFaces * 4);
    }
    
    public void drawTranslucent() {
        glBindBuffer(GL_ARRAY_BUFFER, VBOVertexHandleTranslucent);
        glVertexPointer(3, GL_FLOAT, 0, 0L);
        glBindBuffer(GL_ARRAY_BUFFER, VBOTextureHandleTranslucent);
        glBindTexture(GL_TEXTURE_2D, 1);
        glTexCoordPointer(2, GL_FLOAT, 0, 0L);
        glDrawArrays(GL_QUADS, 0, numVisibleFacesTranslucent * 4);
    }
    
    public float distanceTo(float x, float y, float z) {
        return (float) Math.sqrt(Math.pow(chunkX - x, 2) + Math.pow(chunkY - y, 2) + Math.pow(chunkZ - z, 2));
    }

    /**
    * method: swapMesh()
    * purpose: flip the excludeHidden flag and then rebuild the mesh.
    **/
    public void swapMesh() {
        excludeHidden = !excludeHidden;
        rebuildMesh();
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
        float[] positionData = new float[numBlocks * 6 * floatsPerFacePosition];
        float[] textureData = new float[numBlocks * 6 * floatsPerFaceTexture];
        
        int writeIndexPositionTranslucent = 0;
        int writeIndexTextureTranslucent = 0;
        float[] positionDataTranslucent = new float[numBlocks * 6 * floatsPerFacePosition];
        float[] textureDataTranslucent = new float[numBlocks * 6 * floatsPerFaceTexture];

        int totalFaces = 0;
        int totalFacesTranslucent = 0;
        
        for (int x = 0; x < chunkSize; x++) {
            for (int z = 0; z < chunkSize; z++) {
                for (int y = 0; y < chunkHeight; y++) {
                    VoxelType voxelType = blocks[x][y][z];
                    
                    // null is used for empty cells
                    if (voxelType == null) {
                        continue;
                    }
                    
                    if (Voxel.isCrossType(voxelType)) {
                        float blockX = (float) (chunkX + x * Voxel.BLOCK_SIZE);
                        float blockY = (float) (y * Voxel.BLOCK_SIZE - chunkHeight * Voxel.BLOCK_SIZE);
                        float blockZ = (float) (chunkZ + z * Voxel.BLOCK_SIZE);
                        
                        createCross(positionData, writeIndexPosition, blockX, blockY, blockZ);
                        writeIndexPosition += floatsPerFacePosition * 4;
                        
                        textureCross(textureData, writeIndexTexture, voxelType);
                        writeIndexTexture += floatsPerFaceTexture * 4;
                        
                        totalFaces += 4;
                        continue;
                    }
                    
                    // check if block is transparent
                    boolean translucentTexture = Voxel.isTranslucent(voxelType);

                    boolean[] faceVisible = new boolean[] { true, true, true, true, true, true };
                    if (excludeHidden) {
                        // compute faces that can not be seen
                        faceVisible = new boolean[] {
                            // top & bottom
                            !(y < chunkHeight - 1 && blocks[x][y+1][z] != null),
                            !(y > 0 && blocks[x][y-1][z] != null),
                            // front & back
                            !(z > 0 && blocks[x][y][z-1] != null),
                            !(z < chunkSize - 1 && blocks[x][y][z+1] != null),
                            // left & right
                            !(x > 0 && blocks[x-1][y][z] != null),
                            !(x < chunkSize - 1 && blocks[x+1][y][z] != null)
                        };
                        
                        if (!translucentTexture) {
                            if (y < chunkHeight - 1)
                                faceVisible[0] |= Voxel.isTranslucent(blocks[x][y+1][z]) || Voxel.isPartiallyTransparent(blocks[x][y+1][z]);
                            if (y > 0)
                                faceVisible[1] |= Voxel.isTranslucent(blocks[x][y-1][z]) || Voxel.isPartiallyTransparent(blocks[x][y-1][z]);
                            if (z > 0)
                                faceVisible[2] |= Voxel.isTranslucent(blocks[x][y][z-1]) || Voxel.isPartiallyTransparent(blocks[x][y][z-1]);
                            if (z < chunkSize - 1)
                                faceVisible[3] |= Voxel.isTranslucent(blocks[x][y][z+1]) || Voxel.isPartiallyTransparent(blocks[x][y][z+1]);
                            if (x > 0)
                                faceVisible[4] |= Voxel.isTranslucent(blocks[x-1][y][z]) || Voxel.isPartiallyTransparent(blocks[x-1][y][z]);
                            if (x < chunkSize - 1)
                                faceVisible[5] |= Voxel.isTranslucent(blocks[x+1][y][z]) || Voxel.isPartiallyTransparent(blocks[x+1][y][z]);
                        }
                    }
                    
                    // compute total number of visible faces
                    int cubeFaces = 0;
                    for (int i = 0; i < faceVisible.length; i++) {
                        if (faceVisible[i]) {
                            cubeFaces++;
                        }
                    }
                    
                    if (!translucentTexture) {
                        totalFaces += cubeFaces;
                    } else {
                        totalFacesTranslucent += cubeFaces;
                    }
                    
                    if (cubeFaces == 0) {
                        continue;
                    }
                    
                    float blockX = (float) (chunkX + x * Voxel.BLOCK_SIZE);
                    float blockY = (float) (y * Voxel.BLOCK_SIZE - chunkHeight * Voxel.BLOCK_SIZE);
                    float blockZ = (float) (chunkZ + z * Voxel.BLOCK_SIZE);
                    
                    // write cube position vertex data and texture vertex data
                    if (!translucentTexture) {
                        createCube(positionData, writeIndexPosition, faceVisible, blockX, blockY, blockZ);
                        writeIndexPosition += cubeFaces * floatsPerFacePosition;
                        
                        textureCube(textureData, writeIndexTexture, faceVisible, voxelType);
                        writeIndexTexture += cubeFaces * floatsPerFaceTexture;
                    } else {
                        createCube(positionDataTranslucent, writeIndexPositionTranslucent, faceVisible, blockX, blockY, blockZ);
                        writeIndexPositionTranslucent += cubeFaces * floatsPerFacePosition;

                        textureCube(textureDataTranslucent, writeIndexTextureTranslucent, faceVisible, voxelType);
                        writeIndexTextureTranslucent += cubeFaces * floatsPerFaceTexture;                        
                    }
                }
            }
        }
        numVisibleFaces = totalFaces;
        numVisibleFacesTranslucent = totalFacesTranslucent;
        
        
        VBOVertexHandle = glGenBuffers();
        VBOTextureHandle = glGenBuffers();
        VBOVertexHandleTranslucent = glGenBuffers();
        VBOTextureHandleTranslucent = glGenBuffers();
        
        FloatBuffer VertexPosition = BufferUtils.createFloatBuffer(totalFaces * floatsPerFacePosition);
        FloatBuffer VertexTexture = BufferUtils.createFloatBuffer(totalFaces * floatsPerFaceTexture);
        FloatBuffer VertexPositionTranslucent = BufferUtils.createFloatBuffer(totalFacesTranslucent * floatsPerFacePosition);
        FloatBuffer VertexTextureTranslucent = BufferUtils.createFloatBuffer(totalFacesTranslucent * floatsPerFaceTexture);
        
        ((FloatBuffer) VertexPosition.clear()).put(positionData, 0, totalFaces * floatsPerFacePosition).flip();
        ((FloatBuffer) VertexTexture.clear()).put(textureData, 0, totalFaces * floatsPerFaceTexture).flip();
        ((FloatBuffer) VertexPositionTranslucent.clear()).put(positionDataTranslucent, 0, totalFacesTranslucent * floatsPerFacePosition).flip();
        ((FloatBuffer) VertexTextureTranslucent.clear()).put(textureDataTranslucent, 0, totalFacesTranslucent * floatsPerFaceTexture).flip();
        
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

    
    /**
    * method: createCube()
    * purpose: Return an array of float vertices that define a cube at position
    * x,y,z.
    **/
    
    private static void createCube(float[] buff, int startIndex, boolean[] faceVisible, float x, float y, float z) {
        float s = ((float) Voxel.BLOCK_SIZE) / 2;
        int floatsPerFace = 3 * 4;
        
        // TOP QUAD
        if (faceVisible[0]) {
            System.arraycopy(new float[] {
                x + s, y + s, z + s,
                x - s, y + s, z + s,
                x - s, y + s, z - s,
                x + s, y + s, z - s
            }, 0, buff, startIndex, floatsPerFace);
            startIndex += floatsPerFace;
        }
        
        // BOTTOM QUAD
        if (faceVisible[1]) {
            System.arraycopy(new float[] {
                x + s, y - s, z - s,
                x - s, y - s, z - s,
                x - s, y - s, z + s,
                x + s, y - s, z + s
            }, 0, buff, startIndex, floatsPerFace);
            startIndex += floatsPerFace;
        }
        
        // FRONT QUAD
        if (faceVisible[2]) {
            System.arraycopy(new float[] {
                x + s, y + s, z - s,
                x - s, y + s, z - s,
                x - s, y - s, z - s,
                x + s, y - s, z - s
            }, 0, buff, startIndex, floatsPerFace);
            startIndex += floatsPerFace;
        }
        
        // BACK QUAD
        if (faceVisible[3]) {
            System.arraycopy(new float[] {
                x + s, y - s, z + s,
                x - s, y - s, z + s,
                x - s, y + s, z + s,
                x + s, y + s, z + s
            }, 0, buff, startIndex, floatsPerFace);
            startIndex += floatsPerFace;
        }
        
        // LEFT QUAD
        if (faceVisible[4]) {
            System.arraycopy(new float[] {
                x - s, y + s, z - s,
                x - s, y + s, z + s,
                x - s, y - s, z + s,
                x - s, y - s, z - s
            }, 0, buff, startIndex, floatsPerFace);
            startIndex += floatsPerFace;
        }
        
        // RIGHT QUAD
        if (faceVisible[5]) {
            System.arraycopy(new float[] {
                x + s, y + s, z + s,
                x + s, y + s, z - s,
                x + s, y - s, z - s,
                x + s, y - s, z + s
            }, 0, buff, startIndex, floatsPerFace);
            startIndex += floatsPerFace;
        }
    }
    
    /**
    * method: getTexture()
    * purpose: Return an array of float vertices that define the texture for
    * a block of type v
    **/
    private void textureCube(float[] buff, int startIndex, boolean[] faceVisible, VoxelType v) {
        float offset = (2048f/16)/2048f;
        int floatsPerFace = 2 * 4;
        int[] t = Voxel.getTexture(v);
        
        // bottom
        if (faceVisible[0]) {
            System.arraycopy(new float[] {
                offset*(t[2] + 1), offset*(t[3] + 1),
                offset*(t[2] + 0), offset*(t[3] + 1),
                offset*(t[2] + 0), offset*(t[3] + 0),
                offset*(t[2] + 1), offset*(t[3] + 0)
            }, 0, buff, startIndex, floatsPerFace);
            startIndex += floatsPerFace;
        }
        
        // top
        if (faceVisible[1]) {
            System.arraycopy(new float[] {
                offset*(t[0] + 1), offset*(t[1] + 1),
                offset*(t[0] + 0), offset*(t[1] + 1),
                offset*(t[0] + 0), offset*(t[1] + 0),
                offset*(t[0] + 1), offset*(t[1] + 0)
            }, 0, buff, startIndex, floatsPerFace);
            startIndex += floatsPerFace;
        }
        
        // front
        if (faceVisible[2]) {
            System.arraycopy(new float[] {
                offset*(t[4] + 0), offset*(t[5] + 0),
                offset*(t[4] + 1), offset*(t[5] + 0),
                offset*(t[4] + 1), offset*(t[5] + 1),
                offset*(t[4] + 0), offset*(t[5] + 1)
            }, 0, buff, startIndex, floatsPerFace);
            startIndex += floatsPerFace;
        }
        
        // back
        if (faceVisible[3]) {
            System.arraycopy(new float[] {
                offset*(t[6] + 1), offset*(t[7] + 1),
                offset*(t[6] + 0), offset*(t[7] + 1),
                offset*(t[6] + 0), offset*(t[7] + 0),
                offset*(t[6] + 1), offset*(t[7] + 0)
            }, 0, buff, startIndex, floatsPerFace);
            startIndex += floatsPerFace;
        }
        
        // left
        if (faceVisible[4]) {
            System.arraycopy(new float[] {
                offset*(t[8] + 0), offset*(t[9] + 0),
                offset*(t[8] + 1), offset*(t[9] + 0),
                offset*(t[8] + 1), offset*(t[9] + 1),
                offset*(t[8] + 0), offset*(t[9] + 1)
            }, 0, buff, startIndex, floatsPerFace);
            startIndex += floatsPerFace;
        }
        
        // right
        if (faceVisible[5]) {
            System.arraycopy(new float[] {
                offset*(t[10] + 0), offset*(t[11] + 0),
                offset*(t[10] + 1), offset*(t[11] + 0),
                offset*(t[10] + 1), offset*(t[11] + 1),
                offset*(t[10] + 0), offset*(t[11] + 1)
            }, 0, buff, startIndex, floatsPerFace);
            startIndex += floatsPerFace;
        }
    }
    
    private static void createCross(float[] buff, int startIndex, float x, float y, float z) {
        float s = ((float) Voxel.BLOCK_SIZE) / 2;
        int floatsPerFace = 3 * 4;
        
        System.arraycopy(new float[] {
            // FRONT QUAD
            x + s, y + s, z,
            x - s, y + s, z,
            x - s, y - s, z,
            x + s, y - s, z,
        
            // BACK QUAD
            x + s, y - s, z,
            x - s, y - s, z,
            x - s, y + s, z,
            x + s, y + s, z,
        
            // LEFT QUAD
            x, y + s, z - s,
            x, y + s, z + s,
            x, y - s, z + s,
            x, y - s, z - s,
        
            // RIGHT QUAD
            x, y + s, z + s,
            x, y + s, z - s,
            x, y - s, z - s,
            x, y - s, z + s
        }, 0, buff, startIndex, floatsPerFace * 4);
    }
    
    private void textureCross(float[] buff, int startIndex, VoxelType v) {
        float offset = (2048f/16)/2048f;
        int floatsPerFace = 2 * 4;
        int[] t = Voxel.getTexture(v);
        
        
        System.arraycopy(new float[] {
            // front
            offset*(t[4] + 0), offset*(t[5] + 0),
            offset*(t[4] + 1), offset*(t[5] + 0),
            offset*(t[4] + 1), offset*(t[5] + 1),
            offset*(t[4] + 0), offset*(t[5] + 1),
        
            // back
            offset*(t[6] + 1), offset*(t[7] + 1),
            offset*(t[6] + 0), offset*(t[7] + 1),
            offset*(t[6] + 0), offset*(t[7] + 0),
            offset*(t[6] + 1), offset*(t[7] + 0),

            // left
            offset*(t[8] + 0), offset*(t[9] + 0),
            offset*(t[8] + 1), offset*(t[9] + 0),
            offset*(t[8] + 1), offset*(t[9] + 1),
            offset*(t[8] + 0), offset*(t[9] + 1),

            // right
            offset*(t[10] + 0), offset*(t[11] + 0),
            offset*(t[10] + 1), offset*(t[11] + 0),
            offset*(t[10] + 1), offset*(t[11] + 1),
            offset*(t[10] + 0), offset*(t[11] + 1)
        }, 0, buff, startIndex, floatsPerFace * 4);
    }       
}
