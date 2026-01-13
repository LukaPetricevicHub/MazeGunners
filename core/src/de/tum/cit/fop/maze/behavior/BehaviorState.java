package de.tum.cit.fop.maze.behavior;

import de.tum.cit.fop.maze.enemies.Enemy;
import de.tum.cit.fop.maze.map.Maze;

public interface BehaviorState {

    // Returns behavior category
    BehaviorStateType getType();


    // Executes behavior logic
    void execute(Enemy enemy, float deltaTime, Maze maze);
}
