package com.kizitonwose.calendar.core

import androidx.compose.runtime.Immutable
import java.io.Serializable
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

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

    /**
     * Extracts the date component from the time block's start time.
     *
     * @return The LocalDate of the start time.
     */
    public fun startDate(): LocalDate = startTime.toLocalDate()

    /**
     * Extracts the date component from the time block's end time.
     *
     * @return The LocalDate of the end time.
     */
    public fun endDate(): LocalDate = endTime.toLocalDate()

    /**
     * Checks if the time block spans multiple days.
     *
     * @return true if the block crosses midnight, false otherwise.
     */
    public fun isMultiDay(): Boolean = startDate() != endDate()

    public companion object {
        private const val serialVersionUID: Long = 1L
    }
}
