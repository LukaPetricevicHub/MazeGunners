package de.tum.cit.fop.maze.ai;
/**
 * Represents an entrance tile of a room.
 *
 * Stores the tile position of the entrance and the direction
 * pointing out of the room into a hallway.
 *
 * @param x tile x-coordinate of the entrance
 * @param y tile y-coordinate of the entrance
 * @param dirX x-direction pointing out of the room
 * @param dirY y-direction pointing out of the room
 */
public record RoomEntrance(int x, int y, int dirX, int dirY) {}

