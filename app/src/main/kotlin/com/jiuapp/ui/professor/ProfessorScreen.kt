package com.jiuapp.ui.professor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jiuapp.R
import com.jiuapp.ui.common.currentLocale
import com.jiuapp.domain.belt.Belt
import com.jiuapp.domain.belt.Rank
import com.jiuapp.domain.community.ClassGroup
import com.jiuapp.domain.community.Student
import com.jiuapp.export.ShareHelper
import com.jiuapp.ui.common.ChoiceChips
import com.jiuapp.ui.common.DateField
import com.jiuapp.ui.common.EmptyState
import com.jiuapp.ui.common.LoadingBox
import com.jiuapp.ui.common.SectionCard
import com.jiuapp.ui.common.Spacing
import com.jiuapp.ui.common.beltLabel
import com.jiuapp.ui.common.rankLabel
import com.jiuapp.ui.techniques.BackButton
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import kotlinx.coroutines.launch

@Composable
fun ProfessorScreen(onBack: () -> Unit, viewModel: ProfessorViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var addClass by remember { mutableStateOf(false) }
    var addStudent by remember { mutableStateOf<Boolean>(false) }
    var graduating by remember { mutableStateOf<Student?>(null) }

    val messageText = state.message?.let { stringResource(it) }
    LaunchedEffect(messageText) {
        if (messageText != null) {
            snackbar.showSnackbar(messageText)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.professor_title)) }, navigationIcon = { BackButton(onBack) }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (state.loading) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        if (!state.professorMode) {
            EmptyState(R.string.professor_disabled, Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            SectionCard(title = stringResource(R.string.professor_classes)) {
                if (state.classes.isEmpty()) Text(stringResource(R.string.professor_no_classes), style = MaterialTheme.typography.bodyMedium)
                state.classes.forEach { classGroup ->
                    ClassLine(classGroup, selected = classGroup.id == state.selectedClassId, onSelect = { viewModel.selectClass(classGroup.id) }, onDelete = { viewModel.deleteClass(classGroup.id) })
                }
                OutlinedButton(onClick = { addClass = true }, modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget)) {
                    Text(stringResource(R.string.professor_add_class))
                }
            }

            SectionCard(title = stringResource(R.string.professor_roll_call)) {
                if (state.selectedClassId == null) {
                    Text(stringResource(R.string.professor_pick_class), style = MaterialTheme.typography.bodyMedium)
                } else {
                    DateField(stringResource(R.string.field_date), state.date, { it?.let(viewModel::setDate) })
                    if (state.students.isEmpty()) Text(stringResource(R.string.professor_no_students), style = MaterialTheme.typography.bodyMedium)
                    state.students.forEach { student ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = student.id in state.presentToday,
                                onCheckedChange = { viewModel.setPresence(student.id, it) },
                            )
                            Text(student.name, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            SectionCard(title = stringResource(R.string.professor_students)) {
                state.students.forEach { student ->
                    Column {
                        Text("${student.name} · ${rankLabel(student.rank)}", style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            TextButton(onClick = { graduating = student }) { Text(stringResource(R.string.professor_graduate)) }
                            TextButton(onClick = { viewModel.deleteStudent(student.id) }) { Text(stringResource(R.string.action_delete)) }
                            state.rates[student.id]?.rate?.let {
                                Text(stringResource(R.string.professor_rate, it * PERCENT), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                OutlinedButton(onClick = { addStudent = true }, modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget)) {
                    Text(stringResource(R.string.professor_add_student))
                }
            }

            SectionCard(title = stringResource(R.string.professor_long_absent)) {
                if (state.longAbsent.isEmpty()) {
                    Text(stringResource(R.string.professor_none_absent), style = MaterialTheme.typography.bodyMedium)
                } else {
                    state.longAbsent.forEach { Text(it.name, style = MaterialTheme.typography.bodyMedium) }
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        val csv = viewModel.attendanceCsv()
                        val file = ShareHelper.writeFile(context, "presenca.csv", csv.toByteArray(Charsets.UTF_8))
                        context.startActivity(Intent.createChooser(ShareHelper.shareIntent(context, file, "text/csv"), null))
                    }
                },
                modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget),
            ) { Text(stringResource(R.string.professor_export_csv)) }
        }
    }

    if (addClass) {
        ClassDialog(
            onDismiss = { addClass = false },
            onSave = { name, day, time ->
                viewModel.saveClass(name, day, time)
                addClass = false
            },
        )
    }
    if (addStudent) {
        StudentDialog(
            classes = state.classes,
            onDismiss = { addStudent = false },
            onSave = { name, classId, rank, birth ->
                viewModel.saveStudent(name, classId, rank, birth)
                addStudent = false
            },
        )
    }
    graduating?.let { student ->
        GraduateDialog(
            student = student,
            onDismiss = { graduating = null },
            onSave = { belt, degree ->
                viewModel.graduate(student, belt, degree)
                graduating = null
            },
        )
    }
}

@Composable
private fun ClassLine(classGroup: ClassGroup, selected: Boolean, onSelect: () -> Unit, onDelete: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = onSelect, modifier = Modifier.weight(1f).height(Spacing.touchTarget)) {
            val day = classGroup.weekday?.getDisplayName(TextStyle.SHORT, currentLocale()).orEmpty()
            val time = classGroup.time?.format(DateTimeFormatter.ofPattern("HH:mm")).orEmpty()
            Text(listOf(classGroup.name, day, time).filter { it.isNotEmpty() }.joinToString(" · ") + if (selected) " ✓" else "")
        }
        TextButton(onClick = onDelete) { Text(stringResource(R.string.action_delete)) }
    }
}

@Composable
private fun ClassDialog(onDismiss: () -> Unit, onSave: (String, DayOfWeek?, LocalTime?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var day by remember { mutableStateOf<DayOfWeek?>(null) }
    var time by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<Int?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.professor_add_class)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.field_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                ChoiceChips(DayOfWeek.entries, day, { it.getDisplayName(TextStyle.SHORT, currentLocale()) }, { day = if (it == day) null else it })
                OutlinedTextField(time, { time = it }, label = { Text(stringResource(R.string.field_reminder_time)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = time.trim().ifEmpty { null }?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
                when {
                    name.isBlank() -> error = R.string.error_name_required
                    time.isNotBlank() && parsed == null -> error = R.string.error_time_format
                    else -> onSave(name, day, parsed)
                }
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun StudentDialog(classes: List<ClassGroup>, onDismiss: () -> Unit, onSave: (String, String, Rank, LocalDate?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var classId by remember { mutableStateOf(classes.firstOrNull()?.id) }
    var belt by remember { mutableStateOf(Belt.WHITE) }
    var degree by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<Int?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.professor_add_student)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.field_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (classes.isEmpty()) {
                    Text(stringResource(R.string.professor_no_classes), color = MaterialTheme.colorScheme.error)
                } else {
                    ChoiceChips(classes, classes.firstOrNull { it.id == classId }, { it.name }, { classId = it.id })
                }
                ChoiceChips(Belt.entries, belt, { beltLabel(it) }, { belt = it })
                ChoiceChips((0..4).toList(), degree, { if (it == 0) stringResource(R.string.degree_none) else "$it" }, { degree = it })
                error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val target = classId
                when {
                    name.isBlank() -> error = R.string.error_name_required
                    target == null -> error = R.string.professor_no_classes
                    else -> onSave(name, target, Rank(belt, degree), null)
                }
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun GraduateDialog(student: Student, onDismiss: () -> Unit, onSave: (Belt, Int) -> Unit) {
    var belt by remember { mutableStateOf(student.rank.belt) }
    var degree by remember { mutableStateOf(student.rank.degree) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.professor_graduate_title, student.name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                ChoiceChips(Belt.entries, belt, { beltLabel(it) }, { belt = it })
                ChoiceChips((0..4).toList(), degree, { if (it == 0) stringResource(R.string.degree_none) else "$it" }, { degree = it })
            }
        },
        confirmButton = { TextButton(onClick = { onSave(belt, degree) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

private const val PERCENT = 100.0
