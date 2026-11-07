package com.jiuapp.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jiuapp.R
import com.jiuapp.ui.common.currentLocale
import com.jiuapp.domain.stats.GoalStatus
import com.jiuapp.ui.common.KeyValue
import com.jiuapp.ui.common.LoadingBox
import com.jiuapp.ui.common.MedicalDisclaimer
import com.jiuapp.ui.common.SectionCard
import com.jiuapp.ui.common.Spacing
import com.jiuapp.ui.common.rankLabel
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun HomeScreen(
    onOpenCompetitions: () -> Unit,
    onOpenHealth: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val summary = state.summary
    if (state.loading || summary == null) {
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
        Text(
            text = state.athleteName?.let { stringResource(R.string.home_greeting, it) } ?: stringResource(R.string.home_title),
            style = MaterialTheme.typography.headlineMedium,
        )

        SectionCard(title = stringResource(R.string.home_week_card)) {
            Text(
                text = stringResource(R.string.home_week_progress, summary.weekSessions, summary.weeklyGoal),
                style = MaterialTheme.typography.bodyLarge,
            )
            LinearProgressIndicator(
                progress = { summary.goalProgress },
                modifier = Modifier.fillMaxWidth().semantics {
                    contentDescription = "${summary.weekSessions}/${summary.weeklyGoal}"
                },
            )
            Text(text = stringResource(goalStatusText(summary.goalStatus)), style = MaterialTheme.typography.bodyMedium)
            KeyValue(stringResource(R.string.home_streak_label), stringResource(R.string.home_streak, summary.streakWeeks))
        }

        SectionCard(title = stringResource(R.string.home_graduation_card)) {
            state.currentGraduation?.let { KeyValue(stringResource(R.string.home_current_rank), rankLabel(it.rank)) }
            summary.daysSinceGraduation?.let {
                KeyValue(stringResource(R.string.home_days_label), stringResource(R.string.home_days_since_graduation, it.toInt()))
            }
            KeyValue(
                stringResource(R.string.home_sessions_label),
                stringResource(R.string.home_since_graduation, summary.sessionsSinceGraduation),
            )
        }

        summary.nextCompetition?.let { competition ->
            SectionCard(title = stringResource(R.string.home_next_competition)) {
                Text(text = competition.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = stringResource(R.string.home_countdown, competition.daysUntil(java.time.LocalDate.now()).toInt()),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = competition.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(currentLocale())),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        if (summary.activeInjuries.isNotEmpty()) {
            SectionCard(title = stringResource(R.string.home_active_injuries)) {
                summary.activeInjuries.take(MAX_LISTED_INJURIES).forEach { injury ->
                    Text(
                        text = stringResource(R.string.home_injury_line, injury.region.label, injury.type),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        summary.overload?.let {
            SectionCard(title = stringResource(R.string.home_overload_title)) {
                Text(
                    text = stringResource(R.string.home_overload, (it.ratio - 1) * PERCENT),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (summary.strainAlert) {
            SectionCard(title = stringResource(R.string.home_strain_title)) {
                Text(text = stringResource(R.string.home_strain), style = MaterialTheme.typography.bodyMedium)
            }
        }

        summary.lastSessionDate?.let {
            KeyValue(
                stringResource(R.string.home_last_session),
                it.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(currentLocale())),
            )
        }

        MedicalDisclaimer()
    }
}

private fun goalStatusText(status: GoalStatus): Int = when (status) {
    GoalStatus.ACHIEVED -> R.string.goal_achieved
    GoalStatus.ON_TRACK -> R.string.goal_on_track
    GoalStatus.BEHIND -> R.string.goal_behind
}

private const val MAX_LISTED_INJURIES = 3
private const val PERCENT = 100.0
