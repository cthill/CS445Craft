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
    
    public boolean solidBlockAt(float x, float y, float z) {
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
        
        return chunks[i][j].solidBlockAt(xIndex, yIndex, zIndex);
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
        
        int maxDelta = 7;
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
                        if (y >= 1 && y < CHUNK_H / 2) {
                            type = VoxelType.STONE;
                            if (rand.nextDouble() < 0.001) {
                                if (y < 6)
                                    addOreVein(blocks, x, y, z, VoxelType.DIAMOND);
                                else if (y < 10)
                                    addOreVein(blocks, x, y, z, VoxelType.GOLD);
                            } else if (rand.nextDouble() < 0.05 && y < 16) {
                                if (rand.nextDouble() < 0.5)
                                    addOreVein(blocks, x, y, z, VoxelType.IRON);
                                else
                                    addOreVein(blocks, x, y, z, VoxelType.COAL);
                            }
                            
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
                                if (rand.nextDouble() < 0.005) {
                                    blocks[x][y+1][z] = VoxelType.REED;
                                    blocks[x][y+2][z] = VoxelType.REED;
                                    blocks[x][y+3][z] = VoxelType.REED;
                                }
                            }
                        } else {
                            type = VoxelType.GRASS;
                            
                            if (rand.nextDouble() < 0.05) 
                                blocks[x][y+1][z] = VoxelType.TALL_GRASS;
                            if (rand.nextDouble() < 0.0040)
                                blocks[x][y+1][z] = VoxelType.RED_FLOWER;
                            if (rand.nextDouble() < 0.0040)
                                blocks[x][y+1][z] = VoxelType.YELLOW_FLOWER;
                            if (rand.nextDouble() < 0.001)
                                blocks[x][y+1][z] = VoxelType.RED_MUSHROOM;
                            if (rand.nextDouble() < 0.001)
                                blocks[x][y+1][z] = VoxelType.MUSHROOM;
                            if (rand.nextDouble() < 0.0015)
                                addTree(blocks, x,y + 1,z);
                        }
                    }
                    blocks[x][y][z] = type;
                }
            }
        }
        
        return blocks;
    }
    
    public void addOreVein(VoxelType[][][] blocks, int x, int y, int z, VoxelType v) {
        Random rand = new Random();
        
        int max = 8;
        int min = 2;
        int length = rand.nextInt(max - min + 1) + min;
        int[] coords = new int[length * 3];
        
        // add starting position
        coords[0] = x;
        coords[1] = y;
        coords[2] = z;
        
        for (int i = 3; i < length * 3 - 2; i += 3) {
            int px = coords[i - 3];
            int py = coords[i - 2];
            int pz = coords[i - 1];
            
            double val = rand.nextDouble();
            if (val < 0.16666)
                px += 1;
            else if (val < 0.3333)
                px -= 1;
            else if (val < 0.3333)
                px -= 1;
            else if (val < 0.5)
                py += 1;
            else if (val < 0.6666)
                py -= 1;
            else if (val < 0.75)
                pz += 1;
            else if (val < 0.9166)
                pz -= 1;
            
            coords[i] = px;
            coords[i + 1] = py;
            coords[i + 2] = pz;
        }
        
        addBlocks(blocks, coords, v);
    }
    
    /**
    * method: addTree()
    * purpose: Add a simple tree to the blocks array at a given x,y,z. Uses the
    * addBlocks method (which includes bounds checking) to place the blocks.
    **/
    public void addTree(VoxelType[][][] blocks, int x, int y, int z) {
        int[] trunkCoords = new int[] {
            x, y++, z,
            x, y++, z,
            x, y++, z,
            x, y, z,
            x, y+1, z,
        };
        addBlocks(blocks, trunkCoords, VoxelType.TRUNK);
        
        int[] leafCoords = new int[] {
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
        addBlocks(blocks, leafCoords, VoxelType.LEAVES);
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
                if (blocks[xx][yy][zz] != VoxelType.BEDROCK) {
                    blocks[xx][yy][zz] = type;
                }
            }
        }
    }
    
    public Chunk coordsToChunk(float x, float z) {
        return chunks[0][0];
    }
    
    public void removeBlock(float x, float y, float z) {
        int xIndex = worldPosToBlockIndex(x);
        int yIndex = worldPosToBlockIndex(y);
        int zIndex = worldPosToBlockIndex(z);
        
        int i = blockIndexToChunkNum(xIndex);
        int j = blockIndexToChunkNum(zIndex);
        
        xIndex -= i * CHUNK_S;
        zIndex -= j * CHUNK_S;
        yIndex -= -CHUNK_H;
        
        if (i < 0 || i >= size || j < 0 || j >= size) {
            return;
        }
        
        chunks[i][j].removeBlock(xIndex, yIndex, zIndex);
        chunks[i][j].rebuildMesh();
    }
    
    public void swapMeshes() {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                chunks[i][j].swapMesh();
            }
        }
    }
}
