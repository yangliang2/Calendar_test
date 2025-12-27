package com.kizitonwose.calendar.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import com.kizitonwose.calendar.core.TimeBlock
import com.kizitonwose.calendar.core.TimeBlockType
import java.time.LocalDate

/**
 * Creates a [CalendarState] with time block support that is remembered across compositions.
 *
 * This function combines the standard calendar state with a [TimeBlockStore] for managing
 * time blocks. The time block store is automatically saved and restored across configuration
 * changes.
 *
 * @param startMonth the initial value for [CalendarState.startMonth]
 * @param endMonth the initial value for [CalendarState.endMonth]
 * @param firstDayOfWeek the initial value for [CalendarState.firstDayOfWeek]
 * @param firstVisibleMonth the initial value for [CalendarState.firstVisibleMonth]
 * @param outDateStyle the initial value for [CalendarState.outDateStyle]
 * @param initialTimeBlocks optional list of initial time blocks to populate the store
 *
 * @return A pair of [CalendarState] and [TimeBlockStore] that work together.
 *
 * @see rememberCalendarState
 * @see TimeBlockStore
 *
 * @since 3.0.0
 */
@Composable
public fun rememberCalendarStateWithTimeBlocks(
    startMonth: java.time.YearMonth = java.time.YearMonth.now(),
    endMonth: java.time.YearMonth = startMonth,
    firstVisibleMonth: java.time.YearMonth = startMonth,
    firstDayOfWeek: java.time.DayOfWeek = com.kizitonwose.calendar.core.firstDayOfWeekFromLocale(),
    outDateStyle: com.kizitonwose.calendar.core.OutDateStyle = com.kizitonwose.calendar.core.OutDateStyle.EndOfRow,
    initialTimeBlocks: List<TimeBlock> = emptyList(),
): Pair<CalendarState, TimeBlockStore> {
    val calendarState = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = firstVisibleMonth,
        firstDayOfWeek = firstDayOfWeek,
        outDateStyle = outDateStyle,
    )
    
    val timeBlockStore = rememberSaveable(
        saver = TimeBlockStoreSaver,
    ) {
        TimeBlockStore.create(initialTimeBlocks)
    }
    
    return calendarState to timeBlockStore
}

/**
 * Saver for [TimeBlockStore] to support rememberSaveable.
 *
 * This saver serializes all time blocks in the store and restores them on configuration change.
 * Note: Large numbers of time blocks may impact save/restore performance.
 */
private val TimeBlockStoreSaver: Saver<TimeBlockStore, Any> = listSaver(
    save = { store ->
        // Save as a list of time blocks
        store.getAllBlocks()
    },
    restore = { savedBlocks ->
        @Suppress("UNCHECKED_CAST")
        TimeBlockStore.create(savedBlocks as List<TimeBlock>)
    },
)

/**
 * Extension property to add time block management to an existing [CalendarState].
 *
 * **Note**: This creates a new [TimeBlockStore] on every read. For production use,
 * prefer [rememberCalendarStateWithTimeBlocks] or store the TimeBlockStore separately.
 *
 * This extension is primarily useful for quick prototyping or when the time block store
 * doesn't need to be persisted.
 *
 * ## Example Usage
 * ```kotlin
 * val calendarState = rememberCalendarState()
 * val timeBlockStore = remember { TimeBlockStore.create() }
 *
 * // Add a time block
 * timeBlockStore.addBlock(
 *     TimeBlock(
 *         id = "meeting-1",
 *         startTime = LocalDateTime.now(),
 *         endTime = LocalDateTime.now().plusHours(1),
 *         title = "Team Meeting"
 *     )
 * )
 *
 * // Get blocks for today
 * val todayBlocks = timeBlockStore.getBlocksForDate(LocalDate.now())
 * ```
 *
 * @see rememberCalendarStateWithTimeBlocks
 * @see TimeBlockStore
 *
 * @since 3.0.0
 */
@Composable
public fun CalendarState.rememberTimeBlockStore(
    initialBlocks: List<TimeBlock> = emptyList(),
): TimeBlockStore {
    return remember(this) {
        TimeBlockStore.create(initialBlocks)
    }
}

/**
 * Gets all time blocks for a specific date.
 *
 * This is a convenience extension that delegates to [TimeBlockStore.getBlocksForDate].
 *
 * @param store The time block store to query.
 * @param date The date to query.
 * @return List of time blocks starting on the specified date, sorted by start time.
 *
 * @see TimeBlockStore.getBlocksForDate
 *
 * @since 3.0.0
 */
public fun CalendarState.getTimeBlocksForDate(
    store: TimeBlockStore,
    date: LocalDate,
): List<TimeBlock> = store.getBlocksForDate(date)

/**
 * Gets all time blocks in a date range.
 *
 * This is a convenience extension that delegates to [TimeBlockStore.getBlocksInRange].
 *
 * @param store The time block store to query.
 * @param startDate The start of the date range (inclusive).
 * @param endDate The end of the date range (inclusive).
 * @return List of time blocks overlapping the range, sorted by start time.
 *
 * @see TimeBlockStore.getBlocksInRange
 *
 * @since 3.0.0
 */
public fun CalendarState.getTimeBlocksInRange(
    store: TimeBlockStore,
    startDate: LocalDate,
    endDate: LocalDate,
): List<TimeBlock> = store.getBlocksInRange(startDate, endDate)

/**
 * Gets all time blocks for the currently visible month range.
 *
 * This queries all time blocks that overlap with the first and last visible months
 * in the calendar. Useful for rendering time blocks in the current viewport.
 *
 * @param store The time block store to query.
 * @return List of time blocks visible in the current month range.
 *
 * @since 3.0.0
 */
@Composable
public fun CalendarState.getVisibleTimeBlocks(store: TimeBlockStore): List<TimeBlock> {
    val startDate = this.firstVisibleMonth.yearMonth.atDay(1)
    val endDate = this.lastVisibleMonth.yearMonth.atEndOfMonth()
    return store.getBlocksInRange(startDate, endDate)
}

/**
 * Filters time blocks by type.
 *
 * @param store The time block store to query.
 * @param date The date to query.
 * @param type The time block type to filter by.
 * @return List of time blocks of the specified type on the given date.
 *
 * @since 3.0.0
 */
public fun CalendarState.getTimeBlocksByType(
    store: TimeBlockStore,
    date: LocalDate,
    type: TimeBlockType,
): List<TimeBlock> = store.getBlocksForDate(date).filter { it.blockType == type }

/**
 * Checks if a specific date has any time blocks.
 *
 * @param store The time block store to query.
 * @param date The date to check.
 * @return true if the date has at least one time block, false otherwise.
 *
 * @since 3.0.0
 */
public fun CalendarState.hasTimeBlocksOnDate(
    store: TimeBlockStore,
    date: LocalDate,
): Boolean = store.getBlocksForDate(date).isNotEmpty()

/**
 * Gets the total count of time blocks for a specific date.
 *
 * @param store The time block store to query.
 * @param date The date to query.
 * @return The number of time blocks on the date.
 *
 * @since 3.0.0
 */
public fun CalendarState.getTimeBlockCount(
    store: TimeBlockStore,
    date: LocalDate,
): Int = store.getBlocksForDate(date).size
