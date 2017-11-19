/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cs445craft;

import cs445craft.Voxel.VoxelType;
import static cs445craft.World.CHUNK_H;
import static cs445craft.World.CHUNK_S;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 *
 * @author cthill
 */
public class WorldGenerator {
    private int seed;
    private Random rand;
    
    private SimplexNoise noiseGenHeight;
    private SimplexNoise noiseGenType;
    private World world;
    
    public WorldGenerator(int seed) {
        this.seed = seed;
        this.rand = new Random();
        
        noiseGenHeight = new SimplexNoise(90, 0.40, seed);
        noiseGenType = new SimplexNoise(45, .1, seed + 1);
    }
    
    public World getOrGenerate() {
        if (world != null) {
            return world;
        }
        
        int initialSize = 3;
        
        world = new World(initialSize);
        
        for (int i = 0; i < initialSize; i++) {
            for (int j = 0; j < initialSize; j++) {
                Chunk c = generateRandomChunk(i, j);
                world.addChunk(i, j, c);
            }
        }
        
        // rebuild all the meshes
        world.getChunks().forEach(chunk -> chunk.rebuildMesh());
        
        return world;
    }
    
    public List<Chunk> newChunkPosition(int i, int j) {
        List<Chunk> newChunks = new ArrayList<>();
        int s = 1;
        
        newChunkPositionHelper(i,  0, j,  0, newChunks);
        newChunkPositionHelper(i,  s, j,  0, newChunks);
        newChunkPositionHelper(i, -s, j,  0, newChunks);
        newChunkPositionHelper(i,  s, j,  s, newChunks);
        newChunkPositionHelper(i, -s, j,  s, newChunks);
        newChunkPositionHelper(i,  s, j, -s, newChunks);
        newChunkPositionHelper(i, -s, j, -s, newChunks);
        newChunkPositionHelper(i,  0, j,  s, newChunks);
        newChunkPositionHelper(i,  0, j, -s, newChunks);
        
        Set<Chunk> adjacentChunks = new HashSet<>();
        adjacentChunks.addAll(newChunks);
        newChunks.forEach(chunk -> adjacentChunks.addAll(world.findAdjacent(chunk)));
        adjacentChunks.forEach(chunk -> chunk.rebuildMesh());
        
        return newChunks;
    }
    
    private void newChunkPositionHelper(int i, int di, int j, int dj, List<Chunk> newChunks) {
        if (world.getChunk(i + di, j + dj) == null) {
            Chunk c = generateRandomChunk(i + di, j + dj);
            world.addChunk(i + di, j + dj, c);
            newChunks.add(c);
        }
    }
    
    private Chunk generateRandomChunk(int i, int j) {
        int chunkX = i * CHUNK_S * Voxel.BLOCK_SIZE;
        int chunkZ = j * CHUNK_S * Voxel.BLOCK_SIZE;
        Chunk chunk = new Chunk(world, chunkX, chunkZ, CHUNK_S, CHUNK_H);
        
        int maxDelta = 15;
        int headroom = 3;
        
        VoxelType[][][] blocks = new VoxelType[CHUNK_S][CHUNK_H][CHUNK_S];
        
        for (int x = 0; x < CHUNK_S; x++) {
            for (int z = 0; z < CHUNK_S; z++) {
                int noiseX = x + i * CHUNK_S;
                int noiseZ = z + j * CHUNK_S;
                
                double noise = noiseGenHeight.getNoise(noiseX, noiseZ);
                int heightDelta =(int) (maxDelta * noise + maxDelta) - 1;
                if (heightDelta > maxDelta) {
                    heightDelta = maxDelta;
                }
                
                int maxHeight = CHUNK_H - heightDelta - headroom;
                for (int y = 0; y < maxHeight; y++) {
                    Voxel.VoxelType type = Voxel.VoxelType.BEDROCK;
                    
                    if (y < maxHeight - 1) {
                        if (y >= 1 && y < CHUNK_H / 2) {
                            type = Voxel.VoxelType.STONE;
                            if (rand.nextDouble() < 0.001) {
                                if (y < 6)
                                    generateOreVein(blocks, x, y, z, Voxel.VoxelType.DIAMOND);
                                else if (y < 10)
                                    generateOreVein(blocks, x, y, z, Voxel.VoxelType.GOLD);
                            } else if (rand.nextDouble() < 0.05 && y < 16) {
                                if (rand.nextDouble() < 0.5)
                                    generateOreVein(blocks, x, y, z, Voxel.VoxelType.IRON);
                                else
                                    generateOreVein(blocks, x, y, z, Voxel.VoxelType.COAL);
                            }
                            
                        } else if (y >= CHUNK_H / 2) {
                            type = Voxel.VoxelType.DIRT;
                        }
                    } else {
                        float v = (float) (noiseGenType.getNoise(noiseX, y, noiseZ) + 1) / 2;
                        if (heightDelta == maxDelta) {
                            if (v > 0.5) {
                                type = Voxel.VoxelType.WATER;
                            } else {
                                type = Voxel.VoxelType.SAND;
                                if (rand.nextDouble() < 0.005) {
                                    blocks[x][y+1][z] = Voxel.VoxelType.REED;
                                    blocks[x][y+2][z] = Voxel.VoxelType.REED;
                                    blocks[x][y+3][z] = Voxel.VoxelType.REED;
                                }
                            }
                        } else {
                            type = Voxel.VoxelType.GRASS;
                            
                            if (rand.nextDouble() < 0.05) 
                                blocks[x][y+1][z] = Voxel.VoxelType.TALL_GRASS;
                            if (rand.nextDouble() < 0.0040)
                                blocks[x][y+1][z] = Voxel.VoxelType.RED_FLOWER;
                            if (rand.nextDouble() < 0.0040)
                                blocks[x][y+1][z] = Voxel.VoxelType.YELLOW_FLOWER;
                            if (rand.nextDouble() < 0.001)
                                blocks[x][y+1][z] = Voxel.VoxelType.RED_MUSHROOM;
                            if (rand.nextDouble() < 0.001)
                                blocks[x][y+1][z] = Voxel.VoxelType.MUSHROOM;
                            if (rand.nextDouble() < 0.0015)
                                generateTree(blocks, x,y + 1,z);
                        }
                    }
                    blocks[x][y][z] = type;
                }
            }
        }
        
        chunk.copyBlocks(blocks, 0, CHUNK_S, 0, CHUNK_H, 0, CHUNK_S);
        return chunk;
    }
    
    private void generateOreVein(VoxelType[][][] blocks, int x, int y, int z, VoxelType v) {
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
        
        addBlocksSafe(blocks, coords, v);
    }
    
    /**
    * method: addTree()
    * purpose: Add a simple tree to the blocks array at a given x,y,z. Uses the
    * addBlocks method (which includes bounds checking) to place the blocks.
    **/
    private void generateTree(VoxelType[][][] blocks, int x, int y, int z) {
        int[] trunkCoords = new int[] {
            x, y++, z,
            x, y++, z,
            x, y++, z,
            x, y, z,
            x, y+1, z,
        };
        addBlocksSafe(blocks, trunkCoords, VoxelType.TRUNK);
        
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
        addBlocksSafe(blocks, leafCoords, VoxelType.LEAVES);
    }
    
    /**
    * method: addBlocks()
    * purpose: Add blocks of a given type to all the x,y,z positions in the
    * input float array. (x,y,z refer to indices of the blocks array)
    **/
    private void addBlocksSafe(VoxelType[][][] blocks, int[] blockCoords, VoxelType type) {
        for (int i = 0; i < blockCoords.length - 2; i+= 3) {
            int xx = blockCoords[i + 0];
            int yy = blockCoords[i + 1];
            int zz = blockCoords[i + 2];
            if (
                xx >= 0 && xx < blocks.length &&
                yy >= 0 && yy < blocks[xx].length &&
                zz >= 0 && zz < blocks[xx][yy].length
            ) {
                if (blocks[xx][yy][zz] != VoxelType.BEDROCK) {
                    blocks[xx][yy][zz] = type;
                }
            }
        }
    }
}
