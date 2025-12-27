# Time-blocking Feature - Technical Design Document

**Version**: 1.0  
**Date**: 2025-12-27  
**Author**: Calendar Library Development Team  
**Related Documents**: 
- [Requirements Specification](./time-blocking-requirements.md)
- [GitHub Issue #2](https://github.com/yangliang2/Calendar_test/issues/2)

---

## 1. Design Overview

### 1.1 Architecture Summary

This design follows the existing Calendar library architecture:

```
┌─────────────────────────────────────────────────────────────┐
│  USER APPLICATION                                            │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  Custom UI Components (Time Block Rendering)           │ │
│  │  - Colors, styling, drag-drop, interactions            │ │
│  └──────────────────────┬─────────────────────────────────┘ │
└────────────────────────┼──────────────────────────────────┬──┘
                         │                                  │
                         │ Uses                             │
                         ▼                                  │
┌─────────────────────────────────────────────────────────┐ │
│  COMPOSE MODULE (NEW)                                    │ │
│  ┌────────────────────────────────────────────────────┐  │ │
│  │  CalendarState Extensions                          │  │ │
│  │  - addTimeBlock()                                  │  │ │
│  │  - updateTimeBlock()                               │  │ │
│  │  - removeTimeBlock()                               │  │ │
│  │  - getTimeBlocksForDate()                          │  │ │
│  │  - TimeBlockStore management                       │  │ │
│  └────────────────────┬───────────────────────────────┘  │ │
│                       │ Uses                              │ │
│                       ▼                                   │ │
│  ┌────────────────────────────────────────────────────┐  │ │
│  │  State Preservation                                │  │ │
│  │  - TimeBlockSaver (Compose Saver)                  │  │ │
│  └────────────────────────────────────────────────────┘  │ │
└────────────────────────┬──────────────────────────────────┘ │
                         │                                    │
                         │ Depends on                         │
                         ▼                                    │
┌─────────────────────────────────────────────────────────┐  │
│  CORE MODULE (NEW)                                       │  │
│  ┌────────────────────────────────────────────────────┐  │  │
│  │  TimeBlock Data Class                              │  │  │
│  │  - Immutable data structure                        │  │  │
│  │  - Serializable                                    │  │  │
│  │  - @Immutable annotation                           │  │  │
│  └────────────────────────────────────────────────────┘  │  │
│  ┌────────────────────────────────────────────────────┐  │  │
│  │  TimeBlockType Enum                                │  │  │
│  │  - WORK, PERSONAL, BREAK, etc.                     │  │  │
│  └────────────────────────────────────────────────────┘  │  │
│  ┌────────────────────────────────────────────────────┐  │  │
│  │  Extension Functions                               │  │  │
│  │  - duration(), validation, utilities               │  │  │
│  └────────────────────────────────────────────────────┘  │  │
└────────────────────────┬──────────────────────────────────┘  │
                         │                                     │
                         │ Already exists                      │
                         ▼                                     │
┌─────────────────────────────────────────────────────────┐   │
│  EXISTING CORE MODULE                                    │   │
│  - CalendarDay, CalendarMonth, etc.                      │   │
└─────────────────────────────────────────────────────────┘   │
                                                               │
            Works alongside existing calendar data ───────────┘
```

### 1.2 Module Breakdown

| Module | Changes | Files Added | Lines of Code (Est.) |
|--------|---------|-------------|---------------------|
| `core` | New files | 2-3 files | ~300 LOC |
| `compose` | Extension to CalendarState | 1-2 files | ~400 LOC |
| `compose-multiplatform` | Same as compose | 1-2 files | ~400 LOC |
| Tests | New test files | 3-4 files | ~600 LOC |
| **Total** | - | **7-11 files** | **~1700 LOC** |

---

## 2. Core Module Design

### 2.1 File Structure

```
core/src/main/java/com/kizitonwose/calendar/core/
├── (existing files...)
├── TimeBlock.kt          [NEW]
├── TimeBlockType.kt      [NEW]
└── TimeBlockExtensions.kt [NEW]
```

### 2.2 TimeBlock.kt - Data Class

```kotlin
package com.kizitonwose.calendar.core

import androidx.compose.runtime.Immutable
import java.io.Serializable
import java.time.LocalDateTime
import java.time.Duration

/**
 * Represents a time block within a calendar day.
 *
 * A time block is a designated time period that can be associated with
 * tasks, activities, or categories. Time blocks are commonly used in
 * time-blocking productivity techniques.
 *
 * @param id Unique identifier for this time block. Must be unique within
 *           the same date to ensure proper identification for updates/deletions.
 * @param startTime The start date and time of this block.
 * @param endTime The end date and time of this block. Must be after [startTime].
 * @param title Optional display title for this block (e.g., "Sprint Planning").
 * @param description Optional detailed description of the block's purpose.
 * @param blockType The category or type of this block (WORK, PERSONAL, etc.).
 * @param color Optional color hint in ARGB format. If null, UI implementation
 *              decides the color. This is only a hint and UI may override.
 * @param metadata Extensible key-value storage for additional properties.
 *                 Use this to store custom data without modifying the data class.
 *
 * @see TimeBlockType
 * @see duration
 *
 * @since 3.0.0
 */
@Immutable
public data class TimeBlock(
    val id: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val title: String = "",
    val description: String = "",
    val blockType: TimeBlockType = TimeBlockType.CUSTOM,
    val color: Int? = null,
    val metadata: Map<String, Any> = emptyMap(),
) : Serializable {
    
    init {
        require(id.isNotBlank()) { "TimeBlock id cannot be blank" }
        require(endTime.isAfter(startTime)) {
            "TimeBlock endTime ($endTime) must be after startTime ($startTime)"
        }
    }
    
    /**
     * Calculates the duration of this time block.
     *
     * @return The duration between [startTime] and [endTime].
     */
    public fun duration(): Duration = Duration.between(startTime, endTime)
    
    /**
     * Returns the duration of this time block in minutes.
     *
     * @return Total minutes as a Long value.
     */
    public fun durationInMinutes(): Long = duration().toMinutes()
    
    /**
     * Returns the duration of this time block in hours (fractional).
     *
     * @return Total hours as a Double value (e.g., 1.5 for 1 hour 30 minutes).
     */
    public fun durationInHours(): Double = duration().toMinutes() / 60.0
    
    /**
     * Checks if this time block overlaps with another time block.
     *
     * Two blocks overlap if they share any time period.
     *
     * @param other The time block to check against.
     * @return true if the blocks overlap, false otherwise.
     */
    public fun overlapsWith(other: TimeBlock): Boolean {
        return this.startTime < other.endTime && other.startTime < this.endTime
    }
    
    /**
     * Returns a copy of this time block with updated start and end times.
     *
     * @param newStartTime The new start time.
     * @param newEndTime The new end time.
     * @return A new TimeBlock instance with updated times.
     * @throws IllegalArgumentException if newEndTime is not after newStartTime.
     */
    public fun withUpdatedTime(
        newStartTime: LocalDateTime,
        newEndTime: LocalDateTime,
    ): TimeBlock {
        require(newEndTime.isAfter(newStartTime)) {
            "newEndTime must be after newStartTime"
        }
        return copy(startTime = newStartTime, endTime = newEndTime)
    }
    
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
```

**Design Rationale**:
- ✅ **Immutable**: All properties are `val`, data class ensures structural equality
- ✅ **Validation**: `init` block validates invariants at construction time
- ✅ **Serializable**: Supports Android's state preservation mechanisms
- ✅ **@Immutable**: Optimizes Compose recomposition (skips when unchanged)
- ✅ **Convenience methods**: duration(), overlapsWith() reduce boilerplate for users
- ✅ **Extensible**: `metadata` map allows custom properties without API changes
- ✅ **Defensive**: Validates ID is not blank, validates time ordering

### 2.3 TimeBlockType.kt - Enum

```kotlin
package com.kizitonwose.calendar.core

/**
 * Represents the category or type of a time block.
 *
 * These types provide semantic meaning to time blocks and can be used
 * for filtering, styling, or analytics. Applications are free to use
 * [CUSTOM] and store additional type information in [TimeBlock.metadata].
 *
 * @since 3.0.0
 */
public enum class TimeBlockType {
    /**
     * Work-related activities (meetings, coding, documentation, etc.).
     */
    WORK,
    
    /**
     * Personal activities (hobbies, personal projects, appointments).
     */
    PERSONAL,
    
    /**
     * Break periods (lunch, coffee break, rest time).
     */
    BREAK,
    
    /**
     * Focused deep work sessions (no interruptions, high concentration).
     */
    FOCUS,
    
    /**
     * Meeting or collaboration time blocks.
     */
    MEETING,
    
    /**
     * Physical exercise or sports activities.
     */
    EXERCISE,
    
    /**
     * Learning, studying, or educational activities.
     */
    LEARNING,
    
    /**
     * Custom type defined by the application.
     * Use [TimeBlock.metadata] to store additional type information.
     */
    CUSTOM,
}
```

**Design Rationale**:
- ✅ **Enum vs Sealed Class**: Enum chosen for simplicity and standard serialization
- ✅ **Predefined types**: Common productivity categories based on user research
- ✅ **CUSTOM escape hatch**: Allows users to define their own types
- ✅ **No display strings**: UI layer handles i18n and display text
- ✅ **Semantic naming**: Clear, self-documenting type names

### 2.4 TimeBlockExtensions.kt - Utilities

```kotlin
package com.kizitonwose.calendar.core

import java.time.LocalDate

/**
 * Extension functions and utilities for working with time blocks.
 *
 * @since 3.0.0
 */

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
public fun List<TimeBlock>.totalDuration(): java.time.Duration {
    return fold(java.time.Duration.ZERO) { acc, block ->
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

/**
 * Extracts the date component from the time block's start time.
 *
 * @return The LocalDate of the start time.
 */
public fun TimeBlock.startDate(): LocalDate {
    return startTime.toLocalDate()
}

/**
 * Extracts the date component from the time block's end time.
 *
 * @return The LocalDate of the end time.
 */
public fun TimeBlock.endDate(): LocalDate {
    return endTime.toLocalDate()
}

/**
 * Checks if the time block spans multiple days.
 *
 * @return true if the block crosses midnight, false otherwise.
 */
public fun TimeBlock.isMultiDay(): Boolean {
    return startDate() != endDate()
}
```

**Design Rationale**:
- ✅ **Extension functions**: Feels natural in Kotlin, no additional classes needed
- ✅ **Pure functions**: No side effects, easy to test
- ✅ **Composable**: Can chain operations (filter + sort + totalDuration)
- ✅ **Performance conscious**: Simple algorithms, no premature optimization
- ✅ **Null-safe**: Returns null or empty lists appropriately

---

## 3. Compose Module Design

### 3.1 File Structure

```
compose/src/main/java/com/kizitonwose/calendar/compose/
├── (existing files...)
├── CalendarState.kt       [MODIFIED - add time block support]
├── TimeBlockState.kt      [NEW - time block state management]
└── TimeBlockSaver.kt      [NEW - state preservation]
```

### 3.2 CalendarState Extensions

**File**: `compose/src/main/java/com/kizitonwose/calendar/compose/TimeBlockState.kt`

```kotlin
package com.kizitonwose.calendar.compose

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.kizitonwose.calendar.core.TimeBlock
import com.kizitonwose.calendar.core.TimeBlockType
import com.kizitonwose.calendar.core.findById
import com.kizitonwose.calendar.core.filterByType
import java.time.LocalDate

/**
 * Manages time block state for a calendar.
 *
 * This class is internal and should not be used directly.
 * Access time blocks through [CalendarState] extension functions.
 *
 * @since 3.0.0
 */
@Stable
internal class TimeBlockStore {
    // Using SnapshotStateMap for Compose-aware mutations
    private val blocksMap = SnapshotStateMap<LocalDate, List<TimeBlock>>()
    
    /**
     * Gets all time blocks for a specific date.
     *
     * @param date The date to query.
     * @return Immutable list of time blocks for the date (empty if none).
     */
    fun getBlocksForDate(date: LocalDate): List<TimeBlock> {
        return blocksMap[date] ?: emptyList()
    }
    
    /**
     * Gets all time blocks within a date range.
     *
     * @param dateRange The range of dates to query.
     * @return Map of date to time blocks for dates in the range.
     */
    fun getBlocksForDateRange(
        dateRange: ClosedRange<LocalDate>
    ): Map<LocalDate, List<TimeBlock>> {
        val result = mutableMapOf<LocalDate, List<TimeBlock>>()
        var currentDate = dateRange.start
        
        while (!currentDate.isAfter(dateRange.endInclusive)) {
            blocksMap[currentDate]?.let { blocks ->
                if (blocks.isNotEmpty()) {
                    result[currentDate] = blocks
                }
            }
            currentDate = currentDate.plusDays(1)
        }
        
        return result
    }
    
    /**
     * Adds a time block to a specific date.
     *
     * @param date The date to add the block to.
     * @param block The time block to add.
     * @return true if added successfully, false if block with same ID exists.
     */
    fun addBlock(date: LocalDate, block: TimeBlock): Boolean {
        val currentBlocks = blocksMap[date] ?: emptyList()
        
        // Check for duplicate ID on the same date
        if (currentBlocks.any { it.id == block.id }) {
            return false
        }
        
        blocksMap[date] = currentBlocks + block
        return true
    }
    
    /**
     * Updates an existing time block.
     *
     * @param date The date where the block exists.
     * @param blockId The ID of the block to update.
     * @param updatedBlock The new block data (ID must match blockId).
     * @return true if updated successfully, false if block not found or ID mismatch.
     */
    fun updateBlock(
        date: LocalDate,
        blockId: String,
        updatedBlock: TimeBlock
    ): Boolean {
        val currentBlocks = blocksMap[date] ?: return false
        
        // Ensure ID consistency
        if (updatedBlock.id != blockId) {
            return false
        }
        
        val index = currentBlocks.indexOfFirst { it.id == blockId }
        if (index == -1) {
            return false
        }
        
        val newBlocks = currentBlocks.toMutableList()
        newBlocks[index] = updatedBlock
        blocksMap[date] = newBlocks
        return true
    }
    
    /**
     * Removes a time block from a specific date.
     *
     * @param date The date to remove the block from.
     * @param blockId The ID of the block to remove.
     * @return The removed block, or null if not found.
     */
    fun removeBlock(date: LocalDate, blockId: String): TimeBlock? {
        val currentBlocks = blocksMap[date] ?: return null
        
        val block = currentBlocks.findById(blockId) ?: return null
        val newBlocks = currentBlocks.filter { it.id != blockId }
        
        if (newBlocks.isEmpty()) {
            blocksMap.remove(date)
        } else {
            blocksMap[date] = newBlocks
        }
        
        return block
    }
    
    /**
     * Removes all time blocks for a specific date.
     *
     * @param date The date to clear blocks from.
     * @return The list of removed blocks, or empty list if date had no blocks.
     */
    fun removeAllBlocksForDate(date: LocalDate): List<TimeBlock> {
        return blocksMap.remove(date) ?: emptyList()
    }
    
    /**
     * Clears all time blocks from all dates.
     */
    fun clearAllBlocks() {
        blocksMap.clear()
    }
    
    /**
     * Finds a time block by ID across all dates.
     *
     * @param blockId The ID to search for.
     * @return Pair of (date, block) if found, or null otherwise.
     */
    fun findBlockById(blockId: String): Pair<LocalDate, TimeBlock>? {
        for ((date, blocks) in blocksMap) {
            val block = blocks.findById(blockId)
            if (block != null) {
                return date to block
            }
        }
        return null
    }
    
    /**
     * Gets all time blocks of a specific type across all dates.
     *
     * @param type The block type to filter by.
     * @return List of (date, block) pairs.
     */
    fun getBlocksByType(type: TimeBlockType): List<Pair<LocalDate, TimeBlock>> {
        val result = mutableListOf<Pair<LocalDate, TimeBlock>>()
        for ((date, blocks) in blocksMap) {
            blocks.filterByType(type).forEach { block ->
                result.add(date to block)
            }
        }
        return result
    }
    
    /**
     * Checks if any time blocks exist for a specific date.
     *
     * @param date The date to check.
     * @return true if date has at least one block, false otherwise.
     */
    fun hasBlocks(date: LocalDate): Boolean {
        return blocksMap[date]?.isNotEmpty() == true
    }
    
    /**
     * Gets total number of time blocks across all dates.
     *
     * @return Total block count.
     */
    fun getTotalBlockCount(): Int {
        return blocksMap.values.sumOf { it.size }
    }
    
    /**
     * Exports all time blocks as a map (for state preservation).
     *
     * @return Immutable copy of the blocks map.
     */
    fun exportBlocks(): Map<LocalDate, List<TimeBlock>> {
        return blocksMap.toMap()
    }
    
    /**
     * Imports time blocks from a map (for state restoration).
     *
     * @param blocks The blocks to import (replaces existing blocks).
     */
    fun importBlocks(blocks: Map<LocalDate, List<TimeBlock>>) {
        blocksMap.clear()
        blocksMap.putAll(blocks)
    }
}

/**
 * Extension functions for [CalendarState] to manage time blocks.
 *
 * These functions provide the public API for time block management.
 * They delegate to an internal [TimeBlockStore] instance.
 *
 * @since 3.0.0
 */

// Store instances (one per CalendarState, managed via companion object)
private val calendarTimeBlockStores = mutableMapOf<CalendarState, TimeBlockStore>()

/**
 * Gets or creates the time block store for this calendar state.
 */
private fun CalendarState.timeBlockStore(): TimeBlockStore {
    return calendarTimeBlockStores.getOrPut(this) { TimeBlockStore() }
}

// Public API Extensions

/**
 * Gets all time blocks for a specific date.
 *
 * @param date The date to query.
 * @return Immutable list of time blocks for the date, sorted by start time.
 */
public fun CalendarState.getTimeBlocksForDate(date: LocalDate): List<TimeBlock> {
    return timeBlockStore().getBlocksForDate(date)
}

/**
 * Gets all time blocks within a date range.
 *
 * @param dateRange The range of dates to query.
 * @return Map of date to time blocks for dates in the range.
 */
public fun CalendarState.getTimeBlocksForDateRange(
    dateRange: ClosedRange<LocalDate>
): Map<LocalDate, List<TimeBlock>> {
    return timeBlockStore().getBlocksForDateRange(dateRange)
}

/**
 * Adds a time block to a specific date.
 *
 * If a block with the same ID already exists on the date, this operation fails.
 *
 * @param date The date to add the block to.
 * @param block The time block to add.
 * @return true if added successfully, false if block with same ID exists.
 */
public fun CalendarState.addTimeBlock(date: LocalDate, block: TimeBlock): Boolean {
    return timeBlockStore().addBlock(date, block)
}

/**
 * Adds multiple time blocks to a specific date.
 *
 * @param date The date to add blocks to.
 * @param blocks The time blocks to add.
 * @return Number of blocks successfully added (may be less than input if IDs conflict).
 */
public fun CalendarState.addTimeBlocks(
    date: LocalDate,
    blocks: List<TimeBlock>
): Int {
    var successCount = 0
    blocks.forEach { block ->
        if (addTimeBlock(date, block)) {
            successCount++
        }
    }
    return successCount
}

/**
 * Updates an existing time block.
 *
 * The updated block must have the same ID as the original block.
 *
 * @param date The date where the block exists.
 * @param blockId The ID of the block to update.
 * @param updatedBlock The new block data.
 * @return true if updated successfully, false if block not found or ID mismatch.
 */
public fun CalendarState.updateTimeBlock(
    date: LocalDate,
    blockId: String,
    updatedBlock: TimeBlock
): Boolean {
    return timeBlockStore().updateBlock(date, blockId, updatedBlock)
}

/**
 * Updates a time block using a transformation function.
 *
 * This is useful for modifying specific properties without recreating the entire block.
 *
 * @param date The date where the block exists.
 * @param blockId The ID of the block to update.
 * @param transform Function that receives the current block and returns updated block.
 * @return true if updated successfully, false if block not found.
 */
public fun CalendarState.updateTimeBlockProperty(
    date: LocalDate,
    blockId: String,
    transform: (TimeBlock) -> TimeBlock
): Boolean {
    val currentBlock = getTimeBlocksForDate(date).findById(blockId) ?: return false
    val updatedBlock = transform(currentBlock)
    return updateTimeBlock(date, blockId, updatedBlock)
}

/**
 * Removes a time block from a specific date.
 *
 * @param date The date to remove the block from.
 * @param blockId The ID of the block to remove.
 * @return The removed block, or null if not found.
 */
public fun CalendarState.removeTimeBlock(date: LocalDate, blockId: String): TimeBlock? {
    return timeBlockStore().removeBlock(date, blockId)
}

/**
 * Removes all time blocks for a specific date.
 *
 * @param date The date to clear blocks from.
 * @return The list of removed blocks.
 */
public fun CalendarState.removeAllTimeBlocksForDate(date: LocalDate): List<TimeBlock> {
    return timeBlockStore().removeAllBlocksForDate(date)
}

/**
 * Clears all time blocks from all dates.
 */
public fun CalendarState.clearAllTimeBlocks() {
    timeBlockStore().clearAllBlocks()
}

/**
 * Finds a time block by ID across all dates.
 *
 * @param blockId The ID to search for.
 * @return Pair of (date, block) if found, or null otherwise.
 */
public fun CalendarState.findTimeBlockById(blockId: String): Pair<LocalDate, TimeBlock>? {
    return timeBlockStore().findBlockById(blockId)
}

/**
 * Gets all time blocks of a specific type across all dates.
 *
 * @param type The block type to filter by.
 * @return List of (date, block) pairs.
 */
public fun CalendarState.getTimeBlocksByType(type: TimeBlockType): List<Pair<LocalDate, TimeBlock>> {
    return timeBlockStore().getBlocksByType(type)
}

/**
 * Checks if any time blocks exist for a specific date.
 *
 * @param date The date to check.
 * @return true if date has at least one block, false otherwise.
 */
public fun CalendarState.hasTimeBlocks(date: LocalDate): Boolean {
    return timeBlockStore().hasBlocks(date)
}

/**
 * Gets total number of time blocks across all dates.
 *
 * @return Total block count.
 */
public fun CalendarState.getTotalTimeBlockCount(): Int {
    return timeBlockStore().getTotalBlockCount()
}
```

**Design Rationale**:
- ✅ **Extension functions**: Non-invasive, doesn't modify existing CalendarState class
- ✅ **SnapshotStateMap**: Compose-aware, triggers recomposition on changes
- ✅ **Internal store**: Implementation detail hidden from users
- ✅ **Immutable returns**: Public API returns immutable lists/maps
- ✅ **Rich API**: Comprehensive operations for common use cases
- ✅ **Error handling**: Returns boolean/null to indicate success/failure
- ✅ **Thread-safe**: SnapshotStateMap handles concurrency

### 3.3 State Preservation

**File**: `compose/src/main/java/com/kizitonwose/calendar/compose/TimeBlockSaver.kt`

```kotlin
package com.kizitonwose.calendar.compose

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import com.kizitonwose.calendar.core.TimeBlock
import com.kizitonwose.calendar.core.TimeBlockType
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Saver for time blocks to survive configuration changes.
 *
 * This saver converts time blocks to and from a serializable format
 * that Compose can save and restore.
 *
 * @since 3.0.0
 */
internal val TimeBlockSaver: Saver<Map<LocalDate, List<TimeBlock>>, Any> = listSaver(
    save = { timeBlocksMap ->
        // Convert map to list of serializable entries
        timeBlocksMap.flatMap { (date, blocks) ->
            blocks.map { block ->
                listOf(
                    date.toString(),                  // 0: date as ISO string
                    block.id,                         // 1: block ID
                    block.startTime.toString(),       // 2: start time as ISO string
                    block.endTime.toString(),         // 3: end time as ISO string
                    block.title,                      // 4: title
                    block.description,                // 5: description
                    block.blockType.name,             // 6: type as string
                    block.color,                      // 7: color (nullable Int)
                    // metadata is intentionally not saved (not serializable)
                )
            }
        }
    },
    restore = { savedList ->
        // Convert saved list back to map
        val timeBlocksMap = mutableMapOf<LocalDate, MutableList<TimeBlock>>()
        
        // Each entry is a list of 8 elements
        for (i in savedList.indices step 8) {
            if (i + 7 < savedList.size) {
                try {
                    val date = LocalDate.parse(savedList[i] as String)
                    val block = TimeBlock(
                        id = savedList[i + 1] as String,
                        startTime = LocalDateTime.parse(savedList[i + 2] as String),
                        endTime = LocalDateTime.parse(savedList[i + 3] as String),
                        title = savedList[i + 4] as String,
                        description = savedList[i + 5] as String,
                        blockType = TimeBlockType.valueOf(savedList[i + 6] as String),
                        color = savedList[i + 7] as Int?,
                        metadata = emptyMap() // metadata not restored
                    )
                    
                    timeBlocksMap.getOrPut(date) { mutableListOf() }.add(block)
                } catch (e: Exception) {
                    // Skip invalid entries
                    continue
                }
            }
        }
        
        timeBlocksMap.toMap()
    }
)

/**
 * Integrates time block state saving with CalendarState.
 *
 * This should be called in rememberCalendarState to enable state preservation.
 */
internal fun CalendarState.saveTimeBlocks(): Map<LocalDate, List<TimeBlock>> {
    return timeBlockStore().exportBlocks()
}

/**
 * Restores time block state to CalendarState.
 *
 * This should be called when restoring CalendarState from saved state.
 */
internal fun CalendarState.restoreTimeBlocks(blocks: Map<LocalDate, List<TimeBlock>>) {
    timeBlockStore().importBlocks(blocks)
}
```

**Design Rationale**:
- ✅ **listSaver**: Compose-compatible serialization format
- ✅ **ISO strings**: Standard date/time serialization
- ✅ **Error handling**: Gracefully skips invalid entries during restore
- ✅ **Metadata limitation**: Maps with Any values aren't serializable (documented)
- ✅ **Efficient**: Flat list structure for minimal overhead

---

## 4. Testing Strategy

### 4.1 Unit Tests Structure

```
core/src/test/java/com/kizitonwose/calendar/core/
├── TimeBlockTest.kt
├── TimeBlockExtensionsTest.kt
└── TimeBlockValidationTest.kt

compose/src/test/java/com/kizitonwose/calendar/compose/
├── TimeBlockStoreTest.kt
├── TimeBlockStateExtensionsTest.kt
└── TimeBlockSaverTest.kt
```

### 4.2 Test Coverage Plan

| Component | Test Cases | Priority |
|-----------|------------|----------|
| TimeBlock data class | 15 tests | P0 |
| TimeBlock validation | 10 tests | P0 |
| Extension functions | 20 tests | P1 |
| TimeBlockStore | 25 tests | P0 |
| State extensions | 20 tests | P0 |
| Saver | 10 tests | P1 |
| **Total** | **~100 tests** | - |

### 4.3 Key Test Scenarios

#### TimeBlock Tests
```kotlin
@Test
fun `timeBlock duration calculation is correct`() {
    val block = TimeBlock(
        id = "test",
        startTime = LocalDateTime.of(2025, 1, 1, 9, 0),
        endTime = LocalDateTime.of(2025, 1, 1, 11, 30),
        blockType = TimeBlockType.WORK
    )
    
    assertEquals(150L, block.durationInMinutes())
    assertEquals(2.5, block.durationInHours(), 0.01)
}

@Test
fun `timeBlock validates start before end`() {
    assertThrows<IllegalArgumentException> {
        TimeBlock(
            id = "test",
            startTime = LocalDateTime.of(2025, 1, 1, 11, 0),
            endTime = LocalDateTime.of(2025, 1, 1, 9, 0),
            blockType = TimeBlockType.WORK
        )
    }
}

@Test
fun `timeBlock detects overlaps correctly`() {
    val block1 = TimeBlock(
        id = "1",
        startTime = LocalDateTime.of(2025, 1, 1, 9, 0),
        endTime = LocalDateTime.of(2025, 1, 1, 11, 0),
        blockType = TimeBlockType.WORK
    )
    
    val block2 = TimeBlock(
        id = "2",
        startTime = LocalDateTime.of(2025, 1, 1, 10, 0),
        endTime = LocalDateTime.of(2025, 1, 1, 12, 0),
        blockType = TimeBlockType.MEETING
    )
    
    assertTrue(block1.overlapsWith(block2))
    assertTrue(block2.overlapsWith(block1))
}
```

#### State Management Tests
```kotlin
@Test
fun `addTimeBlock stores block correctly`() = runTest {
    val state = CalendarState(...)
    val date = LocalDate.of(2025, 1, 1)
    val block = TimeBlock(
        id = "test",
        startTime = date.atTime(9, 0),
        endTime = date.atTime(11, 0),
        blockType = TimeBlockType.WORK
    )
    
    assertTrue(state.addTimeBlock(date, block))
    assertEquals(listOf(block), state.getTimeBlocksForDate(date))
}

@Test
fun `addTimeBlock rejects duplicate ID`() = runTest {
    val state = CalendarState(...)
    val date = LocalDate.of(2025, 1, 1)
    val block1 = TimeBlock(id = "same-id", ...)
    val block2 = TimeBlock(id = "same-id", ...)
    
    assertTrue(state.addTimeBlock(date, block1))
    assertFalse(state.addTimeBlock(date, block2))
}

@Test
fun `updateTimeBlock modifies existing block`() = runTest {
    val state = CalendarState(...)
    val date = LocalDate.of(2025, 1, 1)
    val original = TimeBlock(id = "test", title = "Original", ...)
    val updated = original.copy(title = "Updated")
    
    state.addTimeBlock(date, original)
    assertTrue(state.updateTimeBlock(date, "test", updated))
    assertEquals("Updated", state.getTimeBlocksForDate(date).first().title)
}
```

#### Saver Tests
```kotlin
@Test
fun `TimeBlockSaver preserves blocks across save-restore`() {
    val original = mapOf(
        LocalDate.of(2025, 1, 1) to listOf(
            TimeBlock(id = "1", ...),
            TimeBlock(id = "2", ...)
        )
    )
    
    val saved = with(TimeBlockSaver) { save(original) }
    val restored = TimeBlockSaver.restore(saved)
    
    assertEquals(original, restored)
}
```

---

## 5. API Documentation Plan

### 5.1 KDoc Requirements

Every public API must have:
- ✅ Summary description
- ✅ @param documentation for all parameters
- ✅ @return documentation if applicable
- ✅ @throws documentation for exceptions
- ✅ @see references to related APIs
- ✅ @since version tag
- ✅ Usage examples for complex APIs

### 5.2 Example Usage Documentation

Create `docs/time-blocking-usage.md`:

```markdown
# Time-blocking Feature Usage Guide

## Basic Usage

### Creating Time Blocks

```kotlin
@Composable
fun MyCalendar() {
    val calendarState = rememberCalendarState()
    
    // Create a time block
    val workBlock = TimeBlock(
        id = UUID.randomUUID().toString(),
        startTime = LocalDateTime.of(2025, 1, 1, 9, 0),
        endTime = LocalDateTime.of(2025, 1, 1, 11, 0),
        title = "Sprint Planning",
        blockType = TimeBlockType.WORK,
        color = Color.Blue.toArgb()
    )
    
    // Add to calendar
    LaunchedEffect(Unit) {
        calendarState.addTimeBlock(LocalDate.of(2025, 1, 1), workBlock)
    }
    
    HorizontalCalendar(
        state = calendarState,
        dayContent = { day -> DayWithTimeBlocks(day, calendarState) }
    )
}
```

### Custom Day Cell with Time Blocks

```kotlin
@Composable
fun DayWithTimeBlocks(day: CalendarDay, calendarState: CalendarState) {
    val blocks = calendarState.getTimeBlocksForDate(day.date)
    
    Column(modifier = Modifier.fillMaxSize()) {
        Text(day.date.dayOfMonth.toString())
        
        blocks.forEach { block ->
            TimeBlockChip(block)
        }
    }
}

@Composable
fun TimeBlockChip(block: TimeBlock) {
    Surface(
        color = block.color?.let { Color(it) } ?: Color.Gray,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(2.dp)
    ) {
        Text(
            text = block.title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
```

(... more examples ...)
```

---

## 6. Implementation Phases

### Phase 1: Core Data Model (Days 1-2)
- [ ] Create TimeBlock.kt
- [ ] Create TimeBlockType.kt
- [ ] Create TimeBlockExtensions.kt
- [ ] Write unit tests for core (40+ tests)
- [ ] Code review

### Phase 2: Compose State (Days 3-5)
- [ ] Create TimeBlockStore.kt
- [ ] Add CalendarState extensions
- [ ] Write unit tests for state (40+ tests)
- [ ] Code review

### Phase 3: State Preservation (Days 5-6)
- [ ] Create TimeBlockSaver.kt
- [ ] Integrate with CalendarState
- [ ] Write unit tests for saver (10+ tests)
- [ ] Test on Android device (rotation, process death)

### Phase 4: Multiplatform Support (Days 6-7)
- [ ] Copy changes to compose-multiplatform module
- [ ] Adapt for kotlinx-datetime (if needed)
- [ ] Test on iOS, Desktop, Web
- [ ] Handle platform-specific issues

### Phase 5: Documentation (Days 7-8)
- [ ] Complete KDoc for all public APIs
- [ ] Write usage guide with examples
- [ ] Update library README
- [ ] Create sample app demo

### Phase 6: Review & Polish (Days 8-10)
- [ ] Final code review
- [ ] Performance profiling
- [ ] API design review
- [ ] Address feedback

---

## 7. Performance Considerations

### 7.1 Memory Usage

**Estimated memory per TimeBlock**: ~200 bytes
- Strings (id, title, description): ~150 bytes
- LocalDateTime (2x): ~32 bytes
- Enum, Int, Map: ~18 bytes

**For 1000 blocks**: ~200 KB (acceptable)

### 7.2 Computational Complexity

| Operation | Complexity | Notes |
|-----------|------------|-------|
| Add block | O(1) | Map + list append |
| Get blocks for date | O(1) | Direct map lookup |
| Update block | O(n) | Where n = blocks per date (typically < 20) |
| Remove block | O(n) | Where n = blocks per date |
| Find by ID | O(n*m) | Where n = dates, m = blocks per date |
| Get by type | O(n*m) | Full scan needed |

### 7.3 Optimization Strategies

1. **Lazy loading**: Only load blocks for visible months
2. **Caching**: Derived state for expensive computations
3. **Efficient filtering**: Use indexed data structures if needed
4. **Minimal recomposition**: Use @Stable and @Immutable correctly

---

## 8. Risks and Mitigations

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| API too complex | High | Low | Keep minimal API surface, user testing |
| Performance issues | Medium | Low | Profiling, lazy loading, benchmarks |
| State preservation bugs | Medium | Medium | Thorough testing, fallback to empty state |
| Multiplatform incompatibility | High | Low | Use only common APIs, test all platforms |
| Breaking existing code | Critical | Very Low | Extension-only approach, no modifications to existing classes |

---

## 9. Success Criteria

### 9.1 Code Quality
- ✅ All tests pass (100%)
- ✅ Test coverage > 80%
- ✅ Ktlint passes with no errors
- ✅ Binary compatibility validator passes
- ✅ No performance regressions in existing features

### 9.2 Functionality
- ✅ All functional requirements from requirements doc implemented
- ✅ Works on all target platforms (Android, iOS, Desktop, Web)
- ✅ State survives configuration changes
- ✅ No memory leaks detected

### 9.3 Documentation
- ✅ All public APIs have complete KDoc
- ✅ Usage guide with examples created
- ✅ Sample app demonstrates feature
- ✅ README updated

---

## 10. Open Technical Questions

1. **Q**: Should we provide a separate artifact for time-blocking?  
   **A**: No, include in main library. Feature is lightweight and optional.

2. **Q**: Should TimeBlockStore be per-CalendarState or global?  
   **A**: Per-CalendarState (via map keyed by state instance).

3. **Q**: How to handle time blocks spanning multiple days?  
   **A**: Store on start date only. Users can check isMultiDay() if needed.

4. **Q**: Should we provide Compose remember functions?  
   **A**: No, CalendarState extensions are sufficient.

5. **Q**: How to handle timezone changes?  
   **A**: Out of scope. LocalDateTime is timezone-naive.

---

## 11. Approval and Next Steps

**Design Review Status**: 🟡 Awaiting Review

**Required Approvals**:
- [ ] Technical Lead
- [ ] Library Maintainer
- [ ] Community Representative (optional)

**Upon Approval**:
1. Begin Phase 1 implementation (core module)
2. Set up tracking branch: `feature/time-blocking`
3. Configure CI for new tests
4. Begin implementation following TDD approach

---

**Version History**:
- v1.0 (2025-12-27): Initial technical design

**Related Documents**:
- [Requirements Specification](./time-blocking-requirements.md)
- [GitHub Issue #2](https://github.com/yangliang2/Calendar_test/issues/2)
