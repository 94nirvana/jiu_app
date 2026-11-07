package com.jiuapp.domain.export

import kotlin.test.Test
import kotlin.test.assertEquals

class CsvExportTest {

    @Test
    fun `celulas com gatilho de formula sao neutralizadas`() {
        assertEquals("\"'=HYPERLINK(\"\"x\"\")\"", Csv.line(listOf("=HYPERLINK(\"x\")")))
        assertEquals("'+1", Csv.line(listOf("+1")))
        assertEquals("'@cmd", Csv.line(listOf("@cmd")))
    }

    @Test
    fun `virgula aspas e quebra de linha sao escapadas`() {
        assertEquals("\"a,b\"", Csv.line(listOf("a,b")))
        assertEquals("\"diz \"\"oi\"\"\"", Csv.line(listOf("diz \"oi\"")))
        assertEquals("\"linha1\nlinha2\"", Csv.line(listOf("linha1\nlinha2")))
    }

    @Test
    fun `nulo vira celula vazia`() {
        assertEquals("a,,b", Csv.line(listOf("a", null, "b")))
    }

    @Test
    fun `documento tem cabecalho e quebras CRLF`() {
        assertEquals("x,y\r\n1,2\r\n", Csv.document(listOf("x", "y"), listOf(listOf(1, 2))))
    }
}
