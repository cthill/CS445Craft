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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureLoader;

public class Chunk implements Drawable {
    public static final int BLOCK_SIZE = 2;
    private int chunkSize;
    private int chunkHeight;
    private int numBlocks;
    private int numVisibleFaces;
    
    public enum VoxelType {
        GRASS,
        SAND,
        WATER,
        DIRT,
        STONE,
        TRUNK,
        LEAVES,
        BEDROCK
    }
    
    public float chunkX, chunkY, chunkZ;
    private VoxelType[][][] blocks;
    
    private int VBOVertexHandle;
    private int VBOTextureHandle;
    private Texture texture;
    private boolean excludeHidden;
    
    public Chunk(float chunkX, float chunkY, float chunkZ, int chunkSize, int chunkHeight) throws IOException {
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;
        this.chunkSize = chunkSize;
        this.chunkHeight = chunkHeight;
        this.numBlocks = chunkSize * chunkHeight * chunkSize;
        
        
        blocks = new VoxelType[chunkSize][chunkHeight][chunkSize];
        
        texture = TextureLoader.getTexture("png", new FileInputStream(new File("res/terrain.png")));

        excludeHidden = true;
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

    public boolean blockAt(int x, int y, int z) {
        return safeLookup(x, y, z) != null; 
    }
    
    public float depthAt(int x, int y, int z) {
        for (int i = y; i >= 0; i--) {
            if (safeLookup(x, i, z) != null) {
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
        glPushMatrix();

        glBindBuffer(GL_ARRAY_BUFFER, VBOVertexHandle);
        glVertexPointer(3, GL_FLOAT, 0, 0L);
        
        glBindBuffer(GL_ARRAY_BUFFER, VBOTextureHandle);
        glBindTexture(GL_TEXTURE_2D, 1);
        glTexCoordPointer(2, GL_FLOAT, 0, 0L);

        glDrawArrays(GL_QUADS, 0, numVisibleFaces * 4);
        glPopMatrix();   
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
        if (false) {
            for (int i = 0; i < chunkSize; i++) {
                blocks[i][chunkHeight - 1][0] = VoxelType.BEDROCK;
                blocks[0][chunkHeight - 1][i] = VoxelType.BEDROCK;
                blocks[i][chunkHeight - 1][chunkSize - 1] = VoxelType.BEDROCK;
                blocks[chunkSize - 1][chunkHeight - 1][i] = VoxelType.BEDROCK;
            }
        }
        
        int floatsPerFacePosition = 3 * 4;
        int floatsPerFaceTexture = 2 * 4;        

        int writeIndexPosition = 0;
        int writeIndexTexture = 0;
        float[] positionData = new float[numBlocks * 6 * floatsPerFacePosition];
        float[] textureData = new float[numBlocks * 6 * floatsPerFaceTexture];

        int totalFaces = 0;
        for (int x = 0; x < chunkSize; x++) {
            for (int z = 0; z < chunkSize; z++) {
                for (int y = chunkHeight - 1; y >= 0; y--) {
                    VoxelType voxelType = blocks[x][y][z];
                    
                    // null is used for empty cells
                    if (voxelType == null) {
                        continue;
                    }

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
                    }
                    
                    // compute total number of visible faces
                    int cubeFaces = 0;
                    for (int i = 0; i < faceVisible.length; i++) {
                        if (faceVisible[i]) {
                            totalFaces++;
                            cubeFaces++;
                        }
                    }
                    
                    if (cubeFaces == 0) {
                        continue;
                    }
                    
                    // write cube position vertex data
                    float blockX = (float) (chunkX + x * BLOCK_SIZE);
                    float blockY = (float) (y * BLOCK_SIZE - chunkHeight * BLOCK_SIZE);
                    float blockZ = (float) (chunkZ + z * BLOCK_SIZE);
                    createCube(positionData, writeIndexPosition, faceVisible, blockX, blockY, blockZ);
                    writeIndexPosition += cubeFaces * floatsPerFacePosition;
                    
                    // write cube texture vertex data
                    textureCube(textureData, writeIndexTexture, faceVisible, voxelType);
                    writeIndexTexture += cubeFaces * floatsPerFaceTexture;
                }
            }
        }
        numVisibleFaces = totalFaces;
        
        VBOVertexHandle = glGenBuffers();
        VBOTextureHandle = glGenBuffers();
        FloatBuffer VertexPositionData = BufferUtils.createFloatBuffer(totalFaces * floatsPerFacePosition);
        FloatBuffer VertexTextureData = BufferUtils.createFloatBuffer(totalFaces * floatsPerFaceTexture);
        
        ((FloatBuffer) VertexPositionData.clear()).put(positionData, 0, totalFaces * floatsPerFacePosition).flip();
        ((FloatBuffer) VertexTextureData.clear()).put(textureData, 0, totalFaces * floatsPerFaceTexture).flip();
        
        glBindBuffer(GL_ARRAY_BUFFER, VBOVertexHandle);
        glBufferData(GL_ARRAY_BUFFER, VertexPositionData, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        
        glBindBuffer(GL_ARRAY_BUFFER, VBOTextureHandle);
        glBufferData(GL_ARRAY_BUFFER, VertexTextureData, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    
    /**
    * method: createCube()
    * purpose: Return an array of float vertices that define a cube at position
    * x,y,z.
    **/
    
    private static void createCube(float[] buff, int startIndex, boolean[] faceVisible, float x, float y, float z) {
        float s = ((float) BLOCK_SIZE) / 2;
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
        
        if (faceVisible[1]) {
            System.arraycopy(new float[] {
                // BOTTOM QUAD
                x + s, y - s, z - s,
                x - s, y - s, z - s,
                x - s, y - s, z + s,
                x + s, y - s, z + s
            }, 0, buff, startIndex, floatsPerFace);
            startIndex += floatsPerFace;
        }
        
        if (faceVisible[2]) {
            System.arraycopy(new float[] {
                // FRONT QUAD
                x + s, y + s, z - s,
                x - s, y + s, z - s,
                x - s, y - s, z - s,
                x + s, y - s, z - s
            }, 0, buff, startIndex, floatsPerFace);
            startIndex += floatsPerFace;
        }
        
        if (faceVisible[3]) {
            System.arraycopy(new float[] {
                // BACK QUAD
                x + s, y - s, z + s,
                x - s, y - s, z + s,
                x - s, y + s, z + s,
                x + s, y + s, z + s
            }, 0, buff, startIndex, floatsPerFace);
            startIndex += floatsPerFace;
        }
        
        if (faceVisible[4]) {
            System.arraycopy(new float[] {
                // LEFT QUAD
                x - s, y + s, z - s,
                x - s, y + s, z + s,
                x - s, y - s, z + s,
                x - s, y - s, z - s
            }, 0, buff, startIndex, floatsPerFace);
            startIndex += floatsPerFace;
        }
        
        if (faceVisible[5]) {
            System.arraycopy(new float[] {
                // RIGHT QUAD
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
        
        int btmX, topX, fntX, bckX, lftX, rhtX;
        int btmY, topY, fntY, bckY, lftY, rhtY;
        
        switch (v) {
            case GRASS:
                btmX = 2;
                btmY = 9;
                topX = 2;
                topY = 0;
                lftX = rhtX = bckX = fntX = 3;
                lftY = rhtY = bckY = fntY = 0;
                break;
            case SAND:
                btmX = topX = lftX = rhtX = fntX = bckX = 2;
                btmY = topY = lftY = rhtY = fntY = bckY = 1;
                break;
            case WATER:
                btmX = topX = lftX = rhtX = fntX = bckX = 13;
                btmY = topY = lftY = rhtY = fntY = bckY = 12;
                break;
            case DIRT:
                btmX = topX = lftX = rhtX = fntX = bckX = 2;
                btmY = topY = lftY = rhtY = fntY = bckY = 0;
                break;
            case STONE:
                btmX = topX = lftX = rhtX = fntX = bckX = 1;
                btmY = topY = lftY = rhtY = fntY = bckY = 0;
                break;
            case TRUNK:
                btmX = topX = 5;
                btmY = topY = 1;
                lftX = rhtX = bckX = fntX = 4;
                lftY = rhtY = bckY = fntY = 1;
                break;
            case LEAVES:
                btmX = topX = lftX = rhtX = fntX = bckX = 1;
                btmY = topY = lftY = rhtY = fntY = bckY = 9;
                break;
            case BEDROCK:
                btmX = topX = lftX = rhtX = fntX = bckX = 1;
                btmY = topY = lftY = rhtY = fntY = bckY = 1;
                break;
            default:
                btmX = topX = lftX = rhtX = fntX = bckX = 10;
                btmY = topY = lftY = rhtY = fntY = bckY = 1;
                break;
        }
        
        int floatsPerFace = 2 * 4;
        
        // bottom
        if (faceVisible[0]) {
            System.arraycopy(new float[] {
                offset*(btmX + 1), offset*(btmY + 1),
                offset*(btmX + 0), offset*(btmY + 1),
                offset*(btmX + 0), offset*(btmY + 0),
                offset*(btmX + 1), offset*(btmY + 0)
            }, 0, buff, startIndex, floatsPerFace);
            startIndex += floatsPerFace;
        }
        
        // top
        if (faceVisible[1]) {
            System.arraycopy(new float[] {
                offset*(topX + 1), offset*(topY + 1),
                offset*(topX + 0), offset*(topY + 1),
                offset*(topX + 0), offset*(topY + 0),
                offset*(topX + 1), offset*(topY + 0)
            }, 0, buff, startIndex, floatsPerFace);
            startIndex += floatsPerFace;
        }
        
        // front
        if (faceVisible[2]) {
            System.arraycopy(new float[] {
                offset*(fntX + 0), offset*(fntY + 0),
                offset*(fntX + 1), offset*(fntY + 0),
                offset*(fntX + 1), offset*(fntY + 1),
                offset*(fntX + 0), offset*(fntY + 1)
            }, 0, buff, startIndex, floatsPerFace);
            startIndex += floatsPerFace;
        }
        
        // back
        if (faceVisible[3]) {
            System.arraycopy(new float[] {
                offset*(bckX + 1), offset*(bckY + 1),
                offset*(bckX + 0), offset*(bckY + 1),
                offset*(bckX + 0), offset*(bckY + 0),
                offset*(bckX + 1), offset*(bckY + 0)
            }, 0, buff, startIndex, floatsPerFace);
            startIndex += floatsPerFace;
        }
        
        // left
        if (faceVisible[4]) {
            System.arraycopy(new float[] {
                offset*(lftX + 0), offset*(lftY + 0),
                offset*(lftX + 1), offset*(lftY + 0),
                offset*(lftX + 1), offset*(lftY + 1),
                offset*(lftX + 0), offset*(lftY + 1)
            }, 0, buff, startIndex, floatsPerFace);
            startIndex += floatsPerFace;
        }
        
        // right
        if (faceVisible[5]) {
            System.arraycopy(new float[] {
                offset*(rhtX + 0), offset*(rhtY + 0),
                offset*(rhtX + 1), offset*(rhtY + 0),
                offset*(rhtX + 1), offset*(rhtY + 1),
                offset*(rhtX + 0), offset*(rhtY + 1)
            }, 0, buff, startIndex, floatsPerFace);
            startIndex += floatsPerFace;
        }
    }
}
