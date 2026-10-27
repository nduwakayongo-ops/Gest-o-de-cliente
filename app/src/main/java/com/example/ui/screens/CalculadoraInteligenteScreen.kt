package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.KwanzaFormatters
import com.example.ui.theme.*
import com.example.ui.viewmodel.CalculadoraViewModel
import com.example.ui.viewmodel.TipoCalculoJuros
import com.example.util.LembreteHelper
import com.example.util.PdfGeneratorHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculadoraInteligenteScreen(
    calculadoraViewModel: CalculadoraViewModel,
    onApplyToNewCliente: (Double, Double, Int) -> Unit,
    onSpeakSummary: (String) -> Unit
) {
    val valorDesembolsadoText by calculadoraViewModel.valorDesembolsadoInput.collectAsStateWithLifecycle()
    val taxaJurosText by calculadoraViewModel.taxaJurosInput.collectAsStateWithLifecycle()
    val prazoMesesText by calculadoraViewModel.prazoMesesInput.collectAsStateWithLifecycle()
    val parcelasPagasText by calculadoraViewModel.parcelasPagasInput.collectAsStateWithLifecycle()
    val tipoCalculo by calculadoraViewModel.tipoCalculo.collectAsStateWithLifecycle()
    val state by calculadoraViewModel.calculoState.collectAsStateWithLifecycle()

    var showSchedule by remember { mutableStateOf(false) }
    var showWhatsAppDialog by remember { mutableStateOf(false) }
    var clienteNomeInput by remember { mutableStateOf("") }
    var clienteTelefoneInput by remember { mutableStateOf("") }
    val context = LocalContext.current

    if (showWhatsAppDialog) {
        AlertDialog(
            onDismissRequest = { showWhatsAppDialog = false },
            title = {
                Text("Enviar Simulação ao Cliente", fontWeight = FontWeight.Bold, color = DeepBlue)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Envie a simulação oficial diretamente ao cliente. Funciona mesmo com o aparelho offline (o WhatsApp e SMS guardam e enviam diretamente na rede).",
                        fontSize = 12.sp,
                        color = GrayText
                    )
                    OutlinedTextField(
                        value = clienteNomeInput,
                        onValueChange = { clienteNomeInput = it },
                        label = { Text("Nome do Cliente (opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = clienteTelefoneInput,
                        onValueChange = { clienteTelefoneInput = it },
                        label = { Text("Telefone / WhatsApp") },
                        placeholder = { Text("Ex: 923 000 000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showWhatsAppDialog = false
                        val nomeStr = if (clienteNomeInput.isNotBlank()) " para $clienteNomeInput" else ""
                        val msg = """
                            *MBULI-MICROCRÉDITO • SIMULAÇÃO DE CRÉDITO*
                            *Gestor Responsável:* Nduwa Kayongo
                            ${if (clienteNomeInput.isNotBlank()) "*Cliente:* $clienteNomeInput\n" else ""}
                            *Capital:* ${KwanzaFormatters.formatAOA(state.principal)}
                            *Taxa de Juros:* ${state.taxaPercentual}% ao mês
                            *Prazo:* ${state.prazoMeses} meses (${state.tipoCalculo.label})
                            ------------------------------------
                            *Prestação Mensal:* ${KwanzaFormatters.formatAOA(state.prestacaoMensal)}
                            *Total de Juros:* ${KwanzaFormatters.formatAOA(state.valorJurosTotal)}
                            *Total a Liquidar:* ${KwanzaFormatters.formatAOA(state.valorTotalLiquidar)}
                            ------------------------------------
                            _Plano completo de pagamentos com amortização até à liquidação total._
                        """.trimIndent()
                        LembreteHelper.enviarWhatsApp(context, clienteTelefoneInput, msg)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                ) {
                    Text("WhatsApp (Direto)")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            showWhatsAppDialog = false
                            val msg = "MBULI-MICROCRÉDITO: Simulação para ${clienteNomeInput.ifBlank { "Cliente" }}: Capital: ${KwanzaFormatters.formatAOA(state.principal)}, Prazo: ${state.prazoMeses} meses, Prestação: ${KwanzaFormatters.formatAOA(state.prestacaoMensal)}, Total a Liquidar: ${KwanzaFormatters.formatAOA(state.valorTotalLiquidar)}. Gestor: Nduwa Kayongo."
                            LembreteHelper.enviarSms(context, clienteTelefoneInput, msg)
                        }
                    ) {
                        Text("SMS Offline", color = DeepBlue)
                    }
                    TextButton(onClick = { showWhatsAppDialog = false }) {
                        Text("Cancelar")
                    }
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(GrayBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DeepBlue),
                modifier = Modifier.fillMaxWidth().testTag("calculadora_inteligente_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Calculate, contentDescription = null, tint = AccentGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("⚡ Calculadora Inteligente", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(
                            onClick = {
                                val resumoTexto = "Cálculo do Saldo Devedor. Valor do crédito: ${KwanzaFormatters.formatAOA(state.principal)}. " +
                                        "Taxa: ${state.taxaPercentual} por cento. Prazo: ${state.prazoMeses} meses. " +
                                        "Prestação mensal: ${KwanzaFormatters.formatAOA(state.prestacaoMensal)}. " +
                                        "Juros total: ${KwanzaFormatters.formatAOA(state.valorJurosTotal)}. " +
                                        "Saldo devedor atual: ${KwanzaFormatters.formatAOA(state.saldoDevedorAtual)}."
                                onSpeakSummary(resumoTexto)
                            }
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Ouvir Resumo", tint = AccentGold)
                        }
                    }
                    Text(
                        "Cálculo inteligente de juros, prestações e saldo devedor em tempo real.",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Section: Form Inputs
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("⚙ Parâmetros do Crédito", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DeepBlue)

                    OutlinedTextField(
                        value = valorDesembolsadoText,
                        onValueChange = { calculadoraViewModel.updateValorDesembolsado(it) },
                        label = { Text("Valor Desembolsado / Capital (AOA)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("input_calc_valor"),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = taxaJurosText,
                            onValueChange = { calculadoraViewModel.updateTaxaJuros(it) },
                            label = { Text("Taxa (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("input_calc_taxa"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = prazoMesesText,
                            onValueChange = { calculadoraViewModel.updatePrazoMeses(it) },
                            label = { Text("Prazo (Meses)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("input_calc_prazo"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = parcelasPagasText,
                            onValueChange = { calculadoraViewModel.updateParcelasPagas(it) },
                            label = { Text("Pagas") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("input_calc_pagas"),
                            singleLine = true
                        )
                    }

                    Text("Sistema de Amortização:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TipoCalculoJuros.values().forEach { tipo ->
                            FilterChip(
                                selected = tipoCalculo == tipo,
                                onClick = { calculadoraViewModel.updateTipoCalculo(tipo) },
                                label = { Text(tipo.label.split(" ")[0], fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        // Section: Live Results Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("📊 Resultados Automáticos em Tempo Real", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ForestGreen)

                    // Hero Saldo de Capital Display (Carteira)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = LightGreen,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("SALDO DE CAPITAL EM DÍVIDA (CARTEIRA)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = KwanzaFormatters.formatAOA(state.saldoDevedorAtual),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeepBlue
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { (state.percentualQuitado / 100.0).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = ForestGreen,
                                trackColor = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Capital Amortizado: ${KwanzaFormatters.formatAOA(state.capitalAmortizadoAteAgora)} (${String.format("%.1f%%", state.percentualQuitado)}) • ${state.parcelasPagas} de ${state.prazoMeses} parcelas pagas",
                                fontSize = 10.sp,
                                color = GrayText
                            )
                        }
                    }

                    // Card de Destaque Oficial: COMPOSIÇÃO DA PRESTAÇÃO
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF0FDF4),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ForestGreen.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Calculate, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("COMPOSIÇÃO DA PRESTAÇÃO", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DeepBlue)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Prestação mensal = Capital mensal + Juros mensal",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ForestGreen
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${KwanzaFormatters.formatAOA(state.prestacaoMensal)} = ${KwanzaFormatters.formatAOA(state.capitalMensal)} + ${KwanzaFormatters.formatAOA(state.jurosMensal)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeepBlue
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = ForestGreen.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("• Capital mensal: ${KwanzaFormatters.formatAOA(state.principal)} ÷ ${state.prazoMeses} = ${KwanzaFormatters.formatAOA(state.capitalMensal)}", fontSize = 10.sp, color = DeepBlue)
                            Text("• Juros mensal: ${KwanzaFormatters.formatAOA(state.principal)} × ${state.taxaPercentual}% = ${KwanzaFormatters.formatAOA(state.jurosMensal)}", fontSize = 10.sp, color = DeepBlue)
                            Text("• REGRA DE OURO: Somente o Capital Amortizado reduz a carteira. Os juros recebidos são receita financeira.", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = DarkGold)
                        }
                    }

                    // Grid de Métricas Principais
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricBox(
                            title = "Capital Desembolsado",
                            value = KwanzaFormatters.formatAOA(state.principal),
                            color = DeepBlue,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBox(
                            title = "Prestação Mensal",
                            value = KwanzaFormatters.formatAOA(state.prestacaoMensal),
                            color = ForestGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricBox(
                            title = "Capital Amortizado",
                            value = KwanzaFormatters.formatAOA(state.capitalAmortizadoAteAgora),
                            color = ForestGreen,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBox(
                            title = "Juros Recebidos (Receita)",
                            value = KwanzaFormatters.formatAOA(state.jurosRecebidosAteAgora),
                            color = DarkGold,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricBox(
                            title = "Total Recebido (Cap + Jur)",
                            value = KwanzaFormatters.formatAOA(state.totalPagoAteAgora),
                            color = DeepBlue,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBox(
                            title = "Saldo da Carteira (Dívida)",
                            value = KwanzaFormatters.formatAOA(state.saldoDevedorAtual),
                            color = WarningRed,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Ações Oficiais: Baixar PDF e Enviar WhatsApp (Offline)
                    Button(
                        onClick = {
                            val pdf = PdfGeneratorHelper.generateSimulacaoCreditoPdf(
                                context = context,
                                state = state,
                                nomeCliente = clienteNomeInput.ifBlank { null }
                            )
                            if (pdf != null) {
                                Toast.makeText(context, "PDF da simulação gerado com sucesso!", Toast.LENGTH_SHORT).show()
                                PdfGeneratorHelper.openOrSharePdf(context, pdf)
                            } else {
                                Toast.makeText(context, "Erro ao gerar PDF da simulação.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("btn_baixar_simulador_pdf")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Baixar Tabela Completa em PDF (Até Liquidação)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { showWhatsAppDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepBlue),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ForestGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("btn_enviar_simulacao_whatsapp")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Enviar Simulação por WhatsApp / SMS (Offline)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showSchedule = !showSchedule },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (showSchedule) "Ocultar Cronograma" else "Ver Tabela de Parcelas", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { onApplyToNewCliente(state.principal, state.taxaPercentual, state.prazoMeses) },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Usar em Cadastro", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // Section: Cronograma de Amortizacao (Optional Expandable)
        if (showSchedule && state.cronograma.isNotEmpty()) {
            item {
                Text("📅 Cronograma de Parcelas e Saldo Devedor Projetado", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
            }

            items(state.cronograma) { item ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = if (item.isPaga) LightGreen.copy(alpha = 0.5f) else Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (item.isPaga) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Paga", tint = ForestGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Column {
                                Text("Parcela #${item.numeroParcela}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Amortização: ${KwanzaFormatters.formatAOA(item.valorAmortizacao)} • Juros: ${KwanzaFormatters.formatAOA(item.valorJuros)}", fontSize = 10.sp, color = GrayText)
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(KwanzaFormatters.formatAOA(item.valorPrestacao), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                            Text("Capital em Dívida: ${KwanzaFormatters.formatAOA(item.saldoDevedorRestante)}", fontSize = 10.sp, color = ForestGreen, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricBox(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.08f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, fontSize = 10.sp, color = GrayText, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
