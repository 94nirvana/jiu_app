package com.jiuapp.ui.sessions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.jiuapp.R
import com.jiuapp.domain.belt.Belt
import com.jiuapp.domain.training.TrainingType
import com.jiuapp.ui.common.ChoiceChips
import com.jiuapp.ui.common.DateField
import com.jiuapp.ui.common.LoadingBox
import com.jiuapp.ui.common.ScaleChoice
import com.jiuapp.ui.common.Spacing
import com.jiuapp.ui.common.beltLabel
import com.jiuapp.ui.common.SectionCard
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun SessionFormScreen(
    onDone: () -> Unit,
    viewModel: SessionFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isNew) R.string.session_form_new else R.string.session_form_edit)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        if (state.loading) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            SectionCard(title = stringResource(R.string.session_section_when)) {
                DateField(
                    label = stringResource(R.string.field_date),
                    value = state.date,
                    onValueChange = { date -> viewModel.update { it.copy(date = date) } },
                )
                OutlinedTextField(
                    value = state.durationText,
                    onValueChange = { text -> viewModel.update { it.copy(durationText = text.filter(Char::isDigit).take(MAX_DIGITS)) } },
                    label = { Text(stringResource(R.string.field_duration)) },
                    supportingText = { Text(stringResource(R.string.field_duration_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TimeTextField(state.startTime) { time -> viewModel.update { it.copy(startTime = time) } }
            }

            SectionCard(title = stringResource(R.string.session_section_what)) {
                Text(stringResource(R.string.field_type), style = MaterialTheme.typography.labelLarge)
                ChoiceChips(
                    options = TrainingType.entries,
                    selected = state.type,
                    label = { typeLabel(it) },
                    onSelect = { type -> viewModel.update { it.copy(type = type) } },
                )
                Text(stringResource(R.string.field_intensity), style = MaterialTheme.typography.labelLarge)
                ChoiceChips(
                    options = (1..5).toList(),
                    selected = state.intensity,
                    label = { it.toString() },
                    onSelect = { value -> viewModel.update { it.copy(intensity = value) } },
                )
                Text(stringResource(R.string.field_rank_on_day), style = MaterialTheme.typography.labelLarge)
                ChoiceChips(
                    options = Belt.entries,
                    selected = state.belt,
                    label = { beltLabel(it) },
                    onSelect = { belt -> viewModel.update { it.copy(belt = belt) } },
                )
                ChoiceChips(
                    options = (0..4).toList(),
                    selected = state.degree,
                    label = { if (it == 0) stringResource(R.string.degree_none) else "$it" },
                    onSelect = { degree -> viewModel.update { it.copy(degree = degree) } },
                )
            }

            SectionCard(title = stringResource(R.string.session_section_place)) {
                OutlinedTextField(
                    value = state.academy,
                    onValueChange = { v -> viewModel.update { it.copy(academy = v) } },
                    label = { Text(stringResource(R.string.field_academy)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.professor,
                    onValueChange = { v -> viewModel.update { it.copy(professor = v) } },
                    label = { Text(stringResource(R.string.field_professor)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SectionCard(title = stringResource(R.string.session_section_body)) {
                ScaleChoice(stringResource(R.string.field_fatigue), state.fatigue) { v -> viewModel.update { it.copy(fatigue = v) } }
                ScaleChoice(stringResource(R.string.field_pain), state.pain) { v -> viewModel.update { it.copy(pain = v) } }
                ScaleChoice(stringResource(R.string.field_mood), state.mood) { v -> viewModel.update { it.copy(mood = v) } }
                ScaleChoice(stringResource(R.string.field_energy), state.energy) { v -> viewModel.update { it.copy(energy = v) } }
            }

            SectionCard(title = stringResource(R.string.session_section_notes)) {
                OutlinedTextField(
                    value = state.learned,
                    onValueChange = { v -> viewModel.update { it.copy(learned = v) } },
                    label = { Text(stringResource(R.string.field_learned)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                OutlinedTextField(
                    value = state.toFix,
                    onValueChange = { v -> viewModel.update { it.copy(toFix = v) } },
                    label = { Text(stringResource(R.string.field_to_fix)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = { v -> viewModel.update { it.copy(notes = v) } },
                    label = { Text(stringResource(R.string.field_notes)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                OutlinedTextField(
                    value = state.tagsText,
                    onValueChange = { v -> viewModel.update { it.copy(tagsText = v) } },
                    label = { Text(stringResource(R.string.field_tags)) },
                    supportingText = { Text(stringResource(R.string.field_tags_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.focusTagsText,
                    onValueChange = { v -> viewModel.update { it.copy(focusTagsText = v) } },
                    label = { Text(stringResource(R.string.field_focus_tags)) },
                    supportingText = { Text(stringResource(R.string.field_tags_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (state.techniques.isNotEmpty()) {
                SectionCard(title = stringResource(R.string.session_section_techniques)) {
                    ChoiceChipsMulti(
                        options = state.techniques,
                        selected = state.techniqueIds,
                        label = { it.name },
                        onToggle = { id -> viewModel.update { s -> s.copy(techniqueIds = if (id in s.techniqueIds) s.techniqueIds - id else s.techniqueIds + id) } },
                        idOf = { it.id },
                    )
                }
            }

            SectionCard(title = stringResource(R.string.session_section_attachments)) {
                PhotoAttachments(state.photoUris, viewModel::addPhoto, viewModel::removePhoto)
                OutlinedTextField(
                    value = state.videoLinksText,
                    onValueChange = { v -> viewModel.update { it.copy(videoLinksText = v) } },
                    label = { Text(stringResource(R.string.field_video_links)) },
                    supportingText = { Text(stringResource(R.string.field_video_links_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
            }

            state.error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }

            Button(
                onClick = viewModel::save,
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget),
            ) { Text(stringResource(R.string.action_save)) }
        }
    }
}

@Composable
private fun TimeTextField(value: LocalTime?, onChange: (LocalTime?) -> Unit) {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    var text by remember(value) { mutableStateOf(value?.format(formatter).orEmpty()) }
    var invalid by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            text = raw
            if (raw.isBlank()) {
                invalid = false
                onChange(null)
            } else {
                val parsed = runCatching { LocalTime.parse(raw, formatter) }.getOrNull()
                invalid = parsed == null
                parsed?.let { onChange(it) }
            }
        },
        label = { Text(stringResource(R.string.field_start_time)) },
        supportingText = { Text(stringResource(if (invalid) R.string.error_time_format else R.string.field_time_hint)) },
        isError = invalid,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PhotoAttachments(uris: List<String>, onAdd: (String) -> Unit, onRemove: (String) -> Unit) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            // Permissão de leitura persistente: sem ela a foto some depois de reiniciar o aparelho.
            runCatching {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            onAdd(it.toString())
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        OutlinedButton(
            onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget),
        ) { Text(stringResource(R.string.action_add_photo, uris.size)) }
        uris.forEachIndexed { index, uri ->
            Text(stringResource(R.string.attachment_photo_item, index + 1))
            OutlinedButton(onClick = { onRemove(uri) }, modifier = Modifier.height(Spacing.touchTarget)) {
                Text(stringResource(R.string.action_remove))
            }
        }
    }
}

@Composable
private fun <T> ChoiceChipsMulti(
    options: List<T>,
    selected: Set<String>,
    label: (T) -> String,
    onToggle: (String) -> Unit,
    idOf: (T) -> String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        options.chunked(3).forEach { row ->
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                row.forEach { option ->
                    FilterChip(
                        selected = idOf(option) in selected,
                        onClick = { onToggle(idOf(option)) },
                        label = { Text(label(option)) },
                        modifier = Modifier.height(Spacing.touchTarget),
                    )
                }
            }
        }
    }
}

private const val MAX_DIGITS = 3
