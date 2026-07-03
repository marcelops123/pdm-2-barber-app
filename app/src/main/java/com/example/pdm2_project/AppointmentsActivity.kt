package com.example.pdm2_project

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TableLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.example.pdm2_project.data.AppointmentListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppointmentsActivity : BaseSidebarActivity() {

    private lateinit var tableLista: TableLayout
    private lateinit var txtVazio: TextView
    private lateinit var edtSearch: EditText
    private var listaCompleta: List<AppointmentListItem> = emptyList()

    override fun getContentLayout(): Int = R.layout.activity_appointments
    override fun getNavDestination(): NavDestination = NavDestination.APPOINTMENTS

    override fun onContentReady() {
        tableLista = findViewById(R.id.tableLista)
        txtVazio = findViewById(R.id.txtVazio)
        edtSearch = findViewById(R.id.edtSearch)
        edtSearch.hint = "Buscar por cliente ou serviço..."

        setPageHeader(
            title = "Agendamentos",
            subtitle = "Gerencie todos os agendamentos de cortes",
            actionText = "+ Novo Agendamento"
        ) {
            startActivity(Intent(this, AppointmentEditActivity::class.java).putExtra(IntentExtras.ID, 0L))
        }

        edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                renderList(filterList(s?.toString().orEmpty()))
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        carregar()
    }

    private fun carregar() {
        val db = appDatabase()
        lifecycleScope.launch {
            listaCompleta = withContext(Dispatchers.IO) {
                if (sessionManager.isAdmin()) {
                    db.appointmentDao().listAllWithClient()
                } else {
                    val perfil = db.clientDao().getByUserId(sessionManager.getUserId())
                    if (perfil == null) emptyList()
                    else db.appointmentDao().listByClientId(perfil.id)
                }
            }
            renderList(filterList(edtSearch.text.toString()))
        }
    }

    private fun filterList(query: String): List<AppointmentListItem> {
        if (query.isBlank()) return listaCompleta
        val q = query.lowercase()
        return listaCompleta.filter {
            it.clientNome.lowercase().contains(q) || it.servico.lowercase().contains(q)
        }
    }

    private fun renderList(lista: List<AppointmentListItem>) {
        tableLista.removeAllViews()
        if (lista.isNotEmpty()) {
            val header = wrapRow(
                tableTextCell("Cliente", bold = true),
                tableTextCell("Data", bold = true),
                tableTextCell("Hora", bold = true),
                tableTextCell("Serviço", bold = true),
                tableTextCell("Barbeiro", bold = true)
            )
            tableLista.addView(header)
            for (a in lista) {
                val (nome1, nome2) = splitName(a.clientNome)
                val row = wrapRow(
                    tableTwoLineCell(nome1, nome2, bold = false),
                    tableTextCell(DateFormats.formatDateIso(a.dataHoraUtc), bold = false),
                    tableTextCell(DateFormats.formatTime(a.dataHoraUtc), bold = false),
                    tableTextCell(a.servico.ifEmpty { "—" }, bold = false),
                    tableTextCell(a.barbeiro.ifEmpty { "—" }, bold = false),
                    onRowClick = {
                        startActivity(
                            Intent(this@AppointmentsActivity, AppointmentEditActivity::class.java)
                                .putExtra(IntentExtras.ID, a.id)
                        )
                    }
                )
                tableLista.addView(row)
            }
        }
        txtVazio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
    }
}
