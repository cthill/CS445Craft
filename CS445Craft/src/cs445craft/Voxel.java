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
public class Voxel {
    public static final int BLOCK_SIZE = 2;
    public static enum VoxelType {
        GRASS,
        SAND,
        WATER,
        DIRT,
        STONE,
        TRUNK,
        LEAVES,
        BEDROCK,
        GLASS,
        RED_FLOWER,
        YELLOW_FLOWER,
        RED_MUSHROOM,
        MUSHROOM,
        TALL_GRASS,
        REED,
        COAL,
        IRON,
        GOLD,
        DIAMOND,
        ICE,
        ICE_GRASS,
        DARK_TRUNK,
        CACTUS,
        PUMPKIN,
        SNOW,
        LAVA,
        COBBLE_STONE,
        SAND_STONE
    }
    
    public static boolean isTranslucent(VoxelType v) {
        return (
            v == VoxelType.WATER ||
            v == VoxelType.GLASS ||
            v == VoxelType.ICE
        );
    }
    
    public static boolean isPartiallyTransparent(VoxelType v) {
        return (
            v == VoxelType.LEAVES ||
            v == VoxelType.RED_FLOWER ||
            v == VoxelType.YELLOW_FLOWER ||
            v == VoxelType.RED_MUSHROOM ||
            v == VoxelType.MUSHROOM ||
            v == VoxelType.TALL_GRASS ||
            v == VoxelType.REED ||
            v == VoxelType.CACTUS ||
            v == VoxelType.SNOW
        );
    }
    
    public static boolean isSeeTrough(VoxelType v) {
        return isTranslucent(v) || isPartiallyTransparent(v);
    }
    
    public static boolean isSolid(VoxelType v) {
        return !(
            v == VoxelType.WATER ||
            v == VoxelType.RED_FLOWER ||
            v == VoxelType.YELLOW_FLOWER ||
            v == VoxelType.RED_MUSHROOM ||
            v == VoxelType.MUSHROOM ||
            v == VoxelType.TALL_GRASS ||
            v == VoxelType.SNOW ||
            v == VoxelType.LAVA
        );
    }
    
    public static boolean isBreakable(VoxelType v) {
        return !(
            v == VoxelType.WATER ||
            v == VoxelType.BEDROCK ||
            v == VoxelType.LAVA
        );
    }
    
    public static boolean isMineThrough(VoxelType v) {
        return (
            v == VoxelType.WATER ||
            v == VoxelType.LAVA
        );
    }
    
    public static boolean isCrossType(VoxelType v) {
        return (
            v == VoxelType.RED_FLOWER ||
            v == VoxelType.YELLOW_FLOWER ||
            v == VoxelType.RED_MUSHROOM ||
            v == VoxelType.MUSHROOM ||
            v == VoxelType.TALL_GRASS ||
            v == VoxelType.REED
        );
    }
    
    private static int[] getTextureCoords(VoxelType v) {
        int topX, btmX, fntX, bckX, lftX, rhtX;
        int topY, btmY, fntY, bckY, lftY, rhtY;
        
        switch (v) {
            case GRASS:
                topX = 2;
                topY = 9;
                btmX = 2;
                btmY = 0;
                lftX = rhtX = bckX = fntX = 3;
                lftY = rhtY = bckY = fntY = 0;
                break;
            case SAND:
                topX = btmX = lftX = rhtX = fntX = bckX = 2;
                topY = btmY = lftY = rhtY = fntY = bckY = 1;
                break;
            case WATER:
                topX = btmX = lftX = rhtX = fntX = bckX = 13;
                topY = btmY = lftY = rhtY = fntY = bckY = 12;
                break;
            case DIRT:
                topX = btmX = lftX = rhtX = fntX = bckX = 2;
                topY = btmY = lftY = rhtY = fntY = bckY = 0;
                break;
            case STONE:
                topX = btmX = lftX = rhtX = fntX = bckX = 1;
                topY = btmY = lftY = rhtY = fntY = bckY = 0;
                break;
            case TRUNK:
                topX = btmX = 5;
                topY = btmY = 1;
                lftX = rhtX = bckX = fntX = 4;
                lftY = rhtY = bckY = fntY = 1;
                break;
            case LEAVES:
                topX = btmX = lftX = rhtX = fntX = bckX = 10;
                topY = btmY = lftY = rhtY = fntY = bckY = 1;
                break;
            case BEDROCK:
                topX = btmX = lftX = rhtX = fntX = bckX = 1;
                topY = btmY = lftY = rhtY = fntY = bckY = 1;
                break;
            case GLASS:
                topX = btmX = lftX = rhtX = fntX = bckX = 1;
                topY = btmY = lftY = rhtY = fntY = bckY = 3;
                break;
             case RED_FLOWER:
                topX = btmX = lftX = rhtX = fntX = bckX = 12;
                topY = btmY = lftY = rhtY = fntY = bckY = 0;
                break;
            case YELLOW_FLOWER:
                topX = btmX = lftX = rhtX = fntX = bckX = 13;
                topY = btmY = lftY = rhtY = fntY = bckY = 0;
                break;
            case RED_MUSHROOM:
                topX = btmX = lftX = rhtX = fntX = bckX = 12;
                topY = btmY = lftY = rhtY = fntY = bckY = 1;
                break;
            case MUSHROOM:
                topX = btmX = lftX = rhtX = fntX = bckX = 13;
                topY = btmY = lftY = rhtY = fntY = bckY = 1;
                break;
            case TALL_GRASS:
                topX = btmX = lftX = rhtX = fntX = bckX = 9;
                topY = btmY = lftY = rhtY = fntY = bckY = 5;
                break;
            case REED:
                topX = btmX = lftX = rhtX = fntX = bckX = 9;
                topY = btmY = lftY = rhtY = fntY = bckY = 4;
                break;
            case COAL:
                topX = btmX = lftX = rhtX = fntX = bckX = 2;
                topY = btmY = lftY = rhtY = fntY = bckY = 2;
                break;
            case IRON:
                topX = btmX = lftX = rhtX = fntX = bckX = 1;
                topY = btmY = lftY = rhtY = fntY = bckY = 2;
                break;
            case GOLD:
                topX = btmX = lftX = rhtX = fntX = bckX = 0;
                topY = btmY = lftY = rhtY = fntY = bckY = 2;
                break;
            case DIAMOND:
                topX = btmX = lftX = rhtX = fntX = bckX = 2;
                topY = btmY = lftY = rhtY = fntY = bckY = 3;
                break;
            case ICE:
                topX = btmX = lftX = rhtX = fntX = bckX = 3;
                topY = btmY = lftY = rhtY = fntY = bckY = 4;
                break;
            case ICE_GRASS:
                topX = 2;
                topY = 4;
                btmX = 2;
                btmY = 0;
                lftX = rhtX = bckX = fntX = 4;
                lftY = rhtY = bckY = fntY = 4;
                break;
            case DARK_TRUNK:
                topX = btmX = 5;
                topY = btmY = 1;
                lftX = rhtX = bckX = fntX = 4;
                lftY = rhtY = bckY = fntY = 7;
                break;
            case CACTUS:
                topX = btmX = 5;
                topY = btmY = 4;
                lftX = rhtX = bckX = fntX = 6;
                lftY = rhtY = bckY = fntY = 4;
                break;
            case PUMPKIN:
                topX = 6;
                topY = 6;
                lftX = rhtX = bckX = btmX = 6;
                lftY = rhtY = bckY = btmY = 7;
                fntX = 7;
                fntY = 7;
                break;
            case SNOW:
                topX = btmX = lftX = rhtX = fntX = bckX = 2;
                topY = btmY = lftY = rhtY = fntY = bckY = 4;
                break;
            case LAVA:
                topX = btmX = lftX = rhtX = fntX = bckX = 15;
                topY = btmY = lftY = rhtY = fntY = bckY = 15;
                break;
            case COBBLE_STONE:
                topX = btmX = lftX = rhtX = fntX = bckX = 0;
                topY = btmY = lftY = rhtY = fntY = bckY = 1;
                break;
            case SAND_STONE:
                topX = 0;
                topY = 11;
                btmX = 0;
                btmY = 13;
                lftX = rhtX = bckX = fntX = 0;
                lftY = rhtY = bckY = fntY = 12;
                break;
            default:
                topX = btmX = lftX = rhtX = fntX = bckX = 11;
                topY = btmY = lftY = rhtY = fntY = bckY = 1;
                break;
        }
        
        return new int[] {
            topX, topY,
            btmX, btmY,
            fntX, fntY,
            bckX, bckY,
            lftX, lftY,
            rhtX, rhtY
        };
    }
    
    public static int getTextureVertices(float[] buff, int startIndex, boolean[] faceVisible, VoxelType v) {
        if (isCrossType(v)) {
            faceVisible = new boolean[] { false, false, true, true, true, true };
        }
        return getTextureVerticesCube(buff, startIndex, faceVisible, v);
    }
    
    private static int getTextureVerticesCube(float[] buff, int startIndex, boolean[] faceVisible, VoxelType v) {
        int quadsWritten = 0;
        float offset = (2048f/16)/2048f;
        int floatsPerFace = 2 * 4;
        int[] t = getTextureCoords(v);
        
        // top
        if (faceVisible[0]) {
            System.arraycopy(new float[] {
                offset*(t[0] + 1), offset*(t[1] + 1),
                offset*(t[0] + 0), offset*(t[1] + 1),
                offset*(t[0] + 0), offset*(t[1] + 0),
                offset*(t[0] + 1), offset*(t[1] + 0)
            }, 0, buff, startIndex, floatsPerFace);
            startIndex += floatsPerFace;
            quadsWritten++;
        }
        
        // bottom
        if (faceVisible[1]) {
            System.arraycopy(new float[] {
                offset*(t[2] + 1), offset*(t[3] + 1),
                offset*(t[2] + 0), offset*(t[3] + 1),
                offset*(t[2] + 0), offset*(t[3] + 0),
                offset*(t[2] + 1), offset*(t[3] + 0)
            }, 0, buff, startIndex, floatsPerFace);
            startIndex += floatsPerFace;
            quadsWritten++;
        }
        
        // front
        if (faceVisible[2]) {
            System.arraycopy(new float[] {
                offset*(t[4] + 0), offset*(t[5] + 0),
                offset*(t[4] + 1), offset*(t[5] + 0),
                offset*(t[4] + 1), offset*(t[5] + 1),
                offset*(t[4] + 0), offset*(t[5] + 1)
            }, 0, buff, startIndex, floatsPerFace);
            startIndex += floatsPerFace;
            quadsWritten++;
        }
        
        // back
        if (faceVisible[3]) {
            System.arraycopy(new float[] {
                offset*(t[6] + 1), offset*(t[7] + 1),
                offset*(t[6] + 0), offset*(t[7] + 1),
                offset*(t[6] + 0), offset*(t[7] + 0),
                offset*(t[6] + 1), offset*(t[7] + 0)
            }, 0, buff, startIndex, floatsPerFace);
            startIndex += floatsPerFace;
            quadsWritten++;
        }
        
        // left
        if (faceVisible[4]) {
            System.arraycopy(new float[] {
                offset*(t[8] + 0), offset*(t[9] + 0),
                offset*(t[8] + 1), offset*(t[9] + 0),
                offset*(t[8] + 1), offset*(t[9] + 1),
                offset*(t[8] + 0), offset*(t[9] + 1)
            }, 0, buff, startIndex, floatsPerFace);
            startIndex += floatsPerFace;
            quadsWritten++;
        }
        
        // right
        if (faceVisible[5]) {
            System.arraycopy(new float[] {
                offset*(t[10] + 0), offset*(t[11] + 0),
                offset*(t[10] + 1), offset*(t[11] + 0),
                offset*(t[10] + 1), offset*(t[11] + 1),
                offset*(t[10] + 0), offset*(t[11] + 1)
            }, 0, buff, startIndex, floatsPerFace);
            startIndex += floatsPerFace;
            quadsWritten++;
        }
        
        return quadsWritten * floatsPerFace;
    }
    
    public static int getVertices(float[] buff, int startIndex, boolean[] faceVisible, VoxelType v, float x, float y, float z) {
        if (isCrossType(v)) {
            return getVerticesCross(buff, startIndex, v, x, y, z);
        } else {
            return getVerticesCube(buff, startIndex, faceVisible, v, x, y, z);
        }
    }
    
    private static int getVerticesCube(float[] buff, int startIndex, boolean[] faceVisible, VoxelType v, float x, float y, float z) {
        int quadsAdded = 0;
        float s = ((float) Voxel.BLOCK_SIZE) / 2;
        int floatsPerFace = 3 * 4;
        
        float e = 0.0f; //extra side offset (for things like cacti)
        float h = 0.0f; //extra top offset (for things like torches or snow)
        if (v == VoxelType.CACTUS) {
            e = 0.125f;
        } else if (v == VoxelType.SNOW) {
            h = Voxel.BLOCK_SIZE - 0.10f;
            faceVisible[1] = false;
        }
        
        // TOP QUAD
        if (faceVisible[0]) {
            System.arraycopy(new float[] {
                x + s, y + s - h, z + s,
                x - s, y + s - h, z + s,
                x - s, y + s - h, z - s,
                x + s, y + s - h, z - s
            }, 0, buff, startIndex, floatsPerFace);
            startIndex += floatsPerFace;
            quadsAdded++;
        }
        
        // BOTTOM QUAD
        if (faceVisible[1]) {
            System.arraycopy(new float[] {
                x + s, y - s, z - s,
                x - s, y - s, z - s,
                x - s, y - s, z + s,
                x + s, y - s, z + s
            }, 0, buff, startIndex, floatsPerFace);
            startIndex += floatsPerFace;
            quadsAdded++;
        }
        
        // FRONT QUAD
        if (faceVisible[2]) {
            System.arraycopy(new float[] {
                x + s, y + s - h, z - s + e,
                x - s, y + s - h, z - s + e,
                x - s, y - s, z - s + e,
                x + s, y - s, z - s + e
            }, 0, buff, startIndex, floatsPerFace);
            startIndex += floatsPerFace;
            quadsAdded++;
        }
        
        // BACK QUAD
        if (faceVisible[3]) {
            System.arraycopy(new float[] {
                x + s, y - s, z + s - e,
                x - s, y - s, z + s - e,
                x - s, y + s - h, z + s - e,
                x + s, y + s - h, z + s - e
            }, 0, buff, startIndex, floatsPerFace);
            startIndex += floatsPerFace;
            quadsAdded++;
        }
        
        // LEFT QUAD
        if (faceVisible[4]) {
            System.arraycopy(new float[] {
                x - s + e, y + s - h, z - s,
                x - s + e, y + s - h, z + s,
                x - s + e, y - s, z + s,
                x - s + e, y - s, z - s
            }, 0, buff, startIndex, floatsPerFace);
            startIndex += floatsPerFace;
            quadsAdded++;
        }
        
        // RIGHT QUAD
        if (faceVisible[5]) {
            System.arraycopy(new float[] {
                x + s - e, y + s - h, z + s,
                x + s - e, y + s - h, z - s,
                x + s - e, y - s, z - s,
                x + s - e, y - s, z + s
            }, 0, buff, startIndex, floatsPerFace);
            startIndex += floatsPerFace;
            quadsAdded++;
        }
    
        // return number of floats written
        return quadsAdded * floatsPerFace;
    }
    
    private static int getVerticesCross(float[] buff, int startIndex, VoxelType v, float x, float y, float z) {
        int quadsAdded = 4;
        float s = ((float) Voxel.BLOCK_SIZE) / 2;
        int floatsPerFace = 3 * 4;
        
        System.arraycopy(new float[] {
            // FRONT QUAD
            x + s, y + s, z,
            x - s, y + s, z,
            x - s, y - s, z,
            x + s, y - s, z,
        
            // BACK QUAD
            x + s, y - s, z,
            x - s, y - s, z,
            x - s, y + s, z,
            x + s, y + s, z,
        
            // LEFT QUAD
            x, y + s, z - s,
            x, y + s, z + s,
            x, y - s, z + s,
            x, y - s, z - s,
        
            // RIGHT QUAD
            x, y + s, z + s,
            x, y + s, z - s,
            x, y - s, z - s,
            x, y - s, z + s
        }, 0, buff, startIndex, floatsPerFace * 4);
        
        return floatsPerFace * quadsAdded;
    }
}
