/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cs445craft;

import java.util.ArrayList;
import java.util.List;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glTranslatef;

/**
 *
 * @author cthill
 */
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
    
    public void draw() {
        glPushMatrix();
        glTranslatef(x, y, z);
        for (Voxel block: blocks) {
            block.draw();
        }
        glPopMatrix();
    }
}
