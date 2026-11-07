package com.jiuapp.domain.technique

import com.jiuapp.domain.sync.Syncable
import java.time.Instant

data class Opponent(
    val name: String,
    val level: Int = 3,
    val strengths: String? = null,
    val worked: String? = null,
    val didNotWork: String? = null,
    val notes: String? = null,
    override val id: String,
    override val updatedAt: Instant = Instant.EPOCH,
    override val deletedAt: Instant? = null,
) : Syncable {
    init {
        require(name.isNotBlank()) { "Nome do adversário é obrigatório" }
        require(level in 1..5) { "Nível deve estar entre 1 e 5" }
    }
}
