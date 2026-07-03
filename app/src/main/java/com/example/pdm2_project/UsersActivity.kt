package com.example.pdm2_project

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.pdm2_project.data.User
import com.example.pdm2_project.data.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UsersActivity : BaseSidebarActivity() {

    private lateinit var tableLista: TableLayout
    private lateinit var txtVazio: TextView
    private lateinit var edtSearch: EditText
    private var listaCompleta: List<User> = emptyList()

    override fun getContentLayout(): Int = R.layout.activity_users
    override fun getNavDestination(): NavDestination = NavDestination.USERS

    override fun onCreate(savedInstanceState: Bundle?) {
        if (!SessionManager(this).isAdmin()) {
            Toast.makeText(this, "Acesso negado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        super.onCreate(savedInstanceState)
    }

    override fun onContentReady() {
        if (!sessionManager.isAdmin()) return

        tableLista = findViewById(R.id.tableLista)
        txtVazio = findViewById(R.id.txtVazio)
        edtSearch = findViewById(R.id.edtSearch)
        edtSearch.hint = "Buscar por nome ou email..."

        setPageHeader(
            title = "Usuários",
            subtitle = "Gerencie usuários e permissões do sistema",
            actionText = "+ Novo Usuário"
        ) {
            startActivity(Intent(this, UserEditActivity::class.java).putExtra(IntentExtras.ID, 0L))
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
        if (sessionManager.isAdmin()) carregar()
    }

    private fun carregar() {
        lifecycleScope.launch {
            listaCompleta = withContext(Dispatchers.IO) { appDatabase().userDao().getAll() }
            renderList(filterList(edtSearch.text.toString()))
        }
    }

    private fun filterList(query: String): List<User> {
        if (query.isBlank()) return listaCompleta
        val q = query.lowercase()
        return listaCompleta.filter { it.login.lowercase().contains(q) }
    }

    private fun renderList(lista: List<User>) {
        tableLista.removeAllViews()
        if (lista.isNotEmpty()) {
            tableLista.addView(wrapRow(
                tableTextCell("Nome", bold = true),
                tableTextCell("Email", bold = true),
                tableTextCell("Tipo", bold = true)
            ))
            for (u in lista) {
                val roleLabel = if (u.role == UserRole.ADMIN) "Admin" else "Cliente"
                val roleIcon = ImageView(this).apply {
                    setImageResource(if (u.role == UserRole.ADMIN) R.drawable.ic_shield_24 else R.drawable.ic_account_circle_24)
                    val pad = dpToPx(12)
                    setPadding(pad, dpToPx(10), pad, dpToPx(10))
                }
                val roleCell = android.widget.LinearLayout(this).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    val pad = dpToPx(8)
                    setPadding(pad, dpToPx(10), pad, dpToPx(10))
                    addView(roleIcon)
                    addView(TextView(this@UsersActivity).apply {
                        text = roleLabel
                        setPadding(dpToPx(4), 0, 0, 0)
                        setTextColor(getColor(R.color.text_secondary))
                    })
                }
                val (nome1, nome2) = splitName(u.login)
                val row = wrapRow(
                    tableTwoLineCell(nome1, if (u.role == UserRole.ADMIN) "Admin" else nome2.ifEmpty { roleLabel }, bold = false),
                    tableTextCell(u.login, bold = false),
                    roleCell,
                    onRowClick = {
                        startActivity(Intent(this, UserEditActivity::class.java).putExtra(IntentExtras.ID, u.id))
                    }
                )
                tableLista.addView(row)
            }
        }
        txtVazio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
    }
}
