/***************************************************************
* file: Drawable.java
* author: CS445 Group 42^3
* class: CS 445 – Computer Graphics
*
* assignment: Final Project
* date last modified: 10/08/2017
*
* purpose: This abstract class defines the interface for any
* object that will be drawn to the screen
* 
****************************************************************/
package cs445craft;

public abstract class Drawable {    
    private boolean active;
    
    /**
    * method: draw
    * purpose: abstract method to draw the object to the screen. Call on each
    * frame.
    **/
    public abstract void draw();
    public abstract void drawTranslucent();
    
    public abstract float distanceTo(float x, float y, float z);
    
    public abstract float getX();
    public abstract float getY();
    public abstract float getZ();
    
    public void activate() {
        active = true;
    };
    
    public void deactivate() {
        active = false;
    };
    
    public boolean getActive() {
        return active;
    }
}
