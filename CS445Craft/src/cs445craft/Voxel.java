/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cs445craft;

import static org.lwjgl.opengl.GL11.*;

/**
 *
 * @author cthill
 */
public class Voxel extends Drawable {
    protected float r = .5f;
    
    public Voxel(float x, float y, float z) {
        super(x,y,z);
    }
    
    public Voxel(float x, float y, float z, float r) {
        super(x,y,z);
        this.r = r;
    }
    
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
