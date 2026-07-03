package com.example.pdm2_project

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TableLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.example.pdm2_project.data.AppointmentListItem
import com.example.pdm2_project.data.Client
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

class ReportsActivity : BaseSidebarActivity() {

    private lateinit var spinnerTipo: Spinner
    private lateinit var txtDataInicio: TextView
    private lateinit var txtDataFim: TextView
    private lateinit var btnFiltrar: MaterialButton
    private lateinit var btnExportar: MaterialButton
    private lateinit var tableRelatorio: TableLayout
    private lateinit var txtRelatorioVazio: TextView

    private val calInicio: Calendar = Calendar.getInstance()
    private val calFim: Calendar = Calendar.getInstance()
    private var linhasExport: List<String> = emptyList()

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        try {
            contentResolver.openOutputStream(uri)?.use { out ->
                out.write(linhasExport.joinToString("\n").toByteArray(Charsets.UTF_8))
            }
            Toast.makeText(this, "Arquivo exportado", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, "Erro ao exportar", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getContentLayout(): Int = R.layout.activity_reports
    override fun getNavDestination(): NavDestination = NavDestination.REPORTS

    override fun onContentReady() {
        spinnerTipo = findViewById(R.id.spinnerTipo)
        txtDataInicio = findViewById(R.id.txtDataInicio)
        txtDataFim = findViewById(R.id.txtDataFim)
        btnFiltrar = findViewById(R.id.btnFiltrar)
        btnExportar = findViewById(R.id.btnExportar)
        tableRelatorio = findViewById(R.id.tableRelatorio)
        txtRelatorioVazio = findViewById(R.id.txtRelatorioVazio)

        setPageHeader(
            "Relatórios",
            "Análise de agendamentos, clientes e desempenho"
        )

        spinnerTipo.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf("Agendamentos", "Clientes")
        )

        calInicio.set(2024, Calendar.JANUARY, 1)
        calFim.set(2024, Calendar.JUNE, 30)
        atualizarLabelsDatas()

        txtDataInicio.setOnClickListener { escolherData(calInicio) { atualizarLabelsDatas() } }
        txtDataFim.setOnClickListener { escolherData(calFim) { atualizarLabelsDatas() } }
        btnFiltrar.setOnClickListener { gerar() }
        btnExportar.setOnClickListener {
            if (linhasExport.isEmpty()) {
                Toast.makeText(this, "Gere o relatório antes", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            exportLauncher.launch("relatorio_${System.currentTimeMillis()}.txt")
        }
    }

    private fun atualizarLabelsDatas() {
        txtDataInicio.text = DateFormats.formatDate(calInicio.timeInMillis)
        txtDataFim.text = DateFormats.formatDate(calFim.timeInMillis)
    }

    private fun escolherData(cal: Calendar, depois: () -> Unit) {
        android.app.DatePickerDialog(
            this,
            { _, y, m, d ->
                cal.set(Calendar.YEAR, y)
                cal.set(Calendar.MONTH, m)
                cal.set(Calendar.DAY_OF_MONTH, d)
                depois()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun gerar() {
        val startUtc = DateFormats.startOfDayUtc(calInicio)
        val endUtc = DateFormats.endOfDayUtc(calFim)
        if (startUtc > endUtc) {
            Toast.makeText(this, "Data inicial maior que a final", Toast.LENGTH_SHORT).show()
            return
        }

        val tipoAgendamentos = spinnerTipo.selectedItemPosition == 0
        val db = appDatabase()

        lifecycleScope.launch {
            val tabela: Pair<List<String>, List<List<String>>> = withContext(Dispatchers.IO) {
                if (tipoAgendamentos) {
                    val ags: List<AppointmentListItem> = if (sessionManager.isAdmin()) {
                        db.appointmentDao().listInPeriod(startUtc, endUtc)
                    } else {
                        val perfil = db.clientDao().getByUserId(sessionManager.getUserId())
                        if (perfil == null) emptyList()
                        else db.appointmentDao().listForClientsInPeriod(listOf(perfil.id), startUtc, endUtc)
                    }
                    val cabecalho = listOf("Data/hora", "Cliente", "Serviço", "Barbeiro", "Valor")
                    val linhas = ags.map { a ->
                        listOf(
                            DateFormats.formatDateTime(a.dataHoraUtc),
                            a.clientNome,
                            a.servico,
                            a.barbeiro.ifEmpty { "—" },
                            a.valor?.let { v -> String.format(Locale.getDefault(), "%.2f", v) } ?: "—"
                        )
                    }
                    cabecalho to linhas
                } else {
                    val cli: List<Client> = if (sessionManager.isAdmin()) {
                        db.clientDao().listInPeriod(startUtc, endUtc)
                    } else {
                        val perfil = db.clientDao().getByUserId(sessionManager.getUserId())
                        if (perfil != null && perfil.cadastroUtc in startUtc..endUtc) listOf(perfil) else emptyList()
                    }
                    val cabecalho = listOf("Nome", "Telefone", "E-mail", "Cadastro")
                    val linhas = cli.map { c ->
                        listOf(c.nome, c.telefone, c.email, DateFormats.formatDate(c.cadastroUtc))
                    }
                    cabecalho to linhas
                }
            }
            preencherTabela(tabela.first, tabela.second)
            linhasExport = tabela.second.map { cells -> cells.joinToString(" | ") }
            btnExportar.visibility = if (linhasExport.isNotEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun preencherTabela(headers: List<String>, cells: List<List<String>>) {
        tableRelatorio.removeAllViews()
        if (cells.isEmpty()) {
            txtRelatorioVazio.visibility = View.VISIBLE
            return
        }
        txtRelatorioVazio.visibility = View.GONE
        tableRelatorio.addView(wrapRow(*headers.map { h -> tableTextCell(h, bold = true) }.toTypedArray()))
        for (r in cells) {
            tableRelatorio.addView(wrapRow(*r.map { t -> tableTextCell(t, bold = false) }.toTypedArray()))
        }
    }
}
