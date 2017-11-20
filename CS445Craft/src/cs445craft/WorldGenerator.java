/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cs445craft;

import cs445craft.Voxel.VoxelType;
import static cs445craft.Chunk.CHUNK_H;
import static cs445craft.Chunk.CHUNK_S;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 *
 * @author cthill
 */
public class WorldGenerator {
    private static final int CHUNK_GENERATION_BOUNDARY = 3;
    private final int seed;
    private final int initialSize;
    private final Random rand;
    
    private final SimplexNoise noiseGenHeight;
    private final SimplexNoise noiseGenType;
    private final SimplexNoise noiseGenBiome;
    private World world;
    
    public static enum Biome {
        NORMAL,
        ROCKY,
        DESERT,
        WINTER
    }
    
    public WorldGenerator(int seed, int initialSize) {
        this.seed = seed;
        this.initialSize = initialSize;
        this.rand = new Random();
        
        noiseGenHeight = new SimplexNoise(90, 0.40, seed);
        noiseGenType = new SimplexNoise(45, .1, seed + 1);
        noiseGenBiome = new SimplexNoise(Chunk.CHUNK_S * 25, 0.1, seed + 2);
    }
    
    public World getOrGenerate() {
        if (world != null) {
            return world;
        }

        world = new World();
        
        for (int i = 0; i < initialSize; i++) {
            for (int j = 0; j < initialSize; j++) {
                Chunk c = createChunk(i, j);
                fillChunkGenerateRandom(c);
                world.addChunk(c);
            }
        }
        
        // rebuild all the meshes
        world.getChunks().forEach(chunk -> chunk.rebuildMesh());
        
        return world;
    }
    
    private Chunk createChunk(int i, int j) {
        return new Chunk(world, i, j);
    }
    
    private void fillChunkGenerateRandom(Chunk chunk) {
        int maxDelta = 15;
        int headroom = 3;
        
        VoxelType[][][] blocks = new VoxelType[CHUNK_S][CHUNK_H][CHUNK_S];
        
        for (int x = 0; x < CHUNK_S; x++) {
            for (int z = 0; z < CHUNK_S; z++) {
                int noiseX = x + chunk.i * CHUNK_S;
                int noiseZ = z + chunk.j * CHUNK_S;
                Biome biome = getBiome(noiseX, noiseZ);
                
                double noise = noiseGenHeight.getNoise(noiseX, noiseZ);
                int heightDelta = (int) (maxDelta * noise + maxDelta) - 1;
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
                            if (biome == Biome.DESERT) {
                                type = Voxel.VoxelType.SAND;
                            } else {
                                type = Voxel.VoxelType.DIRT;
                            }
                        }
                    } else {
                        if (biome == Biome.DESERT) {
                            type = Voxel.VoxelType.SAND;
                            generateDesertFoliage(blocks, x, y, z);
                        } else {
                            boolean lowPoint = (heightDelta == maxDelta);
                            
                            if (lowPoint) {
                                float v = (float) (noiseGenType.getNoise(noiseX, y, noiseZ) + 1) / 2;
                                if (v > 0.485) {
                                    if (biome == Biome.WINTER) {
                                        type = Voxel.VoxelType.ICE;
                                    } else {
                                        type = Voxel.VoxelType.WATER;
                                    }
                                } else {
                                    type = Voxel.VoxelType.SAND;
                                    if (biome == Biome.WINTER) {
                                        blocks[x][y+1][z] = VoxelType.SNOW;
                                    } else if (biome == Biome.NORMAL) {
                                        generateWetSandFoliage(blocks, x, y, z);
                                    }
                                }
                            } else {
                                if (biome == Biome.WINTER) {
                                    type = Voxel.VoxelType.ICE_GRASS;
                                    generateWinterFoliage(blocks, x, y, z);
                                } else {
                                    type = Voxel.VoxelType.GRASS;
                                    geterateFoliage(blocks, x, y, z);
                                }
                            }
                        }
                    }
                    blocks[x][y][z] = type;
                }
            }
        }
        
        chunk.copyBlocks(blocks, 0, CHUNK_S, 0, CHUNK_H, 0, CHUNK_S);
    }
    
    private Biome getBiome(int x, int y) {
        double biomeNoise = Math.abs(noiseGenBiome.getNoise(x, y)) * 2;
        if (biomeNoise > 0.11) {
            return Biome.NORMAL;
        } else if (biomeNoise > 0.05) {
            return Biome.WINTER;
        } else {
            return Biome.DESERT;
        }
    }
    
    private void generateOreVein(VoxelType[][][] blocks, int x, int y, int z, VoxelType v) {        
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
    
    private void geterateFoliage(VoxelType[][][] blocks, int x, int y, int z) {
        if (rand.nextDouble() < 0.05) 
            blocks[x][y+1][z] = Voxel.VoxelType.TALL_GRASS;
        if (rand.nextDouble() < 0.0040)
            blocks[x][y+1][z] = Voxel.VoxelType.RED_FLOWER;
        if (rand.nextDouble() < 0.0040)
            blocks[x][y+1][z] = Voxel.VoxelType.YELLOW_FLOWER;
        if (rand.nextDouble() < 0.001)
            blocks[x][y+1][z] = Voxel.VoxelType.PUMPKIN;
        if (rand.nextDouble() < 0.001)
            blocks[x][y+1][z] = Voxel.VoxelType.RED_MUSHROOM;
        if (rand.nextDouble() < 0.001)
            blocks[x][y+1][z] = Voxel.VoxelType.MUSHROOM;
        if (rand.nextDouble() < 0.0015)
            generateTree(blocks, x, y + 1, z, false);
    }
    
    private void generateWetSandFoliage(VoxelType[][][] blocks, int x, int y, int z) {
        if (rand.nextDouble() < 0.005) {
            // generate reeds
            int max = 5;
            int min = 2;
            int height = rand.nextInt(max - min + 1) + min;
            for (int i = 1; i <= height; i++) {
                blocks[x][y+i][z] = Voxel.VoxelType.REED;
            }
        }
    }
    
    private void generateWinterFoliage(VoxelType[][][] blocks, int x, int y, int z) {
        blocks[x][y+1][z] = VoxelType.SNOW;
        if (rand.nextDouble() < 0.0015)
            generateTree(blocks, x, y + 1, z, true);
    }
    
    private void generateDesertFoliage(VoxelType[][][] blocks, int x, int y, int z) {
        if (rand.nextDouble() < 0.001) {
            // generate caci
            int max = 6;
            int min = 3;
            int height = rand.nextInt(max - min + 1) + min;
            for (int i = 1; i <= height; i++) {
                blocks[x][y+i][z] = Voxel.VoxelType.CACTUS;
            }
        }
    }
    
    /**
    * method: addTree()
    * purpose: Add a simple tree to the blocks array at a given x,y,z. Uses the
    * addBlocks method (which includes bounds checking) to place the blocks.
    **/
    private void generateTree(VoxelType[][][] blocks, int x, int y, int z, boolean snowy) {
        int[] trunkCoords = new int[] {
            x, y++, z,
            x, y++, z,
            x, y++, z,
            x, y, z,
            x, y+1, z,
        };
        if (snowy) {
            addBlocksSafe(blocks, trunkCoords, VoxelType.DARK_TRUNK);
        } else {
            addBlocksSafe(blocks, trunkCoords, VoxelType.TRUNK);
        }
        
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
        
        if (snowy) {                    
            int[] snowCoords = new int[] {
                x+1, y, z,
                x-1, y, z,
                x, y, z-1,
                x, y, z+1,
                x, y, z,

                x-1, y-1, z-1,
                x-1, y-1, z+1,
                x+1, y-1, z-1,
                x+1, y-1, z+1
            };
            addBlocksSafe(blocks, snowCoords, VoxelType.SNOW);
        }
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
    
    public List<Runnable> newChunkPosition(int i, int j, Screen s) {
        List<Runnable> newTasks = new ArrayList<>();
        List<Chunk> newChunks = new ArrayList<>();

        for (int di = -CHUNK_GENERATION_BOUNDARY; di <= CHUNK_GENERATION_BOUNDARY; di++) {
            for (int dj = -CHUNK_GENERATION_BOUNDARY; dj <= CHUNK_GENERATION_BOUNDARY; dj++) {
                final int indexI = i + di;
                final int indexJ = j + dj;
                
                if (world.getChunk(indexI, indexJ) == null) {
                    Chunk c = createChunk(indexI, indexJ);
                    newChunks.add(c);
                    world.addChunk(c);
                    
                    // generate the chunk later
                    newTasks.add((Runnable) () -> {
                        fillChunkGenerateRandom(c);
                        System.out.println("Generated new chunk " + indexI + " " + indexJ);
                    });
                }
            }
        }

        Set<Chunk> newAndAdjacentChunks = new HashSet<>();
        newAndAdjacentChunks.addAll(newChunks);
        newChunks.forEach(chunk -> newAndAdjacentChunks.addAll(world.findAllAdjacentChunks(chunk)));
        
        // sort the chunks so the closest ones are built first
        List<Chunk> newAndAdjacentChunksSorted = new ArrayList<>(newAndAdjacentChunks);
        newAndAdjacentChunksSorted.sort(Comparator.comparing(object -> ((Chunk) object).gridDistanceTo(i, j)));
        
        // add tasks to asynchronously rebuild the meshes
        newAndAdjacentChunksSorted.forEach(chunk -> {
            newTasks.add((Runnable) () -> {
                chunk.rebuildMesh();
                s.addObject(chunk);
                //System.out.println("rebuilt chunk " + chunk.i + " " + chunk.j);
            });
        });

        return newTasks;
    }
}
