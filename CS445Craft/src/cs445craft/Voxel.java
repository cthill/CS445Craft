/***************************************************************
* file: Voxel.java
* author: CS445 Group 42^3
* class: CS 445 – Computer Graphics
*
* assignment: Final Project
* date last modified: 10/28/2017
*
* purpose: This class is static and not meant to be instantiated.
* It contains an enum of all th VoxelTypes and has static methods
* for writing the vertices of voxel faces to a float buffer.
* 
****************************************************************/
package cs445craft;

public class Voxel {
    public static final int BLOCK_SIZE = 2;
    
    public static final int FACE_TOP = 0;
    public static final int FACE_BOTTOM = 1;
    public static final int FACE_FRONT = 2;
    public static final int FACE_BACK = 3;
    public static final int FACE_LEFT = 4;
    public static final int FACE_RIGHT = 5;
    
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
    
    /**
    * method: isTranslucent()
    * purpose: Check if a given VoxelType is translucent
    **/
    public static boolean isTranslucent(VoxelType v) {
        return (
            v == VoxelType.WATER ||
            v == VoxelType.GLASS ||
            v == VoxelType.ICE
        );
    }
    
    /**
    * method: isPartiallyTransparent()
    * purpose: Check if a given VoxelType has a partially transparent texture
    * (the screen uses GL_ALPHA_TEST to cut out fully transparent parts of textures).
    **/
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
    
    /**
    * method: isSolid()
    * purpose: Check if a given VoxelType is solid.
    **/
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
    
    /**
    * method: isBreakable()
    * purpose: Check if a given VoxelType is breakable.
    **/
    public static boolean isBreakable(VoxelType v) {
        return !(
            v == VoxelType.WATER ||
            v == VoxelType.BEDROCK ||
            v == VoxelType.LAVA
        );
    }
    
    /**
    * method: isMineThrough()
    * purpose: Check if a given VoxelType can be mined through.
    **/
    public static boolean isMineThrough(VoxelType v) {
        return (
            v == VoxelType.WATER ||
            v == VoxelType.LAVA
        );
    }
    
    /**
    * method: isCrossType()
    * purpose: Check if a given VoxelType is a cross shape instead of a cube.
    **/
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
    
    /**
    * method: breakIfSupportRemoved()
    * purpose: Check if a given VoxelType needs to be broken if its supporting
    * Voxel is also broken.
    **/
    public static boolean breakIfSupportRemoved(VoxelType v) {
        return (
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
    
    /**
    * method: writeFaceVertices()
    * purpose: Write the vertices for a single face at a given x, y, z position
    * to a given float buffer. Note, the x,y,z coordinates are in OpenGL space
    * 
    * This method checks if the Voxel is a cross type or not and calls the appropriate
    * helper method.
    **/
    public static void writeFaceVertices(float[] data, int writeIndex, int face, VoxelType voxelType, float x, float y, float z) {
        if (isCrossType(voxelType)) {
            writeFaceVerticesCross(data, writeIndex, face, voxelType, x, y, z);
        } else {
            writeFaceVerticesCube(data, writeIndex, face, voxelType, x, y, z);
        }
    }
    
    /**
    * method: writeFaceVerticesCross()
    * purpose: Write the vertices for a single face at a given x, y, z position
    * to a given float buffer. Checks which face is requested and calls the appropriate
    * helper method. This method is for the cross type.
    **/
    private static void writeFaceVerticesCross(float[] data, int writeIndex, int face, VoxelType voxelType, float x, float y, float z) {        
        float[] vertices;
        switch (face) {
            case FACE_FRONT:
                vertices = getFrontFaceCross(voxelType, x, y, z);
                break;
            case FACE_BACK:
                vertices = getBackFaceCross(voxelType, x, y, z);
                break;
            case FACE_LEFT:
                vertices = getLeftFaceCross(voxelType, x, y, z);
                break;
            case FACE_RIGHT:
                vertices = getRightFaceCross(voxelType, x, y, z);
                break;
            default:
                throw new RuntimeException("Unknown face");
        }
        
        System.arraycopy(vertices, 0, data, writeIndex, vertices.length);
    }

    /**
    * method: writeFaceVertices()
    * purpose: Write the vertices for a single face at a given x, y, z position
    * to a given float buffer. Checks which face is requested and calls the appropriate
    * helper method. This method is for the cube type.
    **/
    private static void writeFaceVerticesCube(float[] data, int writeIndex, int face, VoxelType voxelType, float x, float y, float z) {
        float[] vertices;
        switch (face) {
            case FACE_TOP:
                vertices = getTopFace(voxelType, x, y, z);
                break;
            case FACE_BOTTOM:
                vertices = getBottomFace(voxelType, x, y, z);
                break;
            case FACE_FRONT:
                vertices = getFrontFace(voxelType, x, y, z);
                break;
            case FACE_BACK:
                vertices = getBackFace(voxelType, x, y, z);
                break;
            case FACE_LEFT:
                vertices = getLeftFace(voxelType, x, y, z);
                break;
            case FACE_RIGHT:
                vertices = getRightFace(voxelType, x, y, z);
                break;
            default:
                throw new RuntimeException("Unknown face");
        }
        
        System.arraycopy(vertices, 0, data, writeIndex, vertices.length);
    }
    
    /**
    * method: getTopFace()
    * purpose: Get the vertices of the top face of a given VoxelType at a given
    * x,y,z location. This is for the cube shape.
    **/
    private static float[] getTopFace(VoxelType voxelType, float x, float y, float z) {
        float s = ((float) Voxel.BLOCK_SIZE) / 2;
        
        float e = 0.0f; //extra side offset (for things like cacti)
        float h = 0.0f; //extra top offset (for things like torches or snow)
        if (voxelType == VoxelType.CACTUS) {
            e = 0.125f;
        } else if (voxelType == VoxelType.SNOW) {
            h = Voxel.BLOCK_SIZE - 0.15f;
        }
        
        float offset = (2048f/16)/2048f;
        int[] t = getTextureCoords(voxelType);
        
        // vertex, texture, vertex, texture...
        return new float[] {
            // position
            // color
            // texture

            x + s, y + s - h, z + s,
            offset*(t[0] + 1), offset*(t[1] + 1),
            
            x - s, y + s - h, z + s,
            offset*(t[0] + 0), offset*(t[1] + 1),
            
            x - s, y + s - h, z - s,
            offset*(t[0] + 0), offset*(t[1] + 0),
            
            x + s, y + s - h, z - s,
            offset*(t[0] + 1), offset*(t[1] + 0)
        };
    }
    
    /**
    * method: getBottomFace()
    * purpose: Get the vertices of the bottom face of a given VoxelType at a given
    * x,y,z location. This is for the cube shape.
    **/
    private static float[] getBottomFace(VoxelType voxelType, float x, float y, float z) {
        float s = ((float) Voxel.BLOCK_SIZE) / 2;
        
        float e = 0.0f; //extra side offset (for things like cacti)
        float h = 0.0f; //extra top offset (for things like torches or snow)
        if (voxelType == VoxelType.CACTUS) {
            e = 0.125f;
        } else if (voxelType == VoxelType.SNOW) {
            h = Voxel.BLOCK_SIZE - 0.15f;
        }
        
        float offset = (2048f/16)/2048f;
        int[] t = getTextureCoords(voxelType);
        
        
        // vertex, texture, vertex, texture...
        return new float[] {
            // position
            // texture

            x + s, y - s, z - s,
            offset*(t[2] + 1), offset*(t[3] + 1),
            
            x - s, y - s, z - s,
            offset*(t[2] + 0), offset*(t[3] + 1),
            
            x - s, y - s, z + s,
            offset*(t[2] + 0), offset*(t[3] + 0),
            
            x + s, y - s, z + s,
            offset*(t[2] + 1), offset*(t[3] + 0),
        };
    }
    
    /**
    * method: getFrontFace()
    * purpose: Get the vertices of the front face of a given VoxelType at a given
    * x,y,z location. This is for the cube shape.
    **/
    private static float[] getFrontFace(VoxelType voxelType, float x, float y, float z) {
        float s = ((float) Voxel.BLOCK_SIZE) / 2;
        
        float e = 0.0f; //extra side offset (for things like cacti)
        float h = 0.0f; //extra top offset (for things like torches or snow)
        if (voxelType == VoxelType.CACTUS) {
            e = 0.125f;
        } else if (voxelType == VoxelType.SNOW) {
            h = Voxel.BLOCK_SIZE - 0.15f;
        }
        
        float offset = (2048f/16)/2048f;
        int[] t = getTextureCoords(voxelType);
        
        
        // vertex, texture, vertex, texture...
        return new float[] {
            // position
            // texture
            x + s, y + s - h, z - s + e,
            offset*(t[4] + 0), offset*(t[5] + 0),
            
            x - s, y + s - h, z - s + e,
            offset*(t[4] + 1), offset*(t[5] + 0),
            
            x - s, y - s, z - s + e,
            offset*(t[4] + 1), offset*(t[5] + 1),
            
            x + s, y - s, z - s + e,
            offset*(t[4] + 0), offset*(t[5] + 1),
        };
    }
    
    /**
    * method: getBackFace()
    * purpose: Get the vertices of the back face of a given VoxelType at a given
    * x,y,z location. This is for the cube shape.
    **/
    private static float[] getBackFace(VoxelType voxelType, float x, float y, float z) {
        float s = ((float) Voxel.BLOCK_SIZE) / 2;
        
        float e = 0.0f; //extra side offset (for things like cacti)
        float h = 0.0f; //extra top offset (for things like torches or snow)
        if (voxelType == VoxelType.CACTUS) {
            e = 0.125f;
        } else if (voxelType == VoxelType.SNOW) {
            h = Voxel.BLOCK_SIZE - 0.15f;
        }
        
        float offset = (2048f/16)/2048f;
        int[] t = getTextureCoords(voxelType);
        
        
        // vertex, texture, vertex, texture...
        return new float[] {
            // position
            // texture

            x + s, y - s, z + s - e,
            offset*(t[6] + 1), offset*(t[7] + 1),
            
            x - s, y - s, z + s - e,
            offset*(t[6] + 0), offset*(t[7] + 1),
            
            x - s, y + s - h, z + s - e,
            offset*(t[6] + 0), offset*(t[7] + 0),
            
            x + s, y + s - h, z + s - e,
            offset*(t[6] + 1), offset*(t[7] + 0)
        };
    }
    
    /**
    * method: getLeftFace()
    * purpose: Get the vertices of the left face of a given VoxelType at a given
    * x,y,z location. This is for the cube shape.
    **/
    private static float[] getLeftFace(VoxelType voxelType, float x, float y, float z) {
        float s = ((float) Voxel.BLOCK_SIZE) / 2;
        
        float e = 0.0f; //extra side offset (for things like cacti)
        float h = 0.0f; //extra top offset (for things like torches or snow)
        if (voxelType == VoxelType.CACTUS) {
            e = 0.125f;
        } else if (voxelType == VoxelType.SNOW) {
            h = Voxel.BLOCK_SIZE - 0.15f;
        }
        
        float offset = (2048f/16)/2048f;
        int[] t = getTextureCoords(voxelType);
        
        
        // vertex, texture, vertex, texture...
        return new float[] {
            // position
            // texture

            x - s + e, y + s - h, z - s,
            offset*(t[8] + 0), offset*(t[9] + 0),
            
            x - s + e, y + s - h, z + s,
            offset*(t[8] + 1), offset*(t[9] + 0),
            
            x - s + e, y - s, z + s,
            offset*(t[8] + 1), offset*(t[9] + 1),
            
            x - s + e, y - s, z - s,
            offset*(t[8] + 0), offset*(t[9] + 1)
        };
    }
    
    /**
    * method: getRightFace()
    * purpose: Get the vertices of the right face of a given VoxelType at a given
    * x,y,z location. This is for the cube shape.
    **/
    private static float[] getRightFace(VoxelType voxelType, float x, float y, float z) {
        float s = ((float) Voxel.BLOCK_SIZE) / 2;
        
        float e = 0.0f; //extra side offset (for things like cacti)
        float h = 0.0f; //extra top offset (for things like torches or snow)
        if (voxelType == VoxelType.CACTUS) {
            e = 0.125f;
        } else if (voxelType == VoxelType.SNOW) {
            h = Voxel.BLOCK_SIZE - 0.15f;
        }
        
        float offset = (2048f/16)/2048f;
        int[] t = getTextureCoords(voxelType);
        
        
        // vertex, texture, vertex, texture...
        return new float[] {
            // position
            // texture

            x + s - e, y + s - h, z + s,
            offset*(t[10] + 0), offset*(t[11] + 0),
            
            x + s - e, y + s - h, z - s,
            offset*(t[10] + 1), offset*(t[11] + 0),
            
            x + s - e, y - s, z - s,
            offset*(t[10] + 1), offset*(t[11] + 1),
            
            x + s - e, y - s, z + s,
            offset*(t[10] + 0), offset*(t[11] + 1)
        };
    }
    
    
    /**
    * method: getFrontFace()
    * purpose: Get the vertices of the front face of a given VoxelType at a given
    * x,y,z location. This is for the cross shape.
    **/
    private static float[] getFrontFaceCross(VoxelType voxelType, float x, float y, float z) {
        float s = ((float) Voxel.BLOCK_SIZE) / 2;

        float offset = (2048f/16)/2048f;
        int[] t = getTextureCoords(voxelType);
        
        
        // vertex, texture, vertex, texture...
        return new float[] {
            // position
            // texture
            x + s, y + s, z,
            offset*(t[4] + 0), offset*(t[5] + 0),
            
            x - s, y + s, z,
            offset*(t[4] + 1), offset*(t[5] + 0),
            
            x - s, y - s, z,
            offset*(t[4] + 1), offset*(t[5] + 1),
            
            x + s, y - s, z,
            offset*(t[4] + 0), offset*(t[5] + 1),
        };
    }
    
    /**
    * method: getBackFaceCross()
    * purpose: Get the vertices of the back face of a given VoxelType at a given
    * x,y,z location. This is for the cross shape.
    **/
    private static float[] getBackFaceCross(VoxelType voxelType, float x, float y, float z) {
        float s = ((float) Voxel.BLOCK_SIZE) / 2;
        
        float e = 0.0f; //extra side offset (for things like cacti)
        float h = 0.0f; //extra top offset (for things like torches or snow)
        if (voxelType == VoxelType.CACTUS) {
            e = 0.125f;
        } else if (voxelType == VoxelType.SNOW) {
            h = Voxel.BLOCK_SIZE - 0.15f;
        }
        
        float offset = (2048f/16)/2048f;
        int[] t = getTextureCoords(voxelType);
        
        
        // vertex, texture, vertex, texture...
        return new float[] {
            // position
            // texture

            x + s, y - s, z,
            offset*(t[6] + 1), offset*(t[7] + 1),
            
            x - s, y - s, z,
            offset*(t[6] + 0), offset*(t[7] + 1),
            
            x - s, y + s, z,
            offset*(t[6] + 0), offset*(t[7] + 0),
            
            x + s, y + s, z,
            offset*(t[6] + 1), offset*(t[7] + 0)
        };
    }
    
    /**
    * method: getLeftFaceCross()
    * purpose: Get the vertices of the left face of a given VoxelType at a given
    * x,y,z location. This is for the cross shape.
    **/
    private static float[] getLeftFaceCross(VoxelType voxelType, float x, float y, float z) {
        float s = ((float) Voxel.BLOCK_SIZE) / 2;
        
        float e = 0.0f; //extra side offset (for things like cacti)
        float h = 0.0f; //extra top offset (for things like torches or snow)
        if (voxelType == VoxelType.CACTUS) {
            e = 0.125f;
        } else if (voxelType == VoxelType.SNOW) {
            h = Voxel.BLOCK_SIZE - 0.15f;
        }
        
        float offset = (2048f/16)/2048f;
        int[] t = getTextureCoords(voxelType);
        
        
        // vertex, texture, vertex, texture...
        return new float[] {
            // position
            // texture

            x, y + s, z - s,
            offset*(t[8] + 0), offset*(t[9] + 0),
            
            x, y + s, z + s,
            offset*(t[8] + 1), offset*(t[9] + 0),
            
            x, y - s, z + s,
            offset*(t[8] + 1), offset*(t[9] + 1),
            
            x, y - s, z - s,
            offset*(t[8] + 0), offset*(t[9] + 1)
        };
    }
    
    /**
    * method: getRightFaceCross()
    * purpose: Get the vertices of the right face of a given VoxelType at a given
    * x,y,z location. This is for the cross shape.
    **/
    private static float[] getRightFaceCross(VoxelType voxelType, float x, float y, float z) {
        float s = ((float) Voxel.BLOCK_SIZE) / 2;
        
        float e = 0.0f; //extra side offset (for things like cacti)
        float h = 0.0f; //extra top offset (for things like torches or snow)
        if (voxelType == VoxelType.CACTUS) {
            e = 0.125f;
        } else if (voxelType == VoxelType.SNOW) {
            h = Voxel.BLOCK_SIZE - 0.15f;
        }
        
        float offset = (2048f/16)/2048f;
        int[] t = getTextureCoords(voxelType);
        
        
        // vertex, texture, vertex, texture...
        return new float[] {
            // position
            // texture

            x, y + s, z + s,
            offset*(t[10] + 0), offset*(t[11] + 0),
            
            x, y + s, z - s,
            offset*(t[10] + 1), offset*(t[11] + 0),
            
            x, y - s, z - s,
            offset*(t[10] + 1), offset*(t[11] + 1),
            
            x, y - s, z + s,
            offset*(t[10] + 0), offset*(t[11] + 1)
        };
    }
    
    /**
    * method: getTextureCoords()
    * purpose: Get the x and y texture coordinates for each face of a given VoxelType.
    **/
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
                topX = btmX = lftX = rhtX = fntX = bckX = 11;
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
}
