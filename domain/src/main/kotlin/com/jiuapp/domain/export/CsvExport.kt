package com.jiuapp.domain.export

import com.jiuapp.domain.community.AttendanceRecord
import com.jiuapp.domain.community.Student
import com.jiuapp.domain.health.Injury
import com.jiuapp.domain.health.WeightEntry
import com.jiuapp.domain.training.TrainingSession

/**
 * Geração de CSV (RFC 4180). Células começando com = + - @ ou controle são neutralizadas
 * com um apóstrofo, evitando injeção de fórmulas ao abrir no Excel ou no Planilhas.
 */
object Csv {

    private val formulaTriggers = setOf('=', '+', '-', '@', '\t', '\r')

    fun line(cells: List<Any?>): String = cells.joinToString(",") { escape(it?.toString().orEmpty()) }

    fun document(header: List<String>, rows: List<List<Any?>>): String =
        (listOf(line(header)) + rows.map(::line)).joinToString("\r\n", postfix = "\r\n")

    internal fun escape(raw: String): String {
        val startsWithTrigger = raw.firstOrNull()?.let { it in formulaTriggers } == true
        val safe = if (startsWithTrigger) "'$raw" else raw
        val needsQuotes = safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (needsQuotes) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
    }
}

object CsvExport {

    fun sessions(sessions: List<TrainingSession>): String = Csv.document(
        header = listOf("data", "inicio", "duracao_min", "tipo", "intensidade", "faixa", "grau", "academia", "professor", "cansaco", "dor", "humor", "energia", "aprendi", "corrigir", "observacoes", "tags", "pontos_a_corrigir"),
        rows = sessions.filter { it.deletedAt == null }.sortedBy { it.date }.map { s ->
            listOf(
                s.date, s.startTime, s.durationMinutes, s.type.name, s.intensity,
                s.rankOnDay.belt.label, s.rankOnDay.degree, s.academy, s.professor,
                s.fatigue, s.pain, s.mood, s.energy,
                s.learned, s.toFix, s.notes,
                s.tags.sorted().joinToString(";"), s.focusTags.sorted().joinToString(";"),
            )
        },
    )

    fun injuries(injuries: List<Injury>): String = Csv.document(
        header = listOf("regiao", "tipo", "gravidade", "inicio", "previsao_recuperacao", "recuperacao_real", "tratamento", "observacoes"),
        rows = injuries.filter { it.deletedAt == null }.sortedBy { it.startDate }.map { i ->
            listOf(i.region.name, i.type, i.severity, i.startDate, i.expectedRecoveryDate, i.actualRecoveryDate, i.treatment, i.notes)
        },
    )

    fun weights(entries: List<WeightEntry>): String = Csv.document(
        header = listOf("data", "peso_kg"),
        rows = entries.filter { it.deletedAt == null }.sortedBy { it.date }.map { listOf(it.date, it.kg) },
    )

    fun attendance(records: List<AttendanceRecord>, students: List<Student>): String {
        val names = students.associate { it.id to it.name }
        return Csv.document(
            header = listOf("data", "aluno", "turma_id", "presente"),
            rows = records.filter { it.deletedAt == null }.sortedBy { it.date }.map { r ->
                listOf(r.date, names[r.studentId] ?: "desconhecido", r.classId, if (r.present) "sim" else "não")
            },
        )
    }
}
