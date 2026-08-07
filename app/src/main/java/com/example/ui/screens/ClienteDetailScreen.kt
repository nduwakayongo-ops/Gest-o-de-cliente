package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ClienteEntity
import com.example.data.model.CobrancaEntity
import com.example.data.model.CreditoEntity
import com.example.data.model.PagamentoEntity
import com.example.ui.components.KwanzaFormatters
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClienteDetailScreen(
    cliente: ClienteEntity?,
    creditos: List<CreditoEntity>,
    pagamentos: List<PagamentoEntity>,
    cobrancas: List<CobrancaEntity>,
    onBackClick: () -> Unit,
    onRegistrarPagamento: (PagamentoEntity) -> Unit,
    onRegistrarCobranca: (CobrancaEntity) -> Unit,
    onSpeakResumo: (ClienteEntity, CreditoEntity?) -> Unit = { _, _ -> },
    onEditCliente: (ClienteEntity) -> Unit = {},
    onDeleteCliente: (ClienteEntity) -> Unit = {},
    onDeletePagamento: (PagamentoEntity) -> Unit = {},
    onDeleteCobranca: (CobrancaEntity) -> Unit = {}
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Geral, 1: Pagamentos, 2: Cobranças

    var showPagamentoModal by remember { mutableStateOf(false) }
    var showCobrancaModal by remember { mutableStateOf(false) }
    var showDeleteClientDialog by remember { mutableStateOf(false) }
    var pagamentoToDelete by remember { mutableStateOf<PagamentoEntity?>(null) }
    var cobrancaToDelete by remember { mutableStateOf<CobrancaEntity?>(null) }

    if (cliente == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = DeepBlue)
        }
        return
    }

    val credito = creditos.firstOrNull()
    val isAtraso = credito?.estado == "Em atraso" || (credito?.diasEmAtraso ?: 0) > 0

    if (showDeleteClientDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteClientDialog = false },
            title = { Text("Excluir Cliente", fontWeight = FontWeight.Bold, color = WarningRed) },
            text = { Text("Deseja realmente excluir permanentemente o cliente ${cliente.nome}? Todos os dados e registos associados serão removidos.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteClientDialog = false
                        onDeleteCliente(cliente)
                        onBackClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarningRed)
                ) {
                    Text("Excluir", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteClientDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    pagamentoToDelete?.let { pag ->
        AlertDialog(
            onDismissRequest = { pagamentoToDelete = null },
            title = { Text("Excluir Registo de Pagamento", fontWeight = FontWeight.Bold, color = WarningRed) },
            text = { Text("Deseja excluir este registo de pagamento de ${KwanzaFormatters.formatAOA(pag.valorPago)}?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePagamento(pag)
                        pagamentoToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarningRed)
                ) {
                    Text("Excluir", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pagamentoToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    cobrancaToDelete?.let { cob ->
        AlertDialog(
            onDismissRequest = { cobrancaToDelete = null },
            title = { Text("Excluir Registo de Cobrança", fontWeight = FontWeight.Bold, color = WarningRed) },
            text = { Text("Deseja excluir este registo de contato via ${cob.tipoContato}?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCobranca(cob)
                        cobrancaToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarningRed)
                ) {
                    Text("Excluir", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { cobrancaToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(cliente.nome, fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val pdfFile = com.example.util.PdfGeneratorHelper.generateExtratoClientePdf(
                            context = context,
                            cliente = cliente,
                            credito = credito,
                            pagamentos = pagamentos,
                            cobrancas = cobrancas
                        )
                        if (pdfFile != null) {
                            com.example.util.PdfGeneratorHelper.openOrSharePdf(context, pdfFile)
                        }
                    }, modifier = Modifier.testTag("btn_export_pdf_client")) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Gerar PDF", tint = AccentGold)
                    }
                    IconButton(onClick = { onEditCliente(cliente) }, modifier = Modifier.testTag("btn_edit_client")) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar Cliente", tint = Color.White)
                    }
                    IconButton(onClick = { showDeleteClientDialog = true }, modifier = Modifier.testTag("btn_delete_client")) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir Cliente", tint = Color(0xFFFF6B6B))
                    }
                    IconButton(onClick = {
                        val pdfFile = com.example.util.PdfGeneratorHelper.generateExtratoClientePdf(
                            context = context,
                            cliente = cliente,
                            credito = credito,
                            pagamentos = pagamentos,
                            cobrancas = cobrancas
                        )
                        if (pdfFile != null) {
                            com.example.util.PdfGeneratorHelper.sharePdf(context, pdfFile)
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Partilhar Extrato PDF", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlue)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(GrayBackground)
        ) {
            // Profile Summary Header Banner
            Card(
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                colors = CardDefaults.cardColors(containerColor = DeepBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!cliente.fotoUri.isNullOrEmpty()) {
                            AsyncImage(
                                model = cliente.fotoUri,
                                contentDescription = "Foto do Cliente",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = cliente.nome.take(2).uppercase(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepBlue
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = cliente.nome,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${cliente.numeroInterno} • ${cliente.empresa} • ${cliente.telefone}",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action buttons bar
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { onSpeakResumo(cliente, credito) },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkGold),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_speak_resumo")
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ouvir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showPagamentoModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_registrar_pagamento")
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pagamento", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showCobrancaModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_registrar_cobranca")
                        ) {
                            Icon(Icons.Default.PhoneCallback, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cobrança", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${cliente.telefone}"))
                                context.startActivity(intent)
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Ligar", tint = Color.White)
                        }
                    }
                }
            }

            // Tabs Bar
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = DeepBlue
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Visão Geral", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Pagamentos (${pagamentos.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Cobranças (${cobrancas.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
            }

            // Tab Content
            when (selectedTab) {
                0 -> VisaoGeralTab(cliente, credito)
                1 -> PagamentosTab(pagamentos, onDeletePagamento = { pagamentoToDelete = it })
                2 -> CobrancasTab(cobrancas, onDeleteCobranca = { cobrancaToDelete = it })
            }
        }
    }

    // Modal Registrar Pagamento
    if (showPagamentoModal && credito != null) {
        RegistrarPagamentoModal(
            cliente = cliente,
            credito = credito,
            onDismiss = { showPagamentoModal = false },
            onConfirm = { valPago, obs ->
                onRegistrarPagamento(
                    PagamentoEntity(
                        creditoId = credito.id,
                        clienteId = cliente.id,
                        valorPago = valPago,
                        dataPagamento = System.currentTimeMillis(),
                        numeroPrestacao = credito.prestacoesPagas + 1,
                        comprovativoRef = "REC-${System.currentTimeMillis().toString().takeLast(6)}",
                        observacao = obs
                    )
                )
                showPagamentoModal = false
            }
        )
    }

    // Modal Registrar Cobrança
    if (showCobrancaModal) {
        RegistrarCobrancaModal(
            cliente = cliente,
            onDismiss = { showCobrancaModal = false },
            onConfirm = { tipo, obs ->
                onRegistrarCobranca(
                    CobrancaEntity(
                        clienteId = cliente.id,
                        dataContato = System.currentTimeMillis(),
                        tipoContato = tipo,
                        observacao = obs,
                        oficialResponsavel = "Nduwa Kayongo"
                    )
                )
                showCobrancaModal = false
            }
        )
    }
}

@Composable
fun VisaoGeralTab(cliente: ClienteEntity, credito: CreditoEntity?) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Dados do Crédito Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "💳 Informações do Crédito",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepBlue
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    DetailRow("Valor Desembolsado", KwanzaFormatters.formatAOA(credito?.valorDesembolsado ?: 0.0))
                    DetailRow("Valor Líquido", KwanzaFormatters.formatAOA(credito?.valorLiquido ?: 0.0))
                    DetailRow("Taxa de Juros", "${credito?.taxaPercentual ?: 0.0}%")
                    DetailRow("Valor dos Juros", KwanzaFormatters.formatAOA(credito?.valorJuros ?: 0.0))
                    DetailRow("Prestação Mensal", KwanzaFormatters.formatAOA(credito?.prestacaoMensal ?: 0.0), isBold = true)
                    DetailRow("Prestações Pagas / Total", "${credito?.prestacoesPagas ?: 0} / ${credito?.quantidadePrestacoes ?: 0}")
                    DetailRow("Data do Desembolso", KwanzaFormatters.formatDate(credito?.dataDesembolso ?: 0L))
                    DetailRow("Início do Reembolso", KwanzaFormatters.formatDate(credito?.dataPrimeiroPagamento ?: 0L))
                    DetailRow("Fim do Reembolso", KwanzaFormatters.formatDate(credito?.dataUltimoPagamento ?: 0L))
                    DetailRow("Próximo Vencimento", KwanzaFormatters.formatDate(credito?.proximoVencimento ?: 0L))
                    DetailRow("Estado do Crédito", credito?.estado ?: "N/A", isStatus = true)
                    if ((credito?.diasEmAtraso ?: 0) > 0) {
                        DetailRow("Valor em Atraso", KwanzaFormatters.formatAOA(credito?.valorEmAtraso ?: 0.0), color = WarningRed)
                        DetailRow("Dias em Atraso", "${credito?.diasEmAtraso} dias", color = WarningRed)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (credito?.estado == "Liquidado") ForestGreen.copy(alpha = 0.1f) else AccentGold.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (credito?.estado == "Liquidado") Icons.Default.CheckCircle else Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = if (credito?.estado == "Liquidado") ForestGreen else DeepBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (credito?.estado == "Liquidado")
                                    "✅ Crédito totalmente liquidado! Os lembretes automáticos foram concluídos com sucesso."
                                else
                                    "🔔 Lembretes automáticos ativos acompanhando de ${KwanzaFormatters.formatDate(credito?.dataPrimeiroPagamento ?: 0L)} até ${KwanzaFormatters.formatDate(credito?.dataUltimoPagamento ?: 0L)} até a confirmação total do pagamento.",
                                fontSize = 11.sp,
                                color = DeepBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Dados Pessoais & Bancários Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🏛 Dados Pessoais e Bancários",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepBlue
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    DetailRow("Banco", cliente.banco)
                    DetailRow("Número da Conta", cliente.numeroConta)
                    DetailRow("IBAN", cliente.iban)
                    DetailRow("NIF", cliente.nif)
                    DetailRow("Empresa", cliente.empresa)
                    DetailRow("Salário Declarado", KwanzaFormatters.formatAOA(cliente.salario))
                    DetailRow("Província / Município", "${cliente.provincia} / ${cliente.municipio}")
                    DetailRow("Comuna / Endereço", "${cliente.comuna}, ${cliente.endereco}")
                    DetailRow("Observações", cliente.observacoes.ifEmpty { "Sem observações." })
                }
            }
        }
    }
}

@Composable
fun PagamentosTab(
    pagamentos: List<PagamentoEntity>,
    onDeletePagamento: (PagamentoEntity) -> Unit = {}
) {
    if (pagamentos.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nenhum pagamento registrado ainda.", color = GrayText)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(pagamentos) { pag ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Prestação nº ${pag.numeroPrestacao}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Ref: ${pag.comprovativoRef} • ${KwanzaFormatters.formatDate(pag.dataPagamento)}",
                                fontSize = 11.sp,
                                color = GrayText
                            )
                            if (pag.observacao.isNotEmpty()) {
                                Text(
                                    text = pag.observacao,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = KwanzaFormatters.formatAOA(pag.valorPago),
                                fontWeight = FontWeight.Bold,
                                color = ForestGreen,
                                fontSize = 14.sp
                            )
                            IconButton(onClick = { onDeletePagamento(pag) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Excluir Pagamento", tint = WarningRed, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CobrancasTab(
    cobrancas: List<CobrancaEntity>,
    onDeleteCobranca: (CobrancaEntity) -> Unit = {}
) {
    if (cobrancas.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nenhum histórico de cobrança registrado.", color = GrayText)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(cobrancas) { cob ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Contato via ${cob.tipoContato}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = DeepBlue
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = KwanzaFormatters.formatDate(cob.dataContato),
                                    fontSize = 11.sp,
                                    color = GrayText
                                )
                                IconButton(onClick = { onDeleteCobranca(cob) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Excluir Cobrança", tint = WarningRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = cob.observacao,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Oficial: ${cob.oficialResponsavel}",
                            fontSize = 10.sp,
                            color = GrayText
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    isStatus: Boolean = false,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = GrayText)
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = if (isBold || isStatus) FontWeight.Bold else FontWeight.Normal,
            color = if (isStatus) DeepBlue else color
        )
    }
}

@Composable
fun RegistrarPagamentoModal(
    cliente: ClienteEntity,
    credito: CreditoEntity,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var valorText by remember { mutableStateOf(credito.prestacaoMensal.toInt().toString()) }
    var obsText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Pagamento", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Cliente: ${cliente.nome}")
                Text("Prestação Mensal: ${KwanzaFormatters.formatAOA(credito.prestacaoMensal)}")

                OutlinedTextField(
                    value = valorText,
                    onValueChange = { valorText = it },
                    label = { Text("Valor Pago (AOA)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = obsText,
                    onValueChange = { obsText = it },
                    label = { Text("Observação / Comprovativo") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val valDouble = valorText.toDoubleOrNull() ?: credito.prestacaoMensal
                    onConfirm(valDouble, obsText)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun RegistrarCobrancaModal(
    cliente: ClienteEntity,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var tipoContato by remember { mutableStateOf("Chamada") }
    var obsText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Contato / Cobrança", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Cliente: ${cliente.nome}")

                Text("Tipo de Contato:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Chamada", "WhatsApp", "Visita", "Email").forEach { tipo ->
                        FilterChip(
                            selected = tipoContato == tipo,
                            onClick = { tipoContato = tipo },
                            label = { Text(tipo, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = obsText,
                    onValueChange = { obsText = it },
                    label = { Text("Observação / Promessa de Pagamento") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(tipoContato, obsText) },
                colors = ButtonDefaults.buttonColors(containerColor = DeepBlue)
            ) {
                Text("Salvar Registro")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
