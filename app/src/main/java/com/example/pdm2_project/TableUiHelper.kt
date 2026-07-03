package com.example.pdm2_project

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import com.google.android.material.R as MR
import com.google.android.material.color.MaterialColors

fun Context.dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

fun Context.tableTextCell(
    text: String,
    bold: Boolean
): TextView = TextView(this).apply {
    this.text = text
    if (bold) {
        setTypeface(null, Typeface.BOLD)
    }
    val pad = dpToPx(12)
    setPadding(pad, dpToPx(10), pad, dpToPx(10))
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
    setTextColor(
        MaterialColors.getColor(
            this@tableTextCell,
            MR.attr.colorOnSurface,
            0xFF000000.toInt()
        )
    )
    minWidth = dpToPx(96)
    maxWidth = dpToPx(280)
    gravity = Gravity.CENTER_VERTICAL
}

fun Context.wrapRow(vararg views: android.view.View, onRowClick: (() -> Unit)? = null): TableRow {
    val row = TableRow(this)
    val lp = TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    views.forEach { row.addView(it, lp) }
    onRowClick?.let { act ->
        row.setOnClickListener { act() }
    }
    row.setBackgroundResource(android.R.drawable.list_selector_background)
    return row
}

fun Context.tableTwoLineCell(line1: String, line2: String, bold: Boolean): LinearLayout {
    return LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        val pad = dpToPx(12)
        setPadding(pad, dpToPx(10), pad, dpToPx(10))
        minimumWidth = dpToPx(96)
        addView(TextView(this@tableTwoLineCell).apply {
            text = line1
            if (bold) setTypeface(null, Typeface.BOLD)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, if (bold) 14f else 14f)
            setTextColor(MaterialColors.getColor(this@tableTwoLineCell, MR.attr.colorOnSurface, 0xFF000000.toInt()))
        })
        if (line2.isNotEmpty()) {
            addView(TextView(this@tableTwoLineCell).apply {
                text = line2
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTextColor(MaterialColors.getColor(this@tableTwoLineCell, MR.attr.colorOnSurface, 0xFF666666.toInt()))
            })
        }
    }
}

fun splitName(nome: String): Pair<String, String> {
    val parts = nome.trim().split("\\s+".toRegex(), limit = 2)
    return when {
        parts.size >= 2 -> parts[0] to parts[1]
        parts.isNotEmpty() -> parts[0] to ""
        else -> "—" to ""
    }
}
