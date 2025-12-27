package com.kizitonwose.calendar.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDateTime

/**
 * Unit tests for TimeBlock extension functions.
 *
 * Tests cover:
 * - Overlap detection for collections
 * - Finding overlapping pairs
 * - Sorting by start time
 * - Filtering by type
 * - Total duration calculation
 * - Finding by ID
 */
class TimeBlockExtensionsTest {

    private fun createBlock(
        id: String,
        startHour: Int,
        endHour: Int,
        type: TimeBlockType = TimeBlockType.WORK,
    ): TimeBlock {
        return TimeBlock(
            id = id,
            startTime = LocalDateTime.of(2024, 1, 15, startHour, 0),
            endTime = LocalDateTime.of(2024, 1, 15, endHour, 0),
            blockType = type,
        )
    }

    @Test
    fun `hasOverlaps returns false for empty list`() {
        val blocks = emptyList<TimeBlock>()
        assertFalse(blocks.hasOverlaps())
    }

    @Test
    fun `hasOverlaps returns false for single block`() {
        val blocks = listOf(createBlock("1", 9, 10))
        assertFalse(blocks.hasOverlaps())
    }

    @Test
    fun `hasOverlaps returns false for non-overlapping blocks`() {
        val blocks = listOf(
            createBlock("1", 9, 10),
            createBlock("2", 10, 11),
            createBlock("3", 11, 12),
        )
        assertFalse(blocks.hasOverlaps())
    }

    @Test
    fun `hasOverlaps returns true for overlapping blocks`() {
        val blocks = listOf(
            createBlock("1", 9, 11),
            createBlock("2", 10, 12),
        )
        assertTrue(blocks.hasOverlaps())
    }

    @Test
    fun `hasOverlaps returns true for multiple overlapping blocks`() {
        val blocks = listOf(
            createBlock("1", 9, 12),
            createBlock("2", 10, 11),
            createBlock("3", 11, 13),
        )
        assertTrue(blocks.hasOverlaps())
    }

    @Test
    fun `findOverlaps returns empty list for no overlaps`() {
        val blocks = listOf(
            createBlock("1", 9, 10),
            createBlock("2", 10, 11),
        )
        val overlaps = blocks.findOverlaps()
        assertTrue(overlaps.isEmpty())
    }

    @Test
    fun `findOverlaps returns correct pairs`() {
        val block1 = createBlock("1", 9, 11)
        val block2 = createBlock("2", 10, 12)
        val block3 = createBlock("3", 13, 14)

        val blocks = listOf(block1, block2, block3)
        val overlaps = blocks.findOverlaps()

        assertEquals(1, overlaps.size)
        assertEquals(block1, overlaps[0].first)
        assertEquals(block2, overlaps[0].second)
    }

    @Test
    fun `findOverlaps returns all overlapping pairs`() {
        val block1 = createBlock("1", 9, 12)
        val block2 = createBlock("2", 10, 11)
        val block3 = createBlock("3", 11, 13)

        val blocks = listOf(block1, block2, block3)
        val overlaps = blocks.findOverlaps()

        // block1 overlaps with both block2 and block3
        // block2 does not overlap with block3
        assertEquals(2, overlaps.size)
    }

    @Test
    fun `sortedByStartTime returns correctly sorted list`() {
        val block1 = createBlock("1", 11, 12)
        val block2 = createBlock("2", 9, 10)
        val block3 = createBlock("3", 10, 11)

        val blocks = listOf(block1, block2, block3)
        val sorted = blocks.sortedByStartTime()

        assertEquals("2", sorted[0].id)
        assertEquals("3", sorted[1].id)
        assertEquals("1", sorted[2].id)
    }

    @Test
    fun `sortedByStartTime maintains stable sort`() {
        val block1 = createBlock("1", 9, 10)
        val block2 = createBlock("2", 9, 11)
        val block3 = createBlock("3", 9, 12)

        val blocks = listOf(block1, block2, block3)
        val sorted = blocks.sortedByStartTime()

        // All start at 9, should maintain original order
        assertEquals("1", sorted[0].id)
        assertEquals("2", sorted[1].id)
        assertEquals("3", sorted[2].id)
    }

    @Test
    fun `filterByType returns matching blocks`() {
        val blocks = listOf(
            createBlock("1", 9, 10, TimeBlockType.WORK),
            createBlock("2", 10, 11, TimeBlockType.MEETING),
            createBlock("3", 11, 12, TimeBlockType.WORK),
            createBlock("4", 12, 13, TimeBlockType.BREAK),
        )

        val workBlocks = blocks.filterByType(TimeBlockType.WORK)

        assertEquals(2, workBlocks.size)
        assertEquals("1", workBlocks[0].id)
        assertEquals("3", workBlocks[1].id)
    }

    @Test
    fun `filterByType returns empty list when no matches`() {
        val blocks = listOf(
            createBlock("1", 9, 10, TimeBlockType.WORK),
            createBlock("2", 10, 11, TimeBlockType.WORK),
        )

        val meetingBlocks = blocks.filterByType(TimeBlockType.MEETING)

        assertTrue(meetingBlocks.isEmpty())
    }

    @Test
    fun `filterByTypes returns blocks matching any type`() {
        val blocks = listOf(
            createBlock("1", 9, 10, TimeBlockType.WORK),
            createBlock("2", 10, 11, TimeBlockType.MEETING),
            createBlock("3", 11, 12, TimeBlockType.BREAK),
            createBlock("4", 12, 13, TimeBlockType.FOCUS),
        )

        val filtered = blocks.filterByTypes(
            setOf(TimeBlockType.WORK, TimeBlockType.MEETING),
        )

        assertEquals(2, filtered.size)
        assertEquals("1", filtered[0].id)
        assertEquals("2", filtered[1].id)
    }

    @Test
    fun `filterByTypes returns empty list when no matches`() {
        val blocks = listOf(
            createBlock("1", 9, 10, TimeBlockType.WORK),
        )

        val filtered = blocks.filterByTypes(
            setOf(TimeBlockType.MEETING, TimeBlockType.BREAK),
        )

        assertTrue(filtered.isEmpty())
    }

    @Test
    fun `filterByTypes with empty set returns empty list`() {
        val blocks = listOf(
            createBlock("1", 9, 10, TimeBlockType.WORK),
        )

        val filtered = blocks.filterByTypes(emptySet())

        assertTrue(filtered.isEmpty())
    }

    @Test
    fun `totalDuration returns zero for empty list`() {
        val blocks = emptyList<TimeBlock>()
        val total = blocks.totalDuration()

        assertEquals(Duration.ZERO, total)
    }

    @Test
    fun `totalDuration returns correct sum`() {
        val blocks = listOf(
            createBlock("1", 9, 10),   // 1 hour
            createBlock("2", 10, 11),  // 1 hour
            createBlock("3", 11, 13),  // 2 hours
        )

        val total = blocks.totalDuration()

        assertEquals(Duration.ofHours(4), total)
    }

    @Test
    fun `totalDuration handles overlapping blocks`() {
        val blocks = listOf(
            createBlock("1", 9, 11),   // 2 hours
            createBlock("2", 10, 12),  // 2 hours
        )

        // Should sum durations even if they overlap
        val total = blocks.totalDuration()

        assertEquals(Duration.ofHours(4), total)
    }

    @Test
    fun `findById returns matching block`() {
        val blocks = listOf(
            createBlock("1", 9, 10),
            createBlock("2", 10, 11),
            createBlock("3", 11, 12),
        )

        val found = blocks.findById("2")

        assertEquals("2", found?.id)
    }

    @Test
    fun `findById returns null when not found`() {
        val blocks = listOf(
            createBlock("1", 9, 10),
            createBlock("2", 10, 11),
        )

        val found = blocks.findById("3")

        assertNull(found)
    }

    @Test
    fun `findById returns null for empty list`() {
        val blocks = emptyList<TimeBlock>()

        val found = blocks.findById("1")

        assertNull(found)
    }

    @Test
    fun `findById returns first match for duplicate IDs`() {
        // Edge case: duplicate IDs (shouldn't happen in practice)
        val blocks = listOf(
            createBlock("1", 9, 10),
            createBlock("1", 10, 11),
        )

        val found = blocks.findById("1")

        assertEquals(9, found?.startTime?.hour)
    }

    @Test
    fun `extension functions work on mutable lists`() {
        val blocks = mutableListOf(
            createBlock("1", 11, 12),
            createBlock("2", 9, 10),
        )

        val sorted = blocks.sortedByStartTime()

        assertEquals("2", sorted[0].id)
        assertEquals("1", sorted[1].id)

        // Original list unchanged
        assertEquals("1", blocks[0].id)
        assertEquals("2", blocks[1].id)
    }

    @Test
    fun `chaining extension functions works correctly`() {
        val blocks = listOf(
            createBlock("1", 9, 10, TimeBlockType.WORK),
            createBlock("2", 11, 12, TimeBlockType.WORK),
            createBlock("3", 10, 11, TimeBlockType.MEETING),
            createBlock("4", 8, 9, TimeBlockType.WORK),
        )

        val result = blocks
            .filterByType(TimeBlockType.WORK)
            .sortedByStartTime()

        assertEquals(3, result.size)
        assertEquals("4", result[0].id)
        assertEquals("1", result[1].id)
        assertEquals("2", result[2].id)
    }
}
