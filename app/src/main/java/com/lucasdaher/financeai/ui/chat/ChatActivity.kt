package com.lucasdaher.financeai.ui.chat

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.lucasdaher.financeai.databinding.ActivityChatBinding
import com.lucasdaher.financeai.model.Gasto
import com.lucasdaher.financeai.viewmodel.ChatViewModel
import org.json.JSONObject
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

import com.lucasdaher.financeai.BuildConfig

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val viewModel: ChatViewModel by viewModels()
    private var gastoAtual: Gasto? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observarDados()
        configurarBotoes()

        viewModel.adicionarMensagem(getString(com.lucasdaher.financeai.R.string.chat_boas_vindas), false)
    }

    private fun observarDados() {
        viewModel.mensagens.observe(this) { mensagens ->
            val itens = mensagens.map { (texto, isUsuario) ->
                if (isUsuario) "Você: $texto" else "IA: $texto"
            }
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, itens)
            binding.lvMensagens.adapter = adapter
            binding.lvMensagens.setSelection(itens.size - 1)
        }

        viewModel.gastoInterpretado.observe(this) { gasto ->
            if (gasto != null) {
                gastoAtual = gasto
                binding.layoutConfirmacao.visibility = View.VISIBLE
                binding.tvGastoInterpretado.text =
                    "Valor: R$ %.2f\nCategoria: ${gasto.categoria}\nDescrição: ${gasto.descricao}".format(gasto.valor)
            } else {
                binding.layoutConfirmacao.visibility = View.GONE
                gastoAtual = null
            }
        }
    }

    private fun configurarBotoes() {
        binding.btnVoltar.setOnClickListener { finish() }

        binding.btnEnviar.setOnClickListener {
            val mensagem = binding.etMensagem.text.toString().trim()
            if (mensagem.isNotEmpty()) {
                binding.etMensagem.setText("")
                viewModel.adicionarMensagem(mensagem, true)
                enviarParaIA(mensagem)
            }
        }

        binding.btnConfirmar.setOnClickListener {
            gastoAtual?.let {
                viewModel.confirmarGasto(it)
                viewModel.adicionarMensagem("Gasto de R$ %.2f registrado com sucesso!".format(it.valor), false)
            }
        }

        binding.btnCancelar.setOnClickListener {
            viewModel.setGastoInterpretado(null)
            viewModel.adicionarMensagem("Tudo bem, gasto cancelado.", false)
        }
    }

    private fun enviarParaIA(mensagem: String) {
        scope.launch {
            viewModel.adicionarMensagem("Interpretando...", false)
            try {
                android.util.Log.d("FinanceAI", "Iniciando chamada para API...")
                val resposta = withContext(Dispatchers.IO) {
                    chamarAPI(mensagem)
                }
                android.util.Log.d("FinanceAI", "Resposta recebida: $resposta")
                interpretarResposta(resposta)
            } catch (e: Exception) {
                android.util.Log.e("FinanceAI", "Erro na chamada: ${e.javaClass.simpleName} - ${e.message}")
                android.util.Log.e("FinanceAI", "Stack trace: ${e.stackTraceToString()}")
                viewModel.adicionarMensagem("Erro: ${e.message}", false)
            }
        }
    }

    private fun chamarAPI(mensagem: String): String {
        android.util.Log.d("FinanceAI", "Conectando em: https://api.openai.com/v1/chat/completions")

        val url = URL("https://api.openai.com/v1/chat/completions")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Authorization", "Bearer ${BuildConfig.OPENAI_API_KEY}")
        connection.doOutput = true

        val prompt = "Você é um assistente de controle financeiro. " +
                "O usuário disse: \"$mensagem\". " +
                "Extraia o gasto e responda APENAS com um JSON válido neste formato, sem texto extra: " +
                "{\"valor\": 0.0, \"categoria\": \"Alimentação\", \"descricao\": \"descrição curta\"}. " +
                "Categorias possíveis: Alimentação, Transporte, Lazer, Saúde, Outros. " +
                "Se não conseguir identificar um gasto, responda: {\"erro\": \"não identificado\"}"

        // Monta o JSON com segurança usando JSONObject
        val mensagemJson = JSONObject()
        mensagemJson.put("role", "user")
        mensagemJson.put("content", prompt)

        val bodyJson = JSONObject()
        bodyJson.put("model", "gpt-4o-mini")
        bodyJson.put("max_tokens", 200)
        bodyJson.put("messages", org.json.JSONArray().put(mensagemJson))

        val bodyString = bodyJson.toString()
        android.util.Log.d("FinanceAI", "Enviando body: $bodyString")

        connection.outputStream.write(bodyString.toByteArray(Charsets.UTF_8))

        val responseCode = connection.responseCode
        android.util.Log.d("FinanceAI", "Response code: $responseCode")

        return if (responseCode == HttpURLConnection.HTTP_OK) {
            val resposta = connection.inputStream.bufferedReader().readText()
            android.util.Log.d("FinanceAI", "Resposta OK: $resposta")
            resposta
        } else {
            val erro = connection.errorStream?.bufferedReader()?.readText() ?: "sem detalhes"
            android.util.Log.e("FinanceAI", "Erro HTTP $responseCode: $erro")
            throw Exception("HTTP $responseCode: $erro")
        }
    }

    private fun interpretarResposta(respostaRaw: String) {
        try {
            val respostaJson = JSONObject(respostaRaw)
            val texto = respostaJson
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()

            val dados = JSONObject(texto)

            if (dados.has("erro")) {
                viewModel.adicionarMensagem("Não consegui identificar um gasto. Tente descrever melhor, ex: \"Gastei R\$30 no almoço\"", false)
                return
            }

            val data = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            val gasto = Gasto(
                id = System.currentTimeMillis().toInt(),
                valor = dados.getDouble("valor"),
                categoria = dados.getString("categoria"),
                descricao = dados.getString("descricao"),
                data = data
            )

            viewModel.adicionarMensagem("Encontrei este gasto, confirma?", false)
            viewModel.setGastoInterpretado(gasto)

        } catch (e: Exception) {
            viewModel.adicionarMensagem("Não consegui interpretar o gasto. Tente novamente.", false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}