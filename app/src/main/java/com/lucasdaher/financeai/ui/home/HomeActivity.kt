package com.lucasdaher.financeai.ui.home

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.lucasdaher.financeai.databinding.ActivityHomeBinding
import com.lucasdaher.financeai.ui.GastoAdapter
import com.lucasdaher.financeai.ui.chat.ChatActivity
import com.lucasdaher.financeai.ui.historico.HistoricoActivity
import com.lucasdaher.financeai.viewmodel.HomeViewModel
import kotlin.getValue

class HomeActivity : AppCompatActivity() {

    // ViewBinding
    private lateinit var binding: ActivityHomeBinding

    // ViewModel
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa o ViewBinding
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observarDados()
        configurarBotoes()

        viewModel.atualizarDados()
    }

    private fun observarDados() {
        viewModel.total.observe(this) { total ->
            binding.tvSaldoValor.text = "R$ %.2f".format(total)
        }

        viewModel.gastos.observe(this) { gastos ->
            val adapter = GastoAdapter(this, gastos)
            binding.lvGastosHome.adapter = adapter
        }
    }

    private fun configurarBotoes() {
        binding.btnChat.setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }

        binding.btnHistorico.setOnClickListener {
            startActivity(Intent(this, HistoricoActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // Atualiza os dados ao voltar para a Home
        viewModel.atualizarDados()
    }
}