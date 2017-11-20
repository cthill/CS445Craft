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
        ICE
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
            v == VoxelType.REED
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
            v == VoxelType.TALL_GRASS
        );
    }
    
    public static boolean isBreakable(VoxelType v) {
        return !(
            v == VoxelType.WATER ||
            v == VoxelType.BEDROCK
        );
    }
    
    public static boolean isMineThrough(VoxelType v) {
        return (
            v == VoxelType.WATER
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
    
    public static int[] getTexture(VoxelType v) {
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
    
}
