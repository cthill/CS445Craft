/***************************************************************
* file: CS445Craft.java
* author: CS445 Group 42^3
* class: CS 445 – Computer Graphics
*
* assignment: Final Project
* date last modified: 10/28/2017
*
* purpose: This is the main class in the program. It instantiates
* the Game object and catches exceptions.
* 
****************************************************************/
package cs445craft;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.LWJGLException;

public class CS445Craft {    
    /**
    * method: main
    * purpose: Start the main program by instantiating a Game object and calling
    * run().
    **/
    public static void main(String[] args) {
        Screen s;
        try {
            Game game = new Game();
            game.run();
        } catch (LWJGLException ex) {
            Logger.getLogger(CS445Craft.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CS445Craft.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
