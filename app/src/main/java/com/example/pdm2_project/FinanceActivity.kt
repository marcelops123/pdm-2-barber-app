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

    private lateinit var recyclerView: RecyclerView
    private val adapter = FinanceAdapter(
        onEntryClick = { entry ->
            startActivity(Intent(this, FinanceEditActivity::class.java).putExtra(IntentExtras.ID, entry.id))
        },
        onNovaEntradaClick = {
            startActivity(Intent(this, FinanceEditActivity::class.java).putExtra(IntentExtras.ID, 0L))
        },
        onHeaderReady = {
            setPageHeader("Financeiro", "Controle de receitas e despesas")
        }
    )

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
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
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
            val chartData = buildWeeklyChartData(db)

            adapter.submit(
                list = lista,
                header = FinanceAdapter.HeaderData(
                    receitaText = String.format(Locale.getDefault(), "R$ %.2f", receita),
                    despesaText = String.format(Locale.getDefault(), "R$ %.2f", despesa),
                    saldoText = String.format(Locale.getDefault(), "R$ %.2f", saldo),
                    saldoColor = getColor(if (saldo >= 0) R.color.success else R.color.danger),
                    isEmpty = lista.isEmpty(),
                    chartData = chartData.first,
                    chartLabels = chartData.second
                )
            )
        }
    }

    private suspend fun buildWeeklyChartData(db: com.example.pdm2_project.data.AppDatabase): Pair<LineData, Array<String>> {
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
        return LineData(receitaSet, despesaSet) to labels.toTypedArray()
    }

    private class FinanceAdapter(
        private val onEntryClick: (FinancialEntry) -> Unit,
        private val onNovaEntradaClick: () -> Unit,
        private val onHeaderReady: () -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        data class HeaderData(
            val receitaText: String,
            val despesaText: String,
            val saldoText: String,
            val saldoColor: Int,
            val isEmpty: Boolean,
            val chartData: LineData,
            val chartLabels: Array<String>
        )

        private var items: List<FinancialEntry> = emptyList()
        private var headerData: HeaderData? = null
        private var headerReadyCalled = false

        fun submit(list: List<FinancialEntry>, header: HeaderData) {
            items = list
            headerData = header
            notifyDataSetChanged()
        }

        override fun getItemViewType(position: Int): Int {
            return if (position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_ENTRY
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return when (viewType) {
                VIEW_TYPE_HEADER -> {
                    val v = inflater.inflate(R.layout.activity_finance_header, parent, false)
                    HeaderVH(v)
                }
                else -> {
                    val v = inflater.inflate(R.layout.item_two_lines, parent, false)
                    EntryVH(v)
                }
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder) {
                is HeaderVH -> bindHeader(holder)
                is EntryVH -> bindEntry(holder, items[position - 1])
            }
        }

        private fun bindHeader(holder: HeaderVH) {
            if (!headerReadyCalled) {
                onHeaderReady()
                holder.btnNovaEntrada.setOnClickListener { onNovaEntradaClick() }
                headerReadyCalled = true
            }

            val data = headerData ?: return
            holder.txtReceita.text = data.receitaText
            holder.txtDespesa.text = data.despesaText
            holder.txtSaldo.text = data.saldoText
            holder.txtSaldo.setTextColor(data.saldoColor)
            holder.txtVazio.visibility = if (data.isEmpty) View.VISIBLE else View.GONE

            holder.chartFinance.data = data.chartData
            holder.chartFinance.description.isEnabled = false
            holder.chartFinance.xAxis.valueFormatter = IndexAxisValueFormatter(data.chartLabels)
            holder.chartFinance.xAxis.position = XAxis.XAxisPosition.BOTTOM
            holder.chartFinance.axisRight.isEnabled = false
            holder.chartFinance.setTouchEnabled(false)
            holder.chartFinance.isDragEnabled = false
            holder.chartFinance.setScaleEnabled(false)
            holder.chartFinance.setPinchZoom(false)
            holder.chartFinance.invalidate()
        }

        private fun bindEntry(holder: EntryVH, entry: FinancialEntry) {
            val tipo = if (entry.tipo == FinancialType.RECEITA) "Receita" else "Despesa"
            holder.linha1.text = "$tipo — ${DateFormats.formatDate(entry.dataUtc)}"
            holder.linha2.text = "${entry.descricao} | R$ ${String.format(Locale.getDefault(), "%.2f", entry.valor)}"
            holder.itemView.setOnClickListener { onEntryClick(entry) }
        }

        override fun getItemCount(): Int = 1 + items.size

        class HeaderVH(v: View) : RecyclerView.ViewHolder(v) {
            val txtReceita: TextView = v.findViewById(R.id.txtReceitaValue)
            val txtDespesa: TextView = v.findViewById(R.id.txtDespesaValue)
            val txtSaldo: TextView = v.findViewById(R.id.txtSaldoValue)
            val chartFinance: LineChart = v.findViewById(R.id.chartFinance)
            val btnNovaEntrada: MaterialButton = v.findViewById(R.id.btnNovaEntrada)
            val txtVazio: TextView = v.findViewById(R.id.txtVazio)
        }

        class EntryVH(v: View) : RecyclerView.ViewHolder(v) {
            val linha1: TextView = v.findViewById(R.id.txtLinha1)
            val linha2: TextView = v.findViewById(R.id.txtLinha2)
        }

        companion object {
            private const val VIEW_TYPE_HEADER = 0
            private const val VIEW_TYPE_ENTRY = 1
        }
    }
}
