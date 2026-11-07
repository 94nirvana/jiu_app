package com.jiuapp.ui.competition

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jiuapp.R
import com.jiuapp.ui.common.currentLocale
import com.jiuapp.domain.competition.Competition
import com.jiuapp.domain.competition.Match
import com.jiuapp.domain.competition.MatchResult
import com.jiuapp.domain.competition.VictoryMethod
import com.jiuapp.domain.training.MediaLink
import com.jiuapp.ui.common.ChoiceChips
import com.jiuapp.ui.common.DateField
import com.jiuapp.ui.common.EmptyState
import com.jiuapp.ui.common.KeyValue
import com.jiuapp.ui.common.LoadingBox
import com.jiuapp.ui.common.MedicalDisclaimer
import com.jiuapp.ui.common.SectionCard
import com.jiuapp.ui.common.Spacing
import com.jiuapp.ui.techniques.BackButton
import com.jiuapp.ui.techniques.ConfirmDialog
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.UUID

@Composable
fun CompetitionListScreen(
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
    viewModel: CompetitionListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.competitions_title)) }, navigationIcon = { BackButton(onBack) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.competition_add)) }
        },
    ) { padding ->
        if (state.loading) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item {
                SectionCard(title = stringResource(R.string.competition_campaign)) {
                    Text(
                        stringResource(R.string.competition_campaign_line, state.campaign.wins, state.campaign.losses, state.campaign.draws),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    state.campaign.winRate?.let {
                        Text(stringResource(R.string.competition_win_rate, it * PERCENT), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item { Text(stringResource(R.string.competition_upcoming), style = MaterialTheme.typography.titleMedium) }
            if (state.upcoming.isEmpty()) item { EmptyState(R.string.competition_none_upcoming) }
            items(state.upcoming, key = { it.id }) { CompetitionRow(it) { onOpen(it.id) } }
            item { Text(stringResource(R.string.competition_past), style = MaterialTheme.typography.titleMedium) }
            if (state.past.isEmpty()) item { EmptyState(R.string.competition_none_past) }
            items(state.past, key = { it.id }) { CompetitionRow(it) { onOpen(it.id) } }
        }
    }
}

@Composable
private fun CompetitionRow(competition: Competition, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(competition.name, style = MaterialTheme.typography.titleSmall)
            Text(
                competition.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(currentLocale())),
                style = MaterialTheme.typography.bodySmall,
            )
            val today = java.time.LocalDate.now()
            if (competition.isUpcoming(today)) {
                Text(
                    stringResource(R.string.home_countdown, competition.daysUntil(today).toInt()),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
fun CompetitionDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: CompetitionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var addMatch by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }
    val competition = state.competition

    Scaffold(
        topBar = { TopAppBar(title = { Text(competition?.name ?: stringResource(R.string.competition_title)) }, navigationIcon = { BackButton(onBack) }) },
    ) { padding ->
        if (state.loading) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        if (competition == null) {
            EmptyState(R.string.error_not_found, Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            SectionCard(title = stringResource(R.string.competition_info)) {
                KeyValue(stringResource(R.string.field_date), competition.date.toString())
                competition.location?.let { KeyValue(stringResource(R.string.field_location), it) }
                competition.organizer?.let { KeyValue(stringResource(R.string.field_organizer), it) }
                competition.weightCategory?.let { KeyValue(stringResource(R.string.field_weight_category), it) }
                competition.finalPlacement?.let { KeyValue(stringResource(R.string.field_placement), it.toString()) }
                OutlinedButton(onClick = { onEdit(competition.id) }, modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget)) {
                    Text(stringResource(R.string.action_edit))
                }
            }

            state.weightPlan?.let { plan ->
                SectionCard(title = stringResource(R.string.competition_weight_plan)) {
                    KeyValue(stringResource(R.string.competition_required_loss), stringResource(R.string.kg_format, plan.requiredLossKg))
                    KeyValue(stringResource(R.string.competition_loss_per_week), stringResource(R.string.percent_format, plan.lossPerWeekPercent))
                    if (plan.isRapid) {
                        Text(stringResource(R.string.competition_rapid_warning), color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            SectionCard(title = stringResource(R.string.competition_checklist)) {
                LinearProgressIndicator(progress = { competition.checklistProgress() }, modifier = Modifier.fillMaxWidth())
                competition.checklist.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = item.done, onCheckedChange = { viewModel.toggleChecklist(item) })
                        Text(item.label)
                    }
                }
            }

            SectionCard(title = stringResource(R.string.competition_matches)) {
                Text(
                    stringResource(R.string.competition_campaign_line, state.campaign.wins, state.campaign.losses, state.campaign.draws),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (state.submissionsWon.isNotEmpty()) {
                    KeyValue(stringResource(R.string.competition_submissions_won), state.submissionsWon.entries.joinToString { "${it.key} (${it.value})" })
                }
                if (state.submissionsSuffered.isNotEmpty()) {
                    KeyValue(stringResource(R.string.competition_submissions_suffered), state.submissionsSuffered.entries.joinToString { "${it.key} (${it.value})" })
                }
                state.matches.forEach { match ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.match_line, match.opponentName, resultLabel(match.result), methodLabel(match.method)),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { viewModel.deleteMatch(match.id) }) { Text(stringResource(R.string.action_delete)) }
                    }
                }
                Button(onClick = { addMatch = true }, modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget)) {
                    Text(stringResource(R.string.match_add))
                }
            }

            state.message?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall) }
            OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget)) {
                Text(stringResource(R.string.action_delete))
            }
            MedicalDisclaimer()
        }
    }

    if (addMatch && competition != null) {
        MatchDialog(
            competitionId = competition.id,
            onDismiss = { addMatch = false },
            onSave = { match ->
                viewModel.saveMatch(match)
                addMatch = false
            },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.competition_delete_title),
            onConfirm = {
                confirmDelete = false
                viewModel.deleteCompetition()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
fun CompetitionFormScreen(
    onDone: () -> Unit,
    viewModel: CompetitionFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onDone() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isNew) R.string.competition_add else R.string.competition_edit)) },
                navigationIcon = { BackButton(onDone) },
            )
        },
    ) { padding ->
        if (state.loading) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            OutlinedTextField(state.name, { v -> viewModel.update { it.copy(name = v) } }, label = { Text(stringResource(R.string.field_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            DateField(stringResource(R.string.field_date), state.date, { d -> viewModel.update { it.copy(date = d) } })
            DateField(stringResource(R.string.field_weighin_date), state.weighInDate, { d -> viewModel.update { it.copy(weighInDate = d) } }, allowClear = true)
            OutlinedTextField(state.location, { v -> viewModel.update { it.copy(location = v) } }, label = { Text(stringResource(R.string.field_location)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.organizer, { v -> viewModel.update { it.copy(organizer = v) } }, label = { Text(stringResource(R.string.field_organizer)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.beltCategory, { v -> viewModel.update { it.copy(beltCategory = v) } }, label = { Text(stringResource(R.string.field_belt_category)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.ageCategory, { v -> viewModel.update { it.copy(ageCategory = v) } }, label = { Text(stringResource(R.string.field_age_category)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.weightCategory, { v -> viewModel.update { it.copy(weightCategory = v) } }, label = { Text(stringResource(R.string.field_weight_category)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.targetWeightText, { v -> viewModel.update { it.copy(targetWeightText = v) } }, label = { Text(stringResource(R.string.field_target_weight)) }, singleLine = true, modifier = Modifier.fillMaxWidth())

            SectionCard(title = stringResource(R.string.competition_checklist)) {
                state.checklist.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = item.done, onCheckedChange = { viewModel.toggleChecklistItem(item.id) })
                        Text(item.label, modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.removeChecklistItem(item.id) }) { Text(stringResource(R.string.action_remove)) }
                    }
                }
                OutlinedTextField(state.newItem, { v -> viewModel.update { it.copy(newItem = v) } }, label = { Text(stringResource(R.string.field_checklist_item)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedButton(onClick = viewModel::addChecklistItem, modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget)) {
                    Text(stringResource(R.string.action_add))
                }
            }

            OutlinedTextField(state.notes, { v -> viewModel.update { it.copy(notes = v) } }, label = { Text(stringResource(R.string.field_notes)) }, minLines = 2, modifier = Modifier.fillMaxWidth())
            state.error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget)) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}

@Composable
private fun MatchDialog(competitionId: String, onDismiss: () -> Unit, onSave: (Match) -> Unit) {
    var opponent by remember { mutableStateOf("") }
    var result by remember { mutableStateOf(MatchResult.WIN) }
    var method by remember { mutableStateOf(VictoryMethod.POINTS) }
    var submission by remember { mutableStateOf("") }
    var myPoints by remember { mutableStateOf("0") }
    var theirPoints by remember { mutableStateOf("0") }
    var minutes by remember { mutableStateOf("") }
    var videos by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<Int?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.match_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(opponent, { opponent = it }, label = { Text(stringResource(R.string.field_opponent_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                ChoiceChips(MatchResult.entries, result, { resultLabel(it) }, { result = it })
                ChoiceChips(VictoryMethod.entries, method, { methodLabel(it) }, { method = it })
                if (method == VictoryMethod.SUBMISSION) {
                    OutlinedTextField(submission, { submission = it }, label = { Text(stringResource(R.string.field_submission_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    OutlinedTextField(myPoints, { myPoints = it.filter(Char::isDigit) }, label = { Text(stringResource(R.string.field_my_points)) }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(theirPoints, { theirPoints = it.filter(Char::isDigit) }, label = { Text(stringResource(R.string.field_their_points)) }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit) }, label = { Text(stringResource(R.string.field_match_minutes)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(videos, { videos = it }, label = { Text(stringResource(R.string.field_video_links)) }, minLines = 2, modifier = Modifier.fillMaxWidth())
                error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val links = videos.lines().map { it.trim() }.filter { it.isNotEmpty() }
                when {
                    opponent.isBlank() -> error = R.string.error_name_required
                    !links.all(MediaLink::isAllowed) -> error = R.string.error_video_link
                    method == VictoryMethod.SUBMISSION && submission.isBlank() -> error = R.string.error_submission_name
                    else -> runCatching {
                        Match(
                            competitionId = competitionId,
                            opponentName = opponent.trim(),
                            result = result,
                            method = method,
                            submissionName = submission.trim().ifEmpty { null }.takeIf { method == VictoryMethod.SUBMISSION },
                            myPoints = myPoints.toIntOrNull() ?: 0,
                            opponentPoints = theirPoints.toIntOrNull() ?: 0,
                            durationSeconds = minutes.toIntOrNull()?.times(SECONDS_PER_MINUTE),
                            videoLinks = links,
                            id = UUID.randomUUID().toString(),
                        )
                    }.onSuccess(onSave).onFailure { error = R.string.error_invalid_data }
                }
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
fun resultLabel(result: MatchResult): String = stringResource(
    when (result) {
        MatchResult.WIN -> R.string.result_win
        MatchResult.LOSS -> R.string.result_loss
        MatchResult.DRAW -> R.string.result_draw
    },
)

@Composable
fun methodLabel(method: VictoryMethod): String = stringResource(
    when (method) {
        VictoryMethod.SUBMISSION -> R.string.method_submission
        VictoryMethod.POINTS -> R.string.method_points
        VictoryMethod.ADVANTAGES -> R.string.method_advantages
        VictoryMethod.REFEREE_DECISION -> R.string.method_referee
        VictoryMethod.DISQUALIFICATION -> R.string.method_disqualification
        VictoryMethod.WALKOVER -> R.string.method_walkover
        VictoryMethod.OTHER -> R.string.method_other
    },
)

private const val PERCENT = 100.0
private const val SECONDS_PER_MINUTE = 60
