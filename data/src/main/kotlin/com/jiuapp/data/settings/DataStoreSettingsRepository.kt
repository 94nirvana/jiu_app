package com.jiuapp.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jiuapp.domain.repository.SettingsRepository
import com.jiuapp.domain.settings.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Preferências em DataStore. Valores inválidos gravados por versões anteriores caem no padrão,
 * em vez de derrubar o app na leitura.
 */
@Singleton
class DataStoreSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {

    private val store: DataStore<Preferences> get() = context.settingsDataStore
    private val writeLock = Mutex()

    override fun observeSettings(): Flow<AppSettings> = store.data.map(::read)

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        writeLock.withLock {
            val current = read(store.data.first())
            val next = transform(current)
            store.edit { prefs -> write(prefs, next) }
        }
    }

    private fun read(prefs: Preferences): AppSettings {
        val defaults = AppSettings()
        return runCatching {
            AppSettings(
                weeklyGoal = prefs[WEEKLY_GOAL] ?: defaults.weeklyGoal,
                defaultSessionMinutes = prefs[SESSION_MINUTES] ?: defaults.defaultSessionMinutes,
                professorModeEnabled = prefs[PROFESSOR_MODE] ?: defaults.professorModeEnabled,
                reminderEnabled = prefs[REMINDER_ENABLED] ?: defaults.reminderEnabled,
                reminderTime = prefs[REMINDER_TIME]?.let(LocalTime::parse) ?: defaults.reminderTime,
                reminderDays = prefs[REMINDER_DAYS]?.let(::parseDays) ?: defaults.reminderDays,
                loadAlertRatio = prefs[LOAD_RATIO] ?: defaults.loadAlertRatio,
                targetWeightKg = prefs[TARGET_WEIGHT],
                onboardingDone = prefs[ONBOARDING_DONE] ?: defaults.onboardingDone,
                privacyConsentDate = prefs[PRIVACY_CONSENT]?.let(LocalDate::parse),
            )
        }.getOrDefault(defaults)
    }

    private fun write(prefs: androidx.datastore.preferences.core.MutablePreferences, s: AppSettings) {
        prefs[WEEKLY_GOAL] = s.weeklyGoal
        prefs[SESSION_MINUTES] = s.defaultSessionMinutes
        prefs[PROFESSOR_MODE] = s.professorModeEnabled
        prefs[REMINDER_ENABLED] = s.reminderEnabled
        prefs[REMINDER_TIME] = s.reminderTime.toString()
        prefs[REMINDER_DAYS] = s.reminderDays.joinToString(",") { it.name }
        prefs[LOAD_RATIO] = s.loadAlertRatio
        s.targetWeightKg?.let { prefs[TARGET_WEIGHT] = it } ?: prefs.remove(TARGET_WEIGHT)
        prefs[ONBOARDING_DONE] = s.onboardingDone
        s.privacyConsentDate?.let { prefs[PRIVACY_CONSENT] = it.toString() } ?: prefs.remove(PRIVACY_CONSENT)
    }

    private fun parseDays(raw: String): Set<DayOfWeek> =
        raw.split(",").filter(String::isNotBlank).map { DayOfWeek.valueOf(it) }.toSet()

    private companion object {
        val WEEKLY_GOAL = intPreferencesKey("weekly_goal")
        val SESSION_MINUTES = intPreferencesKey("default_session_minutes")
        val PROFESSOR_MODE = booleanPreferencesKey("professor_mode")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_TIME = stringPreferencesKey("reminder_time")
        val REMINDER_DAYS = stringPreferencesKey("reminder_days")
        val LOAD_RATIO = doublePreferencesKey("load_alert_ratio")
        val TARGET_WEIGHT = doublePreferencesKey("target_weight_kg")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val PRIVACY_CONSENT = stringPreferencesKey("privacy_consent_date")
    }
}

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
