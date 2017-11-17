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
import java.util.Random;
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
    
    /**
    * method: addBlocks()
    * purpose: Add blocks of a given type to all the x,y,z positions in the
    * input float array. (x,y,z refer to indices of the blocks array)
    **/
//    private void addBlocks(int[] blockCoords, VoxelType type) {
//        for (int i = 0; i < blockCoords.length - 2; i+= 3) {
//            int xx = blockCoords[i + 0];
//            int yy = blockCoords[i + 1];
//            int zz = blockCoords[i + 2];
//            if (
//                xx >= 0 && xx < chunkSize &&
//                yy >= 0 && yy < chunkSize &&
//                zz >= 0 && zz < chunkSize
//            ) {
//                blocks[xx][yy][zz] = type;
//            }
//        }
//    }
    
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
        if (true) {
            for (int i = 0; i < chunkSize; i++) {
                blocks[i][chunkHeight - 1][0] = VoxelType.BEDROCK;
                blocks[0][chunkHeight - 1][i] = VoxelType.BEDROCK;
                blocks[i][chunkHeight - 1][chunkSize - 1] = VoxelType.BEDROCK;
                blocks[chunkSize - 1][chunkHeight - 1][i] = VoxelType.BEDROCK;
            }
        }
        
        int floatsPerFacePosition = 3 * 4;
        int floatsPerFaceTexture = 2 * 4;        

        
        VBOVertexHandle = glGenBuffers();
        VBOTextureHandle = glGenBuffers();
        
        FloatBuffer VertexPositionData = BufferUtils.createFloatBuffer(numBlocks * 6 * floatsPerFacePosition);
        FloatBuffer VertexTextureData = BufferUtils.createFloatBuffer(numBlocks * 6 * floatsPerFaceTexture);
                
        int totalFaces = 0;
        for (int x = 0; x < chunkSize; x++) {
            for (int z = 0; z < chunkSize; z++) {
                for (int y = 0; y < chunkHeight; y++) {
                    VoxelType v = blocks[x][y][z];
                    
                    // null is used for empty cells
                    if (v == null) {
                        continue;
                    }

                    boolean[] faceVisible;
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
                    } else {
                        faceVisible = new boolean[] { true, true, true, true, true, true };
                    }
                    
                    // compute total number of visible faces
                    for (int i = 0; i < faceVisible.length; i++) {
                        if (faceVisible[i]) {
                            totalFaces++;
                        }
                    }
                    
                    // create the vertices
                    float[] vertexPos = createCube((float) (chunkX + x * BLOCK_SIZE),
                                (float) (y * BLOCK_SIZE - chunkHeight * BLOCK_SIZE),
                                (float) (chunkZ + z * BLOCK_SIZE)
                            );
                    float[] texturePos = getTexture(v);

                    // remove hidden faces
                    float[] vertexPosTrimmed = removeHiddenFaces(vertexPos, faceVisible, floatsPerFacePosition);
                    float[] texturePosTrimmed = removeHiddenFaces(texturePos, faceVisible, floatsPerFaceTexture);

                    // insert into vertex buffer
                    VertexPositionData.put(vertexPosTrimmed);
                    VertexTextureData.put(texturePosTrimmed);
                }
            }
        }
        
        numVisibleFaces = totalFaces;
        
        // resize the float buffers (if excludeHidden is set)
        if (excludeHidden) {
            float[] resizedPositionData = new float[totalFaces * floatsPerFacePosition];
            VertexPositionData.rewind();
            VertexPositionData.get(resizedPositionData);
            VertexPositionData = BufferUtils.createFloatBuffer(totalFaces * floatsPerFacePosition);
            VertexPositionData.put(resizedPositionData);

            float[] resizedTextureData = new float[totalFaces * floatsPerFaceTexture];
            VertexTextureData.rewind();
            VertexTextureData.get(resizedTextureData);
            VertexTextureData = BufferUtils.createFloatBuffer(totalFaces * floatsPerFaceTexture);
            VertexTextureData.put(resizedTextureData);
        }
        
        VertexPositionData.flip();
        VertexTextureData.flip();
        
        glBindBuffer(GL_ARRAY_BUFFER, VBOVertexHandle);
        glBufferData(GL_ARRAY_BUFFER, VertexPositionData, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        
        glBindBuffer(GL_ARRAY_BUFFER, VBOTextureHandle);
        glBufferData(GL_ARRAY_BUFFER, VertexTextureData, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }
    
    
    /**
    * method: removeHiddenFaces()
    * purpose: Delete hidden faces from the vertexArray by looking up their
    * visibility in the faceVisible array.
    **/
    private static float[] removeHiddenFaces(float[] vertexArray, boolean[] faceVisible, int floatsPerFace) {
        int totalFaces = 0;
        for (int i = 0; i < faceVisible.length; i++) {
            if (faceVisible[i]) {
                totalFaces++;
            }
        }
        
        float[] output = new float[totalFaces * floatsPerFace];
        
        int outputArrayIndex = 0;
        for (int i = 0; i < 6; i++) {
            if (faceVisible[i]) {
                for (int j = 0; j < floatsPerFace; j++) {
                    int idx = i * floatsPerFace + j;
                    output[outputArrayIndex++] = vertexArray[idx];
                }
            }
        }
        
        return output;
    }
    
    /**
    * method: createCube()
    * purpose: Return an array of float vertices that define a cube at position
    * x,y,z.
    **/
    private static float[] createCube(float x, float y, float z) {
        float s = ((float) BLOCK_SIZE) / 2;
        return new float[] {
            // TOP QUAD
            x + s, y + s, z + s,
            x - s, y + s, z + s,
            x - s, y + s, z - s,
            x + s, y + s, z - s,

            // BOTTOM QUAD
            x + s, y - s, z - s,
            x - s, y - s, z - s,
            x - s, y - s, z + s,
            x + s, y - s, z + s
            ,
            // FRONT QUAD
            x + s, y + s, z - s,
            x - s, y + s, z - s,
            x - s, y - s, z - s,
            x + s, y - s, z - s,

            // BACK QUAD
            x + s, y - s, z + s,
            x - s, y - s, z + s,
            x - s, y + s, z + s,
            x + s, y + s, z + s,

            // LEFT QUAD
            x - s, y + s, z - s,
            x - s, y + s, z + s,
            x - s, y - s, z + s,
            x - s, y - s, z - s,

            // RIGHT QUAD
            x + s, y + s, z + s,
            x + s, y + s, z - s,
            x + s, y - s, z - s,
            x + s, y - s, z + s
        };
    }
    
    /**
    * method: getTexture()
    * purpose: Return an array of float vertices that define the texture for
    * a block of type v
    **/
    private float[] getTexture(VoxelType v) {
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
        
        return new float[] {
            // bottom
            offset*(btmX + 1), offset*(btmY + 1),
            offset*(btmX + 0), offset*(btmY + 1),
            offset*(btmX + 0), offset*(btmY + 0),
            offset*(btmX + 1), offset*(btmY + 0),
            // top
            offset*(topX + 1), offset*(topY + 1),
            offset*(topX + 0), offset*(topY + 1),
            offset*(topX + 0), offset*(topY + 0),
            offset*(topX + 1), offset*(topY + 0),
            // front
            offset*(fntX + 0), offset*(fntY + 0),
            offset*(fntX + 1), offset*(fntY + 0),
            offset*(fntX + 1), offset*(fntY + 1),
            offset*(fntX + 0), offset*(fntY + 1),
            // back
            offset*(bckX + 1), offset*(bckY + 1),
            offset*(bckX + 0), offset*(bckY + 1),
            offset*(bckX + 0), offset*(bckY + 0),
            offset*(bckX + 1), offset*(bckY + 0),
            // left
            offset*(lftX + 0), offset*(lftY + 0),
            offset*(lftX + 1), offset*(lftY + 0),
            offset*(lftX + 1), offset*(lftY + 1),
            offset*(lftX + 0), offset*(lftY + 1),
            // right
            offset*(rhtX + 0), offset*(rhtY + 0),
            offset*(rhtX + 1), offset*(rhtY + 0),
            offset*(rhtX + 1), offset*(rhtY + 1),
            offset*(rhtX + 0), offset*(rhtY + 1)
        };   
    }
}
