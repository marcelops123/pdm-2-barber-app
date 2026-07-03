package com.example.pdm2_project.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ClientDao {

    @Query("SELECT * FROM clients ORDER BY nome COLLATE NOCASE")
    suspend fun getAll(): List<Client>

    @Query("SELECT COUNT(*) FROM clients")
    suspend fun count(): Long

    @Query("SELECT COUNT(*) FROM clients WHERE cadastroUtc >= :startUtc AND cadastroUtc <= :endUtc")
    suspend fun countInPeriod(startUtc: Long, endUtc: Long): Long

    @Query(
        "SELECT * FROM clients WHERE cadastroUtc >= :startUtc AND cadastroUtc <= :endUtc ORDER BY nome COLLATE NOCASE"
    )
    suspend fun listInPeriod(startUtc: Long, endUtc: Long): List<Client>

    @Query("SELECT * FROM clients WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Client?

    @Query("SELECT * FROM clients WHERE userId = :userId LIMIT 1")
    suspend fun getByUserId(userId: Long): Client?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(client: Client): Long

    @Update
    suspend fun update(client: Client)

    @Delete
    suspend fun delete(client: Client)
}
