package com.kizitonwose.calendar.compose

import com.kizitonwose.calendar.core.TimeBlock
import com.kizitonwose.calendar.core.TimeBlockType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Unit tests for [TimeBlockStore].
 *
 * Tests cover:
 * - Factory methods
 * - CRUD operations (add, update, remove, clear)
 * - Query operations (get by date, range, ID)
 * - Bulk operations
 * - Edge cases and error handling
 * - State management
 */
class TimeBlockStoreTest {

    private lateinit var store: TimeBlockStore

    private fun createBlock(
        id: String,
        date: LocalDate = LocalDate.of(2024, 1, 15),
        startHour: Int = 9,
        endHour: Int = 10,
        type: TimeBlockType = TimeBlockType.WORK,
    ): TimeBlock {
        return TimeBlock(
            id = id,
            startTime = date.atTime(startHour, 0),
            endTime = date.atTime(endHour, 0),
            title = "Block $id",
            blockType = type,
        )
    }

    @BeforeEach
    fun setup() {
        store = TimeBlockStore.create()
    }

    // Factory Method Tests

    @Test
    fun `create() returns empty store`() {
        val store = TimeBlockStore.create()

        assertTrue(store.isEmpty())
        assertEquals(0, store.size())
    }

    @Test
    fun `create(initialBlocks) populates store`() {
        val blocks = listOf(
            createBlock("1"),
            createBlock("2"),
        )

        val store = TimeBlockStore.create(blocks)

        assertEquals(2, store.size())
        assertFalse(store.isEmpty())
    }

    // Add Operations

    @Test
    fun `addBlock adds single block`() {
        val block = createBlock("1")

        store.addBlock(block)

        assertEquals(1, store.size())
        assertTrue(store.containsBlock("1"))
    }

    @Test
    fun `addBlock replaces existing block with same ID`() {
        val block1 = createBlock("1", startHour = 9, endHour = 10)
        val block2 = createBlock("1", startHour = 10, endHour = 11)

        store.addBlock(block1)
        store.addBlock(block2)

        assertEquals(1, store.size())
        val retrieved = store.getBlockById("1")
        assertEquals(10, retrieved?.startTime?.hour)
    }

    @Test
    fun `addBlocks adds multiple blocks`() {
        val blocks = listOf(
            createBlock("1"),
            createBlock("2"),
            createBlock("3"),
        )

        store.addBlocks(blocks)

        assertEquals(3, store.size())
    }

    @Test
    fun `addBlocks handles empty list`() {
        store.addBlocks(emptyList())

        assertTrue(store.isEmpty())
    }

    @Test
    fun `addBlocks deduplicates by ID`() {
        val blocks = listOf(
            createBlock("1", startHour = 9, endHour = 10),
            createBlock("1", startHour = 10, endHour = 11),
            createBlock("2"),
        )

        store.addBlocks(blocks)

        assertEquals(2, store.size())
        val block1 = store.getBlockById("1")
        assertEquals(10, block1?.startTime?.hour)
    }

    // Update Operations

    @Test
    fun `updateBlock updates existing block`() {
        val original = createBlock("1", startHour = 9, endHour = 10)
        store.addBlock(original)

        val updated = original.copy(title = "Updated")
        val result = store.updateBlock(updated)

        assertTrue(result)
        val retrieved = store.getBlockById("1")
        assertEquals("Updated", retrieved?.title)
    }

    @Test
    fun `updateBlock returns false for non-existent block`() {
        val block = createBlock("1")

        val result = store.updateBlock(block)

        assertFalse(result)
    }

    @Test
    fun `updateBlock moves block to new date`() {
        val original = createBlock("1", date = LocalDate.of(2024, 1, 15))
        store.addBlock(original)

        val newDate = LocalDate.of(2024, 1, 16)
        val updated = createBlock("1", date = newDate)
        store.updateBlock(updated)

        val oldDateBlocks = store.getBlocksForDate(LocalDate.of(2024, 1, 15))
        val newDateBlocks = store.getBlocksForDate(newDate)

        assertTrue(oldDateBlocks.isEmpty())
        assertEquals(1, newDateBlocks.size)
    }

    // Remove Operations

    @Test
    fun `removeBlock by ID removes block`() {
        val block = createBlock("1")
        store.addBlock(block)

        val result = store.removeBlock("1")

        assertTrue(result)
        assertTrue(store.isEmpty())
        assertFalse(store.containsBlock("1"))
    }

    @Test
    fun `removeBlock by object removes block`() {
        val block = createBlock("1")
        store.addBlock(block)

        val result = store.removeBlock(block)

        assertTrue(result)
        assertTrue(store.isEmpty())
    }

    @Test
    fun `removeBlock returns false for non-existent block`() {
        val result = store.removeBlock("nonexistent")

        assertFalse(result)
    }

    @Test
    fun `removeBlocksForDate removes all blocks on date`() {
        val date = LocalDate.of(2024, 1, 15)
        store.addBlock(createBlock("1", date = date))
        store.addBlock(createBlock("2", date = date))
        store.addBlock(createBlock("3", date = LocalDate.of(2024, 1, 16)))

        val removed = store.removeBlocksForDate(date)

        assertEquals(2, removed)
        assertEquals(1, store.size())
    }

    @Test
    fun `removeBlocksForDate returns 0 for empty date`() {
        val removed = store.removeBlocksForDate(LocalDate.of(2024, 1, 15))

        assertEquals(0, removed)
    }

    @Test
    fun `clear removes all blocks`() {
        store.addBlocks(
            listOf(
                createBlock("1"),
                createBlock("2"),
                createBlock("3"),
            ),
        )

        store.clear()

        assertTrue(store.isEmpty())
        assertEquals(0, store.size())
    }

    // Query Operations

    @Test
    fun `getBlocksForDate returns blocks on specific date`() {
        val date = LocalDate.of(2024, 1, 15)
        store.addBlock(createBlock("1", date = date, startHour = 9))
        store.addBlock(createBlock("2", date = date, startHour = 10))
        store.addBlock(createBlock("3", date = LocalDate.of(2024, 1, 16)))

        val blocks = store.getBlocksForDate(date)

        assertEquals(2, blocks.size)
        assertEquals("1", blocks[0].id)
        assertEquals("2", blocks[1].id)
    }

    @Test
    fun `getBlocksForDate returns sorted by start time`() {
        val date = LocalDate.of(2024, 1, 15)
        store.addBlock(createBlock("1", date = date, startHour = 11))
        store.addBlock(createBlock("2", date = date, startHour = 9))
        store.addBlock(createBlock("3", date = date, startHour = 10))

        val blocks = store.getBlocksForDate(date)

        assertEquals("2", blocks[0].id)
        assertEquals("3", blocks[1].id)
        assertEquals("1", blocks[2].id)
    }

    @Test
    fun `getBlocksForDate returns empty list for date with no blocks`() {
        val blocks = store.getBlocksForDate(LocalDate.of(2024, 1, 15))

        assertTrue(blocks.isEmpty())
    }

    @Test
    fun `getBlocksInRange returns blocks in date range`() {
        store.addBlock(createBlock("1", date = LocalDate.of(2024, 1, 15)))
        store.addBlock(createBlock("2", date = LocalDate.of(2024, 1, 16)))
        store.addBlock(createBlock("3", date = LocalDate.of(2024, 1, 17)))
        store.addBlock(createBlock("4", date = LocalDate.of(2024, 1, 18)))

        val blocks = store.getBlocksInRange(
            LocalDate.of(2024, 1, 16),
            LocalDate.of(2024, 1, 17),
        )

        assertEquals(2, blocks.size)
        assertTrue(blocks.any { it.id == "2" })
        assertTrue(blocks.any { it.id == "3" })
    }

    @Test
    fun `getBlocksInRange handles single day range`() {
        val date = LocalDate.of(2024, 1, 15)
        store.addBlock(createBlock("1", date = date))

        val blocks = store.getBlocksInRange(date, date)

        assertEquals(1, blocks.size)
    }

    @Test
    fun `getBlocksInRange throws on invalid range`() {
        assertThrows(IllegalArgumentException::class.java) {
            store.getBlocksInRange(
                LocalDate.of(2024, 1, 16),
                LocalDate.of(2024, 1, 15),
            )
        }
    }

    @Test
    fun `getBlocksInRange handles multi-day blocks`() {
        val multiDayBlock = TimeBlock(
            id = "multi",
            startTime = LocalDate.of(2024, 1, 15).atTime(20, 0),
            endTime = LocalDate.of(2024, 1, 16).atTime(2, 0),
        )
        store.addBlock(multiDayBlock)

        val blocks = store.getBlocksInRange(
            LocalDate.of(2024, 1, 15),
            LocalDate.of(2024, 1, 16),
        )

        // Multi-day block indexed by start date only
        assertEquals(1, blocks.size)
        assertEquals("multi", blocks[0].id)
    }

    @Test
    fun `getBlockById returns matching block`() {
        store.addBlock(createBlock("1"))
        store.addBlock(createBlock("2"))

        val block = store.getBlockById("2")

        assertNotNull(block)
        assertEquals("2", block?.id)
    }

    @Test
    fun `getBlockById returns null for non-existent ID`() {
        val block = store.getBlockById("nonexistent")

        assertNull(block)
    }

    @Test
    fun `getAllBlocks returns all blocks sorted`() {
        store.addBlock(createBlock("1", date = LocalDate.of(2024, 1, 16), startHour = 10))
        store.addBlock(createBlock("2", date = LocalDate.of(2024, 1, 15), startHour = 9))
        store.addBlock(createBlock("3", date = LocalDate.of(2024, 1, 16), startHour = 9))

        val blocks = store.getAllBlocks()

        assertEquals(3, blocks.size)
        assertEquals("2", blocks[0].id) // 2024-01-15 09:00
        assertEquals("3", blocks[1].id) // 2024-01-16 09:00
        assertEquals("1", blocks[2].id) // 2024-01-16 10:00
    }

    // Utility Methods

    @Test
    fun `containsBlock returns true for existing block`() {
        store.addBlock(createBlock("1"))

        assertTrue(store.containsBlock("1"))
    }

    @Test
    fun `containsBlock returns false for non-existent block`() {
        assertFalse(store.containsBlock("nonexistent"))
    }

    @Test
    fun `size returns correct count`() {
        assertEquals(0, store.size())

        store.addBlock(createBlock("1"))
        assertEquals(1, store.size())

        store.addBlock(createBlock("2"))
        assertEquals(2, store.size())

        store.removeBlock("1")
        assertEquals(1, store.size())
    }

    @Test
    fun `isEmpty returns correct value`() {
        assertTrue(store.isEmpty())

        store.addBlock(createBlock("1"))
        assertFalse(store.isEmpty())

        store.clear()
        assertTrue(store.isEmpty())
    }

    @Test
    fun `toString includes useful information`() {
        store.addBlock(createBlock("1"))
        store.addBlock(createBlock("2"))

        val string = store.toString()

        assertTrue(string.contains("TimeBlockStore"))
        assertTrue(string.contains("blocks=2"))
    }

    // Edge Cases

    @Test
    fun `multiple blocks on same date and time`() {
        val date = LocalDate.of(2024, 1, 15)
        store.addBlock(createBlock("1", date = date, startHour = 9, endHour = 10))
        store.addBlock(createBlock("2", date = date, startHour = 9, endHour = 10))

        assertEquals(2, store.size())
        val blocks = store.getBlocksForDate(date)
        assertEquals(2, blocks.size)
    }

    @Test
    fun `blocks across year boundary`() {
        store.addBlock(createBlock("1", date = LocalDate.of(2023, 12, 31)))
        store.addBlock(createBlock("2", date = LocalDate.of(2024, 1, 1)))

        val blocks = store.getBlocksInRange(
            LocalDate.of(2023, 12, 31),
            LocalDate.of(2024, 1, 1),
        )

        assertEquals(2, blocks.size)
    }

    @Test
    fun `blocks on leap day`() {
        val leapDay = LocalDate.of(2024, 2, 29)
        store.addBlock(createBlock("leap", date = leapDay))

        val blocks = store.getBlocksForDate(leapDay)

        assertEquals(1, blocks.size)
        assertEquals("leap", blocks[0].id)
    }

    @Test
    fun `large number of blocks`() {
        val blocks = (1..1000).map { createBlock("$it", date = LocalDate.of(2024, 1, it % 28 + 1)) }
        store.addBlocks(blocks)

        assertEquals(1000, store.size())
        assertNotNull(store.getBlockById("500"))
    }
}
