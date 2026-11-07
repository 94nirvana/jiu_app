package com.jiuapp.domain.settings

import com.jiuapp.domain.InvalidSettingsException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class AppSettingsTest {

    @Test
    fun `valores padrao sao validos`() {
        AppSettings()
    }

    @Test
    fun `meta semanal zero e rejeitada`() {
        assertFailsWith<InvalidSettingsException> { AppSettings(weeklyGoal = 0) }
    }

    @Test
    fun `limite de carga de 1 ou menos e rejeitado`() {
        assertFailsWith<InvalidSettingsException> { AppSettings(loadAlertRatio = 1.0) }
    }

    @Test
    fun `peso alvo implausivel e rejeitado`() {
        assertFailsWith<InvalidSettingsException> { AppSettings(targetWeightKg = 5.0) }
    }
}
