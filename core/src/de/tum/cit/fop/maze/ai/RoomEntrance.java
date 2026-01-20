package de.tum.cit.fop.maze.ai;

public record RoomEntrance(
        int x,
        int y,
        int dirX,
        int dirY
) {
    // dirX / dirY point TOWARDS the hall
}

