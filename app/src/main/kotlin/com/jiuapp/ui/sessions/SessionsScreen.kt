package com.jiuapp.ui.sessions

import androidx.compose.runtime.getValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jiuapp.R
import com.jiuapp.ui.common.currentLocale
import com.jiuapp.domain.training.TrainingSession
import com.jiuapp.domain.training.TrainingType
import com.jiuapp.ui.common.EmptyState
import com.jiuapp.ui.common.LoadingBox
import com.jiuapp.ui.common.Spacing
import com.jiuapp.ui.common.rankLabel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun SessionsScreen(
    onOpenSession: (String) -> Unit,
    onAddSession: () -> Unit,
    viewModel: SessionsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.loading) {
        LoadingBox()
        return
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(text = stringResource(R.string.sessions_title), style = MaterialTheme.typography.headlineMedium)
            PrimaryTabRow(selectedTabIndex = state.view.ordinal) {
                SessionsView.entries.forEach { view ->
                    Tab(
                        selected = state.view == view,
                        onClick = { viewModel.setView(view) },
                        modifier = Modifier.height(Spacing.touchTarget),
                        text = {
                            Text(stringResource(if (view == SessionsView.LIST) R.string.sessions_view_list else R.string.sessions_view_calendar))
                        },
                    )
                }
            }
            when (state.view) {
                SessionsView.LIST -> SessionList(state, viewModel, onOpenSession)
                SessionsView.CALENDAR -> CalendarView(state, viewModel, onOpenSession)
            }
        }
        FloatingActionButton(
            onClick = onAddSession,
            modifier = Modifier.align(Alignment.BottomEnd).padding(Spacing.md),
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.sessions_add))
        }
    }
}

@Composable
private fun SessionList(state: SessionsUiState, viewModel: SessionsViewModel, onOpenSession: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        OutlinedTextField(
            value = state.filter.query,
            onValueChange = viewModel::setQuery,
            label = { Text(stringResource(R.string.sessions_search)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        TypeFilterRow(state.filter.types, viewModel::toggleType)
    }
    if (state.sessions.isEmpty()) {
        EmptyState(R.string.sessions_empty)
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.sm), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp)) {
        items(state.sessions, key = { it.id }) { session ->
            SessionRow(session) { onOpenSession(session.id) }
        }
    }
}

@Composable
private fun TypeFilterRow(selected: Set<TrainingType>, onToggle: (TrainingType) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        items(TrainingType.entries) { type ->
            FilterChip(
                selected = type in selected,
                onClick = { onToggle(type) },
                label = { Text(typeLabel(type)) },
                modifier = Modifier.height(Spacing.touchTarget),
            )
        }
    }
}

@Composable
private fun CalendarView(state: SessionsUiState, viewModel: SessionsViewModel, onOpenSession: (String) -> Unit) {
    MonthHeader(state.month, viewModel::previousMonth, viewModel::nextMonth)
    MonthGrid(state.month, state.trainedDays, state.selectedDay, viewModel::selectDay)
    val day = state.selectedDay
    if (day != null) {
        val dayAll = state.sessions.filter { it.date == day }
        Text(
            text = day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(currentLocale())),
            style = MaterialTheme.typography.titleMedium,
        )
        if (dayAll.isEmpty()) {
            EmptyState(R.string.sessions_day_empty)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                items(dayAll, key = { it.id }) { session -> SessionRow(session) { onOpenSession(session.id) } }
            }
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton(onClick = onPrevious, modifier = Modifier.size(Spacing.touchTarget)) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = stringResource(R.string.action_previous))
        }
        Text(
            text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", currentLocale())),
            style = MaterialTheme.typography.titleMedium,
        )
        IconButton(onClick = onNext, modifier = Modifier.size(Spacing.touchTarget)) {
            Icon(Icons.Filled.ChevronRight, contentDescription = stringResource(R.string.action_next))
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    trained: Map<LocalDate, Int>,
    selected: LocalDate?,
    onSelect: (LocalDate) -> Unit,
) {
    val first = month.atDay(1)
    val leading = (first.dayOfWeek.value - 1)
    val cells = leading + month.lengthOfMonth()
    val rows = (cells + DAYS_IN_WEEK - 1) / DAYS_IN_WEEK
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                for (column in 0 until DAYS_IN_WEEK) {
                    val index = row * DAYS_IN_WEEK + column
                    val dayNumber = index - leading + 1
                    if (dayNumber in 1..month.lengthOfMonth()) {
                        val date = month.atDay(dayNumber)
                        DayCell(date, trained[date] ?: 0, date == selected) { onSelect(date) }
                    } else {
                        Box(modifier = Modifier.size(Spacing.touchTarget))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, count: Int, selected: Boolean, onClick: () -> Unit) {
    val trainedDescription = if (count > 0) stringResource(R.string.sessions_day_trained, count) else stringResource(R.string.sessions_day_none)
    Box(
        modifier = Modifier
            .size(Spacing.touchTarget)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "${date.dayOfMonth}, $trainedDescription" },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(Spacing.touchTarget - Spacing.xs)
                .clip(CircleShape)
                .then(
                    if (count > 0) Modifier.padding(0.dp) else Modifier,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun SessionRow(session: TrainingSession, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = session.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(currentLocale())),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.sessions_row_detail, typeLabel(session.type), session.durationMinutes),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(text = rankLabel(session.rankOnDay), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun typeLabel(type: TrainingType): String = stringResource(
    when (type) {
        TrainingType.GI -> R.string.type_gi
        TrainingType.NO_GI -> R.string.type_no_gi
        TrainingType.DRILLING -> R.string.type_drilling
        TrainingType.SPARRING -> R.string.type_sparring
        TrainingType.TECHNIQUE -> R.string.type_technique
        TrainingType.CONDITIONING -> R.string.type_conditioning
        TrainingType.COMPETITION -> R.string.type_competition
        TrainingType.PRIVATE_LESSON -> R.string.type_private_lesson
        TrainingType.SEMINAR -> R.string.type_seminar
    },
)

private const val DAYS_IN_WEEK = 7
