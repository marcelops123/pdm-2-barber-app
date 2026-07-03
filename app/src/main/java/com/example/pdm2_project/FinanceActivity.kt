package com.example.pdm2_project

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pdm2_project.data.FinancialEntry
import com.example.pdm2_project.data.FinancialType
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class FinanceActivity : BaseSidebarActivity() {

    private lateinit var txtReceita: TextView
    private lateinit var txtDespesa: TextView
    private lateinit var txtSaldo: TextView
    private lateinit var chartFinance: LineChart
    private lateinit var recyclerView: RecyclerView
    private val adapter = FinanceAdapter { entry ->
        startActivity(Intent(this, FinanceEditActivity::class.java).putExtra(IntentExtras.ID, entry.id))
    }

    override fun getContentLayout(): Int = R.layout.activity_finance
    override fun getNavDestination(): NavDestination = NavDestination.FINANCE

    override fun onCreate(savedInstanceState: Bundle?) {
        if (!SessionManager(this).isAdmin()) {
            Toast.makeText(this, "Acesso negado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        super.onCreate(savedInstanceState)
    }

    override fun onContentReady() {
        txtReceita = findViewById(R.id.txtReceitaValue)
        txtDespesa = findViewById(R.id.txtDespesaValue)
        txtSaldo = findViewById(R.id.txtSaldoValue)
        chartFinance = findViewById(R.id.chartFinance)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        setPageHeader("Financeiro", "Controle de receitas e despesas")

        findViewById<MaterialButton>(R.id.btnNovaEntrada).setOnClickListener {
            startActivity(Intent(this, FinanceEditActivity::class.java).putExtra(IntentExtras.ID, 0L))
        }
    }

    override fun onResume() {
        super.onResume()
        carregar()
    }

    private fun carregar() {
        val db = appDatabase()
        lifecycleScope.launch {
            val receita = withContext(Dispatchers.IO) { db.financialDao().sumByType(FinancialType.RECEITA) }
            val despesa = withContext(Dispatchers.IO) { db.financialDao().sumByType(FinancialType.DESPESA) }
            val saldo = receita - despesa
            val lista = withContext(Dispatchers.IO) { db.financialDao().getAll() }

            txtReceita.text = String.format(Locale.getDefault(), "R$ %.2f", receita)
            txtDespesa.text = String.format(Locale.getDefault(), "R$ %.2f", despesa)
            txtSaldo.text = String.format(Locale.getDefault(), "R$ %.2f", saldo)
            txtSaldo.setTextColor(getColor(if (saldo >= 0) R.color.success else R.color.danger))

            setupWeeklyChart(db)
            adapter.submit(lista)
        }
    }

    private suspend fun setupWeeklyChart(db: com.example.pdm2_project.data.AppDatabase) {
        val labels = mutableListOf<String>()
        val receitas = mutableListOf<Float>()
        val despesas = mutableListOf<Float>()
        for (week in 5 downTo 0) {
            val start = DashboardHelper.weekStartUtc(week)
            val end = DashboardHelper.weekEndUtc(start)
            labels.add("S${6 - week}")
            receitas.add(
                withContext(Dispatchers.IO) {
                    db.financialDao().sumByTypeInPeriod(FinancialType.RECEITA, start, end).toFloat()
                }
            )
            despesas.add(
                withContext(Dispatchers.IO) {
                    db.financialDao().sumByTypeInPeriod(FinancialType.DESPESA, start, end).toFloat()
                }
            )
        }
        val receitaEntries = receitas.mapIndexed { i, v -> Entry(i.toFloat(), v) }
        val despesaEntries = despesas.mapIndexed { i, v -> Entry(i.toFloat(), v) }
        val receitaSet = LineDataSet(receitaEntries, "Receita").apply {
            color = getColor(R.color.success)
            setCircleColor(getColor(R.color.success))
            lineWidth = 2f
            circleRadius = 3f
            setDrawValues(false)
        }
        val despesaSet = LineDataSet(despesaEntries, "Despesa").apply {
            color = getColor(R.color.danger)
            setCircleColor(getColor(R.color.danger))
            lineWidth = 2f
            circleRadius = 3f
            setDrawValues(false)
        }
        chartFinance.data = LineData(receitaSet, despesaSet)
        chartFinance.description.isEnabled = false
        chartFinance.xAxis.valueFormatter = IndexAxisValueFormatter(labels.toTypedArray())
        chartFinance.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chartFinance.axisRight.isEnabled = false
        chartFinance.invalidate()
    }

    private class FinanceAdapter(
        private val onClick: (FinancialEntry) -> Unit
    ) : RecyclerView.Adapter<FinanceAdapter.VH>() {

        private var items: List<FinancialEntry> = emptyList()

        fun submit(list: List<FinancialEntry>) {
            items = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_two_lines, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val e = items[position]
            val tipo = if (e.tipo == FinancialType.RECEITA) "Receita" else "Despesa"
            holder.linha1.text = "$tipo — ${DateFormats.formatDate(e.dataUtc)}"
            holder.linha2.text = "${e.descricao} | R$ ${String.format(Locale.getDefault(), "%.2f", e.valor)}"
            holder.itemView.setOnClickListener { onClick(e) }
        }

        override fun getItemCount(): Int = items.size

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val linha1: TextView = v.findViewById(R.id.txtLinha1)
            val linha2: TextView = v.findViewById(R.id.txtLinha2)
        }
    }
}
