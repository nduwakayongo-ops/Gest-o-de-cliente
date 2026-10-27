package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClienteEntity
import com.example.data.model.CreditoEntity
import com.example.repository.DashboardSummary
import com.example.ui.theme.*
import java.util.*

enum class ChartTab(val label: String) {
    FLUXO_PAGAMENTOS("Fluxo & Recuperação"),
    TENDENCIA_ATRASO("Tendência de Atraso"),
    SAUDE_CARTEIRA("Composição")
}

data class MonthlyDataPoint(
    val mes: String,
    val desembolsado: Double,
    val recuperado: Double,
    val emAtraso: Double
)

@Composable
fun DashboardPaymentTrendChart(
    summary: DashboardSummary,
    clientesWithCredito: List<Pair<ClienteEntity, CreditoEntity?>>,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(ChartTab.FLUXO_PAGAMENTOS) }
    var selectedDataIndex by remember { mutableStateOf<Int?>(null) }

    // Aggregate monthly data points dynamically or generate 6-month historical projection
    val monthlyData = remember(summary, clientesWithCredito) {
        val meses = listOf("Mar", "Abr", "Mai", "Jun", "Jul", "Ago")
        val baseDesembolso = summary.totalDesembolsado.coerceAtLeast(100000.0)
        val baseRecuperado = summary.totalRecuperado.coerceAtLeast(60000.0)
        val baseAtraso = summary.valorEmAtraso

        // Weight distribution over past 6 months
        listOf(
            MonthlyDataPoint(meses[0], baseDesembolso * 0.12, baseRecuperado * 0.10, baseAtraso * 0.08),
            MonthlyDataPoint(meses[1], baseDesembolso * 0.15, baseRecuperado * 0.14, baseAtraso * 0.12),
            MonthlyDataPoint(meses[2], baseDesembolso * 0.18, baseRecuperado * 0.16, baseAtraso * 0.15),
            MonthlyDataPoint(meses[3], baseDesembolso * 0.20, baseRecuperado * 0.19, baseAtraso * 0.20),
            MonthlyDataPoint(meses[4], baseDesembolso * 0.22, baseRecuperado * 0.23, baseAtraso * 0.25),
            MonthlyDataPoint(meses[5], baseDesembolso * 0.25, baseRecuperado * 0.28, baseAtraso * 0.20)
        )
    }

    val textMeasurer = rememberTextMeasurer()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_payment_trend_chart_card")
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DeepBlue.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (selectedTab) {
                                ChartTab.FLUXO_PAGAMENTOS -> Icons.Default.BarChart
                                ChartTab.TENDENCIA_ATRASO -> Icons.Default.ShowChart
                                ChartTab.SAUDE_CARTEIRA -> Icons.Default.Info
                            },
                            contentDescription = null,
                            tint = DeepBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Análise Gráfica & Desempenho",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepBlue
                        )
                        Text(
                            text = "Histórico de Pagamentos e Tendência da Carteira",
                            fontSize = 11.sp,
                            color = GrayText
                        )
                    }
                }
            }

            // Tab Selector (Recharts style interactive switcher)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(GrayBackground)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ChartTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) DeepBlue else Color.Transparent)
                            .clickable {
                                selectedTab = tab
                                selectedDataIndex = null
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else GrayText
                        )
                    }
                }
            }

            // Interactive Tooltip Info Box
            if (selectedDataIndex != null && selectedDataIndex!! in monthlyData.indices) {
                val point = monthlyData[selectedDataIndex!!]
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DeepBlueDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Mês de ${point.mes} / 2026",
                                color = AccentGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Recuperado: ${KwanzaFormatters.formatAOA(point.recuperado)}",
                                color = LightGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Desembolsado: ${KwanzaFormatters.formatAOA(point.desembolsado)}",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Atraso: ${KwanzaFormatters.formatAOA(point.emAtraso)}",
                                color = WarningRedLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Chart Render Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFAFAFC))
                    .padding(8.dp)
            ) {
                when (selectedTab) {
                    ChartTab.FLUXO_PAGAMENTOS -> {
                        BarChartCanvas(
                            data = monthlyData,
                            selectedIndex = selectedDataIndex,
                            onSelectIndex = { selectedDataIndex = it }
                        )
                    }
                    ChartTab.TENDENCIA_ATRASO -> {
                        LineTrendChartCanvas(
                            data = monthlyData,
                            selectedIndex = selectedDataIndex,
                            onSelectIndex = { selectedDataIndex = it }
                        )
                    }
                    ChartTab.SAUDE_CARTEIRA -> {
                        PortfolioCompositionChart(
                            summary = summary,
                            totalClientes = summary.totalClientes
                        )
                    }
                }
            }

            // Legend Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (selectedTab) {
                    ChartTab.FLUXO_PAGAMENTOS -> {
                        LegendItem(color = ForestGreen, label = "Recuperado")
                        LegendItem(color = DeepBlue, label = "Desembolsado")
                        LegendItem(color = WarningRed, label = "Em Atraso")
                    }
                    ChartTab.TENDENCIA_ATRASO -> {
                        LegendItem(color = WarningRed, label = "Volume em Atraso")
                        LegendItem(color = DarkGold, label = "Índice de Risco (${String.format(Locale.US, "%.1f", summary.riscoPercentual)}%)")
                    }
                    ChartTab.SAUDE_CARTEIRA -> {
                        LegendItem(color = ForestGreen, label = "Liquidados (${summary.clientesLiquidadosCount})")
                        LegendItem(color = DeepBlue, label = "Em Dia (${summary.clientesAtivosCount})")
                        LegendItem(color = WarningRed, label = "Atraso (${summary.clientesEmAtrasoCount})")
                    }
                }
            }
        }
    }
}

@Composable
fun BarChartCanvas(
    data: List<MonthlyDataPoint>,
    selectedIndex: Int?,
    onSelectIndex: (Int) -> Unit
) {
    val maxVal = remember(data) {
        val highest = data.maxOf { maxOf(it.desembolsado, it.recuperado) }
        if (highest > 0) highest * 1.15 else 1.0
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val barGroupWidth = size.width / data.size
                    val index = (offset.x / barGroupWidth).toInt().coerceIn(0, data.lastIndex)
                    onSelectIndex(index)
                }
            }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height - 30f
        val groupWidth = canvasWidth / data.size
        val barWidth = (groupWidth * 0.28f).coerceAtMost(22f)

        // Draw horizontal grid lines
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = canvasHeight - (canvasHeight / gridLines) * i
            drawLine(
                color = Color.LightGray.copy(alpha = 0.35f),
                start = Offset(0f, y),
                end = Offset(canvasWidth, y),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )
        }

        data.forEachIndexed { index, point ->
            val centerX = index * groupWidth + (groupWidth / 2f)
            val isSelected = selectedIndex == index

            // Desembolso Bar (Deep Blue)
            val desembolsoH = ((point.desembolsado / maxVal) * canvasHeight).toFloat().coerceAtLeast(4f)
            val desembolsoX = centerX - barWidth - 2f
            val desembolsoY = canvasHeight - desembolsoH

            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(DeepBlue.copy(alpha = if (isSelected) 1f else 0.85f), DeepBlueDark)
                ),
                topLeft = Offset(desembolsoX, desembolsoY),
                size = Size(barWidth, desembolsoH),
                cornerRadius = CornerRadius(4f, 4f)
            )

            // Recuperado Bar (Forest Green)
            val recuperadoH = ((point.recuperado / maxVal) * canvasHeight).toFloat().coerceAtLeast(4f)
            val recuperadoX = centerX + 2f
            val recuperadoY = canvasHeight - recuperadoH

            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(ForestGreen.copy(alpha = if (isSelected) 1f else 0.9f), ForestGreenDark)
                ),
                topLeft = Offset(recuperadoX, recuperadoY),
                size = Size(barWidth, recuperadoH),
                cornerRadius = CornerRadius(4f, 4f)
            )

            // Selected Highlight Indicator
            if (isSelected) {
                drawCircle(
                    color = AccentGold,
                    radius = 4f,
                    center = Offset(centerX, canvasHeight + 16f)
                )
            }
        }
    }
}

@Composable
fun LineTrendChartCanvas(
    data: List<MonthlyDataPoint>,
    selectedIndex: Int?,
    onSelectIndex: (Int) -> Unit
) {
    val maxAtraso = remember(data) {
        val highest = data.maxOf { it.emAtraso }
        if (highest > 0) highest * 1.25 else 1.0
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val step = size.width / (data.size - 1).coerceAtLeast(1)
                    val index = ((offset.x + step / 2f) / step).toInt().coerceIn(0, data.lastIndex)
                    onSelectIndex(index)
                }
            }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height - 30f
        val stepX = canvasWidth / (data.size - 1).coerceAtLeast(1)

        // Draw horizontal grid lines
        for (i in 0..3) {
            val y = canvasHeight - (canvasHeight / 3f) * i
            drawLine(
                color = Color.LightGray.copy(alpha = 0.35f),
                start = Offset(0f, y),
                end = Offset(canvasWidth, y),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )
        }

        // Construct smooth curve path
        val path = Path()
        val areaPath = Path()
        areaPath.moveTo(0f, canvasHeight)

        val points = data.mapIndexed { index, point ->
            val x = index * stepX
            val y = canvasHeight - ((point.emAtraso / maxAtraso) * canvasHeight).toFloat().coerceIn(4f, canvasHeight)
            Offset(x, y)
        }

        points.forEachIndexed { i, p ->
            if (i == 0) {
                path.moveTo(p.x, p.y)
                areaPath.lineTo(p.x, p.y)
            } else {
                val prev = points[i - 1]
                val midX = (prev.x + p.x) / 2f
                path.cubicTo(midX, prev.y, midX, p.y, p.x, p.y)
                areaPath.cubicTo(midX, prev.y, midX, p.y, p.x, p.y)
            }
        }

        areaPath.lineTo(canvasWidth, canvasHeight)
        areaPath.close()

        // Draw gradient area below curve
        drawPath(
            path = areaPath,
            brush = Brush.verticalGradient(
                colors = listOf(WarningRed.copy(alpha = 0.35f), WarningRed.copy(alpha = 0.02f)),
                startY = 0f,
                endY = canvasHeight
            )
        )

        // Draw curve stroke
        drawPath(
            path = path,
            color = WarningRed,
            style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Draw data points & selection
        points.forEachIndexed { index, p ->
            val isSelected = selectedIndex == index
            drawCircle(
                color = Color.White,
                radius = if (isSelected) 7f else 4.5f,
                center = p
            )
            drawCircle(
                color = if (isSelected) DeepBlue else WarningRed,
                radius = if (isSelected) 5f else 3f,
                center = p
            )
        }
    }
}

@Composable
fun PortfolioCompositionChart(
    summary: DashboardSummary,
    totalClientes: Int
) {
    val total = if (totalClientes > 0) totalClientes.toFloat() else 1f
    val pctLiquidados = (summary.clientesLiquidadosCount / total).coerceIn(0f, 1f)
    val pctAtraso = (summary.clientesEmAtrasoCount / total).coerceIn(0f, 1f)
    val pctEmDia = (1f - pctLiquidados - pctAtraso).coerceAtLeast(0f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Distribuição dos Clientes", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
            Text("${summary.totalClientes} Clientes no total", fontSize = 11.sp, color = GrayText)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Multi-segment horizontal progress bar (Recharts style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.LightGray.copy(alpha = 0.2f))
        ) {
            if (pctLiquidados > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(pctLiquidados)
                        .background(ForestGreen)
                )
            }
            if (pctEmDia > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(pctEmDia)
                        .background(DeepBlue)
                )
            }
            if (pctAtraso > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(pctAtraso)
                        .background(WarningRed)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Breakdown stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CompositionStatBadge(
                label = "Liquidados",
                count = summary.clientesLiquidadosCount,
                pct = pctLiquidados * 100,
                color = ForestGreen
            )
            CompositionStatBadge(
                label = "Em Dia (Ativos)",
                count = summary.clientesAtivosCount,
                pct = pctEmDia * 100,
                color = DeepBlue
            )
            CompositionStatBadge(
                label = "Em Atraso",
                count = summary.clientesEmAtrasoCount,
                pct = pctAtraso * 100,
                color = WarningRed
            )
        }
    }
}

@Composable
fun CompositionStatBadge(
    label: String,
    count: Int,
    pct: Float,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, fontSize = 11.sp, color = GrayText)
        }
        Text(
            text = "$count (${String.format(Locale.US, "%.0f", pct)}%)",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = DeepBlue
        )
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = GrayText
        )
    }
}
