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

import static org.lwjgl.opengl.GL11.*;

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
    * method: draw
    * purpose: use GL_QUADS to render a cube centered at (x,y,z) with side length
    * equal to 2r.
    **/
    public void draw() {
        glPushMatrix();
        glTranslatef(x, y, z);
        glBegin(GL_QUADS);
        
        // top face
	glColor3f(1.0f, 0.0f, 0.0f);
	glVertex3f(-r, r, r);
	glVertex3f(r, r, r);
	glVertex3f(r, r, -r);
	glVertex3f(-r, r, -r);
        
        // bottom face
        glColor3f(1.0f, 1.0f, 0.0f);
	glVertex3f(r, -r, -r);
	glVertex3f(-r, -r, -r);
	glVertex3f(-r, -r, r);
	glVertex3f(r, -r, r);
 
	// front face
	glColor3f(0.0f, 1.0f, 0.0f);
	glVertex3f(r, -r, r);
	glVertex3f(r, r, r);
	glVertex3f(-r, r, r);
	glVertex3f(-r, -r, r);
        
        // back face
        glColor3f(0.0f, 1.0f, 1.0f);
	glVertex3f(-r, r, -r);
	glVertex3f(-r, -r, -r);
	glVertex3f(r, -r, -r);
	glVertex3f(r, r, -r);
 
	// right face
	glColor3f(0.0f, 0.0f, 1.0f);
	glVertex3f(r, r, -r);
	glVertex3f(r, r, r);
	glVertex3f(r, -r, r);
	glVertex3f(r, -r, -r);
        
        // left face
	glColor3f(1.0f, 0.0f, 1.0f);
	glVertex3f(-r, -r, r);
	glVertex3f(-r, -r, -r);
	glVertex3f(-r, r, -r);
	glVertex3f(-r, r, r);
        
        glEnd();
        glPopMatrix();
    }
}
