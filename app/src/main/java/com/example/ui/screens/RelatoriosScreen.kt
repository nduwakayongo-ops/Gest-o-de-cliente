package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClienteEntity
import com.example.data.model.CreditoEntity
import com.example.data.model.PagamentoEntity
import com.example.repository.DashboardSummary
import com.example.ui.components.KwanzaFormatters
import com.example.ui.theme.*
import com.example.util.PdfGeneratorHelper
import com.example.util.TipoRelatorioPersonalizado
import java.text.SimpleDateFormat
import java.util.*

enum class PresetPeriodo(val label: String) {
    ESTE_MES("Este Mês (Mês Atual)"),
    MES_ANTERIOR("Mês Anterior"),
    SEMESTRE_EXEMPLO("01/06 a 31/12/2026"),
    ESTE_ANO("Ano Atual (2026)"),
    TODOS("Histórico Geral (Todos)"),
    PERSONALIZADO("Personalizado...")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelatoriosScreen(
    summary: DashboardSummary,
    clientesWithCredito: List<Pair<ClienteEntity, CreditoEntity?>>,
    pagamentos: List<PagamentoEntity> = emptyList()
) {
    val context = LocalContext.current

    var selectedTipoRelatorio by remember { mutableStateOf(TipoRelatorioPersonalizado.GERAL) }
    var selectedPreset by remember { mutableStateOf(PresetPeriodo.SEMESTRE_EXEMPLO) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("Todos") }

    // Start Date & End Date timestamps (Defaults to 01/06/2026 to 31/12/2026)
    val defaultStartCal = Calendar.getInstance().apply {
        set(2026, Calendar.JUNE, 1, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val defaultEndCal = Calendar.getInstance().apply {
        set(2026, Calendar.DECEMBER, 31, 23, 59, 59)
        set(Calendar.MILLISECOND, 999)
    }

    var dataInicioTimestamp by remember { mutableLongStateOf(defaultStartCal.timeInMillis) }
    var dataFimTimestamp by remember { mutableLongStateOf(defaultEndCal.timeInMillis) }
    var showCustomDateDialog by remember { mutableStateOf(false) }

    fun setPreset(preset: PresetPeriodo) {
        selectedPreset = preset
        when (preset) {
            PresetPeriodo.TODOS -> {
                dataInicioTimestamp = 0L
                dataFimTimestamp = Long.MAX_VALUE
            }
            PresetPeriodo.ESTE_MES -> {
                val start = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val end = (start.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                dataInicioTimestamp = start.timeInMillis
                dataFimTimestamp = end.timeInMillis
            }
            PresetPeriodo.MES_ANTERIOR -> {
                val start = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    add(Calendar.MONTH, -1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val end = (start.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                dataInicioTimestamp = start.timeInMillis
                dataFimTimestamp = end.timeInMillis
            }
            PresetPeriodo.SEMESTRE_EXEMPLO -> {
                val start = Calendar.getInstance().apply {
                    set(2026, Calendar.JUNE, 1, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val end = Calendar.getInstance().apply {
                    set(2026, Calendar.DECEMBER, 31, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                dataInicioTimestamp = start.timeInMillis
                dataFimTimestamp = end.timeInMillis
            }
            PresetPeriodo.ESTE_ANO -> {
                val start = Calendar.getInstance().apply {
                    set(Calendar.MONTH, Calendar.JANUARY)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val end = Calendar.getInstance().apply {
                    set(Calendar.MONTH, Calendar.DECEMBER)
                    set(Calendar.DAY_OF_MONTH, 31)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                dataInicioTimestamp = start.timeInMillis
                dataFimTimestamp = end.timeInMillis
            }
            PresetPeriodo.PERSONALIZADO -> {
                showCustomDateDialog = true
            }
        }
    }

    val periodoDescricao = remember(selectedPreset, dataInicioTimestamp, dataFimTimestamp) {
        if (selectedPreset == PresetPeriodo.TODOS) {
            "Histórico Geral (Todos os Contratos)"
        } else {
            val startStr = KwanzaFormatters.formatDate(dataInicioTimestamp)
            val endStr = KwanzaFormatters.formatDate(dataFimTimestamp)
            "$startStr a $endStr"
        }
    }

    // Filter credits & clients belonging to period
    val clientesFiltradosPeriodo = remember(clientesWithCredito, dataInicioTimestamp, dataFimTimestamp, searchQuery, selectedStatusFilter) {
        clientesWithCredito.filter { (cliente, credito) ->
            val matchSearch = if (searchQuery.isBlank()) true else {
                cliente.nome.contains(searchQuery, ignoreCase = true) ||
                        cliente.telefone.contains(searchQuery) ||
                        cliente.banco.contains(searchQuery, ignoreCase = true) ||
                        cliente.numeroInterno.contains(searchQuery, ignoreCase = true)
            }

            val matchStatus = when (selectedStatusFilter) {
                "Todos" -> true
                "Ativo" -> credito?.estado == "Ativo"
                "Em atraso" -> credito?.estado == "Em atraso" || (credito?.diasEmAtraso ?: 0) > 0
                "Liquidado" -> credito?.estado == "Liquidado"
                else -> true
            }

            val matchPeriod = if (dataInicioTimestamp == 0L && dataFimTimestamp == Long.MAX_VALUE) {
                true
            } else {
                if (credito == null) {
                    cliente.dataCriacao in dataInicioTimestamp..dataFimTimestamp
                } else {
                    val inDisbursement = credito.dataDesembolso in dataInicioTimestamp..dataFimTimestamp
                    val inDue = credito.proximoVencimento in dataInicioTimestamp..dataFimTimestamp
                    val activeDuring = credito.dataDesembolso <= dataFimTimestamp && (credito.dataUltimoPagamento >= dataInicioTimestamp || credito.estado != "Liquidado")
                    inDisbursement || inDue || activeDuring
                }
            }

            matchSearch && matchStatus && matchPeriod
        }
    }

    // Filter payments in period
    val pagamentosPeriodo = remember(pagamentos, dataInicioTimestamp, dataFimTimestamp) {
        pagamentos.filter {
            if (dataInicioTimestamp == 0L && dataFimTimestamp == Long.MAX_VALUE) true
            else it.dataPagamento in dataInicioTimestamp..dataFimTimestamp
        }
    }

    // Dynamic Period Summary calculations based strictly on Mbuli Portfolio Gold Rule:
    // Capital Amortizado = Fraction of payments that amortize principal
    // Juros Recebidos = Revenue from interest (does NOT reduce loan portfolio)
    // Saldo da Carteira = Capital Desembolsado - Capital Amortizado
    val totalDesembolsadoPeriodo = remember(clientesFiltradosPeriodo) {
        clientesFiltradosPeriodo.sumOf { it.second?.valorDesembolsado ?: 0.0 }
    }
    val capitalAmortizadoPeriodo = remember(clientesFiltradosPeriodo) {
        clientesFiltradosPeriodo.sumOf { (_, credito) ->
            if (credito != null && credito.quantidadePrestacoes > 0) {
                val pagas = credito.prestacoesPagas.coerceIn(0, credito.quantidadePrestacoes)
                (credito.valorDesembolsado / credito.quantidadePrestacoes) * pagas
            } else 0.0
        }
    }
    val jurosRecebidosPeriodo = remember(clientesFiltradosPeriodo) {
        clientesFiltradosPeriodo.sumOf { (_, credito) ->
            if (credito != null && credito.quantidadePrestacoes > 0) {
                val pagas = credito.prestacoesPagas.coerceIn(0, credito.quantidadePrestacoes)
                (credito.valorDesembolsado * (credito.taxaPercentual / 100.0)) * pagas
            } else 0.0
        }
    }
    val totalRecuperadoPeriodo = remember(capitalAmortizadoPeriodo, jurosRecebidosPeriodo, pagamentosPeriodo) {
        val somaPags = pagamentosPeriodo.sumOf { it.valorPago }
        if (somaPags > 0.0) somaPags else (capitalAmortizadoPeriodo + jurosRecebidosPeriodo)
    }
    val saldoCarteiraPeriodo = remember(totalDesembolsadoPeriodo, capitalAmortizadoPeriodo) {
        (totalDesembolsadoPeriodo - capitalAmortizadoPeriodo).coerceAtLeast(0.0)
    }
    val totalEmAtrasoPeriodo = remember(clientesFiltradosPeriodo) {
        clientesFiltradosPeriodo.filter { it.second?.estado == "Em atraso" || (it.second?.diasEmAtraso ?: 0) > 0 }
            .sumOf { it.second?.valorEmAtraso ?: 0.0 }
    }

    val periodSummary = remember(summary, totalDesembolsadoPeriodo, capitalAmortizadoPeriodo, jurosRecebidosPeriodo, totalRecuperadoPeriodo, saldoCarteiraPeriodo, totalEmAtrasoPeriodo, clientesFiltradosPeriodo) {
        val countAtivos = clientesFiltradosPeriodo.count { it.second?.estado == "Ativo" || it.second?.estado == "Em atraso" }
        val countLiquidados = clientesFiltradosPeriodo.count { it.second?.estado == "Liquidado" }
        val countAtraso = clientesFiltradosPeriodo.count { it.second?.estado == "Em atraso" || (it.second?.diasEmAtraso ?: 0) > 0 }
        val risco = if (saldoCarteiraPeriodo > 0) (totalEmAtrasoPeriodo / saldoCarteiraPeriodo) * 100 else 0.0

        DashboardSummary(
            carteiraAtiva = saldoCarteiraPeriodo,
            totalClientes = clientesFiltradosPeriodo.size,
            clientesAtivosCount = countAtivos,
            clientesLiquidadosCount = countLiquidados,
            clientesEmAtrasoCount = countAtraso,
            totalDesembolsado = totalDesembolsadoPeriodo,
            capitalAmortizado = capitalAmortizadoPeriodo,
            jurosRecebidos = jurosRecebidosPeriodo,
            totalRecebido = totalRecuperadoPeriodo,
            totalRecuperado = totalRecuperadoPeriodo,
            valorEmAtraso = totalEmAtrasoPeriodo,
            riscoPercentual = risco,
            prestacoesVencendoCount = summary.prestacoesVencendoCount,
            notificacoesCount = summary.notificacoesCount
        )
    }

    val countTodosClientes = remember(clientesWithCredito) { clientesWithCredito.size }
    val countCarteiraAtiva = remember(clientesFiltradosPeriodo) {
        clientesFiltradosPeriodo.count { it.second != null && it.second?.estado != "Liquidado" }
    }
    val countInadimplentes = remember(clientesFiltradosPeriodo, dataInicioTimestamp, dataFimTimestamp) {
        clientesFiltradosPeriodo.count { (_, crd) ->
            crd != null && crd.estado != "Liquidado" &&
            (crd.estado == "Em atraso" || crd.diasEmAtraso > 0 ||
             (if (dataInicioTimestamp > 0L || dataFimTimestamp < Long.MAX_VALUE) crd.proximoVencimento in dataInicioTimestamp..dataFimTimestamp else true))
        }
    }
    val countReembolsos = remember(pagamentosPeriodo) { pagamentosPeriodo.size }
    val countDesembolsos = remember(clientesFiltradosPeriodo, dataInicioTimestamp, dataFimTimestamp) {
        clientesFiltradosPeriodo.count {
            it.second != null && (if (dataInicioTimestamp > 0L || dataFimTimestamp < Long.MAX_VALUE) it.second!!.dataDesembolso in dataInicioTimestamp..dataFimTimestamp else true)
        }
    }
    val countGeral = remember(clientesFiltradosPeriodo) { clientesFiltradosPeriodo.size }

    val listaCarteiraAtiva = remember(clientesFiltradosPeriodo) {
        clientesFiltradosPeriodo.filter { it.second != null && it.second?.estado != "Liquidado" }
    }
    val listaInadimplentes = remember(clientesFiltradosPeriodo, dataInicioTimestamp, dataFimTimestamp) {
        clientesFiltradosPeriodo.filter { (_, crd) ->
            crd != null && crd.estado != "Liquidado" &&
            (crd.estado == "Em atraso" || crd.diasEmAtraso > 0 ||
             (if (dataInicioTimestamp > 0L || dataFimTimestamp < Long.MAX_VALUE) crd.proximoVencimento in dataInicioTimestamp..dataFimTimestamp else true))
        }
    }
    val listaDesembolsos = remember(clientesFiltradosPeriodo, dataInicioTimestamp, dataFimTimestamp) {
        clientesFiltradosPeriodo.filter {
            it.second != null && (if (dataInicioTimestamp > 0L || dataFimTimestamp < Long.MAX_VALUE) it.second!!.dataDesembolso in dataInicioTimestamp..dataFimTimestamp else true)
        }
    }

    val activeItemsCount = remember(selectedTipoRelatorio, countTodosClientes, countCarteiraAtiva, countInadimplentes, countReembolsos, countDesembolsos, countGeral) {
        when (selectedTipoRelatorio) {
            TipoRelatorioPersonalizado.TODOS_CLIENTES -> countTodosClientes
            TipoRelatorioPersonalizado.CARTEIRA_ATIVA -> countCarteiraAtiva
            TipoRelatorioPersonalizado.INADIMPLENTES_PENDENCIAS -> countInadimplentes
            TipoRelatorioPersonalizado.APENAS_REEMBOLSOS -> countReembolsos
            TipoRelatorioPersonalizado.APENAS_DESEMBOLSOS -> countDesembolsos
            TipoRelatorioPersonalizado.GERAL -> countGeral
        }
    }

    val relatorioTexto = remember(selectedTipoRelatorio, periodoDescricao, periodSummary, clientesFiltradosPeriodo, pagamentosPeriodo, listaCarteiraAtiva, listaInadimplentes, listaDesembolsos) {
        buildString {
            appendLine("============================================")
            appendLine(selectedTipoRelatorio.tituloCompleto)
            appendLine("           Nduwa Kayongo — Crédito")
            appendLine("============================================")
            appendLine("Período Selecionado: $periodoDescricao")
            when (selectedTipoRelatorio) {
                TipoRelatorioPersonalizado.TODOS_CLIENTES -> {
                    appendLine("Total de Clientes Cadastrados: ${clientesWithCredito.size}")
                    appendLine("Total Desembolsado Histórico: ${KwanzaFormatters.formatAOA(clientesWithCredito.sumOf { it.second?.valorDesembolsado ?: 0.0 })}")
                    appendLine("--------------------------------------------")
                    clientesFiltradosPeriodo.forEachIndexed { idx, (cli, crd) ->
                        appendLine("${idx + 1}. ${KwanzaFormatters.formatPrimeiroEUltimoNome(cli.nome)}")
                        appendLine("   Tel: ${cli.telefone} | Banco: ${cli.banco} (${cli.numeroConta})")
                        appendLine("   Capital: ${KwanzaFormatters.formatAOA(crd?.valorDesembolsado ?: 0.0)} | Estado: ${crd?.estado ?: "Sem crédito"}")
                    }
                }
                TipoRelatorioPersonalizado.CARTEIRA_ATIVA -> {
                    appendLine("Contratos Ativos: ${listaCarteiraAtiva.size}")
                    appendLine("Capital Desembolsado Ativo: ${KwanzaFormatters.formatAOA(totalDesembolsadoPeriodo)}")
                    appendLine("Capital Amortizado: ${KwanzaFormatters.formatAOA(capitalAmortizadoPeriodo)}")
                    appendLine("SALDO DA CARTEIRA (EM DÍVIDA): ${KwanzaFormatters.formatAOA(saldoCarteiraPeriodo)}")
                    appendLine("REGRA DE OURO: Saldo = Capital Desembolsado - Capital Amortizado. Juros não reduzem a carteira.")
                    appendLine("--------------------------------------------")
                    listaCarteiraAtiva.forEachIndexed { idx, (cli, crd) ->
                        val desemb = crd?.valorDesembolsado ?: 0.0
                        val pagas = crd?.prestacoesPagas?.coerceIn(0, crd.quantidadePrestacoes) ?: 0
                        val amort = if (crd != null && crd.quantidadePrestacoes > 0) (crd.valorDesembolsado / crd.quantidadePrestacoes) * pagas else 0.0
                        val saldo = (desemb - amort).coerceAtLeast(0.0)
                        appendLine("${idx + 1}. ${KwanzaFormatters.formatPrimeiroEUltimoNome(cli.nome)} (${cli.banco})")
                        appendLine("   Desemb: ${KwanzaFormatters.formatAOA(desemb)} | Amort: ${KwanzaFormatters.formatAOA(amort)}")
                        appendLine("   Saldo em Dívida: ${KwanzaFormatters.formatAOA(saldo)} | Prestação: ${KwanzaFormatters.formatAOA(crd?.prestacaoMensal ?: 0.0)}")
                    }
                }
                TipoRelatorioPersonalizado.INADIMPLENTES_PENDENCIAS -> {
                    appendLine("Clientes com Pendência no Período: ${listaInadimplentes.size}")
                    appendLine("Total em Atraso: ${KwanzaFormatters.formatAOA(periodSummary.valorEmAtraso)}")
                    appendLine("Multas de Mora: 4%/dia sobre o total dos juros contratuais")
                    appendLine("--------------------------------------------")
                    listaInadimplentes.forEachIndexed { idx, (cli, crd) ->
                        val dias = crd?.diasEmAtraso ?: 0
                        val diasCalc = if (dias > 0) dias else 1
                        val pagamentosCliente = pagamentosPeriodo.filter { it.creditoId == crd?.id }
                        val moraResultado = if (crd != null) {
                            com.example.util.MoraCalculatorHelper.calcular(crd, pagamentosCliente, diasCalc)
                        } else null
                        val prest = crd?.prestacaoMensal ?: 0.0
                        val mora = moraResultado?.jurosMoraAcumulados ?: (prest * 0.04 * diasCalc)
                        val total = moraResultado?.valorTotalEmDivida ?: (prest + mora)
                        appendLine("${idx + 1}. ${KwanzaFormatters.formatPrimeiroEUltimoNome(cli.nome)} (${cli.telefone})")
                        appendLine("   Atraso: ${dias}d | Prestação: ${KwanzaFormatters.formatAOA(prest)}")
                        appendLine("   Mora (4%/d juros contratuais): ${KwanzaFormatters.formatAOA(mora)} | Total a Pagar: ${KwanzaFormatters.formatAOA(total)}")
                    }
                }
                TipoRelatorioPersonalizado.APENAS_REEMBOLSOS -> {
                    val totalCap = pagamentosPeriodo.sumOf { pag ->
                        val crd = clientesWithCredito.find { it.second?.id == pag.creditoId }?.second
                        if (crd != null && crd.quantidadePrestacoes > 0) crd.valorDesembolsado / crd.quantidadePrestacoes else pag.valorPago * 0.5263
                    }
                    val totalJur = (pagamentosPeriodo.sumOf { it.valorPago } - totalCap).coerceAtLeast(0.0)
                    appendLine("Total de Reembolsos no Período: ${pagamentosPeriodo.size}")
                    appendLine("Capital Amortizado (Reduz Carteira): ${KwanzaFormatters.formatAOA(totalCap)}")
                    appendLine("Juros Recebidos (Receita Mbuli): ${KwanzaFormatters.formatAOA(totalJur)}")
                    appendLine("Total Geral Pago: ${KwanzaFormatters.formatAOA(pagamentosPeriodo.sumOf { it.valorPago })}")
                    appendLine("REGRA: Apenas o Capital Amortizado reduz a carteira. Juros são receita.")
                    appendLine("--------------------------------------------")
                    pagamentosPeriodo.forEachIndexed { idx, pag ->
                        val cli = clientesWithCredito.find { it.first.id == pag.clienteId }?.first
                        val crd = clientesWithCredito.find { it.second?.id == pag.creditoId }?.second
                        val cap = if (crd != null && crd.quantidadePrestacoes > 0) crd.valorDesembolsado / crd.quantidadePrestacoes else pag.valorPago * 0.5263
                        val jur = (pag.valorPago - cap).coerceAtLeast(0.0)
                        appendLine("${idx + 1}. ${KwanzaFormatters.formatDate(pag.dataPagamento)} — ${KwanzaFormatters.formatPrimeiroEUltimoNome(cli?.nome ?: "Cliente #${pag.clienteId}")}")
                        appendLine("   Prestação nº ${pag.numeroPrestacao} | Cap: ${KwanzaFormatters.formatAOA(cap)} | Jur: ${KwanzaFormatters.formatAOA(jur)} | Total: ${KwanzaFormatters.formatAOA(pag.valorPago)}")
                    }
                }
                TipoRelatorioPersonalizado.APENAS_DESEMBOLSOS -> {
                    appendLine("Novos Financiamentos no Período: ${listaDesembolsos.size}")
                    appendLine("Total Capital Desembolsado: ${KwanzaFormatters.formatAOA(listaDesembolsos.sumOf { it.second?.valorDesembolsado ?: 0.0 })}")
                    appendLine("REGRA: Desembolso aumenta a carteira apenas pelo capital concedido.")
                    appendLine("--------------------------------------------")
                    listaDesembolsos.forEachIndexed { idx, (cli, crd) ->
                        appendLine("${idx + 1}. ${KwanzaFormatters.formatPrimeiroEUltimoNome(cli.nome)} (${cli.banco})")
                        appendLine("   Data: ${KwanzaFormatters.formatDate(crd?.dataDesembolso ?: 0L)} | Capital: ${KwanzaFormatters.formatAOA(crd?.valorDesembolsado ?: 0.0)}")
                        appendLine("   Taxa: ${crd?.taxaPercentual}% | Prazo: ${crd?.quantidadePrestacoes}m | Prestação: ${KwanzaFormatters.formatAOA(crd?.prestacaoMensal ?: 0.0)}")
                    }
                }
                TipoRelatorioPersonalizado.GERAL -> {
                    appendLine("Total Desembolsado: ${KwanzaFormatters.formatAOA(periodSummary.totalDesembolsado)}")
                    appendLine("Capital Amortizado: ${KwanzaFormatters.formatAOA(periodSummary.capitalAmortizado)}")
                    appendLine("Juros Recebidos (Receita): ${KwanzaFormatters.formatAOA(periodSummary.jurosRecebidos)}")
                    appendLine("Total Recebido (Cap + Jur): ${KwanzaFormatters.formatAOA(periodSummary.totalRecebido)}")
                    appendLine("Saldo da Carteira (Dívida): ${KwanzaFormatters.formatAOA(periodSummary.carteiraAtiva)}")
                    appendLine("Valor em Atraso: ${KwanzaFormatters.formatAOA(periodSummary.valorEmAtraso)}")
                    appendLine("Taxa de Risco: ${String.format(Locale.US, "%.2f", periodSummary.riscoPercentual)}%")
                    appendLine("--------------------------------------------")
                    clientesFiltradosPeriodo.forEachIndexed { idx, (cliente, credito) ->
                        appendLine("${idx + 1}. ${KwanzaFormatters.formatPrimeiroEUltimoNome(cliente.nome)}")
                        appendLine("   Capital: ${KwanzaFormatters.formatAOA(credito?.valorDesembolsado ?: 0.0)} | Prestação: ${KwanzaFormatters.formatAOA(credito?.prestacaoMensal ?: 0.0)}")
                    }
                }
            }
            appendLine("--------------------------------------------")
            appendLine("Extrato Oficial gerado sem limite de páginas • Nduwa Kayongo")
        }
    }

    fun handleGerarPdf() {
        val pdfFile = com.example.util.PdfGeneratorHelper.generateRelatorioPersonalizadoPdf(
            context = context,
            tipo = selectedTipoRelatorio,
            clientesWithCredito = clientesFiltradosPeriodo,
            summary = periodSummary,
            periodoDescricao = periodoDescricao,
            dataInicio = dataInicioTimestamp,
            dataFim = dataFimTimestamp,
            pagamentos = pagamentosPeriodo
        )
        if (pdfFile != null) {
            Toast.makeText(context, "${selectedTipoRelatorio.tituloCurto} gerado com sucesso! Abrindo...", Toast.LENGTH_SHORT).show()
            com.example.util.PdfGeneratorHelper.openOrSharePdf(context, pdfFile)
        } else {
            Toast.makeText(context, "Erro ao gerar PDF do relatório.", Toast.LENGTH_SHORT).show()
        }
    }

    fun handlePartilharPdf() {
        val pdfFile = com.example.util.PdfGeneratorHelper.generateRelatorioPersonalizadoPdf(
            context = context,
            tipo = selectedTipoRelatorio,
            clientesWithCredito = clientesFiltradosPeriodo,
            summary = periodSummary,
            periodoDescricao = periodoDescricao,
            dataInicio = dataInicioTimestamp,
            dataFim = dataFimTimestamp,
            pagamentos = pagamentosPeriodo
        )
        if (pdfFile != null) {
            com.example.util.PdfGeneratorHelper.sharePdf(context, pdfFile)
        } else {
            Toast.makeText(context, "Erro ao gerar PDF para partilhar.", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleExportarPendenciasEmail() {
        val pdfFile = com.example.util.PdfGeneratorHelper.generateRelatorioPendenciasPdf(
            context = context,
            clientesWithCredito = clientesWithCredito,
            summary = summary
        )
        if (pdfFile != null) {
            val totalAtrasoStr = KwanzaFormatters.formatAOA(summary.valorEmAtraso)
            val body = """
                Prezada Gestão / Direção Mbuli-Microcrédito,

                Segue em anexo o Relatório Oficial de Clientes com Pendências Financeiras e Inadimplência gerado em ${KwanzaFormatters.formatDate(System.currentTimeMillis())}.

                Resumo:
                • Total de clientes com pendências: ${summary.clientesEmAtrasoCount}
                • Montante total em atraso: $totalAtrasoStr
                • Juros de mora aplicados: 4%/dia sobre as prestações vencidas

                Elaborado por: Nduwa Kayongo (Gestor de Carteira de Crédito)
                Mbuli-Microcrédito
            """.trimIndent()

            com.example.util.PdfGeneratorHelper.sharePdfViaEmail(
                context = context,
                file = pdfFile,
                subject = "Relatório de Pendências Financeiras e Inadimplência — Mbuli-Microcrédito",
                bodyText = body
            )
        } else {
            Toast.makeText(context, "Erro ao gerar PDF de pendências.", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleExportarPendenciasPdf() {
        val pdfFile = com.example.util.PdfGeneratorHelper.generateRelatorioPendenciasPdf(
            context = context,
            clientesWithCredito = clientesWithCredito,
            summary = summary
        )
        if (pdfFile != null) {
            Toast.makeText(context, "PDF de pendências gerado com sucesso!", Toast.LENGTH_SHORT).show()
            com.example.util.PdfGeneratorHelper.openOrSharePdf(context, pdfFile)
        } else {
            Toast.makeText(context, "Erro ao gerar PDF de pendências.", Toast.LENGTH_SHORT).show()
        }
    }

    // Master Scrollable Screen with LazyColumn: fluid up/down scrolling without clipping
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(GrayBackground)
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
    ) {
        // 1. HERO CARD: Prominent One-Click PDF Generation with Visual Appeal
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DeepBlue),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(DeepBlue, Color(0xFF16325C), DeepBlue)
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = AccentGold.copy(alpha = 0.2f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        val heroIcon = when (selectedTipoRelatorio) {
                                            TipoRelatorioPersonalizado.TODOS_CLIENTES -> Icons.Default.People
                                            TipoRelatorioPersonalizado.CARTEIRA_ATIVA -> Icons.Default.AccountBalanceWallet
                                            TipoRelatorioPersonalizado.INADIMPLENTES_PENDENCIAS -> Icons.Default.Warning
                                            TipoRelatorioPersonalizado.APENAS_REEMBOLSOS -> Icons.Default.ReceiptLong
                                            TipoRelatorioPersonalizado.APENAS_DESEMBOLSOS -> Icons.Default.Payments
                                            TipoRelatorioPersonalizado.GERAL -> Icons.Default.Assessment
                                        }
                                        Icon(
                                            heroIcon,
                                            contentDescription = null,
                                            tint = AccentGold,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        selectedTipoRelatorio.tituloCurto,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${periodoDescricao} • $activeItemsCount itens",
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Extrato de Carteira", relatorioTexto)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "${selectedTipoRelatorio.tituloCurto} copiado!", Toast.LENGTH_SHORT).show()
                                },
                                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.15f))
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar Texto", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }

                        // Big Prominent Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { handleGerarPdf() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ForestGreen,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(vertical = 14.dp, horizontal = 12.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("btn_gerar_pdf_relatorio")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "GERAR PDF AGORA",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            Button(
                                onClick = { handlePartilharPdf() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentGold,
                                    contentColor = DeepBlue
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(vertical = 14.dp, horizontal = 12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_compartilhar_relatorio")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = DeepBlue, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Partilhar",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepBlue
                                )
                            }
                        }
                    }
                }
            }
        }

        // 1.2 SELETOR DE RELATÓRIOS PERSONALIZADOS (1 A 6)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_seletor_relatorios_personalizados")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FilterList, contentDescription = null, tint = DeepBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Relatórios Personalizados",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepBlue
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AccentGold.copy(alpha = 0.2f)
                        ) {
                            Text(
                                "6 Tipos de Relatório",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepBlue,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        "Selecione o modelo pretendido para emitir o PDF oficial de acordo com a regra da carteira:",
                        fontSize = 11.sp,
                        color = GrayText
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TipoRelatorioPersonalizado.values().forEach { tipo ->
                            val isSelected = selectedTipoRelatorio == tipo
                            val badgeCount = when (tipo) {
                                TipoRelatorioPersonalizado.TODOS_CLIENTES -> "$countTodosClientes clientes"
                                TipoRelatorioPersonalizado.CARTEIRA_ATIVA -> "$countCarteiraAtiva ativos"
                                TipoRelatorioPersonalizado.INADIMPLENTES_PENDENCIAS -> "$countInadimplentes pendentes"
                                TipoRelatorioPersonalizado.APENAS_REEMBOLSOS -> "$countReembolsos reembolsos"
                                TipoRelatorioPersonalizado.APENAS_DESEMBOLSOS -> "$countDesembolsos desembolsos"
                                TipoRelatorioPersonalizado.GERAL -> "$countGeral consolidados"
                            }
                            val icon = when (tipo) {
                                TipoRelatorioPersonalizado.TODOS_CLIENTES -> Icons.Default.People
                                TipoRelatorioPersonalizado.CARTEIRA_ATIVA -> Icons.Default.AccountBalanceWallet
                                TipoRelatorioPersonalizado.INADIMPLENTES_PENDENCIAS -> Icons.Default.Warning
                                TipoRelatorioPersonalizado.APENAS_REEMBOLSOS -> Icons.Default.ReceiptLong
                                TipoRelatorioPersonalizado.APENAS_DESEMBOLSOS -> Icons.Default.Payments
                                TipoRelatorioPersonalizado.GERAL -> Icons.Default.Assessment
                            }
                            val iconColor = when (tipo) {
                                TipoRelatorioPersonalizado.INADIMPLENTES_PENDENCIAS -> if (isSelected) WarningRed else WarningRed.copy(alpha = 0.8f)
                                TipoRelatorioPersonalizado.CARTEIRA_ATIVA -> if (isSelected) ForestGreen else ForestGreen.copy(alpha = 0.8f)
                                TipoRelatorioPersonalizado.APENAS_REEMBOLSOS -> if (isSelected) DeepBlue else DeepBlue.copy(alpha = 0.8f)
                                TipoRelatorioPersonalizado.APENAS_DESEMBOLSOS -> if (isSelected) Color(0xFF1565C0) else Color(0xFF1565C0).copy(alpha = 0.8f)
                                else -> if (isSelected) DeepBlue else GrayText
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) DeepBlue.copy(alpha = 0.08f) else GrayBackground,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, DeepBlue) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedTipoRelatorio = tipo }
                                    .testTag("tipo_relatorio_${tipo.codigo}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) DeepBlue else Color.White),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                icon,
                                                contentDescription = null,
                                                tint = if (isSelected) AccentGold else iconColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    tipo.tituloCurto,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                    color = if (isSelected) DeepBlue else DarkText
                                                )
                                                if (isSelected) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Icon(
                                                        Icons.Default.CheckCircle,
                                                        contentDescription = "Selecionado",
                                                        tint = ForestGreen,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                tipo.descricao,
                                                fontSize = 10.5.sp,
                                                color = GrayText,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSelected) DeepBlue else Color.White
                                    ) {
                                        Text(
                                            badgeCount,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else DeepBlue,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1.5 DEDICATED CARD: Relatório de Pendências Financeiras e Envio por E-mail
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().testTag("card_relatorio_pendencias_email")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(WarningRedLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = WarningRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Relatório de Clientes com Pendências",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepBlue
                                )
                                Text(
                                    text = "Inadimplência, juros de 4%/dia e envio por E-mail",
                                    fontSize = 11.sp,
                                    color = GrayText
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = WarningRedLight
                        ) {
                            Text(
                                text = "${summary.clientesEmAtrasoCount} com Atraso",
                                color = WarningRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Mini Stats summary
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFFF8F8))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total em Atraso", fontSize = 10.sp, color = GrayText)
                            Text(
                                KwanzaFormatters.formatAOA(summary.valorEmAtraso),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarningRed
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Multas de Mora", fontSize = 10.sp, color = GrayText)
                            Text("4% ao dia / prestação", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                        }
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { handleExportarPendenciasEmail() },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.3f).testTag("btn_exportar_pendencias_email")
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, tint = AccentGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Enviar por E-mail", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        OutlinedButton(
                            onClick = { handleExportarPendenciasPdf() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("btn_ver_pdf_pendencias")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = WarningRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF Completo", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // 2. PERIOD SELECTOR CARD
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = DeepBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Período do Relatório", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DeepBlue.copy(alpha = 0.08f),
                            modifier = Modifier.clickable { showCustomDateDialog = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = DeepBlue, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ajustar Datas", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Horizontal Scroll of Presets for comfortable one-touch selection
                    val scrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PresetPeriodo.values().forEach { preset ->
                            val isSelected = selectedPreset == preset
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) DeepBlue else GrayBackground,
                                modifier = Modifier.clickable { setPreset(preset) }
                            ) {
                                Text(
                                    text = preset.label,
                                    color = if (isSelected) Color.White else DeepBlue,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Highlighted Period Banner
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DeepBlue.copy(alpha = 0.06f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Período Ativo do Extrato:", fontSize = 10.sp, color = GrayText, fontWeight = FontWeight.SemiBold)
                                Text(periodoDescricao, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ForestGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "${clientesFiltradosPeriodo.size} Registos",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ForestGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. PERIOD SUMMARY METRICS (Portfolio Rule Breakdown)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Capital Desembolsado", fontSize = 11.sp, color = GrayText)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = KwanzaFormatters.formatAOA(totalDesembolsadoPeriodo),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepBlue
                            )
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Capital Amortizado", fontSize = 11.sp, color = GrayText)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = KwanzaFormatters.formatAOA(capitalAmortizadoPeriodo),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreen
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Juros Recebidos (Receita)", fontSize = 11.sp, color = GrayText)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = KwanzaFormatters.formatAOA(jurosRecebidosPeriodo),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkGold
                            )
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Total Recebido (Cap + Jur)", fontSize = 11.sp, color = GrayText)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = KwanzaFormatters.formatAOA(totalRecuperadoPeriodo),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepBlue
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Saldo da Carteira (Dívida)", fontSize = 11.sp, color = GrayText)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = KwanzaFormatters.formatAOA(saldoCarteiraPeriodo),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkGold
                            )
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Valor em Atraso", fontSize = 11.sp, color = GrayText)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = KwanzaFormatters.formatAOA(totalEmAtrasoPeriodo),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarningRed
                            )
                        }
                    }
                }
            }
        }

        // 4. SEARCH & FILTER BAR FOR THE CUSTOMER LIST
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val listTitle = when (selectedTipoRelatorio) {
                        TipoRelatorioPersonalizado.TODOS_CLIENTES -> "Lista Completa de Clientes Cadastrados"
                        TipoRelatorioPersonalizado.CARTEIRA_ATIVA -> "Lista de Contratos da Carteira Ativa"
                        TipoRelatorioPersonalizado.INADIMPLENTES_PENDENCIAS -> "Clientes com Inadimplência / Pendências"
                        TipoRelatorioPersonalizado.APENAS_REEMBOLSOS -> "Extrato de Reembolsos Recebidos"
                        TipoRelatorioPersonalizado.APENAS_DESEMBOLSOS -> "Extrato de Desembolsos Concedidos"
                        TipoRelatorioPersonalizado.GERAL -> "Lista de Contratos do Extrato Geral"
                    }
                    val countSubtitle = when (selectedTipoRelatorio) {
                        TipoRelatorioPersonalizado.APENAS_REEMBOLSOS -> "${pagamentosPeriodo.size} pagamentos"
                        TipoRelatorioPersonalizado.APENAS_DESEMBOLSOS -> "${listaDesembolsos.size} desembolsos"
                        TipoRelatorioPersonalizado.INADIMPLENTES_PENDENCIAS -> "${listaInadimplentes.size} pendentes"
                        TipoRelatorioPersonalizado.CARTEIRA_ATIVA -> "${listaCarteiraAtiva.size} ativos"
                        TipoRelatorioPersonalizado.TODOS_CLIENTES -> "${clientesFiltradosPeriodo.size} clientes"
                        TipoRelatorioPersonalizado.GERAL -> "${clientesFiltradosPeriodo.size} contratos"
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            listTitle,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepBlue
                        )
                        Text(
                            countSubtitle,
                            fontSize = 11.sp,
                            color = GrayText
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Pesquisar nome, tel, banco...", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(13.dp)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = GrayBackground,
                                focusedContainerColor = Color.White
                            )
                        )

                        // Status Filter Dropdown
                        var statusMenuOpen by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { statusMenuOpen = true },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text(selectedStatusFilter, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            }

                            DropdownMenu(
                                expanded = statusMenuOpen,
                                onDismissRequest = { statusMenuOpen = false }
                            ) {
                                listOf("Todos", "Ativo", "Em atraso", "Liquidado").forEach { st ->
                                    DropdownMenuItem(
                                        text = { Text(st, fontSize = 12.sp) },
                                        onClick = {
                                            selectedStatusFilter = st
                                            statusMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. LIST ITEMS CONTEXTUAL TO THE SELECTED REPORT (Rendered directly in LazyColumn for natural fluid scrolling)
        when (selectedTipoRelatorio) {
            TipoRelatorioPersonalizado.APENAS_REEMBOLSOS -> {
                if (pagamentosPeriodo.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = GrayText, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Nenhum reembolso registado para o período selecionado.", fontSize = 12.sp, color = GrayText, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                } else {
                    itemsIndexed(pagamentosPeriodo) { index, pag ->
                        val cli = clientesWithCredito.find { it.first.id == pag.clienteId }?.first
                        val crd = clientesWithCredito.find { it.second?.id == pag.creditoId }?.second
                        val cap = if (crd != null && crd.quantidadePrestacoes > 0) crd.valorDesembolsado / crd.quantidadePrestacoes else pag.valorPago * 0.5263
                        val jur = (pag.valorPago - cap).coerceAtLeast(0.0)

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Surface(shape = CircleShape, color = ForestGreen.copy(alpha = 0.12f), modifier = Modifier.size(24.dp)) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("${index + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = KwanzaFormatters.formatPrimeiroEUltimoNome(cli?.nome ?: "Cliente #${pag.clienteId}"),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = DeepBlue
                                            )
                                            Text(
                                                text = "Data: ${KwanzaFormatters.formatDate(pag.dataPagamento)} • Parcela ${pag.numeroPrestacao}",
                                                fontSize = 11.sp,
                                                color = GrayText
                                            )
                                        }
                                    }
                                    Surface(shape = RoundedCornerShape(6.dp), color = DeepBlue.copy(alpha = 0.08f)) {
                                        Text(
                                            text = pag.comprovativoRef.ifEmpty { "Transf" }.take(16),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepBlue,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = GraySurface.copy(alpha = 0.6f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Cap. Amortizado (Abate)", fontSize = 10.sp, color = ForestGreen, fontWeight = FontWeight.SemiBold)
                                        Text(KwanzaFormatters.formatAOA(cap), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Juros (Receita)", fontSize = 10.sp, color = DarkGold, fontWeight = FontWeight.SemiBold)
                                        Text(KwanzaFormatters.formatAOA(jur), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkGold)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Total Pago", fontSize = 10.sp, color = DeepBlue, fontWeight = FontWeight.SemiBold)
                                        Text(KwanzaFormatters.formatAOA(pag.valorPago), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            TipoRelatorioPersonalizado.APENAS_DESEMBOLSOS -> {
                if (listaDesembolsos.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Payments, contentDescription = null, tint = GrayText, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Nenhum desembolso registado para o período selecionado.", fontSize = 12.sp, color = GrayText, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                } else {
                    itemsIndexed(listaDesembolsos) { index, (cli, crd) ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Surface(shape = CircleShape, color = DeepBlue.copy(alpha = 0.1f), modifier = Modifier.size(24.dp)) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("${index + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = KwanzaFormatters.formatPrimeiroEUltimoNome(cli.nome),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = DeepBlue
                                            )
                                            Text(
                                                text = "${cli.banco} • Desembolso: ${KwanzaFormatters.formatDate(crd?.dataDesembolso ?: 0L)}",
                                                fontSize = 11.sp,
                                                color = GrayText
                                            )
                                        }
                                    }
                                    Surface(shape = RoundedCornerShape(6.dp), color = ForestGreen.copy(alpha = 0.15f)) {
                                        Text(
                                            text = "+ Aumenta Carteira",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ForestGreen,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = GraySurface.copy(alpha = 0.6f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Capital Desembolsado", fontSize = 10.sp, color = GrayText)
                                        Text(KwanzaFormatters.formatAOA(crd?.valorDesembolsado ?: 0.0), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Prazo / Taxa", fontSize = 10.sp, color = GrayText)
                                        Text("${crd?.quantidadePrestacoes}m • ${crd?.taxaPercentual}%/mês", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = DarkText)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Prestação Mensal", fontSize = 10.sp, color = GrayText)
                                        Text(KwanzaFormatters.formatAOA(crd?.prestacaoMensal ?: 0.0), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            TipoRelatorioPersonalizado.INADIMPLENTES_PENDENCIAS -> {
                if (listaInadimplentes.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Excelente! Nenhuma pendência ou inadimplência encontrada no período selecionado.", fontSize = 12.sp, color = ForestGreen, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                } else {
                    itemsIndexed(listaInadimplentes) { index, (cli, crd) ->
                        val dias = crd?.diasEmAtraso ?: 0
                        val diasCalc = if (dias > 0) dias else 1
                        val pagamentosCliente = pagamentosPeriodo.filter { it.creditoId == crd?.id }
                        val moraResultado = if (crd != null) {
                            com.example.util.MoraCalculatorHelper.calcular(crd, pagamentosCliente, diasCalc)
                        } else null
                        val prest = crd?.prestacaoMensal ?: 0.0
                        val mora = moraResultado?.jurosMoraAcumulados ?: (prest * 0.04 * diasCalc)
                        val totalDevido = moraResultado?.valorTotalEmDivida ?: (prest + mora)

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7F7)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Surface(shape = CircleShape, color = WarningRed.copy(alpha = 0.15f), modifier = Modifier.size(24.dp)) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("${index + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WarningRed)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = KwanzaFormatters.formatPrimeiroEUltimoNome(cli.nome),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = DeepBlue
                                            )
                                            Text(
                                                text = "Tel: ${cli.telefone} • Venc: ${if (crd != null) KwanzaFormatters.formatDate(crd.proximoVencimento) else "-"}",
                                                fontSize = 11.sp,
                                                color = GrayText
                                            )
                                        }
                                    }
                                    Surface(shape = RoundedCornerShape(6.dp), color = WarningRed.copy(alpha = 0.15f)) {
                                        Text(
                                            text = "${dias}d de atraso",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = WarningRed,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = WarningRed.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Prestação Vencida", fontSize = 10.sp, color = GrayText)
                                        Text(KwanzaFormatters.formatAOA(prest), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DarkText)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Mora 4%/dia", fontSize = 10.sp, color = WarningRed)
                                        Text(KwanzaFormatters.formatAOA(mora), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WarningRed)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Total a Cobrar", fontSize = 10.sp, color = WarningRed, fontWeight = FontWeight.Bold)
                                        Text(KwanzaFormatters.formatAOA(totalDevido), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = WarningRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            TipoRelatorioPersonalizado.CARTEIRA_ATIVA -> {
                if (listaCarteiraAtiva.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = GrayText, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Nenhum contrato ativo na carteira para o período selecionado.", fontSize = 12.sp, color = GrayText, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                } else {
                    itemsIndexed(listaCarteiraAtiva) { index, (cli, crd) ->
                        val desemb = crd?.valorDesembolsado ?: 0.0
                        val pagas = crd?.prestacoesPagas?.coerceIn(0, crd.quantidadePrestacoes) ?: 0
                        val amort = if (crd != null && crd.quantidadePrestacoes > 0) (crd.valorDesembolsado / crd.quantidadePrestacoes) * pagas else 0.0
                        val saldo = (desemb - amort).coerceAtLeast(0.0)

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Surface(shape = CircleShape, color = ForestGreen.copy(alpha = 0.1f), modifier = Modifier.size(24.dp)) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("${index + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = KwanzaFormatters.formatPrimeiroEUltimoNome(cli.nome),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = DeepBlue
                                            )
                                            Text(
                                                text = "${cli.banco} • Prestação: ${crd?.prestacoesPagas}/${crd?.quantidadePrestacoes}",
                                                fontSize = 11.sp,
                                                color = GrayText
                                            )
                                        }
                                    }
                                    Surface(shape = RoundedCornerShape(6.dp), color = DeepBlue.copy(alpha = 0.08f)) {
                                        Text(
                                            text = "Saldo: ${KwanzaFormatters.formatAOA(saldo)}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepBlue,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = GraySurface.copy(alpha = 0.6f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Capital Desembolsado", fontSize = 10.sp, color = GrayText)
                                        Text(KwanzaFormatters.formatAOA(desemb), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DeepBlue)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Capital Amortizado", fontSize = 10.sp, color = ForestGreen)
                                        Text(KwanzaFormatters.formatAOA(amort), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Saldo em Dívida", fontSize = 10.sp, color = DarkGold, fontWeight = FontWeight.Bold)
                                        Text(KwanzaFormatters.formatAOA(saldo), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkGold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            TipoRelatorioPersonalizado.TODOS_CLIENTES, TipoRelatorioPersonalizado.GERAL -> {
                if (clientesFiltradosPeriodo.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.SearchOff, contentDescription = null, tint = GrayText, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Nenhum contrato encontrado para o período e filtros selecionados.",
                                        fontSize = 12.sp,
                                        color = GrayText,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                } else {
                    itemsIndexed(clientesFiltradosPeriodo) { index, (cliente, credito) ->
                        val isOverdue = credito?.estado == "Em atraso" || (credito?.diasEmAtraso ?: 0) > 0
                        val isLiquidado = credito?.estado == "Liquidado"

                        val statusColor = when {
                            isLiquidado -> GrayText
                            isOverdue -> WarningRed
                            else -> ForestGreen
                        }
                        val statusText = when {
                            isLiquidado -> "Liquidado"
                            isOverdue -> "Em atraso (${credito?.diasEmAtraso ?: 0}d)"
                            else -> "Ativo"
                        }

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isOverdue) Color(0xFFFFF7F7) else Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Surface(
                                            shape = CircleShape,
                                            color = DeepBlue.copy(alpha = 0.1f),
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    "${index + 1}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = DeepBlue
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = KwanzaFormatters.formatPrimeiroEUltimoNome(cliente.nome),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = DeepBlue,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${cliente.banco} • ${cliente.telefone}",
                                                fontSize = 11.sp,
                                                color = GrayText
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = statusColor.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = statusText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = statusColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = GraySurface.copy(alpha = 0.6f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Valor Concedido", fontSize = 10.sp, color = GrayText)
                                        Text(
                                            KwanzaFormatters.formatAOA(credito?.valorDesembolsado ?: 0.0),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = DeepBlue
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Prestação", fontSize = 10.sp, color = GrayText)
                                        Text(
                                            KwanzaFormatters.formatAOA(credito?.prestacaoMensal ?: 0.0),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ForestGreen
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Reembolso", fontSize = 10.sp, color = GrayText)
                                        Text(
                                            text = if (credito != null) KwanzaFormatters.formatDate(credito.proximoVencimento) else "N/A",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isOverdue) WarningRed else DeepBlue
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. BOTTOM BANNER TO TRIGGER PDF EASILY AT THE END OF SCROLL
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ForestGreen.copy(alpha = 0.1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { handleGerarPdf() }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Imprimir ou Guardar Relatório", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
                            Text("Gerar documento oficial em PDF do período", fontSize = 11.sp, color = GrayText)
                        }
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = ForestGreen)
                }
            }
        }
    }

    // Custom Date Range Dialog
    if (showCustomDateDialog) {
        CustomDateRangePickerDialog(
            initialStart = dataInicioTimestamp,
            initialEnd = dataFimTimestamp,
            onDismiss = { showCustomDateDialog = false },
            onConfirm = { start: Long, end: Long ->
                dataInicioTimestamp = start
                dataFimTimestamp = end
                selectedPreset = PresetPeriodo.PERSONALIZADO
                showCustomDateDialog = false
            }
        )
    }
}

@Composable
fun CustomDateRangePickerDialog(
    initialStart: Long,
    initialEnd: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long) -> Unit
) {
    val context = LocalContext.current

    val calStart = Calendar.getInstance().apply {
        if (initialStart > 0L) timeInMillis = initialStart
        else {
            set(2026, Calendar.JUNE, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    val calEnd = Calendar.getInstance().apply {
        if (initialEnd < Long.MAX_VALUE && initialEnd > 0L) timeInMillis = initialEnd
        else {
            set(2026, Calendar.DECEMBER, 31, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }
    }

    var selectedStartYear by remember { mutableIntStateOf(calStart.get(Calendar.YEAR)) }
    var selectedStartMonth by remember { mutableIntStateOf(calStart.get(Calendar.MONTH)) }
    var selectedStartDay by remember { mutableIntStateOf(calStart.get(Calendar.DAY_OF_MONTH)) }

    var selectedEndYear by remember { mutableIntStateOf(calEnd.get(Calendar.YEAR)) }
    var selectedEndMonth by remember { mutableIntStateOf(calEnd.get(Calendar.MONTH)) }
    var selectedEndDay by remember { mutableIntStateOf(calEnd.get(Calendar.DAY_OF_MONTH)) }

    val startFormatted = String.format(Locale.getDefault(), "%02d/%02d/%04d", selectedStartDay, selectedStartMonth + 1, selectedStartYear)
    val endFormatted = String.format(Locale.getDefault(), "%02d/%02d/%04d", selectedEndDay, selectedEndMonth + 1, selectedEndYear)

    fun openStartDatePicker() {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedStartYear = year
                selectedStartMonth = month
                selectedStartDay = dayOfMonth
            },
            selectedStartYear,
            selectedStartMonth,
            selectedStartDay
        ).show()
    }

    fun openEndDatePicker() {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedEndYear = year
                selectedEndMonth = month
                selectedEndDay = dayOfMonth
            },
            selectedEndYear,
            selectedEndMonth,
            selectedEndDay
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = DeepBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Escolher Período do Extrato", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DeepBlue)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Defina as datas inicial e final para gerar o extrato completo da carteira de crédito em PDF com páginas correspondentes.",
                    fontSize = 12.sp,
                    color = GrayText
                )

                // Data Inicial Card
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = GrayBackground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openStartDatePicker() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Data Inicial (De)", fontSize = 11.sp, color = GrayText)
                            Text(startFormatted, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                        }
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Selecionar Data Inicial", tint = DeepBlue)
                    }
                }

                // Data Final Card
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = GrayBackground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openEndDatePicker() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Data Final (Até)", fontSize = 11.sp, color = GrayText)
                            Text(endFormatted, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                        }
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Selecionar Data Final", tint = DeepBlue)
                    }
                }

                // Quick Preset Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            selectedStartYear = 2026
                            selectedStartMonth = Calendar.JUNE
                            selectedStartDay = 1

                            selectedEndYear = 2026
                            selectedEndMonth = Calendar.DECEMBER
                            selectedEndDay = 31
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("01/06 a 31/12/2026", fontSize = 10.sp, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            val c = Calendar.getInstance()
                            selectedStartYear = c.get(Calendar.YEAR)
                            selectedStartMonth = c.get(Calendar.MONTH)
                            selectedStartDay = 1

                            selectedEndYear = c.get(Calendar.YEAR)
                            selectedEndMonth = c.get(Calendar.MONTH)
                            selectedEndDay = c.getActualMaximum(Calendar.DAY_OF_MONTH)
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("Mês Atual", fontSize = 10.sp, maxLines = 1)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sCal = Calendar.getInstance().apply {
                        set(selectedStartYear, selectedStartMonth, selectedStartDay, 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val eCal = Calendar.getInstance().apply {
                        set(selectedEndYear, selectedEndMonth, selectedEndDay, 23, 59, 59)
                        set(Calendar.MILLISECOND, 999)
                    }
                    onConfirm(sCal.timeInMillis, eCal.timeInMillis)
                },
                colors = ButtonDefaults.buttonColors(containerColor = DeepBlue)
            ) {
                Text("Aplicar Período", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

