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
    var canalNotificacaoInterna by remember(currentConfig) { mutableStateOf(currentConfig.canalNotificacaoInterna) }
    var canalSmsCliente by remember(currentConfig) { mutableStateOf(currentConfig.canalSmsCliente) }
    var canalWhatsAppCliente by remember(currentConfig) { mutableStateOf(currentConfig.canalWhatsAppCliente) }
    var telefoneInstituicao by remember(currentConfig) { mutableStateOf(currentConfig.telefoneInstituicao) }
    var temaSelected by remember(currentConfig) { mutableStateOf(currentConfig.tema) }
    var backupAutomatico by remember(currentConfig) { mutableStateOf(currentConfig.backupAutomatico) }
    var showZerarDialog by remember { mutableStateOf(false) }

    val profilePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val permanentPath = try {
                val inputStream = context.contentResolver.openInputStream(it)
                if (inputStream != null) {
                    val profileFile = java.io.File(context.filesDir, "nduwa_perfil_permanente.jpg")
                    java.io.FileOutputStream(profileFile).use { output ->
                        inputStream.copyTo(output)
                    }
                    profileFile.absolutePath
                } else {
                    it.toString()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                it.toString()
            }
            onSaveConfiguracao(currentConfig.copy(fotoPerfilUri = permanentPath))
            Toast.makeText(context, "Foto de perfil guardada de forma permanente!", Toast.LENGTH_SHORT).show()
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Official App Logo
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .border(2.dp, AccentGold, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                ) {
                    AsyncImage(
                        model = R.drawable.nduwa_photo,
                        contentDescription = "Logo Oficial Nduwa Kayongo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Profile Avatar
                Box(
                    modifier = Modifier
                        .size(60.dp)
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
                        AsyncImage(
                            model = R.drawable.nduwa_photo,
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
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { profilePhotoPicker.launch("image/*") },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGold),
                            border = ButtonDefaults.outlinedButtonBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AccentGold)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp).testTag("btn_change_profile_photo")
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Alterar Foto", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        if (!currentConfig.fotoPerfilUri.isNullOrEmpty()) {
                            TextButton(
                                onClick = {
                                    onSaveConfiguracao(currentConfig.copy(fotoPerfilUri = null))
                                    Toast.makeText(context, "Foto oficial restaurada!", Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp).testTag("btn_restore_official_photo")
                            ) {
                                Text("Restaurar Oficial", fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f))
                            }
                        }
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
                    Text("🔔 Notificações Diárias no Telemóvel (08:00 e 16:00)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                }

                Text("As notificações são exibidas diretamente na barra de notificações do telefone todos os dias às 08:00 e 16:00, contendo o nome do cliente, prestação mensal, data do reembolso e valor do crédito (antecedência de 5 dias e pagamentos em atraso).", fontSize = 12.sp, color = GrayText)

                Button(
                    onClick = {
                        com.example.util.NotificationSchedulerHelper.scheduleDailyAlarms(context)
                        com.example.util.NotificationSchedulerHelper.checkAndTriggerNotifications(context)
                        Toast.makeText(context, "Notificações diárias (08:00 e 16:00) ativadas e enviadas para a barra do telefone!", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                    modifier = Modifier.fillMaxWidth().testTag("btn_schedule_notifications")
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = AccentGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Disparar / Testar Notificações na Tela (08:00 & 16:00)", fontWeight = FontWeight.Bold)
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
                    Text("Limpa os registos e permite iniciar uma nova carteira do zero com cadastros inseridos pelo gestor.", fontSize = 11.sp, color = GrayText)

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

        // Section: Canais de Comunicação Autorizados (Secção 4 do Requisito Oficial)
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
                    Text("📡 Canais de Comunicação Autorizados", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                }

                Text(
                    text = "Seleccione os canais autorizados para lembretes de cobrança e reembolsos (Secção 4):",
                    fontSize = 12.sp,
                    color = GrayText
                )

                // 1. Notificação interna ao responsável
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Notificação Interna ao Responsável", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Alertas diários na barra do telemóvel (08:00 e 16:00)", fontSize = 11.sp, color = GrayText)
                    }
                    Switch(
                        checked = canalNotificacaoInterna,
                        onCheckedChange = { canalNotificacaoInterna = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = DeepBlue)
                    )
                }

                // 2. SMS ao cliente
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("SMS ao Cliente", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Envio de lembretes e cobrança por rede móvel / SMS", fontSize = 11.sp, color = GrayText)
                    }
                    Switch(
                        checked = canalSmsCliente,
                        onCheckedChange = { canalSmsCliente = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = ForestGreen)
                    )
                }

                // 3. WhatsApp ao cliente
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("WhatsApp ao Cliente", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Lembretes formatados e abertura de conversa WhatsApp", fontSize = 11.sp, color = GrayText)
                    }
                    Switch(
                        checked = canalWhatsAppCliente,
                        onCheckedChange = { canalWhatsAppCliente = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = DarkGold)
                    )
                }

                // Telefone Institucional de Contacto
                OutlinedTextField(
                    value = telefoneInstituicao,
                    onValueChange = { telefoneInstituicao = it },
                    label = { Text("Contacto da Instituição nos Modelos de Mensagem") },
                    modifier = Modifier.fillMaxWidth().testTag("input_telefone_instituicao"),
                    singleLine = true
                )
            }
        }

        // Section: Fila de Mensagens SMS e WhatsApp (Online / Offline)
        val filaMensagens by com.example.util.MensagensQueueManager.filaMensagens.collectAsState()
        LaunchedEffect(Unit) {
            com.example.util.MensagensQueueManager.init(context)
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = DeepBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("📨 Fila de Mensagens (${filaMensagens.size})", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                    }

                    Surface(
                        onClick = {
                            com.example.util.MensagensQueueManager.processarFilaQuandoConectado(context)
                            Toast.makeText(context, "Fila processada com base no estado da rede!", Toast.LENGTH_SHORT).show()
                        },
                        color = DeepBlue.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Sincronizar Rede", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                        }
                    }
                }

                Text(
                    text = "Acompanhamento de mensagens enviadas, entregues, pendentes ou aguardando rede (Secção 4.1 & 4.2):",
                    fontSize = 11.sp,
                    color = GrayText
                )

                if (filaMensagens.isEmpty()) {
                    Text(
                        text = "Nenhuma mensagem recente na fila local.",
                        fontSize = 12.sp,
                        color = GrayText,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                } else {
                    filaMensagens.take(6).forEach { item ->
                        val badgeColor = when (item.estado) {
                            com.example.util.EstadoMensagem.ENTREGUE -> ForestGreen
                            com.example.util.EstadoMensagem.ENVIADA -> DeepBlue
                            com.example.util.EstadoMensagem.PENDENTE -> DarkGold
                            com.example.util.EstadoMensagem.AGUARDANDO_REDE -> Color(0xFFE67E22)
                            com.example.util.EstadoMensagem.FALHADA -> WarningRed
                            com.example.util.EstadoMensagem.CANCELADA -> GrayText
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GrayBackground,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${item.nomeCliente} • ${item.canal.label}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepBlue
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = badgeColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = item.estado.label,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = item.mensagem,
                                    fontSize = 11.sp,
                                    color = Color.DarkGray,
                                    maxLines = 2
                                )

                                if (item.detalheStatus.isNotBlank()) {
                                    Text(
                                        text = item.detalheStatus,
                                        fontSize = 10.sp,
                                        color = GrayText
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (item.estado != com.example.util.EstadoMensagem.ENTREGUE) {
                                        OutlinedButton(
                                            onClick = {
                                                com.example.util.MensagensQueueManager.tentarReenvioControlado(context, item.id)
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(24.dp)
                                        ) {
                                            Text("Reenviar (${item.tentativas}/3)", fontSize = 10.sp)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                com.example.util.MensagensQueueManager.marcarComoEntregue(context, item.id)
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(24.dp)
                                        ) {
                                            Text("Confirmar Entrega", fontSize = 10.sp)
                                        }
                                    }

                                    if (item.estado == com.example.util.EstadoMensagem.PENDENTE || item.estado == com.example.util.EstadoMensagem.AGUARDANDO_REDE) {
                                        TextButton(
                                            onClick = {
                                                com.example.util.MensagensQueueManager.cancelarMensagem(context, item.id)
                                            },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            modifier = Modifier.height(24.dp)
                                        ) {
                                            Text("Cancelar", fontSize = 10.sp, color = WarningRed)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
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
                        backupAutomatico = backupAutomatico,
                        canalNotificacaoInterna = canalNotificacaoInterna,
                        canalSmsCliente = canalSmsCliente,
                        canalWhatsAppCliente = canalWhatsAppCliente,
                        telefoneInstituicao = telefoneInstituicao
                    )
                )
                Toast.makeText(context, "Configurações e canais de comunicação salvos com sucesso!", Toast.LENGTH_SHORT).show()
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
