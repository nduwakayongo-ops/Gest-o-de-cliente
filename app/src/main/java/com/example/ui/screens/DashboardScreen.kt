package com.example.ui.screens

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClienteEntity
import com.example.data.model.CreditoEntity
import com.example.repository.DashboardSummary
import com.example.ui.components.DashboardPaymentTrendChart
import com.example.ui.components.KwanzaFormatters
import com.example.ui.components.MetricCard
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
    summary: DashboardSummary,
    recentClientes: List<Pair<ClienteEntity, CreditoEntity?>>,
    onNavigateToClientes: () -> Unit,
    onNavigateToAddCliente: () -> Unit,
    onNavigateToAlertas: () -> Unit,
    onNavigateToRelatorios: () -> Unit,
    onNavigateToCalculadora: () -> Unit,
    onSelectCliente: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(GrayBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Active Portfolio Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Unspecified),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("carteira_ativa_card")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(DeepBlueDark, DeepBlue)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = AccentGold,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "📊 Carteira Ativa",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = ForestGreen,
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = "Ativa",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = KwanzaFormatters.formatAOA(summary.carteiraAtiva),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Capital atualmente em dívida (Desembolsado − Amortizado)",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Capital Desembolsado", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                                Text(
                                    KwanzaFormatters.formatAOA(summary.totalDesembolsado),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Capital Amortizado", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                                Text(
                                    KwanzaFormatters.formatAOA(summary.capitalAmortizado),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AccentGold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Juros Recebidos (Receita)", fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.7f))
                                Text(
                                    KwanzaFormatters.formatAOA(summary.jurosRecebidos),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF6EE7B7) // Light mint green
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Total Recebido (Cap + Jur)", fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.7f))
                                Text(
                                    KwanzaFormatters.formatAOA(summary.totalRecebido),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Key Indicators Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Indicadores Principais",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "👥 Clientes",
                        value = "${summary.totalClientes}",
                        subtitle = "${summary.clientesAtivosCount} ativos • ${summary.clientesLiquidadosCount} liquidados",
                        icon = Icons.Default.People,
                        iconColor = DeepBlue,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToClientes() }
                    )
                    MetricCard(
                        title = "💰 Desembolsos",
                        value = KwanzaFormatters.formatAOA(summary.totalDesembolsado),
                        subtitle = "Total concedido",
                        icon = Icons.Default.Payments,
                        iconColor = ForestGreen,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "📉 Capital Amortizado",
                        value = KwanzaFormatters.formatAOA(summary.capitalAmortizado),
                        subtitle = "Reduziu a carteira",
                        icon = Icons.Default.TrendingDown,
                        iconColor = ForestGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "💵 Juros Recebidos",
                        value = KwanzaFormatters.formatAOA(summary.jurosRecebidos),
                        subtitle = "Receita acumulada",
                        icon = Icons.Default.MonetizationOn,
                        iconColor = DarkGold,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "💳 Total Recebido",
                        value = KwanzaFormatters.formatAOA(summary.totalRecebido),
                        subtitle = "Capital + Juros",
                        icon = Icons.Default.AccountBalance,
                        iconColor = DeepBlue,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "⚠ Em Atraso",
                        value = KwanzaFormatters.formatAOA(summary.valorEmAtraso),
                        subtitle = "${summary.clientesEmAtrasoCount} clientes em atraso",
                        icon = Icons.Default.Warning,
                        iconColor = WarningRed,
                        backgroundColor = WarningRedLight.copy(alpha = 0.3f),
                        borderColor = WarningRed,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToAlertas() }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "📉 Risco",
                        value = String.format("%.2f%%", summary.riscoPercentual),
                        subtitle = "Atraso / Carteira Ativa",
                        icon = Icons.Default.PieChart,
                        iconColor = DarkGold,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "⏳ Prestações",
                        value = "${summary.prestacoesVencendoCount}",
                        subtitle = "Vencem em até 5 dias",
                        icon = Icons.Default.Schedule,
                        iconColor = DeepBlue,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToAlertas() }
                    )
                }
            }
        }

        // Analytics Chart (Recharts style: Pagamentos, Recuperação & Tendência de Atrasos)
        item {
            DashboardPaymentTrendChart(
                summary = summary,
                clientesWithCredito = recentClientes
            )
        }

        // Quick Actions
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Ações Rápidas",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        QuickActionButton(
                            icon = Icons.Default.PersonAdd,
                            label = "Novo Cliente",
                            onClick = onNavigateToAddCliente
                        )
                        QuickActionButton(
                            icon = Icons.Default.Calculate,
                            label = "Calculadora",
                            onClick = onNavigateToCalculadora
                        )
                        QuickActionButton(
                            icon = Icons.Default.NotificationsActive,
                            label = "Alertas (${summary.notificacoesCount})",
                            onClick = onNavigateToAlertas
                        )
                        QuickActionButton(
                            icon = Icons.Default.PictureAsPdf,
                            label = "Relatórios",
                            onClick = onNavigateToRelatorios
                        )
                    }
                }
            }
        }

        // Continuous Monthly Tracking Banner
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DeepBlue),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToAlertas() }
                    .testTag("card_acompanhamento_mensal_dashboard")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = AccentGold, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("ACOMPANHAMENTO MENSAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AccentGold, letterSpacing = 0.8.sp)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Lembretes às 08:00 & 16:00 Ativos", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Acompanhando reembolsos até à liquidação total.", fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                    Button(
                        onClick = onNavigateToAlertas,
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Ver Todos", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Prestações em Atraso & Próximos Vencimentos List Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔔 Clientes em Destaque / Atraso",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(
                    onClick = onNavigateToClientes,
                    modifier = Modifier.testTag("ver_todos_clientes_btn")
                ) {
                    Text("Ver Todos", color = DeepBlue, fontWeight = FontWeight.Bold)
                }
            }
        }

        // List of Highlighted Clients
        items(recentClientes.take(4)) { (cliente, credito) ->
            ClientDashboardItem(
                cliente = cliente,
                credito = credito,
                onClick = { onSelectCliente(cliente.id) }
            )
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(DeepBlue.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = DeepBlue,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ClientDashboardItem(
    cliente: ClienteEntity,
    credito: CreditoEntity?,
    onClick: () -> Unit
) {
    val isAtraso = credito?.estado == "Em atraso" || (credito?.diasEmAtraso ?: 0) > 0
    val isLiquidado = credito?.estado == "Liquidado"

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isAtraso) WarningRedLight else DeepBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cliente.nome.take(2).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = if (isAtraso) WarningRed else DeepBlue,
                        fontSize = 14.sp
                    )
                }

                Column {
                    Text(
                        text = cliente.nome,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${cliente.banco} • ${cliente.numeroInterno}",
                        fontSize = 11.sp,
                        color = GrayText
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = KwanzaFormatters.formatAOA(credito?.prestacaoMensal ?: 0.0),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isAtraso) WarningRed else DeepBlue
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isAtraso -> WarningRed
                        isLiquidado -> ForestGreen
                        else -> DeepBlue
                    },
                    contentColor = Color.White,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = when {
                            isAtraso -> "⚠ Atraso ${credito?.diasEmAtraso ?: 0}d"
                            isLiquidado -> "Liquidado"
                            else -> "Ativo"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
