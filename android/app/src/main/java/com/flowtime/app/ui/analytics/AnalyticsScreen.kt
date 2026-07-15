package com.flowtime.app.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flowtime.app.domain.hexColor
import com.flowtime.app.ui.common.Dot
import com.flowtime.app.ui.common.EmptyBox
import com.flowtime.app.ui.common.ScreenHeader
import com.flowtime.app.ui.common.StateContent

@Composable
fun AnalyticsScreen() {
    val vm: AnalyticsViewModel = viewModel(factory = AnalyticsViewModel.Factory)
    val state by vm.state.collectAsStateWithLifecycle()

    Column {
        ScreenHeader("Analytics", "How your time is distributed this week.")
        StateContent(state, onRetry = { vm.refresh() }) { data ->
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(Icons.Outlined.Schedule, "Scheduled", "${trim(data.totalHours)}h", Modifier.weight(1f))
                    StatCard(Icons.Outlined.Checklist, "On tasks", "${trim(data.taskHours)}h", Modifier.weight(1f), accent = hexColor("#3b82f6"))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(Icons.Outlined.Group, "In meetings", "${trim(data.meetingHours)}h", Modifier.weight(1f))
                    StatCard(Icons.Outlined.CalendarMonth, "Commitments", "${trim(data.commitmentHours)}h", Modifier.weight(1f))
                }

                if (!data.hasData) {
                    Card { EmptyBoxInline() }
                } else {
                    Card {
                        Text("Time distribution", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(12.dp))
                        DonutChart(data.donut)
                    }
                    Card {
                        Text("Hours by day", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(12.dp))
                        StackedBars(data.weekly)
                    }
                }
            }
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)).padding(16.dp),
    ) { content() }
}

@Composable
private fun EmptyBoxInline() {
    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
        Text(
            "No scheduled time this week yet. Hit “Plan my day” on the Planner.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatCard(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier, accent: Color = MaterialTheme.colorScheme.onSurface) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)).padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = accent)
        Text("this week", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DonutChart(slices: List<Slice>) {
    val total = slices.sumOf { it.hours }.takeIf { it > 0 } ?: 1.0
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(150.dp)) {
            val stroke = size.minDimension * 0.18f
            val inset = stroke / 2
            var startAngle = -90f
            val gap = 3f
            slices.forEach { s ->
                val sweep = (s.hours / total * 360.0).toFloat()
                drawArc(
                    color = hexColor(s.colorHex),
                    startAngle = startAngle + gap / 2,
                    sweepAngle = (sweep - gap).coerceAtLeast(0f),
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke),
                )
                startAngle += sweep
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            slices.forEach { s ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Dot(hexColor(s.colorHex))
                    Text("${s.name} · ${trim(s.hours)}h", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun StackedBars(weekly: List<DayBars>) {
    val maxTotal = weekly.maxOf { d -> d.segments.sumOf { it.second } }.takeIf { it > 0 } ?: 1.0
    Column {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(180.dp),
        ) {
            val slot = size.width / weekly.size
            val barW = slot * 0.5f
            weekly.forEachIndexed { i, day ->
                val x = i * slot + (slot - barW) / 2f
                var yBottom = size.height
                day.segments.forEach { (hex, hours) ->
                    if (hours <= 0) return@forEach
                    val h = (hours / maxTotal * size.height).toFloat()
                    drawRect(
                        color = hexColor(hex),
                        topLeft = Offset(x, yBottom - h),
                        size = Size(barW, h),
                    )
                    yBottom -= h
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            weekly.forEach { d ->
                Text(
                    d.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        // Legend
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            legendItem("#475569", "Meeting")
            legendItem("#0e7490", "Commitment")
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            legendItem("#3b82f6", "Task")
            legendItem("#10b981", "Habit")
        }
    }
}

@Composable
private fun legendItem(hex: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Dot(hexColor(hex), size = 9)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun trim(v: Double): String = if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
