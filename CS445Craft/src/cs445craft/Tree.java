/***************************************************************
* file: Tree.java
* author: CS445 Group 42^3
* class: CS 445 – Computer Graphics
*
* assignment: Final Project
* date last modified: 10/08/2017
*
* purpose: This class extends the Drawable class and uses the
* Voxel class to draw a tree.
* 
****************************************************************/
package cs445craft;

import java.util.ArrayList;
import java.util.List;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glTranslatef;

public class Tree extends Drawable {
    protected List<Voxel> blocks;
    
    public Tree(float x, float y, float z) {
        super(x, y, z);
        // create voxels to build tree
        // TODO: allow for parameters like size and height
        blocks = new ArrayList<>();
        blocks.add(new Voxel(0, -2, 0));
        blocks.add(new Voxel(0, -1, 0));
        blocks.add(new Voxel(0, 0, 0));
        blocks.add(new Voxel(0, 1, 0));
        blocks.add(new Voxel(0, 2, 0));
        blocks.add(new Voxel(0, 3, 0));
        blocks.add(new Voxel(0, 4, 0));
        
        blocks.add(new Voxel(0, 3, 1));
        blocks.add(new Voxel(0,3, -1));
        blocks.add(new Voxel(1,3, 0));
        blocks.add(new Voxel(-1,3, 0));
        blocks.add(new Voxel(1,3, 1));
        blocks.add(new Voxel(1,3, -1));
        blocks.add(new Voxel(-1,3, 1));
        blocks.add(new Voxel(-1,3, -1));
    }
    
    /**
    * method: draw
    * purpose: Use the predefined list of Voxel objects to render a Tree.
    **/
    public void draw() {
        glPushMatrix();
        glTranslatef(x, y, z);
        for (Voxel block: blocks) {
            block.draw();
        }
        glPopMatrix();
    }
}
