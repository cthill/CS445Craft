/***************************************************************
* file: Screen.java
* author: CS445 Group 42^3
* class: CS 445 – Computer Graphics
*
* assignment: Final Project
* date last modified: 10/28/2017
*
* purpose: This class is responsible for managing the OpenGL window
* and maintains a list of 'Drawable' objects that need to be
* rendered on each frame. It requires a Camera object so the lookThrough method
* can be called.
* 
****************************************************************/
package cs445craft;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import static org.lwjgl.opengl.GL11.*;
import org.lwjgl.util.glu.GLU;

public class Screen {
    private static float DRAW_DIST = Chunk.CHUNK_S * Voxel.BLOCK_SIZE * 6.5f;
    private final int width, height;
    private float r, g, b;
    private final Camera camera;
    private final List<Drawable> objects;
    
    public Screen(int width, int height, String title, Camera camera) throws LWJGLException {
        this.width = width;
        this.height = height;
        this.camera = camera;
        objects = new ArrayList<>();
        r = 1.0f;
        g = 1.0f;
        b = 1.0f;
        
        // create window
        Display.setFullscreen(false);
        Display.setDisplayMode(new DisplayMode(width, height));
        Display.setTitle(title);
        Display.create();
        
        // setup some opengl config
        glClearColor(0.5f, 0.85f, 1.0f, 1.0f);
        glEnable(GL_TEXTURE_2D);
        glAlphaFunc(GL_GREATER, 0.5f);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glFrontFace(GL_CW);      
    }
    
    /**
    * method: addObject()
    * purpose: add an object that satisfies the Drawable interface. Object will
    * be drawn to the screen on each frame.
    **/
    public void addObject(Drawable object) {
        if (!objects.contains(object)) {
            objects.add(object);
        }
    }
    
    /**
    * method: addObject()
    * purpose: add a collection of objects that satisfy the Drawable interface.
    * Objects will be drawn to the screen on each frame.
    **/
    public void addObjects(Collection< ? extends Drawable> coll) {
        Collection< ? extends Drawable> filtered = coll.stream().filter(object -> !objects.contains(object)).collect(Collectors.toList());
        objects.addAll(filtered);
    }
    
    /**
    * method: setTintColor()
    * purpose: Set the screen tint color. Normally the color is pure white. Sometimes,
    * it may be desirable to tint the screen a color (like blue when the player is
    * underwater).
    **/
    public void setTintColor(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }
    
    /**
    * method: incDrawDist()
    * purpose: Add a value to the DRAW_DIST
    **/
    public void incDrawDist(float d) {
        DRAW_DIST += d;
    }
    
    /**
    * method: drawFrame()
    * purpose: Draw one frame by looping through the list of Drawable objects
    * and rendering each one.
    **/
    public void drawFrame() {
        // render 3D objects
        render3D();
        // render 2d hud objects
        render2D();
        
        // update display and sync to 60 hz.
        Display.update();
        Display.sync(60);
    }
    
    /**
    * method: render3D()
    * purpose: Render 3D objects to the display. Rendering is a 4 step process.
    * 
    * Step 1: setup OpenGL config, look through camera, etc.
    * 
    * Step 2: Sort all opaque 3D objects within the draw distance. Sort order is
    *  front to back, based on distance from the camera's x,y,z. After the sort,
    *  render the objects to the screen.
    * 
    * Step 3: Sort all translucent 3D objects within the draw distance. Sort order
    * is back to front, based on distance from the camera's x,y,z. After the sort,
    * render the objects to the screen. Translucent objects must be rendered in
    * after the opaque objects, and must be rendered back to front. Otherwise there
    * will be alpha blending artifacts
    **/
    private void render3D() {
        // setup 3d config
        glEnable(GL_DEPTH_TEST); // enable z-buffering
        glEnable(GL_CULL_FACE); // don't draw occluded faces
        glDisable(GL_BLEND); // turn off alpha blending
        glMatrixMode(GL_PROJECTION); // setup projection matrix
        glLoadIdentity();
        GLU.gluPerspective(100.0f, (float) width / (float) height, 0.05f, DRAW_DIST);
        glMatrixMode(GL_MODELVIEW);
        glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        
        // 3d draw prepare
        glColor3f(r, g, b);
        glLoadIdentity();
        glPushMatrix();
        camera.lookThrough();
        
        // 3d draw solid objects
        // enable alpha test so we can cut out transparent parts of textures (like leaves or flowers)
        glEnable(GL_ALPHA_TEST);
        // sort opaque objects front to back
        objects.sort(Comparator.comparing(object -> ((Drawable) object).distanceTo(camera.x, camera.y, camera.z)).reversed());
        for (Drawable object: objects) {
            if (object.distanceTo(camera.x, object.getY(), camera.z) <= DRAW_DIST) {
                object.activate();
                object.draw();
            } else {
                object.deactivate();
            }
        }
        glDisable(GL_ALPHA_TEST);
        
        // 3d draw translucent objects
        glEnable(GL_BLEND);
        glDisable(GL_CULL_FACE);
        glDepthMask(false);
        // sort translucent objects back to front
        objects.sort(Comparator.comparing(object -> object.distanceTo(camera.x, camera.y, camera.z)));
        for (Drawable object: objects) {
            if (object.distanceTo(camera.x, object.getY(), camera.z) <= DRAW_DIST) {
                object.drawTranslucent();
            }
        }
        glDepthMask(true);
        
        // 3d draw finish
        glPopMatrix();
        glColor3f(1.0f,1.0f,1.0f);
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
    }
    
    /**
    * method: render2D()
    * purpose: Render 2d objects to the display. Currently, this method just uses
    * GL_POINTS to draw crosshairs.
    **/
    private void render2D() {        
        // switch to 2d mode for hud draw
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(-width/2, width/2, -height/2, height/2, 1, -1);
        glMatrixMode(GL_MODELVIEW);
        glClear(GL_DEPTH_BUFFER_BIT);
        
        // draw crosshairs
        glLoadIdentity();
        glPushMatrix();
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
    }
    
    /**
    * method: getCloseRequested()
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
    * method: close()
    * purpose: close the window
    **/
    public void close() {
        Display.destroy();
    }
}
