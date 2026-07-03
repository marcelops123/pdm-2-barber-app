package com.example.pdm2_project.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "financial_entries",
    indices = [Index("dataUtc")]
)
data class FinancialEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tipo: FinancialType,
    val descricao: String,
    val valor: Double,
    val dataUtc: Long
)
