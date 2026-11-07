package com.jiuapp.ui.techniques

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jiuapp.R
import com.jiuapp.domain.technique.PlanNodeKind
import com.jiuapp.domain.technique.TechniqueCategory
import com.jiuapp.domain.technique.TechniqueStatus
import com.jiuapp.ui.common.ChoiceChips
import com.jiuapp.ui.common.LoadingBox
import com.jiuapp.ui.common.SectionCard
import com.jiuapp.ui.common.Spacing

@Composable
fun TechniqueFormScreen(
    onDone: () -> Unit,
    viewModel: TechniqueFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(state.saved) { if (state.saved) onDone() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isNew) R.string.technique_new else R.string.technique_edit)) },
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
            OutlinedTextField(
                value = state.name,
                onValueChange = { v -> viewModel.update { it.copy(name = v) } },
                label = { Text(stringResource(R.string.field_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            SectionCard(title = stringResource(R.string.field_category)) {
                ChoiceChips(
                    options = TechniqueCategory.entries,
                    selected = state.category,
                    label = { categoryLabel(it) },
                    onSelect = { c -> viewModel.update { it.copy(category = c) } },
                )
            }
            SectionCard(title = stringResource(R.string.field_status)) {
                ChoiceChips(
                    options = TechniqueStatus.entries,
                    selected = state.status,
                    label = { statusLabel(it) },
                    onSelect = viewModel::changeStatus,
                )
                if (state.history.isNotEmpty()) {
                    state.history.sortedBy { it.date }.forEach { change ->
                        Text(
                            stringResource(R.string.technique_history_line, change.date.toString(), statusLabel(change.status)),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            SectionCard(title = stringResource(R.string.technique_details)) {
                OutlinedTextField(
                    value = state.originPosition,
                    onValueChange = { v -> viewModel.update { it.copy(originPosition = v) } },
                    label = { Text(stringResource(R.string.field_origin_position)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.description,
                    onValueChange = { v -> viewModel.update { it.copy(description = v) } },
                    label = { Text(stringResource(R.string.field_description)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.keyNotes,
                    onValueChange = { v -> viewModel.update { it.copy(keyNotes = v) } },
                    label = { Text(stringResource(R.string.field_key_notes)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.videoLinksText,
                    onValueChange = { v -> viewModel.update { it.copy(videoLinksText = v) } },
                    label = { Text(stringResource(R.string.field_video_links)) },
                    supportingText = { Text(stringResource(R.string.field_video_links_hint)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            state.error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget)) {
                Text(stringResource(R.string.action_save))
            }
            if (!state.isNew) {
                OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget)) {
                    Text(stringResource(R.string.action_delete))
                }
            }
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.technique_delete_title),
            onConfirm = { confirmDelete = false; viewModel.delete() },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
fun PlanEditorScreen(
    onDone: () -> Unit,
    viewModel: PlanEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(state.saved) { if (state.saved) onDone() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.plan_editor_title)) },
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
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = { Text(stringResource(R.string.field_plan_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            SectionCard(title = stringResource(R.string.plan_nodes_title)) {
                state.nodes.forEachIndexed { index, node ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Text("${index + 1}. ${node.label} (${nodeKindLabel(node.kind)})", modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.removeNode(node.id) }) { Text(stringResource(R.string.action_remove)) }
                    }
                }
                OutlinedTextField(
                    value = state.newNodeLabel,
                    onValueChange = viewModel::setNewNodeLabel,
                    label = { Text(stringResource(R.string.field_node_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                ChoiceChips(
                    options = PlanNodeKind.entries,
                    selected = state.newNodeKind,
                    label = { nodeKindLabel(it) },
                    onSelect = viewModel::setNewNodeKind,
                )
                OutlinedButton(onClick = viewModel::addNode, modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget)) {
                    Text(stringResource(R.string.action_add_node))
                }
            }
            SectionCard(title = stringResource(R.string.plan_edges_title)) {
                Text(stringResource(R.string.plan_from), style = MaterialTheme.typography.labelLarge)
                ChoiceChips(
                    options = state.nodes,
                    selected = state.nodes.firstOrNull { it.id == state.edgeFrom },
                    label = { it.label },
                    onSelect = { viewModel.selectEdgeFrom(it.id) },
                )
                Text(stringResource(R.string.plan_to), style = MaterialTheme.typography.labelLarge)
                ChoiceChips(
                    options = state.nodes,
                    selected = state.nodes.firstOrNull { it.id == state.edgeTo },
                    label = { it.label },
                    onSelect = { viewModel.selectEdgeTo(it.id) },
                )
                OutlinedButton(onClick = viewModel::addEdge, modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget)) {
                    Text(stringResource(R.string.action_add_connection))
                }
                state.edges.forEachIndexed { index, edge ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        val from = state.nodes.firstOrNull { it.id == edge.fromId }?.label.orEmpty()
                        val to = state.nodes.firstOrNull { it.id == edge.toId }?.label.orEmpty()
                        Text("$from → $to", modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.removeEdge(index) }) { Text(stringResource(R.string.action_remove)) }
                    }
                }
            }
            state.error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget)) {
                Text(stringResource(R.string.action_save))
            }
            OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget)) {
                Text(stringResource(R.string.action_delete))
            }
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.plan_delete_title),
            onConfirm = { confirmDelete = false; viewModel.delete() },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
fun OpponentFormScreen(
    onDone: () -> Unit,
    viewModel: OpponentFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(state.saved) { if (state.saved) onDone() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.opponent_title)) },
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
            OutlinedTextField(
                value = state.name,
                onValueChange = { v -> viewModel.update { it.copy(name = v) } },
                label = { Text(stringResource(R.string.field_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            SectionCard(title = stringResource(R.string.opponent_level_title)) {
                Text(stringResource(R.string.opponents_level, state.level), style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = state.level.toFloat(),
                    onValueChange = { v -> viewModel.update { it.copy(level = v.toInt()) } },
                    valueRange = 1f..5f,
                    steps = 3,
                    modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget),
                )
            }
            SectionCard(title = stringResource(R.string.opponent_notes_title)) {
                OutlinedTextField(
                    value = state.strengths,
                    onValueChange = { v -> viewModel.update { it.copy(strengths = v) } },
                    label = { Text(stringResource(R.string.field_strengths)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.worked,
                    onValueChange = { v -> viewModel.update { it.copy(worked = v) } },
                    label = { Text(stringResource(R.string.field_what_worked)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.didNotWork,
                    onValueChange = { v -> viewModel.update { it.copy(didNotWork = v) } },
                    label = { Text(stringResource(R.string.field_what_did_not_work)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = { v -> viewModel.update { it.copy(notes = v) } },
                    label = { Text(stringResource(R.string.field_notes)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            state.error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget)) {
                Text(stringResource(R.string.action_save))
            }
            OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget)) {
                Text(stringResource(R.string.action_delete))
            }
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.opponent_delete_title),
            onConfirm = { confirmDelete = false; viewModel.delete() },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
    }
}

@Composable
fun ConfirmDialog(title: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(stringResource(R.string.confirm_irreversible)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_delete)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
fun nodeKindLabel(kind: PlanNodeKind): String = stringResource(
    when (kind) {
        PlanNodeKind.START -> R.string.node_start
        PlanNodeKind.POSITION -> R.string.node_position
        PlanNodeKind.TECHNIQUE -> R.string.node_technique
        PlanNodeKind.FINISH -> R.string.node_finish
    },
)

