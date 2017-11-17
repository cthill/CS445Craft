/***************************************************************
* file: Screen.java
* author: CS445 Group 42^3
* class: CS 445 – Computer Graphics
*
* assignment: Final Project
* date last modified: 10/16/2017
*
* purpose: This class is responsible for managing the OpenGL window
* and maintains a list of 'Drawable' objects that need to be
* rendered on each frame. It requires a Camera object so the lookThrough method
* can be called.
* 
****************************************************************/
package cs445craft;

import java.util.ArrayList;
import java.util.List;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import static org.lwjgl.opengl.GL11.*;
import org.lwjgl.util.glu.GLU;

public class Screen {
    protected int width, height;
    protected String title;
    protected Camera camera;
    protected List<Drawable> objects;
    
    public Screen(int width, int height, String title, Camera camera) throws LWJGLException {
        this.width = width;
        this.height = height;
        this.title = title;
        this.camera = camera;
        objects = new ArrayList<>();
        
        // create window
        Display.setFullscreen(false);
        Display.setDisplayMode(new DisplayMode(width, height));
        Display.setTitle(title);
        Display.create();
        
        glClearColor(0.5f, 0.85f, 1.0f, 0.0f);
        glEnable(GL_TEXTURE_2D);
        glEnableClientState (GL_TEXTURE_COORD_ARRAY);
        glEnableClientState(GL_VERTEX_ARRAY);
        glFrontFace(GL_CW);

        glLoadIdentity();
        glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);
    }

    /**
    * method: addObject
    * purpose: add an object that satisfies the Drawable interface. Object will
    * be drawn to the screen on each frame.
    **/
    public void addObject(Drawable object) {
        objects.add(object);
    }
    
    /**
    * method: drawFrame
    * purpose: Draw one frame by looping through the list of Drawable objects
    * and rendering each one.
    **/
    public void drawFrame() {
        // 3d draw
        
        glColor3f(1.0f,1.0f,1.0f);
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        GLU.gluPerspective(100.0f, (float) width / (float) height, 0.05f, 300.0f);
        glMatrixMode(GL_MODELVIEW);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        
        
        camera.lookThrough();
        
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        
        for (Drawable object: objects) {
            object.draw();
        }
        glPopMatrix();
        
        // switch to 2d mode for hud draw
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(-width/2, width/2, -height/2, height/2, 1, -1);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glClear(GL_DEPTH_BUFFER_BIT);
        
        // draw crosshairs
        glBindTexture(GL_TEXTURE_2D, 0);
        glColor3f(1.0f, 1.0f, 1.0f);
        glPointSize(2);
        glBegin(GL_POINTS);
            glVertex2f(0,0);
            glVertex2f(2,0);
            glVertex2f(4,0);
            glVertex2f(-2, 0);
            glVertex2f(-4, 0);
            glVertex2f(0, 2);
            glVertex2f(0,4);
            glVertex2f(0,-2);
            glVertex2f(0,-4);
        glEnd();
        glPopMatrix();
        
        // update display and sync to 60 hz.
        Display.update();
        Display.sync(60);
    }
    
    /**
    * method: getCloseRequested
    * purpose: Returns true if the user requested to close the window.
    **/
    public boolean getCloseRequested() {
        if (Display.isCloseRequested()) {
            Display.destroy();
            return true;
        }
        
        return false;
    }
    
    /**
    * method: close
    * purpose: close the window
    **/
    public void close() {
        Display.destroy();
    }
}
