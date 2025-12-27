package com.kizitonwose.calendar.core

import java.time.Duration

/**
 * Extension functions and utilities for working with time blocks.
 *
 * @since 3.0.0
 */

// Overlap detection functions

/**
 * Checks if a collection of time blocks contains any overlapping blocks.
 *
 * @return true if any two blocks overlap, false otherwise.
 */
public fun List<TimeBlock>.hasOverlaps(): Boolean {
    if (size <= 1) return false
    for (i in 0 until size - 1) {
        for (j in i + 1 until size) {
            if (this[i].overlapsWith(this[j])) {
                return true
            }
        }
    }
    return false
}

/**
 * Returns a list of all overlapping time block pairs.
 *
 * @return List of pairs where each pair represents two overlapping blocks.
 */
public fun List<TimeBlock>.findOverlaps(): List<Pair<TimeBlock, TimeBlock>> {
    val overlaps = mutableListOf<Pair<TimeBlock, TimeBlock>>()
    if (size <= 1) return overlaps

    for (i in 0 until size - 1) {
        for (j in i + 1 until size) {
            if (this[i].overlapsWith(this[j])) {
                overlaps.add(this[i] to this[j])
            }
        }
    }
    return overlaps
}

/**
 * Sorts time blocks chronologically by start time.
 *
 * @return A new list with blocks sorted by startTime (ascending).
 */
public fun List<TimeBlock>.sortedByStartTime(): List<TimeBlock> {
    return sortedBy { it.startTime }
}

/**
 * Filters time blocks by type.
 *
 * @param type The type to filter by.
 * @return A new list containing only blocks of the specified type.
 */
public fun List<TimeBlock>.filterByType(type: TimeBlockType): List<TimeBlock> {
    return filter { it.blockType == type }
}

/**
 * Filters time blocks by multiple types.
 *
 * @param types The types to include.
 * @return A new list containing blocks matching any of the specified types.
 */
public fun List<TimeBlock>.filterByTypes(types: Set<TimeBlockType>): List<TimeBlock> {
    return filter { it.blockType in types }
}

/**
 * Calculates total duration across all time blocks.
 *
 * @return The sum of all block durations.
 */
public fun List<TimeBlock>.totalDuration(): Duration {
    return fold(Duration.ZERO) { acc, block ->
        acc.plus(block.duration())
    }
}

/**
 * Finds a time block by its ID.
 *
 * @param id The ID to search for.
 * @return The time block with matching ID, or null if not found.
 */
public fun List<TimeBlock>.findById(id: String): TimeBlock? {
    return firstOrNull { it.id == id }
}
