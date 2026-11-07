package com.jiuapp.ui.onboarding

import com.jiuapp.R
import com.jiuapp.domain.belt.Belt
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OnboardingStateTest {

    private val valid = OnboardingState(
        step = 1,
        name = "Ana",
        startDate = LocalDate.now().minusYears(1),
    )

    @Test
    fun `consentimento e obrigatorio no primeiro passo`() {
        assertEquals(R.string.error_consent_required, OnboardingState(step = 0, consent = false).stepError())
        assertNull(OnboardingState(step = 0, consent = true).stepError())
    }

    @Test
    fun `nome vazio e rejeitado no passo de perfil`() {
        assertEquals(R.string.error_name_required, valid.copy(name = "   ").stepError())
    }

    @Test
    fun `data de inicio no futuro e rejeitada`() {
        assertEquals(R.string.error_date_in_future, valid.copy(startDate = LocalDate.now().plusDays(1)).stepError())
    }

    @Test
    fun `data de graduacao obrigatoria no passo de faixa`() {
        val state = OnboardingState(step = 2, belt = Belt.BLUE, graduationDate = null)

        assertEquals(R.string.error_graduation_date_required, state.stepError())
    }

    @Test
    fun `ultimo passo nao tem validacao extra`() {
        assertNull(OnboardingState(step = OnboardingState.LAST_STEP).stepError())
    }
}
