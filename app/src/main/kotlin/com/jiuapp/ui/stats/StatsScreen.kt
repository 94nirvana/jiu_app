package com.jiuapp.ui.stats

import androidx.compose.runtime.getValue
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jiuapp.R
import com.jiuapp.ui.common.currentLocale
import com.jiuapp.domain.belt.Belt
import com.jiuapp.domain.stats.Bucket
import com.jiuapp.domain.stats.HeatMetric
import com.jiuapp.domain.stats.Period
import com.jiuapp.domain.stats.Share
import com.jiuapp.domain.stats.TagCount
import com.jiuapp.domain.training.TrainingType
import com.jiuapp.ui.common.ChoiceChips
import com.jiuapp.ui.common.EmptyState
import com.jiuapp.ui.common.LoadingBox
import com.jiuapp.ui.common.MedicalDisclaimer
import com.jiuapp.ui.common.SectionCard
import com.jiuapp.ui.common.Spacing
import com.jiuapp.ui.common.beltLabel
import com.jiuapp.ui.sessions.typeLabel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.loading) {
        LoadingBox()
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(text = stringResource(R.string.stats_title), style = MaterialTheme.typography.headlineMedium)
        PeriodSelector(state.period, viewModel::setPeriod)
        PeriodNavigator(state, viewModel::shift)
        FilterBar(state, viewModel)

        SectionCard(title = stringResource(R.string.stats_summary)) {
            Text(
                text = stringResource(R.string.stats_summary_line, state.summary.sessions, state.summary.hours),
                style = MaterialTheme.typography.bodyLarge,
            )
            state.summary.changeSessionsPercent?.let {
                Text(
                    text = stringResource(R.string.stats_change, formatSigned(it)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } ?: Text(stringResource(R.string.stats_change_none), style = MaterialTheme.typography.bodySmall)
            StreakAndGoal(state)
        }

        if (state.buckets.sumOf { it.sessions } == 0) {
            EmptyState(R.string.stats_empty)
        } else {
            SectionCard(title = stringResource(R.string.stats_bars_title)) {
                BarChart(state.buckets, state.period)
            }
        }

        SectionCard(title = stringResource(R.string.stats_heatmap_title)) {
            HeatmapSection(state, viewModel)
        }

        SectionCard(title = stringResource(R.string.stats_distribution_title)) {
            state.giShare?.let { ShareBars(stringResource(R.string.stats_gi_vs_nogi), it.first, it.second, R.string.type_gi, R.string.type_no_gi) }
            state.sparringShare?.let { ShareBars(stringResource(R.string.stats_sparring_vs_technical), it.first, it.second, R.string.stats_sparring, R.string.stats_technical) }
        }

        SectionCard(title = stringResource(R.string.stats_time_title)) {
            WeekdayBars(state.weekdayCounts)
            HourLine(state.hourCounts)
        }

        SectionCard(title = stringResource(R.string.stats_belt_title)) {
            state.beltTotals.entries.sortedBy { it.key.ordinal }.forEach { (belt, totals) ->
                Text(
                    text = stringResource(R.string.stats_belt_line, beltLabel(belt), totals.sessions, totals.hours),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (state.beltTotals.isEmpty()) Text(stringResource(R.string.stats_empty), style = MaterialTheme.typography.bodySmall)
        }

        SectionCard(title = stringResource(R.string.stats_tags_places)) {
            TagCloud(state.topPlaces)
        }
        SectionCard(title = stringResource(R.string.stats_tags_focus)) {
            TagCloud(state.topFocus)
        }

        MedicalDisclaimer()
    }
}

@Composable
private fun PeriodSelector(selected: Period, onSelect: (Period) -> Unit) {
    PrimaryTabRow(selectedTabIndex = selected.ordinal) {
        Period.entries.forEach { period ->
            Tab(
                selected = period == selected,
                onClick = { onSelect(period) },
                modifier = Modifier.height(Spacing.touchTarget),
                text = {
                    Text(
                        stringResource(
                            when (period) {
                                Period.WEEK -> R.string.period_week
                                Period.MONTH -> R.string.period_month
                                Period.YEAR -> R.string.period_year
                            },
                        ),
                    )
                },
            )
        }
    }
}

@Composable
private fun PeriodNavigator(state: StatsUiState, onShift: (Long) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton(onClick = { onShift(-1) }, modifier = Modifier.size(Spacing.touchTarget)) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = stringResource(R.string.action_previous))
        }
        Text(text = rangeLabel(state), style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = { onShift(1) }, modifier = Modifier.size(Spacing.touchTarget)) {
            Icon(Icons.Filled.ChevronRight, contentDescription = stringResource(R.string.action_next))
        }
    }
}

@Composable
private fun rangeLabel(state: StatsUiState): String {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", currentLocale())
    return when (state.period) {
        Period.MONTH -> YearMonth.from(state.range.start).format(DateTimeFormatter.ofPattern("MMMM yyyy", currentLocale()))
        Period.YEAR -> state.range.start.year.toString()
        Period.WEEK -> "${state.range.start.format(formatter)} – ${state.range.end.format(formatter)}"
    }
}

@Composable
private fun FilterBar(state: StatsUiState, viewModel: StatsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            TrainingType.entries.forEach { type ->
                FilterChip(
                    selected = type in state.typeFilter,
                    onClick = { viewModel.toggleType(type) },
                    label = { Text(typeLabel(type)) },
                    modifier = Modifier.height(Spacing.touchTarget),
                )
            }
        }
        ChoiceChips(
            options = Belt.entries,
            selected = state.beltFilter,
            label = { beltLabel(it) },
            onSelect = { belt -> viewModel.setBelt(if (belt == state.beltFilter) null else belt) },
        )
    }
}

@Composable
private fun StreakAndGoal(state: StatsUiState) {
    val progress = if (state.goal == 0) 0f else (state.weekSessions.toFloat() / state.goal).coerceAtMost(1f)
    Text(text = stringResource(R.string.stats_goal_line, state.weekSessions, state.goal), style = MaterialTheme.typography.bodyMedium)
    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
    Text(
        text = stringResource(R.string.stats_streak_line, state.streak, state.longestStreak),
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun BarChart(buckets: List<Bucket>, period: Period) {
    val max = (buckets.maxOfOrNull { it.sessions } ?: 0).coerceAtLeast(1)
    val barColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val axisSteps = listOf(0, max / 2, max)
    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(CHART_HEIGHT)
                .semantics {
                    contentDescription = buckets.joinToString { "${it.start}: ${it.sessions}" }
                },
        ) {
            val gap = size.width / (buckets.size * GAP_DIVISOR)
            val barWidth = (size.width - gap * (buckets.size + 1)) / buckets.size
            buckets.forEachIndexed { index, bucket ->
                val left = gap + index * (barWidth + gap)
                drawRect(color = trackColor, topLeft = Offset(left, 0f), size = Size(barWidth, size.height))
                val barHeight = size.height * bucket.sessions / max
                if (barHeight > 0f) {
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(left, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(BAR_RADIUS.toPx()),
                    )
                }
            }
            axisSteps.distinct().forEach { step ->
                val y = size.height - size.height * step / max
                drawLine(color = labelColor.copy(alpha = AXIS_ALPHA), start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = AXIS_STROKE.toPx())
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = bucketLabel(buckets.first(), period), style = MaterialTheme.typography.labelSmall)
            Text(text = bucketLabel(buckets.last(), period), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun bucketLabel(bucket: Bucket, period: Period): String = when (period) {
    Period.WEEK -> bucket.start.dayOfWeek.getDisplayName(TextStyle.SHORT, currentLocale())
    Period.MONTH -> bucket.start.format(DateTimeFormatter.ofPattern("dd/MM"))
    Period.YEAR -> bucket.start.month.getDisplayName(TextStyle.SHORT, currentLocale())
}

@Composable
private fun HeatmapSection(state: StatsUiState, viewModel: StatsViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton(onClick = { viewModel.shiftHeatYear(-1) }, modifier = Modifier.size(Spacing.touchTarget)) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = stringResource(R.string.action_previous))
        }
        Text(text = state.heatYear.toString(), style = MaterialTheme.typography.titleSmall)
        IconButton(onClick = { viewModel.shiftHeatYear(1) }, modifier = Modifier.size(Spacing.touchTarget)) {
            Icon(Icons.Filled.ChevronRight, contentDescription = stringResource(R.string.action_next))
        }
    }
    ChoiceChips(
        options = HeatMetric.entries,
        selected = state.heatMetric,
        label = { stringResource(if (it == HeatMetric.COUNT) R.string.heat_by_count else R.string.heat_by_hours) },
        onSelect = viewModel::setHeatMetric,
    )
    val colors = (0..HEAT_LEVELS).map { heatColor(it) }
    Heatmap(state.heatYear, state.heatLevels, colors)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(stringResource(R.string.heat_less), style = MaterialTheme.typography.labelSmall)
        colors.forEach { color ->
            Canvas(modifier = Modifier.size(HEAT_CELL)) {
                drawRoundRect(color, cornerRadius = CornerRadius(HEAT_RADIUS.toPx()))
            }
        }
        Text(stringResource(R.string.heat_more), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun Heatmap(year: Int, levels: Map<LocalDate, Int>, colors: List<Color>) {
    val first = LocalDate.of(year, 1, 1)
    val leading = first.dayOfWeek.value - 1
    val days = if (java.time.Year.isLeap(year.toLong())) 366 else 365
    val columns = (leading + days + 6) / 7
    val description = stringResource(R.string.heat_description, levels.size)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(columns.toFloat() / DAYS_IN_WEEK)
            .semantics { contentDescription = description },
    ) {
        val cell = size.width / columns
        val inner = cell * HEAT_FILL
        for (index in 0 until days) {
            val date = first.plusDays(index.toLong())
            val column = (index + leading) / DAYS_IN_WEEK
            val row = (date.dayOfWeek.value - 1)
            drawRoundRect(
                color = colors[levels[date] ?: 0],
                topLeft = Offset(column * cell, row * cell),
                size = Size(inner, inner),
                cornerRadius = CornerRadius(inner * HEAT_CORNER),
            )
        }
    }
}

@Composable
private fun heatColor(level: Int): Color {
    val base = MaterialTheme.colorScheme.primary
    return if (level <= 0) MaterialTheme.colorScheme.surfaceVariant else base.copy(alpha = HEAT_ALPHA_STEP * level.coerceIn(1, HEAT_LEVELS) + HEAT_ALPHA_MIN)
}

@Composable
private fun ShareBars(title: String, first: Share, second: Share, firstLabel: Int, secondLabel: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        ShareLine(stringResource(firstLabel), first)
        ShareLine(stringResource(secondLabel), second)
    }
}

@Composable
private fun ShareLine(label: String, share: Share) {
    Column {
        Text(stringResource(R.string.stats_share_line, label, share.count, share.fraction * PERCENT), style = MaterialTheme.typography.bodySmall)
        LinearProgressIndicator(progress = { share.fraction.toFloat() }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun WeekdayBars(counts: Map<DayOfWeek, Int>) {
    val max = (counts.values.maxOrNull() ?: 0).coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(stringResource(R.string.stats_weekday_title), style = MaterialTheme.typography.labelLarge)
        DayOfWeek.entries.forEach { day ->
            val count = counts[day] ?: 0
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(day.getDisplayName(TextStyle.SHORT, currentLocale()), style = MaterialTheme.typography.labelSmall, modifier = Modifier.size(WEEKDAY_LABEL_WIDTH, Spacing.md))
                LinearProgressIndicator(progress = { count.toFloat() / max }, modifier = Modifier.weight(1f))
                Text("$count", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun HourLine(counts: Map<Int, Int>) {
    if (counts.isEmpty()) return
    val top = counts.maxByOrNull { it.value }!!
    Text(
        text = stringResource(R.string.stats_peak_hour, top.key, top.value),
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun TagCloud(tags: List<TagCount>) {
    if (tags.isEmpty()) {
        Text(stringResource(R.string.stats_empty), style = MaterialTheme.typography.bodySmall)
        return
    }
    val max = tags.maxOf { it.count }.toFloat()
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        tags.forEach { tag ->
            val weight = tag.count / max
            Text(
                text = "${tag.name} (${tag.count})",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = (MIN_TAG_SP + (MAX_TAG_SP - MIN_TAG_SP) * weight).sp,
                    fontWeight = if (weight > HEAVY_TAG) FontWeight.SemiBold else FontWeight.Normal,
                ),
            )
        }
    }
}

@Composable
private fun formatSigned(value: Double): String = String.format(currentLocale(), "%+.0f%%", value)

private val CHART_HEIGHT = 160.dp
private val AXIS_STROKE = 1.dp
private val BAR_RADIUS = 4.dp
private val HEAT_CELL = 16.dp
private val HEAT_RADIUS = 3.dp
private val WEEKDAY_LABEL_WIDTH = 40.dp
private const val AXIS_ALPHA = 0.2f
private const val GAP_DIVISOR = 3f
private const val HEAT_FILL = 0.8f
private const val HEAT_CORNER = 0.2f
private const val HEAT_LEVELS = 4
private const val HEAT_ALPHA_STEP = 0.2f
private const val HEAT_ALPHA_MIN = 0.1f
private const val DAYS_IN_WEEK = 7
private const val PERCENT = 100.0
private const val MIN_TAG_SP = 12f
private const val MAX_TAG_SP = 22f
private const val HEAVY_TAG = 0.6f
