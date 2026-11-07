package com.jiuapp.domain.technique

import com.jiuapp.domain.InvalidAttachmentException
import com.jiuapp.domain.InvalidSessionException
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class TechniqueTest {

    private val start = LocalDate.of(2026, 1, 10)

    private fun technique() = Technique(
        name = "Triângulo",
        category = TechniqueCategory.SUBMISSION,
        statusHistory = listOf(StatusChange(TechniqueStatus.LEARNING, start)),
        id = "t1",
    )

    @Test
    fun `mudanca de status acrescenta historico`() {
        val updated = technique().changeStatus(TechniqueStatus.WORKS_IN_SPARRING, LocalDate.of(2026, 2, 1))

        assertEquals(TechniqueStatus.WORKS_IN_SPARRING, updated.status)
        assertEquals(2, updated.statusHistory.size)
    }

    @Test
    fun `repetir o status atual nao gera entrada`() {
        val original = technique()

        assertSame(original, original.changeStatus(TechniqueStatus.LEARNING, LocalDate.of(2026, 3, 1)))
    }

    @Test
    fun `data anterior a ultima mudanca e rejeitada`() {
        val updated = technique().changeStatus(TechniqueStatus.WORKS_IN_SPARRING, LocalDate.of(2026, 2, 1))

        assertFailsWith<InvalidSessionException> {
            updated.changeStatus(TechniqueStatus.LEARNING, LocalDate.of(2026, 1, 15))
        }
    }

    @Test
    fun `video invalido na tecnica e rejeitado`() {
        assertFailsWith<InvalidAttachmentException> {
            technique().copy(videoLinks = listOf("http://youtube.com/x"))
        }
    }

    @Test
    fun `sugestoes iniciais nao tem nomes repetidos`() {
        val names = SuggestedTechniques.all.map { it.first }

        assertEquals(names.size, names.toSet().size)
    }
}
