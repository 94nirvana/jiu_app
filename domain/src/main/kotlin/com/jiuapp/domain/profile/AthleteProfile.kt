package com.jiuapp.domain.profile

import com.jiuapp.domain.sync.Syncable
import java.time.Instant
import java.time.LocalDate

/** Perfil do atleta. A faixa atual não fica aqui: é derivada da última graduação. */
data class AthleteProfile(
    val name: String,
    val startDate: LocalDate,
    val birthDate: LocalDate? = null,
    val academy: String? = null,
    val professor: String? = null,
    val photoUri: String? = null,
    override val id: String = SINGLETON_ID,
    override val updatedAt: Instant = Instant.EPOCH,
    override val deletedAt: Instant? = null,
) : Syncable {
    init {
        require(name.isNotBlank()) { "Nome do atleta é obrigatório" }
        require(name.length <= MAX_NAME_LENGTH) { "Nome muito longo" }
    }

    companion object {
        const val SINGLETON_ID = "athlete-profile"
        const val MAX_NAME_LENGTH = 80
    }
}
