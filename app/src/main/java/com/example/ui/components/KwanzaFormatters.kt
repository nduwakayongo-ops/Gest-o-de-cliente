package com.example.ui.components

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object KwanzaFormatters {

    fun formatAOA(amount: Double, showDecimals: Boolean = false): String {
        val formatter = NumberFormat.getInstance(Locale.forLanguageTag("pt-AO")).apply {
            minimumFractionDigits = if (showDecimals) 2 else 0
            maximumFractionDigits = 2
        }
        return "AOA ${formatter.format(amount)}"
    }

    fun formatDate(timestamp: Long): String {
        if (timestamp <= 0) return "-"
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-AO"))
        return sdf.format(Date(timestamp))
    }

    fun calculateDaysUntilOrOverdue(vencimentoTimestamp: Long): Int {
        if (vencimentoTimestamp <= 0) return 0
        val now = System.currentTimeMillis()
        val diffMs = vencimentoTimestamp - now
        return (diffMs / (24 * 3600 * 1000)).toInt()
    }
}
