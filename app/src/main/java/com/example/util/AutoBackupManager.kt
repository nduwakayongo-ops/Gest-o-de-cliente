package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.model.ConfiguracaoEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object AutoBackupManager {

    private const val TAG = "AutoBackupManager"

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _lastBackupTime = MutableStateFlow(0L)
    val lastBackupTime: StateFlow<Long> = _lastBackupTime.asStateFlow()

    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm != null) {
            val activeNetwork = cm.activeNetwork
            val capabilities = cm.getNetworkCapabilities(activeNetwork)
            val hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            _isConnected.value = hasInternet

            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isConnected.value = true
                    Log.d(TAG, "Internet connected. Triggering automatic backup...")
                    CoroutineScope(Dispatchers.IO).launch {
                        executeBackup(context)
                    }
                }

                override fun onLost(network: Network) {
                    _isConnected.value = false
                    Log.d(TAG, "Internet disconnected.")
                }
            })
        }
    }

    suspend fun executeBackup(context: Context): Boolean {
        return try {
            val db = AppDatabase.getDatabase(context)
            val clientes = db.clienteDao().getAllClientes()
            val creditos = db.creditoDao().getAllCreditos()
            val config = db.configuracaoDao().getConfiguracao() ?: ConfiguracaoEntity()

            val backupJson = JSONObject().apply {
                put("app", "GestaoClientes_NduwaKayongo")
                put("backupDate", System.currentTimeMillis())
                put("gestor", "Nduwa Kayongo")
                put("totalClientes", clientes.size)
                put("totalCreditos", creditos.size)

                val clientesArr = JSONArray()
                clientes.forEach { c ->
                    clientesArr.put(JSONObject().apply {
                        put("id", c.id)
                        put("numeroInterno", c.numeroInterno)
                        put("nome", c.nome)
                        put("telefone", c.telefone)
                        put("banco", c.banco)
                        put("salario", c.salario)
                        put("empresa", c.empresa)
                    })
                }
                put("clientes", clientesArr)

                val creditosArr = JSONArray()
                creditos.forEach { cr ->
                    creditosArr.put(JSONObject().apply {
                        put("id", cr.id)
                        put("clienteId", cr.clienteId)
                        put("valorDesembolsado", cr.valorDesembolsado)
                        put("taxaPercentual", cr.taxaPercentual)
                        put("prestacaoMensal", cr.prestacaoMensal)
                        put("estado", cr.estado)
                    })
                }
                put("creditos", creditosArr)
            }

            // Save JSON file internally
            val backupDir = File(context.filesDir, "backups")
            if (!backupDir.exists()) backupDir.mkdirs()

            val backupFile = File(backupDir, "auto_backup_gestor.json")
            backupFile.writeText(backupJson.toString(2))

            val now = System.currentTimeMillis()
            _lastBackupTime.value = now

            // Update configuration in room
            db.configuracaoDao().insertOrUpdateConfiguracao(
                config.copy(backupAutomatico = true)
            )

            Log.d(TAG, "Automatic backup completed successfully at $now. Size: ${backupFile.length()} bytes.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error performing auto backup: ${e.message}", e)
            false
        }
    }
}
