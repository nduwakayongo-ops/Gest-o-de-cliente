package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClienteEntity
import com.example.data.model.CreditoEntity
import com.example.ui.components.KwanzaFormatters
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarioScreen(
    clientesWithCredito: List<Pair<ClienteEntity, CreditoEntity?>>,
    onSelectCliente: (Long) -> Unit
) {
    var calendarMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDay by remember { mutableIntStateOf(calendarMonth.get(Calendar.DAY_OF_MONTH)) }

    val daysInMonth = calendarMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    val monthName = SimpleDateFormat("MMMM yyyy", Locale("pt", "AO")).format(calendarMonth.time)

    // Map due dates to calendar days
    val dueDaysMap = remember(clientesWithCredito, calendarMonth) {
        val map = mutableMapOf<Int, MutableList<Pair<ClienteEntity, CreditoEntity>>>()
        clientesWithCredito.forEach { (cliente, credito) ->
            if (credito != null && credito.proximoVencimento > 0) {
                val cal = Calendar.getInstance().apply { timeInMillis = credito.proximoVencimento }
                if (cal.get(Calendar.MONTH) == calendarMonth.get(Calendar.MONTH) &&
                    cal.get(Calendar.YEAR) == calendarMonth.get(Calendar.YEAR)
                ) {
                    val day = cal.get(Calendar.DAY_OF_MONTH)
                    map.getOrPut(day) { mutableListOf() }.add(Pair(cliente, credito))
                }
            }
        }
        map
    }

    val selectedDayClients = dueDaysMap[selectedDay] ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GrayBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Calendar Month Header
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        val newCal = calendarMonth.clone() as Calendar
                        newCal.add(Calendar.MONTH, -1)
                        calendarMonth = newCal
                    }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mês Anterior", tint = DeepBlue)
                    }

                    Text(
                        text = monthName.replaceFirstChar { it.uppercase() },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepBlue
                    )

                    IconButton(onClick = {
                        val newCal = calendarMonth.clone() as Calendar
                        newCal.add(Calendar.MONTH, 1)
                        calendarMonth = newCal
                    }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Próximo Mês", tint = DeepBlue)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Days of Week Labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb").forEach { day ->
                        Text(
                            text = day,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrayText,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Days Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items((1..daysInMonth).toList()) { day ->
                        val isSelected = day == selectedDay
                        val hasDues = dueDaysMap.containsKey(day)

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> DeepBlue
                                        hasDues -> AccentGold.copy(alpha = 0.3f)
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable { selectedDay = day },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$day",
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected || hasDues) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                if (hasDues && !isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(WarningRed)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected Day Details Header
        Text(
            text = "📅 Recebimentos do Dia $selectedDay de $monthName (${selectedDayClients.size})",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = DeepBlue
        )

        if (selectedDayClients.isEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nenhum vencimento agendado para esta data.", fontSize = 13.sp, color = GrayText)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(selectedDayClients) { (cliente, credito) ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectCliente(cliente.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(cliente.nome, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("${cliente.banco} • Tel: ${cliente.telefone}", fontSize = 11.sp, color = GrayText)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    KwanzaFormatters.formatAOA(credito.prestacaoMensal),
                                    fontWeight = FontWeight.Bold,
                                    color = DeepBlue,
                                    fontSize = 13.sp
                                )
                                Text(credito.estado, fontSize = 10.sp, color = ForestGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
