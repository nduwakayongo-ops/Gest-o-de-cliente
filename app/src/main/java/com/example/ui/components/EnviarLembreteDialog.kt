package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
        "Data acordada"
    }

    val nomeFormatado = KwanzaFormatters.formatPrimeiroEUltimoNome(cliente.nome)
    val valorPrestacao = credito?.prestacaoMensal ?: 0.0
    val valorPrestacaoStr = KwanzaFormatters.formatAOA(valorPrestacao)

    // Calculate default overdue days
    val initialDiasAtraso = remember(credito) {
        val dbDias = credito?.diasEmAtraso ?: 0
        if (dbDias > 0) {
            dbDias
        } else if (credito != null && credito.proximoVencimento > 0 && credito.proximoVencimento < System.currentTimeMillis()) {
            val diff = System.currentTimeMillis() - credito.proximoVencimento
            (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
        } else {
            1
        }
    }

    var customDiasAtraso by remember { mutableIntStateOf(initialDiasAtraso) }

    val isEmAtrasoDefault = (credito?.diasEmAtraso ?: 0) > 0 || credito?.estado == "Em atraso" || (credito != null && credito.proximoVencimento < System.currentTimeMillis() && credito.estado != "Liquidado")

    // 1: Lembrete Vencimento, 2: Cobrança Atraso / Comprovativo, 3: Juros de Mora (4%/dia)
    var selectedTemplate by remember { mutableIntStateOf(if (isEmAtrasoDefault) 2 else 1) }

    // Generate messages reactively
    val msg1 = remember(nomeFormatado, dataVencimentoStr, valorPrestacaoStr) {
        LembreteHelper.formatMensagemLembreteVencimento(
            nomeCliente = nomeFormatado,
            dataReembolso = dataVencimentoStr,
            valorAPagar = valorPrestacaoStr
        )
    }

    val msg2 = remember(nomeFormatado, dataVencimentoStr) {
        LembreteHelper.formatMensagemCobrancaAtraso(
            nomeCliente = nomeFormatado,
            dataEmAtraso = dataVencimentoStr
        )
    }

    val msg3 = remember(nomeFormatado, customDiasAtraso, credito) {
        if (credito != null) {
            LembreteHelper.formatMensagemJurosMoraOficial(
                nomeCliente = nomeFormatado,
                credito = credito,
                diasAtrasoManual = customDiasAtraso
            )
        } else {
            LembreteHelper.formatMensagemJurosMora(
                nomeCliente = nomeFormatado,
                diasEmAtraso = customDiasAtraso,
                prestacaoMensal = valorPrestacao
            )
        }
    }

    var editedMessage by remember(selectedTemplate, msg1, msg2, msg3) {
        mutableStateOf(
            when (selectedTemplate) {
                1 -> msg1
                2 -> msg2
                3 -> msg3
                else -> msg1
            }
        )
    }

    // Calculated amounts for Template 3 info banner using official formula: Total Juros * 4% * dias
    val moraCalcResultado = remember(credito, customDiasAtraso) {
        if (credito != null) {
            com.example.util.MoraCalculatorHelper.calcular(
                credito = credito,
                pagamentos = emptyList(),
                diasAtrasoManual = customDiasAtraso
            )
        } else null
    }

    val jurosMoraCalc = moraCalcResultado?.jurosMoraAcumulados ?: (valorPrestacao * 0.04 * customDiasAtraso)
    val totalComMoraCalc = moraCalcResultado?.valorTotalEmDivida ?: (valorPrestacao + jurosMoraCalc)

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
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Enviar Lembrete / Cobrança",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepBlue
                        )
                        Text(
                            text = "Mbuli-Microcrédito • Notificações",
                            fontSize = 11.sp,
                            color = DarkGold,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar", tint = GrayText)
                }
            }
        },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Client & Loan Info Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = GrayBackground),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "👤 $nomeFormatado",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = DeepBlue
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isEmAtrasoDefault) WarningRed.copy(alpha = 0.15f) else ForestGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (isEmAtrasoDefault) "Em atraso" else "Em dia",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isEmAtrasoDefault) WarningRed else ForestGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "📞 ${cliente.telefone} • 🏦 ${cliente.banco}",
                            fontSize = 11.sp,
                            color = GrayText
                        )
                        if (credito != null) {
                            Text(
                                text = "📅 Vencimento: $dataVencimentoStr | Prestação: $valorPrestacaoStr",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DeepBlue
                            )
                        }
                    }
                }

                Text(
                    text = "Escolha o Modelo de Mensagem:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepBlue
                )

                // MODELO 1: Lembrete de Vencimento
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedTemplate == 1) DeepBlue.copy(alpha = 0.08f) else Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (selectedTemplate == 1) 2.dp else 1.dp,
                            color = if (selectedTemplate == 1) DeepBlue else Color.LightGray.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            selectedTemplate = 1
                            editedMessage = msg1
                        }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedTemplate == 1,
                                onClick = {
                                    selectedTemplate = 1
                                    editedMessage = msg1
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = DeepBlue)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "1. Lembrete de Vencimento (Antes do prazo)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepBlue
                            )
                        }
                        Text(
                            text = msg1,
                            fontSize = 11.sp,
                            color = Color.DarkGray,
                            modifier = Modifier.padding(start = 36.dp, top = 2.dp)
                        )
                    }
                }

                // MODELO 2: Cobrança em Atraso (Comprovativo)
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedTemplate == 2) Color(0xFFFFF3F3) else Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (selectedTemplate == 2) 2.dp else 1.dp,
                            color = if (selectedTemplate == 2) WarningRed else Color.LightGray.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            selectedTemplate = 2
                            editedMessage = msg2
                        }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedTemplate == 2,
                                onClick = {
                                    selectedTemplate = 2
                                    editedMessage = msg2
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = WarningRed)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "2. Cobrança de Atraso (Comprovativo)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarningRed
                            )
                        }
                        Text(
                            text = msg2,
                            fontSize = 11.sp,
                            color = Color.DarkGray,
                            modifier = Modifier.padding(start = 36.dp, top = 2.dp)
                        )
                    }
                }

                // MODELO 3: Juros de Mora (4%/dia com cálculo automático)
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedTemplate == 3) DarkGold.copy(alpha = 0.10f) else Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (selectedTemplate == 3) 2.dp else 1.dp,
                            color = if (selectedTemplate == 3) DarkGold else Color.LightGray.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            selectedTemplate = 3
                            editedMessage = msg3
                        }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = selectedTemplate == 3,
                                    onClick = {
                                        selectedTemplate = 3
                                        editedMessage = msg3
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = DarkGold)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "3. Juros de Mora (4%/dia - Cálculo Auto)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkGold
                                )
                            }
                        }

                        // Controller for Days in Delay (Dias em Atraso)
                        if (selectedTemplate == 3) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 32.dp, top = 6.dp, bottom = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Dias em Atraso:", fontSize = 11.sp, color = GrayText)
                                        Text("$customDiasAtraso ${if (customDiasAtraso == 1) "dia" else "dias"} (4%/dia)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                if (customDiasAtraso > 1) {
                                                    customDiasAtraso--
                                                }
                                            },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.Default.RemoveCircle, contentDescription = "Diminuir", tint = DeepBlue, modifier = Modifier.size(20.dp))
                                        }

                                        Text(
                                            text = "$customDiasAtraso",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )

                                        IconButton(
                                            onClick = { customDiasAtraso++ },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.Default.AddCircle, contentDescription = "Aumentar", tint = DeepBlue, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }

                            // Calculation badge conforming strictly to Section 6 formula
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = DarkGold.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 32.dp, bottom = 6.dp)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                    if (moraCalcResultado != null) {
                                        Text("• Total Juros Contratuais: ${KwanzaFormatters.formatAOA(moraCalcResultado.totalJurosContratuais)}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = DeepBlue)
                                        Text("• Mora Diária (4% juros contratuais): ${KwanzaFormatters.formatAOA(moraCalcResultado.jurosMoraDiarios)}/dia", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = DeepBlue)
                                        Text("• Mora Acumulada ($customDiasAtraso d): ${KwanzaFormatters.formatAOA(moraCalcResultado.jurosMoraAcumulados)}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = DeepBlue)
                                        Text("• Valor em Atraso: ${KwanzaFormatters.formatAOA(moraCalcResultado.valorEmAtraso)}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = DeepBlue)
                                        Text("• Total Exigível (Atraso + Mora): ${KwanzaFormatters.formatAOA(moraCalcResultado.totalExigivel)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
                                    } else {
                                        Text("• Juros de Mora ($customDiasAtraso d): ${KwanzaFormatters.formatAOA(jurosMoraCalc)}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = DeepBlue)
                                        Text("• Total a Pagar c/ Mora: ${KwanzaFormatters.formatAOA(totalComMoraCalc)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
                                    }
                                }
                            }
                        }

                        Text(
                            text = msg3,
                            fontSize = 11.sp,
                            color = Color.DarkGray,
                            modifier = Modifier.padding(start = 36.dp, top = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Editable Final Text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mensagem que será enviada:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepBlue
                    )
                    Text(
                        text = "${editedMessage.length} caracteres",
                        fontSize = 10.sp,
                        color = GrayText
                    )
                }

                OutlinedTextField(
                    value = editedMessage,
                    onValueChange = { editedMessage = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepBlue,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                    )
                )

                // Quick Copy Button
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Lembrete Mbuli-Microcredito", editedMessage)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Mensagem copiada com sucesso!", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = DeepBlue)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copiar Mensagem para Área de Transferência", fontSize = 11.sp, color = DeepBlue, fontWeight = FontWeight.Bold)
                }
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
                            com.example.util.MensagensQueueManager.enviarOuEnfileirarWhatsApp(
                                context = context,
                                cliente = cliente,
                                credito = credito,
                                mensagem = editedMessage
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("btn_send_whatsapp")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // SMS Button
                    Button(
                        onClick = {
                            com.example.util.MensagensQueueManager.enviarOuEnfileirarSms(
                                context = context,
                                cliente = cliente,
                                credito = credito,
                                mensagem = editedMessage
                            )
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
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SMS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Fechar", color = GrayText)
                }
            }
        },
        dismissButton = null
    )
}
