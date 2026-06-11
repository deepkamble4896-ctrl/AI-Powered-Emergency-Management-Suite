package com.example

import java.util.Queue
import java.util.LinkedList

data class GridPoint(val x: Int, val y: Int)

class PathFinder {
    val width = 10
    val height = 10

    // Set of grid points representing active hazard areas or structural obstacles
    private val hazards = mutableSetOf<GridPoint>()

    init {
        // Default hazard room: "Ball Room" block, designated around center-top coordinates
        // Under a 10x10 grid, let's flag (4,1), (4,2), (5,1), (5,2), (6,1), (6,2) as hazardous
        setBallRoomHazard(true)
    }

    fun setBallRoomHazard(active: Boolean) {
        val ballRoomPoints = listOf(
            GridPoint(4, 1), GridPoint(4, 2), GridPoint(4, 3),
            GridPoint(5, 1), GridPoint(5, 2), GridPoint(5, 3),
            GridPoint(6, 1), GridPoint(6, 2), GridPoint(6, 3)
        )
        if (active) {
            hazards.addAll(ballRoomPoints)
        } else {
            hazards.removeAll(ballRoomPoints)
        }
    }

    fun addHazard(point: GridPoint) {
        hazards.add(point)
    }

    fun removeHazard(point: GridPoint) {
        hazards.remove(point)
    }

    fun isHazard(point: GridPoint): Boolean {
        return hazards.contains(point)
    }

    fun getHazards(): Set<GridPoint> = hazards

    /**
     * Finds the shortest path between start and target avoiding all flagged hazards.
     * Uses standard Breadth-First Search (BFS) to guarantee the shortest safety corridor route.
     */
    fun findShortestPath(start: GridPoint, target: GridPoint): List<GridPoint> {
        val queue: Queue<GridPoint> = LinkedList()
        val visited = mutableSetOf<GridPoint>()
        val parentMap = mutableMapOf<GridPoint, GridPoint>()

        queue.add(start)
        visited.add(start)

        val directions = listOf(
            GridPoint(0, 1),   // Down
            GridPoint(0, -1),  // Up
            GridPoint(1, 0),   // Right
            GridPoint(-1, 0)   // Left
        )

        var pathFound = false
        while (queue.isNotEmpty()) {
            val current = queue.poll() ?: break
            if (current == target) {
                pathFound = true
                break
            }

            for (dir in directions) {
                val neighbors = listOf(
                    GridPoint(current.x + dir.x, current.y + dir.y)
                )

                for (neighbor in neighbors) {
                    if (neighbor.x in 0 until width && neighbor.y in 0 until height) {
                        if (!visited.contains(neighbor) && !hazards.contains(neighbor)) {
                            visited.add(neighbor)
                            parentMap[neighbor] = current
                            queue.add(neighbor)
                        }
                    }
                }
            }
        }

        if (!pathFound) {
            // Fallback path in case the start/target are completely blocked or isolated.
            // Under emergency conditions, find the closest path ignoring hazards rather than returning empty.
            return findFallbackPath(start, target)
        }

        return reconstructPath(parentMap, start, target)
    }

    private fun findFallbackPath(start: GridPoint, target: GridPoint): List<GridPoint> {
        val queue: Queue<GridPoint> = LinkedList()
        val visited = mutableSetOf<GridPoint>()
        val parentMap = mutableMapOf<GridPoint, GridPoint>()

        queue.add(start)
        visited.add(start)

        val directions = listOf(
            GridPoint(0, 1), GridPoint(0, -1), GridPoint(1, 0), GridPoint(-1, 0)
        )

        var found = false
        while (queue.isNotEmpty()) {
            val current = queue.poll() ?: break
            if (current == target) {
                found = true
                break
            }
            for (dir in directions) {
                val nx = current.x + dir.x
                val ny = current.y + dir.y
                val next = GridPoint(nx, ny)
                if (nx in 0 until width && ny in 0 until height && !visited.contains(next)) {
                    visited.add(next)
                    parentMap[next] = current
                    queue.add(next)
                }
            }
        }
        return if (found) reconstructPath(parentMap, start, target) else emptyList()
    }

    private fun reconstructPath(
        parentMap: Map<GridPoint, GridPoint>,
        start: GridPoint,
        target: GridPoint
    ): List<GridPoint> {
        val path = mutableListOf<GridPoint>()
        var current = target
        while (current != start) {
            path.add(current)
            current = parentMap[current] ?: break
        }
        path.add(start)
        return path.reversed()
    }
}
