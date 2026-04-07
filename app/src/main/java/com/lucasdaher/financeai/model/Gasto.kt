package com.lucasdaher.financeai.model

data class Gasto(
    val id: Int,
    val valor: Double,
    val categoria: String,
    val descricao: String,
    val data: String
)

