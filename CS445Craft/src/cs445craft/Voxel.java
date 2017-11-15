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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.lwjgl.opengl.GL11.*;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureLoader;

public class Voxel extends Drawable {
    protected float r = .5f;
    
    public Voxel(float x, float y, float z) {
        super(x,y,z);
    }
    
    public Voxel(float x, float y, float z, float r) {
        super(x,y,z);
        this.r = r;
    }
    /**
     * method: loadTexture
     * purpose: Use the TextureLoader class to load in the texture from a file
     */
    public Texture loadTexture(String key) {
        try {
            return TextureLoader.getTexture("png", new FileInputStream(new File("res/" + key + ".png")));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Screen.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Screen.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    /**
    * method: draw
    * purpose: use GL_QUADS to render a cube centered at (x,y,z) with side length
    * equal to 2r.
    **/
    public void draw() {
        Texture terrain = loadTexture("terrain");
        glPushMatrix();
        glTranslatef(x, y, z);
        
        terrain.bind();
        
        glBegin(GL_QUADS);
                
        // top face
//	glColor3f(1.0f, 0.0f, 0.0f);
        glTexCoord2f(0,0);
	glVertex3f(-r, r, r);
        glTexCoord2f(0,1);
	glVertex3f(r, r, r);
        glTexCoord2f(1,1);
	glVertex3f(r, r, -r);
        glTexCoord2f(1,0);
	glVertex3f(-r, r, -r);
        
        // bottom face
//        glColor3f(1.0f, 1.0f, 0.0f);
        glTexCoord2f(0,0);
	glVertex3f(r, -r, -r);
        glTexCoord2f(0,1);
	glVertex3f(-r, -r, -r);
        glTexCoord2f(1,1);
	glVertex3f(-r, -r, r);
        glTexCoord2f(1,0);
	glVertex3f(r, -r, r);
        
 
	// front face
//	glColor3f(0.0f, 1.0f, 0.0f);
        glTexCoord2f(0,0);
	glVertex3f(r, -r, r);
        glTexCoord2f(0,1);
	glVertex3f(r, r, r);
        glTexCoord2f(1,1);
	glVertex3f(-r, r, r);
        glTexCoord2f(1,0);
	glVertex3f(-r, -r, r);
        
        
        // back face
//        glColor3f(0.0f, 1.0f, 1.0f);
        glTexCoord2f(0,0);
	glVertex3f(-r, r, -r);
        glTexCoord2f(0,1);
	glVertex3f(-r, -r, -r);
        glTexCoord2f(1,1);
	glVertex3f(r, -r, -r);
        glTexCoord2f(1,0);
	glVertex3f(r, r, -r);
        
 
	// right face
//	glColor3f(0.0f, 0.0f, 1.0f);
        glTexCoord2f(0,0);
	glVertex3f(r, r, -r);
        glTexCoord2f(0,1);
	glVertex3f(r, r, r);
        glTexCoord2f(1,1);
	glVertex3f(r, -r, r);
        glTexCoord2f(1,0);
	glVertex3f(r, -r, -r);
        
        
        // left face
//	glColor3f(1.0f, 0.0f, 1.0f);
        glTexCoord2f(0,0);
	glVertex3f(-r, -r, r);
        glTexCoord2f(0,1);
	glVertex3f(-r, -r, -r);
        glTexCoord2f(1,1);
	glVertex3f(-r, r, -r);
        glTexCoord2f(1,0);
	glVertex3f(-r, r, r);
        
        
        glEnd();
        glPopMatrix();
    }
    
    /**
     * method: chooseTexture
     * purpose: based on block location (which should be passed in as a parameter)
     * the the proper texture coordinates for the relevant block will be applied
     */
    public void chooseTexture() {
        // texture logic
    }
}
