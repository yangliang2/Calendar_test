package com.kizitonwose.calendar.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Unit tests for [TimeBlock] data class.
 *
 * Tests cover:
 * - Construction and validation
 * - Duration calculations
 * - Overlap detection
 * - Time manipulation
 * - Date extraction
 * - Multi-day block detection
 * - Serialization
 * - Equality and hashCode
 */
class TimeBlockTest {

    @Test
    fun `create valid time block`() {
        val startTime = LocalDateTime.of(2024, 1, 15, 9, 0)
        val endTime = LocalDateTime.of(2024, 1, 15, 10, 0)

        val block = TimeBlock(
            id = "test-1",
            startTime = startTime,
            endTime = endTime,
            title = "Meeting",
            description = "Team standup",
            blockType = TimeBlockType.MEETING,
            color = 0xFF0000FF.toInt(),
            metadata = mapOf("location" to "Room 101"),
        )

        assertEquals("test-1", block.id)
        assertEquals(startTime, block.startTime)
        assertEquals(endTime, block.endTime)
        assertEquals("Meeting", block.title)
        assertEquals("Team standup", block.description)
        assertEquals(TimeBlockType.MEETING, block.blockType)
        assertEquals(0xFF0000FF.toInt(), block.color)
        assertEquals(mapOf("location" to "Room 101"), block.metadata)
    }

    @Test
    fun `create time block with default values`() {
        val startTime = LocalDateTime.now()
        val endTime = startTime.plusHours(1)

        val block = TimeBlock(
            id = "test-1",
            startTime = startTime,
            endTime = endTime,
        )

        assertEquals("", block.title)
        assertEquals("", block.description)
        assertEquals(TimeBlockType.CUSTOM, block.blockType)
        assertEquals(null, block.color)
        assertEquals(emptyMap<String, Any>(), block.metadata)
    }

    @Test
    fun `blank id throws exception`() {
        val startTime = LocalDateTime.now()
        val endTime = startTime.plusHours(1)

        assertThrows(IllegalArgumentException::class.java) {
            TimeBlock(
                id = "",
                startTime = startTime,
                endTime = endTime,
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            TimeBlock(
                id = "   ",
                startTime = startTime,
                endTime = endTime,
            )
        }
    }

    @Test
    fun `end time before start time throws exception`() {
        val startTime = LocalDateTime.now()
        val endTime = startTime.minusHours(1)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            TimeBlock(
                id = "test-1",
                startTime = startTime,
                endTime = endTime,
            )
        }

        assertTrue(exception.message!!.contains("must be after"))
    }

    @Test
    fun `end time equal to start time throws exception`() {
        val time = LocalDateTime.now()

        assertThrows(IllegalArgumentException::class.java) {
            TimeBlock(
                id = "test-1",
                startTime = time,
                endTime = time,
            )
        }
    }

    @Test
    fun `duration returns correct value`() {
        val startTime = LocalDateTime.of(2024, 1, 15, 9, 0)
        val endTime = LocalDateTime.of(2024, 1, 15, 11, 30)

        val block = TimeBlock(
            id = "test-1",
            startTime = startTime,
            endTime = endTime,
        )

        assertEquals(Duration.ofHours(2).plusMinutes(30), block.duration())
    }

    @Test
    fun `durationInMinutes returns correct value`() {
        val startTime = LocalDateTime.of(2024, 1, 15, 9, 0)
        val endTime = LocalDateTime.of(2024, 1, 15, 11, 30)

        val block = TimeBlock(
            id = "test-1",
            startTime = startTime,
            endTime = endTime,
        )

        assertEquals(150L, block.durationInMinutes())
    }

    @Test
    fun `durationInHours returns correct value`() {
        val startTime = LocalDateTime.of(2024, 1, 15, 9, 0)
        val endTime = LocalDateTime.of(2024, 1, 15, 11, 30)

        val block = TimeBlock(
            id = "test-1",
            startTime = startTime,
            endTime = endTime,
        )

        assertEquals(2.5, block.durationInHours(), 0.01)
    }

    @Test
    fun `overlapsWith detects overlapping blocks`() {
        val block1 = TimeBlock(
            id = "1",
            startTime = LocalDateTime.of(2024, 1, 15, 9, 0),
            endTime = LocalDateTime.of(2024, 1, 15, 10, 0),
        )

        val block2 = TimeBlock(
            id = "2",
            startTime = LocalDateTime.of(2024, 1, 15, 9, 30),
            endTime = LocalDateTime.of(2024, 1, 15, 10, 30),
        )

        assertTrue(block1.overlapsWith(block2))
        assertTrue(block2.overlapsWith(block1))
    }

    @Test
    fun `overlapsWith detects non-overlapping blocks`() {
        val block1 = TimeBlock(
            id = "1",
            startTime = LocalDateTime.of(2024, 1, 15, 9, 0),
            endTime = LocalDateTime.of(2024, 1, 15, 10, 0),
        )

        val block2 = TimeBlock(
            id = "2",
            startTime = LocalDateTime.of(2024, 1, 15, 10, 0),
            endTime = LocalDateTime.of(2024, 1, 15, 11, 0),
        )

        assertFalse(block1.overlapsWith(block2))
        assertFalse(block2.overlapsWith(block1))
    }

    @Test
    fun `overlapsWith detects adjacent blocks`() {
        val block1 = TimeBlock(
            id = "1",
            startTime = LocalDateTime.of(2024, 1, 15, 9, 0),
            endTime = LocalDateTime.of(2024, 1, 15, 10, 0),
        )

        val block2 = TimeBlock(
            id = "2",
            startTime = LocalDateTime.of(2024, 1, 15, 10, 0),
            endTime = LocalDateTime.of(2024, 1, 15, 11, 0),
        )

        // Adjacent blocks don't overlap
        assertFalse(block1.overlapsWith(block2))
    }

    @Test
    fun `overlapsWith detects contained blocks`() {
        val outer = TimeBlock(
            id = "outer",
            startTime = LocalDateTime.of(2024, 1, 15, 9, 0),
            endTime = LocalDateTime.of(2024, 1, 15, 12, 0),
        )

        val inner = TimeBlock(
            id = "inner",
            startTime = LocalDateTime.of(2024, 1, 15, 10, 0),
            endTime = LocalDateTime.of(2024, 1, 15, 11, 0),
        )

        assertTrue(outer.overlapsWith(inner))
        assertTrue(inner.overlapsWith(outer))
    }

    @Test
    fun `withUpdatedTime creates new block with updated times`() {
        val original = TimeBlock(
            id = "test-1",
            startTime = LocalDateTime.of(2024, 1, 15, 9, 0),
            endTime = LocalDateTime.of(2024, 1, 15, 10, 0),
            title = "Meeting",
        )

        val newStartTime = LocalDateTime.of(2024, 1, 15, 10, 0)
        val newEndTime = LocalDateTime.of(2024, 1, 15, 11, 0)

        val updated = original.withUpdatedTime(newStartTime, newEndTime)

        assertEquals(newStartTime, updated.startTime)
        assertEquals(newEndTime, updated.endTime)
        assertEquals(original.id, updated.id)
        assertEquals(original.title, updated.title)
    }

    @Test
    fun `withUpdatedTime throws on invalid times`() {
        val original = TimeBlock(
            id = "test-1",
            startTime = LocalDateTime.now(),
            endTime = LocalDateTime.now().plusHours(1),
        )

        val newStartTime = LocalDateTime.now()
        val invalidEndTime = newStartTime.minusHours(1)

        assertThrows(IllegalArgumentException::class.java) {
            original.withUpdatedTime(newStartTime, invalidEndTime)
        }
    }

    @Test
    fun `startDate extracts correct date`() {
        val block = TimeBlock(
            id = "test-1",
            startTime = LocalDateTime.of(2024, 1, 15, 9, 0),
            endTime = LocalDateTime.of(2024, 1, 15, 10, 0),
        )

        assertEquals(LocalDate.of(2024, 1, 15), block.startDate())
    }

    @Test
    fun `endDate extracts correct date`() {
        val block = TimeBlock(
            id = "test-1",
            startTime = LocalDateTime.of(2024, 1, 15, 9, 0),
            endTime = LocalDateTime.of(2024, 1, 16, 10, 0),
        )

        assertEquals(LocalDate.of(2024, 1, 16), block.endDate())
    }

    @Test
    fun `isMultiDay returns false for same-day block`() {
        val block = TimeBlock(
            id = "test-1",
            startTime = LocalDateTime.of(2024, 1, 15, 9, 0),
            endTime = LocalDateTime.of(2024, 1, 15, 23, 59),
        )

        assertFalse(block.isMultiDay())
    }

    @Test
    fun `isMultiDay returns true for multi-day block`() {
        val block = TimeBlock(
            id = "test-1",
            startTime = LocalDateTime.of(2024, 1, 15, 9, 0),
            endTime = LocalDateTime.of(2024, 1, 16, 10, 0),
        )

        assertTrue(block.isMultiDay())
    }

    @Test
    fun `isMultiDay returns true for block spanning midnight`() {
        val block = TimeBlock(
            id = "test-1",
            startTime = LocalDateTime.of(2024, 1, 15, 23, 30),
            endTime = LocalDateTime.of(2024, 1, 16, 0, 30),
        )

        assertTrue(block.isMultiDay())
    }

    @Test
    fun `equality works correctly`() {
        val block1 = TimeBlock(
            id = "test-1",
            startTime = LocalDateTime.of(2024, 1, 15, 9, 0),
            endTime = LocalDateTime.of(2024, 1, 15, 10, 0),
            title = "Meeting",
        )

        val block2 = TimeBlock(
            id = "test-1",
            startTime = LocalDateTime.of(2024, 1, 15, 9, 0),
            endTime = LocalDateTime.of(2024, 1, 15, 10, 0),
            title = "Meeting",
        )

        assertEquals(block1, block2)
        assertEquals(block1.hashCode(), block2.hashCode())
    }

    @Test
    fun `inequality works correctly`() {
        val block1 = TimeBlock(
            id = "test-1",
            startTime = LocalDateTime.of(2024, 1, 15, 9, 0),
            endTime = LocalDateTime.of(2024, 1, 15, 10, 0),
        )

        val block2 = TimeBlock(
            id = "test-2",
            startTime = LocalDateTime.of(2024, 1, 15, 9, 0),
            endTime = LocalDateTime.of(2024, 1, 15, 10, 0),
        )

        assertNotEquals(block1, block2)
    }

    @Test
    fun `copy creates independent instance`() {
        val original = TimeBlock(
            id = "test-1",
            startTime = LocalDateTime.of(2024, 1, 15, 9, 0),
            endTime = LocalDateTime.of(2024, 1, 15, 10, 0),
            title = "Original",
        )

        val copy = original.copy(title = "Copy")

        assertEquals("test-1", copy.id)
        assertEquals(original.startTime, copy.startTime)
        assertEquals(original.endTime, copy.endTime)
        assertEquals("Copy", copy.title)
        assertNotEquals(original, copy)
    }

    @Test
    fun `toString contains key information`() {
        val block = TimeBlock(
            id = "test-1",
            startTime = LocalDateTime.of(2024, 1, 15, 9, 0),
            endTime = LocalDateTime.of(2024, 1, 15, 10, 0),
            title = "Meeting",
        )

        val string = block.toString()
        assertTrue(string.contains("test-1"))
        assertTrue(string.contains("Meeting"))
    }
}
