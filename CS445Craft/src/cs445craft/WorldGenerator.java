/***************************************************************
* file: WorldGenerator.java
* author: CS445 Group 42^3
* class: CS 445 – Computer Graphics
*
* assignment: Final Project
* date last modified: 10/28/2017
*
* purpose: This class is responsible for generating new chunks
* using SimplexNoise and java.util.Random. It supports four biomes,
* various plants, ores, water, and caverns.
* 
****************************************************************/
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
    
    private static final int NOISE_OFFSET_X = 0;
    private static final int NOISE_OFFSET_Z = 0;
    private static final int NOISE_FACTOR_HEIGHT = 8;
    private static final int NOISE_FACTOR_LOCAL_HEIGHT = 5;
    private static final int NOISE_FACTOR_REGION_HEIGHT = 7;

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
    
    /**
    * method: getOrGenerate()
    * purpose: Get or create a new world object. When creating a world, intialize
    * it with new chunks.
    **/
    public World getOrGenerate() {
        if (world != null) {
            return world;
        }

        world = new World();
        
        for (int i = 0; i < initialSize; i++) {
            for (int j = 0; j < initialSize; j++) {
                Chunk c = new Chunk(world, i, j);
                fillChunkGenerateRandom(c);
                world.addChunk(c);
            }
        }
        
        // rebuild all the meshes
        world.getChunks().forEach(chunk -> {
            chunk.setGenerated();
            chunk.rebuildMesh();
            chunk.copyMeshToVBO();
        });
        
        return world;
    }
    
    /**
    * method: createNewChunksIfNeeded()
    * purpose: Scan the voxel grid around a given i and j chunk location to determine
    * if new chunks need to be created. Used for on the fly chunk generation when
    * the player moves towards ungenerated parts of the world.
    **/
    public List<Chunk> createNewChunksIfNeeded(int i, int j, int distance, Screen screen) {
        List<Chunk> newChunks = new ArrayList<>();
        for (int d = 1; d <= distance; d++) {
            for (int di = -d; di <= d; di++) {
                createChunk(i + di, j + d, newChunks, screen);
                createChunk(i + di, j - d, newChunks, screen);
            }
            for (int dj = -d; dj <= d; dj++) {
                createChunk(i + d, j + dj, newChunks, screen);
                createChunk(i - d, j + dj, newChunks, screen);
            }
        }
        
        return newChunks;
    }
    
    /**
    * method: createChunk()
    * purpose: Create an empty chunk at a give i and j index and add it to the
    * screen and the world.
    **/
    private void createChunk(int i, int j, List<Chunk> newChunks, Screen screen) {
        if (world.getChunk(i, j) == null) {
            Chunk c = new Chunk(world, i, j);
            newChunks.add(c);
            world.addChunk(c);
            screen.addObject(c);
        }
    }
    
    /**
    * method: getNoise2d()
    * purpose: get 2d simplex noise using a given noise generator. shift the x
    * and z values by NOISE_OFFSET_X and NOISE_OFFSET_Z respectively.
    **/
    private double getNoise2d(SimplexNoise noiseGen, int x, int z) {
        return noiseGen.getNoise(x + NOISE_OFFSET_X, z + NOISE_OFFSET_Z);
    }
    
    /**
    * method: getNoise3d()
    * purpose: get 3d simplex noise using a given noise generator. shift the x
    * and z values by NOISE_OFFSET_X and NOISE_OFFSET_Z respectively.
    **/
    private double getNoise3d(SimplexNoise noiseGen, int x, int y, int z) {
        return noiseGen.getNoise(x + NOISE_OFFSET_X, y, z + NOISE_OFFSET_Z);
    }
    
    /**
    * method: fillChunkGenerateRandom()
    * purpose: fill a given chunk with randomly generated voxel data. Since all
    * chunks generated by the same world generator use the same SimplexNoise objects,
    * the generated world will be contiguous between them.
    **/
    public void fillChunkGenerateRandom(Chunk chunk) {
        // create thread timer
        ThreadMXBean threadTimer = ManagementFactory.getThreadMXBean();
        long start = threadTimer.getCurrentThreadCpuTime();
        
        /*
        The ordering of the blocks array is [y][x][z]. The reason for doing this
        is because the WorldGenerator was easier to design if we iterate first on
        the y-axis and then on the x and z axiis. If we iterate in the same way
        the array is structured, we get better cache locality and hopefully better
        performance.
        */
        VoxelType[][][] blocks = new VoxelType[CHUNK_H][CHUNK_S][CHUNK_S];
        
        // generate heights and biome for each xz position
        int[][] cellHeights = new int[CHUNK_S][CHUNK_S];
        Biome[][] biomes = new Biome[CHUNK_S][CHUNK_S];
        for (int x = 0; x < CHUNK_S; x++) {
            for (int z = 0; z < CHUNK_S; z++) {
                // computer x and z coords for the noise generator
                int noiseX = x + chunk.indexI * CHUNK_S;
                int noiseZ = z + chunk.indexJ * CHUNK_S;
                
                // compute local and region height factors
                double localHeightNoise = getNoise2d(noiseGenLocalHeight, noiseX, noiseZ) * NOISE_FACTOR_LOCAL_HEIGHT;
                double regionHeightNoise = 1 + getNoise2d(noiseGenRegionHeight, chunk.indexI, chunk.indexJ) * NOISE_FACTOR_REGION_HEIGHT;
                
                // multiply height factors
                double combinedHeightNoise = Math.abs(NOISE_FACTOR_HEIGHT * localHeightNoise * regionHeightNoise);
                
                // computer the height delta for this cell
                int heightDelta = (int) combinedHeightNoise;
                if (heightDelta > UPPER_LAYER_MAX_HEIGHT_DELTA) {
                    heightDelta = UPPER_LAYER_MAX_HEIGHT_DELTA;
                }
                if (heightDelta < 1) {
                    heightDelta = 1;
                }
                
                // compute the height of this cell
                int cellHeight = BEDROCK_HEIGHT + ROCK_LAYER_HEIGHT + heightDelta;
                if (cellHeight > CHUNK_H - 1 - WORLD_HEADROOM) {
                    cellHeight = CHUNK_H - 1 - WORLD_HEADROOM;
                }
                cellHeights[x][z] = cellHeight;
                
                
                // choose biome
                double biomeNoise = Math.abs(getNoise2d(noiseGenBiome, noiseX, noiseZ)) * 3;
                if (Math.abs(localHeightNoise) < 1 && Math.abs(regionHeightNoise) < 1 && Math.abs(combinedHeightNoise) < 1) {
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
        
        // loop over each vertical layer in this chunk
        for (int y = 0; y < CHUNK_H - WORLD_HEADROOM; y++) {
            if (y < BEDROCK_HEIGHT) {
                // lowest layer is bedrock
                generateFlatLayer(blocks, y, VoxelType.BEDROCK);
            } else if (y < BEDROCK_HEIGHT + ROCK_LAYER_HEIGHT) {
                // middle layers are rocky
                generateRockLayer(chunk, blocks, y);
            } else {
                // upper layers
                generateUpperLayer(chunk, blocks, y, cellHeights, biomes);
            }
            // yeild incase other threads need to do something important
            Thread.yield();
        }
        
        // once all the layers are generated, copy the data and mark the chunk as generated
        chunk.copyBlocks(blocks, 0, CHUNK_S, 0, CHUNK_H, 0, CHUNK_S);
        chunk.setGenerated();
        System.out.println("Generated " + chunk.indexI + "," + chunk.indexJ + " in " + (threadTimer.getCurrentThreadCpuTime() - start) / 1000000000.0);
    }
    
    /**
    * method: generateFlatLayer()
    * purpose: generate a flat layer filled with a single VoxelType.
    **/
    private void generateFlatLayer(VoxelType[][][] blocks, int y, VoxelType type) {
        for (int x = 0; x < CHUNK_S; x++) {
            Arrays.fill(blocks[y][x], type);
        }
    }
    
    /**
    * method: generateRockLayer()
    * purpose: generate a rocky underground layer
    **/
    private void generateRockLayer(Chunk chunk, VoxelType[][][] blocks, int y) {
        for (int x = 0; x < CHUNK_S; x++) {
            for (int z = 0; z < CHUNK_S; z++) {
                generateRockLayerSingle(chunk, blocks, x, y, z);
            }
        }
    }
    
    /**
    * method: generateRockLayerSingle()
    * purpose: generate a single cell of rocky underground. Use simple noise to
    * determine if this cell should be open space. Use Random() to generate ore
    * veins.
    **/
    private void generateRockLayerSingle(Chunk chunk, VoxelType[][][] blocks, int x, int y, int z) {
        int noiseX = x + chunk.indexI * CHUNK_S;
        int noiseZ = z + chunk.indexJ * CHUNK_S;
        double noise = (getNoise3d(noiseGenCavern, noiseX, y, noiseZ) + 1) / 2;
        boolean openSpace = (noise < 0.475);

        if (!openSpace) {
            blocks[y][x][z] = VoxelType.STONE;
            
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
    
    /**
    * method: generateUpperLayer()
    * purpose: generate an upper layer. Use the boiomes array to choose which
    * helper method to call.
    **/
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
    
    /**
    * method: generateCellNormalBiome()
    * purpose: generate a single cell of the normal biome
    **/
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
                blocks[y][x][z] = VoxelType.GRASS;
                geterateGrassFoliage(blocks, x, y, z);
            } else {
                blocks[y][x][z] = VoxelType.DIRT;
            }
        }
    }
    
    /**
    * method: generateCellWinterBiome()
    * purpose: generate a single cell of the winter biome
    **/
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
                        blocks[y][x][z] = VoxelType.SAND;
                        blocks[y+1][x][z] = VoxelType.SNOW;
                    } else if (v > .985) {
                        blocks[y][x][z] = VoxelType.ICE;
                    } else {
                        blocks[y][x][z] = VoxelType.ICE_GRASS;
                        blocks[y+1][x][z] = VoxelType.SNOW;
                        generateWinterFoliage(blocks, x, y, z);
                    }
                } else {
                    // high top layer
                    blocks[y][x][z] = VoxelType.ICE_GRASS;
                    blocks[y+1][x][z] = VoxelType.SNOW;
                    generateWinterFoliage(blocks, x, y, z);
                }
            } else {
                blocks[y][x][z] = VoxelType.DIRT;
            }
        }
    }
    
    /**
    * method: generateCellOceanBiome()
    * purpose: generate a single cell of the ocean biome
    **/
    private void generateCellOceanBiome(Chunk chunk, VoxelType[][][] blocks, int x, int y, int z, int cellHeight, int rockyCutoff) {
        int noiseX = x + chunk.indexI * CHUNK_S;
        int noiseZ = z + chunk.indexJ * CHUNK_S;
        double v = getNoise2d(noiseGenBlockType, noiseX, noiseZ);
        if (v > 0.45) {
            blocks[y][x][z] = VoxelType.SAND;
            if (y == cellHeight) {
                generateWetSandFoliage(blocks, x, y, z); 
            }
        } else {
            if (y == cellHeight) {
                blocks[y][x][z] = VoxelType.WATER;
            } else {
                blocks[y][x][z] = VoxelType.DIRT;
            }
        }
    }
    
    /**
    * method: generateCellDesertBiome()
    * purpose: generate a single cell of the desert biome
    **/
    private void generateCellDesertBiome(Chunk chunk, VoxelType[][][] blocks, int x, int y, int z, int cellHeight, int rockyCutoff) {
        blocks[y][x][z] = VoxelType.SAND;
        if (y == cellHeight) {
            generateDesertFoliage(blocks, x, y, z);
        }
    }
    
    /**
    * method: generateOreVein()
    * purpose: generate an ore vein at a given x,y,z location.
    **/
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
    
    /**
    * method: geterateGrassFoliage()
    * purpose: use Random() to sometimes add foliage to a given x, y, z position
    **/
    private void geterateGrassFoliage(VoxelType[][][] blocks, int x, int y, int z) {
        if (rand.nextDouble() < 0.05) 
            blocks[y+1][x][z] = VoxelType.TALL_GRASS;
        if (rand.nextDouble() < 0.0040)
            blocks[y+1][x][z] = VoxelType.RED_FLOWER;
        if (rand.nextDouble() < 0.0040)
            blocks[y+1][x][z] = VoxelType.YELLOW_FLOWER;
        if (rand.nextDouble() < 0.001)
            blocks[y+1][x][z] = VoxelType.PUMPKIN;
        if (rand.nextDouble() < 0.001)
            blocks[y+1][x][z] = VoxelType.RED_MUSHROOM;
        if (rand.nextDouble() < 0.001)
            blocks[y+1][x][z] = VoxelType.MUSHROOM;
        if (rand.nextDouble() < 0.0015)
            generateTree(blocks, x, y + 1, z, false);
    }
    
    /**
    * method: generateWetSandFoliage()
    * purpose: use Random() to sometimes add reeds to a given x, y, z position
    **/
    private void generateWetSandFoliage(VoxelType[][][] blocks, int x, int y, int z) {
        if (rand.nextDouble() < 0.005) {
            // generate reeds
            int max = 5;
            int min = 2;
            int height = rand.nextInt(max - min + 1) + min;
            for (int i = 1; i <= height; i++) {
                blocks[y+i][x][z] = VoxelType.REED;
            }
        }
    }
    
    /**
    * method: generateWetSandFoliage()
    * purpose: use Random() to add snow and sometimes trees to a given x, y, z position
    **/
    private void generateWinterFoliage(VoxelType[][][] blocks, int x, int y, int z) {
        blocks[y+1][x][z] = VoxelType.SNOW;
        if (rand.nextDouble() < 0.0015)
            generateTree(blocks, x, y + 1, z, true);
    }
    
    /**
    * method: generateDesertFoliage()
    * purpose: use Random() to add cacti to a given x, y, z position
    **/
    private void generateDesertFoliage(VoxelType[][][] blocks, int x, int y, int z) {
        if (rand.nextDouble() < 0.001) {
            // generate caci
            int max = 6;
            int min = 3;
            int height = rand.nextInt(max - min + 1) + min;
            for (int i = 1; i <= height; i++) {
                blocks[y+i][x][z] = VoxelType.CACTUS;
            }
        }
    }
    
    /**
    * method: generateTree()
    * purpose: Add a simple tree to the blocks array at a given x,y,z. Uses the
    * addBlocks method (which includes bounds checking) to place the blocks.
    **/
    private void generateTree(VoxelType[][][] blocks, int x, int y, int z, boolean snowy) {
        if (x == 0) {
            x++;
        } else if (x == CHUNK_S - 1) {
            x--;
        }
        
        if (z == 0) {
            z++;
        } else if (z == CHUNK_S - 1) {
            z--;
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
    * method: addBlocksSafe()
    * purpose: Add blocks of a given type to all the x,y,z positions in the
    * input float array. (x,y,z refer to indices of the blocks array)
    **/
    private void addBlocksSafe(VoxelType[][][] blocks, int[] blockCoords, VoxelType type) {
        for (int i = 0; i < blockCoords.length - 2; i+= 3) {
            int xx = blockCoords[i + 0];
            int yy = blockCoords[i + 1];
            int zz = blockCoords[i + 2];
            if (
                yy >= 0 && yy < blocks.length &&
                xx >= 0 && xx < blocks[yy].length &&
                zz >= 0 && zz < blocks[yy][xx].length
            ) {
                if (blocks[yy][xx][zz] != VoxelType.BEDROCK) {
                    blocks[yy][xx][zz] = type;
                }
            }
        }
    }
}
