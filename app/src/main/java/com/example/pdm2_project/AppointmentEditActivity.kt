package com.example.pdm2_project

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import com.google.android.material.button.MaterialButton
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.pdm2_project.data.AppDatabase
import com.example.pdm2_project.data.Appointment
import com.example.pdm2_project.data.Client
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class AppointmentEditActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var spinnerCliente: Spinner
    private lateinit var btnDataHora: MaterialButton
    private lateinit var txtDataHora: TextView
    private lateinit var edtServico: EditText
    private lateinit var edtBarbeiro: EditText
    private lateinit var edtObservacao: EditText
    private lateinit var edtValor: EditText
    private lateinit var btnSalvar: MaterialButton
    private lateinit var btnExcluir: MaterialButton

    private var appointmentId: Long = 0
    private var selectedUtc: Long = System.currentTimeMillis()

    private var clientesLista: List<Client> = emptyList()
    private var fixedClientId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appointment_form)

        sessionManager = SessionManager(this)
        appointmentId = intent.getLongExtra(IntentExtras.ID, 0L)

        spinnerCliente = findViewById(R.id.spinnerCliente)
        btnDataHora = findViewById(R.id.btnDataHora)
        txtDataHora = findViewById(R.id.txtDataHora)
        edtServico = findViewById(R.id.edtServico)
        edtBarbeiro = findViewById(R.id.edtBarbeiro)
        edtObservacao = findViewById(R.id.edtObservacao)
        edtValor = findViewById(R.id.edtValor)
        btnSalvar = findViewById(R.id.btnSalvar)
        btnExcluir = findViewById(R.id.btnExcluir)

        atualizarLabelData()

        val db = appDatabase()
        lifecycleScope.launch {
            if (sessionManager.isAdmin()) {
                spinnerCliente.visibility = View.VISIBLE
                clientesLista = withContext(Dispatchers.IO) { db.clientDao().getAll() }
                val nomes = clientesLista.map { it.nome }
                spinnerCliente.adapter = ArrayAdapter(
                    this@AppointmentEditActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    nomes
                )
            } else {
                spinnerCliente.visibility = View.GONE
                val perfil = withContext(Dispatchers.IO) {
                    db.clientDao().getByUserId(sessionManager.getUserId())
                }
                if (perfil == null) {
                    Toast.makeText(
                        this@AppointmentEditActivity,
                        "Cadastre seu perfil em Clientes primeiro",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                    return@launch
                }
                fixedClientId = perfil.id
            }

            if (appointmentId != 0L) {
                btnExcluir.visibility = View.VISIBLE
                carregarAgendamentoInterno()
            } else {
                btnExcluir.visibility = View.GONE
            }
        }

        btnDataHora.setOnClickListener { abrirPickerDataHora() }
        btnSalvar.setOnClickListener { salvar() }
        btnExcluir.setOnClickListener { excluir() }
    }

    private fun atualizarLabelData() {
        txtDataHora.text = DateFormats.formatDateTime(selectedUtc)
    }

    private fun abrirPickerDataHora() {
        val cal = Calendar.getInstance()
        cal.timeInMillis = selectedUtc
        DatePickerDialog(
            this,
            { _, y, m, d ->
                val c2 = Calendar.getInstance()
                c2.timeInMillis = selectedUtc
                c2.set(Calendar.YEAR, y)
                c2.set(Calendar.MONTH, m)
                c2.set(Calendar.DAY_OF_MONTH, d)
                TimePickerDialog(
                    this,
                    { _, h, min ->
                        c2.set(Calendar.HOUR_OF_DAY, h)
                        c2.set(Calendar.MINUTE, min)
                        c2.set(Calendar.SECOND, 0)
                        selectedUtc = c2.timeInMillis
                        atualizarLabelData()
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    true
                ).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun clientIdParaSalvar(): Long? {
        if (!sessionManager.isAdmin()) {
            return fixedClientId
        }
        if (clientesLista.isEmpty()) {
            return null
        }
        val idx = spinnerCliente.selectedItemPosition
        if (idx < 0 || idx >= clientesLista.size) return null
        return clientesLista[idx].id
    }

    private suspend fun podeEditar(db: AppDatabase, clientId: Long): Boolean {
        if (sessionManager.isAdmin()) return true
        val perfil = db.clientDao().getByUserId(sessionManager.getUserId())
        return perfil != null && perfil.id == clientId
    }

    private fun carregarAgendamentoInterno() {
        val db = appDatabase()
        lifecycleScope.launch {
            val ag = withContext(Dispatchers.IO) { db.appointmentDao().getById(appointmentId) }
            if (ag == null) {
                Toast.makeText(this@AppointmentEditActivity, "Agendamento não encontrado", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            if (!withContext(Dispatchers.IO) { podeEditar(db, ag.clientId) }) {
                Toast.makeText(this@AppointmentEditActivity, "Acesso negado", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            selectedUtc = ag.dataHoraUtc
            atualizarLabelData()
            edtServico.setText(ag.servico)
            edtBarbeiro.setText(ag.barbeiro)
            edtObservacao.setText(ag.observacao)
            edtValor.setText(ag.valor?.toString().orEmpty())

            if (sessionManager.isAdmin()) {
                val idx = clientesLista.indexOfFirst { it.id == ag.clientId }
                if (idx >= 0) spinnerCliente.setSelection(idx)
            }
        }
    }

    private fun salvar() {
        val db = appDatabase()
        lifecycleScope.launch {
            val cId = clientIdParaSalvar()
            if (cId == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AppointmentEditActivity, "Selecione um cliente", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val servico = edtServico.text.toString().trim()
            val barbeiro = edtBarbeiro.text.toString().trim()
            val obs = edtObservacao.text.toString().trim()
            val valorTxt = edtValor.text.toString().trim()
            val valor = valorTxt.toDoubleOrNull()

            if (appointmentId == 0L) {
                val novo = Appointment(
                    clientId = cId,
                    dataHoraUtc = selectedUtc,
                    servico = servico,
                    observacao = obs,
                    valor = valor,
                    barbeiro = barbeiro
                )
                withContext(Dispatchers.IO) { db.appointmentDao().insert(novo) }
            } else {
                val atual = withContext(Dispatchers.IO) { db.appointmentDao().getById(appointmentId) }
                if (atual == null || !withContext(Dispatchers.IO) { podeEditar(db, atual.clientId) }) {
                    withContext(Dispatchers.Main) { finish() }
                    return@launch
                }
                val atualizado = atual.copy(
                    clientId = cId,
                    dataHoraUtc = selectedUtc,
                    servico = servico,
                    observacao = obs,
                    valor = valor,
                    barbeiro = barbeiro
                )
                withContext(Dispatchers.IO) { db.appointmentDao().update(atualizado) }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(this@AppointmentEditActivity, "Salvo", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun excluir() {
        val db = appDatabase()
        lifecycleScope.launch {
            val ag = withContext(Dispatchers.IO) { db.appointmentDao().getById(appointmentId) }
            if (ag == null || !withContext(Dispatchers.IO) { podeEditar(db, ag.clientId) }) {
                withContext(Dispatchers.Main) { finish() }
                return@launch
            }
            withContext(Dispatchers.IO) { db.appointmentDao().delete(ag) }
            withContext(Dispatchers.Main) {
                Toast.makeText(this@AppointmentEditActivity, "Removido", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
