/***************************************************************
* file: Voxel.java
* author: CS445 Group 42^3
* class: CS 445 – Computer Graphics
*
* assignment: Final Project
* date last modified: 10/08/2017
*
* purpose: This class defines a Voxel at a fixed x,y, and z position.
* It uses GL_QUADS to render a cube centered at that point with
* side length s = 2r
* 
****************************************************************/
package cs445craft;

public class Voxel {
    protected Type type;
    
    public enum Type {
        GRASS,
        SAND,
        WATER,
        DIRT,
        STONE,
        TRUNK,
        LEAVES,
        BEDROCK
    }
    
    public Voxel(boolean active, Type type) {
        this.type = type;
    }
    
    public int getID() {
        return type.ordinal();
    }
}
