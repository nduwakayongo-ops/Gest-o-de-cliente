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
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGeneratorHelper {

    fun generateExtratoClientePdf(
        context: Context,
        cliente: ClienteEntity,
        credito: CreditoEntity?,
        pagamentos: List<PagamentoEntity>,
        cobrancas: List<CobrancaEntity>
    ): File? {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint().apply {
                isAntiAlias = true
            }

            // Colors
            val deepBlue = Color.parseColor("#0A192F")
            val accentGold = Color.parseColor("#D4AF37")
            val darkText = Color.parseColor("#1E293B")
            val grayText = Color.parseColor("#64748B")
            val lightBg = Color.parseColor("#F8FAFC")
            val forestGreen = Color.parseColor("#2E7D32")
            val warningRed = Color.parseColor("#C62828")

            // Header Banner
            paint.color = deepBlue
            canvas.drawRect(0f, 0f, 595f, 100f, paint)

            paint.color = accentGold
            canvas.drawRect(0f, 96f, 595f, 100f, paint)

            // Header Title
            paint.color = Color.WHITE
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 18f
            canvas.drawText("GESTÃO DE CLIENTES — NDUWA KAYONGO", 20f, 40f, paint)

            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("FICHA DO CLIENTE E EXTRATO OFICIAL DE CRÉDITO", 20f, 65f, paint)

            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            canvas.drawText("Emissão: ${dateFormat.format(Date())}", 380f, 65f, paint)

            var y = 130f

            // Section 1: Dados do Cliente
            paint.color = deepBlue
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 14f
            canvas.drawText("1. DADOS PESSOAIS E BANCÁRIOS", 20f, y, paint)
            y += 8f
            paint.color = accentGold
            canvas.drawLine(20f, y, 575f, y, paint)
            y += 20f

            paint.color = darkText
            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            canvas.drawText("NOME DO CLIENTE: ${KwanzaFormatters.formatPrimeiroEUltimoNome(cliente.nome)}", 20f, y, paint)
            canvas.drawText("Nº Interno: ${cliente.numeroInterno}", 350f, y, paint)
            y += 18f

            canvas.drawText("TELEFONE: ${cliente.telefone}", 20f, y, paint)
            canvas.drawText("Sexo: ${if (cliente.sexo == "M") "Masculino" else "Feminino"}", 350f, y, paint)
            y += 18f

            canvas.drawText("Empresa / Empregador: ${cliente.empresa.ifEmpty { "N/A" }}", 20f, y, paint)
            canvas.drawText("NIF: ${cliente.nif.ifEmpty { "N/A" }}", 350f, y, paint)
            y += 18f

            canvas.drawText("Província/Município: ${cliente.provincia} / ${cliente.municipio}", 20f, y, paint)
            canvas.drawText("Salário: ${KwanzaFormatters.formatAOA(cliente.salario)}", 350f, y, paint)
            y += 18f

            canvas.drawText("BANCO: ${cliente.banco}", 20f, y, paint)
            canvas.drawText("Nº Conta: ${cliente.numeroConta.ifEmpty { "N/A" }}", 200f, y, paint)
            canvas.drawText("IBAN: ${cliente.iban.ifEmpty { "N/A" }}", 380f, y, paint)
            y += 30f

            // Section 2: Resumo do Empréstimo
            paint.color = deepBlue
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 14f
            canvas.drawText("2. RESUMO DO CRÉDITO E FINANCIAMENTO", 20f, y, paint)
            y += 8f
            paint.color = accentGold
            canvas.drawLine(20f, y, 575f, y, paint)
            y += 20f

            if (credito != null) {
                // Background Box for Loan Summary
                paint.color = lightBg
                canvas.drawRoundRect(20f, y - 10f, 575f, y + 85f, 8f, 8f, paint)

                paint.color = darkText
                paint.textSize = 11f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                canvas.drawText("VALOR RECEBIDO: ${KwanzaFormatters.formatAOA(credito.valorDesembolsado)}", 30f, y + 10f, paint)
                canvas.drawText("Taxa de Juros: ${credito.taxaPercentual}%", 320f, y + 10f, paint)

                val valorTotal = credito.prestacaoMensal * credito.quantidadePrestacoes
                canvas.drawText("Valor Total do Crédito: ${KwanzaFormatters.formatAOA(valorTotal)}", 30f, y + 30f, paint)
                canvas.drawText("PRESTAÇÃO: ${KwanzaFormatters.formatAOA(credito.prestacaoMensal)}", 320f, y + 30f, paint)

                val pagas = credito.prestacoesPagas
                val totalP = credito.quantidadePrestacoes
                canvas.drawText("Prestações Pagas: $pagas de $totalP (${(pagas * 100) / totalP.coerceAtLeast(1)}%)", 30f, y + 50f, paint)
                
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.color = if (credito.estado == "Em atraso") warningRed else forestGreen
                canvas.drawText("Estado do Crédito: ${credito.estado}", 320f, y + 50f, paint)

                val proximoFormatado = KwanzaFormatters.formatDate(credito.proximoVencimento)
                paint.color = darkText
                canvas.drawText("DIA DO REEMBOLSO: $proximoFormatado", 30f, y + 70f, paint)
                y += 105f
            } else {
                canvas.drawText("Nenhum contrato de crédito ativo.", 20f, y, paint)
                y += 30f
            }

            // Section 3: Histórico de Pagamentos
            paint.color = deepBlue
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 14f
            canvas.drawText("3. HISTÓRICO DE PAGAMENTOS REALIZADOS", 20f, y, paint)
            y += 8f
            paint.color = accentGold
            canvas.drawLine(20f, y, 575f, y, paint)
            y += 20f

            // Table Header
            paint.color = deepBlue
            canvas.drawRect(20f, y - 10f, 575f, y + 12f, paint)

            paint.color = Color.WHITE
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Nº PRESTAÇÃO", 30f, y + 2f, paint)
            canvas.drawText("DATA PAGAMENTO", 140f, y + 2f, paint)
            canvas.drawText("MÉTODO / COMPROVANTE", 270f, y + 2f, paint)
            canvas.drawText("VALOR PAGO (AOA)", 450f, y + 2f, paint)
            y += 22f

            paint.color = darkText
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            if (pagamentos.isEmpty()) {
                canvas.drawText("Nenhum pagamento registrado até o momento.", 30f, y, paint)
                y += 20f
            } else {
                pagamentos.forEachIndexed { idx, pag ->
                    if (idx % 2 == 1) {
                        paint.color = lightBg
                        canvas.drawRect(20f, y - 10f, 575f, y + 8f, paint)
                    }
                    paint.color = darkText
                    canvas.drawText("Prestação #${pag.numeroPrestacao}", 30f, y, paint)
                    canvas.drawText(KwanzaFormatters.formatDate(pag.dataPagamento), 140f, y, paint)
                    canvas.drawText(pag.comprovativoRef.ifEmpty { "Transferência" }, 270f, y, paint)

                    paint.color = forestGreen
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText(KwanzaFormatters.formatAOA(pag.valorPago), 450f, y, paint)

                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    y += 18f
                }
            }

            // Footer Signatures
            y = 770f
            paint.color = grayText
            canvas.drawLine(50f, y, 250f, y, paint)
            canvas.drawLine(340f, y, 540f, y, paint)
            y += 15f
            paint.textSize = 10f
            canvas.drawText("Assinatura do Gestor de Crédito", 65f, y, paint)
            canvas.drawText("Assinatura do Cliente", 380f, y, paint)

            pdfDocument.finishPage(page)

            // Save to cache file
            val file = File(context.cacheDir, "Extrato_${cliente.nome.replace(" ", "_")}.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()

            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun generateRelatorioGeralPdf(
        context: Context,
        clientesWithCredito: List<Pair<ClienteEntity, CreditoEntity?>>,
        summary: DashboardSummary
    ): File? {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }

            val deepBlue = Color.parseColor("#0A192F")
            val accentGold = Color.parseColor("#D4AF37")
            val darkText = Color.parseColor("#1E293B")
            val grayText = Color.parseColor("#64748B")
            val lightBg = Color.parseColor("#F8FAFC")
            val forestGreen = Color.parseColor("#2E7D32")
            val warningRed = Color.parseColor("#C62828")

            // Header Banner
            paint.color = deepBlue
            canvas.drawRect(0f, 0f, 595f, 100f, paint)

            paint.color = accentGold
            canvas.drawRect(0f, 96f, 595f, 100f, paint)

            paint.color = Color.WHITE
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 18f
            canvas.drawText("GESTÃO DE CLIENTES — NDUWA KAYONGO", 20f, 40f, paint)

            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("RELATÓRIO EXECUTIVO DA CARTEIRA DE CRÉDITO", 20f, 65f, paint)

            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            canvas.drawText("Data: ${dateFormat.format(Date())}", 400f, 65f, paint)

            var y = 130f

            // Executive Summary Box
            paint.color = lightBg
            canvas.drawRoundRect(20f, y - 10f, 575f, y + 80f, 8f, 8f, paint)

            paint.color = deepBlue
            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("RESUMO DA CARTEIRA", 30f, y + 10f, paint)

            paint.color = darkText
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            canvas.drawText("Carteira Ativa: ${KwanzaFormatters.formatAOA(summary.carteiraAtiva)}", 30f, y + 30f, paint)
            canvas.drawText("Total Clientes: ${summary.totalClientes} (${summary.clientesAtivosCount} ativos)", 300f, y + 30f, paint)

            canvas.drawText("Total Desembolsado: ${KwanzaFormatters.formatAOA(summary.totalDesembolsado)}", 30f, y + 48f, paint)
            canvas.drawText("Total Recuperado: ${KwanzaFormatters.formatAOA(summary.totalRecuperado)}", 300f, y + 48f, paint)

            canvas.drawText("Valor em Atraso: ${KwanzaFormatters.formatAOA(summary.valorEmAtraso)}", 30f, y + 66f, paint)
            canvas.drawText("Taxa de Risco de Crédito: ${String.format(Locale.US, "%.2f", summary.riscoPercentual)}%", 300f, y + 66f, paint)

            y += 110f

            // Client Table
            paint.color = deepBlue
            paint.textSize = 13f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("LISTA GERAL DE CLIENTES E CONTRATOS", 20f, y, paint)
            y += 8f
            paint.color = accentGold
            canvas.drawLine(20f, y, 575f, y, paint)
            y += 18f

            // Table Header
            paint.color = deepBlue
            canvas.drawRect(20f, y - 10f, 575f, y + 14f, paint)

            paint.color = Color.WHITE
            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("NOME DO CLIENTE", 25f, y + 2f, paint)
            canvas.drawText("TELEFONE", 140f, y + 2f, paint)
            canvas.drawText("BANCO", 215f, y + 2f, paint)
            canvas.drawText("VALOR RECEBIDO", 270f, y + 2f, paint)
            canvas.drawText("PRESTAÇÃO", 380f, y + 2f, paint)
            canvas.drawText("DIA REEMBOLSO", 480f, y + 2f, paint)
            y += 22f

            paint.color = darkText
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            clientesWithCredito.forEachIndexed { idx, (cliente, credito) ->
                if (idx % 2 == 1) {
                    paint.color = lightBg
                    canvas.drawRect(20f, y - 8f, 575f, y + 8f, paint)
                }

                paint.color = darkText
                paint.textSize = 8f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                val shortNome = KwanzaFormatters.formatPrimeiroEUltimoNome(cliente.nome)
                val displayNome = if (shortNome.length > 18) shortNome.take(16) + ".." else shortNome
                canvas.drawText(displayNome, 25f, y, paint)
                canvas.drawText(cliente.telefone, 140f, y, paint)
                canvas.drawText(cliente.banco, 215f, y, paint)

                val valorRecebido = credito?.valorDesembolsado ?: 0.0
                canvas.drawText(KwanzaFormatters.formatAOA(valorRecebido), 270f, y, paint)

                val prestacaoVal = credito?.prestacaoMensal ?: 0.0
                canvas.drawText(KwanzaFormatters.formatAOA(prestacaoVal), 380f, y, paint)

                val diaReembolso = if (credito != null) KwanzaFormatters.formatDate(credito.proximoVencimento) else "N/A"
                canvas.drawText(diaReembolso, 480f, y, paint)

                y += 16f
            }

            // Footer
            y = 800f
            paint.color = grayText
            paint.textSize = 9f
            canvas.drawText("Gestão Inteligente da Carteira de Crédito — Nduwa Kayongo © 2026", 130f, y, paint)

            pdfDocument.finishPage(page)

            val file = File(context.cacheDir, "Relatorio_Geral_Carteira.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()

            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
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
