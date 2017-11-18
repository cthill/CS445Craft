/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cs445craft;

import cs445craft.Voxel.VoxelType;
import java.util.Random;

/**
 *
 * @author cthill
 */
public class WorldGenerator {
    public static void generateRandomWorld(VoxelType[][][] blocks, int numBlocksXZ, int numBlocksY) {
        int maxDelta = 7;
        int headroom = 5;
        Random rand = new Random();
        SimplexNoise noiseGenHeight = new SimplexNoise(90, 0.45, rand.nextInt());
        SimplexNoise noiseGenType = new SimplexNoise(45, .1, rand.nextInt());
        
        for (int x = 0; x < numBlocksXZ; x++) {
            for (int z = 0; z < numBlocksXZ; z++) {
                double noise = noiseGenHeight.getNoise(x, z);
                int heightDelta =(int) (maxDelta * noise + maxDelta) - 1;
                if (heightDelta > maxDelta) {
                    heightDelta = maxDelta;
                }
                
                int maxHeight = numBlocksY - heightDelta - headroom;
                for (int y = 0; y < maxHeight; y++) {
                    Voxel.VoxelType type = Voxel.VoxelType.BEDROCK;
                    
                    if (y < maxHeight - 2) {
                        if (y >= 1 && y < numBlocksY / 2) {
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
                            
                        } else if (y >= numBlocksY / 2) {
                            type = Voxel.VoxelType.DIRT;
                        }
                    } else {
                        float v = (float) (noiseGenType.getNoise(x, y, z) + 1) / 2;
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
    }
    
    private static void generateOreVein(VoxelType[][][] blocks, int x, int y, int z, VoxelType v) {
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
    private static void generateTree(VoxelType[][][] blocks, int x, int y, int z) {
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
    private static void addBlocksSafe(VoxelType[][][] blocks, int[] blockCoords, VoxelType type) {
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
