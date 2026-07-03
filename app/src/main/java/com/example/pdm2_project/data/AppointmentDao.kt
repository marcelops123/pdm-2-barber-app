package com.example.pdm2_project.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

data class AppointmentListItem(
    val id: Long,
    val clientId: Long,
    val dataHoraUtc: Long,
    val servico: String,
    val observacao: String,
    val valor: Double?,
    val clientNome: String,
    val barbeiro: String
)

data class MonthCount(
    val year: Int,
    val month: Int,
    val count: Int
)

@Dao
interface AppointmentDao {

    @Query("SELECT COUNT(*) FROM appointments")
    suspend fun count(): Long

    @Query("SELECT COUNT(*) FROM appointments WHERE dataHoraUtc >= :startUtc AND dataHoraUtc <= :endUtc")
    suspend fun countInPeriod(startUtc: Long, endUtc: Long): Long

    @Query("SELECT COUNT(*) FROM appointments WHERE clientId = :clientId")
    suspend fun countByClientId(clientId: Long): Int

    @Query(
        """
        SELECT COUNT(*) FROM appointments
        WHERE strftime('%Y', datetime(dataHoraUtc/1000, 'unixepoch')) = :year
        AND strftime('%m', datetime(dataHoraUtc/1000, 'unixepoch')) = :month
        """
    )
    suspend fun countByYearMonth(year: String, month: String): Int

    @Query(
        """
        SELECT a.id AS id, a.clientId AS clientId, a.dataHoraUtc AS dataHoraUtc,
               a.servico AS servico, a.observacao AS observacao, a.valor AS valor,
               c.nome AS clientNome, a.barbeiro AS barbeiro
        FROM appointments a
        INNER JOIN clients c ON c.id = a.clientId
        ORDER BY a.dataHoraUtc DESC
        """
    )
    suspend fun listAllWithClient(): List<AppointmentListItem>

    @Query(
        """
        SELECT a.id AS id, a.clientId AS clientId, a.dataHoraUtc AS dataHoraUtc,
               a.servico AS servico, a.observacao AS observacao, a.valor AS valor,
               c.nome AS clientNome, a.barbeiro AS barbeiro
        FROM appointments a
        INNER JOIN clients c ON c.id = a.clientId
        WHERE a.clientId = :clientId
        ORDER BY a.dataHoraUtc DESC
        """
    )
    suspend fun listByClientId(clientId: Long): List<AppointmentListItem>

    @Query(
        """
        SELECT a.id AS id, a.clientId AS clientId, a.dataHoraUtc AS dataHoraUtc,
               a.servico AS servico, a.observacao AS observacao, a.valor AS valor,
               c.nome AS clientNome, a.barbeiro AS barbeiro
        FROM appointments a
        INNER JOIN clients c ON c.id = a.clientId
        WHERE a.clientId IN (:clientIds)
        AND a.dataHoraUtc >= :startUtc AND a.dataHoraUtc <= :endUtc
        ORDER BY a.dataHoraUtc DESC
        """
    )
    suspend fun listForClientsInPeriod(
        clientIds: List<Long>,
        startUtc: Long,
        endUtc: Long
    ): List<AppointmentListItem>

    @Query(
        """
        SELECT a.id AS id, a.clientId AS clientId, a.dataHoraUtc AS dataHoraUtc,
               a.servico AS servico, a.observacao AS observacao, a.valor AS valor,
               c.nome AS clientNome, a.barbeiro AS barbeiro
        FROM appointments a
        INNER JOIN clients c ON c.id = a.clientId
        WHERE a.dataHoraUtc >= :startUtc AND a.dataHoraUtc <= :endUtc
        ORDER BY a.dataHoraUtc DESC
        """
    )
    suspend fun listInPeriod(startUtc: Long, endUtc: Long): List<AppointmentListItem>

    @Query("SELECT * FROM appointments WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Appointment?

    @Insert
    suspend fun insert(appointment: Appointment): Long

    @Update
    suspend fun update(appointment: Appointment)

    @Delete
    suspend fun delete(appointment: Appointment)
}
