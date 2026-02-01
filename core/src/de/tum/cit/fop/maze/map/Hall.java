package de.tum.cit.fop.maze.map;

import java.awt.Point;

/**
 * Represents a straight hallway connecting two points in the maze grid.
 *
 * <p>The hallway is defined by its two endpoints and is assumed to be
 * either horizontal or vertical. Diagonal halls are not supported.</p>
 *
 * @param a start point of the hallway
 * @param b end point of the hallway
 */
public record Hall(Point a, Point b) { }
