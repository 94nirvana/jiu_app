package com.jiuapp.notifications

import com.jiuapp.domain.settings.AppSettings
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReminderSchedulerTest {

    private val monday = LocalDate.of(2026, 11, 2)

    @Test
    fun `avisa apenas nos dias configurados`() {
        val settings = AppSettings(reminderDays = setOf(DayOfWeek.MONDAY))

        assertTrue(ReminderScheduler.shouldRemindOn(settings, monday))
        assertFalse(ReminderScheduler.shouldRemindOn(settings, monday.plusDays(1)))
    }

    @Test
    fun `lembrete desligado nunca avisa`() {
        val settings = AppSettings(reminderEnabled = false, reminderDays = setOf(DayOfWeek.MONDAY))

        assertFalse(ReminderScheduler.shouldRemindOn(settings, monday))
    }
}
