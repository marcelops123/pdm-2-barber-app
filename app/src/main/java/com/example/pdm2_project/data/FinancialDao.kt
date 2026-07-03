package com.example.pdm2_project.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface FinancialDao {

    @Query("SELECT * FROM financial_entries ORDER BY dataUtc DESC")
    suspend fun getAll(): List<FinancialEntry>

    @Query(
        """
        SELECT COALESCE(SUM(valor), 0) FROM financial_entries
        WHERE tipo = :tipo AND dataUtc >= :startUtc AND dataUtc <= :endUtc
        """
    )
    suspend fun sumByTypeInPeriod(tipo: FinancialType, startUtc: Long, endUtc: Long): Double

    @Query(
        """
        SELECT COALESCE(SUM(valor), 0) FROM financial_entries WHERE tipo = :tipo
        """
    )
    suspend fun sumByType(tipo: FinancialType): Double

    @Query("SELECT * FROM financial_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FinancialEntry?

    @Query(
        "SELECT * FROM financial_entries WHERE dataUtc >= :startUtc AND dataUtc <= :endUtc ORDER BY dataUtc DESC"
    )
    suspend fun listInPeriod(startUtc: Long, endUtc: Long): List<FinancialEntry>

    @Insert
    suspend fun insert(entry: FinancialEntry): Long

    @Update
    suspend fun update(entry: FinancialEntry)

    @Delete
    suspend fun delete(entry: FinancialEntry)
}
