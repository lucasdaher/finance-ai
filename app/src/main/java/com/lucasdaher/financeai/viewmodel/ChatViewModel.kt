package com.lucasdaher.financeai.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.lucasdaher.financeai.model.Gasto
import com.lucasdaher.financeai.repository.GastoRepository

class ChatViewModel : ViewModel() {

    private val repository = GastoRepository.instance

    private val _mensagens = MutableLiveData<List<Pair<String, Boolean>>>()
    val mensagens: LiveData<List<Pair<String, Boolean>>> = _mensagens

    private val _gastoInterpretado = MutableLiveData<Gasto?>()
    val gastoInterpretado: LiveData<Gasto?> = _gastoInterpretado

    private val listaMensagens = mutableListOf<Pair<String, Boolean>>()

    fun adicionarMensagem(texto: String, isUsuario: Boolean) {
        listaMensagens.add(Pair(texto, isUsuario))
        _mensagens.value = listaMensagens.toList()
    }

    fun confirmarGasto(gasto: Gasto) {
        repository.adicionarGasto(gasto)
        _gastoInterpretado.value = null
    }

    fun setGastoInterpretado(gasto: Gasto?) {
        _gastoInterpretado.value = gasto
    }
}