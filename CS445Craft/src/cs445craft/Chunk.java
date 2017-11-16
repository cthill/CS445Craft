/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cs445craft;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureLoader;

/**
 *
 * @author cthill
 */
public class Chunk extends Drawable {
    public static final int CUBE_S = 2;
    public static final int CHUNK_S = 30; // 30 x 30 x 30 chunk
    public static final int HEADROOM = 5;
    public static final int NUM_BLOCKS = CHUNK_S * CHUNK_S * CHUNK_S;
    
    protected float chunkX, chunkY, chunkZ;
    protected Random rand;
    protected Voxel[][][] data;
    
    private int VBOVertexHandle;
    private int VBOTextureHandle;
    private Texture texture;
    
    public Chunk(float chunkX, float chunkY, float chunkZ) {
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;
        
        rand = new Random();
        data = new Voxel[CHUNK_S][CHUNK_S][CHUNK_S];
        
        texture = loadTexture("terrain");

        generateRandomChunk();
        rebuildMesh();
    }
    
    private void generateRandomChunk() {
        int maxDelta = 6;
        SimplexNoise noiseGenHeight = new SimplexNoise(30, 0.35, rand.nextInt());
        SimplexNoise noiseGenType = new SimplexNoise(15, .1, rand.nextInt());
        
        for (int x = 0; x < CHUNK_S; x++) {
            for (int z = 0; z < CHUNK_S; z++) {
                double noise = noiseGenHeight.getNoise(x, z);
                int heightDelta =(int) (maxDelta * noise + maxDelta) - 1;
                if (heightDelta > maxDelta) {
                    heightDelta = maxDelta;
                }
                
                int maxHeight = CHUNK_S - heightDelta - HEADROOM;
                for (int y = 0; y < maxHeight; y++) {
                    Voxel.Type type = Voxel.Type.BEDROCK;
                    
                    if (y < maxHeight - 1) {
                        if (y > 2 && y < CHUNK_S / 2) {
                            type = Voxel.Type.STONE;
                        } else if (y >= CHUNK_S / 2) {
                            type = Voxel.Type.DIRT;
                        }
                    } else {
                        float v = (float) (noiseGenType.getNoise(x, y, z) + 1) / 2;
                        if (heightDelta == maxDelta) {
                            if (v > 0.5) {
                                type = Voxel.Type.WATER;
                            } else {
                                type = Voxel.Type.SAND;
                            }
                        } else {
                            type = Voxel.Type.GRASS;
                            if (
                                x > 3 && z > 3 &&
                                x < CHUNK_S - 4 && z < CHUNK_S - 4 &&
                                rand.nextDouble() < 0.005
                            ) {
                                addTree(x,y + 1,z);
                            }
                        }
                    }
                    data[x][y][z] = new Voxel(true, type);
                }
            }
        }
    }
    
    public void addTree(int x, int y, int z) {
        int[] trunkCords = new int[] {
            x, y++, z,
            x, y++, z,
            x, y++, z
        };
        addBlocks(trunkCords, Voxel.Type.TRUNK);
        
        int[] leafCords = new int[] {
            // layer 1
            x+1, y, z,
            x-1, y, z,
            x, y, z-1,
            x, y, z+1,
            x, y++, z,

            // layer 2
            x+1, y, z,
            x-1, y, z,
            x, y, z-1,
            x, y, z+1,
            x+1, y, z+1,
            x-1, y, z+1,
            x+1, y, z-1,
            x-1, y, z-1,
            x, y++, z,

            // layer 3
            x+1, y, z,
            x-1, y, z,
            x, y, z-1,
            x, y, z+1,
            x, y++, z
        };
        addBlocks(leafCords, Voxel.Type.LEAVES);
    }
    
    public void addBlocks(int[] blockCoords, Voxel.Type type) {
        for (int i = 0; i < blockCoords.length - 2; i+= 3) {
            int xx = blockCoords[i + 0];
            int yy = blockCoords[i + 1];
            int zz = blockCoords[i + 2];
            if (
                xx >= 0 && xx < CHUNK_S &&
                yy >= 0 && yy < CHUNK_S &&
                zz >= 0 && zz < CHUNK_S
            ) {
                data[xx][yy][zz] = new Voxel(true, type);
            }
        }
    }
    
    public boolean blockAt(float x, float y, float z) {
        int xx = CHUNK_S - (int) Math.round((x - chunkX) / CUBE_S);
        int yy = CHUNK_S - (int) Math.round((y - chunkY) / CUBE_S);
        
        // block are not centered on the z axis so we truncate instead of rounding
        int zz = CHUNK_S - (int) ((z - chunkZ) / CUBE_S);
        
        return safeLookup(xx, yy, zz) != null;
    }
    
    public float depthAt(float x, float y, float z) {
        int xx = CHUNK_S - (int) Math.round((x - chunkX) / CUBE_S);
        int yy = CHUNK_S - (int) Math.round((y - chunkY) / CUBE_S);
        int zz = CHUNK_S - (int) ((z - chunkZ) / CUBE_S);
        
        for (int i = yy; i >= 0; i--) {
            if (safeLookup(xx, i, zz) != null) {
                return i;
            }
        }
        
        return CHUNK_S;
    }
    
    public Voxel safeLookup(int x, int y, int z) {
        if (
            x >= 0 && x < CHUNK_S &&
            y >= 0 && y < CHUNK_S &&
            z >= 0 && z < CHUNK_S
        ) {
            return data[x][y][z];
        }
        
        return null;
    }
    
    public void draw() {
        glPushMatrix();
        glPushMatrix();

        glBindBuffer(GL_ARRAY_BUFFER, VBOVertexHandle);
        glVertexPointer(3, GL_FLOAT, 0, 0L);
        
        glBindBuffer(GL_ARRAY_BUFFER, VBOTextureHandle);
        glBindTexture(GL_TEXTURE_2D, 1);
        glTexCoordPointer(2, GL_FLOAT, 0, 0L);

        glDrawArrays(GL_QUADS, 0, NUM_BLOCKS * 24);
        glPopMatrix();       
    }
    
    private Texture loadTexture(String key) {
        try {
            return TextureLoader.getTexture("png", new FileInputStream(new File("res/" + key + ".png")));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Screen.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Screen.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    private void rebuildMesh() {
        VBOTextureHandle = glGenBuffers();
        VBOVertexHandle = glGenBuffers();
        
        FloatBuffer VertexPositionData = BufferUtils.createFloatBuffer(NUM_BLOCKS * 6 * 12);
        FloatBuffer VertexTextureData = BufferUtils.createFloatBuffer(NUM_BLOCKS * 6 * 12);
        
        for (int x = 0; x < CHUNK_S; x++) {
            for (int z = 0; z < CHUNK_S; z++) {
                for (int y = 0; y < CHUNK_S; y++) {
                    Voxel v = data[x][y][z];
                    
                    if (v == null) {
                        continue;
                    }

                    // don't add hidden voxels to the mesh
                    boolean hidden = false;
                    if (
                        x > 0 && y > 0 && z > 0 &&
                        x < CHUNK_S - 1 && y < CHUNK_S - 1 && z < CHUNK_S - 1
                    ) {
                        hidden = data[x+1][y][z] != null &&
                                 data[x-1][y][z] != null &&
                                 data[x][y+1][z] != null &&
                                 data[x][y-1][z] != null &&
                                 data[x][y][z+1] != null &&
                                 data[x][y][z-1] != null;
                    }
                    
                    //if (!hidden) {
                    if (true) {
                        VertexPositionData.put(
                            createCube(
                                (float) (chunkX + x * CUBE_S),
                                (float) (y * CUBE_S - CHUNK_S * CUBE_S),
                                (float) (chunkZ + z * CUBE_S)
                            )
                        );

                        VertexTextureData.put(getTexture(v));
                    }
                }
            }
        }
        
        VertexTextureData.flip();
        VertexPositionData.flip();
        
        glBindBuffer(GL_ARRAY_BUFFER, VBOVertexHandle);
        glBufferData(GL_ARRAY_BUFFER, VertexPositionData, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        
        glBindBuffer(GL_ARRAY_BUFFER, VBOTextureHandle);
        glBufferData(GL_ARRAY_BUFFER, VertexTextureData, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
}
    
    private static float[] createCube(float x, float y, float z) {
        float s = ((float) CUBE_S) / 2;
        return new float[] {
            // TOP QUAD
            x + s, y + s, z,
            x - s, y + s, z,
            x - s, y + s, z - CUBE_S,
            x + s, y + s, z - CUBE_S,

            // BOTTOM QUAD
            x + s, y - s, z - CUBE_S,
            x - s, y - s, z - CUBE_S,
            x - s, y - s, z,
            x + s, y - s, z
            ,
            // FRONT QUAD
            x + s, y + s, z - CUBE_S,
            x - s, y + s, z - CUBE_S,
            x - s, y - s, z - CUBE_S,
            x + s, y - s, z - CUBE_S,

            // BACK QUAD
            x + s, y - s, z,
            x - s, y - s, z,
            x - s, y + s, z,
            x + s, y + s, z,

            // LEFT QUAD
            x - s, y + s, z - CUBE_S,
            x - s, y + s, z,
            x - s, y - s, z,
            x - s, y - s, z - CUBE_S,

            // RIGHT QUAD
            x + s, y + s, z,
            x + s, y + s, z - CUBE_S,
            x + s, y - s, z - CUBE_S,
            x + s, y - s, z 
        };
    }
    
    public float[] getTexture(Voxel v) {
        float offset = (2048f/16)/2048f;
        
        int btmX, topX, fntX, bckX, lftX, rhtX;
        int btmY, topY, fntY, bckY, lftY, rhtY;
        
        switch (v.type) {
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
