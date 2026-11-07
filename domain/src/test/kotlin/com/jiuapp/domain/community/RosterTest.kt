package com.jiuapp.domain.community

import com.jiuapp.domain.belt.Belt
import com.jiuapp.domain.belt.Rank
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RosterTest {

    private val today = LocalDate.of(2026, 5, 1)
    private val ana = Student("Ana", "turma-1", Rank(Belt.BLUE, 1), id = "s1")
    private val bruno = Student("Bruno", "turma-1", Rank(Belt.WHITE, 2), id = "s2")

    private fun record(student: Student, date: LocalDate, present: Boolean, id: String) =
        AttendanceRecord(student.id, "turma-1", date, present, id = id)

    @Test
    fun `taxa de presenca considera apenas o periodo`() {
        val records = listOf(
            record(ana, LocalDate.of(2026, 4, 1), true, "1"),
            record(ana, LocalDate.of(2026, 4, 2), false, "2"),
            record(ana, LocalDate.of(2026, 1, 1), true, "3"),
        )

        val rate = Roster.attendanceRate(records, ana.id, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30))

        assertEquals(1, rate.present)
        assertEquals(2, rate.total)
        assertEquals(0.5, rate.rate)
    }

    @Test
    fun `taxa sem registros e nula`() {
        assertNull(Roster.attendanceRate(emptyList(), ana.id, today, today).rate)
    }

    @Test
    fun `ausentes incluem quem nunca compareceu e quem esta sem presenca ha muitos dias`() {
        val records = listOf(record(ana, today.minusDays(3), true, "1"))

        val absent = Roster.longAbsent(listOf(ana, bruno), records, today)

        assertEquals(listOf("Bruno"), absent.map { it.name })
    }

    @Test
    fun `aluno presente recentemente nao aparece como ausente`() {
        val records = listOf(record(ana, today.minusDays(13), true, "1"))

        assertFalse(Roster.longAbsent(listOf(ana), records, today).any { it.id == ana.id })
        assertTrue(Roster.longAbsent(listOf(ana), records, today, absenceDays = 7).any { it.id == ana.id })
    }
}
