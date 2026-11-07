package com.jiuapp.domain.competition

import com.jiuapp.domain.InvalidCompetitionException
import com.jiuapp.domain.sync.Syncable
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

data class ChecklistItem(
    val label: String,
    val done: Boolean = false,
    val id: String = UUID.randomUUID().toString(),
) {
    init {
        require(label.isNotBlank()) { "Item do checklist sem descrição" }
    }
}

data class Competition(
    val name: String,
    val date: LocalDate,
    val location: String? = null,
    val organizer: String? = null,
    val beltCategory: String? = null,
    val ageCategory: String? = null,
    val weightCategory: String? = null,
    val targetWeightKg: Double? = null,
    val weighInDate: LocalDate? = null,
    val finalPlacement: Int? = null,
    val notes: String? = null,
    val checklist: List<ChecklistItem> = emptyList(),
    override val id: String,
    override val updatedAt: Instant = Instant.EPOCH,
    override val deletedAt: Instant? = null,
) : Syncable {
    init {
        require(name.isNotBlank()) { "Nome do campeonato é obrigatório" }
        if (finalPlacement != null && finalPlacement < 1) throw InvalidCompetitionException("Colocação deve ser ≥ 1")
        if (weighInDate != null && weighInDate.isAfter(date)) {
            throw InvalidCompetitionException("Pesagem não pode ser depois do campeonato")
        }
    }

    fun daysUntil(today: LocalDate): Long = ChronoUnit.DAYS.between(today, date)

    fun isUpcoming(today: LocalDate): Boolean = !date.isBefore(today)

    fun checklistProgress(): Float =
        if (checklist.isEmpty()) 0f else checklist.count { it.done }.toFloat() / checklist.size
}

/** Próximo campeonato a partir de hoje, ou null se não houver. */
fun nextCompetition(competitions: List<Competition>, today: LocalDate): Competition? =
    competitions.filter { it.deletedAt == null && it.isUpcoming(today) }.minByOrNull { it.date }
