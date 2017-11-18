/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cs445craft;

import cs445craft.Chunk.VoxelType;
import java.io.IOException;
import java.util.Random;

public class World {
    public static final int CHUNK_S = 30; // 30 x 30 x 30 chunk
    public static final int CHUNK_H = 30; // 30 x 30 x 30 chunk
    
    public static final int NUM_BLOCKS = CHUNK_S * CHUNK_S * CHUNK_H;
    
    private int size;
    private Chunk[][] chunks;
    
    public World(int size) throws IOException {
        this.size = size;
        chunks = new Chunk[size][size];
        
        // generate a large random world
        VoxelType[][][] world = generateRandomWorld(size, 1, size);
        
        // split work into CHUNK_S sized chunks
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                float offsetX = CHUNK_S * i;
                float offsetZ = CHUNK_S * j;
                
                Chunk c = new Chunk(offsetX * Chunk.BLOCK_SIZE, 0, offsetZ * Chunk.BLOCK_SIZE, CHUNK_S, CHUNK_H);
                c.copyBlocks(world, i * CHUNK_S, CHUNK_S, 0, CHUNK_H, j * CHUNK_S, CHUNK_S);
                c.rebuildMesh();
                chunks[i][j] = c;
            }
        }    
    }
    
    public Chunk[][] getChunks() {
        return chunks;
    }
    
    public float getWorldSize() {
        return CHUNK_S * Chunk.BLOCK_SIZE * size;
    }
    
    public int worldPosToBlockIndex(float pos) {
        return -(int) (Math.round(pos / Chunk.BLOCK_SIZE));
    }
    
    public int blockIndexToChunkNum(int index) {
        return index / CHUNK_S;
    }
    
    public boolean blockAt(float x, float y, float z) {
        int xIndex = worldPosToBlockIndex(x);
        int yIndex = worldPosToBlockIndex(y);
        int zIndex = worldPosToBlockIndex(z);
        
        int i = blockIndexToChunkNum(xIndex);
        int j = blockIndexToChunkNum(zIndex);
        
        xIndex -= i * CHUNK_S;
        zIndex -= j * CHUNK_S;
        yIndex -= -CHUNK_H;
        
        if (i < 0 || i >= size || j < 0 || j >= size) {
            return false;
        }
        
        return chunks[i][j].blockAt(xIndex, yIndex, zIndex);
    }
    
    public float depthAt(float x, float y, float z) {
        int xIndex = worldPosToBlockIndex(x);
        int yIndex = worldPosToBlockIndex(y);
        int zIndex = worldPosToBlockIndex(z);
        
        int i = blockIndexToChunkNum(xIndex);
        int j = blockIndexToChunkNum(zIndex);
        
        xIndex -= i * CHUNK_S;
        zIndex -= j * CHUNK_S;
        yIndex -= -CHUNK_H;
        
        return chunks[i][j].depthAt(xIndex, yIndex, zIndex);
    }
    
    /**
    * method: generateRandomWorld()
    * purpose: Use simplex noise to fill the blocks array with blocks. Also adds
    * trees at random.
    **/
    private VoxelType[][][] generateRandomWorld(int sizeX, int sizeY, int sizeZ) {
        int numBlocksX = sizeX * CHUNK_S;
        int numBlocksY = sizeY * CHUNK_H;
        int numBlockzZ = sizeZ * CHUNK_S;
        
        VoxelType[][][] blocks = new VoxelType[numBlocksX][numBlocksY][numBlockzZ];
        
        int maxDelta = 15;
        int headroom = 2;
        Random rand = new Random();
        SimplexNoise noiseGenHeight = new SimplexNoise(90, 0.35, rand.nextInt());
        SimplexNoise noiseGenType = new SimplexNoise(45, .1, rand.nextInt());
        
        for (int x = 0; x < numBlocksX; x++) {
            for (int z = 0; z < numBlockzZ; z++) {
                double noise = noiseGenHeight.getNoise(x, z);
                int heightDelta =(int) (maxDelta * noise + maxDelta) - 1;
                if (heightDelta > maxDelta) {
                    heightDelta = maxDelta;
                }
                
                int maxHeight = numBlocksY - heightDelta - headroom;
                for (int y = 0; y < maxHeight; y++) {
                    VoxelType type = VoxelType.BEDROCK;
                    
                    if (y < maxHeight - 1) {
                        if (y > 2 && y < CHUNK_H / 2) {
                            type = VoxelType.STONE;
                        } else if (y >= CHUNK_H / 2) {
                            type = VoxelType.DIRT;
                        }
                    } else {
                        float v = (float) (noiseGenType.getNoise(x, y, z) + 1) / 2;
                        if (heightDelta == maxDelta) {
                            if (v > 0.5) {
                                type = VoxelType.WATER;
                            } else {
                                type = VoxelType.SAND;
                            }
                        } else {
                            type = VoxelType.GRASS;
                            if (rand.nextDouble() < 0.0015) {
                                addTree(blocks, x,y + 1,z);
                            }
                        }
                    }
                    blocks[x][y][z] = type;
                }
            }
        }
        
        return blocks;
    }
    
    /**
    * method: addTree()
    * purpose: Add a simple tree to the blocks array at a given x,y,z. Uses the
    * addBlocks method (which includes bounds checking) to place the blocks.
    **/
    public void addTree(VoxelType[][][] blocks, int x, int y, int z) {
        int[] trunkCords = new int[] {
            x, y++, z,
            x, y++, z,
            x, y++, z,
            x, y, z,
            x, y+1, z,
        };
        addBlocks(blocks, trunkCords, VoxelType.TRUNK);
        
        int[] leafCords = new int[] {
            // layer 1
            x+1, y, z,
            x-1, y, z,
            x, y, z-1,
            x, y++, z+1,

            // layer 2
            x+1, y, z,
            x-1, y, z,
            x, y, z-1,
            x, y, z+1,
            x+1, y, z+1,
            x-1, y, z+1,
            x+1, y, z-1,
            x-1, y++, z-1,

            // layer 3
            x+1, y, z,
            x-1, y, z,
            x, y, z-1,
            x, y, z+1,
            x, y++, z
        };
        addBlocks(blocks, leafCords, VoxelType.LEAVES);
    }
    
    /**
    * method: addBlocks()
    * purpose: Add blocks of a given type to all the x,y,z positions in the
    * input float array. (x,y,z refer to indices of the blocks array)
    **/
    private void addBlocks(VoxelType[][][] blocks, int[] blockCoords, VoxelType type) {
        for (int i = 0; i < blockCoords.length - 2; i+= 3) {
            int xx = blockCoords[i + 0];
            int yy = blockCoords[i + 1];
            int zz = blockCoords[i + 2];
            if (
                xx >= 0 && xx < CHUNK_S * size &&
                yy >= 0 && yy < CHUNK_H &&
                zz >= 0 && zz < CHUNK_S * size
            ) {
                blocks[xx][yy][zz] = type;
            }
        }
    }
    
    public Chunk coordsToChunk(float x, float z) {
        return chunks[0][0];
    }
    
    public void draw() {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                chunks[i][j].draw();
            }
        }
    }
    
    public void drawTranslucent() {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                chunks[i][j].drawTranslucent();
            }
        }
    }
    
    public void swapMeshes() {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                chunks[i][j].swapMesh();
            }
        }
    }
}
