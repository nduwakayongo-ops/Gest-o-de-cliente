package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.KwanzaFormatters
import com.example.ui.theme.*
import com.example.ui.viewmodel.CalculadoraViewModel
import com.example.ui.viewmodel.TipoCalculoJuros

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
                        "Cálculo e simulação automática de juros, prestações e saldo devedor em tempo real.",
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
                        modifier = Modifier.fillMaxWidth().testTag("input_calc_valor"),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = taxaJurosText,
                            onValueChange = { calculadoraViewModel.updateTaxaJuros(it) },
                            label = { Text("Taxa (%)") },
                            modifier = Modifier.weight(1f).testTag("input_calc_taxa"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = prazoMesesText,
                            onValueChange = { calculadoraViewModel.updatePrazoMeses(it) },
                            label = { Text("Prazo (Meses)") },
                            modifier = Modifier.weight(1f).testTag("input_calc_prazo"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = parcelasPagasText,
                            onValueChange = { calculadoraViewModel.updateParcelasPagas(it) },
                            label = { Text("Pagas") },
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

                    // Hero Saldo Devedor Display
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = LightGreen,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("SALDO DEVEDOR ATUAL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
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
                                text = "Liquidado: ${String.format("%.1f%%", state.percentualQuitado)} • ${state.parcelasPagas} de ${state.prazoMeses} parcelas pagas",
                                fontSize = 10.sp,
                                color = GrayText
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricBox(
                            title = "Prestação Mensal",
                            value = KwanzaFormatters.formatAOA(state.prestacaoMensal),
                            color = DeepBlue,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBox(
                            title = "Juros Totais",
                            value = KwanzaFormatters.formatAOA(state.valorJurosTotal),
                            color = DarkGold,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricBox(
                            title = "Total a Liquidar",
                            value = KwanzaFormatters.formatAOA(state.valorTotalLiquidar),
                            color = DeepBlue,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBox(
                            title = "Total Já Pago",
                            value = KwanzaFormatters.formatAOA(state.totalPagoAteAgora),
                            color = ForestGreen,
                            modifier = Modifier.weight(1f)
                        )
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
                            Text("Saldo Restante: ${KwanzaFormatters.formatAOA(item.saldoDevedorRestante)}", fontSize = 10.sp, color = ForestGreen, fontWeight = FontWeight.SemiBold)
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
