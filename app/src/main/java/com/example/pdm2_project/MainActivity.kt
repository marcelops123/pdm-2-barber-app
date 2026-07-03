package com.example.pdm2_project

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.example.pdm2_project.data.FinancialType
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : BaseSidebarActivity() {

    override fun getContentLayout(): Int = R.layout.activity_dashboard
    override fun getNavDestination(): NavDestination = NavDestination.DASHBOARD

    override fun onContentReady() {
        carregarDashboard()
    }

    override fun onResume() {
        super.onResume()
        carregarDashboard()
    }

    private fun carregarDashboard() {
        val db = appDatabase()
        lifecycleScope.launch {
            val (curStart, curEnd) = DashboardHelper.currentMonthRange()
            val (prevStart, prevEnd) = DashboardHelper.previousMonthRange()

            val agendamentosAtual = withContext(Dispatchers.IO) {
                db.appointmentDao().countInPeriod(curStart, curEnd)
            }
            val agendamentosAnterior = withContext(Dispatchers.IO) {
                db.appointmentDao().countInPeriod(prevStart, prevEnd)
            }
            val clientesAtual = withContext(Dispatchers.IO) {
                db.clientDao().countInPeriod(curStart, curEnd)
            }
            val clientesAnterior = withContext(Dispatchers.IO) {
                db.clientDao().countInPeriod(prevStart, prevEnd)
            }
            val clientesTotal = withContext(Dispatchers.IO) { db.clientDao().count() }
            val usuariosTotal = withContext(Dispatchers.IO) { db.userDao().count() }
            val receitaAtual = withContext(Dispatchers.IO) {
                db.financialDao().sumByTypeInPeriod(FinancialType.RECEITA, curStart, curEnd)
            }
            val receitaAnterior = withContext(Dispatchers.IO) {
                db.financialDao().sumByTypeInPeriod(FinancialType.RECEITA, prevStart, prevEnd)
            }

            bindKpi(
                R.id.kpiAgendamentos,
                "Agendamentos",
                agendamentosAtual.toString(),
                DashboardHelper.growthPercent(agendamentosAtual, agendamentosAnterior),
                R.drawable.ic_nav_calendar
            )
            bindKpi(
                R.id.kpiClientes,
                "Clientes",
                clientesTotal.toString(),
                DashboardHelper.growthPercent(clientesAtual, clientesAnterior),
                R.drawable.ic_nav_people
            )
            bindKpi(
                R.id.kpiReceita,
                "Receita",
                String.format(Locale.getDefault(), "R$ %.0f", receitaAtual),
                DashboardHelper.growthPercent(receitaAtual.toLong(), receitaAnterior.toLong()),
                R.drawable.ic_nav_money
            )
            bindKpi(
                R.id.kpiUsuarios,
                "Usuários Ativos",
                usuariosTotal.toString(),
                "+3% este mês",
                R.drawable.ic_nav_user
            )

            setupChart(db)
        }
    }

    private fun bindKpi(rootId: Int, label: String, value: String, growth: String, iconRes: Int) {
        val root = findViewById<View>(rootId)
        root.findViewById<TextView>(R.id.txtKpiLabel).text = label
        root.findViewById<TextView>(R.id.txtKpiValue).text = value
        val growthView = root.findViewById<TextView>(R.id.txtKpiGrowth)
        growthView.text = growth
        growthView.visibility = if (growth == "—") View.GONE else View.VISIBLE
        root.findViewById<ImageView>(R.id.imgKpiIcon).setImageResource(iconRes)
    }

    private suspend fun setupChart(db: com.example.pdm2_project.data.AppDatabase) {
        val months = DashboardHelper.monthYearPairs(6)
        val counts = withContext(Dispatchers.IO) {
            months.map { (_, year, month) ->
                db.appointmentDao().countByYearMonth(year, month).toFloat()
            }
        }
        val labels = months.map { it.first }.toTypedArray()
        val entries = counts.mapIndexed { i, v -> Entry(i.toFloat(), v) }

        val chart = findViewById<LineChart>(R.id.chartAgendamentos)
        val dataSet = LineDataSet(entries, "Agendamentos").apply {
            color = resources.getColor(R.color.accent_blue, theme)
            setCircleColor(resources.getColor(R.color.accent_blue, theme))
            lineWidth = 2f
            circleRadius = 4f
            setDrawValues(false)
        }
        chart.data = LineData(dataSet)
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.axisRight.isEnabled = false
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        chart.xAxis.granularity = 1f
        chart.invalidate()
    }
}
