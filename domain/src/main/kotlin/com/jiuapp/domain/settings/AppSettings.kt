package com.jiuapp.domain.settings

import com.jiuapp.domain.InvalidSettingsException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** Preferências do usuário. Persistidas via DataStore; o domínio só valida os limites. */
data class AppSettings(
    val weeklyGoal: Int = DEFAULT_WEEKLY_GOAL,
    val defaultSessionMinutes: Int = DEFAULT_SESSION_MINUTES,
    val professorModeEnabled: Boolean = false,
    val reminderEnabled: Boolean = true,
    val reminderTime: LocalTime = LocalTime.of(19, 0),
    val reminderDays: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
    val loadAlertRatio: Double = DEFAULT_LOAD_ALERT_RATIO,
    val targetWeightKg: Double? = null,
    val onboardingDone: Boolean = false,
    /** Data do aceite do tratamento de dados de saúde (LGPD). Registro de consentimento. */
    val privacyConsentDate: LocalDate? = null,
) {
    init {
        if (weeklyGoal !in 1..MAX_WEEKLY_GOAL) throw InvalidSettingsException("Meta semanal entre 1 e $MAX_WEEKLY_GOAL")
        if (defaultSessionMinutes !in 15..240) throw InvalidSettingsException("Duração padrão entre 15 e 240 min")
        if (loadAlertRatio <= 1.0 || loadAlertRatio > 3.0) throw InvalidSettingsException("Limite de carga entre 1.0 e 3.0")
        targetWeightKg?.let {
            if (it !in 20.0..300.0) throw InvalidSettingsException("Peso-alvo fora da faixa plausível")
        }
    }

    companion object {
        const val DEFAULT_WEEKLY_GOAL = 3
        const val MAX_WEEKLY_GOAL = 14
        const val DEFAULT_SESSION_MINUTES = 90
        const val DEFAULT_LOAD_ALERT_RATIO = 1.5
    }
}
