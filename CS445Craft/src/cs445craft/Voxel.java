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
        DIAMOND
    }
    
    public static boolean isTranslucent(VoxelType v) {
        return (
            v == VoxelType.WATER ||
            v == VoxelType.GLASS
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
        int btmX, topX, fntX, bckX, lftX, rhtX;
        int btmY, topY, fntY, bckY, lftY, rhtY;
        
        switch (v) {
            case GRASS:
                btmX = 2;
                btmY = 9;
                topX = 2;
                topY = 0;
                lftX = rhtX = bckX = fntX = 3;
                lftY = rhtY = bckY = fntY = 0;
                break;
            case SAND:
                btmX = topX = lftX = rhtX = fntX = bckX = 2;
                btmY = topY = lftY = rhtY = fntY = bckY = 1;
                break;
            case WATER:
                btmX = topX = lftX = rhtX = fntX = bckX = 13;
                btmY = topY = lftY = rhtY = fntY = bckY = 12;
                break;
            case DIRT:
                btmX = topX = lftX = rhtX = fntX = bckX = 2;
                btmY = topY = lftY = rhtY = fntY = bckY = 0;
                break;
            case STONE:
                btmX = topX = lftX = rhtX = fntX = bckX = 1;
                btmY = topY = lftY = rhtY = fntY = bckY = 0;
                break;
            case TRUNK:
                btmX = topX = 5;
                btmY = topY = 1;
                lftX = rhtX = bckX = fntX = 4;
                lftY = rhtY = bckY = fntY = 1;
                break;
            case LEAVES:
                btmX = topX = lftX = rhtX = fntX = bckX = 10;
                btmY = topY = lftY = rhtY = fntY = bckY = 1;
                break;
            case BEDROCK:
                btmX = topX = lftX = rhtX = fntX = bckX = 1;
                btmY = topY = lftY = rhtY = fntY = bckY = 1;
                break;
            case GLASS:
                btmX = topX = lftX = rhtX = fntX = bckX = 1;
                btmY = topY = lftY = rhtY = fntY = bckY = 3;
                break;
             case RED_FLOWER:
                btmX = topX = lftX = rhtX = fntX = bckX = 12;
                btmY = topY = lftY = rhtY = fntY = bckY = 0;
                break;
            case YELLOW_FLOWER:
                btmX = topX = lftX = rhtX = fntX = bckX = 13;
                btmY = topY = lftY = rhtY = fntY = bckY = 0;
                break;
            case RED_MUSHROOM:
                btmX = topX = lftX = rhtX = fntX = bckX = 12;
                btmY = topY = lftY = rhtY = fntY = bckY = 1;
                break;
            case MUSHROOM:
                btmX = topX = lftX = rhtX = fntX = bckX = 13;
                btmY = topY = lftY = rhtY = fntY = bckY = 1;
                break;
            case TALL_GRASS:
                btmX = topX = lftX = rhtX = fntX = bckX = 9;
                btmY = topY = lftY = rhtY = fntY = bckY = 5;
                break;
            case REED:
                btmX = topX = lftX = rhtX = fntX = bckX = 9;
                btmY = topY = lftY = rhtY = fntY = bckY = 4;
                break;
            case COAL:
                btmX = topX = lftX = rhtX = fntX = bckX = 2;
                btmY = topY = lftY = rhtY = fntY = bckY = 2;
                break;
            case IRON:
                btmX = topX = lftX = rhtX = fntX = bckX = 1;
                btmY = topY = lftY = rhtY = fntY = bckY = 2;
                break;
            case GOLD:
                btmX = topX = lftX = rhtX = fntX = bckX = 0;
                btmY = topY = lftY = rhtY = fntY = bckY = 2;
                break;
            case DIAMOND:
                btmX = topX = lftX = rhtX = fntX = bckX = 2;
                btmY = topY = lftY = rhtY = fntY = bckY = 3;
                break;
            default:
                btmX = topX = lftX = rhtX = fntX = bckX = 11;
                btmY = topY = lftY = rhtY = fntY = bckY = 1;
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
