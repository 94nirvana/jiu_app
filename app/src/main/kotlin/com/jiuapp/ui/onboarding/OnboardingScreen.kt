package com.jiuapp.ui.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import com.jiuapp.R
import com.jiuapp.ui.common.currentLocale
import com.jiuapp.domain.belt.Belt
import com.jiuapp.ui.common.ChoiceChips
import com.jiuapp.ui.common.DateField
import com.jiuapp.ui.common.MedicalDisclaimer
import com.jiuapp.ui.common.Spacing
import com.jiuapp.ui.common.beltLabel
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .safeDrawingPadding()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            LinearProgressIndicator(
                progress = { (state.step + 1).toFloat() / (state.lastStep + 1) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.onboarding_step_count, state.step + 1, state.lastStep + 1),
                style = MaterialTheme.typography.labelLarge,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                when (state.step) {
                    0 -> WelcomeStep(state.consent) { value -> viewModel.update { it.copy(consent = value) } }
                    1 -> ProfileStep(state, viewModel)
                    2 -> BeltStep(state, viewModel)
                    else -> GoalStep(state, viewModel)
                }
            }
            state.error?.let { error ->
                Text(
                    text = stringResource(error),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                if (state.step > 0) {
                    OutlinedButton(
                        onClick = viewModel::back,
                        modifier = Modifier.weight(1f).height(Spacing.touchTarget),
                        enabled = !state.saving,
                    ) { Text(stringResource(R.string.action_back)) }
                }
                Button(
                    onClick = viewModel::next,
                    modifier = Modifier.weight(1f).height(Spacing.touchTarget),
                    enabled = !state.saving,
                ) {
                    Text(
                        stringResource(
                            if (state.step == state.lastStep) R.string.onboarding_finish else R.string.action_next,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep(consent: Boolean, onConsent: (Boolean) -> Unit) {
    Text(
        text = stringResource(R.string.onboarding_welcome_title),
        style = MaterialTheme.typography.headlineMedium,
    )
    Text(text = stringResource(R.string.onboarding_welcome_body), style = MaterialTheme.typography.bodyLarge)
    HorizontalDivider()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = consent, onCheckedChange = onConsent)
        Text(text = stringResource(R.string.onboarding_consent), modifier = Modifier.padding(start = Spacing.sm))
    }
    MedicalDisclaimer()
}

@Composable
private fun ProfileStep(state: OnboardingState, viewModel: OnboardingViewModel) {
    Text(text = stringResource(R.string.onboarding_profile_title), style = MaterialTheme.typography.headlineSmall)
    OutlinedTextField(
        value = state.name,
        onValueChange = { value -> viewModel.update { it.copy(name = value) } },
        label = { Text(stringResource(R.string.field_name)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = state.academy,
        onValueChange = { value -> viewModel.update { it.copy(academy = value) } },
        label = { Text(stringResource(R.string.field_academy)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = state.professor,
        onValueChange = { value -> viewModel.update { it.copy(professor = value) } },
        label = { Text(stringResource(R.string.field_professor)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    DateField(
        label = stringResource(R.string.field_start_date),
        value = state.startDate,
        onValueChange = { date -> viewModel.update { it.copy(startDate = date) } },
    )
}

@Composable
private fun BeltStep(state: OnboardingState, viewModel: OnboardingViewModel) {
    Text(text = stringResource(R.string.onboarding_belt_title), style = MaterialTheme.typography.headlineSmall)
    ChoiceChips(
        options = Belt.entries,
        selected = state.belt,
        label = { beltLabel(it) },
        onSelect = { belt -> viewModel.update { it.copy(belt = belt) } },
    )
    Text(text = stringResource(R.string.field_degree), style = MaterialTheme.typography.labelLarge)
    ChoiceChips(
        options = (0..4).toList(),
        selected = state.degree,
        label = { if (it == 0) stringResource(R.string.degree_none) else "$it" },
        onSelect = { degree -> viewModel.update { it.copy(degree = degree) } },
    )
    DateField(
        label = stringResource(R.string.field_graduation_date),
        value = state.graduationDate,
        onValueChange = { date -> viewModel.update { it.copy(graduationDate = date) } },
    )
}

@Composable
private fun GoalStep(state: OnboardingState, viewModel: OnboardingViewModel) {
    Text(text = stringResource(R.string.onboarding_goal_title), style = MaterialTheme.typography.headlineSmall)
    Text(text = stringResource(R.string.field_weekly_goal), style = MaterialTheme.typography.labelLarge)
    ChoiceChips(
        options = (1..7).toList(),
        selected = state.weeklyGoal,
        label = { "$it" },
        onSelect = { goal -> viewModel.update { it.copy(weeklyGoal = goal) } },
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = stringResource(R.string.field_reminders), style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = state.reminderEnabled,
            onCheckedChange = { value -> viewModel.update { it.copy(reminderEnabled = value) } },
        )
    }
    if (state.reminderEnabled) {
        ReminderTimeField(state.reminderTime) { time -> viewModel.update { it.copy(reminderTime = time) } }
        ReminderDays(state.reminderDays) { day ->
            viewModel.update {
                it.copy(reminderDays = if (day in it.reminderDays) it.reminderDays - day else it.reminderDays + day)
            }
        }
    }
    MedicalDisclaimer()
}

@Composable
fun ReminderTimeField(time: LocalTime, onChange: (LocalTime) -> Unit) {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    var text by remember(time) { mutableStateOf(time.format(formatter)) }
    var invalid by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            text = raw
            val parsed = runCatching { LocalTime.parse(raw, formatter) }.getOrNull()
            invalid = parsed == null
            parsed?.let(onChange)
        },
        label = { Text(stringResource(R.string.field_reminder_time)) },
        supportingText = { if (invalid) Text(stringResource(R.string.error_time_format)) },
        isError = invalid,
        singleLine = true,
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "HH:mm" },
    )
}

@Composable
private fun ReminderDays(selected: Set<DayOfWeek>, onToggle: (DayOfWeek) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        DayOfWeek.entries.forEach { day ->
            FilterChip(
                selected = day in selected,
                onClick = { onToggle(day) },
                label = { Text(day.getDisplayName(java.time.format.TextStyle.SHORT, currentLocale())) },
                modifier = Modifier.height(Spacing.touchTarget),
            )
        }
    }
}
