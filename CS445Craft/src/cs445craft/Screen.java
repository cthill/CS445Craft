/***************************************************************
* file: Screen.java
* author: CS445 Group 42^3
* class: CS 445 – Computer Graphics
*
* assignment: Final Project
* date last modified: 10/08/2017
*
* purpose: This class is responsible for managing the OpenGL window
* and maintains a list of 'Drawable' objects that need to be
* rendered on each frame. It requires a Camera object.
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
        
        // init OpenGL
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        glEnable(GL_TEXTURE_2D);
        glEnableClientState (GL_TEXTURE_COORD_ARRAY);
        glEnable(GL_DEPTH_TEST);
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        GLU.gluPerspective(100.0f, (float) width / (float) height, 0.05f, 300.0f);
        glMatrixMode(GL_MODELVIEW);
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
        glLoadIdentity();
        camera.lookThrough();
        
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        
        for (Drawable object: objects) {
            object.draw();
        }

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
