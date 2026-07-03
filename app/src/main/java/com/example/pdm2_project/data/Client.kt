package com.example.pdm2_project.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "clients",
    indices = [Index(value = ["userId"], unique = true)]
)
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val telefone: String,
    val email: String = "",
    val userId: Long? = null,
    /** Momento do cadastro; usado em relatórios por período. */
    val cadastroUtc: Long = 0L
)
