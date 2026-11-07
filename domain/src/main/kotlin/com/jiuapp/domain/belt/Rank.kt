package com.jiuapp.domain.belt

import com.jiuapp.domain.InvalidRankException

/**
 * Faixas do sistema IBJJF para adultos. Coral e vermelha ficam após a preta.
 * Graus vão de 0 (sem grau) até [MAX_DEGREE].
 */
enum class Belt(val label: String) {
    WHITE("Branca"),
    BLUE("Azul"),
    PURPLE("Roxa"),
    BROWN("Marrom"),
    BLACK("Preta"),
    CORAL("Coral"),
    RED("Vermelha");

    companion object {
        const val MAX_DEGREE = 4
    }
}

/** Faixa + grau. Ordenação natural: primeiro pela faixa, depois pelo grau. */
data class Rank(val belt: Belt, val degree: Int = 0) : Comparable<Rank> {

    init {
        if (degree !in 0..Belt.MAX_DEGREE) {
            throw InvalidRankException("Grau fora do intervalo 0..${Belt.MAX_DEGREE}: $degree")
        }
    }

    override fun compareTo(other: Rank): Int =
        compareValuesBy(this, other, { it.belt.ordinal }, { it.degree })
}
