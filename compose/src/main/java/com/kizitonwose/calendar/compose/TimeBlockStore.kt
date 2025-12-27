package com.kizitonwose.calendar.compose

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.kizitonwose.calendar.core.TimeBlock
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * A state store for managing time blocks within a calendar.
 *
 * This store provides efficient CRUD operations for time blocks, organized by date
 * for fast lookups. It uses Compose's snapshot state system to automatically trigger
 * recomposition when time blocks are added, updated, or removed.
 *
 * ## Thread Safety
 * This class uses Compose's snapshot state system, which provides thread safety
 * for state reads and writes within the Compose runtime.
 *
 * ## Performance Considerations
 * - Time blocks are indexed by LocalDate for O(1) lookup by date
 * - Bulk operations should be preferred over individual operations when possible
 * - The store automatically deduplicates time blocks by ID
 *
 * @see TimeBlock
 * @see CalendarState
 *
 * @since 3.0.0
 */
@Stable
public class TimeBlockStore internal constructor() {
    /**
     * Internal storage: Map of LocalDate to List of TimeBlocks
     * Using SnapshotStateMap for automatic recomposition on changes
     */
    private val blocksMap: SnapshotStateMap<LocalDate, MutableList<TimeBlock>> =
        androidx.compose.runtime.mutableStateMapOf()

    /**
     * Version counter incremented on any modification.
     * Used for efficient change detection and triggering recomposition.
     */
    private var version by mutableStateOf(0L)

    /**
     * Returns a read-only snapshot of all time blocks in the store.
     *
     * @return List of all time blocks across all dates, sorted by start time.
     */
    public fun getAllBlocks(): List<TimeBlock> {
        // Trigger recomposition by reading version
        @Suppress("UNUSED_EXPRESSION")
        version
        return blocksMap.values
            .flatten()
            .sortedBy { it.startTime }
    }

    /**
     * Returns all time blocks for a specific date.
     *
     * This includes blocks that start on the given date, even if they span multiple days.
     * For multi-day blocks, use [getBlocksInRange] to find all blocks that overlap a date.
     *
     * @param date The date to query.
     * @return List of time blocks starting on the specified date, sorted by start time.
     *         Returns an empty list if no blocks exist for the date.
     */
    public fun getBlocksForDate(date: LocalDate): List<TimeBlock> {
        // Trigger recomposition by reading version
        @Suppress("UNUSED_EXPRESSION")
        version
        return blocksMap[date]?.sortedBy { it.startTime } ?: emptyList()
    }

    /**
     * Returns all time blocks that overlap with the specified date range.
     *
     * A block overlaps with the range if:
     * - Its start date is within [startDate, endDate]
     * - Its end date is within [startDate, endDate]
     * - It completely spans the range (starts before and ends after)
     *
     * @param startDate The start of the date range (inclusive).
     * @param endDate The end of the date range (inclusive).
     * @return List of time blocks overlapping the range, sorted by start time.
     * @throws IllegalArgumentException if startDate is after endDate.
     */
    public fun getBlocksInRange(startDate: LocalDate, endDate: LocalDate): List<TimeBlock> {
        require(!startDate.isAfter(endDate)) {
            "startDate ($startDate) must not be after endDate ($endDate)"
        }

        // Trigger recomposition by reading version
        @Suppress("UNUSED_EXPRESSION")
        version

        val result = mutableListOf<TimeBlock>()
        var currentDate = startDate

        while (!currentDate.isAfter(endDate)) {
            blocksMap[currentDate]?.let { blocks ->
                for (block in blocks) {
                    val blockEndDate = block.endDate()
                    // Include block if it overlaps with the range
                    if (!blockEndDate.isBefore(startDate) && !block.startDate().isAfter(endDate)) {
                        // Avoid duplicates for multi-day blocks
                        if (result.none { it.id == block.id }) {
                            result.add(block)
                        }
                    }
                }
            }
            currentDate = currentDate.plusDays(1)
        }

        return result.sortedBy { it.startTime }
    }

    /**
     * Finds a time block by its unique ID.
     *
     * @param id The unique identifier of the time block.
     * @return The time block with the specified ID, or null if not found.
     */
    public fun getBlockById(id: String): TimeBlock? {
        // Trigger recomposition by reading version
        @Suppress("UNUSED_EXPRESSION")
        version
        return blocksMap.values
            .flatten()
            .firstOrNull { it.id == id }
    }

    /**
     * Adds a new time block to the store.
     *
     * If a block with the same ID already exists, it will be replaced with the new block.
     * Multi-day blocks are indexed under their start date only.
     *
     * @param block The time block to add.
     */
    public fun addBlock(block: TimeBlock) {
        val date = block.startDate()

        // Remove existing block with same ID if present
        removeBlockById(block.id, incrementVersion = false)

        // Add to the appropriate date
        val dateBlocks = blocksMap.getOrPut(date) { mutableListOf() }
        dateBlocks.add(block)

        incrementVersion()
    }

    /**
     * Adds multiple time blocks to the store in a single operation.
     *
     * This is more efficient than calling [addBlock] multiple times, as it only
     * triggers one recomposition.
     *
     * @param blocks The time blocks to add.
     */
    public fun addBlocks(blocks: List<TimeBlock>) {
        for (block in blocks) {
            val date = block.startDate()
            removeBlockById(block.id, incrementVersion = false)

            val dateBlocks = blocksMap.getOrPut(date) { mutableListOf() }
            dateBlocks.add(block)
        }
        incrementVersion()
    }

    /**
     * Updates an existing time block.
     *
     * If the block's start date has changed, it will be moved to the new date's index.
     * If no block with the given ID exists, this operation has no effect.
     *
     * @param block The updated time block. The ID must match an existing block.
     * @return true if a block was updated, false if no block with the ID exists.
     */
    public fun updateBlock(block: TimeBlock): Boolean {
        val existingBlock = getBlockById(block.id) ?: return false

        // Remove from old location
        val oldDate = existingBlock.startDate()
        blocksMap[oldDate]?.removeAll { it.id == block.id }
        if (blocksMap[oldDate]?.isEmpty() == true) {
            blocksMap.remove(oldDate)
        }

        // Add to new location
        val newDate = block.startDate()
        val dateBlocks = blocksMap.getOrPut(newDate) { mutableListOf() }
        dateBlocks.add(block)

        incrementVersion()
        return true
    }

    /**
     * Removes a time block by its ID.
     *
     * @param id The unique identifier of the time block to remove.
     * @return true if a block was removed, false if no block with the ID exists.
     */
    public fun removeBlock(id: String): Boolean {
        return removeBlockById(id, incrementVersion = true)
    }

    /**
     * Removes a time block from the store.
     *
     * @param block The time block to remove (matched by ID).
     * @return true if a block was removed, false if the block was not in the store.
     */
    public fun removeBlock(block: TimeBlock): Boolean {
        return removeBlock(block.id)
    }

    /**
     * Removes all time blocks from the specified date.
     *
     * @param date The date for which to remove all blocks.
     * @return The number of blocks removed.
     */
    public fun removeBlocksForDate(date: LocalDate): Int {
        val removed = blocksMap.remove(date)?.size ?: 0
        if (removed > 0) {
            incrementVersion()
        }
        return removed
    }

    /**
     * Removes all time blocks from the store.
     */
    public fun clear() {
        blocksMap.clear()
        incrementVersion()
    }

    /**
     * Checks if the store contains a time block with the specified ID.
     *
     * @param id The unique identifier to check.
     * @return true if a block with the ID exists, false otherwise.
     */
    public fun containsBlock(id: String): Boolean {
        // Trigger recomposition by reading version
        @Suppress("UNUSED_EXPRESSION")
        version
        return blocksMap.values
            .flatten()
            .any { it.id == id }
    }

    /**
     * Returns the total number of time blocks in the store.
     *
     * @return The count of all time blocks across all dates.
     */
    public fun size(): Int {
        // Trigger recomposition by reading version
        @Suppress("UNUSED_EXPRESSION")
        version
        return blocksMap.values.sumOf { it.size }
    }

    /**
     * Checks if the store is empty.
     *
     * @return true if no time blocks exist, false otherwise.
     */
    public fun isEmpty(): Boolean = size() == 0

    /**
     * Internal helper to remove a block by ID.
     *
     * @param id The ID of the block to remove.
     * @param incrementVersion Whether to increment the version counter.
     * @return true if a block was removed, false otherwise.
     */
    private fun removeBlockById(id: String, incrementVersion: Boolean): Boolean {
        var removed = false
        val iterator = blocksMap.iterator()

        while (iterator.hasNext()) {
            val (date, blocks) = iterator.next()
            val initialSize = blocks.size
            blocks.removeAll { it.id == id }

            if (blocks.size < initialSize) {
                removed = true
                // Remove empty date entries
                if (blocks.isEmpty()) {
                    iterator.remove()
                }
            }
        }

        if (removed && incrementVersion) {
            incrementVersion()
        }

        return removed
    }

    /**
     * Increments the version counter to trigger recomposition.
     */
    private fun incrementVersion() {
        version++
    }

    /**
     * Returns a string representation of the store for debugging.
     *
     * @return A string showing the number of dates and total blocks.
     */
    override fun toString(): String {
        return "TimeBlockStore(dates=${blocksMap.size}, blocks=${size()}, version=$version)"
    }

    public companion object {
        /**
         * Creates a new empty TimeBlockStore.
         *
         * @return A new TimeBlockStore instance.
         */
        public fun create(): TimeBlockStore = TimeBlockStore()

        /**
         * Creates a TimeBlockStore with initial time blocks.
         *
         * @param initialBlocks The initial time blocks to populate the store.
         * @return A new TimeBlockStore instance with the provided blocks.
         */
        public fun create(initialBlocks: List<TimeBlock>): TimeBlockStore {
            return TimeBlockStore().apply {
                addBlocks(initialBlocks)
            }
        }
    }
}
