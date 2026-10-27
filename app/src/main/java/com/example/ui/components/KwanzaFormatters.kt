package com.example.ui.components

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object KwanzaFormatters {

    fun formatAOA(amount: Double, showDecimals: Boolean = false): String {
        val symbols = java.text.DecimalFormatSymbols(Locale.forLanguageTag("pt-AO")).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        val pattern = if (showDecimals) "#,##0.00" else "#,##0"
        val formatter = java.text.DecimalFormat(pattern, symbols)
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

    fun formatPrimeiroEUltimoNome(fullName: String): String {
        val trimmed = fullName.trim()
        if (trimmed.isEmpty()) return ""
        val parts = trimmed.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        return if (parts.size <= 1) {
            parts.firstOrNull() ?: ""
        } else {
            "${parts.first()} ${parts.last()}"
        }
    }
}
