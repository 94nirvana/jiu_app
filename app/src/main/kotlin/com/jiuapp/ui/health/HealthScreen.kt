package com.jiuapp.ui.health

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jiuapp.R
import com.jiuapp.ui.common.currentLocale
import com.jiuapp.domain.health.BodyRegion
import com.jiuapp.domain.health.Injury
import com.jiuapp.domain.health.WeightEntry
import com.jiuapp.ui.common.ChoiceChips
import com.jiuapp.ui.common.DateField
import com.jiuapp.ui.common.EmptyState
import com.jiuapp.ui.common.LoadingBox
import com.jiuapp.ui.common.MedicalDisclaimer
import com.jiuapp.ui.common.SectionCard
import com.jiuapp.ui.common.Spacing
import com.jiuapp.ui.common.ScaleChoice
import com.jiuapp.ui.common.KeyValue
import java.time.LocalDate

@Composable
fun HealthScreen(onBack: () -> Unit, viewModel: HealthViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var addInjury by remember { mutableStateOf(false) }
    var addWeight by remember { mutableStateOf(false) }
    val messageText = state.message?.let { stringResource(it) }
    LaunchedEffect(messageText) {
        if (messageText != null) {
            snackbar.showSnackbar(messageText)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.health_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (state.loading) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            SectionCard(title = stringResource(R.string.health_injuries_title)) {
                if (state.active.isEmpty()) {
                    Text(stringResource(R.string.health_no_active), style = MaterialTheme.typography.bodyMedium)
                } else {
                    state.active.forEach { injury ->
                        Text(stringResource(R.string.home_injury_line, injury.region.label, injury.type), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (state.recurring.isNotEmpty()) {
                    Text(
                        stringResource(R.string.health_recurring, state.recurring.joinToString { it.label }),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Button(onClick = { addInjury = true }, modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget)) {
                    Text(stringResource(R.string.health_add_injury))
                }
                state.injuries.filter { !it.isActive() }.take(MAX_HISTORY).forEach { injury ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.health_history_line, injury.region.label, injury.startDate.toString()),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { viewModel.deleteInjury(injury.id) }) { Text(stringResource(R.string.action_delete)) }
                    }
                }
            }

            SectionCard(title = stringResource(R.string.health_weight_title)) {
                if (state.weights.isEmpty()) {
                    EmptyState(R.string.health_weight_empty)
                } else {
                    WeightChart(state.weights)
                    state.weeklyChangePercent?.let {
                        KeyValue(stringResource(R.string.health_weekly_change), String.format(currentLocale(), "%+.1f%%", it))
                    }
                    state.rapidLoss?.let {
                        Text(stringResource(R.string.health_rapid_loss), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                    state.targetWeightKg?.let { target ->
                        KeyValue(stringResource(R.string.health_target), String.format(currentLocale(), "%.1f kg", target))
                        state.remainingKg?.let {
                            KeyValue(stringResource(R.string.health_remaining), String.format(currentLocale(), "%.1f kg", it))
                        }
                    }
                }
                Button(onClick = { addWeight = true }, modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget)) {
                    Text(stringResource(R.string.health_add_weight))
                }
                state.weights.takeLast(MAX_HISTORY).reversed().forEach { entry ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.health_weight_line, entry.date.toString(), entry.kg),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { viewModel.deleteWeight(entry.id) }) { Text(stringResource(R.string.action_delete)) }
                    }
                }
                TargetWeightField(state.targetWeightKg) { viewModel.saveTargetWeight(it) }
            }

            MedicalDisclaimer()
        }
    }

    if (addInjury) {
        InjuryDialog(
            onDismiss = { addInjury = false },
            onSave = { injury ->
                viewModel.saveInjury(injury)
                addInjury = false
            },
        )
    }
    if (addWeight) {
        WeightDialog(
            onDismiss = { addWeight = false },
            onSave = { date, kg ->
                viewModel.saveWeight(date, kg)
                addWeight = false
            },
        )
    }
}

@Composable
private fun InjuryDialog(onDismiss: () -> Unit, onSave: (Injury) -> Unit) {
    var region by remember { mutableStateOf(BodyRegion.OTHER) }
    var type by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf(DEFAULT_SEVERITY) }
    var start by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var expected by remember { mutableStateOf<LocalDate?>(null) }
    var recovered by remember { mutableStateOf<LocalDate?>(null) }
    var treatment by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<Int?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.health_add_injury)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(stringResource(R.string.field_body_region), style = MaterialTheme.typography.labelLarge)
                ChoiceChips(BodyRegion.entries, region, { it.label }, { region = it })
                OutlinedTextField(type, { type = it }, label = { Text(stringResource(R.string.field_injury_type)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                ScaleChoice(stringResource(R.string.field_severity), severity) { severity = it ?: DEFAULT_SEVERITY }
                DateField(stringResource(R.string.field_start_date), start, { start = it })
                DateField(stringResource(R.string.field_expected_recovery), expected, { expected = it }, allowClear = true)
                DateField(stringResource(R.string.field_actual_recovery), recovered, { recovered = it }, allowClear = true)
                OutlinedTextField(treatment, { treatment = it }, label = { Text(stringResource(R.string.field_treatment)) }, modifier = Modifier.fillMaxWidth())
                error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val startDate = start
                when {
                    type.isBlank() -> error = R.string.error_injury_type
                    startDate == null -> error = R.string.error_start_date_required
                    recovered?.isBefore(startDate) == true -> error = R.string.error_recovery_before_start
                    else -> onSave(
                        Injury(
                            region = region,
                            type = type.trim(),
                            severity = severity,
                            startDate = startDate,
                            treatment = treatment.trim().ifEmpty { null },
                            expectedRecoveryDate = expected,
                            actualRecoveryDate = recovered,
                            id = java.util.UUID.randomUUID().toString(),
                        ),
                    )
                }
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun WeightDialog(onDismiss: () -> Unit, onSave: (LocalDate, Double) -> Unit) {
    var date by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<Int?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.health_add_weight)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                DateField(stringResource(R.string.field_date), date, { date = it })
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.field_weight_kg)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val kg = text.replace(',', '.').toDoubleOrNull()
                val day = date
                when {
                    kg == null -> error = R.string.error_number_format
                    day == null || day.isAfter(LocalDate.now()) -> error = R.string.error_date_in_future
                    else -> onSave(day, kg)
                }
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun TargetWeightField(current: Double?, onSave: (Double?) -> Unit) {
    var text by remember(current) { mutableStateOf(current?.toString().orEmpty()) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text(stringResource(R.string.field_target_weight)) },
        supportingText = { Text(stringResource(R.string.field_target_weight_hint)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedButton(
        onClick = { onSave(text.replace(',', '.').toDoubleOrNull()) },
        modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget),
    ) { Text(stringResource(R.string.action_save)) }
}

@Composable
private fun WeightChart(entries: List<WeightEntry>) {
    val line = MaterialTheme.colorScheme.primary
    val description = stringResource(R.string.health_chart_description, entries.size)
    val min = entries.minOf { it.kg }
    val max = entries.maxOf { it.kg }
    val range = (max - min).coerceAtLeast(MIN_RANGE_KG)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(CHART_HEIGHT)
            .semantics { contentDescription = description },
    ) {
        if (entries.size < 2) return@Canvas
        val stepX = size.width / (entries.size - 1)
        val points = entries.mapIndexed { index, entry ->
            Offset(index * stepX, size.height - (size.height * ((entry.kg - min) / range)).toFloat())
        }
        points.zipWithNext().forEach { (a, b) -> drawLine(line, a, b, strokeWidth = STROKE.toPx()) }
        val dot = DOT.toPx()
        points.forEach { drawRect(line, Offset(it.x - dot / 2, it.y - dot / 2), Size(dot, dot)) }
    }
}

private const val DEFAULT_SEVERITY = 3
private const val MAX_HISTORY = 5
private const val MIN_RANGE_KG = 1.0
private val CHART_HEIGHT = 120.dp
private val STROKE = 2.dp
private val DOT = 6.dp
