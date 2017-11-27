/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cs445craft;

/**
 *
 * @author cthill
 */
public class LightSource {
    public static final int MAX = 15;
    public static final int MIN = 3;
    public int x, y, z, value;
        
    public LightSource(int x, int y, int z, int value) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.value = value;
        
        if (this.value > MAX) {
            this.value = MAX;
        } else if (this.value < MIN) {
            this.value = MIN;
        }
    }
    
    public static float mapBrightness(int value) {
        if (value < MIN) {
            value = MIN;
        }
        return (float) value / (float) MAX;
    }
}
