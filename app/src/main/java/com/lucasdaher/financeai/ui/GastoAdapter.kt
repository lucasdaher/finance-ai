package com.lucasdaher.financeai.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.lucasdaher.financeai.R
import com.lucasdaher.financeai.model.Gasto

class GastoAdapter(
    context: Context,
    private val gastos: List<Gasto>
) : ArrayAdapter<Gasto>(context, 0, gastos) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_gasto, parent, false)

        val gasto = gastos[position]

        val tvDescricao = view.findViewById<TextView>(R.id.tvDescricao)
        val tvCategoria = view.findViewById<TextView>(R.id.tvCategoria)
        val tvData = view.findViewById<TextView>(R.id.tvData)
        val tvValor = view.findViewById<TextView>(R.id.tvValor)
        val ivCategoria = view.findViewById<ImageView>(R.id.ivCategoria)

        tvDescricao.text = gasto.descricao
        tvCategoria.text = gasto.categoria
        tvData.text = gasto.data
        tvValor.text = "- R$ %.2f".format(gasto.valor)

        // Ícone por categoria
        ivCategoria.setImageResource(
            when (gasto.categoria) {
                "Alimentação" -> android.R.drawable.ic_menu_gallery
                "Transporte" -> android.R.drawable.ic_menu_mylocation
                "Lazer" -> android.R.drawable.ic_menu_slideshow
                "Saúde" -> android.R.drawable.ic_menu_help
                else -> android.R.drawable.ic_menu_info_details
            }
        )

        return view
    }
}