package com.jiuapp.domain.community

import com.jiuapp.domain.belt.Rank
import com.jiuapp.domain.sync.Syncable
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

data class ClassGroup(
    val name: String,
    val weekday: DayOfWeek? = null,
    val time: LocalTime? = null,
    override val id: String,
    override val updatedAt: Instant = Instant.EPOCH,
    override val deletedAt: Instant? = null,
) : Syncable {
    init {
        require(name.isNotBlank()) { "Nome da turma é obrigatório" }
    }
}

data class Student(
    val name: String,
    val classId: String,
    val rank: Rank,
    val birthDate: LocalDate? = null,
    override val id: String,
    override val updatedAt: Instant = Instant.EPOCH,
    override val deletedAt: Instant? = null,
) : Syncable {
    init {
        require(name.isNotBlank()) { "Nome do aluno é obrigatório" }
    }
}

data class AttendanceRecord(
    val studentId: String,
    val classId: String,
    val date: LocalDate,
    val present: Boolean,
    override val id: String,
    override val updatedAt: Instant = Instant.EPOCH,
    override val deletedAt: Instant? = null,
) : Syncable

data class AttendanceRate(val present: Int, val total: Int) {
    val rate: Double? get() = if (total == 0) null else present.toDouble() / total
}

object Roster {

    /** Um registro por aluno e dia: o id deriva dessa chave para evitar duplicatas na sincronização. */
    fun attendanceId(studentId: String, date: LocalDate): String = "$studentId:$date"

    /** Dias sem presença a partir dos quais o aluno aparece na lista de ausentes. */
    const val DEFAULT_ABSENCE_DAYS = 14L

    fun attendanceRate(records: List<AttendanceRecord>, studentId: String, from: LocalDate, to: LocalDate): AttendanceRate {
        val own = records.filter { it.studentId == studentId && it.deletedAt == null && !it.date.isBefore(from) && !it.date.isAfter(to) }
        return AttendanceRate(present = own.count { it.present }, total = own.size)
    }

    /**
     * Alunos sem presença registrada nos últimos [absenceDays] dias.
     * Alunos sem nenhum registro também aparecem, para que o professor veja quem nunca compareceu.
     */
    fun longAbsent(
        students: List<Student>,
        records: List<AttendanceRecord>,
        today: LocalDate,
        absenceDays: Long = DEFAULT_ABSENCE_DAYS,
    ): List<Student> {
        val lastPresence = records
            .filter { it.present && it.deletedAt == null }
            .groupBy { it.studentId }
            .mapValues { (_, list) -> list.maxOf { it.date } }
        return students
            .filter { it.deletedAt == null }
            .filter { student ->
                val last = lastPresence[student.id] ?: return@filter true
                ChronoUnit.DAYS.between(last, today) >= absenceDays
            }
    }
}
