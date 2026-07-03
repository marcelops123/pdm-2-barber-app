package com.example.pdm2_project

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import com.google.android.material.button.MaterialButton
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.pdm2_project.data.User
import com.example.pdm2_project.data.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserEditActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var edtLogin: EditText
    private lateinit var edtSenha: EditText
    private lateinit var spinnerRole: Spinner
    private lateinit var btnSalvar: MaterialButton
    private lateinit var btnExcluir: MaterialButton

    private var userId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_form)

        sessionManager = SessionManager(this)
        if (!sessionManager.isAdmin()) {
            Toast.makeText(this, "Acesso negado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        userId = intent.getLongExtra(IntentExtras.ID, 0L)

        edtLogin = findViewById(R.id.edtLogin)
        edtSenha = findViewById(R.id.edtSenha)
        spinnerRole = findViewById(R.id.spinnerRole)
        btnSalvar = findViewById(R.id.btnSalvar)
        btnExcluir = findViewById(R.id.btnExcluir)

        val labels = arrayOf("Administrador", "Cliente")
        spinnerRole.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)

        if (userId == 0L) {
            btnExcluir.visibility = View.GONE
            edtSenha.hint = "Senha"
        } else {
            btnExcluir.visibility = View.VISIBLE
            carregarUsuario()
        }

        btnSalvar.setOnClickListener { salvar() }
        btnExcluir.setOnClickListener { excluir() }
    }

    private fun carregarUsuario() {
        val db = appDatabase()
        lifecycleScope.launch {
            val u = withContext(Dispatchers.IO) { db.userDao().getById(userId) }
            if (u == null) {
                Toast.makeText(this@UserEditActivity, "Usuário não encontrado", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            edtLogin.setText(u.login)
            spinnerRole.setSelection(if (u.role == UserRole.ADMIN) 0 else 1)
        }
    }

    private fun roleSelecionada(): UserRole {
        return if (spinnerRole.selectedItemPosition == 0) UserRole.ADMIN else UserRole.CLIENT
    }

    private fun salvar() {
        val login = edtLogin.text.toString().trim()
        val senha = edtSenha.text.toString()

        if (login.isEmpty()) {
            edtLogin.error = "Informe o login"
            return
        }

        if (userId == 0L && senha.isEmpty()) {
            edtSenha.error = "Informe a senha"
            return
        }

        val db = appDatabase()
        lifecycleScope.launch {
            if (userId == 0L) {
                val existe = withContext(Dispatchers.IO) { db.userDao().getByLogin(login) }
                if (existe != null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@UserEditActivity, "Login já existe", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                val novo = User(
                    login = login,
                    passwordHash = PasswordHasher.hash(senha),
                    role = roleSelecionada()
                )
                withContext(Dispatchers.IO) { db.userDao().insert(novo) }
            } else {
                val atual = withContext(Dispatchers.IO) { db.userDao().getById(userId) }
                if (atual == null) {
                    withContext(Dispatchers.Main) { finish() }
                    return@launch
                }
                val outro = withContext(Dispatchers.IO) { db.userDao().getByLogin(login) }
                if (outro != null && outro.id != userId) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@UserEditActivity, "Login já existe", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                val hash = if (senha.isNotEmpty()) PasswordHasher.hash(senha) else atual.passwordHash
                val atualizado = atual.copy(
                    login = login,
                    passwordHash = hash,
                    role = roleSelecionada()
                )
                withContext(Dispatchers.IO) { db.userDao().update(atualizado) }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(this@UserEditActivity, "Salvo", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun excluir() {
        if (userId == sessionManager.getUserId()) {
            Toast.makeText(this, "Não é possível excluir o próprio usuário logado", Toast.LENGTH_SHORT).show()
            return
        }
        val db = appDatabase()
        lifecycleScope.launch {
            val u = withContext(Dispatchers.IO) { db.userDao().getById(userId) }
            if (u != null) {
                withContext(Dispatchers.IO) { db.userDao().delete(u) }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(this@UserEditActivity, "Removido", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
