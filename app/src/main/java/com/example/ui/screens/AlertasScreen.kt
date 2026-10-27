package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClienteEntity
import com.example.data.model.CreditoEntity
import com.example.ui.components.KwanzaFormatters
import com.example.ui.theme.*
import com.example.util.PdfGeneratorHelper

@Composable
fun AlertasScreen(
    clientesWithCredito: List<Pair<ClienteEntity, CreditoEntity?>>,
    onSelectCliente: (Long) -> Unit,
    onConfirmarReembolso: (ClienteEntity, CreditoEntity) -> Unit = { _, _ -> },
    onSpeakCliente: (ClienteEntity, CreditoEntity?) -> Unit = { _, _ -> },
    onSpeakAll: () -> Unit = {},
    onStopSpeak: () -> Unit = {},
    isSpeaking: Boolean = false
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Acompanhamento Mensal, 1: Atrasos, 2: Liquidados
    var lembreteTarget by remember { mutableStateOf<Pair<ClienteEntity, CreditoEntity?>?>(null) }
    var clientToConfirmPayment by remember { mutableStateOf<Pair<ClienteEntity, CreditoEntity>?>(null) }
    var celebratoryMessage by remember { mutableStateOf<String?>(null) }

    // Active clients being tracked month-by-month
    val activeTrackedClients = remember(clientesWithCredito) {
        clientesWithCredito.filter { (_, credito) ->
            credito != null && credito.estado != "Liquidado" && credito.prestacoesPagas < credito.quantidadePrestacoes
        }.sortedBy { it.second?.proximoVencimento ?: 0L }
    }

    // Overdue clients
    val overdueClients = remember(clientesWithCredito) {
        clientesWithCredito.filter { (_, credito) ->
            credito != null && (credito.estado == "Em atraso" || (credito.diasEmAtraso > 0))
        }.sortedByDescending { it.second?.diasEmAtraso ?: 0 }
    }

    // Liquidated clients
    val liquidatedClients = remember(clientesWithCredito) {
        clientesWithCredito.filter { (_, credito) ->
            credito != null && (credito.estado == "Liquidado" || credito.prestacoesPagas >= credito.quantidadePrestacoes)
        }
    }

    // Modal Confirmation of Monthly Reimbursement
    clientToConfirmPayment?.let { (cliente, credito) ->
        val proximaPrestacao = credito.prestacoesPagas + 1
        val isFinalInstallment = proximaPrestacao >= credito.quantidadePrestacoes
        val valorFormatado = KwanzaFormatters.formatAOA(credito.prestacaoMensal)
        val dataExata = KwanzaFormatters.formatDate(credito.proximoVencimento)

        AlertDialog(
            onDismissRequest = { clientToConfirmPayment = null },
            icon = {
                Icon(
                    imageVector = if (isFinalInstallment) Icons.Default.Celebration else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isFinalInstallment) AccentGold else ForestGreen,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (isFinalInstallment) "🎉 Liquidação Total do Crédito" else "Confirmar Reembolso do Mês",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = DeepBlue
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Cliente: ${cliente.nome}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Data Exata do Reembolso: $dataExata",
                        fontSize = 13.sp,
                        color = GrayText
                    )
                    Text(
                        text = "Prestação Mensal: $valorFormatado (Mês $proximaPrestacao de ${credito.quantidadePrestacoes})",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = ForestGreen
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = if (isFinalInstallment) {
                            "Esta é a última prestação! Ao confirmar, o crédito será 100% liquidado e os lembretes automáticos deste contrato serão finalizados com sucesso."
                        } else {
                            "Ao confirmar o reembolso, o sistema avançará automaticamente para o Mês ${proximaPrestacao + 1} e continuará o acompanhamento com lembretes diários (08:00 e 16:00) até à liquidação total."
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clientToConfirmPayment = null
                        onConfirmarReembolso(cliente, credito)
                        if (isFinalInstallment) {
                            celebratoryMessage = "Parabéns! O crédito de ${cliente.nome} foi 100% liquidado com sucesso!"
                        } else {
                            Toast.makeText(context, "Reembolso do Mês $proximaPrestacao confirmado! Próximo ciclo agendado.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                    modifier = Modifier.testTag("btn_confirm_reimbursement_dialog")
                ) {
                    Text("Confirmar Reembolso", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { clientToConfirmPayment = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Celebration Dialog for Complete Liquidation
    celebratoryMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { celebratoryMessage = null },
            icon = {
                Icon(Icons.Default.Celebration, contentDescription = null, tint = AccentGold, modifier = Modifier.size(42.dp))
            },
            title = {
                Text("🎉 Crédito Totalmente Liquidado!", fontWeight = FontWeight.Bold, color = DeepBlue)
            },
            text = {
                Text(msg, fontSize = 14.sp)
            },
            confirmButton = {
                Button(
                    onClick = { celebratoryMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepBlue)
                ) {
                    Text("Excelente!")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GrayBackground)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Ultra-Compact Master Control Banner
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = DeepBlue),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = ForestGreen,
                            modifier = Modifier.size(6.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Lembretes 08h & 16h (${activeTrackedClients.size} pendentes)",
                            color = AccentGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Compact Action Chips: Ouvir, Disparar, PDF
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Surface(
                            onClick = {
                                if (isSpeaking) onStopSpeak() else onSpeakAll()
                            },
                            color = if (isSpeaking) WarningRed else AccentGold,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(24.dp).testTag("btn_listen_all_alerts")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = if (isSpeaking) Color.White else DeepBlue,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = if (isSpeaking) "Parar" else "Ouvir",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSpeaking) Color.White else DeepBlue
                                )
                            }
                        }

                        Surface(
                            onClick = {
                                com.example.util.NotificationSchedulerHelper.scheduleDailyAlarms(context)
                                com.example.util.NotificationSchedulerHelper.checkAndTriggerNotifications(context)
                                Toast.makeText(context, "Lembretes disparados na barra de notificações!", Toast.LENGTH_SHORT).show()
                            },
                            color = Color.White.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(24.dp).testTag("btn_test_notification")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = AccentGold, modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Disparar", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Surface(
                            onClick = {
                                val pdfFile = PdfGeneratorHelper.generateRelatorioPendenciasPdf(
                                    context = context,
                                    clientesWithCredito = clientesWithCredito
                                )
                                if (pdfFile != null) {
                                    val body = """
                                        Prezada Gestão Mbuli-Microcrédito,

                                        Segue em anexo o Relatório Oficial de Acompanhamento e Liquidação de Reembolsos gerado em ${KwanzaFormatters.formatDate(System.currentTimeMillis())}.

                                        • Clientes em acompanhamento: ${activeTrackedClients.size}
                                        • Clientes em atraso: ${overdueClients.size}
                                        • Gestor Responsável: Nduwa Kayongo
                                    """.trimIndent()

                                    PdfGeneratorHelper.sharePdfViaEmail(
                                        context = context,
                                        file = pdfFile,
                                        subject = "Relatório de Acompanhamento de Reembolsos — Mbuli-Microcrédito",
                                        bodyText = body
                                    )
                                } else {
                                    Toast.makeText(context, "Erro ao gerar PDF de pendências.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            color = ForestGreen,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(24.dp).testTag("btn_exportar_pendencias_alertas")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("PDF", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Compressed Segmented Filter Buttons: Acompanhamento, Atrasos, Liquidados
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val tabsList = listOf(
                Triple(0, "Acompanhar (${activeTrackedClients.size})", DeepBlue),
                Triple(1, "Atrasos (${overdueClients.size})", if (overdueClients.isNotEmpty()) WarningRed else DeepBlue),
                Triple(2, "Liquidados (${liquidatedClients.size})", ForestGreen)
            )

            tabsList.forEach { (index, title, activeTint) ->
                val isSelected = selectedTab == index
                Surface(
                    onClick = { selectedTab = index },
                    color = if (isSelected) activeTint.copy(alpha = 0.12f) else Color.Transparent,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f).height(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = title,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) activeTint else GrayText,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Tab Content
        when (selectedTab) {
            0 -> AcompanhamentoTabContent(
                activeClients = activeTrackedClients,
                onSelectCliente = onSelectCliente,
                onConfirmarReembolsoClick = { c, cr -> clientToConfirmPayment = Pair(c, cr) },
                onLembreteClick = { c, cr -> lembreteTarget = Pair(c, cr) },
                onSpeakCliente = onSpeakCliente
            )
            1 -> AtrasosTabContent(
                overdueClients = overdueClients,
                onSelectCliente = onSelectCliente,
                onConfirmarReembolsoClick = { c, cr -> clientToConfirmPayment = Pair(c, cr) },
                onLembreteClick = { c, cr -> lembreteTarget = Pair(c, cr) },
                onSpeakCliente = onSpeakCliente
            )
            2 -> LiquidadosTabContent(
                liquidatedClients = liquidatedClients,
                onSelectCliente = onSelectCliente
            )
        }
    }

    lembreteTarget?.let { (cliente, credito) ->
        com.example.ui.components.EnviarLembreteDialog(
            cliente = cliente,
            credito = credito,
            onDismiss = { lembreteTarget = null }
        )
    }
}

@Composable
private fun AcompanhamentoTabContent(
    activeClients: List<Pair<ClienteEntity, CreditoEntity?>>,
    onSelectCliente: (Long) -> Unit,
    onConfirmarReembolsoClick: (ClienteEntity, CreditoEntity) -> Unit,
    onLembreteClick: (ClienteEntity, CreditoEntity) -> Unit,
    onSpeakCliente: (ClienteEntity, CreditoEntity?) -> Unit
) {
    val context = LocalContext.current

    if (activeClients.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(56.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Nenhum cliente com reembolso pendente!", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DeepBlue)
                Text("Todos os créditos ativos estão em dia ou foram liquidados.", fontSize = 12.sp, color = GrayText)
            }
        }
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(activeClients, key = { it.first.id }) { (cliente, credito) ->
            if (credito != null) {
                val dataExata = KwanzaFormatters.formatDate(credito.proximoVencimento)
                val prestacaoFormatada = KwanzaFormatters.formatAOA(credito.prestacaoMensal)
                val prestacaoAtual = credito.prestacoesPagas + 1
                val totalPrestacoes = credito.quantidadePrestacoes
                val progress = if (totalPrestacoes > 0) credito.prestacoesPagas.toFloat() / totalPrestacoes.toFloat() else 0f
                val daysUntil = KwanzaFormatters.calculateDaysUntilOrOverdue(credito.proximoVencimento)

                val badgeColor: Color
                val badgeText: String
                when {
                    daysUntil < 0 -> {
                        badgeColor = WarningRed
                        badgeText = "Atraso de ${-daysUntil} dias"
                    }
                    daysUntil == 0 -> {
                        badgeColor = AccentGold
                        badgeText = "HOJE"
                    }
                    daysUntil in 1..5 -> {
                        badgeColor = Color(0xFFE67E22)
                        badgeText = "Em $daysUntil dias"
                    }
                    else -> {
                        badgeColor = DeepBlue
                        badgeText = "Em $daysUntil dias"
                    }
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectCliente(cliente.id) }
                        .testTag("acompanhamento_card_${cliente.id}")
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cliente.nome,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = DeepBlue
                                )
                                Text(
                                    text = "${cliente.banco} • Tel: ${cliente.telefone}",
                                    fontSize = 11.sp,
                                    color = GrayText
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = badgeColor.copy(alpha = 0.15f),
                                contentColor = badgeColor
                            ) {
                                Text(
                                    text = badgeText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Exact Date Highlight Card
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DeepBlue.copy(alpha = 0.05f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("DATA EXATA", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = GrayText)
                                    Text(dataExata, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("PRESTAÇÃO MENSAL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = GrayText)
                                    Text(prestacaoFormatada, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(5.dp))

                        // Month progress bar: Mês X de Y até Liquidação Total
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Mês $prestacaoAtual de $totalPrestacoes",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DeepBlue
                            )
                            Text(
                                text = "${credito.prestacoesPagas}/$totalPrestacoes (${(progress * 100).toInt()}%)",
                                fontSize = 10.sp,
                                color = GrayText
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                            color = ForestGreen,
                            trackColor = Color.LightGray.copy(alpha = 0.4f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Action Buttons: Confirm Reimbursement, Reminder, Call, Listen
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { onConfirmarReembolsoClick(cliente, credito) },
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1.3f).height(30.dp).testTag("btn_confirm_reembolso_${cliente.id}")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Confirmar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { onLembreteClick(cliente, credito) },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkGold),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1.1f).height(30.dp).testTag("btn_lembrete_${cliente.id}")
                            ) {
                                Text("Lembrete", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            IconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${cliente.telefone}"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = "Ligar", tint = DeepBlue, modifier = Modifier.size(16.dp))
                            }

                            IconButton(
                                onClick = { onSpeakCliente(cliente, credito) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "Ouvir", tint = DeepBlue, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AtrasosTabContent(
    overdueClients: List<Pair<ClienteEntity, CreditoEntity?>>,
    onSelectCliente: (Long) -> Unit,
    onConfirmarReembolsoClick: (ClienteEntity, CreditoEntity) -> Unit,
    onLembreteClick: (ClienteEntity, CreditoEntity) -> Unit,
    onSpeakCliente: (ClienteEntity, CreditoEntity?) -> Unit
) {
    val context = LocalContext.current

    if (overdueClients.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(56.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Nenhum cliente em atraso!", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DeepBlue)
                Text("A carteira está 100% regularizada.", fontSize = 12.sp, color = GrayText)
            }
        }
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(overdueClients, key = { it.first.id }) { (cliente, credito) ->
            if (credito != null) {
                val dataExata = KwanzaFormatters.formatDate(credito.proximoVencimento)
                val prestacaoFormatada = KwanzaFormatters.formatAOA(credito.prestacaoMensal)

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(WarningRed)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onSelectCliente(cliente.id) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = WarningRed, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reembolso em Atraso", fontWeight = FontWeight.Bold, color = WarningRed, fontSize = 14.sp)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = WarningRedLight,
                                contentColor = WarningRed
                            ) {
                                Text(
                                    text = "${credito.diasEmAtraso} dias de atraso",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        val moraCalculada = com.example.util.MoraCalculatorHelper.calcular(credito = credito, diasAtrasoManual = credito.diasEmAtraso)
                        Text(cliente.nome, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Data Prevista: $dataExata • Prestação: $prestacaoFormatada", fontSize = 12.sp, color = WarningRed, fontWeight = FontWeight.SemiBold)
                        Text("Mora Acumulada (4%/dia juros): ${KwanzaFormatters.formatAOA(moraCalculada.jurosMoraAcumulados)}", fontSize = 11.5.sp, color = DarkGold, fontWeight = FontWeight.Bold)
                        Text("Total Exigível com Mora: ${KwanzaFormatters.formatAOA(moraCalculada.totalExigivel)}", fontSize = 12.sp, color = WarningRed, fontWeight = FontWeight.Bold)
                        Text("Banco: ${cliente.banco} • Tel: ${cliente.telefone}", fontSize = 11.5.sp, color = GrayText)

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { onConfirmarReembolsoClick(cliente, credito) },
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Text("Liquidado?", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { onLembreteClick(cliente, credito) },
                                colors = ButtonDefaults.buttonColors(containerColor = WarningRed),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Text("Cobrança", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            IconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${cliente.telefone}"))
                                    context.startActivity(intent)
                                }
                            ) {
                                Icon(Icons.Default.Call, contentDescription = "Ligar", tint = DeepBlue)
                            }

                            IconButton(onClick = { onSpeakCliente(cliente, credito) }) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "Ouvir", tint = DeepBlue)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiquidadosTabContent(
    liquidatedClients: List<Pair<ClienteEntity, CreditoEntity?>>,
    onSelectCliente: (Long) -> Unit
) {
    if (liquidatedClients.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Info, contentDescription = null, tint = GrayText, modifier = Modifier.size(56.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Nenhum crédito totalmente liquidado ainda.", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DeepBlue)
                Text("Assim que os clientes concluírem os reembolsos, aparecerão aqui com status de liquidação total.", fontSize = 12.sp, color = GrayText)
            }
        }
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(liquidatedClients, key = { it.first.id }) { (cliente, credito) ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().clickable { onSelectCliente(cliente.id) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(cliente.nome, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DeepBlue)
                            Text("${cliente.banco} • Tel: ${cliente.telefone}", fontSize = 12.sp, color = GrayText)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ForestGreen.copy(alpha = 0.15f),
                            contentColor = ForestGreen
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("LIQUIDADO 100%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Valor Desembolsado: ${KwanzaFormatters.formatAOA(credito?.valorDesembolsado ?: 0.0)} • ${credito?.quantidadePrestacoes} prestações totalmente reembolsadas.",
                        fontSize = 12.sp,
                        color = ForestGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
