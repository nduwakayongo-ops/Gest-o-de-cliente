package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Save
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
import com.example.data.model.CreditoEntity
import com.example.ui.components.KwanzaFormatters
import com.example.ui.theme.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditClienteScreen(
    onBackClick: () -> Unit,
    onSaveCliente: (ClienteEntity, CreditoEntity) -> Unit,
    clienteToEdit: ClienteEntity? = null,
    creditoToEdit: CreditoEntity? = null
) {
    var fotoUriString by remember(clienteToEdit) { mutableStateOf<String?>(clienteToEdit?.fotoUri) }
    val clientPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { fotoUriString = it.toString() }
    }

    var nome by remember(clienteToEdit) { mutableStateOf(clienteToEdit?.nome ?: "") }
    var numeroInterno by remember(clienteToEdit) { mutableStateOf(clienteToEdit?.numeroInterno ?: "CLI-${(1000..9999).random()}") }
    var telefone by remember(clienteToEdit) { mutableStateOf(clienteToEdit?.telefone ?: "+244 ") }
    var sexo by remember(clienteToEdit) { mutableStateOf(clienteToEdit?.sexo ?: "M") }
    var provincia by remember(clienteToEdit) { mutableStateOf(clienteToEdit?.provincia ?: "Luanda") }
    var municipio by remember(clienteToEdit) { mutableStateOf(clienteToEdit?.municipio ?: "Luanda") }
    var comuna by remember(clienteToEdit) { mutableStateOf(clienteToEdit?.comuna ?: "Ingombota") }
    var endereco by remember(clienteToEdit) { mutableStateOf(clienteToEdit?.endereco ?: "") }
    var banco by remember(clienteToEdit) { mutableStateOf(clienteToEdit?.banco ?: "BAI") }
    var numeroConta by remember(clienteToEdit) { mutableStateOf(clienteToEdit?.numeroConta ?: "") }
    var iban by remember(clienteToEdit) { mutableStateOf(clienteToEdit?.iban ?: "") }
    var nif by remember(clienteToEdit) { mutableStateOf(clienteToEdit?.nif ?: "") }
    var empresa by remember(clienteToEdit) { mutableStateOf(clienteToEdit?.empresa ?: "") }
    var salarioText by remember(clienteToEdit) { mutableStateOf(clienteToEdit?.salario?.toInt()?.toString() ?: "300000") }
    var observacoes by remember(clienteToEdit) { mutableStateOf(clienteToEdit?.observacoes ?: "") }

    val context = LocalContext.current

    // Credit Fields
    var valorDesembolsadoText by remember(creditoToEdit) { mutableStateOf(creditoToEdit?.valorDesembolsado?.toInt()?.toString() ?: "1500000") }
    var taxaText by remember(creditoToEdit) { mutableStateOf(creditoToEdit?.taxaPercentual?.toInt()?.toString() ?: "5") }
    var qtdPrestacoesText by remember(creditoToEdit) { mutableStateOf(creditoToEdit?.quantidadePrestacoes?.toString() ?: "18") }
    var diasCreditoText by remember(creditoToEdit) { mutableStateOf("540") }
    var selectedTipoJuros by remember { mutableStateOf(com.example.ui.viewmodel.TipoCalculoJuros.COMPOSTO_PRICE) }
    var showSchedulePreview by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val defaultStartCal = Calendar.getInstance().apply {
        timeInMillis = now
        add(Calendar.MONTH, 1)
    }
    val initialStartDate = creditoToEdit?.dataPrimeiroPagamento?.takeIf { it > 0 } ?: defaultStartCal.timeInMillis
    var dataInicioReembolso by remember(creditoToEdit) { mutableLongStateOf(initialStartDate) }

    val initialEndDate = creditoToEdit?.dataUltimoPagamento?.takeIf { it > 0 } ?: Calendar.getInstance().apply {
        timeInMillis = initialStartDate
        add(Calendar.MONTH, creditoToEdit?.quantidadePrestacoes ?: 18)
    }.timeInMillis
    var dataFimReembolso by remember(creditoToEdit) { mutableLongStateOf(initialEndDate) }

    val valorDesembolsado = valorDesembolsadoText.toDoubleOrNull() ?: 0.0
    val taxaPercentual = taxaText.toDoubleOrNull() ?: 0.0
    val qtdPrestacoes = (qtdPrestacoesText.toIntOrNull() ?: 12).coerceAtLeast(1)

    val simState = remember(valorDesembolsado, taxaPercentual, qtdPrestacoes, selectedTipoJuros) {
        com.example.ui.viewmodel.CalculadoraViewModel.calcularSaldoDevedorEParcelas(
            principal = valorDesembolsado,
            taxaPercentual = taxaPercentual,
            prazoMeses = qtdPrestacoes,
            parcelasPagas = creditoToEdit?.prestacoesPagas ?: 0,
            tipo = selectedTipoJuros
        )
    }

    val valorLiquido = (valorDesembolsado * (1.0 - (taxaPercentual / 100.0))).coerceAtLeast(0.0)
    val valorJuros = simState.valorJurosTotal
    val prestacaoMensal = simState.prestacaoMensal
    val totalALiquidar = simState.valorTotalLiquidar

    val banksList = listOf("BPC", "BAI", "BFA", "Banco Económico", "SOL", "Banco Atlântico", "BIC", "BCI", "KEVE", "Standard Bank", "YETU", "Caixa Tota", "Outro")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (clienteToEdit != null) "Editar Cliente" else "Novo Cadastro de Cliente", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Dados Pessoais
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("👤 Dados Pessoais e Foto de Perfil", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DeepBlue)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .border(2.dp, AccentGold, CircleShape)
                                .background(GraySurface)
                                .clickable { clientPhotoPicker.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!fotoUriString.isNullOrEmpty()) {
                                AsyncImage(
                                    model = fotoUriString,
                                    contentDescription = "Foto do Cliente",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(Icons.Default.Person, contentDescription = null, tint = DeepBlue, modifier = Modifier.size(32.dp))
                            }
                        }

                        Column {
                            Button(
                                onClick = { clientPhotoPicker.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("btn_pick_client_photo")
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Foto da Galeria", fontSize = 12.sp)
                            }
                            if (!fotoUriString.isNullOrEmpty()) {
                                TextButton(
                                    onClick = { fotoUriString = null },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Remover Foto", color = WarningRed, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = nome,
                        onValueChange = { nome = it },
                        label = { Text("Nome Completo *") },
                        modifier = Modifier.fillMaxWidth().testTag("input_nome"),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = numeroInterno,
                            onValueChange = { numeroInterno = it },
                            label = { Text("Nº Interno") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = telefone,
                            onValueChange = { telefone = it },
                            label = { Text("Telefone *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Sexo: ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        FilterChip(selected = sexo == "M", onClick = { sexo = "M" }, label = { Text("Masculino") })
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(selected = sexo == "F", onClick = { sexo = "F" }, label = { Text("Feminino") })
                    }

                    OutlinedTextField(
                        value = empresa,
                        onValueChange = { empresa = it },
                        label = { Text("Empresa / Empregador") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = salarioText,
                        onValueChange = { salarioText = it },
                        label = { Text("Salário Declarado (AOA)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = provincia,
                            onValueChange = { provincia = it },
                            label = { Text("Província") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = municipio,
                            onValueChange = { municipio = it },
                            label = { Text("Município") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = endereco,
                        onValueChange = { endereco = it },
                        label = { Text("Endereço Residencial") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // Section 2: Dados Bancários
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("🏛 Dados Bancários", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DeepBlue)

                    Text("Selecione o Banco:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(banksList.size) { index ->
                            val b = banksList[index]
                            FilterChip(
                                selected = banco == b || (b == "Outro" && !banksList.dropLast(1).contains(banco)),
                                onClick = { banco = b },
                                label = { Text(b, fontSize = 11.sp) }
                            )
                        }
                    }

                    if (banco == "Outro" || !banksList.dropLast(1).contains(banco)) {
                        OutlinedTextField(
                            value = if (banco == "Outro") "" else banco,
                            onValueChange = { banco = it },
                            label = { Text("Nome do Banco (Outro)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = numeroConta,
                        onValueChange = { numeroConta = it },
                        label = { Text("Número da Conta") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = iban,
                        onValueChange = { iban = it },
                        label = { Text("IBAN") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = nif,
                        onValueChange = { nif = it },
                        label = { Text("NIF") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // Section 3: Dados do Crédito
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("💰 Configuração do Crédito", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ForestGreen)

                    OutlinedTextField(
                        value = valorDesembolsadoText,
                        onValueChange = { valorDesembolsadoText = it },
                        label = { Text("Valor Desembolsado (AOA) *") },
                        modifier = Modifier.fillMaxWidth().testTag("input_valor_desembolsado"),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = taxaText,
                            onValueChange = { taxaText = it },
                            label = { Text("Taxa (%)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = qtdPrestacoesText,
                            onValueChange = { qtdPrestacoesText = it },
                            label = { Text("Qtd Prestações") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Text("Modelo de Cálculo de Juros:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        com.example.ui.viewmodel.TipoCalculoJuros.values().forEach { tipo ->
                            FilterChip(
                                selected = selectedTipoJuros == tipo,
                                onClick = { selectedTipoJuros = tipo },
                                label = { Text(tipo.label.split(" ")[0], fontSize = 11.sp) }
                            )
                        }
                    }

                    // Calculation Summary Card with Live Simulation
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = LightGreen,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("⚡ Simulação Automática de Juros", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
                                Surface(shape = RoundedCornerShape(6.dp), color = ForestGreen, contentColor = Color.White) {
                                    Text("Ativa", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("• Valor Desembolsado: ${KwanzaFormatters.formatAOA(valorDesembolsado)}", fontSize = 12.sp)
                            Text("• Valor Líquido do Cliente: ${KwanzaFormatters.formatAOA(valorLiquido)}", fontSize = 12.sp)
                            Text("• Juros Totais Calculados: ${KwanzaFormatters.formatAOA(valorJuros)}", fontSize = 12.sp, color = DarkGold, fontWeight = FontWeight.SemiBold)
                            Text("• Total Saldo Devedor a Liquidar: ${KwanzaFormatters.formatAOA(totalALiquidar)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("• Prestação Mensal Recomendada: ${KwanzaFormatters.formatAOA(prestacaoMensal)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DeepBlue)

                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(
                                onClick = { showSchedulePreview = !showSchedulePreview },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(if (showSchedulePreview) "Ocultar Prévia de Parcelas" else "👁 Simular Tabela de Parcelas", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                            }

                            if (showSchedulePreview && simState.cronograma.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    simState.cronograma.take(6).forEach { item ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Parcela #${item.numeroParcela}", fontSize = 11.sp)
                                            Text("${KwanzaFormatters.formatAOA(item.valorPrestacao)} (Rest: ${KwanzaFormatters.formatAOA(item.saldoDevedorRestante)})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    if (simState.cronograma.size > 6) {
                                        Text("+ mais ${simState.cronograma.size - 6} parcelas...", fontSize = 10.sp, color = GrayText)
                                    }
                                }
                            }
                        }
                    }

                    // Section 3.1: Datas do Reembolso
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = GrayBackground),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = DeepBlue, modifier = Modifier.size(18.dp))
                                Text("📅 Datas do Reembolso (Início e Fim)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Data Inicio Reembolso
                                OutlinedCard(
                                    onClick = {
                                        val cal = Calendar.getInstance().apply { timeInMillis = dataInicioReembolso }
                                        android.app.DatePickerDialog(
                                            context,
                                            { _, year, month, dayOfMonth ->
                                                val selCal = Calendar.getInstance().apply {
                                                    set(Calendar.YEAR, year)
                                                    set(Calendar.MONTH, month)
                                                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                                    set(Calendar.HOUR_OF_DAY, 8)
                                                }
                                                dataInicioReembolso = selCal.timeInMillis
                                                val endCal = Calendar.getInstance().apply {
                                                    timeInMillis = selCal.timeInMillis
                                                    add(Calendar.MONTH, qtdPrestacoes)
                                                }
                                                dataFimReembolso = endCal.timeInMillis
                                            },
                                            cal.get(Calendar.YEAR),
                                            cal.get(Calendar.MONTH),
                                            cal.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    },
                                    modifier = Modifier.weight(1f).testTag("btn_select_data_inicio"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Início do Reembolso *", fontSize = 10.sp, color = GrayText)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(KwanzaFormatters.formatDate(dataInicioReembolso), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                                        Text("Clique p/ alterar", fontSize = 9.sp, color = DarkGold)
                                    }
                                }

                                // Data Fim Reembolso
                                OutlinedCard(
                                    onClick = {
                                        val cal = Calendar.getInstance().apply { timeInMillis = dataFimReembolso }
                                        android.app.DatePickerDialog(
                                            context,
                                            { _, year, month, dayOfMonth ->
                                                val selCal = Calendar.getInstance().apply {
                                                    set(Calendar.YEAR, year)
                                                    set(Calendar.MONTH, month)
                                                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                                    set(Calendar.HOUR_OF_DAY, 8)
                                                }
                                                dataFimReembolso = selCal.timeInMillis
                                            },
                                            cal.get(Calendar.YEAR),
                                            cal.get(Calendar.MONTH),
                                            cal.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    },
                                    modifier = Modifier.weight(1f).testTag("btn_select_data_fim"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Fim do Reembolso *", fontSize = 10.sp, color = GrayText)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(KwanzaFormatters.formatDate(dataFimReembolso), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
                                        Text("Clique p/ alterar", fontSize = 9.sp, color = DarkGold)
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AccentGold.copy(alpha = 0.15f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = DeepBlue, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Os lembretes automáticos diários (08:00 e 17:00) acompanharão este cliente do dia ${KwanzaFormatters.formatDate(dataInicioReembolso)} até ${KwanzaFormatters.formatDate(dataFimReembolso)} até a liquidação final.",
                                        fontSize = 10.sp,
                                        color = DeepBlue,
                                        lineHeight = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = observacoes,
                        onValueChange = { observacoes = it },
                        label = { Text("Observações") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            }

            // Save Button
            Button(
                onClick = {
                    if (nome.isNotEmpty()) {
                        val nowTime = System.currentTimeMillis()

                        val cliente = ClienteEntity(
                            id = clienteToEdit?.id ?: 0L,
                            numeroInterno = numeroInterno,
                            nome = nome,
                            telefone = telefone,
                            sexo = sexo,
                            provincia = provincia,
                            municipio = municipio,
                            comuna = comuna,
                            endereco = endereco,
                            banco = banco,
                            numeroConta = numeroConta,
                            iban = iban,
                            nif = nif,
                            empresa = empresa,
                            salario = salarioText.toDoubleOrNull() ?: 0.0,
                            fotoUri = fotoUriString,
                            observacoes = observacoes
                        )

                        val credito = CreditoEntity(
                            id = creditoToEdit?.id ?: 0L,
                            clienteId = clienteToEdit?.id ?: 0L,
                            valorDesembolsado = valorDesembolsado,
                            valorLiquido = valorLiquido,
                            taxaPercentual = taxaPercentual,
                            valorJuros = valorJuros,
                            prestacaoMensal = prestacaoMensal,
                            quantidadePrestacoes = qtdPrestacoes,
                            prestacoesPagas = creditoToEdit?.prestacoesPagas ?: 0,
                            dataDesembolso = creditoToEdit?.dataDesembolso ?: nowTime,
                            dataPrimeiroPagamento = dataInicioReembolso,
                            dataUltimoPagamento = dataFimReembolso,
                            diasCredito = diasCreditoText.toIntOrNull() ?: 360,
                            estado = creditoToEdit?.estado ?: "Ativo",
                            valorEmAtraso = creditoToEdit?.valorEmAtraso ?: 0.0,
                            diasEmAtraso = creditoToEdit?.diasEmAtraso ?: 0,
                            proximoVencimento = if (creditoToEdit == null || creditoToEdit.prestacoesPagas == 0) dataInicioReembolso else (creditoToEdit.proximoVencimento.takeIf { it > 0 } ?: dataInicioReembolso)
                        )

                        onSaveCliente(cliente, credito)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_salvar_cliente"),
                colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Salvar Cliente e Gerar Crédito", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
