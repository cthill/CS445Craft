/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cs445craft;

import static cs445craft.Chunk.CHUNK_H;
import static cs445craft.Chunk.CHUNK_S;
import cs445craft.Voxel.VoxelType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class World {    
    private final List<Chunk> chunksList;
    private final Map<Integer, Map<Integer, Chunk>> chunks;
    private final Map<Chunk, Integer> chunkToIndexI;
    private final Map<Chunk, Integer> chunkToIndexJ;
    
    public World() {
        chunksList = new ArrayList<>();
        chunks = new HashMap<>();        
        chunkToIndexI = new HashMap<>();
        chunkToIndexJ = new HashMap<>();
    }
    
    public void addChunk(Chunk chunk) {
        chunksList.add(chunk);
        if (chunks.get(chunk.i) == null) {
            chunks.put(chunk.i, new HashMap<>());
        }
        chunks.get(chunk.i).put(chunk.j, chunk);
        chunkToIndexI.put(chunk, chunk.i);
        chunkToIndexJ.put(chunk, chunk.j);
    }
    
    public Chunk getChunk(int i, int j) {
        if (chunks.get(i) == null) {
            return null;
        }
        return chunks.get(i).get(j);
    }
    
    public List<Chunk> getChunks() {
        return chunksList;
    }
    
    public Chunk findAdjacentChunk(Chunk thisChunk, int xDir, int zDir) {
        Integer i = chunkToIndexI.get(thisChunk);
        Integer j = chunkToIndexJ.get(thisChunk);
        if (i == null || j == null) {
            return null;
        }
        
        if (xDir < 0)
            i--;
        else if (xDir > 0)
            i++;
        
        if (zDir < 0)
            j--;
        else if (zDir > 0)
            j++;
        
        return getChunk(i, j);
    }
    
    public Set<Chunk> findAllAdjacentChunks(Chunk c) {
        Set<Chunk> adjacent = new HashSet<>();
        Integer i = chunkToIndexI.get(c);
        Integer j = chunkToIndexJ.get(c);
        if (i == null || j == null) {
            return adjacent;
        }
        
        adjacent.add(getChunk(i - 1, j));
        adjacent.add(getChunk(i + 1, j));
        adjacent.add(getChunk(i, j - 1));
        adjacent.add(getChunk(i, j + 1));
        adjacent.remove(null);
        
        return adjacent;
    }
    
    public int worldPosToBlockIndex(float pos) {
        return (int) (Math.round(pos / Voxel.BLOCK_SIZE));
    }
    
    public int blockIndexToChunkNum(int index) {
        if (index < 0) {
            float idx = (float) index;
            float div = idx / CHUNK_S;
            int ceil = -(int) Math.ceil(-div);
            return ceil;
        }
        return index / CHUNK_S;
    }
    
    public float blockIndexToWorldPos(int index) {
        return (float) index * Voxel.BLOCK_SIZE;
    }
    
    public VoxelType blockAt(float x, float y, float z) {
        int xIndex = worldPosToBlockIndex(x);
        int yIndex = worldPosToBlockIndex(y);
        int zIndex = worldPosToBlockIndex(z);
        
        int i = blockIndexToChunkNum(xIndex);
        int j = blockIndexToChunkNum(zIndex);

        Chunk c = getChunk(i, j);
        if (c != null) {
            return c.blockAt(Math.floorMod(xIndex, CHUNK_S), yIndex, Math.floorMod(zIndex, CHUNK_S));
        }
        
        return null;
    }
    
    public VoxelType solidBlockAt(float x, float y, float z) {
        int xIndex = worldPosToBlockIndex(x);
        int yIndex = worldPosToBlockIndex(y);
        int zIndex = worldPosToBlockIndex(z);
        
        int i = blockIndexToChunkNum(xIndex);
        int j = blockIndexToChunkNum(zIndex);
        
        Chunk c = getChunk(i, j);
        if (c != null) {
            return c.solidBlockAt(Math.floorMod(xIndex, CHUNK_S), yIndex, Math.floorMod(zIndex, CHUNK_S));
        }
        
        return null;
    }
    
    public float depthAt(float x, float y, float z) {
        int xIndex = worldPosToBlockIndex(x);
        int yIndex = worldPosToBlockIndex(y);
        int zIndex = worldPosToBlockIndex(z);
        
        int i = blockIndexToChunkNum(xIndex);
        int j = blockIndexToChunkNum(zIndex);
        
        Chunk c = getChunk(i, j);
        if (c != null) {
            return blockIndexToWorldPos(c.depthAt(Math.floorMod(xIndex, CHUNK_S), yIndex, Math.floorMod(zIndex, CHUNK_S)));
        }
        
        return 0.0f;
    }
    
    public void removeBlock(float x, float y, float z) {
        int xIndex = worldPosToBlockIndex(x);
        int yIndex = worldPosToBlockIndex(y);
        int zIndex = worldPosToBlockIndex(z);
        
        int i = blockIndexToChunkNum(xIndex);
        int j = blockIndexToChunkNum(zIndex);
        
        Chunk c = getChunk(i, j);
        if (c != null) {
            c.removeBlock(Math.floorMod(xIndex, CHUNK_S), yIndex, Math.floorMod(zIndex, CHUNK_S));
        }
    }
    
    public void swapMeshes() {
        for (Chunk c: getChunks()) {
            c.swapMesh();
        }
    }
}
