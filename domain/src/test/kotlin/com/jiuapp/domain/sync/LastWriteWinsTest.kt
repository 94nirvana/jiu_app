package com.jiuapp.domain.sync

import com.jiuapp.domain.profile.AthleteProfile
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LastWriteWinsTest {

    private fun profile(name: String, at: Long) = AthleteProfile(
        name = name,
        startDate = LocalDate.of(2020, 1, 1),
        updatedAt = Instant.ofEpochSecond(at),
    )

    @Test
    fun `versao remota mais recente vence`() {
        val merged = LastWriteWins.resolve(profile("local", 10), profile("remota", 20))

        assertEquals("remota", merged.name)
    }

    @Test
    fun `versao local mais recente permanece`() {
        val merged = LastWriteWins.resolve(profile("local", 30), profile("remota", 20))

        assertEquals("local", merged.name)
    }

    @Test
    fun `empate mantem a versao local`() {
        val merged = LastWriteWins.resolve(profile("local", 10), profile("remota", 10))

        assertEquals("local", merged.name)
    }

    @Test
    fun `ids diferentes sao rejeitados`() {
        val other = AthleteProfile(name = "x", startDate = LocalDate.of(2020, 1, 1), id = "outro")
        assertFailsWith<IllegalArgumentException> { LastWriteWins.resolve(profile("a", 1), other) }
    }
}
