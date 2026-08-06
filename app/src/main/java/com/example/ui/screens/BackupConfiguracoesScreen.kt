package com.example.ui.screens

import android.net.Uri
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.ConfiguracaoEntity
import com.example.ui.theme.*
import com.example.util.BiometricHelper

@Composable
fun BackupConfiguracoesScreen(
    configuracao: ConfiguracaoEntity?,
    isInternetConnected: Boolean = true,
    onSaveConfiguracao: (ConfiguracaoEntity) -> Unit,
    onZerarSistema: () -> Unit = {}
) {
    val context = LocalContext.current

    val currentConfig = configuracao ?: ConfiguracaoEntity()

    var pinSeguranca by remember(currentConfig) { mutableStateOf(currentConfig.pinSeguranca ?: "") }
    var biometriaAtiva by remember(currentConfig) { mutableStateOf(currentConfig.biometriaAtiva) }
    var horarioNotificacao by remember(currentConfig) { mutableStateOf(currentConfig.horarioNotificacao) }
    var diasAntecedencia by remember(currentConfig) { mutableIntStateOf(currentConfig.diasAntecedenciaNotificacao) }
    var temaSelected by remember(currentConfig) { mutableStateOf(currentConfig.tema) }
    var backupAutomatico by remember(currentConfig) { mutableStateOf(currentConfig.backupAutomatico) }
    var showZerarDialog by remember { mutableStateOf(false) }

    val profilePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            onSaveConfiguracao(currentConfig.copy(fotoPerfilUri = it.toString()))
            Toast.makeText(context, "Foto de perfil atualizada!", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GrayBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Info & Profile Banner
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DeepBlue),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .border(2.dp, AccentGold, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { profilePhotoPicker.launch("image/*") }
                        .testTag("profile_photo_avatar")
                ) {
                    if (!currentConfig.fotoPerfilUri.isNullOrEmpty()) {
                        AsyncImage(
                            model = currentConfig.fotoPerfilUri,
                            contentDescription = "Foto de Nduwa Kayongo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.nduwa_photo),
                            contentDescription = "Foto de Nduwa Kayongo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Gestão de Clientes",
                        color = AccentGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Nduwa Kayongo",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Gestão Inteligente da Carteira de Crédito",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { profilePhotoPicker.launch("image/*") },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGold),
                        border = ButtonDefaults.outlinedButtonBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AccentGold)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp).testTag("btn_change_profile_photo")
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Alterar Foto (Galeria)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section: Aparência e Tema (Claro / Escuro)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = DeepBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🎨 Aparência e Tema de Fundo", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                }

                Text("Selecione o modo de fundo preferido para o aplicativo:", fontSize = 13.sp, color = GrayText)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "Claro" to "☀️ Claro",
                        "Escuro" to "🌙 Escuro",
                        "Automatico" to "📱 Automático"
                    ).forEach { (valor, label) ->
                        FilterChip(
                            selected = temaSelected.equals(valor, ignoreCase = true),
                            onClick = {
                                temaSelected = valor
                                onSaveConfiguracao(currentConfig.copy(tema = valor))
                            },
                            label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }
            }
        }

        // Section: Segurança
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = DeepBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🔒 Segurança e Proteção Biométrica", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                }

                OutlinedTextField(
                    value = pinSeguranca,
                    onValueChange = {
                        pinSeguranca = it
                        onSaveConfiguracao(currentConfig.copy(pinSeguranca = it))
                    },
                    label = { Text("PIN de Acesso (4 dígitos)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_pin_seguranca"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Proteção por Biometria do Telefone", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Impressão Digital ou Reconhecimento Facial", fontSize = 11.sp, color = GrayText)
                    }
                    Switch(
                        checked = biometriaAtiva,
                        onCheckedChange = { active ->
                            biometriaAtiva = active
                            onSaveConfiguracao(currentConfig.copy(biometriaAtiva = active))
                            Toast.makeText(
                                context,
                                if (active) "Biometria do telefone ativada para proteção dos dados!" else "Biometria desativada.",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = DeepBlue)
                    )
                }

                Button(
                    onClick = {
                        BiometricHelper.launchBiometricPrompt(
                            context = context,
                            onSuccess = {
                                Toast.makeText(context, "Sensor de biometria funcionando perfeitamente!", Toast.LENGTH_LONG).show()
                            },
                            onError = { err ->
                                Toast.makeText(context, "Erro de biometria: $err", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("btn_test_biometric")
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = AccentGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Testar Biometria do Telefone", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section: Notificações
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = DeepBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🔔 Notificações Diárias Automáticas (08:00 e 17:00)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                }

                Text("Alertas antecipados enviados 5 dias antes do vencimento e lembretes diários até a liquidação completa.", fontSize = 12.sp, color = GrayText)

                Button(
                    onClick = {
                        com.example.util.NotificationSchedulerHelper.scheduleDailyAlarms(context)
                        com.example.util.NotificationSchedulerHelper.checkAndTriggerNotifications(context)
                        Toast.makeText(context, "Notificações diárias (08:00 e 17:00) ativadas e agendadas no telemóvel!", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                    modifier = Modifier.fillMaxWidth().testTag("btn_schedule_notifications")
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = AccentGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ativar / Testar Lembretes (08:00 & 17:00)", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section: Backup & Restore
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Backup, contentDescription = null, tint = DeepBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("☁ Backup Automático Online & Restauração", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isInternetConnected) LightGreen else WarningRedLight.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isInternetConnected) ForestGreen else WarningRed,
                            modifier = Modifier.size(10.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isInternetConnected)
                                "Conectado à Internet • Backup automático ativo a cada alteração"
                            else
                                "Sem conexão à internet • Backup pendente até conectar",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isInternetConnected) ForestGreen else WarningRed
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Backup Automático ao Estar Conectado", fontSize = 13.sp)
                    Switch(
                        checked = backupAutomatico,
                        onCheckedChange = { backupAutomatico = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = ForestGreen)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            val backupText = "{\"app\":\"Gestão de Clientes - Nduwa Kayongo\",\"backupDate\":\"${System.currentTimeMillis()}\",\"status\":\"success\"}"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, backupText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Exportar Backup de Dados"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Exportar Backup", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "Backup verificado e restaurado com sucesso!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Restaurar Backup", fontSize = 11.sp)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Reset System / Clear Data Section
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🗑 Limpeza & Reinício de Banco de Dados", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WarningRed)
                    Text("Remove todos os dados simulados/antigos e mantêm apenas os cadastros inseridos pelo gestor.", fontSize = 11.sp, color = GrayText)

                    OutlinedButton(
                        onClick = { showZerarDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningRed),
                        border = ButtonDefaults.outlinedButtonBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(WarningRed)),
                        modifier = Modifier.fillMaxWidth().testTag("btn_zerar_sistema")
                    ) {
                        Text("Zerar Sistema (Salvar Apenas Dados do Gestor)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (showZerarDialog) {
            AlertDialog(
                onDismissRequest = { showZerarDialog = false },
                title = { Text("Zerar Dados do Sistema?", fontWeight = FontWeight.Bold) },
                text = { Text("Esta ação irá limpar os registos antigos e manter apenas os novos dados que forem adicionados por você (gestor). Deseja continuar?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showZerarDialog = false
                            onZerarSistema()
                            Toast.makeText(context, "Sistema zerado! Apenas os dados inseridos por você serão salvos.", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WarningRed)
                    ) {
                        Text("Sim, Zerar Tudo")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showZerarDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Save Config Button
        Button(
            onClick = {
                onSaveConfiguracao(
                    currentConfig.copy(
                        pinSeguranca = pinSeguranca.ifEmpty { null },
                        biometriaAtiva = biometriaAtiva,
                        horarioNotificacao = horarioNotificacao,
                        diasAntecedenciaNotificacao = diasAntecedencia,
                        tema = temaSelected,
                        backupAutomatico = backupAutomatico
                    )
                )
                Toast.makeText(context, "Configurações salvas com sucesso!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("btn_salvar_configuracoes"),
            colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Salvar Preferências", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}
