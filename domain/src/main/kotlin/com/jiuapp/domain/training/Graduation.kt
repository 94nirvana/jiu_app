package com.jiuapp.domain.training

import com.jiuapp.domain.belt.Rank
import com.jiuapp.domain.sync.Syncable
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class Graduation(
    val rank: Rank,
    val date: LocalDate,
    val professor: String? = null,
    val note: String? = null,
    val photoUri: String? = null,
    override val id: String = UUID.randomUUID().toString(),
    override val updatedAt: Instant = Instant.EPOCH,
    override val deletedAt: Instant? = null,
) : Syncable
