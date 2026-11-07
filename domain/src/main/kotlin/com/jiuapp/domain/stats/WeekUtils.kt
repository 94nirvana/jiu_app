package com.jiuapp.domain.stats

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Semanas começam na segunda-feira (ISO-8601). */
fun LocalDate.weekStart(): LocalDate =
    with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
