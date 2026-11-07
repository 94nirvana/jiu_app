package com.jiuapp.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiuapp.domain.belt.Belt
import com.jiuapp.domain.repository.SessionRepository
import com.jiuapp.domain.repository.SettingsRepository
import com.jiuapp.domain.stats.Bucket
import com.jiuapp.domain.stats.Comparison
import com.jiuapp.domain.stats.DateRange
import com.jiuapp.domain.stats.Distributions
import com.jiuapp.domain.stats.HeatMetric
import com.jiuapp.domain.stats.Heatmap
import com.jiuapp.domain.stats.Period
import com.jiuapp.domain.stats.Periods
import com.jiuapp.domain.stats.SessionFilter
import com.jiuapp.domain.stats.Share
import com.jiuapp.domain.stats.TagCount
import com.jiuapp.domain.stats.TagRanking
import com.jiuapp.domain.stats.TimeOfDay
import com.jiuapp.domain.stats.Totals
import com.jiuapp.domain.stats.currentStreak
import com.jiuapp.domain.stats.longestStreak
import com.jiuapp.domain.stats.toTotals
import com.jiuapp.domain.stats.weekStart
import com.jiuapp.domain.stats.totalsByBelt
import com.jiuapp.domain.training.TrainingType
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class StatsSummary(val sessions: Int, val minutes: Int, val changeSessionsPercent: Double?, val changeMinutesPercent: Double?) {
    val hours: Double get() = minutes / MINUTES_PER_HOUR

    private companion object {
        const val MINUTES_PER_HOUR = 60.0
    }
}

data class StatsUiState(
    val period: Period = Period.MONTH,
    val range: DateRange = Periods.rangeOf(Period.MONTH, LocalDate.now()),
    val buckets: List<Bucket> = emptyList(),
    val summary: StatsSummary = StatsSummary(0, 0, null, null),
    val heatYear: Int = LocalDate.now().year,
    val heatMetric: HeatMetric = HeatMetric.COUNT,
    val heatLevels: Map<LocalDate, Int> = emptyMap(),
    val typeFilter: Set<TrainingType> = emptySet(),
    val beltFilter: Belt? = null,
    val giShare: Pair<Share, Share>? = null,
    val sparringShare: Pair<Share, Share>? = null,
    val weekdayCounts: Map<DayOfWeek, Int> = emptyMap(),
    val hourCounts: Map<Int, Int> = emptyMap(),
    val beltTotals: Map<Belt, Totals> = emptyMap(),
    val topPlaces: List<TagCount> = emptyList(),
    val topFocus: List<TagCount> = emptyList(),
    val weekSessions: Int = 0,
    val goal: Int = 0,
    val streak: Int = 0,
    val longestStreak: Int = 0,
    val loading: Boolean = true,
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val sessions: SessionRepository,
    settings: SettingsRepository,
    private val clock: Clock,
) : ViewModel() {

    private val period = MutableStateFlow(Period.MONTH)
    private val anchor = MutableStateFlow(LocalDate.now(clock))
    private val heatYear = MutableStateFlow(LocalDate.now(clock).year)
    private val heatMetric = MutableStateFlow(HeatMetric.COUNT)
    private val typeFilter = MutableStateFlow<Set<TrainingType>>(emptySet())
    private val beltFilter = MutableStateFlow<Belt?>(null)

    private val filterState = combine(typeFilter, beltFilter) { types, belt -> types to belt }

    val state: StateFlow<StatsUiState> = combine(
        sessions.observeAll(),
        settings.observeSettings(),
        period,
        anchor,
        filterState,
    ) { all, appSettings, currentPeriod, currentAnchor, (types, belt) ->
        val live = all.filter { it.deletedAt == null }
        val filtered = SessionFilter(types = types, belt = belt).apply(live)
        val range = Periods.rangeOf(currentPeriod, currentAnchor)
        val previous = Periods.shift(currentPeriod, range, -1)
        val current = filtered.filter { range.contains(it.date) }.toTotals()
        val before = filtered.filter { previous.contains(it.date) }.toTotals()
        val today = LocalDate.now(clock)
        StatsUiState(
            period = currentPeriod,
            range = range,
            buckets = Periods.buckets(filtered, range, currentPeriod),
            summary = StatsSummary(
                sessions = current.sessions,
                minutes = current.minutes,
                changeSessionsPercent = Comparison.percentChange(current.sessions.toDouble(), before.sessions.toDouble()),
                changeMinutesPercent = Comparison.percentChange(current.minutes.toDouble(), before.minutes.toDouble()),
            ),
            typeFilter = types,
            beltFilter = belt,
            giShare = Distributions.giVsNoGi(filtered),
            sparringShare = Distributions.sparringVsTechnical(filtered),
            weekdayCounts = TimeOfDay.byWeekday(filtered),
            hourCounts = TimeOfDay.byHour(filtered),
            beltTotals = totalsByBelt(filtered),
            topPlaces = TagRanking.rank(filtered, focus = false).take(TOP_TAGS),
            topFocus = TagRanking.rank(filtered, focus = true).take(TOP_TAGS),
            weekSessions = live.count { it.date.weekStart() == today.weekStart() },
            goal = appSettings.weeklyGoal,
            streak = currentStreak(live, appSettings.weeklyGoal, today),
            longestStreak = longestStreak(live, appSettings.weeklyGoal),
            loading = false,
        )
    }.combine(heatState()) { base, heat ->
        base.copy(heatYear = heat.year, heatMetric = heat.metric, heatLevels = heat.levels)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), StatsUiState())

    private fun heatState() = combine(sessions.observeAll(), heatYear, heatMetric) { all, year, metric ->
        HeatState(year, metric, Heatmap.levels(all.filter { it.deletedAt == null }, year, metric))
    }

    fun setPeriod(value: Period) = period.update { value }

    fun shift(steps: Long) {
        val current = period.value
        anchor.update { day ->
            when (current) {
                Period.WEEK -> day.plusWeeks(steps)
                Period.MONTH -> day.plusMonths(steps)
                Period.YEAR -> day.plusYears(steps)
            }
        }
    }

    fun toggleType(type: TrainingType) = typeFilter.update { if (type in it) it - type else it + type }

    fun setBelt(belt: Belt?) = beltFilter.update { belt }

    fun setHeatMetric(metric: HeatMetric) = heatMetric.update { metric }

    fun shiftHeatYear(delta: Int) = heatYear.update { it + delta }

    private data class HeatState(val year: Int, val metric: HeatMetric, val levels: Map<LocalDate, Int>)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val TOP_TAGS = 12
    }
}
