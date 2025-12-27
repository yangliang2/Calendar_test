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
