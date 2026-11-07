package com.jiuapp.ui.techniques

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jiuapp.R
import com.jiuapp.domain.technique.Opponent
import com.jiuapp.domain.technique.Technique
import com.jiuapp.domain.technique.TechniqueCategory
import com.jiuapp.domain.technique.TechniqueStatus
import com.jiuapp.domain.technique.GamePlan
import com.jiuapp.ui.common.ChoiceChips
import com.jiuapp.ui.common.EmptyState
import com.jiuapp.ui.common.LoadingBox
import com.jiuapp.ui.common.Spacing
import com.jiuapp.ui.common.Spacing.touchTarget

private enum class LibraryTab { TECHNIQUES, PLANS, OPPONENTS }

@Composable
fun TechniquesScreen(
    onOpenTechnique: (String) -> Unit,
    onAddTechnique: () -> Unit,
    onOpenPlan: (String) -> Unit,
    onAddPlan: () -> Unit,
    onOpenOpponent: (String) -> Unit,
    onAddOpponent: () -> Unit,
    viewModel: TechniquesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(LibraryTab.TECHNIQUES) }
    if (state.loading) {
        LoadingBox()
        return
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(text = stringResource(R.string.techniques_title), style = MaterialTheme.typography.headlineMedium)
            PrimaryTabRow(selectedTabIndex = tab.ordinal) {
                LibraryTab.entries.forEach { entry ->
                    Tab(
                        selected = tab == entry,
                        onClick = { tab = entry },
                        modifier = Modifier.height(touchTarget),
                        text = {
                            Text(
                                stringResource(
                                    when (entry) {
                                        LibraryTab.TECHNIQUES -> R.string.techniques_tab_library
                                        LibraryTab.PLANS -> R.string.techniques_tab_plans
                                        LibraryTab.OPPONENTS -> R.string.techniques_tab_opponents
                                    },
                                ),
                            )
                        },
                    )
                }
            }
            when (tab) {
                LibraryTab.TECHNIQUES -> TechniqueLibrary(state, viewModel, onOpenTechnique)
                LibraryTab.PLANS -> PlanList(state.plans, onOpenPlan)
                LibraryTab.OPPONENTS -> OpponentList(state.opponents, onOpenOpponent)
            }
        }
        FloatingActionButton(
            onClick = {
                when (tab) {
                    LibraryTab.TECHNIQUES -> onAddTechnique()
                    LibraryTab.PLANS -> onAddPlan()
                    LibraryTab.OPPONENTS -> onAddOpponent()
                }
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(Spacing.md),
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_add))
        }
    }
}

@Composable
private fun TechniqueLibrary(state: LibraryUiState, viewModel: TechniquesViewModel, onOpen: (String) -> Unit) {
    ChoiceChips(
        options = TechniqueCategory.entries,
        selected = state.categoryFilter,
        label = { categoryLabel(it) },
        onSelect = viewModel::setCategory,
    )
    ChoiceChips(
        options = TechniqueStatus.entries,
        selected = state.statusFilter,
        label = { statusLabel(it) },
        onSelect = viewModel::setStatus,
    )
    if (state.suggestionsAdded != null) {
        Text(stringResource(R.string.techniques_suggestions_done, state.suggestionsAdded), style = MaterialTheme.typography.bodySmall)
    }
    OutlinedButton(onClick = viewModel::addSuggestedTechniques, modifier = Modifier.fillMaxWidth().height(touchTarget)) {
        Text(stringResource(R.string.techniques_add_suggested))
    }
    if (state.techniques.isEmpty()) {
        EmptyState(R.string.techniques_empty)
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.sm), contentPadding = PaddingValues(bottom = 96.dp)) {
        items(state.techniques, key = { it.id }) { technique -> TechniqueRow(technique) { onOpen(technique.id) } }
    }
}

@Composable
private fun TechniqueRow(technique: Technique, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(technique.name, style = MaterialTheme.typography.titleSmall)
            Text(
                "${categoryLabel(technique.category)} · ${statusLabel(technique.status)}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun PlanList(plans: List<GamePlan>, onOpen: (String) -> Unit) {
    if (plans.isEmpty()) {
        EmptyState(R.string.plans_empty)
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.sm), contentPadding = PaddingValues(bottom = 96.dp)) {
        items(plans, key = { it.id }) { plan ->
            Card(modifier = Modifier.fillMaxWidth().clickable { onOpen(plan.id) }) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    Text(plan.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(R.string.plans_summary, plan.nodes.size, plan.edges.size),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun OpponentList(opponents: List<Opponent>, onOpen: (String) -> Unit) {
    if (opponents.isEmpty()) {
        EmptyState(R.string.opponents_empty)
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.sm), contentPadding = PaddingValues(bottom = 96.dp)) {
        items(opponents, key = { it.id }) { opponent ->
            Card(modifier = Modifier.fillMaxWidth().clickable { onOpen(opponent.id) }) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    Text(opponent.name, style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.opponents_level, opponent.level), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun categoryLabel(category: TechniqueCategory): String = stringResource(
    when (category) {
        TechniqueCategory.POSITION -> R.string.category_position
        TechniqueCategory.SUBMISSION -> R.string.category_submission
        TechniqueCategory.SWEEP -> R.string.category_sweep
        TechniqueCategory.PASS -> R.string.category_pass
        TechniqueCategory.TAKEDOWN -> R.string.category_takedown
        TechniqueCategory.DEFENSE -> R.string.category_defense
        TechniqueCategory.ESCAPE -> R.string.category_escape
    },
)

@Composable
fun statusLabel(status: TechniqueStatus): String = stringResource(
    when (status) {
        TechniqueStatus.LEARNING -> R.string.status_learning
        TechniqueStatus.WORKS_IN_TECHNICAL_TRAINING -> R.string.status_technical
        TechniqueStatus.WORKS_IN_SPARRING -> R.string.status_sparring
        TechniqueStatus.WORKS_IN_COMPETITION -> R.string.status_competition
    },
)
