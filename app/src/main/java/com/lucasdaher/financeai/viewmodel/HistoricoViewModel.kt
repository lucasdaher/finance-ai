package com.lucasdaher.financeai.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.lucasdaher.financeai.model.Gasto
import com.lucasdaher.financeai.repository.GastoRepository

class HistoricoViewModel : ViewModel() {

    private val repository = GastoRepository.instance

    private val _gastos = MutableLiveData<List<Gasto>>()
    val gastos: LiveData<List<Gasto>> = _gastos

    fun carregarGastos() {
        _gastos.value = repository.gastos
    }
}