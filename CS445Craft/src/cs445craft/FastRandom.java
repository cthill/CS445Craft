package cs445craft;

import java.util.Random;

public class FastRandom extends Random {
    private long seed;

    public FastRandom() {
        seed = System.nanoTime();
    }
    
    public FastRandom(int seed) {
        this.seed = seed;
    }
    
    protected int next(int nbits) {
        synchronized (this) {
            long x = this.seed;
            x ^= (x << 21);
            x ^= (x >>> 35);
            x ^= (x << 4);
            this.seed = x;
        
            x &= ((1L << nbits) -1);
            return (int) x;
        }
    }
}