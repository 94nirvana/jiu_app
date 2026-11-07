package com.jiuapp.ui.sessions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jiuapp.R
import com.jiuapp.ui.common.currentLocale
import com.jiuapp.domain.training.Attachment
import com.jiuapp.domain.training.AttachmentKind
import com.jiuapp.domain.training.TrainingSession
import com.jiuapp.ui.common.EmptyState
import com.jiuapp.ui.common.KeyValue
import com.jiuapp.ui.common.LoadingBox
import com.jiuapp.ui.common.SectionCard
import com.jiuapp.ui.common.Spacing
import com.jiuapp.ui.common.rankLabel
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.compose.ui.platform.LocalUriHandler

@Composable
fun SessionDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: SessionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    val session = state.session

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.session_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading -> LoadingBox()
            session == null -> EmptyState(R.string.session_not_found, Modifier.padding(padding))
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                SessionSummaryCard(session)
                SessionNotesCard(session, state.techniqueNames)
                SessionAttachmentsCard(session.attachments)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.weight(1f).height(Spacing.touchTarget)) {
                        Text(stringResource(R.string.action_delete))
                    }
                    Button(onClick = { onEdit(session.id) }, modifier = Modifier.weight(1f).height(Spacing.touchTarget)) {
                        Text(stringResource(R.string.action_edit))
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.session_delete_title)) },
            text = { Text(stringResource(R.string.session_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete()
                    onBack()
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun SessionSummaryCard(session: TrainingSession) {
    SectionCard(title = session.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(currentLocale()))) {
        KeyValue(stringResource(R.string.field_type), typeLabel(session.type))
        KeyValue(stringResource(R.string.field_duration), stringResource(R.string.minutes_format, session.durationMinutes))
        KeyValue(stringResource(R.string.field_intensity), session.intensity.toString())
        KeyValue(stringResource(R.string.field_rank_on_day), rankLabel(session.rankOnDay))
        session.academy?.let { KeyValue(stringResource(R.string.field_academy), it) }
        session.professor?.let { KeyValue(stringResource(R.string.field_professor), it) }
        session.startTime?.let { KeyValue(stringResource(R.string.field_start_time), it.toString()) }
        session.fatigue?.let { KeyValue(stringResource(R.string.field_fatigue), it.toString()) }
        session.pain?.let { KeyValue(stringResource(R.string.field_pain), it.toString()) }
        session.mood?.let { KeyValue(stringResource(R.string.field_mood), it.toString()) }
        session.energy?.let { KeyValue(stringResource(R.string.field_energy), it.toString()) }
    }
}

@Composable
private fun SessionNotesCard(session: TrainingSession, techniqueNames: List<String>) {
    SectionCard(title = stringResource(R.string.session_notes_title)) {
        session.learned?.let { KeyValue(stringResource(R.string.field_learned), it) }
        session.toFix?.let { KeyValue(stringResource(R.string.field_to_fix), it) }
        session.notes?.let { KeyValue(stringResource(R.string.field_notes), it) }
        if (session.tags.isNotEmpty()) KeyValue(stringResource(R.string.field_tags), session.tags.sorted().joinToString(", "))
        if (session.focusTags.isNotEmpty()) KeyValue(stringResource(R.string.field_focus_tags), session.focusTags.sorted().joinToString(", "))
        if (techniqueNames.isNotEmpty()) KeyValue(stringResource(R.string.field_techniques), techniqueNames.sorted().joinToString(", "))
    }
}

@Composable
private fun SessionAttachmentsCard(attachments: List<Attachment>) {
    if (attachments.isEmpty()) return
    val uriHandler = LocalUriHandler.current
    SectionCard(title = stringResource(R.string.session_attachments_title)) {
        attachments.forEach { attachment ->
            when (attachment.kind) {
                AttachmentKind.VIDEO_LINK -> TextButton(onClick = { uriHandler.openUri(attachment.uri) }) {
                    Text(stringResource(R.string.action_open_video))
                }
                AttachmentKind.PHOTO -> Text(stringResource(R.string.attachment_photo), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
