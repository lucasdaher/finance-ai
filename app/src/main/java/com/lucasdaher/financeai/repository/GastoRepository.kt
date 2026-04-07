package com.lucasdaher.financeai.repository

import com.lucasdaher.financeai.model.Gasto

class GastoRepository {
    companion object {
        val instance = GastoRepository()
    }

    private val _gastos = mutableListOf<Gasto>()
    val gastos: List<Gasto> get() = _gastos

    fun adicionarGasto(gasto: Gasto){
        _gastos.add(gasto)
    }

    fun calcularTotal(): Double {
        return _gastos.sumOf { it.valor }
    }
}