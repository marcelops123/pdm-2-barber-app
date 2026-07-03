package com.example.pdm2_project

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pdm2_project.data.Client
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClientsActivity : BaseSidebarActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var txtVazio: TextView
    private lateinit var edtSearch: EditText
    private var listaCompleta: List<Client> = emptyList()
    private val adapter = ClientAdapter(
        onEdit = { client ->
            startActivity(Intent(this, ClientEditActivity::class.java).putExtra(IntentExtras.ID, client.id))
        },
        onDelete = { client -> confirmarExclusao(client) }
    )

    override fun getContentLayout(): Int = R.layout.activity_clients
    override fun getNavDestination(): NavDestination = NavDestination.CLIENTS

    override fun onContentReady() {
        recyclerView = findViewById(R.id.recyclerView)
        txtVazio = findViewById(R.id.txtVazio)
        edtSearch = findViewById(R.id.edtSearch)
        edtSearch.hint = "Buscar por nome, email ou telefone..."

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        setPageHeader(
            title = "Clientes",
            subtitle = "Gerencie a base de clientes da barbearia",
            actionText = "+ Novo Cliente"
        ) {
            startActivity(Intent(this, ClientEditActivity::class.java).putExtra(IntentExtras.ID, 0L))
        }

        edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.submit(filterList(s?.toString().orEmpty()))
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
                if (sessionManager.isAdmin()) db.clientDao().getAll()
                else {
                    val c = db.clientDao().getByUserId(sessionManager.getUserId())
                    if (c == null) emptyList() else listOf(c)
                }
            }
            val counts = withContext(Dispatchers.IO) {
                listaCompleta.associate { it.id to db.appointmentDao().countByClientId(it.id) }
            }
            adapter.setCounts(counts)
            adapter.submit(filterList(edtSearch.text.toString()))
            txtVazio.visibility = if (listaCompleta.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun filterList(query: String): List<Client> {
        if (query.isBlank()) return listaCompleta
        val q = query.lowercase()
        return listaCompleta.filter {
            it.nome.lowercase().contains(q) ||
                it.email.lowercase().contains(q) ||
                it.telefone.contains(q)
        }
    }

    private fun confirmarExclusao(client: Client) {
        if (!sessionManager.isAdmin()) {
            Toast.makeText(this, "Apenas administradores podem excluir", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Excluir cliente")
            .setMessage("Deseja excluir ${client.nome}?")
            .setPositiveButton("Excluir") { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) { appDatabase().clientDao().delete(client) }
                    carregar()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private class ClientAdapter(
        private val onEdit: (Client) -> Unit,
        private val onDelete: (Client) -> Unit
    ) : RecyclerView.Adapter<ClientAdapter.VH>() {

        private var items: List<Client> = emptyList()
        private var counts: Map<Long, Int> = emptyMap()

        fun submit(list: List<Client>) {
            items = list
            notifyDataSetChanged()
        }

        fun setCounts(map: Map<Long, Int>) {
            counts = map
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_client_card, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val c = items[position]
            holder.txtName.text = c.nome
            holder.txtSince.text = if (c.cadastroUtc > 0) {
                "Cliente desde ${DateFormats.formatDateIso(c.cadastroUtc)}"
            } else {
                "Cliente desde —"
            }
            holder.txtEmail.text = c.email.ifEmpty { "—" }
            holder.txtPhone.text = c.telefone.ifEmpty { "—" }
            val count = counts[c.id] ?: 0
            holder.txtCount.text = "$count agendamentos realizados"
            holder.btnEdit.setOnClickListener { onEdit(c) }
            holder.btnDelete.setOnClickListener { onDelete(c) }
        }

        override fun getItemCount(): Int = items.size

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val txtName: TextView = v.findViewById(R.id.txtClientName)
            val txtSince: TextView = v.findViewById(R.id.txtClientSince)
            val txtEmail: TextView = v.findViewById(R.id.txtClientEmail)
            val txtPhone: TextView = v.findViewById(R.id.txtClientPhone)
            val txtCount: TextView = v.findViewById(R.id.txtAppointmentsCount)
            val btnEdit: ImageView = v.findViewById(R.id.btnEdit)
            val btnDelete: ImageView = v.findViewById(R.id.btnDelete)
        }
    }
}
