package com.jiuapp.domain.sync

import java.time.Instant

/** Registro sincronizável. A exclusão é lógica: [deletedAt] marca o registro como removido. */
interface Syncable {
    val id: String
    val updatedAt: Instant
    val deletedAt: Instant?
}

object LastWriteWins {

    /**
     * Resolve conflito entre a versão local e a remota pela data de atualização.
     * Em empate, a versão local prevalece para evitar regravações desnecessárias.
     */
    fun <T : Syncable> resolve(local: T, remote: T): T {
        require(local.id == remote.id) { "Conflito entre ids diferentes: ${local.id} x ${remote.id}" }
        return if (remote.updatedAt.isAfter(local.updatedAt)) remote else local
    }
}
