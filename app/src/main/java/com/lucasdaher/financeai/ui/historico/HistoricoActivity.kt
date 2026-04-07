package com.lucasdaher.financeai.ui.historico

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.lucasdaher.financeai.databinding.ActivityHistoricoBinding
import com.lucasdaher.financeai.viewmodel.HistoricoViewModel

class HistoricoActivity : AppCompatActivity() {

    // ViewBinding
    private lateinit var binding: ActivityHistoricoBinding

    // ViewModel
    private val viewModel: HistoricoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHistoricoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observarDados()
        configurarBotoes()

        viewModel.carregarGastos()
    }

    private fun observarDados() {
        viewModel.gastos.observe(this) { gastos ->
            if (gastos.isEmpty()) {
                binding.tvHistoricoVazio.visibility = View.VISIBLE
                binding.lvHistorico.visibility = View.GONE
            } else {
                binding.tvHistoricoVazio.visibility = View.GONE
                binding.lvHistorico.visibility = View.VISIBLE

                val itens = gastos.map {
                    "${it.categoria} — R$ %.2f\n${it.descricao} • ${it.data}".format(it.valor)
                }
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, itens)
                binding.lvHistorico.adapter = adapter
            }
        }
    }

    private fun configurarBotoes() {
        // Demonstração de findViewById (comparação com ViewBinding)
        val btnVoltar = findViewById<android.widget.ImageView>(com.lucasdaher.financeai.R.id.btnVoltar)
        btnVoltar.setOnClickListener {
            finish()
        }
    }
}