package com.example.pdm2_project

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import com.google.android.material.button.MaterialButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var edtLogin: EditText
    private lateinit var edtSenha: EditText
    private lateinit var btnEntrar: MaterialButton
    private lateinit var txtCadastrar: TextView
    private lateinit var txtAdminHint: TextView
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        sessionManager = SessionManager(this)

        edtLogin = findViewById(R.id.edtLogin)
        edtSenha = findViewById(R.id.edtSenha)
        btnEntrar = findViewById(R.id.btnEntrar)
        txtCadastrar = findViewById(R.id.txtCadastrar)
        txtAdminHint = findViewById(R.id.txtAdminHint)

        btnEntrar.setOnClickListener { realizarLogin() }

        txtCadastrar.setOnClickListener {
            startActivity(Intent(this, CadastroActivity::class.java))
        }
    }

    private fun realizarLogin() {
        val login = edtLogin.text.toString().trim()
        val senha = edtSenha.text.toString()

        when {
            login.isEmpty() -> {
                edtLogin.error = "Informe o login"
                edtLogin.requestFocus()
            }

            senha.isEmpty() -> {
                edtSenha.error = "Informe a senha"
                edtSenha.requestFocus()
            }

            else -> {
                val db = appDatabase()
                lifecycleScope.launch {
                    val user = withContext(Dispatchers.IO) { db.userDao().getByLogin(login) }
                    if (user != null && PasswordHasher.verify(senha, user.passwordHash)) {
                        sessionManager.salvarSessao(user.id, user.login, user.role)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@LoginActivity, "Login realizado com sucesso!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@LoginActivity, "Login ou senha inválidos", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}
