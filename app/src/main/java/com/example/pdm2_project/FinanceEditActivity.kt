package com.example.pdm2_project

import android.app.DatePickerDialog
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
import com.example.pdm2_project.data.FinancialEntry
import com.example.pdm2_project.data.FinancialType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class FinanceEditActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var spinnerTipo: Spinner
    private lateinit var edtDescricao: EditText
    private lateinit var edtValor: EditText
    private lateinit var btnData: MaterialButton
    private lateinit var txtData: TextView
    private lateinit var btnSalvar: MaterialButton
    private lateinit var btnExcluir: MaterialButton

    private var entryId: Long = 0
    private var dataUtc: Long = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_finance_form)

        sessionManager = SessionManager(this)
        if (!sessionManager.isAdmin()) {
            Toast.makeText(this, "Acesso negado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        entryId = intent.getLongExtra(IntentExtras.ID, 0L)

        spinnerTipo = findViewById(R.id.spinnerTipo)
        edtDescricao = findViewById(R.id.edtDescricao)
        edtValor = findViewById(R.id.edtValor)
        btnData = findViewById(R.id.btnData)
        txtData = findViewById(R.id.txtData)
        btnSalvar = findViewById(R.id.btnSalvar)
        btnExcluir = findViewById(R.id.btnExcluir)

        val tipos = arrayOf("Receita", "Despesa")
        spinnerTipo.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, tipos)

        atualizarLabelData()

        if (entryId == 0L) {
            btnExcluir.visibility = View.GONE
        } else {
            btnExcluir.visibility = View.VISIBLE
            carregar()
        }

        btnData.setOnClickListener { abrirData() }
        btnSalvar.setOnClickListener { salvar() }
        btnExcluir.setOnClickListener { excluir() }
    }

    private fun atualizarLabelData() {
        txtData.text = DateFormats.formatDate(dataUtc)
    }

    private fun abrirData() {
        val cal = Calendar.getInstance()
        cal.timeInMillis = dataUtc
        DatePickerDialog(
            this,
            { _, y, m, d ->
                val c = Calendar.getInstance()
                c.timeInMillis = dataUtc
                c.set(Calendar.YEAR, y)
                c.set(Calendar.MONTH, m)
                c.set(Calendar.DAY_OF_MONTH, d)
                dataUtc = DateFormats.startOfDayUtc(c)
                atualizarLabelData()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun tipoSelecionado(): FinancialType {
        return if (spinnerTipo.selectedItemPosition == 0) FinancialType.RECEITA else FinancialType.DESPESA
    }

    private fun carregar() {
        val db = appDatabase()
        lifecycleScope.launch {
            val e = withContext(Dispatchers.IO) { db.financialDao().getById(entryId) }
            if (e == null) {
                Toast.makeText(this@FinanceEditActivity, "Lançamento não encontrado", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            spinnerTipo.setSelection(if (e.tipo == FinancialType.RECEITA) 0 else 1)
            edtDescricao.setText(e.descricao)
            edtValor.setText(e.valor.toString())
            dataUtc = e.dataUtc
            atualizarLabelData()
        }
    }

    private fun salvar() {
        val desc = edtDescricao.text.toString().trim()
        val valTxt = edtValor.text.toString().trim().replace(",", ".")
        val valor = valTxt.toDoubleOrNull()

        if (desc.isEmpty()) {
            edtDescricao.error = "Informe a descrição"
            return
        }
        if (valor == null) {
            edtValor.error = "Informe o valor"
            return
        }

        val db = appDatabase()
        lifecycleScope.launch {
            if (entryId == 0L) {
                val novo = FinancialEntry(
                    tipo = tipoSelecionado(),
                    descricao = desc,
                    valor = valor,
                    dataUtc = dataUtc
                )
                withContext(Dispatchers.IO) { db.financialDao().insert(novo) }
            } else {
                val atual = withContext(Dispatchers.IO) { db.financialDao().getById(entryId) }
                if (atual == null) {
                    withContext(Dispatchers.Main) { finish() }
                    return@launch
                }
                val atualizado = atual.copy(
                    tipo = tipoSelecionado(),
                    descricao = desc,
                    valor = valor,
                    dataUtc = dataUtc
                )
                withContext(Dispatchers.IO) { db.financialDao().update(atualizado) }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(this@FinanceEditActivity, "Salvo", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun excluir() {
        val db = appDatabase()
        lifecycleScope.launch {
            val e = withContext(Dispatchers.IO) { db.financialDao().getById(entryId) }
            if (e != null) {
                withContext(Dispatchers.IO) { db.financialDao().delete(e) }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(this@FinanceEditActivity, "Removido", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
