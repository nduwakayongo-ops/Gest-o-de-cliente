package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.repository.DashboardSummary
import com.example.ui.components.KwanzaFormatters
import com.example.ui.theme.*

@Composable
fun EstatisticasScreen(
    summary: DashboardSummary
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Desempenho, 1: Simulador de Risco

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GrayBackground)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = DeepBlue
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("📊 Evolução & Banco", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("🧮 Simulador de Risco", fontWeight = FontWeight.Bold) }
            )
        }

        when (selectedTab) {
            0 -> EvolucaoEstatisticasView(summary)
            1 -> SimuladorRiscoView()
        }
    }
}

@Composable
fun EvolucaoEstatisticasView(summary: DashboardSummary) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Recovery Rate Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("📈 Taxa de Recuperação da Carteira", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                Spacer(modifier = Modifier.height(10.dp))

                val recoveryPercent = if (summary.totalDesembolsado > 0) (summary.totalRecuperado / summary.totalDesembolsado) * 100 else 0.0

                LinearProgressIndicator(
                    progress = { (recoveryPercent / 100).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp),
                    color = ForestGreen,
                    trackColor = LightGreen
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Recuperado: ${KwanzaFormatters.formatAOA(summary.totalRecuperado)}", fontSize = 12.sp, color = ForestGreen, fontWeight = FontWeight.Bold)
                    Text("Taxa: ${String.format("%.1f%%", recoveryPercent)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Risk Level Gauge Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("📉 Índice de Risco Global", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                Spacer(modifier = Modifier.height(8.dp))

                val risk = summary.riscoPercentual

                LinearProgressIndicator(
                    progress = { (risk / 100).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp),
                    color = if (risk > 15) WarningRed else DarkGold,
                    trackColor = GraySurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Em Atraso: ${KwanzaFormatters.formatAOA(summary.valorEmAtraso)}", fontSize = 12.sp, color = WarningRed)
                    Text("Índice de Risco: ${String.format("%.2f%%", risk)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (risk > 15) WarningRed else DarkGold)
                }
            }
        }

        // Distribution by Bank Mock / Representative Visuals
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🏛 Distribuição da Carteira por Banco", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                Spacer(modifier = Modifier.height(12.dp))

                BankDistributionBar("BAI", 0.35f, "AOA 8.225.000", ForestGreen)
                BankDistributionBar("BFA", 0.25f, "AOA 5.875.000", DeepBlue)
                BankDistributionBar("Banco Económico", 0.20f, "AOA 4.700.000", DarkGold)
                BankDistributionBar("Banco Atlântico", 0.12f, "AOA 2.820.000", DeepBlueLight)
                BankDistributionBar("Outros", 0.08f, "AOA 1.880.000", GrayText)
            }
        }
    }
}

@Composable
fun BankDistributionBar(banco: String, fraction: Float, amount: String, color: Color) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(banco, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(amount, fontSize = 12.sp, color = GrayText)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = color,
            trackColor = GraySurface
        )
    }
}

@Composable
fun SimuladorRiscoView() {
    var salarioText by remember { mutableStateOf("400000") }
    var valorSolicitadoText by remember { mutableStateOf("2000000") }
    var parcelasText by remember { mutableStateOf("18") }
    var taxaText by remember { mutableStateOf("5") }

    val salario = salarioText.toDoubleOrNull() ?: 0.0
    val valorSolicitado = valorSolicitadoText.toDoubleOrNull() ?: 0.0
    val parcelas = parcelasText.toIntOrNull() ?: 12
    val taxa = taxaText.toDoubleOrNull() ?: 0.0

    val jurosTotal = valorSolicitado * (taxa / 100.0)
    val prestacaoEstimada = if (parcelas > 0) (valorSolicitado + jurosTotal) / parcelas else 0.0

    val taxaEsforco = if (salario > 0) (prestacaoEstimada / salario) * 100 else 0.0
    val maxRecomendado = salario * 0.35 * parcelas / (1.0 + (taxa / 100.0))

    val (nivelRisco, corRisco) = when {
        taxaEsforco <= 35.0 -> Pair("Risco Baixo (Aprovável)", ForestGreen)
        taxaEsforco <= 50.0 -> Pair("Risco Médio (Atenção)", DarkGold)
        else -> Pair("Risco Alto (Reprovável)", WarningRed)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("🧮 Simulador de Risco e Capacidade de Crédito", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DeepBlue)

                OutlinedTextField(
                    value = salarioText,
                    onValueChange = { salarioText = it },
                    label = { Text("Salário Líquido Mensal do Cliente (AOA)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_simulador_salario"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = valorSolicitadoText,
                    onValueChange = { valorSolicitadoText = it },
                    label = { Text("Valor Solicitado do Crédito (AOA)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_simulador_valor"),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = parcelasText,
                        onValueChange = { parcelasText = it },
                        label = { Text("Nº Prestações") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = taxaText,
                        onValueChange = { taxaText = it },
                        label = { Text("Taxa (%)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        }

        // Result Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DeepBlue),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Resultado da Simulação", fontSize = 13.sp, color = AccentGold, fontWeight = FontWeight.Bold)

                Text(
                    text = "Prestação Mensal Estimada: ${KwanzaFormatters.formatAOA(prestacaoEstimada)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "Taxa de Esforço (DTI): ${String.format("%.1f%%", taxaEsforco)} do Salário",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = corRisco,
                    contentColor = Color.White
                ) {
                    Text(
                        text = nivelRisco,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Máximo Recomendado (35% limite): ${KwanzaFormatters.formatAOA(maxRecomendado.coerceAtLeast(0.0))}",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}
