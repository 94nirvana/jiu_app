package com.jiuapp.data.mapper

import com.jiuapp.data.db.SessionTagEntity
import com.jiuapp.data.db.TechniqueStatusHistoryEntity
import com.jiuapp.domain.belt.Belt
import com.jiuapp.domain.belt.Rank
import com.jiuapp.domain.competition.ChecklistItem
import com.jiuapp.domain.competition.Competition
import com.jiuapp.domain.technique.GamePlan
import com.jiuapp.domain.technique.PlanEdge
import com.jiuapp.domain.technique.PlanNode
import com.jiuapp.domain.technique.PlanNodeKind
import com.jiuapp.domain.technique.StatusChange
import com.jiuapp.domain.technique.Technique
import com.jiuapp.domain.technique.TechniqueCategory
import com.jiuapp.domain.technique.TechniqueStatus
import com.jiuapp.domain.training.Attachment
import com.jiuapp.domain.training.AttachmentKind
import com.jiuapp.domain.training.TrainingSession
import com.jiuapp.domain.training.TrainingType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class MappersTest {

    private val updated = Instant.parse("2026-03-01T10:00:00Z")

    @Test
    fun `treino sobrevive ao ciclo de linhas e relacoes`() {
        val session = TrainingSession(
            id = "s1",
            date = LocalDate.of(2026, 3, 1),
            durationMinutes = 90,
            type = TrainingType.SPARRING,
            intensity = 4,
            rankOnDay = Rank(Belt.PURPLE, 2),
            fatigue = 3,
            startTime = LocalTime.of(19, 30),
            learned = "Passagem",
            tags = setOf("guarda"),
            focusTags = setOf("queda"),
            techniqueIds = setOf("t1"),
            attachments = listOf(Attachment("a1", AttachmentKind.VIDEO_LINK, "https://youtu.be/x")),
            updatedAt = updated,
        )

        val rows = session.toRows()
        val restored = rows.session.toDomain(
            SessionRelations(
                tags = rows.links.filterNot(SessionTagEntity::focus).map { it.tagName }.toSet(),
                focusTags = rows.links.filter(SessionTagEntity::focus).map { it.tagName }.toSet(),
                attachments = rows.attachments.map { it.toDomain() },
                techniqueIds = rows.techniques.map { it.techniqueId }.toSet(),
            ),
        )

        assertEquals(session, restored)
    }

    @Test
    fun `tags sao normalizadas e sem vazios ao gravar`() {
        val session = TrainingSession(
            id = "s2",
            date = LocalDate.of(2026, 3, 1),
            durationMinutes = 60,
            type = TrainingType.GI,
            intensity = 2,
            rankOnDay = Rank(Belt.WHITE, 0),
            tags = setOf("  Guarda ", "guarda", " "),
        )

        val links = session.toRows().links

        assertEquals(listOf("guarda"), links.map { it.tagName })
    }

    @Test
    fun `tecnica preserva historico de status ordenado`() {
        val technique = Technique(
            name = "Armlock",
            category = TechniqueCategory.SUBMISSION,
            status = TechniqueStatus.WORKS_IN_SPARRING,
            statusHistory = listOf(
                StatusChange(TechniqueStatus.LEARNING, LocalDate.of(2026, 1, 1)),
                StatusChange(TechniqueStatus.WORKS_IN_SPARRING, LocalDate.of(2026, 2, 1)),
            ),
            id = "t1",
            updatedAt = updated,
        )
        val entity = technique.toEntity()
        val history: List<TechniqueStatusHistoryEntity> = technique.historyRows().reversed()

        assertEquals(technique, entity.toDomain(history))
    }

    @Test
    fun `plano de jogo preserva nos e conexoes`() {
        val plan = GamePlan(
            name = "Plano",
            nodes = listOf(PlanNode("a", "Guarda", PlanNodeKind.START, 1f, 2f), PlanNode("b", "Finalização", PlanNodeKind.FINISH)),
            edges = listOf(PlanEdge("a", "b", "se abrir")),
            id = "p1",
        )

        assertEquals(plan, plan.toEntity().toDomain())
    }

    @Test
    fun `competicao preserva checklist`() {
        val competition = Competition(
            name = "Copa",
            date = LocalDate.of(2026, 6, 1),
            checklist = listOf(ChecklistItem("Kimono", done = true, id = "c1")),
            id = "comp1",
        )

        val rows = competition.toRows()

        assertEquals(competition, rows.competition.toDomain(rows.checklist))
    }
}
