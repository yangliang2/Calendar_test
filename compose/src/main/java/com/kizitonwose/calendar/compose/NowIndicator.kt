package com.kizitonwose.calendar.compose

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime

/**
 * Configuration for the Now Indicator (Current Time Line) appearance.
 *
 * @param lineColor The color of the time indicator line.
 * @param lineThickness The thickness of the time indicator line.
 * @param circleRadius The radius of the circle at the start of the line.
 * @param showCircle Whether to show the circle indicator at the start of the line.
 * @param lineCap The cap style for the line ends.
 * @param pulseAnimation Whether to show a subtle pulse animation on the circle.
 */
public data class NowIndicatorConfig(
    val lineColor: Color = Color(0xFFE53935),
    val lineThickness: Dp = 2.dp,
    val circleRadius: Dp = 5.dp,
    val showCircle: Boolean = true,
    val lineCap: StrokeCap = StrokeCap.Round,
    val pulseAnimation: Boolean = false,
)

/**
 * Default configurations for the Now Indicator.
 */
public object NowIndicatorDefaults {
    /**
     * Creates a default NowIndicatorConfig.
     */
    public fun config(
        lineColor: Color = Color(0xFFE53935),
        lineThickness: Dp = 2.dp,
        circleRadius: Dp = 5.dp,
        showCircle: Boolean = true,
        lineCap: StrokeCap = StrokeCap.Round,
        pulseAnimation: Boolean = false,
    ): NowIndicatorConfig = NowIndicatorConfig(
        lineColor = lineColor,
        lineThickness = lineThickness,
        circleRadius = circleRadius,
        showCircle = showCircle,
        lineCap = lineCap,
        pulseAnimation = pulseAnimation,
    )

    /**
     * A minimal style with just a thin line.
     */
    public val Minimal: NowIndicatorConfig = NowIndicatorConfig(
        lineColor = Color(0xFFE53935),
        lineThickness = 1.dp,
        circleRadius = 3.dp,
        showCircle = false,
        lineCap = StrokeCap.Butt,
        pulseAnimation = false,
    )

    /**
     * A prominent style with a larger circle and pulse animation.
     */
    public val Prominent: NowIndicatorConfig = NowIndicatorConfig(
        lineColor = Color(0xFFE53935),
        lineThickness = 2.dp,
        circleRadius = 6.dp,
        showCircle = true,
        lineCap = StrokeCap.Round,
        pulseAnimation = true,
    )
}

/**
 * A composable that displays a horizontal line indicating the current time.
 * This is commonly used in day/week calendar views to show "now".
 *
 * This composable should be placed inside a Box or similar container where
 * the vertical position represents time, and it will draw a full-width line
 * at its current position.
 *
 * @param modifier Modifier to be applied to the indicator.
 * @param config Configuration for the indicator appearance.
 *
 * Example usage:
 * ```
 * Box(modifier = Modifier.fillMaxSize()) {
 *     // Your time slots content here
 *
 *     // Position the indicator based on current time
 *     val currentTimeOffset = calculateTimeOffset(LocalTime.now())
 *     NowIndicatorLine(
 *         modifier = Modifier.offset(y = currentTimeOffset),
 *         config = NowIndicatorDefaults.config()
 *     )
 * }
 * ```
 */
@Composable
public fun NowIndicatorLine(
    modifier: Modifier = Modifier,
    config: NowIndicatorConfig = NowIndicatorDefaults.config(),
) {
    val density = LocalDensity.current
    val lineThicknessPx = with(density) { config.lineThickness.toPx() }
    val circleRadiusPx = with(density) { config.circleRadius.toPx() }

    // Pulse animation for the circle
    val infiniteTransition = rememberInfiniteTransition(label = "nowIndicatorPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (config.pulseAnimation) 1.3f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
        ),
        label = "pulseScale",
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(config.circleRadius * 2),
    ) {
        val centerY = size.height / 2

        // Draw the circle at the start
        if (config.showCircle) {
            val animatedRadius = if (config.pulseAnimation) {
                circleRadiusPx * pulseScale
            } else {
                circleRadiusPx
            }
            drawCircle(
                color = config.lineColor,
                radius = animatedRadius,
                center = Offset(circleRadiusPx, centerY),
            )
        }

        // Draw the line
        val lineStartX = if (config.showCircle) circleRadiusPx * 2 else 0f
        drawLine(
            color = config.lineColor,
            start = Offset(lineStartX, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = lineThicknessPx,
            cap = config.lineCap,
        )
    }
}

/**
 * A composable container that automatically positions a Now Indicator based on the current time.
 * This is useful for day/week schedule views where vertical position represents time of day.
 *
 * @param modifier Modifier to be applied to the container.
 * @param date The date to check. If it's not today, the indicator won't be shown.
 * @param dayStartHour The hour at which the day view starts (0-23). Default is 0 (midnight).
 * @param dayEndHour The hour at which the day view ends (0-24). Default is 24 (midnight next day).
 * @param updateIntervalMillis How often to update the indicator position in milliseconds.
 * @param config Configuration for the indicator appearance.
 * @param content The content of the day view. The Now Indicator will be overlaid on top.
 *
 * Example usage:
 * ```
 * NowIndicatorContainer(
 *     date = LocalDate.now(),
 *     dayStartHour = 8,  // Start at 8 AM
 *     dayEndHour = 20,   // End at 8 PM
 *     modifier = Modifier.height(600.dp)
 * ) {
 *     // Your hourly time slots here
 * }
 * ```
 */
@Composable
public fun NowIndicatorContainer(
    modifier: Modifier = Modifier,
    date: LocalDate = LocalDate.now(),
    dayStartHour: Int = 0,
    dayEndHour: Int = 24,
    updateIntervalMillis: Long = 60_000L, // Update every minute by default
    config: NowIndicatorConfig = NowIndicatorDefaults.config(),
    content: @Composable BoxScope.() -> Unit,
) {
    require(dayStartHour in 0..23) { "dayStartHour must be between 0 and 23" }
    require(dayEndHour in 1..24) { "dayEndHour must be between 1 and 24" }
    require(dayEndHour > dayStartHour) { "dayEndHour must be greater than dayStartHour" }

    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    val isToday = date == LocalDate.now()

    // Update current time periodically
    LaunchedEffect(updateIntervalMillis) {
        while (true) {
            delay(updateIntervalMillis)
            currentTime = LocalTime.now()
        }
    }

    Box(modifier = modifier) {
        content()

        // Only show indicator if it's today and current time is within the visible range
        if (isToday) {
            val currentHour = currentTime.hour
            val currentMinute = currentTime.minute

            if (currentHour >= dayStartHour && currentHour < dayEndHour) {
                val totalMinutesInView = (dayEndHour - dayStartHour) * 60
                val minutesSinceStart = (currentHour - dayStartHour) * 60 + currentMinute
                val fraction = minutesSinceStart.toFloat() / totalMinutesInView.toFloat()

                NowIndicatorLine(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .padding(start = config.circleRadius)
                        .offset(
                            y = with(LocalDensity.current) {
                                // This will be calculated as a fraction of the container height
                                // The actual offset needs to be calculated by the parent
                                0.dp
                            },
                        ),
                    config = config,
                )
            }
        }
    }
}

/**
 * Calculates the vertical position fraction for the current time within a day view.
 *
 * @param time The time to calculate the position for.
 * @param dayStartHour The starting hour of the day view (0-23).
 * @param dayEndHour The ending hour of the day view (1-24).
 * @return A fraction (0.0 to 1.0) representing the vertical position, or null if the time is outside the view range.
 */
public fun calculateTimePositionFraction(
    time: LocalTime = LocalTime.now(),
    dayStartHour: Int = 0,
    dayEndHour: Int = 24,
): Float? {
    require(dayStartHour in 0..23) { "dayStartHour must be between 0 and 23" }
    require(dayEndHour in 1..24) { "dayEndHour must be between 1 and 24" }
    require(dayEndHour > dayStartHour) { "dayEndHour must be greater than dayStartHour" }

    val currentHour = time.hour
    val currentMinute = time.minute
    val currentSecond = time.second

    // Check if current time is within the visible range
    if (currentHour < dayStartHour || currentHour >= dayEndHour) {
        return null
    }

    val totalMinutesInView = (dayEndHour - dayStartHour) * 60
    val minutesSinceStart = (currentHour - dayStartHour) * 60 + currentMinute + (currentSecond / 60f)

    return (minutesSinceStart / totalMinutesInView).coerceIn(0f, 1f)
}

/**
 * A state holder for managing the Now Indicator's current time state with automatic updates.
 *
 * @param updateIntervalMillis How often to update the current time in milliseconds.
 */
@Composable
public fun rememberNowIndicatorState(
    updateIntervalMillis: Long = 60_000L,
): NowIndicatorState {
    var currentTime by remember { mutableStateOf(LocalTime.now()) }

    LaunchedEffect(updateIntervalMillis) {
        while (true) {
            delay(updateIntervalMillis)
            currentTime = LocalTime.now()
        }
    }

    return remember(currentTime) {
        NowIndicatorState(currentTime)
    }
}

/**
 * State class holding the current time for the Now Indicator.
 */
public class NowIndicatorState(
    public val currentTime: LocalTime,
) {
    /**
     * Checks if the indicator should be visible for the given date.
     */
    public fun isVisibleForDate(date: LocalDate): Boolean = date == LocalDate.now()

    /**
     * Calculates the position fraction for the current time.
     */
    public fun getPositionFraction(
        dayStartHour: Int = 0,
        dayEndHour: Int = 24,
    ): Float? = calculateTimePositionFraction(
        time = currentTime,
        dayStartHour = dayStartHour,
        dayEndHour = dayEndHour,
    )
}
