package com.example.pdm2_project

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DateFormats {

    private val dateTimeBr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val dateBr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val dateIso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeHm = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun formatDateTime(utcMillis: Long): String = dateTimeBr.format(utcMillis)

    fun formatDate(utcMillis: Long): String = dateBr.format(utcMillis)

    fun formatDateIso(utcMillis: Long): String = dateIso.format(utcMillis)

    fun formatTime(utcMillis: Long): String = timeHm.format(utcMillis)

    fun startOfDayUtc(cal: Calendar): Long {
        val c = cal.clone() as Calendar
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    fun endOfDayUtc(cal: Calendar): Long {
        val c = cal.clone() as Calendar
        c.set(Calendar.HOUR_OF_DAY, 23)
        c.set(Calendar.MINUTE, 59)
        c.set(Calendar.SECOND, 59)
        c.set(Calendar.MILLISECOND, 999)
        return c.timeInMillis
    }
}
