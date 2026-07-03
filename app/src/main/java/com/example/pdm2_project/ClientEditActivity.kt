package com.example.pdm2_project

import android.os.Bundle
import android.view.View
import com.google.android.material.button.MaterialButton
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.pdm2_project.data.Client
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClientEditActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var edtNome: EditText
    private lateinit var edtTelefone: EditText
    private lateinit var edtEmail: EditText
    private lateinit var btnSalvar: MaterialButton
    private lateinit var btnExcluir: MaterialButton

    private var clientId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_form)

        sessionManager = SessionManager(this)

        clientId = intent.getLongExtra(IntentExtras.ID, 0L)

        edtNome = findViewById(R.id.edtNome)
        edtTelefone = findViewById(R.id.edtTelefone)
        edtEmail = findViewById(R.id.edtEmail)
        btnSalvar = findViewById(R.id.btnSalvar)
        btnExcluir = findViewById(R.id.btnExcluir)

        if (clientId == 0L) {
            btnExcluir.visibility = View.GONE
        } else {
            btnExcluir.visibility = View.VISIBLE
            carregar()
        }

        btnSalvar.setOnClickListener { salvar() }
        btnExcluir.setOnClickListener { excluir() }
    }

    private fun carregar() {
        val db = appDatabase()
        lifecycleScope.launch {
            val c = withContext(Dispatchers.IO) { db.clientDao().getById(clientId) }
            if (c == null) {
                Toast.makeText(this@ClientEditActivity, "Cliente não encontrado", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            if (!podeEditar(c)) {
                Toast.makeText(this@ClientEditActivity, "Acesso negado", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            edtNome.setText(c.nome)
            edtTelefone.setText(c.telefone)
            edtEmail.setText(c.email)
        }
    }

    private fun podeEditar(c: Client): Boolean {
        if (sessionManager.isAdmin()) return true
        return c.userId != null && c.userId == sessionManager.getUserId()
    }

    private fun salvar() {
        val nome = edtNome.text.toString().trim()
        val tel = edtTelefone.text.toString().trim()
        val email = edtEmail.text.toString().trim()

        if (nome.isEmpty()) {
            edtNome.error = "Informe o nome"
            return
        }
        if (tel.isEmpty()) {
            edtTelefone.error = "Informe o telefone"
            return
        }

        val db = appDatabase()
        lifecycleScope.launch {
            val agora = System.currentTimeMillis()
            if (clientId == 0L) {
                val uid: Long? = if (sessionManager.isAdmin()) {
                    null
                } else {
                    sessionManager.getUserId()
                }
                if (!sessionManager.isAdmin() && uid != null) {
                    val ja = withContext(Dispatchers.IO) { db.clientDao().getByUserId(uid) }
                    if (ja != null) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ClientEditActivity, "Você já possui um cadastro", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }
                }
                val novo = Client(
                    nome = nome,
                    telefone = tel,
                    email = email,
                    userId = uid,
                    cadastroUtc = agora
                )
                withContext(Dispatchers.IO) { db.clientDao().insert(novo) }
            } else {
                val atual = withContext(Dispatchers.IO) { db.clientDao().getById(clientId) }
                if (atual == null || !podeEditar(atual)) {
                    withContext(Dispatchers.Main) { finish() }
                    return@launch
                }
                val atualizado = atual.copy(
                    nome = nome,
                    telefone = tel,
                    email = email
                )
                withContext(Dispatchers.IO) { db.clientDao().update(atualizado) }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ClientEditActivity, "Salvo", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun excluir() {
        val db = appDatabase()
        lifecycleScope.launch {
            val c = withContext(Dispatchers.IO) { db.clientDao().getById(clientId) }
            if (c == null || !podeEditar(c)) {
                withContext(Dispatchers.Main) { finish() }
                return@launch
            }
            withContext(Dispatchers.IO) { db.clientDao().delete(c) }
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ClientEditActivity, "Removido", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
