/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cs445craft;

import cs445craft.Voxel.VoxelType;
import java.io.IOException;

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
        VoxelType[][][] world = new VoxelType[size * CHUNK_S][CHUNK_H][size * CHUNK_S];
                
        WorldGenerator.generateRandomWorld(world, size * CHUNK_S, CHUNK_H);
        
        // split work into CHUNK_S sized chunks
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                float offsetX = CHUNK_S * i;
                float offsetZ = CHUNK_S * j;
                
                Chunk c = new Chunk(offsetX * Voxel.BLOCK_SIZE, 0, offsetZ * Voxel.BLOCK_SIZE, CHUNK_S, CHUNK_H);
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
        return CHUNK_S * Voxel.BLOCK_SIZE * size;
    }
    
    public int worldPosToBlockIndex(float pos) {
        return -(int) (Math.round(pos / Voxel.BLOCK_SIZE));
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
