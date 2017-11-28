/***************************************************************
* file: Drawable.java
* author: CS445 Group 42^3
* class: CS 445 – Computer Graphics
*
* assignment: Final Project
* date last modified: 10/27/2017
*
* purpose: This abstract class defines the interface for any
* object that will be drawn to the screen
* 
****************************************************************/
package cs445craft;

public abstract class Drawable {    
    private boolean active;
    
    /**
    * method: draw()
    * purpose: abstract method to draw the opaque part of the object to the screen.
    **/
    public abstract void draw();
    
    /**
    * method: drawTranslucent()
    * purpose: abstract method to draw the translucent part of the object to the
    * screen.
    **/
    public abstract void drawTranslucent();
    
    /**
    * method: distanceTo()
    * purpose: abstract method to calculate the distance from this object to a
    * given point in 3d space (OpenGL space, relative to the OpenGL origin).
    **/
    public abstract float distanceTo(float x, float y, float z);
    
    /**
    * method: getX()
    * purpose: abstract method to get this objects X coordinate in OpenGL space,
    * relative to the OpenGL origin.
    **/
    public abstract float getX();
    
    /**
    * method: getY()
    * purpose: abstract method to get this objects Y coordinate in OpenGL space,
    * relative to the OpenGL origin.
    **/
    public abstract float getY();
    
    /**
    * method: getZ()
    * purpose: abstract method to get this objects Z coordinate in OpenGL space,
    * relative to the OpenGL origin.
    **/
    public abstract float getZ();
    
    /**
    * method: activate()
    * purpose: Set the active flag to true. Called when the object is within the
    * screens render distance.
    **/
    public void activate() {
        active = true;
    };
    
    /**
    * method: deactivate()
    * purpose: Set the active flag to false. Called when the object is outsize the
    * screens render distance.
    **/
    public void deactivate() {
        active = false;
    };
    
    
    /**
    * method: getActive()
    * purpose: Return the state of the active flag.
    **/
    public boolean getActive() {
        return active;
    }
}
