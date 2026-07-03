package com.example.pdm2_project

import android.os.Bundle
import android.widget.EditText
import com.google.android.material.button.MaterialButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.pdm2_project.data.User
import com.example.pdm2_project.data.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CadastroActivity : AppCompatActivity() {

    private lateinit var edtNovoLogin: EditText
    private lateinit var edtNovaSenha: EditText
    private lateinit var edtConfirmarSenha: EditText
    private lateinit var btnCadastrarUsuario: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro)

        edtNovoLogin = findViewById(R.id.edtNovoLogin)
        edtNovaSenha = findViewById(R.id.edtNovaSenha)
        edtConfirmarSenha = findViewById(R.id.edtConfirmarSenha)
        btnCadastrarUsuario = findViewById(R.id.btnCadastrarUsuario)

        btnCadastrarUsuario.setOnClickListener { cadastrarUsuario() }
    }

    private fun cadastrarUsuario() {
        val login = edtNovoLogin.text.toString().trim()
        val senha = edtNovaSenha.text.toString()
        val confirmarSenha = edtConfirmarSenha.text.toString()

        when {
            login.isEmpty() -> {
                edtNovoLogin.error = "Informe um login"
                edtNovoLogin.requestFocus()
            }

            senha.isEmpty() -> {
                edtNovaSenha.error = "Informe uma senha"
                edtNovaSenha.requestFocus()
            }

            confirmarSenha.isEmpty() -> {
                edtConfirmarSenha.error = "Confirme a senha"
                edtConfirmarSenha.requestFocus()
            }

            senha != confirmarSenha -> {
                edtConfirmarSenha.error = "As senhas não coincidem"
                edtConfirmarSenha.requestFocus()
            }

            else -> {
                val db = appDatabase()
                lifecycleScope.launch {
                    val existe = withContext(Dispatchers.IO) { db.userDao().getByLogin(login) }
                    if (existe != null) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@CadastroActivity, "Usuário já cadastrado", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }
                    withContext(Dispatchers.IO) {
                        val user = User(
                            login = login,
                            passwordHash = PasswordHasher.hash(senha),
                            role = UserRole.CLIENT
                        )
                        db.userDao().insert(user)
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CadastroActivity, "Conta de cliente criada com sucesso!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }
    }
}
