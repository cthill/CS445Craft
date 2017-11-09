/***************************************************************
* file: CS445Craft.java
* author: CS445 Group 42^3
* class: CS 445 – Computer Graphics
*
* assignment: Final Project
* date last modified: 10/08/2017
*
* purpose: This is the main class in the program. It instantiates
* the screen, the camera, and collects keyboard/mouse input
* 
****************************************************************/
package cs445craft;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class CS445Craft {

    /**
    * method: main
    * purpose: Start the main program by instantiating a Camera, a Screen,
    * and calling run().
    **/
    public static void main(String[] args) {
        Screen s;
        try {
            Camera c = new Camera(0,0,0);
            s = new Screen(640, 480, "CS445Craft", c);
            s.addObject(new Voxel(0,0,-2f, 1.0f));
            //s.addObject(new Tree(0,0,-2f));
            run(s, c);
        } catch (LWJGLException ex) {
            Logger.getLogger(CS445Craft.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
    * method: run
    * purpose: The main event loop of the game. Collects user input and requires
    * a Screen and Camera object.
    **/
    private static void run(Screen screen, Camera camera) {
        float speed = .05f;
        
        float dx = 0.0f;
        float dy = 0.0f;
        float dt = 0.0f; //length of frame
        float lastTime = 0.0f; // when the last frame was
        long time = 0;
        
        float mouseSens = 0.09f;
        Mouse.setGrabbed(true);
        
        
        while(true) {
            camera.incYaw(Mouse.getDX() * mouseSens);
            camera.incPitch(Mouse.getDY() * mouseSens);
            
            // listen for q key
            if (Keyboard.isKeyDown(Keyboard.KEY_Q)) {
                screen.close();
                break;
            }
            
            // listen for movement keys
            if (Keyboard.isKeyDown(Keyboard.KEY_W))
                camera.move(speed);
            if (Keyboard.isKeyDown(Keyboard.KEY_A))
                camera.strafe(-speed);
            if (Keyboard.isKeyDown(Keyboard.KEY_S))
                camera.move(-speed);
            if (Keyboard.isKeyDown(Keyboard.KEY_D))
                camera.strafe(speed);
            if (Keyboard.isKeyDown(Keyboard.KEY_SPACE))
                camera.elevate(-speed);
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT))
                camera.elevate(speed);

            // listen for close requested
            if (screen.getCloseRequested()) {
                break;
            }
            
            // draw frame
            screen.drawFrame();
        }
    }
}
