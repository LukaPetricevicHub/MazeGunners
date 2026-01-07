package de.tum.cit.fop.maze.level;

public class Tiles {

    // Declare variables for the maze
    public static final int Floor = 0;
    public static final int Wall = 1;
    public static final int Entry = 2;
    public static final int Exit = 3;
    public final int specialA = 4;
    public static final int specialB = 5;

    public static boolean isWall(int tile){
        return tile == Wall;
    }

    public static boolean isWalkable(int tile){
        return tile != Wall;
    }

}
