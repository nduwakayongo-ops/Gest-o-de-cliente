package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.ClienteEntity
import com.example.data.model.CobrancaEntity
import com.example.data.model.CreditoEntity
import com.example.data.model.PagamentoEntity
import com.example.repository.DashboardSummary
import com.example.ui.components.KwanzaFormatters
import com.example.ui.viewmodel.CalculoSaldoDevedorState
import com.example.ui.viewmodel.ItemParcelaSchedule
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TipoRelatorioPersonalizado(
    val id: Int,
    val codigo: String,
    val tituloCurto: String,
    val tituloCompleto: String,
    val descricao: String
) {
    TODOS_CLIENTES(
        1,
        "todos_clientes",
        "1. Todos Clientes",
        "RELATÓRIO COMPLETO DE CLIENTES CADASTRADOS",
        "Relação nominal de todos os clientes, dados cadastrais, bancos e situação"
    ),
    CARTEIRA_ATIVA(
        2,
        "carteira_ativa",
        "2. Carteira Ativa",
        "RELATÓRIO DA CARTEIRA ATIVA DE CRÉDITO",
        "Contratos em vigor, capital desembolsado, amortizações e saldo da carteira"
    ),
    INADIMPLENTES_PENDENCIAS(
        3,
        "inadimplentes_pendencias",
        "3. Clientes Inadimplentes",
        "RELATÓRIO DE INADIMPLÊNCIA E REEMBOLSOS DO PERÍODO",
        "Clientes com atrasos ou reembolsos pendentes no período, juros de mora e total devido"
    ),
    APENAS_REEMBOLSOS(
        4,
        "apenas_reembolsos",
        "4. Apenas Reembolsos",
        "EXTRATO DETALHADO DE REEMBOLSOS RECEBIDOS",
        "Relação de pagamentos recebidos com separação de Capital Amortizado e Juros"
    ),
    APENAS_DESEMBOLSOS(
        5,
        "apenas_desembolsos",
        "5. Apenas Desembolsos",
        "EXTRATO DE DESEMBOLSOS DE CRÉDITO DO PERÍODO",
        "Operações de crédito desembolsadas no período que aumentam a carteira"
    ),
    GERAL(
        6,
        "geral",
        "6. Relatório Geral",
        "EXTRATO CONSOLIDADO DA CARTEIRA DE CRÉDITO",
        "Visão executiva global: indicadores de carteira, clientes e histórico de recebimentos"
    )
}

object PdfGeneratorHelper {

    private val deepBlue = Color.parseColor("#0A192F")
    private val accentGold = Color.parseColor("#D4AF37")
    private val darkText = Color.parseColor("#1E293B")
    private val grayText = Color.parseColor("#64748B")
    private val lightBg = Color.parseColor("#F8FAFC")
    private val lightRowAlt = Color.parseColor("#F1F5F9")
    private val forestGreen = Color.parseColor("#2E7D32")
    private val warningRed = Color.parseColor("#C62828")
    private val dividerColor = Color.parseColor("#E2E8F0")

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_LEFT = 20f
    private const val MARGIN_RIGHT = 575f
    private const val CONTENT_WIDTH = MARGIN_RIGHT - MARGIN_LEFT // 555f
    private const val MAX_CONTENT_Y = 770f
    private const val FOOTER_Y = 810f

    /**
     * Generates a multi-page comprehensive Portfolio & Statement PDF with unlimited dynamic pages.
     */
    fun generateRelatorioGeralPdf(
        context: Context,
        clientesWithCredito: List<Pair<ClienteEntity, CreditoEntity?>>,
        summary: DashboardSummary,
        periodoDescricao: String = "Geral",
        dataInicio: Long = 0L,
        dataFim: Long = Long.MAX_VALUE,
        pagamentos: List<PagamentoEntity> = emptyList(),
        tituloRelatorio: String = "EXTRATO DE CONTAS E RELATÓRIO DA CARTEIRA",
        subtituloRelatorio: String = "Gestão de Clientes e Carteira de Crédito"
    ): File? {
        try {
            val pdfDocument = PdfDocument()
            val paint = Paint().apply { isAntiAlias = true }

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            var currentPage = pdfDocument.startPage(pageInfo)
            var canvas = currentPage.canvas

            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val emissaoDataStr = dateFormat.format(Date())

            fun drawHeader(c: Canvas, pNum: Int, isFirst: Boolean) {
                val headerHeight = if (isFirst) 90f else 62f
                paint.color = deepBlue
                c.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), headerHeight, paint)

                paint.color = accentGold
                c.drawRect(0f, headerHeight - 4f, PAGE_WIDTH.toFloat(), headerHeight, paint)

                paint.color = Color.WHITE
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = if (isFirst) 16f else 13f
                c.drawText("GESTÃO DE CLIENTES — NDUWA KAYONGO", MARGIN_LEFT, if (isFirst) 32f else 26f, paint)

                paint.textSize = if (isFirst) 11f else 9.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                c.drawText(tituloRelatorio, MARGIN_LEFT, if (isFirst) 50f else 42f, paint)

                paint.textSize = 9f
                if (isFirst) {
                    paint.color = accentGold
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    c.drawText("Período: $periodoDescricao", MARGIN_LEFT, 72f, paint)

                    paint.color = Color.WHITE
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    c.drawText("Emissão: $emissaoDataStr", 410f, 72f, paint)
                } else {
                    paint.color = accentGold
                    c.drawText("Período: $periodoDescricao  •  Emissão: $emissaoDataStr", MARGIN_LEFT, 55f, paint)
                }
            }

            fun drawFooter(c: Canvas, pNum: Int) {
                paint.color = dividerColor
                c.drawLine(MARGIN_LEFT, FOOTER_Y - 10f, MARGIN_RIGHT, FOOTER_Y - 10f, paint)

                paint.color = grayText
                paint.textSize = 8.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                c.drawText("Gestão Inteligente de Crédito • Nduwa Kayongo", MARGIN_LEFT, FOOTER_Y + 4f, paint)
                c.drawText("Página $pNum", 525f, FOOTER_Y + 4f, paint)
            }

            fun startNewPage() {
                drawFooter(canvas, pageNumber)
                pdfDocument.finishPage(currentPage)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                currentPage = pdfDocument.startPage(pageInfo)
                canvas = currentPage.canvas
                drawHeader(canvas, pageNumber, isFirst = false)
            }

            // Draw initial header for Page 1
            drawHeader(canvas, pageNumber, isFirst = true)
            var y = 105f

            // Page 1: Resumo Executivo da Carteira e do Período
            paint.color = lightBg
            canvas.drawRoundRect(MARGIN_LEFT, y, MARGIN_RIGHT, y + 102f, 8f, 8f, paint)

            paint.color = deepBlue
            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("RESUMO EXECUTIVO DO PERÍODO & CARTEIRA DE CRÉDITO", MARGIN_LEFT + 10f, y + 16f, paint)

            paint.color = darkText
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            canvas.drawText("Capital Desembolsado: ${KwanzaFormatters.formatAOA(summary.totalDesembolsado)}", MARGIN_LEFT + 10f, y + 34f, paint)
            canvas.drawText("Capital Amortizado: ${KwanzaFormatters.formatAOA(summary.capitalAmortizado)}", 300f, y + 34f, paint)

            canvas.drawText("Juros Recebidos (Receita): ${KwanzaFormatters.formatAOA(summary.jurosRecebidos)}", MARGIN_LEFT + 10f, y + 50f, paint)
            canvas.drawText("Total Recebido (Cap + Jur): ${KwanzaFormatters.formatAOA(summary.totalRecebido)}", 300f, y + 50f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = forestGreen
            canvas.drawText("Saldo da Carteira (Dívida): ${KwanzaFormatters.formatAOA(summary.carteiraAtiva)}", MARGIN_LEFT + 10f, y + 68f, paint)

            paint.color = darkText
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Total Clientes / Operações: ${clientesWithCredito.size}", 300f, y + 68f, paint)

            canvas.drawText("Valor em Atraso: ${KwanzaFormatters.formatAOA(summary.valorEmAtraso)}", MARGIN_LEFT + 10f, y + 84f, paint)
            canvas.drawText("Índice de Risco: ${String.format(Locale.US, "%.2f", summary.riscoPercentual)}%", 300f, y + 84f, paint)

            paint.textSize = 7.5f
            paint.color = grayText
            canvas.drawText("Regra de Carteira: Apenas o Capital Amortizado reduz a carteira. Juros recebidos são receita financeira.", MARGIN_LEFT + 10f, y + 96f, paint)

            y += 118f

            // Section 1: Tabela de Clientes e Contratos
            fun drawClientTableHeader(c: Canvas, curY: Float) {
                paint.color = deepBlue
                paint.textSize = 12f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                c.drawText("1. EXTRATO DE CLIENTES E CONTRATOS DA CARTEIRA", MARGIN_LEFT, curY, paint)

                paint.color = accentGold
                c.drawLine(MARGIN_LEFT, curY + 6f, MARGIN_RIGHT, curY + 6f, paint)

                // Table Header Bar
                val tableHeaderY = curY + 12f
                paint.color = deepBlue
                c.drawRect(MARGIN_LEFT, tableHeaderY, MARGIN_RIGHT, tableHeaderY + 18f, paint)

                paint.color = Color.WHITE
                paint.textSize = 7.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                c.drawText("Nº / CLIENTE", 25f, tableHeaderY + 12f, paint)
                c.drawText("TELEFONE / BANCO", 155f, tableHeaderY + 12f, paint)
                c.drawText("VALOR RECEBIDO", 265f, tableHeaderY + 12f, paint)
                c.drawText("PRESTAÇÃO", 355f, tableHeaderY + 12f, paint)
                c.drawText("REEMBOLSO", 440f, tableHeaderY + 12f, paint)
                c.drawText("ESTADO", 515f, tableHeaderY + 12f, paint)
            }

            drawClientTableHeader(canvas, y)
            y += 34f

            var totalValorDesembolsado = 0.0
            var totalValorPrestacoes = 0.0

            clientesWithCredito.forEachIndexed { index, (cliente, credito) ->
                val rowHeight = 17f
                if (y + rowHeight > MAX_CONTENT_Y) {
                    startNewPage()
                    y = 75f
                    drawClientTableHeader(canvas, y)
                    y += 34f
                }

                if (index % 2 == 1) {
                    paint.color = lightRowAlt
                    canvas.drawRect(MARGIN_LEFT, y - 10f, MARGIN_RIGHT, y + 7f, paint)
                }

                val shortNome = KwanzaFormatters.formatPrimeiroEUltimoNome(cliente.nome)
                val displayNome = if (shortNome.length > 20) shortNome.take(18) + ".." else shortNome

                paint.color = darkText
                paint.textSize = 8f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText("${index + 1}. $displayNome", 25f, y, paint)

                val telBanco = "${cliente.telefone} (${cliente.banco})"
                val displayTelBanco = if (telBanco.length > 18) telBanco.take(17) + "." else telBanco
                canvas.drawText(displayTelBanco, 155f, y, paint)

                val valorDesemb = credito?.valorDesembolsado ?: 0.0
                totalValorDesembolsado += valorDesemb
                canvas.drawText(KwanzaFormatters.formatAOA(valorDesemb), 265f, y, paint)

                val prestacao = credito?.prestacaoMensal ?: 0.0
                totalValorPrestacoes += prestacao
                paint.color = forestGreen
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(KwanzaFormatters.formatAOA(prestacao), 355f, y, paint)

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = darkText
                val proximoReembolso = if (credito != null) KwanzaFormatters.formatDate(credito.proximoVencimento) else "-"
                canvas.drawText(proximoReembolso, 440f, y, paint)

                val isAtraso = credito?.estado == "Em atraso" || (credito?.diasEmAtraso ?: 0) > 0
                paint.color = if (isAtraso) warningRed else if (credito?.estado == "Liquidado") forestGreen else deepBlue
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val estadoText = if (isAtraso) "${credito?.diasEmAtraso}d Atraso" else (credito?.estado ?: "Ativo")
                canvas.drawText(estadoText, 515f, y, paint)

                y += rowHeight
            }

            // Totals Row for Clients Table
            if (y + 24f > MAX_CONTENT_Y) {
                startNewPage()
                y = 75f
            }

            paint.color = deepBlue
            canvas.drawRect(MARGIN_LEFT, y - 6f, MARGIN_RIGHT, y + 16f, paint)

            paint.color = Color.WHITE
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("TOTAIS DA CARTEIRA (${clientesWithCredito.size} Contratos)", 25f, y + 8f, paint)
            canvas.drawText(KwanzaFormatters.formatAOA(totalValorDesembolsado), 265f, y + 8f, paint)
            canvas.drawText(KwanzaFormatters.formatAOA(totalValorPrestacoes), 355f, y + 8f, paint)

            y += 32f

            // Section 2: Detailed Payments in Period (Extrato de Pagamentos no Período)
            val pagamentosNoPeriodo = pagamentos.filter {
                if (dataInicio > 0L || dataFim < Long.MAX_VALUE) {
                    it.dataPagamento in dataInicio..dataFim
                } else true
            }

            if (pagamentosNoPeriodo.isNotEmpty()) {
                if (y + 60f > MAX_CONTENT_Y) {
                    startNewPage()
                    y = 75f
                }

                fun drawPaymentTableHeader(c: Canvas, curY: Float) {
                    paint.color = deepBlue
                    paint.textSize = 12f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    c.drawText("2. EXTRATO DETALHADO DE RECEBIMENTOS NO PERÍODO", MARGIN_LEFT, curY, paint)

                    paint.color = accentGold
                    c.drawLine(MARGIN_LEFT, curY + 6f, MARGIN_RIGHT, curY + 6f, paint)

                    val pHeaderY = curY + 12f
                    paint.color = deepBlue
                    c.drawRect(MARGIN_LEFT, pHeaderY, MARGIN_RIGHT, pHeaderY + 18f, paint)

                    paint.color = Color.WHITE
                    paint.textSize = 7.5f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    c.drawText("DATA", 25f, pHeaderY + 12f, paint)
                    c.drawText("CLIENTE", 90f, pHeaderY + 12f, paint)
                    c.drawText("PRESTAÇÃO", 240f, pHeaderY + 12f, paint)
                    c.drawText("COMPROVATIVO / REF", 330f, pHeaderY + 12f, paint)
                    c.drawText("VALOR PAGO (AOA)", 465f, pHeaderY + 12f, paint)
                }

                drawPaymentTableHeader(canvas, y)
                y += 34f

                var totalPagamentosPeriodo = 0.0

                pagamentosNoPeriodo.forEachIndexed { idx, pag ->
                    val rowHeight = 17f
                    if (y + rowHeight > MAX_CONTENT_Y) {
                        startNewPage()
                        y = 75f
                        drawPaymentTableHeader(canvas, y)
                        y += 34f
                    }

                    if (idx % 2 == 1) {
                        paint.color = lightRowAlt
                        canvas.drawRect(MARGIN_LEFT, y - 10f, MARGIN_RIGHT, y + 7f, paint)
                    }

                    val cliente = clientesWithCredito.find { it.first.id == pag.clienteId }?.first
                    val clienteNome = cliente?.let { KwanzaFormatters.formatPrimeiroEUltimoNome(it.nome) } ?: "Cliente #${pag.clienteId}"

                    paint.color = darkText
                    paint.textSize = 8f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    canvas.drawText(KwanzaFormatters.formatDate(pag.dataPagamento), 25f, y, paint)
                    canvas.drawText(clienteNome, 90f, y, paint)
                    canvas.drawText("Prestação nº ${pag.numeroPrestacao}", 240f, y, paint)
                    canvas.drawText(pag.comprovativoRef.ifEmpty { "Transferência" }, 330f, y, paint)

                    paint.color = forestGreen
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText(KwanzaFormatters.formatAOA(pag.valorPago), 465f, y, paint)

                    totalPagamentosPeriodo += pag.valorPago
                    y += rowHeight
                }

                // Total Payments Row
                if (y + 24f > MAX_CONTENT_Y) {
                    startNewPage()
                    y = 75f
                }

                paint.color = deepBlue
                canvas.drawRect(MARGIN_LEFT, y - 6f, MARGIN_RIGHT, y + 16f, paint)

                paint.color = Color.WHITE
                paint.textSize = 8.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("TOTAL RECEBIDO NO PERÍODO (${pagamentosNoPeriodo.size} Pagamentos)", 25f, y + 8f, paint)
                canvas.drawText(KwanzaFormatters.formatAOA(totalPagamentosPeriodo), 465f, y + 8f, paint)

                y += 32f
            }

            // Section 3: Official Signatures & Closure (Ensure fits or new page)
            if (y + 80f > MAX_CONTENT_Y) {
                startNewPage()
                y = 75f
            }

            y += 15f
            paint.color = grayText
            paint.strokeWidth = 1f
            canvas.drawLine(50f, y + 35f, 250f, y + 35f, paint)
            canvas.drawLine(340f, y + 35f, 540f, y + 35f, paint)

            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Gestor de Crédito: Nduwa Kayongo", 60f, y + 50f, paint)
            canvas.drawText("Validação e Fecho do Período", 370f, y + 50f, paint)

            // Draw final page footer
            drawFooter(canvas, pageNumber)
            pdfDocument.finishPage(currentPage)

            val safePeriodoName = periodoDescricao.replace("[^a-zA-Z0-9_]".toRegex(), "_").take(30)
            val file = File(context.cacheDir, "Extrato_Carteira_${safePeriodoName}.pdf")
            FileOutputStream(file).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            pdfDocument.close()

            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Generates custom specialized reports in PDF for the 6 report types requested:
     * 1. Todos os Clientes
     * 2. Carteira Ativa
     * 3. Clientes Inadimplentes / Pendências
     * 4. Apenas Reembolsos
     * 5. Apenas Desembolsos
     * 6. Geral
     */
    fun generateRelatorioPersonalizadoPdf(
        context: Context,
        tipo: TipoRelatorioPersonalizado,
        clientesWithCredito: List<Pair<ClienteEntity, CreditoEntity?>>,
        summary: DashboardSummary,
        periodoDescricao: String = "Geral",
        dataInicio: Long = 0L,
        dataFim: Long = Long.MAX_VALUE,
        pagamentos: List<PagamentoEntity> = emptyList()
    ): File? {
        if (tipo == TipoRelatorioPersonalizado.GERAL) {
            return generateRelatorioGeralPdf(
                context = context,
                clientesWithCredito = clientesWithCredito,
                summary = summary,
                periodoDescricao = periodoDescricao,
                dataInicio = dataInicio,
                dataFim = dataFim,
                pagamentos = pagamentos,
                tituloRelatorio = tipo.tituloCompleto,
                subtituloRelatorio = tipo.descricao
            )
        }

        try {
            val pdfDocument = PdfDocument()
            val paint = Paint().apply { isAntiAlias = true }

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            var currentPage = pdfDocument.startPage(pageInfo)
            var canvas = currentPage.canvas

            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val emissaoDataStr = dateFormat.format(Date())

            fun drawHeader(c: Canvas, pNum: Int, isFirst: Boolean) {
                val headerHeight = if (isFirst) 90f else 62f
                paint.color = deepBlue
                c.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), headerHeight, paint)

                val barColor = if (tipo == TipoRelatorioPersonalizado.INADIMPLENTES_PENDENCIAS) warningRed else accentGold
                paint.color = barColor
                c.drawRect(0f, headerHeight - 4f, PAGE_WIDTH.toFloat(), headerHeight, paint)

                paint.color = Color.WHITE
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = if (isFirst) 15f else 12f
                c.drawText("GESTÃO DE CLIENTES — NDUWA KAYONGO", MARGIN_LEFT, if (isFirst) 32f else 26f, paint)

                paint.textSize = if (isFirst) 10.5f else 9f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                c.drawText(tipo.tituloCompleto, MARGIN_LEFT, if (isFirst) 50f else 42f, paint)

                paint.textSize = 8.5f
                if (isFirst) {
                    paint.color = accentGold
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    c.drawText("Período: $periodoDescricao", MARGIN_LEFT, 72f, paint)

                    paint.color = Color.WHITE
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    c.drawText("Emissão: $emissaoDataStr", 400f, 72f, paint)
                } else {
                    paint.color = accentGold
                    c.drawText("Período: $periodoDescricao  •  Emissão: $emissaoDataStr", MARGIN_LEFT, 55f, paint)
                }
            }

            fun drawFooter(c: Canvas, pNum: Int) {
                paint.color = dividerColor
                c.drawLine(MARGIN_LEFT, FOOTER_Y - 10f, MARGIN_RIGHT, FOOTER_Y - 10f, paint)

                paint.color = grayText
                paint.textSize = 8.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                c.drawText("Gestão Inteligente de Crédito • Nduwa Kayongo", MARGIN_LEFT, FOOTER_Y + 4f, paint)
                c.drawText("Página $pNum", 525f, FOOTER_Y + 4f, paint)
            }

            fun startNewPage() {
                drawFooter(canvas, pageNumber)
                pdfDocument.finishPage(currentPage)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                currentPage = pdfDocument.startPage(pageInfo)
                canvas = currentPage.canvas
                drawHeader(canvas, pageNumber, isFirst = false)
            }

            drawHeader(canvas, pageNumber, isFirst = true)
            var y = 105f

            when (tipo) {
                TipoRelatorioPersonalizado.TODOS_CLIENTES -> {
                    paint.color = lightBg
                    canvas.drawRoundRect(MARGIN_LEFT, y, MARGIN_RIGHT, y + 80f, 8f, 8f, paint)

                    paint.color = deepBlue
                    paint.textSize = 10.5f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("RESUMO GERAL DE CLIENTES E SITUAÇÃO CADASTRAL", MARGIN_LEFT + 10f, y + 16f, paint)

                    paint.color = darkText
                    paint.textSize = 8.5f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    val totalCadastrados = clientesWithCredito.size
                    val ativosCount = clientesWithCredito.count { it.second?.estado == "Ativo" }
                    val atrasoCount = clientesWithCredito.count { it.second?.estado == "Em atraso" || (it.second?.diasEmAtraso ?: 0) > 0 }
                    val liqCount = clientesWithCredito.count { it.second?.estado == "Liquidado" }
                    val totalDesembGeral = clientesWithCredito.sumOf { it.second?.valorDesembolsado ?: 0.0 }

                    canvas.drawText("Total de Clientes Cadastrados: $totalCadastrados", MARGIN_LEFT + 10f, y + 34f, paint)
                    canvas.drawText("Contratos Ativos: $ativosCount", 300f, y + 34f, paint)
                    canvas.drawText("Contratos em Atraso: $atrasoCount", MARGIN_LEFT + 10f, y + 50f, paint)
                    canvas.drawText("Contratos Liquidados: $liqCount", 300f, y + 50f, paint)
                    paint.color = forestGreen
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("Total Capital Desembolsado: ${KwanzaFormatters.formatAOA(totalDesembGeral)}", MARGIN_LEFT + 10f, y + 68f, paint)

                    y += 95f

                    fun drawAllClientsHeader(c: Canvas, curY: Float) {
                        paint.color = deepBlue
                        c.drawRect(MARGIN_LEFT, curY, MARGIN_RIGHT, curY + 18f, paint)
                        paint.color = Color.WHITE
                        paint.textSize = 7.5f
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        c.drawText("#", 25f, curY + 12f, paint)
                        c.drawText("CLIENTE / CONTACTO", 45f, curY + 12f, paint)
                        c.drawText("BANCO / CONTA", 185f, curY + 12f, paint)
                        c.drawText("CAPITAL CONCEDIDO", 295f, curY + 12f, paint)
                        c.drawText("PRESTAÇÃO", 380f, curY + 12f, paint)
                        c.drawText("SALDO CAPITAL", 455f, curY + 12f, paint)
                        c.drawText("ESTADO", 525f, curY + 12f, paint)
                    }

                    drawAllClientsHeader(canvas, y)
                    y += 20f

                    var totalCap = 0.0
                    var totalPrest = 0.0
                    var totalSaldoCap = 0.0

                    clientesWithCredito.forEachIndexed { idx, (cli, crd) ->
                        val rowHeight = 18f
                        if (y + rowHeight > MAX_CONTENT_Y) {
                            startNewPage()
                            y = 75f
                            drawAllClientsHeader(canvas, y)
                            y += 20f
                        }

                        if (idx % 2 == 1) {
                            paint.color = lightRowAlt
                            canvas.drawRect(MARGIN_LEFT, y - 10f, MARGIN_RIGHT, y + 7f, paint)
                        }

                        paint.color = darkText
                        paint.textSize = 7.5f
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        canvas.drawText("${idx + 1}", 25f, y, paint)

                        val nomeCurto = KwanzaFormatters.formatPrimeiroEUltimoNome(cli.nome).take(20)
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        canvas.drawText(nomeCurto, 45f, y, paint)

                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        val bancoStr = "${cli.banco} (${cli.numeroConta.take(8)})"
                        canvas.drawText(bancoStr, 185f, y, paint)

                        val vDesemb = crd?.valorDesembolsado ?: 0.0
                        totalCap += vDesemb
                        canvas.drawText(KwanzaFormatters.formatAOA(vDesemb), 295f, y, paint)

                        val prest = crd?.prestacaoMensal ?: 0.0
                        totalPrest += prest
                        canvas.drawText(KwanzaFormatters.formatAOA(prest), 380f, y, paint)

                        val capAmort = if (crd != null && crd.quantidadePrestacoes > 0) {
                            (crd.valorDesembolsado / crd.quantidadePrestacoes) * crd.prestacoesPagas.coerceIn(0, crd.quantidadePrestacoes)
                        } else 0.0
                        val saldoCap = if (crd != null) (crd.valorDesembolsado - capAmort).coerceAtLeast(0.0) else 0.0
                        totalSaldoCap += saldoCap

                        paint.color = forestGreen
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        canvas.drawText(KwanzaFormatters.formatAOA(saldoCap), 455f, y, paint)

                        val isAtraso = crd?.estado == "Em atraso" || (crd?.diasEmAtraso ?: 0) > 0
                        paint.color = if (isAtraso) warningRed else if (crd?.estado == "Liquidado") grayText else deepBlue
                        canvas.drawText(if (isAtraso) "Em atraso" else (crd?.estado ?: "Ativo"), 525f, y, paint)

                        y += rowHeight
                    }

                    if (y + 24f > MAX_CONTENT_Y) {
                        startNewPage()
                        y = 75f
                    }
                    paint.color = deepBlue
                    canvas.drawRect(MARGIN_LEFT, y - 6f, MARGIN_RIGHT, y + 16f, paint)
                    paint.color = Color.WHITE
                    paint.textSize = 8f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("TOTAIS (${clientesWithCredito.size} CLIENTES)", 25f, y + 8f, paint)
                    canvas.drawText(KwanzaFormatters.formatAOA(totalCap), 295f, y + 8f, paint)
                    canvas.drawText(KwanzaFormatters.formatAOA(totalPrest), 380f, y + 8f, paint)
                    canvas.drawText(KwanzaFormatters.formatAOA(totalSaldoCap), 455f, y + 8f, paint)
                    y += 32f
                }

                TipoRelatorioPersonalizado.CARTEIRA_ATIVA -> {
                    val listaAtivos = clientesWithCredito.filter { it.second != null && it.second?.estado != "Liquidado" }

                    paint.color = lightBg
                    canvas.drawRoundRect(MARGIN_LEFT, y, MARGIN_RIGHT, y + 92f, 8f, 8f, paint)

                    paint.color = deepBlue
                    paint.textSize = 10.5f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("INDICADORES DA CARTEIRA ATIVA (REGRA DO CAPITAL EM DÍVIDA)", MARGIN_LEFT + 10f, y + 16f, paint)

                    val totalDesembAtivos = listaAtivos.sumOf { it.second?.valorDesembolsado ?: 0.0 }
                    val totalAmortAtivos = listaAtivos.sumOf { (_, crd) ->
                        if (crd != null && crd.quantidadePrestacoes > 0) {
                            (crd.valorDesembolsado / crd.quantidadePrestacoes) * crd.prestacoesPagas.coerceIn(0, crd.quantidadePrestacoes)
                        } else 0.0
                    }
                    val saldoCarteiraReal = (totalDesembAtivos - totalAmortAtivos).coerceAtLeast(0.0)

                    paint.color = darkText
                    paint.textSize = 8.5f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    canvas.drawText("Capital Desembolsado Ativo: ${KwanzaFormatters.formatAOA(totalDesembAtivos)}", MARGIN_LEFT + 10f, y + 34f, paint)
                    canvas.drawText("Capital Amortizado: ${KwanzaFormatters.formatAOA(totalAmortAtivos)}", 300f, y + 34f, paint)

                    paint.color = forestGreen
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("SALDO DA CARTEIRA (EM DÍVIDA): ${KwanzaFormatters.formatAOA(saldoCarteiraReal)}", MARGIN_LEFT + 10f, y + 52f, paint)
                    canvas.drawText("Total de Contratos Ativos: ${listaAtivos.size}", 300f, y + 52f, paint)

                    paint.color = grayText
                    paint.textSize = 7.5f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    canvas.drawText("Regra de Ouro: Saldo da Carteira = Capital Desembolsado − Capital Amortizado. Juros não abatem na carteira.", MARGIN_LEFT + 10f, y + 72f, paint)

                    y += 106f

                    fun drawActiveHeader(c: Canvas, curY: Float) {
                        paint.color = deepBlue
                        c.drawRect(MARGIN_LEFT, curY, MARGIN_RIGHT, curY + 18f, paint)
                        paint.color = Color.WHITE
                        paint.textSize = 7.5f
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        c.drawText("#", 25f, curY + 12f, paint)
                        c.drawText("CLIENTE / CONTACTO", 45f, curY + 12f, paint)
                        c.drawText("BANCO", 180f, curY + 12f, paint)
                        c.drawText("CAPITAL DESEMB.", 255f, curY + 12f, paint)
                        c.drawText("CAPITAL AMORT.", 335f, curY + 12f, paint)
                        c.drawText("SALDO EM DÍVIDA", 415f, curY + 12f, paint)
                        c.drawText("PRESTAÇÃO", 485f, curY + 12f, paint)
                        c.drawText("PRÓX. VENC.", 535f, curY + 12f, paint)
                    }

                    drawActiveHeader(canvas, y)
                    y += 20f

                    listaAtivos.forEachIndexed { idx, (cli, crd) ->
                        val rowHeight = 18f
                        if (y + rowHeight > MAX_CONTENT_Y) {
                            startNewPage()
                            y = 75f
                            drawActiveHeader(canvas, y)
                            y += 20f
                        }

                        if (idx % 2 == 1) {
                            paint.color = lightRowAlt
                            canvas.drawRect(MARGIN_LEFT, y - 10f, MARGIN_RIGHT, y + 7f, paint)
                        }

                        paint.color = darkText
                        paint.textSize = 7.5f
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        canvas.drawText("${idx + 1}", 25f, y, paint)

                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        canvas.drawText(KwanzaFormatters.formatPrimeiroEUltimoNome(cli.nome).take(20), 45f, y, paint)

                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        canvas.drawText(cli.banco.take(10), 180f, y, paint)

                        val desemb = crd?.valorDesembolsado ?: 0.0
                        canvas.drawText(KwanzaFormatters.formatAOA(desemb), 255f, y, paint)

                        val amort = if (crd != null && crd.quantidadePrestacoes > 0) {
                            (crd.valorDesembolsado / crd.quantidadePrestacoes) * crd.prestacoesPagas.coerceIn(0, crd.quantidadePrestacoes)
                        } else 0.0
                        canvas.drawText(KwanzaFormatters.formatAOA(amort), 335f, y, paint)

                        val saldo = (desemb - amort).coerceAtLeast(0.0)
                        paint.color = forestGreen
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        canvas.drawText(KwanzaFormatters.formatAOA(saldo), 415f, y, paint)

                        paint.color = darkText
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        canvas.drawText(KwanzaFormatters.formatAOA(crd?.prestacaoMensal ?: 0.0), 485f, y, paint)

                        val vencStr = if (crd != null) KwanzaFormatters.formatDate(crd.proximoVencimento).take(5) else "-"
                        canvas.drawText(vencStr, 535f, y, paint)

                        y += rowHeight
                    }

                    if (y + 24f > MAX_CONTENT_Y) {
                        startNewPage()
                        y = 75f
                    }
                    paint.color = deepBlue
                    canvas.drawRect(MARGIN_LEFT, y - 6f, MARGIN_RIGHT, y + 16f, paint)
                    paint.color = Color.WHITE
                    paint.textSize = 8f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("TOTAL DA CARTEIRA ATIVA (${listaAtivos.size} CONTRATOS)", 25f, y + 8f, paint)
                    canvas.drawText(KwanzaFormatters.formatAOA(totalDesembAtivos), 255f, y + 8f, paint)
                    canvas.drawText(KwanzaFormatters.formatAOA(totalAmortAtivos), 335f, y + 8f, paint)
                    canvas.drawText(KwanzaFormatters.formatAOA(saldoCarteiraReal), 415f, y + 8f, paint)
                    y += 32f
                }

                TipoRelatorioPersonalizado.INADIMPLENTES_PENDENCIAS -> {
                    val listaPendentes = clientesWithCredito.filter { (_, crd) ->
                        crd != null && crd.estado != "Liquidado" &&
                        (crd.estado == "Em atraso" || crd.diasEmAtraso > 0 ||
                         (if (dataInicio > 0L || dataFim < Long.MAX_VALUE) crd.proximoVencimento in dataInicio..dataFim else true))
                    }

                    paint.color = Color.parseColor("#FFF7F7")
                    canvas.drawRoundRect(MARGIN_LEFT, y, MARGIN_RIGHT, y + 92f, 8f, 8f, paint)

                    paint.color = warningRed
                    paint.textSize = 10.5f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("PAINEL DE PENDÊNCIAS FINANCEIRAS E COBRANÇA (MORA 4%/DIA)", MARGIN_LEFT + 10f, y + 16f, paint)

                    var somaPrestacoes = 0.0
                    var somaMora = 0.0
                    var somaDevido = 0.0

                    listaPendentes.forEach { (_, crd) ->
                        if (crd != null) {
                            val dias = if (crd.diasEmAtraso > 0) crd.diasEmAtraso else 1
                            val moraResultado = MoraCalculatorHelper.calcular(crd, pagamentos.filter { it.creditoId == crd.id }, dias)
                            val prest = if (crd.valorEmAtraso > 0.0) crd.valorEmAtraso else crd.prestacaoMensal
                            somaPrestacoes += prest
                            somaMora += moraResultado.jurosMoraAcumulados
                            somaDevido += moraResultado.valorTotalEmDivida
                        }
                    }

                    paint.color = darkText
                    paint.textSize = 8.5f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    canvas.drawText("Clientes com Pendência no Período: ${listaPendentes.size}", MARGIN_LEFT + 10f, y + 34f, paint)
                    canvas.drawText("Prestações Vencidas: ${KwanzaFormatters.formatAOA(somaPrestacoes)}", 300f, y + 34f, paint)

                    paint.color = warningRed
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("Total de Juros de Mora (4%/dia): ${KwanzaFormatters.formatAOA(somaMora)}", MARGIN_LEFT + 10f, y + 52f, paint)
                    canvas.drawText("TOTAL GERAL A COBRAR: ${KwanzaFormatters.formatAOA(somaDevido)}", 300f, y + 52f, paint)

                    paint.color = grayText
                    paint.textSize = 7.5f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    canvas.drawText("Cobrança institucional Mbuli-Microcrédito com aplicação de penalização de 4% ao dia por prestação vencida.", MARGIN_LEFT + 10f, y + 72f, paint)

                    y += 106f

                    fun drawOverdueHeader(c: Canvas, curY: Float) {
                        paint.color = deepBlue
                        c.drawRect(MARGIN_LEFT, curY, MARGIN_RIGHT, curY + 18f, paint)
                        paint.color = Color.WHITE
                        paint.textSize = 7.5f
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        c.drawText("#", 25f, curY + 12f, paint)
                        c.drawText("CLIENTE / CONTACTO", 45f, curY + 12f, paint)
                        c.drawText("BANCO", 180f, curY + 12f, paint)
                        c.drawText("VENCIMENTO", 245f, curY + 12f, paint)
                        c.drawText("ATRASO", 310f, curY + 12f, paint)
                        c.drawText("PRESTAÇÃO", 365f, curY + 12f, paint)
                        c.drawText("MORA 4%/D", 440f, curY + 12f, paint)
                        c.drawText("TOTAL DEVIDO", 510f, curY + 12f, paint)
                    }

                    drawOverdueHeader(canvas, y)
                    y += 20f

                    listaPendentes.forEachIndexed { idx, (cli, crd) ->
                        val rowHeight = 18f
                        if (y + rowHeight > MAX_CONTENT_Y) {
                            startNewPage()
                            y = 75f
                            drawOverdueHeader(canvas, y)
                            y += 20f
                        }

                        if (idx % 2 == 1) {
                            paint.color = lightRowAlt
                            canvas.drawRect(MARGIN_LEFT, y - 10f, MARGIN_RIGHT, y + 7f, paint)
                        }

                        val dias = crd?.diasEmAtraso ?: 0
                        val diasCalc = if (dias > 0) dias else 1
                        val pagamentosCli = pagamentos.filter { it.creditoId == crd?.id }
                        val moraResultado = if (crd != null) MoraCalculatorHelper.calcular(crd, pagamentosCli, diasCalc) else null
                        val prest = if ((crd?.valorEmAtraso ?: 0.0) > 0.0) crd!!.valorEmAtraso else (crd?.prestacaoMensal ?: 0.0)
                        val mora = moraResultado?.jurosMoraAcumulados ?: (prest * 0.04 * diasCalc)
                        val totalCli = moraResultado?.valorTotalEmDivida ?: (prest + mora)

                        paint.color = darkText
                        paint.textSize = 7.5f
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        canvas.drawText("${idx + 1}", 25f, y, paint)

                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        canvas.drawText(KwanzaFormatters.formatPrimeiroEUltimoNome(cli.nome).take(20), 45f, y, paint)

                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        canvas.drawText(cli.banco.take(10), 180f, y, paint)

                        val vencStr = if (crd != null) KwanzaFormatters.formatDate(crd.proximoVencimento) else "-"
                        canvas.drawText(vencStr, 245f, y, paint)

                        paint.color = warningRed
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        canvas.drawText("${dias}d", 310f, y, paint)

                        paint.color = darkText
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        canvas.drawText(KwanzaFormatters.formatAOA(prest), 365f, y, paint)
                        canvas.drawText(KwanzaFormatters.formatAOA(mora), 440f, y, paint)

                        paint.color = warningRed
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        canvas.drawText(KwanzaFormatters.formatAOA(totalCli), 510f, y, paint)

                        y += rowHeight
                    }

                    if (y + 24f > MAX_CONTENT_Y) {
                        startNewPage()
                        y = 75f
                    }
                    paint.color = warningRed
                    canvas.drawRect(MARGIN_LEFT, y - 6f, MARGIN_RIGHT, y + 16f, paint)
                    paint.color = Color.WHITE
                    paint.textSize = 8f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("TOTAL GERAL DE COBRANÇAS (${listaPendentes.size})", 25f, y + 8f, paint)
                    canvas.drawText(KwanzaFormatters.formatAOA(somaPrestacoes), 365f, y + 8f, paint)
                    canvas.drawText(KwanzaFormatters.formatAOA(somaMora), 440f, y + 8f, paint)
                    canvas.drawText(KwanzaFormatters.formatAOA(somaDevido), 510f, y + 8f, paint)
                    y += 32f
                }

                TipoRelatorioPersonalizado.APENAS_REEMBOLSOS -> {
                    val listaPags = pagamentos.filter {
                        if (dataInicio > 0L || dataFim < Long.MAX_VALUE) it.dataPagamento in dataInicio..dataFim else true
                    }.sortedByDescending { it.dataPagamento }

                    var totalCapAmort = 0.0
                    var totalJurosRec = 0.0
                    var totalGeralPago = 0.0

                    listaPags.forEach { pag ->
                        val crd = clientesWithCredito.find { it.second?.id == pag.creditoId }?.second
                        val cap = if (crd != null && crd.quantidadePrestacoes > 0) {
                            crd.valorDesembolsado / crd.quantidadePrestacoes
                        } else pag.valorPago * 0.5263
                        val jur = (pag.valorPago - cap).coerceAtLeast(0.0)
                        totalCapAmort += cap
                        totalJurosRec += jur
                        totalGeralPago += pag.valorPago
                    }

                    paint.color = lightBg
                    canvas.drawRoundRect(MARGIN_LEFT, y, MARGIN_RIGHT, y + 92f, 8f, 8f, paint)

                    paint.color = deepBlue
                    paint.textSize = 10.5f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("EXTRATO DE REEMBOLSOS — SEGREGAÇÃO DE CAPITAL E JUROS", MARGIN_LEFT + 10f, y + 16f, paint)

                    paint.color = darkText
                    paint.textSize = 8.5f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    canvas.drawText("Total de Reembolsos Efetuados: ${listaPags.size}", MARGIN_LEFT + 10f, y + 34f, paint)

                    paint.color = forestGreen
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("Capital Amortizado (Reduz Carteira): ${KwanzaFormatters.formatAOA(totalCapAmort)}", 260f, y + 34f, paint)

                    paint.color = Color.parseColor("#B8860B")
                    canvas.drawText("Juros Recebidos (Receita Mbuli): ${KwanzaFormatters.formatAOA(totalJurosRec)}", MARGIN_LEFT + 10f, y + 52f, paint)

                    paint.color = deepBlue
                    canvas.drawText("TOTAL GERAL RECEBIDO: ${KwanzaFormatters.formatAOA(totalGeralPago)}", 260f, y + 52f, paint)

                    paint.color = grayText
                    paint.textSize = 7.5f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    canvas.drawText("Regra Fundamental: Apenas o Capital Amortizado (${KwanzaFormatters.formatAOA(totalCapAmort)}) reduz o saldo da carteira. Os juros são receita.", MARGIN_LEFT + 10f, y + 72f, paint)

                    y += 106f

                    fun drawPaymentOnlyHeader(c: Canvas, curY: Float) {
                        paint.color = deepBlue
                        c.drawRect(MARGIN_LEFT, curY, MARGIN_RIGHT, curY + 18f, paint)
                        paint.color = Color.WHITE
                        paint.textSize = 7.5f
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        c.drawText("DATA", 25f, curY + 12f, paint)
                        c.drawText("CLIENTE", 85f, curY + 12f, paint)
                        c.drawText("PREST.", 205f, curY + 12f, paint)
                        c.drawText("COMPROVATIVO / REF", 250f, curY + 12f, paint)
                        c.drawText("CAP. AMORTIZADO", 355f, curY + 12f, paint)
                        c.drawText("JUROS (RECEITA)", 440f, curY + 12f, paint)
                        c.drawText("TOTAL PAGO", 515f, curY + 12f, paint)
                    }

                    drawPaymentOnlyHeader(canvas, y)
                    y += 20f

                    listaPags.forEachIndexed { idx, pag ->
                        val rowHeight = 18f
                        if (y + rowHeight > MAX_CONTENT_Y) {
                            startNewPage()
                            y = 75f
                            drawPaymentOnlyHeader(canvas, y)
                            y += 20f
                        }

                        if (idx % 2 == 1) {
                            paint.color = lightRowAlt
                            canvas.drawRect(MARGIN_LEFT, y - 10f, MARGIN_RIGHT, y + 7f, paint)
                        }

                        val cli = clientesWithCredito.find { it.first.id == pag.clienteId }?.first
                        val crd = clientesWithCredito.find { it.second?.id == pag.creditoId }?.second

                        val cap = if (crd != null && crd.quantidadePrestacoes > 0) {
                            crd.valorDesembolsado / crd.quantidadePrestacoes
                        } else pag.valorPago * 0.5263
                        val jur = (pag.valorPago - cap).coerceAtLeast(0.0)

                        paint.color = darkText
                        paint.textSize = 7.5f
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        canvas.drawText(KwanzaFormatters.formatDate(pag.dataPagamento), 25f, y, paint)

                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        canvas.drawText(KwanzaFormatters.formatPrimeiroEUltimoNome(cli?.nome ?: "Cliente #${pag.clienteId}").take(18), 85f, y, paint)

                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        canvas.drawText("Nº ${pag.numeroPrestacao}", 205f, y, paint)
                        canvas.drawText(pag.comprovativoRef.ifEmpty { "Transf" }.take(14), 250f, y, paint)

                        paint.color = forestGreen
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        canvas.drawText(KwanzaFormatters.formatAOA(cap), 355f, y, paint)

                        paint.color = Color.parseColor("#B8860B")
                        canvas.drawText(KwanzaFormatters.formatAOA(jur), 440f, y, paint)

                        paint.color = deepBlue
                        canvas.drawText(KwanzaFormatters.formatAOA(pag.valorPago), 515f, y, paint)

                        y += rowHeight
                    }

                    if (y + 24f > MAX_CONTENT_Y) {
                        startNewPage()
                        y = 75f
                    }
                    paint.color = deepBlue
                    canvas.drawRect(MARGIN_LEFT, y - 6f, MARGIN_RIGHT, y + 16f, paint)
                    paint.color = Color.WHITE
                    paint.textSize = 8f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("TOTAIS RECEBIDOS (${listaPags.size} PAGAMENTOS)", 25f, y + 8f, paint)
                    canvas.drawText(KwanzaFormatters.formatAOA(totalCapAmort), 355f, y + 8f, paint)
                    canvas.drawText(KwanzaFormatters.formatAOA(totalJurosRec), 440f, y + 8f, paint)
                    canvas.drawText(KwanzaFormatters.formatAOA(totalGeralPago), 515f, y + 8f, paint)
                    y += 32f
                }

                TipoRelatorioPersonalizado.APENAS_DESEMBOLSOS -> {
                    val listaDesemb = clientesWithCredito.filter { (_, crd) ->
                        crd != null && (if (dataInicio > 0L || dataFim < Long.MAX_VALUE) crd.dataDesembolso in dataInicio..dataFim else true)
                    }.sortedByDescending { it.second?.dataDesembolso ?: 0L }

                    val totalDesembSoma = listaDesemb.sumOf { it.second?.valorDesembolsado ?: 0.0 }
                    val totalPrestSoma = listaDesemb.sumOf { it.second?.prestacaoMensal ?: 0.0 }

                    paint.color = lightBg
                    canvas.drawRoundRect(MARGIN_LEFT, y, MARGIN_RIGHT, y + 92f, 8f, 8f, paint)

                    paint.color = deepBlue
                    paint.textSize = 10.5f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("EXTRATO DE DESEMBOLSOS EFETUADOS NO PERÍODO", MARGIN_LEFT + 10f, y + 16f, paint)

                    paint.color = darkText
                    paint.textSize = 8.5f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    canvas.drawText("Total de Novos Financiamentos: ${listaDesemb.size}", MARGIN_LEFT + 10f, y + 34f, paint)

                    paint.color = forestGreen
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("TOTAL CAPITAL DESEMBOLSADO: ${KwanzaFormatters.formatAOA(totalDesembSoma)}", 260f, y + 34f, paint)

                    paint.color = darkText
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    val media = if (listaDesemb.isNotEmpty()) totalDesembSoma / listaDesemb.size else 0.0
                    canvas.drawText("Média por Contrato: ${KwanzaFormatters.formatAOA(media)}", MARGIN_LEFT + 10f, y + 52f, paint)
                    canvas.drawText("Total Prestações Mensais: ${KwanzaFormatters.formatAOA(totalPrestSoma)}", 260f, y + 52f, paint)

                    paint.color = grayText
                    paint.textSize = 7.5f
                    canvas.drawText("Regra de Aumento da Carteira: Cada novo desembolso aumenta a carteira apenas pelo capital efetivamente desembolsado.", MARGIN_LEFT + 10f, y + 72f, paint)

                    y += 106f

                    fun drawDisbursementHeader(c: Canvas, curY: Float) {
                        paint.color = deepBlue
                        c.drawRect(MARGIN_LEFT, curY, MARGIN_RIGHT, curY + 18f, paint)
                        paint.color = Color.WHITE
                        paint.textSize = 7.5f
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        c.drawText("DATA", 25f, curY + 12f, paint)
                        c.drawText("CLIENTE", 95f, curY + 12f, paint)
                        c.drawText("BANCO / CONTA", 220f, curY + 12f, paint)
                        c.drawText("CAPITAL CONCEDIDO", 310f, curY + 12f, paint)
                        c.drawText("TAXA MENSAL", 400f, curY + 12f, paint)
                        c.drawText("PRAZO", 465f, curY + 12f, paint)
                        c.drawText("PRESTAÇÃO", 515f, curY + 12f, paint)
                    }

                    drawDisbursementHeader(canvas, y)
                    y += 20f

                    listaDesemb.forEachIndexed { idx, (cli, crd) ->
                        val rowHeight = 18f
                        if (y + rowHeight > MAX_CONTENT_Y) {
                            startNewPage()
                            y = 75f
                            drawDisbursementHeader(canvas, y)
                            y += 20f
                        }

                        if (idx % 2 == 1) {
                            paint.color = lightRowAlt
                            canvas.drawRect(MARGIN_LEFT, y - 10f, MARGIN_RIGHT, y + 7f, paint)
                        }

                        paint.color = darkText
                        paint.textSize = 7.5f
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        canvas.drawText(KwanzaFormatters.formatDate(crd?.dataDesembolso ?: 0L), 25f, y, paint)

                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        canvas.drawText(KwanzaFormatters.formatPrimeiroEUltimoNome(cli.nome).take(20), 95f, y, paint)

                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        canvas.drawText("${cli.banco} (${cli.numeroConta.take(6)})", 220f, y, paint)

                        paint.color = forestGreen
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        canvas.drawText(KwanzaFormatters.formatAOA(crd?.valorDesembolsado ?: 0.0), 310f, y, paint)

                        paint.color = darkText
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        canvas.drawText("${crd?.taxaPercentual ?: 0.0}%", 400f, y, paint)
                        canvas.drawText("${crd?.quantidadePrestacoes ?: 0} meses", 465f, y, paint)

                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        canvas.drawText(KwanzaFormatters.formatAOA(crd?.prestacaoMensal ?: 0.0), 515f, y, paint)

                        y += rowHeight
                    }

                    if (y + 24f > MAX_CONTENT_Y) {
                        startNewPage()
                        y = 75f
                    }
                    paint.color = deepBlue
                    canvas.drawRect(MARGIN_LEFT, y - 6f, MARGIN_RIGHT, y + 16f, paint)
                    paint.color = Color.WHITE
                    paint.textSize = 8f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("TOTAL DE DESEMBOLSOS (${listaDesemb.size} CONTRATOS)", 25f, y + 8f, paint)
                    canvas.drawText(KwanzaFormatters.formatAOA(totalDesembSoma), 310f, y + 8f, paint)
                    canvas.drawText(KwanzaFormatters.formatAOA(totalPrestSoma), 515f, y + 8f, paint)
                    y += 32f
                }

                TipoRelatorioPersonalizado.GERAL -> {}
            }

            if (y + 80f > MAX_CONTENT_Y) {
                startNewPage()
                y = 75f
            }

            y += 15f
            paint.color = grayText
            paint.strokeWidth = 1f
            canvas.drawLine(50f, y + 35f, 250f, y + 35f, paint)
            canvas.drawLine(340f, y + 35f, 540f, y + 35f, paint)

            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Gestor de Crédito: Nduwa Kayongo", 60f, y + 50f, paint)
            canvas.drawText("Validação e Fecho do Período", 370f, y + 50f, paint)

            drawFooter(canvas, pageNumber)
            pdfDocument.finishPage(currentPage)

            val safePeriodoName = periodoDescricao.replace("[^a-zA-Z0-9_]".toRegex(), "_").take(30)
            val file = File(context.cacheDir, "Relatorio_${tipo.codigo}_${safePeriodoName}.pdf")
            FileOutputStream(file).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            pdfDocument.close()

            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Generates a multi-page individual Client Statement PDF with unlimited dynamic pages.
     */
    fun generateExtratoClientePdf(
        context: Context,
        cliente: ClienteEntity,
        credito: CreditoEntity?,
        pagamentos: List<PagamentoEntity>,
        cobrancas: List<CobrancaEntity>
    ): File? {
        try {
            val pdfDocument = PdfDocument()
            val paint = Paint().apply { isAntiAlias = true }

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            var currentPage = pdfDocument.startPage(pageInfo)
            var canvas = currentPage.canvas

            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val emissaoStr = dateFormat.format(Date())

            fun drawHeader(c: Canvas, pNum: Int, isFirst: Boolean) {
                val headerHeight = if (isFirst) 90f else 62f
                paint.color = deepBlue
                c.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), headerHeight, paint)

                paint.color = accentGold
                c.drawRect(0f, headerHeight - 4f, PAGE_WIDTH.toFloat(), headerHeight, paint)

                paint.color = Color.WHITE
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = if (isFirst) 16f else 13f
                c.drawText("GESTÃO DE CLIENTES — NDUWA KAYONGO", MARGIN_LEFT, if (isFirst) 32f else 26f, paint)

                paint.textSize = if (isFirst) 11f else 9.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                c.drawText("FICHA DO CLIENTE E EXTRATO OFICIAL DE CRÉDITO", MARGIN_LEFT, if (isFirst) 50f else 42f, paint)

                paint.textSize = 9f
                paint.color = Color.WHITE
                if (isFirst) {
                    paint.color = accentGold
                    c.drawText("Cliente: ${KwanzaFormatters.formatPrimeiroEUltimoNome(cliente.nome)}", MARGIN_LEFT, 72f, paint)
                    paint.color = Color.WHITE
                    c.drawText("Emissão: $emissaoStr", 410f, 72f, paint)
                } else {
                    paint.color = accentGold
                    c.drawText("Cliente: ${KwanzaFormatters.formatPrimeiroEUltimoNome(cliente.nome)}  •  Emissão: $emissaoStr", MARGIN_LEFT, 55f, paint)
                }
            }

            fun drawFooter(c: Canvas, pNum: Int) {
                paint.color = dividerColor
                c.drawLine(MARGIN_LEFT, FOOTER_Y - 10f, MARGIN_RIGHT, FOOTER_Y - 10f, paint)

                paint.color = grayText
                paint.textSize = 8.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                c.drawText("Gestão Inteligente de Crédito • Nduwa Kayongo", MARGIN_LEFT, FOOTER_Y + 4f, paint)
                c.drawText("Página $pNum", 525f, FOOTER_Y + 4f, paint)
            }

            fun startNewPage() {
                drawFooter(canvas, pageNumber)
                pdfDocument.finishPage(currentPage)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                currentPage = pdfDocument.startPage(pageInfo)
                canvas = currentPage.canvas
                drawHeader(canvas, pageNumber, isFirst = false)
            }

            drawHeader(canvas, pageNumber, isFirst = true)
            var y = 115f

            // Section 1: Dados do Cliente
            paint.color = deepBlue
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 12f
            canvas.drawText("1. DADOS PESSOAIS E BANCÁRIOS", MARGIN_LEFT, y, paint)
            y += 6f
            paint.color = accentGold
            canvas.drawLine(MARGIN_LEFT, y, MARGIN_RIGHT, y, paint)
            y += 18f

            paint.color = darkText
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            canvas.drawText("NOME DO CLIENTE: ${KwanzaFormatters.formatPrimeiroEUltimoNome(cliente.nome)}", MARGIN_LEFT, y, paint)
            canvas.drawText("Nº Interno: ${cliente.numeroInterno}", 350f, y, paint)
            y += 16f

            canvas.drawText("TELEFONE: ${cliente.telefone}", MARGIN_LEFT, y, paint)
            canvas.drawText("Sexo: ${if (cliente.sexo == "M") "Masculino" else "Feminino"}", 350f, y, paint)
            y += 16f

            canvas.drawText("Empresa / Empregador: ${cliente.empresa.ifEmpty { "N/A" }}", MARGIN_LEFT, y, paint)
            canvas.drawText("NIF: ${cliente.nif.ifEmpty { "N/A" }}", 350f, y, paint)
            y += 16f

            canvas.drawText("Província/Município: ${cliente.provincia} / ${cliente.municipio}", MARGIN_LEFT, y, paint)
            canvas.drawText("Salário: ${KwanzaFormatters.formatAOA(cliente.salario)}", 350f, y, paint)
            y += 16f

            canvas.drawText("BANCO: ${cliente.banco}", MARGIN_LEFT, y, paint)
            canvas.drawText("Nº Conta: ${cliente.numeroConta.ifEmpty { "N/A" }}", 200f, y, paint)
            canvas.drawText("IBAN: ${cliente.iban.ifEmpty { "N/A" }}", 380f, y, paint)
            y += 26f

            // Section 2: Resumo do Empréstimo
            paint.color = deepBlue
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 12f
            canvas.drawText("2. RESUMO DO CRÉDITO E FINANCIAMENTO", MARGIN_LEFT, y, paint)
            y += 6f
            paint.color = accentGold
            canvas.drawLine(MARGIN_LEFT, y, MARGIN_RIGHT, y, paint)
            y += 18f

            if (credito != null) {
                val nTotal = credito.quantidadePrestacoes.coerceAtLeast(1)
                val capMensal = credito.valorDesembolsado / nTotal
                val jurMensal = credito.valorDesembolsado * (credito.taxaPercentual / 100.0)
                val pagas = credito.prestacoesPagas.coerceIn(0, nTotal)
                val capAmortizado = capMensal * pagas
                val jurosRecebidos = jurMensal * pagas
                val totalRecebido = capAmortizado + jurosRecebidos
                val saldoCapitalCarteira = (credito.valorDesembolsado - capAmortizado).coerceAtLeast(0.0)

                paint.color = lightBg
                canvas.drawRoundRect(MARGIN_LEFT, y - 8f, MARGIN_RIGHT, y + 150f, 8f, 8f, paint)

                paint.color = darkText
                paint.textSize = 9.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                canvas.drawText("Capital Desembolsado: ${KwanzaFormatters.formatAOA(credito.valorDesembolsado)}", MARGIN_LEFT + 10f, y + 8f, paint)
                canvas.drawText("Taxa de Juros: ${credito.taxaPercentual}% ao mês", 320f, y + 8f, paint)

                canvas.drawText("Prazo: ${credito.quantidadePrestacoes} meses", MARGIN_LEFT + 10f, y + 24f, paint)
                canvas.drawText("PRESTAÇÃO MENSAL: ${KwanzaFormatters.formatAOA(credito.prestacaoMensal)}", 320f, y + 24f, paint)

                canvas.drawText("Prestações Pagas: $pagas de $nTotal (${(pagas * 100) / nTotal}%)", MARGIN_LEFT + 10f, y + 40f, paint)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.color = if (credito.estado == "Em atraso") warningRed else forestGreen
                canvas.drawText("Estado do Crédito: ${credito.estado}", 320f, y + 40f, paint)

                // COMPOSIÇÃO DA PRESTAÇÃO (ITEM 9)
                paint.color = deepBlue
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 9f
                canvas.drawText("COMPOSIÇÃO DA PRESTAÇÃO:", MARGIN_LEFT + 10f, y + 58f, paint)
                paint.color = forestGreen
                canvas.drawText("Prestação mensal = Capital mensal (${KwanzaFormatters.formatAOA(capMensal)}) + Juros mensal (${KwanzaFormatters.formatAOA(jurMensal)})", MARGIN_LEFT + 10f, y + 72f, paint)

                paint.color = darkText
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.textSize = 8.5f
                canvas.drawText("Capital Amortizado: ${KwanzaFormatters.formatAOA(capAmortizado)}", MARGIN_LEFT + 10f, y + 88f, paint)
                canvas.drawText("Juros Recebidos (Receita): ${KwanzaFormatters.formatAOA(jurosRecebidos)}", 320f, y + 88f, paint)

                canvas.drawText("Total Pago (Cap + Jur): ${KwanzaFormatters.formatAOA(totalRecebido)}", MARGIN_LEFT + 10f, y + 104f, paint)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.color = deepBlue
                canvas.drawText("SALDO DE CAPITAL DA CARTEIRA: ${KwanzaFormatters.formatAOA(saldoCapitalCarteira)}", 320f, y + 104f, paint)

                paint.color = darkText
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                val proximoFormatado = KwanzaFormatters.formatDate(credito.proximoVencimento)
                canvas.drawText("PRÓXIMO REEMBOLSO: $proximoFormatado", MARGIN_LEFT + 10f, y + 120f, paint)

                paint.color = Color.parseColor("#B45309")
                paint.textSize = 7.5f
                canvas.drawText("Nota Oficial: O valor da prestação contém capital + juros, mas somente o capital amortizado reduz a carteira de crédito.", MARGIN_LEFT + 10f, y + 134f, paint)

                y += 162f
            } else {
                canvas.drawText("Nenhum contrato de crédito ativo.", MARGIN_LEFT, y, paint)
                y += 24f
            }

            // Section 3: Histórico de Pagamentos (Multi-page enabled com separação de Capital e Juros)
            fun drawPagamentosHeader(c: Canvas, curY: Float) {
                paint.color = deepBlue
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 12f
                c.drawText("3. HISTÓRICO DE REEMBOLSOS E EVOLUÇÃO DO CAPITAL", MARGIN_LEFT, curY, paint)
                paint.color = accentGold
                c.drawLine(MARGIN_LEFT, curY + 6f, MARGIN_RIGHT, curY + 6f, paint)

                val headerY = curY + 12f
                paint.color = deepBlue
                c.drawRect(MARGIN_LEFT, headerY, MARGIN_RIGHT, headerY + 18f, paint)

                paint.color = Color.WHITE
                paint.textSize = 7.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                c.drawText("Nº", 25f, headerY + 12f, paint)
                c.drawText("DATA", 48f, headerY + 12f, paint)
                c.drawText("PRESTAÇÃO", 125f, headerY + 12f, paint)
                c.drawText("CAPITAL", 215f, headerY + 12f, paint)
                c.drawText("JUROS", 305f, headerY + 12f, paint)
                c.drawText("CAPITAL EM DÍVIDA", 395f, headerY + 12f, paint)
                c.drawText("REF", 515f, headerY + 12f, paint)
            }

            drawPagamentosHeader(canvas, y)
            y += 34f

            if (pagamentos.isEmpty()) {
                paint.color = darkText
                paint.textSize = 9.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText("Nenhum reembolso registrado até o momento.", 30f, y, paint)
                y += 20f
            } else {
                var totalPago = 0.0
                var totalCapAmortizado = 0.0
                var totalJurosRecebidos = 0.0
                val nTotal = credito?.quantidadePrestacoes ?: 1
                val capMensalRef = (credito?.valorDesembolsado ?: 0.0) / nTotal.coerceAtLeast(1)
                val jurMensalRef = (credito?.valorDesembolsado ?: 0.0) * ((credito?.taxaPercentual ?: 0.0) / 100.0)
                var saldoCapitalRestante = credito?.valorDesembolsado ?: 0.0

                pagamentos.forEachIndexed { idx, pag ->
                    val rowHeight = 17f
                    if (y + rowHeight > MAX_CONTENT_Y) {
                        startNewPage()
                        y = 75f
                        drawPagamentosHeader(canvas, y)
                        y += 34f
                    }

                    if (idx % 2 == 1) {
                        paint.color = lightRowAlt
                        canvas.drawRect(MARGIN_LEFT, y - 10f, MARGIN_RIGHT, y + 7f, paint)
                    }

                    val parcelaCap = if (credito != null) capMensalRef else (pag.valorPago * 0.5)
                    val parcelaJur = if (credito != null) jurMensalRef else (pag.valorPago * 0.5)
                    saldoCapitalRestante = (saldoCapitalRestante - parcelaCap).coerceAtLeast(0.0)

                    paint.color = darkText
                    paint.textSize = 8f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    canvas.drawText("${pag.numeroPrestacao}ª", 25f, y, paint)
                    canvas.drawText(KwanzaFormatters.formatDate(pag.dataPagamento), 48f, y, paint)
                    canvas.drawText(KwanzaFormatters.formatAOA(pag.valorPago), 125f, y, paint)
                    canvas.drawText(KwanzaFormatters.formatAOA(parcelaCap), 215f, y, paint)
                    canvas.drawText(KwanzaFormatters.formatAOA(parcelaJur), 305f, y, paint)

                    paint.color = forestGreen
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText(KwanzaFormatters.formatAOA(saldoCapitalRestante), 395f, y, paint)

                    paint.color = grayText
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    canvas.drawText(pag.comprovativoRef.ifEmpty { "Transf" }.take(8), 515f, y, paint)

                    totalPago += pag.valorPago
                    totalCapAmortizado += parcelaCap
                    totalJurosRecebidos += parcelaJur
                    y += rowHeight
                }

                // Total Row
                if (y + 24f > MAX_CONTENT_Y) {
                    startNewPage()
                    y = 75f
                }
                paint.color = deepBlue
                canvas.drawRect(MARGIN_LEFT, y - 6f, MARGIN_RIGHT, y + 16f, paint)
                paint.color = Color.WHITE
                paint.textSize = 8f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("TOTAIS", 25f, y + 8f, paint)
                canvas.drawText(KwanzaFormatters.formatAOA(totalPago), 125f, y + 8f, paint)
                canvas.drawText(KwanzaFormatters.formatAOA(totalCapAmortizado), 215f, y + 8f, paint)
                canvas.drawText(KwanzaFormatters.formatAOA(totalJurosRecebidos), 305f, y + 8f, paint)
                canvas.drawText(KwanzaFormatters.formatAOA(saldoCapitalRestante), 395f, y + 8f, paint)
                y += 32f
            }

            // Signatures
            if (y + 70f > MAX_CONTENT_Y) {
                startNewPage()
                y = 75f
            }

            y += 15f
            paint.color = grayText
            canvas.drawLine(50f, y + 30f, 250f, y + 30f, paint)
            canvas.drawLine(340f, y + 30f, 540f, y + 30f, paint)
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Assinatura do Gestor de Crédito", 65f, y + 45f, paint)
            canvas.drawText("Assinatura do Cliente", 380f, y + 45f, paint)

            drawFooter(canvas, pageNumber)
            pdfDocument.finishPage(currentPage)

            val safeName = cliente.nome.replace("[^a-zA-Z0-9_]".toRegex(), "_").take(30)
            val file = File(context.cacheDir, "Extrato_${safeName}.pdf")
            FileOutputStream(file).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            pdfDocument.close()

            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Gera o Relatório Oficial de Inadimplência e Clientes com Pendências Financeiras em PDF.
     * Inclui detalhamento de dias em atraso, juros de mora a 4%/dia, prestação e total devido.
     */
    fun generateRelatorioPendenciasPdf(
        context: Context,
        clientesWithCredito: List<Pair<ClienteEntity, CreditoEntity?>>,
        summary: DashboardSummary? = null
    ): File? {
        val pdfDocument = PdfDocument()
        try {
            val paint = Paint().apply { isAntiAlias = true }

            // Filtrar apenas clientes com pendência ou em atraso
            val pendencias = clientesWithCredito.filter { (_, credito) ->
                credito != null && (credito.estado == "Em atraso" || credito.diasEmAtraso > 0 || (credito.proximoVencimento < System.currentTimeMillis() && credito.estado != "Liquidado"))
            }

            val listaFinal = if (pendencias.isNotEmpty()) pendencias else clientesWithCredito.filter { it.second?.estado != "Liquidado" }

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            var currentPage = pdfDocument.startPage(pageInfo)
            var canvas = currentPage.canvas

            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val emissaoDataStr = dateFormat.format(Date())

            fun drawHeader(c: Canvas, pNum: Int, isFirst: Boolean) {
                val headerHeight = if (isFirst) 95f else 62f
                paint.color = deepBlue
                c.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), headerHeight, paint)

                paint.color = warningRed
                c.drawRect(0f, headerHeight - 4f, PAGE_WIDTH.toFloat(), headerHeight, paint)

                paint.color = Color.WHITE
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = if (isFirst) 15f else 12f
                c.drawText("MBULI-MICROCRÉDITO — GESTÃO DE COBRANÇAS", MARGIN_LEFT, if (isFirst) 30f else 25f, paint)

                paint.textSize = if (isFirst) 11f else 9.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                c.drawText("RELATÓRIO OFICIAL DE PENDÊNCIAS FINANCEIRAS E INADIMPLÊNCIA", MARGIN_LEFT, if (isFirst) 48f else 40f, paint)

                paint.textSize = 8.5f
                paint.color = accentGold
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                if (isFirst) {
                    c.drawText("Gestor Responsável: Nduwa Kayongo", MARGIN_LEFT, 68f, paint)
                    paint.color = Color.WHITE
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    c.drawText("Data de Emissão: $emissaoDataStr", 380f, 68f, paint)
                    paint.color = Color.parseColor("#FFCDD2")
                    c.drawText("⚠️ Documento Confidencial de Gestão e Cobrança", MARGIN_LEFT, 82f, paint)
                } else {
                    c.drawText("Nduwa Kayongo  •  Emissão: $emissaoDataStr  •  Pendências Financeiras", MARGIN_LEFT, 54f, paint)
                }
            }

            fun drawFooter(c: Canvas, pNum: Int) {
                paint.color = dividerColor
                c.drawLine(MARGIN_LEFT, FOOTER_Y - 10f, MARGIN_RIGHT, FOOTER_Y - 10f, paint)

                paint.color = grayText
                paint.textSize = 8.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                c.drawText("Mbuli-Microcrédito • Gestão de Carteira • Nduwa Kayongo", MARGIN_LEFT, FOOTER_Y + 4f, paint)
                c.drawText("Página $pNum", 525f, FOOTER_Y + 4f, paint)
            }

            fun startNewPage() {
                drawFooter(canvas, pageNumber)
                pdfDocument.finishPage(currentPage)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                currentPage = pdfDocument.startPage(pageInfo)
                canvas = currentPage.canvas
                drawHeader(canvas, pageNumber, isFirst = false)
            }

            drawHeader(canvas, pageNumber, isFirst = true)
            var y = 110f

            // Resumo de Inadimplência no Topo
            val totalEmAtraso = listaFinal.sumOf { it.second?.valorEmAtraso ?: 0.0 }
            val totalPrestacoes = listaFinal.sumOf { it.second?.prestacaoMensal ?: 0.0 }
            val totalJurosMoraCalculado = listaFinal.sumOf { (_, credito) ->
                if (credito != null) {
                    val dias = if (credito.diasEmAtraso > 0) credito.diasEmAtraso else 1
                    val res = MoraCalculatorHelper.calcular(credito = credito, diasAtrasoManual = dias)
                    res.jurosMoraAcumulados
                } else 0.0
            }
            val totalGlobalDevido = listaFinal.sumOf { (_, credito) ->
                if (credito != null) {
                    val dias = if (credito.diasEmAtraso > 0) credito.diasEmAtraso else 1
                    val res = MoraCalculatorHelper.calcular(credito = credito, diasAtrasoManual = dias)
                    res.valorTotalEmDivida
                } else 0.0
            }

            paint.color = Color.parseColor("#FFF5F5")
            canvas.drawRoundRect(MARGIN_LEFT, y, MARGIN_RIGHT, y + 80f, 8f, 8f, paint)
            paint.color = Color.parseColor("#FFCDD2")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawRoundRect(MARGIN_LEFT, y, MARGIN_RIGHT, y + 80f, 8f, 8f, paint)
            paint.style = Paint.Style.FILL

            paint.color = warningRed
            paint.textSize = 10.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("QUADRO CONSOLIDADO DE INADIMPLÊNCIA & COBRANÇA", MARGIN_LEFT + 10f, y + 16f, paint)

            paint.color = darkText
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Total de Clientes em Pendência: ${listaFinal.size}", MARGIN_LEFT + 10f, y + 34f, paint)
            canvas.drawText("Total Capital em Atraso: ${KwanzaFormatters.formatAOA(totalEmAtraso)}", 300f, y + 34f, paint)

            canvas.drawText("Soma das Prestações Devidas: ${KwanzaFormatters.formatAOA(totalPrestacoes)}", MARGIN_LEFT + 10f, y + 50f, paint)
            canvas.drawText("Juros de Mora Acumulados (4%/dia): ${KwanzaFormatters.formatAOA(totalJurosMoraCalculado)}", 300f, y + 50f, paint)

            paint.color = deepBlue
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("VALOR TOTAL A RECUPERAR (c/ Multas de Mora): ${KwanzaFormatters.formatAOA(totalGlobalDevido)}", MARGIN_LEFT + 10f, y + 68f, paint)

            y += 95f

            // Cabeçalho da Tabela
            paint.color = deepBlue
            canvas.drawRect(MARGIN_LEFT, y, MARGIN_RIGHT, y + 22f, paint)

            paint.color = Color.WHITE
            paint.textSize = 7.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

            canvas.drawText("#", MARGIN_LEFT + 4f, y + 14f, paint)
            canvas.drawText("CLIENTE / CONTACTO", MARGIN_LEFT + 22f, y + 14f, paint)
            canvas.drawText("BANCO", MARGIN_LEFT + 160f, y + 14f, paint)
            canvas.drawText("VENCIMENTO", MARGIN_LEFT + 225f, y + 14f, paint)
            canvas.drawText("ATRASO", MARGIN_LEFT + 295f, y + 14f, paint)
            canvas.drawText("PRESTAÇÃO", MARGIN_LEFT + 345f, y + 14f, paint)
            canvas.drawText("MORA 4%/D", MARGIN_LEFT + 420f, y + 14f, paint)
            canvas.drawText("TOTAL A PAGAR", MARGIN_LEFT + 485f, y + 14f, paint)

            y += 22f

            listaFinal.forEachIndexed { index, (cliente, credito) ->
                val rowHeight = 24f
                if (y + rowHeight > MAX_CONTENT_Y) {
                    startNewPage()
                    y = 75f

                    // Repetir cabeçalho
                    paint.color = deepBlue
                    canvas.drawRect(MARGIN_LEFT, y, MARGIN_RIGHT, y + 20f, paint)
                    paint.color = Color.WHITE
                    paint.textSize = 7.5f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("#", MARGIN_LEFT + 4f, y + 13f, paint)
                    canvas.drawText("CLIENTE / CONTACTO", MARGIN_LEFT + 22f, y + 13f, paint)
                    canvas.drawText("BANCO", MARGIN_LEFT + 160f, y + 13f, paint)
                    canvas.drawText("VENCIMENTO", MARGIN_LEFT + 225f, y + 13f, paint)
                    canvas.drawText("ATRASO", MARGIN_LEFT + 295f, y + 13f, paint)
                    canvas.drawText("PRESTAÇÃO", MARGIN_LEFT + 345f, y + 13f, paint)
                    canvas.drawText("MORA 4%/D", MARGIN_LEFT + 420f, y + 13f, paint)
                    canvas.drawText("TOTAL A PAGAR", MARGIN_LEFT + 485f, y + 13f, paint)
                    y += 20f
                }

                // Linha zebrada
                if (index % 2 == 1) {
                    paint.color = lightRowAlt
                    canvas.drawRect(MARGIN_LEFT, y, MARGIN_RIGHT, y + rowHeight, paint)
                }

                val dias = credito?.diasEmAtraso ?: 0
                val prestacao = credito?.prestacaoMensal ?: 0.0
                val diasCalculo = if (dias > 0) dias else 1
                val moraResultado = if (credito != null) MoraCalculatorHelper.calcular(credito = credito, diasAtrasoManual = diasCalculo) else null
                val mora = moraResultado?.jurosMoraAcumulados ?: (prestacao * 0.04 * diasCalculo)
                val totalCliente = moraResultado?.valorTotalEmDivida ?: (prestacao + mora)

                paint.color = darkText
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.textSize = 7.5f

                // Index
                canvas.drawText("${index + 1}", MARGIN_LEFT + 4f, y + 14f, paint)

                // Nome + Telefone
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val nomeCurto = KwanzaFormatters.formatPrimeiroEUltimoNome(cliente.nome).take(22)
                canvas.drawText(nomeCurto, MARGIN_LEFT + 22f, y + 10f, paint)

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = grayText
                paint.textSize = 6.8f
                canvas.drawText(cliente.telefone, MARGIN_LEFT + 22f, y + 19f, paint)

                // Banco
                paint.color = darkText
                paint.textSize = 7.5f
                canvas.drawText(cliente.banco.take(12), MARGIN_LEFT + 160f, y + 14f, paint)

                // Data Vencimento
                val vencStr = if (credito != null) KwanzaFormatters.formatDate(credito.proximoVencimento) else "-"
                canvas.drawText(vencStr, MARGIN_LEFT + 225f, y + 14f, paint)

                // Dias em atraso
                paint.color = if (dias > 0) warningRed else forestGreen
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val atrasoStr = if (dias > 0) "$dias dias" else "No prazo"
                canvas.drawText(atrasoStr, MARGIN_LEFT + 295f, y + 14f, paint)

                // Prestação
                paint.color = darkText
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText(KwanzaFormatters.formatAOA(prestacao), MARGIN_LEFT + 345f, y + 14f, paint)

                // Mora
                paint.color = warningRed
                canvas.drawText(KwanzaFormatters.formatAOA(mora), MARGIN_LEFT + 420f, y + 14f, paint)

                // Total
                paint.color = deepBlue
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(KwanzaFormatters.formatAOA(totalCliente), MARGIN_LEFT + 485f, y + 14f, paint)

                // Linha divisória fina
                paint.color = dividerColor
                canvas.drawLine(MARGIN_LEFT, y + rowHeight, MARGIN_RIGHT, y + rowHeight, paint)

                y += rowHeight
            }

            // Totais Finais
            if (y + 60f > MAX_CONTENT_Y) {
                startNewPage()
                y = 75f
            }

            y += 10f
            paint.color = deepBlue
            canvas.drawRect(MARGIN_LEFT, y, MARGIN_RIGHT, y + 24f, paint)

            paint.color = Color.WHITE
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("TOTAL GERAL DE PENDÊNCIAS", MARGIN_LEFT + 10f, y + 16f, paint)
            canvas.drawText(KwanzaFormatters.formatAOA(totalPrestacoes), MARGIN_LEFT + 345f, y + 16f, paint)
            paint.color = accentGold
            canvas.drawText(KwanzaFormatters.formatAOA(totalJurosMoraCalculado), MARGIN_LEFT + 420f, y + 16f, paint)
            paint.color = Color.WHITE
            canvas.drawText(KwanzaFormatters.formatAOA(totalGlobalDevido), MARGIN_LEFT + 485f, y + 16f, paint)

            y += 35f

            // Instruções de Cobrança e Assinatura
            if (y + 70f > MAX_CONTENT_Y) {
                startNewPage()
                y = 75f
            }

            paint.color = darkText
            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas.drawText("Nota: Valores de juros de mora calculados à taxa padrão de 4% ao dia sobre cada prestação em atraso.", MARGIN_LEFT, y + 10f, paint)
            canvas.drawText("Mbuli-Microcrédito • Cobrança Amigável e Notificações Judiciais.", MARGIN_LEFT, y + 22f, paint)

            y += 35f
            paint.color = grayText
            canvas.drawLine(MARGIN_LEFT + 40f, y + 25f, MARGIN_LEFT + 220f, y + 25f, paint)
            canvas.drawLine(MARGIN_LEFT + 320f, y + 25f, MARGIN_RIGHT - 40f, y + 25f, paint)

            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Nduwa Kayongo (Gestor de Crédito)", MARGIN_LEFT + 45f, y + 38f, paint)
            canvas.drawText("Direção / Gestão Financeira", MARGIN_LEFT + 345f, y + 38f, paint)

            drawFooter(canvas, pageNumber)
            pdfDocument.finishPage(currentPage)

            val file = File(context.cacheDir, "Relatorio_Pendencias_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            try { pdfDocument.close() } catch (_: Exception) {}
            return null
        }
    }

    /**
     * Generates a complete Simulation & Installment Schedule Report in PDF up to total liquidation.
     */
    fun generateSimulacaoCreditoPdf(
        context: Context,
        state: CalculoSaldoDevedorState,
        nomeCliente: String? = null
    ): File? {
        val pdfDocument = PdfDocument()
        val paint = Paint().apply { isAntiAlias = true }
        try {
            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            var currentPage = pdfDocument.startPage(pageInfo)
            var canvas = currentPage.canvas

            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val emissaoDataStr = dateFormat.format(Date())

            fun drawHeader(c: Canvas, pNum: Int, isFirst: Boolean) {
                val headerHeight = if (isFirst) 88f else 62f
                paint.color = deepBlue
                c.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), headerHeight, paint)

                paint.color = accentGold
                c.drawRect(0f, headerHeight - 4f, PAGE_WIDTH.toFloat(), headerHeight, paint)

                paint.color = Color.WHITE
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = if (isFirst) 16f else 13f
                c.drawText("GESTÃO DE CLIENTES — NDUWA KAYONGO", MARGIN_LEFT, if (isFirst) 30f else 26f, paint)

                paint.textSize = if (isFirst) 11f else 9.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                c.drawText("SIMULAÇÃO DE CRÉDITO E TABELA DE AMORTIZAÇÃO COMPLETA", MARGIN_LEFT, if (isFirst) 48f else 42f, paint)

                paint.textSize = 9f
                if (isFirst) {
                    paint.color = accentGold
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    val clienteStr = if (!nomeCliente.isNullOrBlank()) "Cliente: $nomeCliente  •  " else ""
                    c.drawText("${clienteStr}Sistema: ${state.tipoCalculo.label}", MARGIN_LEFT, 70f, paint)

                    paint.color = Color.WHITE
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    c.drawText("Emissão: $emissaoDataStr", 410f, 70f, paint)
                } else {
                    paint.color = accentGold
                    c.drawText("Tabela de Parcelas até Liquidação Total  •  Emissão: $emissaoDataStr", MARGIN_LEFT, 55f, paint)
                }
            }

            fun drawFooter(c: Canvas, pNum: Int) {
                paint.color = dividerColor
                c.drawLine(MARGIN_LEFT, FOOTER_Y - 10f, MARGIN_RIGHT, FOOTER_Y - 10f, paint)

                paint.color = grayText
                paint.textSize = 8.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                c.drawText("Mbuli-Microcrédito • Gestor: Nduwa Kayongo • Documento Oficial de Simulação", MARGIN_LEFT, FOOTER_Y + 4f, paint)
                c.drawText("Página $pNum", 525f, FOOTER_Y + 4f, paint)
            }

            fun startNewPage() {
                drawFooter(canvas, pageNumber)
                pdfDocument.finishPage(currentPage)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                currentPage = pdfDocument.startPage(pageInfo)
                canvas = currentPage.canvas
                drawHeader(canvas, pageNumber, isFirst = false)
            }

            drawHeader(canvas, pageNumber, isFirst = true)
            var y = 102f

            // Executive Summary Card
            paint.color = lightBg
            canvas.drawRoundRect(MARGIN_LEFT, y, MARGIN_RIGHT, y + 84f, 8f, 8f, paint)

            paint.color = deepBlue
            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("PARÂMETROS E RESUMO EXECUTIVO DO FINANCIAMENTO", MARGIN_LEFT + 10f, y + 15f, paint)

            paint.color = darkText
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            canvas.drawText("Capital Inicial (Desembolsado): ${KwanzaFormatters.formatAOA(state.principal)}", MARGIN_LEFT + 10f, y + 32f, paint)
            canvas.drawText("Prestação Mensal: ${KwanzaFormatters.formatAOA(state.prestacaoMensal)}", 300f, y + 32f, paint)

            canvas.drawText("Taxa de Juros: ${state.taxaPercentual}% ao mês", MARGIN_LEFT + 10f, y + 48f, paint)
            canvas.drawText("Total de Juros Projetado: ${KwanzaFormatters.formatAOA(state.valorJurosTotal)}", 300f, y + 48f, paint)

            canvas.drawText("Prazo do Crédito: ${state.prazoMeses} meses", MARGIN_LEFT + 10f, y + 64f, paint)
            canvas.drawText("Total a Liquidar: ${KwanzaFormatters.formatAOA(state.valorTotalLiquidar)}", 300f, y + 64f, paint)

            paint.color = forestGreen
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Capital Amortizado: ${KwanzaFormatters.formatAOA(state.capitalAmortizadoAteAgora)}  •  Saldo da Carteira: ${KwanzaFormatters.formatAOA(state.saldoDevedorAtual)} (${state.parcelasPagas}/${state.prazoMeses} pagas)", MARGIN_LEFT + 10f, y + 78f, paint)

            y += 94f

            // OBRIGATÓRIO (ITEM 9): SEÇÃO COMPOSIÇÃO DA PRESTAÇÃO
            paint.color = Color.parseColor("#F0FDF4") // Light mint
            canvas.drawRoundRect(MARGIN_LEFT, y, MARGIN_RIGHT, y + 64f, 8f, 8f, paint)
            paint.color = forestGreen
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawRoundRect(MARGIN_LEFT, y, MARGIN_RIGHT, y + 64f, 8f, 8f, paint)
            paint.style = Paint.Style.FILL

            paint.color = deepBlue
            paint.textSize = 10.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("COMPOSIÇÃO DA PRESTAÇÃO (REGRA DE OURO MBULI)", MARGIN_LEFT + 10f, y + 14f, paint)

            paint.color = forestGreen
            paint.textSize = 9.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Fórmula Obrigatória: Prestação mensal = Capital mensal + Juros mensal", MARGIN_LEFT + 10f, y + 27f, paint)

            paint.color = darkText
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val capMensalStr = KwanzaFormatters.formatAOA(state.capitalMensal)
            val jurMensalStr = KwanzaFormatters.formatAOA(state.jurosMensal)
            val prestMensalStr = KwanzaFormatters.formatAOA(state.prestacaoMensal)
            val princStr = KwanzaFormatters.formatAOA(state.principal)
            canvas.drawText("• Capital mensal = $princStr ÷ ${state.prazoMeses} = $capMensalStr", MARGIN_LEFT + 10f, y + 39f, paint)
            canvas.drawText("• Juros mensal = $princStr × ${state.taxaPercentual}% = $jurMensalStr", 280f, y + 39f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = deepBlue
            canvas.drawText("• Prestação = $capMensalStr + $jurMensalStr = $prestMensalStr", MARGIN_LEFT + 10f, y + 51f, paint)

            paint.color = Color.parseColor("#B45309") // Dark gold/amber
            paint.textSize = 7.5f
            canvas.drawText("O valor da prestação contém capital + juros, mas SOMENTE o capital amortizado reduz a carteira de crédito.", MARGIN_LEFT + 10f, y + 61f, paint)

            y += 74f

            fun drawTableHeader(c: Canvas, curY: Float) {
                paint.color = deepBlue
                paint.textSize = 12f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                c.drawText("CRONOGRAMA COMPLETO DE PARCELAS ATÉ À LIQUIDAÇÃO TOTAL", MARGIN_LEFT, curY, paint)

                paint.color = accentGold
                c.drawLine(MARGIN_LEFT, curY + 6f, MARGIN_RIGHT, curY + 6f, paint)

                val barY = curY + 12f
                paint.color = deepBlue
                c.drawRect(MARGIN_LEFT, barY, MARGIN_RIGHT, barY + 22f, paint)

                paint.color = Color.WHITE
                paint.textSize = 8.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

                c.drawText("Nº", MARGIN_LEFT + 5f, barY + 14f, paint)
                c.drawText("Prestação (AOA)", MARGIN_LEFT + 35f, barY + 14f, paint)
                c.drawText("Amortização (AOA)", MARGIN_LEFT + 145f, barY + 14f, paint)
                c.drawText("Juros (AOA)", MARGIN_LEFT + 255f, barY + 14f, paint)
                c.drawText("Saldo Restante (AOA)", MARGIN_LEFT + 350f, barY + 14f, paint)
                c.drawText("Situação", MARGIN_LEFT + 475f, barY + 14f, paint)
            }

            drawTableHeader(canvas, y)
            y += 38f

            var totalPrestacoesSoma = 0.0
            var totalAmortizacaoSoma = 0.0
            var totalJurosSoma = 0.0

            state.cronograma.forEachIndexed { index, item ->
                if (y > MAX_CONTENT_Y) {
                    startNewPage()
                    y = 75f
                    drawTableHeader(canvas, y)
                    y += 38f
                }

                totalPrestacoesSoma += item.valorPrestacao
                totalAmortizacaoSoma += item.valorAmortizacao
                totalJurosSoma += item.valorJuros

                if (index % 2 == 1) {
                    paint.color = lightRowAlt
                    canvas.drawRect(MARGIN_LEFT, y, MARGIN_RIGHT, y + 18f, paint)
                }

                paint.color = darkText
                paint.textSize = 8.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                canvas.drawText("${item.numeroParcela}", MARGIN_LEFT + 5f, y + 13f, paint)
                canvas.drawText(KwanzaFormatters.formatAOA(item.valorPrestacao), MARGIN_LEFT + 35f, y + 13f, paint)
                canvas.drawText(KwanzaFormatters.formatAOA(item.valorAmortizacao), MARGIN_LEFT + 145f, y + 13f, paint)
                canvas.drawText(KwanzaFormatters.formatAOA(item.valorJuros), MARGIN_LEFT + 255f, y + 13f, paint)
                canvas.drawText(KwanzaFormatters.formatAOA(item.saldoDevedorRestante), MARGIN_LEFT + 350f, y + 13f, paint)

                if (item.isPaga) {
                    paint.color = forestGreen
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("✓ Paga", MARGIN_LEFT + 475f, y + 13f, paint)
                } else {
                    paint.color = deepBlue
                    canvas.drawText("Pendente", MARGIN_LEFT + 475f, y + 13f, paint)
                }

                paint.color = dividerColor
                canvas.drawLine(MARGIN_LEFT, y + 18f, MARGIN_RIGHT, y + 18f, paint)

                y += 18f
            }

            // Totals Row
            if (y > MAX_CONTENT_Y - 30f) {
                startNewPage()
                y = 80f
            }

            paint.color = lightBg
            canvas.drawRect(MARGIN_LEFT, y, MARGIN_RIGHT, y + 22f, paint)

            paint.color = deepBlue
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 9f
            canvas.drawText("TOTAL", MARGIN_LEFT + 5f, y + 15f, paint)
            canvas.drawText(KwanzaFormatters.formatAOA(totalPrestacoesSoma), MARGIN_LEFT + 35f, y + 15f, paint)
            canvas.drawText(KwanzaFormatters.formatAOA(totalAmortizacaoSoma), MARGIN_LEFT + 145f, y + 15f, paint)
            canvas.drawText(KwanzaFormatters.formatAOA(totalJurosSoma), MARGIN_LEFT + 255f, y + 15f, paint)

            paint.color = forestGreen
            canvas.drawText("Liquidado: 0,00 Kz", MARGIN_LEFT + 350f, y + 15f, paint)
            canvas.drawText("100% Total", MARGIN_LEFT + 475f, y + 15f, paint)

            paint.color = deepBlue
            canvas.drawLine(MARGIN_LEFT, y + 22f, MARGIN_RIGHT, y + 22f, paint)

            y += 34f

            // Signatures & Official Disclaimer Box
            if (y > MAX_CONTENT_Y - 45f) {
                startNewPage()
                y = 80f
            }

            paint.color = grayText
            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas.drawText("Documento emitido oficialmente para fins de simulação e planeamento financeiro pela Mbuli-Microcrédito.", MARGIN_LEFT, y + 10f, paint)
            canvas.drawText("As condições refletem o plano de pagamento contratual até a sua liquidação total.", MARGIN_LEFT, y + 22f, paint)

            // Manager signature block
            paint.color = deepBlue
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 8.5f
            canvas.drawText("Nduwa Kayongo", 430f, y + 22f, paint)
            paint.color = grayText
            paint.textSize = 7.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Gestor de Microcrédito", 430f, y + 32f, paint)

            drawFooter(canvas, pageNumber)
            pdfDocument.finishPage(currentPage)

            val file = File(context.cacheDir, "simulacao_credito_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            try { pdfDocument.close() } catch (_: Exception) {}
            return null
        }
    }

    /**
     * Partilha o ficheiro PDF gerado diretamente por E-mail para o Gestor/Direção ou via Intent universal.
     */
    fun sharePdfViaEmail(
        context: Context,
        file: File,
        subject: String = "Relatório de Pendências Financeiras e Inadimplência — Mbuli-Microcrédito",
        bodyText: String = "Saudações,\n\nSegue em anexo o Relatório Oficial de Pendências Financeiras e Inadimplência de Clientes da Mbuli-Microcrédito, elaborado pelo Gestor Nduwa Kayongo.\n\nAtenciosamente,\nMbuli-Microcrédito",
        emailDestinatario: String? = null
    ) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                if (!emailDestinatario.isNullOrBlank()) {
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(emailDestinatario))
                }
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, bodyText)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(emailIntent, "Enviar Relatório por E-mail")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback para partilha universal
            sharePdf(context, file)
        }
    }

    fun sharePdf(context: Context, file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Partilhar Relatório em PDF")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Erro ao partilhar PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun openOrSharePdf(context: Context, file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Abrir Relatório PDF")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            sharePdf(context, file)
        }
    }
}
