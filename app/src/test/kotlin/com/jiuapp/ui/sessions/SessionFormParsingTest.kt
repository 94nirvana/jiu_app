package com.jiuapp.ui.sessions

import kotlin.test.Test
import kotlin.test.assertEquals

class SessionFormParsingTest {

    @Test
    fun `tags sao normalizadas, sem vazios e sem repeticao`() {
        val tags = SessionFormViewModel.parseTags(" Guarda, guarda ,, Passagem ")

        assertEquals(setOf("guarda", "passagem"), tags)
    }

    @Test
    fun `links sao separados por linha e vazios sao descartados`() {
        val links = SessionFormViewModel.parseLines("https://youtu.be/a\n\n  https://instagram.com/b  \n")

        assertEquals(listOf("https://youtu.be/a", "https://instagram.com/b"), links)
    }
}
