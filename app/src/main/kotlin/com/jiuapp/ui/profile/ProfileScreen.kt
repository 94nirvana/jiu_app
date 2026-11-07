package com.jiuapp.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jiuapp.R
import com.jiuapp.ui.common.currentLocale
import com.jiuapp.domain.achievement.AchievementId
import com.jiuapp.domain.belt.Belt
import com.jiuapp.domain.profile.AthleteProfile
import com.jiuapp.ui.common.ChoiceChips
import com.jiuapp.ui.common.DateField
import com.jiuapp.ui.common.KeyValue
import com.jiuapp.ui.common.LoadingBox
import com.jiuapp.ui.common.SectionCard
import com.jiuapp.ui.common.Spacing
import com.jiuapp.ui.common.beltLabel
import com.jiuapp.ui.common.rankLabel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun ProfileScreen(
    onOpenHealth: () -> Unit,
    onOpenCompetitions: () -> Unit,
    onOpenProfessor: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenData: () -> Unit,
    onOpenPrivacy: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var editProfile by remember { mutableStateOf(false) }
    var addGraduation by remember { mutableStateOf(false) }

    val messageText = state.message?.let { stringResource(it) }
    LaunchedEffect(messageText) {
        if (messageText != null) {
            snackbar.showSnackbar(messageText)
            viewModel.clearMessage()
        }
    }

    if (state.loading) {
        LoadingBox()
        return
    }
    val profile = state.profile
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.headlineMedium)

        SectionCard(title = profile?.name ?: stringResource(R.string.profile_no_name)) {
            profile?.academy?.let { KeyValue(stringResource(R.string.field_academy), it) }
            profile?.professor?.let { KeyValue(stringResource(R.string.field_professor), it) }
            profile?.startDate?.let {
                KeyValue(stringResource(R.string.field_start_date), it.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(currentLocale())))
            }
            OutlinedButton(onClick = { editProfile = true }, modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget)) {
                Text(stringResource(R.string.action_edit))
            }
        }

        SectionCard(title = stringResource(R.string.profile_belt_card)) {
            state.currentGraduation?.let { Text(rankLabel(it.rank), style = MaterialTheme.typography.titleLarge) }
            KeyValue(stringResource(R.string.home_sessions_label), stringResource(R.string.profile_since_graduation, state.sinceGraduation.sessions, state.sinceGraduation.hours))
            state.daysSinceGraduation?.let { KeyValue(stringResource(R.string.home_days_label), stringResource(R.string.home_days_since_graduation, it.toInt())) }
            state.estimatedNextGraduation?.let {
                KeyValue(
                    stringResource(R.string.profile_next_estimate),
                    it.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(currentLocale())),
                )
            } ?: Text(stringResource(R.string.profile_next_estimate_none), style = MaterialTheme.typography.bodySmall)
            Button(onClick = { addGraduation = true }, modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget)) {
                Text(stringResource(R.string.profile_add_graduation))
            }
        }

        SectionCard(title = stringResource(R.string.profile_timeline_title)) {
            state.graduations.forEach { entry ->
                val gap = entry.daysSincePrevious?.let { stringResource(R.string.profile_gap_days, it.toInt()) }
                Column {
                    Text(rankLabel(entry.rank), style = MaterialTheme.typography.titleSmall)
                    Text(
                        listOfNotNull(entry.date.toString(), gap).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        SectionCard(title = stringResource(R.string.profile_achievements_title)) {
            AchievementId.entries.forEach { id ->
                val unlocked = state.achievements[id]
                Text(
                    text = if (unlocked != null) {
                        stringResource(R.string.achievement_unlocked, stringResource(achievementLabel(id)), unlocked.toString())
                    } else {
                        stringResource(R.string.achievement_locked, stringResource(achievementLabel(id)))
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        SectionCard(title = stringResource(R.string.profile_more_title)) {
            OutlinedButton(onClick = onOpenHealth, modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget)) { Text(stringResource(R.string.profile_link_health)) }
            OutlinedButton(onClick = onOpenCompetitions, modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget)) { Text(stringResource(R.string.profile_link_competitions)) }
            OutlinedButton(onClick = onOpenProfessor, modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget)) { Text(stringResource(R.string.profile_link_professor)) }
            OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget)) { Text(stringResource(R.string.profile_link_settings)) }
            OutlinedButton(onClick = onOpenData, modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget)) { Text(stringResource(R.string.profile_link_data)) }
            OutlinedButton(onClick = onOpenPrivacy, modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget)) { Text(stringResource(R.string.profile_link_privacy)) }
        }

        SnackbarHost(snackbar)
    }

    if (editProfile) {
        ProfileDialog(
            initial = profile,
            onDismiss = { editProfile = false },
            onSave = { updated ->
                viewModel.saveProfile(updated)
                editProfile = false
            },
        )
    }
    if (addGraduation) {
        GraduationDialog(
            onDismiss = { addGraduation = false },
            onSave = { belt, degree, date, professor, note ->
                viewModel.addGraduation(belt, degree, date, professor, note)
                addGraduation = false
            },
        )
    }
}

@Composable
private fun ProfileDialog(initial: AthleteProfile?, onDismiss: () -> Unit, onSave: (AthleteProfile) -> Unit) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var academy by remember { mutableStateOf(initial?.academy.orEmpty()) }
    var professor by remember { mutableStateOf(initial?.professor.orEmpty()) }
    var startDate by remember { mutableStateOf(initial?.startDate ?: LocalDate.now()) }
    var birthDate by remember { mutableStateOf(initial?.birthDate) }
    var error by remember { mutableStateOf<Int?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.field_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(academy, { academy = it }, label = { Text(stringResource(R.string.field_academy)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(professor, { professor = it }, label = { Text(stringResource(R.string.field_professor)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                DateField(stringResource(R.string.field_start_date), startDate, { it?.let { date -> startDate = date } })
                DateField(stringResource(R.string.field_birth_date), birthDate, { birthDate = it }, allowClear = true)
                error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) {
                    error = R.string.error_name_required
                } else {
                    onSave(
                        AthleteProfile(
                            name = name.trim(),
                            startDate = startDate,
                            birthDate = birthDate,
                            academy = academy.trim().ifEmpty { null },
                            professor = professor.trim().ifEmpty { null },
                            photoUri = initial?.photoUri,
                            id = initial?.id ?: AthleteProfile.SINGLETON_ID,
                            deletedAt = null,
                        ),
                    )
                }
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun GraduationDialog(onDismiss: () -> Unit, onSave: (Belt, Int, LocalDate, String?, String?) -> Unit) {
    var belt by remember { mutableStateOf(Belt.WHITE) }
    var degree by remember { mutableStateOf(0) }
    var date by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var professor by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<Int?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_add_graduation)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                ChoiceChips(Belt.entries, belt, { beltLabel(it) }, { belt = it })
                ChoiceChips((0..4).toList(), degree, { if (it == 0) stringResource(R.string.degree_none) else "$it" }, { degree = it })
                DateField(stringResource(R.string.field_graduation_date), date, { date = it })
                OutlinedTextField(professor, { professor = it }, label = { Text(stringResource(R.string.field_professor)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(note, { note = it }, label = { Text(stringResource(R.string.field_notes)) }, modifier = Modifier.fillMaxWidth())
                error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val chosen = date
                if (chosen == null || chosen.isAfter(LocalDate.now())) {
                    error = R.string.error_date_in_future
                } else {
                    onSave(belt, degree, chosen, professor, note)
                }
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

fun achievementLabel(id: AchievementId): Int = when (id) {
    AchievementId.SESSIONS_1 -> R.string.ach_sessions_1
    AchievementId.SESSIONS_10 -> R.string.ach_sessions_10
    AchievementId.SESSIONS_50 -> R.string.ach_sessions_50
    AchievementId.SESSIONS_100 -> R.string.ach_sessions_100
    AchievementId.SESSIONS_250 -> R.string.ach_sessions_250
    AchievementId.SESSIONS_500 -> R.string.ach_sessions_500
    AchievementId.SESSIONS_1000 -> R.string.ach_sessions_1000
    AchievementId.WEEK_STREAK_4 -> R.string.ach_week_streak_4
    AchievementId.WEEK_STREAK_12 -> R.string.ach_week_streak_12
    AchievementId.WEEK_STREAK_52 -> R.string.ach_week_streak_52
    AchievementId.YEAR_OF_TRAINING -> R.string.ach_year
    AchievementId.FIRST_COMPETITION -> R.string.ach_first_competition
    AchievementId.FIRST_GRADUATION -> R.string.ach_first_graduation
}
