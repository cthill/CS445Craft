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
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import java.lang.management.ThreadMXBean;
import java.lang.management.ManagementFactory;

/**
 *
 * @author cthill
 */
public class WorldGenerator {
    // generator constants
    private static final int WORLD_HEADROOM = 10; // headroom for player building on top of world
    private static final int BEDROCK_HEIGHT = 1;
    private static final int ROCK_LAYER_HEIGHT = 2 * (Chunk.CHUNK_H - WORLD_HEADROOM) / 3;
    private static final int UPPER_LAYER_DIRT_DEPTH = 8;
    private static final int UPPER_LAYER_MAX_HEIGHT_DELTA = 30;
    
    private static final int DIAMOND_MAX_HEIGHT = 10;
    private static final int GOLD_MAX_HEIGHT = 20;
    private static final double DIAMOND_RATE = 0.0025;
    private static final double GOLD_RATE = 0.005;
    private static final double IRON_RATE = 0.01;
    private static final double COAL_RATE = 0.01;
    private static final int ORE_VEIN_MIN = 1;
    private static final int ORE_VEIN_MAX = 8;
    
    private static final int NOISE_FACTOR_HEIGHT = 10;
    private static final int NOISE_FACTOR_LOCAL_HEIGHT = 2;
    private static final int NOISE_FACTOR_REGION_HEIGHT = 14;

    private final int initialSize;
    private final Random rand;
    
    private final SimplexNoise noiseGenLocalHeight;
    private final SimplexNoise noiseGenBlockType;
    private final SimplexNoise noiseGenBiome;
    private final SimplexNoise noiseGenRegionHeight;
    private final SimplexNoise noiseGenCavern;
    private World world;
    
    public static enum Biome {
        NORMAL,
        DESERT,
        WINTER,
        OCEAN
    }
    
    public WorldGenerator(int seed, int initialSize) {
        this.initialSize = initialSize;
        this.rand = new Random();
        
        noiseGenLocalHeight = new SimplexNoise(Chunk.CHUNK_S * 5, 0.30, seed);
        noiseGenRegionHeight = new SimplexNoise(Chunk.CHUNK_S * 10, 0.25, seed + 1);
        noiseGenBlockType = new SimplexNoise(25, .15, seed + 2);
        noiseGenBiome = new SimplexNoise(Chunk.CHUNK_S * 25, 0.09, seed + 3);
        noiseGenCavern = new SimplexNoise(30, .1, seed + 4);
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
        world.getChunks().forEach(chunk -> {
            chunk.setGenerated();
            chunk.rebuildMesh();
            chunk.setDirty(false);
        });
        
        return world;
    }
    
    private Chunk createChunk(int i, int j) {
        return new Chunk(world, i, j);
    }
    
    private double getNoise2d(SimplexNoise noiseGen, int x, int y) {
        return noiseGen.getNoise(x, y);
    }
    
    private double getNoise3d(SimplexNoise noiseGen, int x, int y, int z) {
        return noiseGen.getNoise(x, y, z );
    }
    
    private void fillChunkGenerateRandom(Chunk chunk) {
        ThreadMXBean threadTimer = ManagementFactory.getThreadMXBean();
        long start = threadTimer.getCurrentThreadCpuTime();
        
        VoxelType[][][] blocks = new VoxelType[CHUNK_S][CHUNK_H][CHUNK_S];
        
        // generate heights for each xz position
        int[][] cellHeights = new int[CHUNK_S][CHUNK_S];
        Biome[][] biomes = new Biome[CHUNK_S][CHUNK_S];

        for (int x = 0; x < CHUNK_S; x++) {
            for (int z = 0; z < CHUNK_S; z++) {
                int noiseX = x + chunk.indexI * CHUNK_S;
                int noiseZ = z + chunk.indexJ * CHUNK_S;
                
                double localHeightNoise = getNoise2d(noiseGenLocalHeight, noiseX, noiseZ) * NOISE_FACTOR_LOCAL_HEIGHT;
                double regionHeightNoise = 1 + getNoise2d(noiseGenRegionHeight, chunk.indexI, chunk.indexJ) * NOISE_FACTOR_REGION_HEIGHT;
                
                double combinedHeightNoise = Math.abs(NOISE_FACTOR_HEIGHT * localHeightNoise * regionHeightNoise);
//                System.out.println(localHeightNoise + " " + regionHeightNoise + " " + combinedHeightNoise);                
                
                int heightDelta = (int) combinedHeightNoise;
                if (heightDelta > UPPER_LAYER_MAX_HEIGHT_DELTA) {
                    heightDelta = UPPER_LAYER_MAX_HEIGHT_DELTA;
                }
                if (heightDelta < 1) {
                    heightDelta = 1;
                }
                        
                int cellHeight = BEDROCK_HEIGHT + ROCK_LAYER_HEIGHT + heightDelta;
                if (cellHeight > CHUNK_H - 1 - WORLD_HEADROOM) {
                    cellHeight = CHUNK_H - 1 - WORLD_HEADROOM;
                }
                
                cellHeights[x][z] = cellHeight;
                
                double biomeNoise = Math.abs(getNoise2d(noiseGenBiome, noiseX, noiseZ)) * 3;
                if (Math.abs(localHeightNoise) < 2 && Math.abs(regionHeightNoise) < 2 && Math.abs(combinedHeightNoise) < 2) {
                    // very low region, generate desert or ocean
                    if (biomeNoise > 0.10) {
                        cellHeights[x][z] = BEDROCK_HEIGHT + ROCK_LAYER_HEIGHT + 1;
                        biomes[x][z] = Biome.OCEAN;
                    } else {
                        int cnh = (int) (combinedHeightNoise * 2);
                        if (cnh < 2) {
                            cnh = 2;
                        }
                        cellHeights[x][z] = BEDROCK_HEIGHT + ROCK_LAYER_HEIGHT + cnh;
                        biomes[x][z] = Biome.DESERT;
                    }
                } else {
                    // hilly region, generate normal or winter
                    if (biomeNoise > 0.12) {
                        biomes[x][z] = Biome.WINTER;
                    } else {
                        biomes[x][z] = Biome.NORMAL;
                    }
                }
            }
        }
        
        //for (int y = ROCK_LAYER_HEIGHT; y < CHUNK_H - WORLD_HEADROOM; y++) {
        for (int y = 0; y < CHUNK_H - WORLD_HEADROOM; y++) {
            if (y < BEDROCK_HEIGHT) {
                generateFlatLayer(blocks, y, VoxelType.BEDROCK);
            } else if (y < BEDROCK_HEIGHT + ROCK_LAYER_HEIGHT) {
                generateRockLayer(chunk, blocks, y);
            } else {
                generateUpperLayer(chunk, blocks, y, cellHeights, biomes);
            }
        }
        
        chunk.copyBlocks(blocks, 0, CHUNK_S, 0, CHUNK_H, 0, CHUNK_S);
        chunk.setGenerated();
        System.out.println("Generated " + chunk.indexI + "," + chunk.indexJ + " in " + (threadTimer.getCurrentThreadCpuTime() - start) / 1000000000.0);
    }
    
    private void generateFlatLayer(VoxelType[][][] blocks, int y, VoxelType type) {
        for (int x = 0; x < CHUNK_S; x++) {
            Arrays.fill(blocks[x][y], type);
        }
    }
    
    private void generateRockLayer(Chunk chunk, VoxelType[][][] blocks, int y) {
        for (int x = 0; x < CHUNK_S; x++) {
            for (int z = 0; z < CHUNK_S; z++) {
                generateRockLayerSingle(chunk, blocks, x, y, z);
            }
        }
    }
    
    private void generateRockLayerSingle(Chunk chunk, VoxelType[][][] blocks, int x, int y, int z) {
        int noiseX = x + chunk.indexI * CHUNK_S;
        int noiseZ = z + chunk.indexJ * CHUNK_S;
        double noise = (getNoise3d(noiseGenCavern, noiseX, y, noiseZ) + 1) / 2;
        boolean openSpace = (noise < 0.475);

        if (!openSpace) {
            blocks[x][y][z] = VoxelType.STONE;
            
            if (y < DIAMOND_MAX_HEIGHT && rand.nextDouble() < DIAMOND_RATE)
                generateOreVein(blocks, x, y, z, VoxelType.DIAMOND);
            if (y < GOLD_MAX_HEIGHT && rand.nextDouble() < GOLD_RATE) 
                generateOreVein(blocks, x, y, z, VoxelType.GOLD);
            if (rand.nextDouble() < IRON_RATE)
                generateOreVein(blocks, x, y, z, VoxelType.IRON);
            if (rand.nextDouble() < COAL_RATE) 
                generateOreVein(blocks, x, y, z, VoxelType.COAL);
        }
    }
    
    private void generateUpperLayer(Chunk chunk, VoxelType[][][] blocks, int y, int[][] cellHeights, Biome[][] biomes) {
        for (int x = 0; x < CHUNK_S; x++) {
            for (int z = 0; z < CHUNK_S; z++) {
                if (y > cellHeights[x][z]) {
                    continue;
                }
                
                int rockyCutoff = cellHeights[x][z] - UPPER_LAYER_DIRT_DEPTH;
                if (rockyCutoff < BEDROCK_HEIGHT + ROCK_LAYER_HEIGHT) {
                   rockyCutoff = BEDROCK_HEIGHT + ROCK_LAYER_HEIGHT;
                }
                
                if (y < rockyCutoff) {
                    generateRockLayerSingle(chunk, blocks, x, y, z);
                } else {
                    switch (biomes[x][z]) {
                        default:
                        case NORMAL:
                            generateCellNormalBiome(chunk, blocks, x, y, z, cellHeights[x][z], rockyCutoff);
                            break;
                        case WINTER:
                            generateCellWinterBiome(chunk, blocks, x, y, z, cellHeights[x][z], rockyCutoff);
                            break;
                        case OCEAN:
                            generateCellOceanBiome(chunk, blocks, x, y, z, cellHeights[x][z], rockyCutoff);
                            break;
                        case DESERT:
                            generateCellDesertBiome(chunk, blocks, x, y, z, cellHeights[x][z], rockyCutoff);
                            break;
                    }
                }
            }
        }
    }
    
    private void generateCellNormalBiome(Chunk chunk, VoxelType[][][] blocks, int x, int y, int z, int cellHeight, int rockyCutoff) {
        boolean openSpace = false;
        int noiseX = x + chunk.indexI * CHUNK_S;
        int noiseZ = z + chunk.indexJ * CHUNK_S;
        double noise = (getNoise3d(noiseGenCavern, noiseX, y, noiseZ) + 1) / 2;
        if (y == cellHeight) {
            openSpace = (noise < 0.4525);
        } else {
            openSpace = (noise < 0.47);
        }
        
        if (!openSpace) {
            if (y == cellHeight) {
                // top layer
                blocks[x][y][z] = VoxelType.GRASS;
                geterateGrassFoliage(blocks, x, y, z);
            } else {
                blocks[x][y][z] = VoxelType.DIRT;
            }
        }
    }
    
    private void generateCellWinterBiome(Chunk chunk, VoxelType[][][] blocks, int x, int y, int z, int cellHeight, int rockyCutoff) {
        boolean openSpace = false;
        int noiseX = x + chunk.indexI * CHUNK_S;
        int noiseZ = z + chunk.indexJ * CHUNK_S;
        double noise = (getNoise3d(noiseGenCavern, noiseX, y, noiseZ) + 1) / 2;
        if (y == cellHeight) {
            openSpace = (noise < 0.4525);
        } else {
            openSpace = (noise < 0.47);
        }
        
        if (!openSpace) {
            if (y == cellHeight) {
                // top layer
                if (cellHeight - rockyCutoff < 2) {
                    // low top layer
                    float v = (float) (getNoise3d(noiseGenBlockType, noiseX, y, noiseZ) + 1);
                    if (v > 1.05) {
                        blocks[x][y][z] = VoxelType.SAND;
                        blocks[x][y+1][z] = VoxelType.SNOW;
                    } else if (v > .985) {
                        blocks[x][y][z] = VoxelType.ICE;
                    } else {
                        blocks[x][y][z] = VoxelType.ICE_GRASS;
                        blocks[x][y+1][z] = VoxelType.SNOW;
                        generateWinterFoliage(blocks, x, y, z);
                    }
                } else {
                    // high top layer
                    blocks[x][y][z] = VoxelType.ICE_GRASS;
                    blocks[x][y+1][z] = VoxelType.SNOW;
                    generateWinterFoliage(blocks, x, y, z);
                }
            } else {
                blocks[x][y][z] = VoxelType.DIRT;
            }
        }
    }
    
    private void generateCellOceanBiome(Chunk chunk, VoxelType[][][] blocks, int x, int y, int z, int cellHeight, int rockyCutoff) {
        int noiseX = x + chunk.indexI * CHUNK_S;
        int noiseZ = z + chunk.indexJ * CHUNK_S;
        double v = getNoise2d(noiseGenBlockType, noiseX, noiseZ);
        if (v > 0.45) {
            blocks[x][y][z] = VoxelType.SAND;
            if (y == cellHeight) {
                generateWetSandFoliage(blocks, x, y, z); 
            }
        } else {
            if (y == cellHeight) {
                blocks[x][y][z] = VoxelType.WATER;
            } else {
                blocks[x][y][z] = VoxelType.DIRT;
            }
        }
    }
    
    private void generateCellDesertBiome(Chunk chunk, VoxelType[][][] blocks, int x, int y, int z, int cellHeight, int rockyCutoff) {
        blocks[x][y][z] = VoxelType.SAND;
        if (y == cellHeight) {
            generateDesertFoliage(blocks, x, y, z);
        }
    }
    
    private void generateOreVein(VoxelType[][][] blocks, int x, int y, int z, VoxelType v) {
        int length = rand.nextInt(ORE_VEIN_MAX - ORE_VEIN_MIN + 1) + ORE_VEIN_MIN;
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
            if (val < 0.2)
                px += 1;
            else if (val < 0.4)
                px -= 1;
            else if (val < 0.6)
                pz += 1;
            else if (val < 0.8)
                pz -= 1;
            else if (val < 1.0)
                py -= 1;
            
            coords[i] = px;
            coords[i + 1] = py;
            coords[i + 2] = pz;
        }
        
        addBlocksSafe(blocks, coords, v);
    }
    
    private void geterateGrassFoliage(VoxelType[][][] blocks, int x, int y, int z) {
        if (rand.nextDouble() < 0.05) 
            blocks[x][y+1][z] = VoxelType.TALL_GRASS;
        if (rand.nextDouble() < 0.0040)
            blocks[x][y+1][z] = VoxelType.RED_FLOWER;
        if (rand.nextDouble() < 0.0040)
            blocks[x][y+1][z] = VoxelType.YELLOW_FLOWER;
        if (rand.nextDouble() < 0.001)
            blocks[x][y+1][z] = VoxelType.PUMPKIN;
        if (rand.nextDouble() < 0.001)
            blocks[x][y+1][z] = VoxelType.RED_MUSHROOM;
        if (rand.nextDouble() < 0.001)
            blocks[x][y+1][z] = VoxelType.MUSHROOM;
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
                blocks[x][y+i][z] = VoxelType.REED;
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
                blocks[x][y+i][z] = VoxelType.CACTUS;
            }
        }
    }
    
    /**
    * method: addTree()
    * purpose: Add a simple tree to the blocks array at a given x,y,z. Uses the
    * addBlocks method (which includes bounds checking) to place the blocks.
    **/
    private void generateTree(VoxelType[][][] blocks, int x, int y, int z, boolean snowy) {
        if (x == 0 || x == CHUNK_S || z == 0 || z == CHUNK_S) {
            return;
        }
        
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
    
    public List<Runnable> generateNewChunksIfNeeded(int i, int j, int distance, Screen screen) {
        List<Runnable> newTasks = new ArrayList<>();
        List<Chunk> newChunks = new ArrayList<>();
        for (int d = 1; d <= distance; d++) {
            for (int di = -d; di <= d; di++) {
                addChunkGenerationTask(i + di, j + d, newTasks, newChunks, screen);
                addChunkGenerationTask(i + di, j - d, newTasks, newChunks, screen);
            }
            for (int dj = -d; dj <= d; dj++) {
                addChunkGenerationTask(i + d, j + dj, newTasks, newChunks, screen);
                addChunkGenerationTask(i - d, j + dj, newTasks, newChunks, screen);
            }
            
            newChunks.forEach(chunk -> {
                // make sure adjacent chunks are rebuilt
                world.findAllAdjacentChunks(chunk).forEach(adjChunk -> {
                    adjChunk.setDirty(true);
                });
            });
        }
        return newTasks;
    }
    
    private void addChunkGenerationTask(int i, int j, List<Runnable> newTasks, List<Chunk> newChunks, Screen screen) {
        if (world.getChunk(i, j) == null) {
            Chunk c = createChunk(i, j);
            newChunks.add(c);
            world.addChunk(c);
            screen.addObject(c);
            
            // generate the chunk later
            newTasks.add((Runnable) () -> {
                fillChunkGenerateRandom(c);
                c.setGenerated();
                c.setDirty(true);
            });
        }
    }
}
