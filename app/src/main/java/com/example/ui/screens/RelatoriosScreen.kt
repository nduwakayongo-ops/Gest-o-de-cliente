package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClienteEntity
import com.example.data.model.CreditoEntity
import com.example.repository.DashboardSummary
import com.example.ui.components.KwanzaFormatters
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RelatoriosScreen(
    summary: DashboardSummary,
    clientesWithCredito: List<Pair<ClienteEntity, CreditoEntity?>>
) {
    val context = LocalContext.current
    var relatorioTipo by remember { mutableStateOf("Mensal") } // Mensal, Semanal, Anual, Personalizado

    val dataAtual = SimpleDateFormat("MMMM yyyy", Locale("pt", "AO")).format(Date())

    val relatorioTexto = remember(summary, clientesWithCredito, relatorioTipo) {
        buildString {
            appendLine("============================================")
            appendLine("           RELATÓRIO DA CARTEIRA")
            appendLine("        Nduwa Kayongo - Crédito")
            appendLine("============================================")
            appendLine("Período: $relatorioTipo ($dataAtual)")
            appendLine("Número de Clientes: ${summary.totalClientes}")
            appendLine("Total Desembolsado: ${KwanzaFormatters.formatAOA(summary.totalDesembolsado)}")
            appendLine("Carteira Ativa: ${KwanzaFormatters.formatAOA(summary.carteiraAtiva)}")
            appendLine("Valor Recuperado: ${KwanzaFormatters.formatAOA(summary.totalRecuperado)}")
            appendLine("Valor em Atraso: ${KwanzaFormatters.formatAOA(summary.valorEmAtraso)}")
            appendLine("Índice de Risco: ${String.format("%.2f%%", summary.riscoPercentual)}")
            appendLine("--------------------------------------------")
            appendLine("TABELA COMPLETA DE CLIENTES")
            appendLine("--------------------------------------------")
            clientesWithCredito.forEachIndexed { idx, (cliente, credito) ->
                appendLine("${idx + 1}. NOME DO CLIENTE: ${KwanzaFormatters.formatPrimeiroEUltimoNome(cliente.nome)}")
                appendLine("   TEL: ${cliente.telefone} | BANCO: ${cliente.banco}")
                appendLine("   VALOR RECEBIDO: ${KwanzaFormatters.formatAOA(credito?.valorDesembolsado ?: 0.0)}")
                appendLine("   PRESTAÇÃO: ${KwanzaFormatters.formatAOA(credito?.prestacaoMensal ?: 0.0)}")
                appendLine("   DIA DO REEMBOLSO: ${if (credito != null) KwanzaFormatters.formatDate(credito.proximoVencimento) else "N/A"}")
                appendLine("--------------------------------------------")
            }
            appendLine("Gestão Inteligente da Carteira de Crédito")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GrayBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Selection Filter Type
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("📑 Opções de Relatório", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Mensal", "Semanal", "Anual", "Personalizado").forEach { tipo ->
                        FilterChip(
                            selected = relatorioTipo == tipo,
                            onClick = { relatorioTipo = tipo },
                            label = { Text(tipo, fontSize = 12.sp) }
                        )
                    }
                }
            }
        }

        // Action Share Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    val pdfFile = com.example.util.PdfGeneratorHelper.generateRelatorioGeralPdf(context, clientesWithCredito, summary)
                    if (pdfFile != null) {
                        com.example.util.PdfGeneratorHelper.openOrSharePdf(context, pdfFile)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_gerar_pdf_relatorio")
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Visualizar PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    val pdfFile = com.example.util.PdfGeneratorHelper.generateRelatorioGeralPdf(context, clientesWithCredito, summary)
                    if (pdfFile != null) {
                        com.example.util.PdfGeneratorHelper.sharePdf(context, pdfFile)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_compartilhar_relatorio")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Partilhar PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Live Preview & Visual Table Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxHeight()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📊 Tabela Oficial de Clientes & PDF", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = WarningRed)
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = GraySurface)
                Spacer(modifier = Modifier.height(10.dp))

                // Table Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DeepBlue, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cliente / Tel", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.8f))
                    Text("Banco", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.0f))
                    Text("Valor Rec.", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                    Text("Prestação", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                    Text("Reembolso", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.1f))
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Scrollable Table Content
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(clientesWithCredito) { (cliente, credito) ->
                        val isOverdue = credito?.estado == "Em atraso" || (credito?.diasEmAtraso ?: 0) > 0
                        val rowBg = if (isOverdue) Color(0xFFFFF0F0) else GraySurface.copy(alpha = 0.5f)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(rowBg, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.8f)) {
                                Text(
                                    text = KwanzaFormatters.formatPrimeiroEUltimoNome(cliente.nome),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DeepBlue
                                )
                                Text(
                                    text = cliente.telefone,
                                    fontSize = 9.sp,
                                    color = GrayText
                                )
                            }
                            Text(
                                text = cliente.banco,
                                fontSize = 10.sp,
                                color = GrayText,
                                modifier = Modifier.weight(1.0f)
                            )
                            Text(
                                text = KwanzaFormatters.formatAOA(credito?.valorDesembolsado ?: 0.0),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DeepBlue,
                                modifier = Modifier.weight(1.2f)
                            )
                            Text(
                                text = KwanzaFormatters.formatAOA(credito?.prestacaoMensal ?: 0.0),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreen,
                                modifier = Modifier.weight(1.2f)
                            )
                            Text(
                                text = if (credito != null) KwanzaFormatters.formatDate(credito.proximoVencimento) else "N/A",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOverdue) WarningRed else DeepBlue,
                                modifier = Modifier.weight(1.1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
