/***************************************************************
* file: World.java
* author: CS445 Group 42^3
* class: CS 445 – Computer Graphics
*
* assignment: Final Project
* date last modified: 10/28/2017
*
* purpose: This class maintains references to every chunk that has
* been created and has useful methods for traversing chunk boundaries
* and translating OpenGL coordinates to voxel grid coordinates.
* 
****************************************************************/
package cs445craft;

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
    
    /**
    * method: addChunk()
    * purpose: Add a chunk to the world.
    **/
    public void addChunk(Chunk chunk) {
        chunksList.add(chunk);
        if (chunks.get(chunk.indexI) == null) {
            chunks.put(chunk.indexI, new HashMap<>());
        }
        chunks.get(chunk.indexI).put(chunk.indexJ, chunk);
        chunkToIndexI.put(chunk, chunk.indexI);
        chunkToIndexJ.put(chunk, chunk.indexJ);
    }
    
    /**
    * method: getChunk()
    * purpose: lookup the chunk at a given i and j index
    **/
    public Chunk getChunk(int i, int j) {
        if (chunks.get(i) == null) {
            return null;
        }
        return chunks.get(i).get(j);
    }
    
    /**
    * method: getChunks()
    * purpose: return all chunks
    **/
    public List<Chunk> getChunks() {
        return chunksList;
    }
    
    /**
    * method: findAdjacentChunk()
    * purpose: return the chunk adjacent to a given chunk in a given direction
    **/
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
    
    /**
    * method: findAllAdjacentChunks()
    * purpose: return all adjacent chunks to a given chunk
    **/
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
    
    /**
    * method: glCoordToVoxelGridLocation()
    * purpose: translate an OpenGL coordinate to a location on the Voxel grid.
    **/
    public int glCoordToVoxelGridLocation(float pos) {
        return (int) (Math.round(pos / Voxel.BLOCK_SIZE));
    }
    
    /**
    * method: voxelGridLocationToChunkNum()
    * purpose: translate a location on the Voxel grid to a chunk index
    **/
    public int voxelGridLocationToChunkNum(int index) {
        if (index < 0) {
            float idx = (float) index;
            float div = idx / CHUNK_S;
            int ceil = -(int) Math.ceil(-div);
            return ceil;
        }
        return index / CHUNK_S;
    }
    
    /**
    * method: voxelGridLocationToGlCoord()
    * purpose: translate a voxel grid location to a gl coordinate.
    **/
    public float voxelGridLocationToGlCoord(int index) {
        return (float) index * Voxel.BLOCK_SIZE;
    }
    
    /**
    * method: blockAt()
    * purpose: return the voxel at a given x,y,z location in OpenGL space. This
    * method will translate the OpenGL coordinates to a voxel grid location.
    **/
    public VoxelType blockAt(float x, float y, float z) {
        // translate gl coordinates to voxel grid location
        int xIndex = glCoordToVoxelGridLocation(x);
        int yIndex = glCoordToVoxelGridLocation(y);
        int zIndex = glCoordToVoxelGridLocation(z);
        
        // find the chunk at the voxel grid location
        int i = voxelGridLocationToChunkNum(xIndex);
        int j = voxelGridLocationToChunkNum(zIndex);

        // lookup appropriate block in that chunk
        Chunk c = getChunk(i, j);
        if (c != null) {
            return c.blockAt(Math.floorMod(xIndex, CHUNK_S), yIndex, Math.floorMod(zIndex, CHUNK_S));
        }
        
        return null;
    }
    
    /**
    * method: solidBlockAt()
    * purpose: return the voxel at a given x,y,z location in OpenGL space if it
    * is solid. This method will translate the OpenGL coordinates to a voxel grid
    * location.
    **/
    public VoxelType solidBlockAt(float x, float y, float z) {
        // translate gl coordinates to voxel grid location
        int xIndex = glCoordToVoxelGridLocation(x);
        int yIndex = glCoordToVoxelGridLocation(y);
        int zIndex = glCoordToVoxelGridLocation(z);
        
        // find the chunk at the voxel grid location
        int i = voxelGridLocationToChunkNum(xIndex);
        int j = voxelGridLocationToChunkNum(zIndex);
        
        Chunk c = getChunk(i, j);
        if (c != null) {
            return c.solidBlockAt(Math.floorMod(xIndex, CHUNK_S), yIndex, Math.floorMod(zIndex, CHUNK_S));
        }
        
        return null;
    }
    
    /**
    * method: depthAt()
    * purpose: return the depth at a given x,y,z location in OpenGL space. This
    * method will translate the OpenGL coordinates to a voxel grid location.
    **/
    public float depthAt(float x, float y, float z) {
        // translate gl coordinates to voxel grid location
        int xIndex = glCoordToVoxelGridLocation(x);
        int yIndex = glCoordToVoxelGridLocation(y);
        int zIndex = glCoordToVoxelGridLocation(z);
        
        // find the chunk at the voxel grid location
        int i = voxelGridLocationToChunkNum(xIndex);
        int j = voxelGridLocationToChunkNum(zIndex);
        
        // lookup appropriate block in that chunk
        Chunk c = getChunk(i, j);
        if (c != null) {
            return voxelGridLocationToGlCoord(c.depthAt(Math.floorMod(xIndex, CHUNK_S), yIndex, Math.floorMod(zIndex, CHUNK_S)));
        }
        
        return 0.0f;
    }
    
    /**
    * method: removeBlock()
    * purpose: remove the block at a given x,y,z location in OpenGL space. This
    * method will translate the OpenGL coordinates to a voxel grid location.
    **/
    public void removeBlock(float x, float y, float z) {
        // translate gl coordinates to voxel grid location
        int xIndex = glCoordToVoxelGridLocation(x);
        int yIndex = glCoordToVoxelGridLocation(y);
        int zIndex = glCoordToVoxelGridLocation(z);
        
        // find the chunk at the voxel grid location
        int i = voxelGridLocationToChunkNum(xIndex);
        int j = voxelGridLocationToChunkNum(zIndex);
        
        // remove appropriate block in that chunk
        Chunk c = getChunk(i, j);
        if (c != null) {
            c.breakBlock(Math.floorMod(xIndex, CHUNK_S), yIndex, Math.floorMod(zIndex, CHUNK_S));
        }
    }
}
