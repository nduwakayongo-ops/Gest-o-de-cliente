package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.ui.theme.*
import com.example.util.LembreteHelper

@Composable
fun EnviarLembreteDialog(
    cliente: ClienteEntity,
    credito: CreditoEntity?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dataVencimentoStr = if (credito != null) {
        KwanzaFormatters.formatDate(credito.proximoVencimento)
    } else {
        "N/A"
    }

    val isEmAtrasoDefault = (credito?.diasEmAtraso ?: 0) > 0 || credito?.estado == "Em atraso"

    // 1: Próximo Vencimento, 2: Vencido
    var selectedTemplate by remember { mutableIntStateOf(if (isEmAtrasoDefault) 2 else 1) }

    val msg1 = remember(dataVencimentoStr) {
        LembreteHelper.formatMensagemProximoVencimento(dataVencimentoStr)
    }
    val msg2 = remember(dataVencimentoStr) {
        LembreteHelper.formatMensagemPagamentoVencido(dataVencimentoStr)
    }

    var editedMessage by remember(selectedTemplate) {
        mutableStateOf(if (selectedTemplate == 1) msg1 else msg2)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        tint = DeepBlue,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Enviar Lembrete ao Cliente",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepBlue
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar", tint = GrayText)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Client & Due Date Banner
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = GrayBackground),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "👤 ${KwanzaFormatters.formatPrimeiroEUltimoNome(cliente.nome)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = DeepBlue
                        )
                        Text(
                            text = "📞 Tel: ${cliente.telefone} | 🏦 Banco: ${cliente.banco}",
                            fontSize = 12.sp,
                            color = GrayText
                        )
                        if (credito != null) {
                            Text(
                                text = "📅 Data Reembolso: $dataVencimentoStr | Prestação: ${KwanzaFormatters.formatAOA(credito.prestacaoMensal)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DarkGold
                            )
                        }
                    }
                }

                Text(
                    text = "Selecione o modelo de mensagem:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepBlue
                )

                // Template 1 Option
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedTemplate == 1) DeepBlue.copy(alpha = 0.08f) else Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (selectedTemplate == 1) 2.dp else 1.dp,
                            color = if (selectedTemplate == 1) DeepBlue else Color.LightGray,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            selectedTemplate = 1
                            editedMessage = msg1
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedTemplate == 1,
                            onClick = {
                                selectedTemplate = 1
                                editedMessage = msg1
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = DeepBlue)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "🔔 Lembrete Próximo Vencimento (5 Dias Antes)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepBlue
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = msg1,
                                fontSize = 11.sp,
                                color = Color.Black
                            )
                        }
                    }
                }

                // Template 2 Option
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedTemplate == 2) WarningRed.copy(alpha = 0.08f) else Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (selectedTemplate == 2) 2.dp else 1.dp,
                            color = if (selectedTemplate == 2) WarningRed else Color.LightGray,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            selectedTemplate = 2
                            editedMessage = msg2
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedTemplate == 2,
                            onClick = {
                                selectedTemplate = 2
                                editedMessage = msg2
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = WarningRed)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "⚠️ Aviso de Pagamento Vencido",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarningRed
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = msg2,
                                fontSize = 11.sp,
                                color = Color.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Editable Preview
                Text(
                    text = "Texto final a enviar:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GrayText
                )

                OutlinedTextField(
                    value = editedMessage,
                    onValueChange = { editedMessage = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepBlue,
                        unfocusedBorderColor = Color.Gray
                    )
                )
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // WhatsApp Button
                    Button(
                        onClick = {
                            LembreteHelper.enviarWhatsApp(context, cliente.telefone, editedMessage)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_send_whatsapp")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // SMS Button
                    Button(
                        onClick = {
                            LembreteHelper.enviarSms(context, cliente.telefone, editedMessage)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_send_sms")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sms,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SMS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancelar", color = GrayText)
                }
            }
        },
        dismissButton = null
    )
}
