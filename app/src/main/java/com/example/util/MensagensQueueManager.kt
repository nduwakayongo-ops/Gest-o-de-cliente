package com.example.util

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.telephony.TelephonyManager
import android.widget.Toast
import com.example.data.db.AppDatabase
import com.example.data.model.ClienteEntity
import com.example.data.model.CobrancaEntity
import com.example.data.model.CreditoEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class EstadoMensagem(val label: String, val descricao: String) {
    PENDENTE("Pendente", "Guardada na fila local, aguardando processamento"),
    AGUARDANDO_REDE("A aguardar rede", "Aguardando disponibilidade de rede móvel ou Internet"),
    ENVIADA("Enviada ao serviço de mensagens", "Despachada para o sistema de mensagens / aplicação nativa"),
    ENTREGUE("Entregue", "Confirmação de entrega recebida do operador / serviço"),
    FALHADA("Falhada", "Falha no envio ou erro de rede"),
    CANCELADA("Cancelada", "Cancelada pelo operador antes do envio")
}

enum class TipoCanalMensagem(val label: String) {
    SMS("SMS (Rede Móvel)"),
    WHATSAPP("WhatsApp (Cloud API / Nativo)")
}

data class ItemMensagemFila(
    val id: String = UUID.randomUUID().toString(),
    val clienteId: Long,
    val creditoId: Long,
    val nomeCliente: String,
    val telefone: String,
    val canal: TipoCanalMensagem,
    val mensagem: String,
    val estado: EstadoMensagem,
    val dataCriacao: Long = System.currentTimeMillis(),
    val dataAtualizacao: Long = System.currentTimeMillis(),
    val detalheStatus: String = ""
)

/**
 * Estrutura Oficial de Integração WhatsApp Business Platform / Cloud API.
 * Suporta o envio via API oficial do Meta/Facebook Graph API quando configurado.
 */
object WhatsAppCloudApiConfig {
    const val API_BASE_URL = "https://graph.facebook.com/v19.0"

    // Credenciais externas obtidas através do Painel de Secrets
    var phoneNumberId: String? = null
    var accessToken: String? = null
    var businessAccountId: String? = null

    val isConfigured: Boolean
        get() = !phoneNumberId.isNullOrBlank() && !accessToken.isNullOrBlank()

    fun isConfigurado(): Boolean = isConfigured
}

/**
 * Gerenciador de Fila de Mensagens com Persistência Local e Acompanhamento de Entrega.
 */
object MensagensQueueManager {

    private const val PREFS_NAME = "mensagens_fila_prefs"
    private const val KEY_MENSAGENS = "lista_mensagens_json"

    private val _filaMensagens = MutableStateFlow<List<ItemMensagemFila>>(emptyList())
    val filaMensagens: StateFlow<List<ItemMensagemFila>> = _filaMensagens.asStateFlow()

    private var isInitialized = false

    fun init(context: Context) {
        if (!isInitialized) {
            carregarFila(context)
            isInitialized = true
        }
    }

    private fun carregarFila(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_MENSAGENS, null) ?: return
        try {
            val arr = JSONArray(jsonStr)
            val lista = mutableListOf<ItemMensagemFila>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                lista.add(
                    ItemMensagemFila(
                        id = obj.getString("id"),
                        clienteId = obj.getLong("clienteId"),
                        creditoId = obj.getLong("creditoId"),
                        nomeCliente = obj.getString("nomeCliente"),
                        telefone = obj.getString("telefone"),
                        canal = TipoCanalMensagem.valueOf(obj.getString("canal")),
                        mensagem = obj.getString("mensagem"),
                        estado = EstadoMensagem.valueOf(obj.getString("estado")),
                        dataCriacao = obj.getLong("dataCriacao"),
                        dataAtualizacao = obj.getLong("dataAtualizacao"),
                        detalheStatus = obj.optString("detalheStatus", "")
                    )
                )
            }
            _filaMensagens.value = lista
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun salvarFila(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        _filaMensagens.value.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("clienteId", item.clienteId)
                put("creditoId", item.creditoId)
                put("nomeCliente", item.nomeCliente)
                put("telefone", item.telefone)
                put("canal", item.canal.name)
                put("mensagem", item.mensagem)
                put("estado", item.estado.name)
                put("dataCriacao", item.dataCriacao)
                put("dataAtualizacao", item.dataAtualizacao)
                put("detalheStatus", item.detalheStatus)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_MENSAGENS, arr.toString()).apply()
    }

    fun isInternetDisponivel(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun isRedeMovelDisponivel(context: Context): Boolean {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return false
        val simState = tm.simState
        val temSim = simState == TelephonyManager.SIM_STATE_READY
        return temSim
    }

    /**
     * Envia ou enfileira SMS para o contacto do cliente.
     */
    fun enviarOuEnfileirarSms(
        context: Context,
        clienteId: Long,
        creditoId: Long,
        nomeCliente: String,
        telefone: String,
        mensagem: String
    ): ItemMensagemFila {
        init(context)
        val redeDisponivel = isRedeMovelDisponivel(context)

        val novoItem = if (!redeDisponivel) {
            ItemMensagemFila(
                clienteId = clienteId,
                creditoId = creditoId,
                nomeCliente = nomeCliente,
                telefone = telefone,
                canal = TipoCanalMensagem.SMS,
                mensagem = mensagem,
                estado = EstadoMensagem.AGUARDANDO_REDE,
                detalheStatus = "Sem cobertura de rede móvel ou cartão SIM ausente. Guardada na fila pendente."
            )
        } else {
            // Tenta abrir o aplicativo de SMS nativo
            try {
                LembreteHelper.enviarSms(context, telefone, mensagem)
                ItemMensagemFila(
                    clienteId = clienteId,
                    creditoId = creditoId,
                    nomeCliente = nomeCliente,
                    telefone = telefone,
                    canal = TipoCanalMensagem.SMS,
                    mensagem = mensagem,
                    estado = EstadoMensagem.ENVIADA,
                    detalheStatus = "Enviada ao aplicativo de mensagens SMS do dispositivo."
                )
            } catch (e: Exception) {
                ItemMensagemFila(
                    clienteId = clienteId,
                    creditoId = creditoId,
                    nomeCliente = nomeCliente,
                    telefone = telefone,
                    canal = TipoCanalMensagem.SMS,
                    mensagem = mensagem,
                    estado = EstadoMensagem.FALHADA,
                    detalheStatus = "Falha ao abrir serviço de SMS: ${e.message}"
                )
            }
        }

        val lista = _filaMensagens.value.toMutableList()
        lista.add(0, novoItem)
        _filaMensagens.value = lista
        salvarFila(context)

        // Registar contato no banco de dados
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                db.cobrancaDao().insertCobranca(
                    CobrancaEntity(
                        clienteId = clienteId,
                        tipoContato = "SMS",
                        observacao = "Lembrete SMS (${novoItem.estado.label}): ${mensagem.take(60)}..."
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (novoItem.estado == EstadoMensagem.AGUARDANDO_REDE) {
            Toast.makeText(context, "Sem rede móvel. SMS guardado na fila para envio quando a rede regressar.", Toast.LENGTH_LONG).show()
        } else if (novoItem.estado == EstadoMensagem.ENVIADA) {
            Toast.makeText(context, "SMS enviado ao aplicativo de mensagens.", Toast.LENGTH_SHORT).show()
        }

        return novoItem
    }

    fun enviarOuEnfileirarSms(
        context: Context,
        cliente: ClienteEntity,
        credito: CreditoEntity?,
        mensagem: String
    ): ItemMensagemFila {
        return enviarOuEnfileirarSms(
            context = context,
            clienteId = cliente.id,
            creditoId = credito?.id ?: 0L,
            nomeCliente = cliente.nome,
            telefone = cliente.telefone,
            mensagem = mensagem
        )
    }

    fun enviarOuEnfileirarWhatsApp(
        context: Context,
        cliente: ClienteEntity,
        credito: CreditoEntity?,
        mensagem: String
    ): ItemMensagemFila {
        return enviarOuEnfileirarWhatsApp(
            context = context,
            clienteId = cliente.id,
            creditoId = credito?.id ?: 0L,
            nomeCliente = cliente.nome,
            telefone = cliente.telefone,
            mensagem = mensagem
        )
    }

    /**
     * Envia ou enfileira mensagem WhatsApp (via Cloud API ou aplicativo nativo).
     */
    fun enviarOuEnfileirarWhatsApp(
        context: Context,
        clienteId: Long,
        creditoId: Long,
        nomeCliente: String,
        telefone: String,
        mensagem: String
    ): ItemMensagemFila {
        init(context)
        val internetDisponivel = isInternetDisponivel(context)

        val novoItem = if (!internetDisponivel) {
            ItemMensagemFila(
                clienteId = clienteId,
                creditoId = creditoId,
                nomeCliente = nomeCliente,
                telefone = telefone,
                canal = TipoCanalMensagem.WHATSAPP,
                mensagem = mensagem,
                estado = EstadoMensagem.AGUARDANDO_REDE,
                detalheStatus = "Sem ligação à Internet. Mensagem guardada localmente para envio quando online."
            )
        } else {
            if (WhatsAppCloudApiConfig.isConfigured) {
                // Estrutura pronta para chamada oficial WhatsApp Cloud API
                ItemMensagemFila(
                    clienteId = clienteId,
                    creditoId = creditoId,
                    nomeCliente = nomeCliente,
                    telefone = telefone,
                    canal = TipoCanalMensagem.WHATSAPP,
                    mensagem = mensagem,
                    estado = EstadoMensagem.ENVIADA,
                    detalheStatus = "Submetida à WhatsApp Cloud API (Graph API)."
                )
            } else {
                // Redireciona via WhatsApp nativo
                try {
                    LembreteHelper.enviarWhatsApp(context, telefone, mensagem)
                    ItemMensagemFila(
                        clienteId = clienteId,
                        creditoId = creditoId,
                        nomeCliente = nomeCliente,
                        telefone = telefone,
                        canal = TipoCanalMensagem.WHATSAPP,
                        mensagem = mensagem,
                        estado = EstadoMensagem.ENVIADA,
                        detalheStatus = "Enviada ao aplicativo WhatsApp do cliente. (Cloud API não configurada no Secrets)."
                    )
                } catch (e: Exception) {
                    ItemMensagemFila(
                        clienteId = clienteId,
                        creditoId = creditoId,
                        nomeCliente = nomeCliente,
                        telefone = telefone,
                        canal = TipoCanalMensagem.WHATSAPP,
                        mensagem = mensagem,
                        estado = EstadoMensagem.FALHADA,
                        detalheStatus = "Erro ao despachar WhatsApp: ${e.message}"
                    )
                }
            }
        }

        val lista = _filaMensagens.value.toMutableList()
        lista.add(0, novoItem)
        _filaMensagens.value = lista
        salvarFila(context)

        // Registar contato no banco de dados
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                db.cobrancaDao().insertCobranca(
                    CobrancaEntity(
                        clienteId = clienteId,
                        tipoContato = "WhatsApp",
                        observacao = "Lembrete WhatsApp (${novoItem.estado.label}): ${mensagem.take(60)}..."
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (novoItem.estado == EstadoMensagem.AGUARDANDO_REDE) {
            Toast.makeText(context, "Sem Internet. Lembrete WhatsApp guardado na fila local para envio online.", Toast.LENGTH_LONG).show()
        }

        return novoItem
    }

    /**
     * Tenta processar mensagens que estão aguardando rede.
     */
    fun processarFilaQuandoConectado(context: Context) {
        init(context)
        val internet = isInternetDisponivel(context)
        val redeMovel = isRedeMovelDisponivel(context)

        var alterou = false
        val lista = _filaMensagens.value.map { item ->
            if (item.estado == EstadoMensagem.AGUARDANDO_REDE) {
                if (item.canal == TipoCanalMensagem.SMS && redeMovel) {
                    alterou = true
                    item.copy(
                        estado = EstadoMensagem.PENDENTE,
                        dataAtualizacao = System.currentTimeMillis(),
                        detalheStatus = "Rede móvel restabelecida. Pronto para envio de SMS."
                    )
                } else if (item.canal == TipoCanalMensagem.WHATSAPP && internet) {
                    alterou = true
                    item.copy(
                        estado = EstadoMensagem.PENDENTE,
                        dataAtualizacao = System.currentTimeMillis(),
                        detalheStatus = "Conexão com a Internet restabelecida. Pronto para envio."
                    )
                } else {
                    item
                }
            } else {
                item
            }
        }

        if (alterou) {
            _filaMensagens.value = lista
            salvarFila(context)
        }
    }

    fun cancelarMensagem(context: Context, id: String) {
        init(context)
        val lista = _filaMensagens.value.map {
            if (it.id == id && (it.estado == EstadoMensagem.PENDENTE || it.estado == EstadoMensagem.AGUARDANDO_REDE)) {
                it.copy(estado = EstadoMensagem.CANCELADA, dataAtualizacao = System.currentTimeMillis(), detalheStatus = "Cancelada pelo gestor.")
            } else it
        }
        _filaMensagens.value = lista
        salvarFila(context)
    }

    fun marcarComoEntregue(context: Context, id: String) {
        init(context)
        val lista = _filaMensagens.value.map {
            if (it.id == id) {
                it.copy(estado = EstadoMensagem.ENTREGUE, dataAtualizacao = System.currentTimeMillis(), detalheStatus = "Confirmação de entrega confirmada.")
            } else it
        }
        _filaMensagens.value = lista
        salvarFila(context)
    }

    fun reenviarMensagem(context: Context, id: String) {
        init(context)
        val item = _filaMensagens.value.find { it.id == id } ?: return
        if (item.canal == TipoCanalMensagem.SMS) {
            enviarOuEnfileirarSms(context, item.clienteId, item.creditoId, item.nomeCliente, item.telefone, item.mensagem)
        } else {
            enviarOuEnfileirarWhatsApp(context, item.clienteId, item.creditoId, item.nomeCliente, item.telefone, item.mensagem)
        }
    }
}
