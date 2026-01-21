package com.kizitonwose.calendar.sample.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kizitonwose.calendar.compose.NowIndicatorConfig
import com.kizitonwose.calendar.compose.NowIndicatorDefaults
import com.kizitonwose.calendar.compose.NowIndicatorLine
import com.kizitonwose.calendar.compose.calculateTimePositionFraction
import com.kizitonwose.calendar.compose.rememberNowIndicatorState
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Example page demonstrating the Now Indicator (Current Time Line) feature.
 * This shows how to integrate the time indicator into a schedule/day view.
 */
@Composable
fun Example12Page() {
    val dayStartHour = 6 // 6 AM
    val dayEndHour = 22 // 10 PM
    val hourHeightDp = 60.dp

    // State that automatically updates with current time
    val nowIndicatorState = rememberNowIndicatorState(
        updateIntervalMillis = 30_000L, // Update every 30 seconds for demo
    )

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(LocalScaffoldPaddingValues.current),
    ) {
        // Header showing current time
        CurrentTimeHeader(nowIndicatorState.currentTime)

        Spacer(modifier = Modifier.height(8.dp))

        // Legend showing different indicator styles
        IndicatorStyleLegend()

        Spacer(modifier = Modifier.height(16.dp))

        // Day schedule view with Now Indicator
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {
            val totalHours = dayEndHour - dayStartHour
            val totalHeight = hourHeightDp * totalHours

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalHeight),
            ) {
                // Draw hour slots
                for (hour in dayStartHour until dayEndHour) {
                    val offsetY = hourHeightDp * (hour - dayStartHour)
                    HourSlot(
                        hour = hour,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(hourHeightDp)
                            .offset(y = offsetY),
                    )
                }

                // Draw sample events
                SampleEvent(
                    title = "Team Meeting",
                    startHour = 9,
                    endHour = 10,
                    dayStartHour = dayStartHour,
                    hourHeight = hourHeightDp,
                    color = Color(0xFF4285F4),
                )

                SampleEvent(
                    title = "Lunch Break",
                    startHour = 12,
                    endHour = 13,
                    dayStartHour = dayStartHour,
                    hourHeight = hourHeightDp,
                    color = Color(0xFF34A853),
                )

                SampleEvent(
                    title = "Project Review",
                    startHour = 14,
                    endHour = 15,
                    dayStartHour = dayStartHour,
                    hourHeight = hourHeightDp,
                    color = Color(0xFFFBBC05),
                )

                SampleEvent(
                    title = "Code Review",
                    startHour = 16,
                    endHour = 17,
                    dayStartHour = dayStartHour,
                    hourHeight = hourHeightDp,
                    color = Color(0xFFEA4335),
                )

                // Now Indicator - only show if today
                if (nowIndicatorState.isVisibleForDate(LocalDate.now())) {
                    val positionFraction = nowIndicatorState.getPositionFraction(
                        dayStartHour = dayStartHour,
                        dayEndHour = dayEndHour,
                    )

                    positionFraction?.let { fraction ->
                        val indicatorOffsetY = totalHeight * fraction

                        NowIndicatorLine(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = indicatorOffsetY - 5.dp) // Adjust for circle radius
                                .padding(start = 50.dp), // Leave space for hour labels
                            config = NowIndicatorDefaults.Prominent,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentTimeHeader(currentTime: LocalTime) {
    val formatter = remember { DateTimeFormatter.ofPattern("HH:mm:ss") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Now Indicator Demo",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Current Time: ${currentTime.format(formatter)}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Today: ${LocalDate.now()}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun IndicatorStyleLegend() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = "Indicator Styles:",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Default style
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Default", fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                NowIndicatorLine(
                    config = NowIndicatorDefaults.config(),
                    modifier = Modifier.width(80.dp),
                )
            }

            // Minimal style
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Minimal", fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                NowIndicatorLine(
                    config = NowIndicatorDefaults.Minimal,
                    modifier = Modifier.width(80.dp),
                )
            }

            // Prominent style
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Prominent", fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                NowIndicatorLine(
                    config = NowIndicatorDefaults.Prominent,
                    modifier = Modifier.width(80.dp),
                )
            }

            // Custom style
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Custom", fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                NowIndicatorLine(
                    config = NowIndicatorConfig(
                        lineColor = Color(0xFF9C27B0),
                        lineThickness = 3.dp,
                        circleRadius = 8.dp,
                        showCircle = true,
                    ),
                    modifier = Modifier.width(80.dp),
                )
            }
        }
    }
}

@Composable
private fun HourSlot(
    hour: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .border(
                width = 0.5.dp,
                color = Color.LightGray.copy(alpha = 0.5f),
            )
            .padding(4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Hour label
        Text(
            text = String.format("%02d:00", hour),
            modifier = Modifier.width(46.dp),
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun SampleEvent(
    title: String,
    startHour: Int,
    endHour: Int,
    dayStartHour: Int,
    hourHeight: Dp,
    color: Color,
) {
    val offsetY = hourHeight * (startHour - dayStartHour)
    val height = hourHeight * (endHour - startHour)

    Box(
        modifier = Modifier
            .padding(start = 54.dp, end = 8.dp)
            .offset(y = offsetY + 2.dp)
            .fillMaxWidth()
            .height(height - 4.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.2f))
            .border(1.dp, color, RoundedCornerShape(4.dp))
            .padding(8.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = color,
            )
            Text(
                text = "${String.format("%02d:00", startHour)} - ${String.format("%02d:00", endHour)}",
                fontSize = 12.sp,
                color = color.copy(alpha = 0.7f),
            )
        }
    }
}

@Preview
@Composable
private fun Example12Preview() {
    Example12Page()
}
