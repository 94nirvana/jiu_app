package com.jiuapp.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.jiuapp.R
import com.jiuapp.ui.common.currentLocale
import com.jiuapp.domain.settings.AppSettings
import com.jiuapp.ui.common.ChoiceChips
import com.jiuapp.ui.common.MultiChoiceChips
import com.jiuapp.ui.common.SectionCard
import com.jiuapp.ui.common.Spacing
import com.jiuapp.ui.onboarding.ReminderTimeField
import java.time.DayOfWeek
import java.time.format.TextStyle

@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.state.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val messageText = message?.let { stringResource(it) }
    LaunchedEffect(messageText) {
        if (messageText != null) {
            snackbar.showSnackbar(messageText)
            viewModel.clearMessage()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            SectionCard(title = stringResource(R.string.settings_goal)) {
                ChoiceChips(
                    options = (1..WEEKLY_GOAL_CHOICES).toList(),
                    selected = settings.weeklyGoal,
                    label = { "$it" },
                    onSelect = { goal -> viewModel.update { it.copy(weeklyGoal = goal) } },
                )
                OutlinedTextField(
                    value = settings.defaultSessionMinutes.toString(),
                    onValueChange = { raw ->
                        raw.toIntOrNull()?.let { minutes -> viewModel.update { it.copy(defaultSessionMinutes = minutes) } }
                    },
                    label = { Text(stringResource(R.string.settings_default_duration)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SectionCard(title = stringResource(R.string.settings_reminders)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.settings_reminders_enabled), style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = settings.reminderEnabled,
                        onCheckedChange = { value -> viewModel.update { it.copy(reminderEnabled = value) } },
                    )
                }
                if (settings.reminderEnabled) {
                    ReminderTimeField(settings.reminderTime) { time -> viewModel.update { it.copy(reminderTime = time) } }
                    MultiChoiceChips(
                        options = DayOfWeek.entries,
                        selected = settings.reminderDays,
                        label = { it.getDisplayName(TextStyle.SHORT, currentLocale()) },
                        onToggle = { day ->
                            viewModel.update {
                                it.copy(reminderDays = if (day in it.reminderDays) it.reminderDays - day else it.reminderDays + day)
                            }
                        },
                    )
                }
            }

            SectionCard(title = stringResource(R.string.settings_load)) {
                Text(stringResource(R.string.settings_load_hint), style = MaterialTheme.typography.bodySmall)
                ChoiceChips(
                    options = LOAD_RATIOS,
                    selected = LOAD_RATIOS.firstOrNull { kotlin.math.abs(it - settings.loadAlertRatio) < RATIO_TOLERANCE },
                    label = { stringResource(R.string.settings_load_option, (it - 1) * PERCENT) },
                    onSelect = { ratio -> viewModel.update { it.copy(loadAlertRatio = ratio) } },
                )
            }

            SectionCard(title = stringResource(R.string.settings_professor)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.settings_professor_mode), style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = settings.professorModeEnabled,
                        onCheckedChange = { value -> viewModel.update { it.copy(professorModeEnabled = value) } },
                    )
                }
                Text(stringResource(R.string.settings_professor_hint), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private val LOAD_RATIOS = listOf(1.3, AppSettings.DEFAULT_LOAD_ALERT_RATIO, 2.0)
private const val WEEKLY_GOAL_CHOICES = 7
private const val PERCENT = 100.0
private const val RATIO_TOLERANCE = 0.01
