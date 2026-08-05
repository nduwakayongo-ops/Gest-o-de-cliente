package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.example.ui.components.KwanzaFormatters
import com.example.ui.theme.*

@Composable
fun AlertasScreen(
    clientesWithCredito: List<Pair<ClienteEntity, CreditoEntity?>>,
    onSelectCliente: (Long) -> Unit,
    onSpeakCliente: (ClienteEntity, CreditoEntity?) -> Unit = { _, _ -> },
    onSpeakAll: () -> Unit = {},
    onStopSpeak: () -> Unit = {},
    isSpeaking: Boolean = false
) {
    val context = LocalContext.current

    // Filter alerts into Overdue and Upcoming
    val alertasOverdue = clientesWithCredito.filter { (_, credito) ->
        credito != null && (credito.estado == "Em atraso" || credito.diasEmAtraso > 0)
    }

    val alertasUpcoming = clientesWithCredito.filter { (_, credito) ->
        if (credito == null) false else {
            val days = KwanzaFormatters.calculateDaysUntilOrOverdue(credito.proximoVencimento)
            days in 0..5 && credito.estado != "Liquidado"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GrayBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DeepBlue),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🔔 Notificações Diárias Ativas (08:00 e 17:00)",
                            color = AccentGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${alertasOverdue.size + alertasUpcoming.size} Alertas (Vencimento em ≤5 dias ou Atrasados)",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Lembretes automáticos enviados diariamente às 08:00 e 17:00 no telemóvel até a liquidação do crédito.",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(32.dp).padding(start = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (isSpeaking) onStopSpeak() else onSpeakAll()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSpeaking) WarningRed else AccentGold,
                            contentColor = if (isSpeaking) Color.White else DeepBlue
                        ),
                        modifier = Modifier.weight(1f).testTag("btn_listen_all_alerts")
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSpeaking) "⏹ Parar" else "🔊 Ouvir Viva Voz",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            com.example.util.NotificationSchedulerHelper.checkAndTriggerNotifications(context)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentGold),
                        modifier = Modifier.weight(1f).testTag("btn_test_notification")
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = AccentGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("🔔 Testar Notificação", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Overdue Alerts Section
            if (alertasOverdue.isNotEmpty()) {
                item {
                    Text(
                        text = "⚠ Clientes em Atraso (${alertasOverdue.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarningRed
                    )
                }

                items(alertasOverdue) { (cliente, credito) ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(WarningRed)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectCliente(cliente.id) }
                            .testTag("alerta_item_${cliente.id}")
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
                                    Text("⚠ Cliente em atraso", fontWeight = FontWeight.Bold, color = WarningRed, fontSize = 14.sp)
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = WarningRedLight,
                                    contentColor = WarningRed
                                ) {
                                    Text(
                                        text = "${credito?.diasEmAtraso ?: 0} dias de atraso",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Cliente: ${cliente.nome}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Banco: ${cliente.banco} • Tel: ${cliente.telefone}", fontSize = 12.sp, color = GrayText)
                            Text("Prestação: ${KwanzaFormatters.formatAOA(credito?.prestacaoMensal ?: 0.0)}", fontWeight = FontWeight.Bold, color = WarningRed, fontSize = 14.sp)

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { onSpeakCliente(cliente, credito) },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkGold),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ouvir", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        val message = "Olá ${cliente.nome}, lembramos que a sua prestação no valor de ${KwanzaFormatters.formatAOA(credito?.prestacaoMensal ?: 0.0)} do ${cliente.banco} encontra-se em atraso há ${credito?.diasEmAtraso} dias. Por favor entre em contato com Nduwa Kayongo."
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=${cliente.telefone.replace(" ", "")}&text=${Uri.encode(message)}"))
                                        context.startActivity(intent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("WhatsApp", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${cliente.telefone}"))
                                        context.startActivity(intent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ligar", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Upcoming Alerts Section
            if (alertasUpcoming.isNotEmpty()) {
                item {
                    Text(
                        text = "🔔 Prestações Próximas (${alertasUpcoming.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepBlue
                    )
                }

                items(alertasUpcoming) { (cliente, credito) ->
                    val daysUntil = KwanzaFormatters.calculateDaysUntilOrOverdue(credito?.proximoVencimento ?: 0L)

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectCliente(cliente.id) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🔔 Prestação próxima", fontWeight = FontWeight.Bold, color = DeepBlue, fontSize = 14.sp)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = DeepBlue.copy(alpha = 0.1f),
                                    contentColor = DeepBlue
                                ) {
                                    Text(
                                        text = if (daysUntil == 0) "VENCE HOJE" else "Faltam $daysUntil dias",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Cliente: ${cliente.nome}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Banco: ${cliente.banco}", fontSize = 12.sp, color = GrayText)
                            Text("Prestação: ${KwanzaFormatters.formatAOA(credito?.prestacaoMensal ?: 0.0)}", fontWeight = FontWeight.Bold, color = DeepBlue, fontSize = 14.sp)
                            Text("Vencimento: ${KwanzaFormatters.formatDate(credito?.proximoVencimento ?: 0L)}", fontSize = 12.sp, color = DarkGold, fontWeight = FontWeight.SemiBold)

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { onSpeakCliente(cliente, credito) },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkGold),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ouvir", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = { onSelectCliente(cliente.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Registrar", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        val message = "Olá ${cliente.nome}, lembramos que a sua prestação no valor de ${KwanzaFormatters.formatAOA(credito?.prestacaoMensal ?: 0.0)} vence no dia ${KwanzaFormatters.formatDate(credito?.proximoVencimento ?: 0L)}."
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=${cliente.telefone.replace(" ", "")}&text=${Uri.encode(message)}"))
                                        context.startActivity(intent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("WhatsApp", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
