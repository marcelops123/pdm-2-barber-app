package com.example.pdm2_project

import java.util.Calendar
import java.util.Locale

object DashboardHelper {

    fun monthRange(monthsAgo: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.MONTH, -monthsAgo)
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.MILLISECOND, -1)
        val end = cal.timeInMillis
        return start to end
    }

    fun currentMonthRange(): Pair<Long, Long> = monthRange(0)

    fun previousMonthRange(): Pair<Long, Long> = monthRange(1)

    fun growthPercent(current: Long, previous: Long): String {
        if (previous == 0L) return if (current > 0) "+100%" else "—"
        val pct = ((current - previous).toDouble() / previous.toDouble()) * 100
        val sign = if (pct >= 0) "+" else ""
        return String.format(Locale.getDefault(), "%s%.0f%% este mês", sign, pct)
    }

    fun monthLabels(count: Int): List<Pair<String, String>> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -(count - 1))
        return (0 until count).map {
            val year = cal.get(Calendar.YEAR).toString()
            val month = String.format(Locale.getDefault(), "%02d", cal.get(Calendar.MONTH) + 1)
            val label = String.format(
                Locale.getDefault(),
                "%s/%s",
                month,
                year.takeLast(2)
            )
            val pair = year to month
            cal.add(Calendar.MONTH, 1)
            label to pair.let { "${it.first}|${it.second}" }
        }.map { (label, key) ->
            val parts = key.split("|")
            label to parts[1]
        }.mapIndexed { index, pair ->
            val cal2 = Calendar.getInstance()
            cal2.add(Calendar.MONTH, -(count - 1 - index))
            val year = cal2.get(Calendar.YEAR).toString()
            val month = String.format(Locale.getDefault(), "%02d", cal2.get(Calendar.MONTH) + 1)
            val label = String.format(Locale.getDefault(), "%s/%s", month, year.takeLast(2))
            label to month
        }
    }

    fun monthYearPairs(count: Int): List<Triple<String, String, String>> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -(count - 1))
        return (0 until count).map {
            val year = cal.get(Calendar.YEAR).toString()
            val month = String.format(Locale.getDefault(), "%02d", cal.get(Calendar.MONTH) + 1)
            val label = String.format(Locale.getDefault(), "%s/%s", month, year.takeLast(2))
            val triple = Triple(label, year, month)
            cal.add(Calendar.MONTH, 1)
            triple
        }
    }

    fun weekStartUtc(weeksAgo: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.WEEK_OF_YEAR, -weeksAgo)
        return cal.timeInMillis
    }

    fun weekEndUtc(weekStart: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = weekStart
        cal.add(Calendar.DAY_OF_YEAR, 6)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }
}
