package com.jiuapp.ui.professor

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiuapp.R
import com.jiuapp.domain.DomainException
import com.jiuapp.domain.belt.Belt
import com.jiuapp.domain.belt.Rank
import com.jiuapp.domain.community.AttendanceRate
import com.jiuapp.domain.community.AttendanceRecord
import com.jiuapp.domain.community.ClassGroup
import com.jiuapp.domain.community.Roster
import com.jiuapp.domain.community.Student
import com.jiuapp.domain.export.CsvExport
import com.jiuapp.domain.repository.RosterRepository
import com.jiuapp.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfessorUiState(
    val loading: Boolean = true,
    val professorMode: Boolean = false,
    val classes: List<ClassGroup> = emptyList(),
    val students: List<Student> = emptyList(),
    val selectedClassId: String? = null,
    val date: LocalDate = LocalDate.now(),
    val presentToday: Set<String> = emptySet(),
    val rates: Map<String, AttendanceRate> = emptyMap(),
    val longAbsent: List<Student> = emptyList(),
    @StringRes val message: Int? = null,
)

@HiltViewModel
class ProfessorViewModel @Inject constructor(
    private val roster: RosterRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
) : ViewModel() {

    private val selectedClass = MutableStateFlow<String?>(null)
    private val date = MutableStateFlow(LocalDate.now(clock))
    private val message = MutableStateFlow<Int?>(null)

    val state: StateFlow<ProfessorUiState> = combine(
        settings.observeSettings(),
        roster.observeClasses(),
        roster.observeStudents(),
        roster.observeAttendance(),
        combine(selectedClass, date, message) { a, b, c -> Triple(a, b, c) },
    ) { appSettings, classes, students, records, (classId, day, note) ->
        val liveStudents = students.filter { it.deletedAt == null && (classId == null || it.classId == classId) }
        val today = LocalDate.now(clock)
        val from = today.minusDays(RATE_WINDOW_DAYS)
        ProfessorUiState(
            loading = false,
            professorMode = appSettings.professorModeEnabled,
            classes = classes.filter { it.deletedAt == null },
            students = liveStudents,
            selectedClassId = classId,
            date = day,
            presentToday = records
                .filter { it.date == day && it.classId == classId && it.present && it.deletedAt == null }
                .map { it.studentId }
                .toSet(),
            rates = liveStudents.associate { it.id to Roster.attendanceRate(records, it.id, from, today) },
            longAbsent = Roster.longAbsent(liveStudents, records, today),
            message = note,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ProfessorUiState())

    fun selectClass(id: String?) = selectedClass.update { if (it == id) null else id }

    fun setDate(day: LocalDate) = date.update { day }

    fun setPresence(studentId: String, present: Boolean) = viewModelScope.launch {
        val classId = selectedClass.value ?: return@launch
        val day = date.value
        roster.saveAttendance(
            listOf(
                AttendanceRecord(
                    studentId = studentId,
                    classId = classId,
                    date = day,
                    present = present,
                    id = Roster.attendanceId(studentId, day),
                    updatedAt = Instant.now(clock),
                ),
            ),
        )
    }

    fun saveClass(name: String, weekday: DayOfWeek?, time: LocalTime?) = viewModelScope.launch {
        try {
            roster.saveClass(ClassGroup(name = name.trim(), weekday = weekday, time = time, id = UUID.randomUUID().toString(), updatedAt = Instant.now(clock)))
            message.update { R.string.class_saved }
        } catch (error: IllegalArgumentException) {
            message.update { R.string.error_name_required }
        }
    }

    fun deleteClass(id: String) = viewModelScope.launch { roster.deleteClass(id, Instant.now(clock)) }

    fun saveStudent(name: String, classId: String, rank: Rank, birthDate: LocalDate?) = viewModelScope.launch {
        try {
            roster.saveStudent(
                Student(
                    name = name.trim(),
                    classId = classId,
                    rank = rank,
                    birthDate = birthDate,
                    id = UUID.randomUUID().toString(),
                    updatedAt = Instant.now(clock),
                ),
            )
            message.update { R.string.student_saved }
        } catch (error: IllegalArgumentException) {
            message.update { R.string.error_name_required }
        }
    }

    /** Graduação de aluno: atualiza a faixa do cadastro. O histórico de presença é preservado. */
    fun graduate(student: Student, belt: Belt, degree: Int) = viewModelScope.launch {
        try {
            roster.saveStudent(student.copy(rank = Rank(belt, degree), updatedAt = Instant.now(clock)))
            message.update { R.string.student_graduated }
        } catch (error: DomainException) {
            message.update { R.string.error_invalid_data }
        }
    }

    fun deleteStudent(id: String) = viewModelScope.launch { roster.deleteStudent(id, Instant.now(clock)) }

    /** Conteúdo CSV da presença de todas as turmas, pronto para compartilhar. */
    suspend fun attendanceCsv(): String = CsvExport.attendance(
        records = roster.observeAttendance().first(),
        students = roster.observeStudents().first(),
    )

    fun clearMessage() = message.update { null }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val RATE_WINDOW_DAYS = 90L
    }
}
