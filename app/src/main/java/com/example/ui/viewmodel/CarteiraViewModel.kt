package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.example.repository.CarteiraRepository
import com.example.repository.DashboardSummary
import com.example.ui.components.KwanzaFormatters
import com.example.util.TextToSpeechHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SortOption {
    A_Z, Z_A, NUMERO, BANCO, EMPRESA, DATA_DESEMBOLSO, MAIOR_CREDITO, MENOR_CREDITO, MAIOR_ATRASO
}

class CarteiraViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = CarteiraRepository(
        db.clienteDao(),
        db.creditoDao(),
        db.pagamentoDao(),
        db.cobrancaDao(),
        db.configuracaoDao()
    )

    init {
        com.example.util.AutoBackupManager.init(application)
    }

    val isInternetConnected: StateFlow<Boolean> = com.example.util.AutoBackupManager.isConnected

    val dashboardSummary: StateFlow<DashboardSummary> = repository.dashboardSummary
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardSummary(
                carteiraAtiva = 0.0,
                totalClientes = 0,
                clientesAtivosCount = 0,
                clientesLiquidadosCount = 0,
                clientesEmAtrasoCount = 0,
                totalDesembolsado = 0.0,
                totalRecuperado = 0.0,
                valorEmAtraso = 0.0,
                riscoPercentual = 0.0,
                prestacoesVencendoCount = 0,
                notificacoesCount = 0
            )
        )

    val allClientes: StateFlow<List<ClienteEntity>> = repository.allClientes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCreditos: StateFlow<List<CreditoEntity>> = repository.allCreditos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPagamentos: StateFlow<List<PagamentoEntity>> = repository.allPagamentos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCobrancas: StateFlow<List<CobrancaEntity>> = repository.allCobrancas
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val configuracao: StateFlow<ConfiguracaoEntity?> = repository.configuracao
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Search and Filter State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedStateFilter = MutableStateFlow("Todos") // Todos, Ativo, Em atraso, Liquidado, Renegociado
    val selectedStateFilter: StateFlow<String> = _selectedStateFilter.asStateFlow()

    private val _selectedBankFilter = MutableStateFlow("Todos")
    val selectedBankFilter: StateFlow<String> = _selectedBankFilter.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.A_Z)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    // Authentication / Security State
    private val _isAuthenticated = MutableStateFlow(true)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateStateFilter(filter: String) {
        _selectedStateFilter.value = filter
    }

    fun updateBankFilter(bank: String) {
        _selectedBankFilter.value = bank
    }

    fun updateSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun authenticate(pinInput: String): Boolean {
        val currentPin = configuracao.value?.pinSeguranca
        return if (currentPin == null || currentPin == pinInput) {
            _isAuthenticated.value = true
            true
        } else {
            false
        }
    }

    fun lockApp() {
        if (configuracao.value?.pinSeguranca != null) {
            _isAuthenticated.value = false
        }
    }

    // Filtered Client List combining search, state, bank, and sort
    val filteredClientes: StateFlow<List<Pair<ClienteEntity, CreditoEntity?>>> = combine(
        repository.allClientes,
        repository.allCreditos,
        _searchQuery,
        _selectedStateFilter,
        _selectedBankFilter,
        _sortOption
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val clientes = args[0] as List<ClienteEntity>
        @Suppress("UNCHECKED_CAST")
        val creditos = args[1] as List<CreditoEntity>
        val query = args[2] as String
        val stateFilter = args[3] as String
        val bankFilter = args[4] as String
        val sort = args[5] as SortOption

        val queryLower = query.lowercase().trim()

        val creditosMap = creditos.associateBy { it.clienteId }

        val filtered = clientes.filter { cliente ->
            val credito = creditosMap[cliente.id]

            // Search filter
            val matchesSearch = queryLower.isEmpty() ||
                    cliente.nome.lowercase().contains(queryLower) ||
                    cliente.banco.lowercase().contains(queryLower) ||
                    cliente.telefone.lowercase().contains(queryLower) ||
                    cliente.empresa.lowercase().contains(queryLower) ||
                    cliente.numeroInterno.lowercase().contains(queryLower) ||
                    (credito != null && credito.valorDesembolsado.toString().contains(queryLower)) ||
                    (credito != null && credito.estado.lowercase().contains(queryLower))

            // State filter
            val matchesState = stateFilter == "Todos" || (credito != null && credito.estado.equals(stateFilter, ignoreCase = true))

            // Bank filter
            val matchesBank = bankFilter == "Todos" || cliente.banco.equals(bankFilter, ignoreCase = true)

            matchesSearch && matchesState && matchesBank
        }

        val pairs = filtered.map { cliente ->
            Pair(cliente, creditosMap[cliente.id])
        }

        when (sort) {
            SortOption.A_Z -> pairs.sortedBy { it.first.nome }
            SortOption.Z_A -> pairs.sortedByDescending { it.first.nome }
            SortOption.NUMERO -> pairs.sortedBy { it.first.numeroInterno }
            SortOption.BANCO -> pairs.sortedBy { it.first.banco }
            SortOption.EMPRESA -> pairs.sortedBy { it.first.empresa }
            SortOption.DATA_DESEMBOLSO -> pairs.sortedByDescending { it.second?.dataDesembolso ?: 0L }
            SortOption.MAIOR_CREDITO -> pairs.sortedByDescending { it.second?.valorDesembolsado ?: 0.0 }
            SortOption.MENOR_CREDITO -> pairs.sortedBy { it.second?.valorDesembolsado ?: 0.0 }
            SortOption.MAIOR_ATRASO -> pairs.sortedByDescending { it.second?.diasEmAtraso ?: 0 }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun zerarSistema(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.zerarSistema()
            if (isInternetConnected.value) {
                com.example.util.AutoBackupManager.executeBackup(getApplication())
            }
            onComplete()
        }
    }

    fun saveClienteComCredito(cliente: ClienteEntity, credito: CreditoEntity, onComplete: () -> Unit) {
        viewModelScope.launch {
            if (cliente.id != 0L) {
                repository.updateCliente(cliente)
                if (credito.id != 0L) {
                    repository.updateCredito(credito)
                } else {
                    repository.insertClienteWithCredito(cliente, credito)
                }
            } else {
                repository.insertClienteWithCredito(cliente, credito)
            }
            if (isInternetConnected.value) {
                com.example.util.AutoBackupManager.executeBackup(getApplication())
            }
            onComplete()
        }
    }

    fun updateCliente(cliente: ClienteEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateCliente(cliente)
            if (isInternetConnected.value) {
                com.example.util.AutoBackupManager.executeBackup(getApplication())
            }
            onComplete()
        }
    }

    fun deleteCliente(cliente: ClienteEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteCliente(cliente)
            if (isInternetConnected.value) {
                com.example.util.AutoBackupManager.executeBackup(getApplication())
            }
            onComplete()
        }
    }

    fun updateCredito(credito: CreditoEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateCredito(credito)
            if (isInternetConnected.value) {
                com.example.util.AutoBackupManager.executeBackup(getApplication())
            }
            onComplete()
        }
    }

    fun deletePagamento(pagamento: PagamentoEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deletePagamento(pagamento)
            if (isInternetConnected.value) {
                com.example.util.AutoBackupManager.executeBackup(getApplication())
            }
            onComplete()
        }
    }

    fun deleteCobranca(cobranca: CobrancaEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteCobranca(cobranca)
            if (isInternetConnected.value) {
                com.example.util.AutoBackupManager.executeBackup(getApplication())
            }
            onComplete()
        }
    }

    fun registrarPagamento(pagamento: PagamentoEntity, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.insertPagamento(pagamento)
            if (isInternetConnected.value) {
                com.example.util.AutoBackupManager.executeBackup(getApplication())
            }
            onComplete()
        }
    }

    fun registrarCobranca(cobranca: CobrancaEntity, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.insertCobranca(cobranca)
            if (isInternetConnected.value) {
                com.example.util.AutoBackupManager.executeBackup(getApplication())
            }
            onComplete()
        }
    }

    // Text to Speech (TTS) Helper for Voice Reading
    private val ttsHelper = TextToSpeechHelper(application)
    val isSpeaking: StateFlow<Boolean> = ttsHelper.isSpeaking

    fun speakText(text: String) {
        ttsHelper.speak(text)
    }

    fun stopSpeaking() {
        ttsHelper.stop()
    }

    fun speakNotificationForCliente(cliente: ClienteEntity, credito: CreditoEntity?) {
        val prestacao = credito?.prestacaoMensal ?: 0.0
        val valorFormatado = KwanzaFormatters.formatAOA(prestacao)
        val dataVenc = KwanzaFormatters.formatDate(credito?.proximoVencimento ?: 0L)
        val estado = credito?.estado ?: "Ativo"
        val diasAtraso = credito?.diasEmAtraso ?: 0

        val texto = if (diasAtraso > 0) {
            "Alerta de vencimento em atraso. Cliente: ${cliente.nome}. Banco: ${cliente.banco}. " +
            "Valor da prestação: $valorFormatado. Data de vencimento: $dataVenc. " +
            "Estado: em atraso há $diasAtraso dias."
        } else {
            "Notificação de vencimento do cliente: ${cliente.nome}. Banco: ${cliente.banco}. " +
            "Valor da prestação: $valorFormatado. Data de vencimento: $dataVenc. " +
            "Estado do crédito: $estado."
        }
        speakText(texto)
    }

    fun speakAllAlerts(alertas: List<Pair<ClienteEntity, CreditoEntity?>>) {
        if (alertas.isEmpty()) {
            speakText("Não existem alertas de cobrança ou vencimentos pendentes no momento.")
            return
        }
        val count = alertas.size
        val sb = StringBuilder("Atenção gestor Nduwa Kayongo. Leitura em viva voz dos $count alertas de clientes. ")
        alertas.take(5).forEachIndexed { index, (cliente, credito) ->
            val valor = KwanzaFormatters.formatAOA(credito?.prestacaoMensal ?: 0.0)
            val dataVenc = KwanzaFormatters.formatDate(credito?.proximoVencimento ?: 0L)
            val dias = credito?.diasEmAtraso ?: 0
            sb.append("Alerta número ${index + 1}: Cliente ${cliente.nome}, Banco ${cliente.banco}. Valor da prestação: $valor. Data de vencimento: $dataVenc. ")
            if (dias > 0) {
                sb.append("Situação: em atraso há $dias dias. ")
            }
        }
        if (count > 5) {
            sb.append("Existem mais ${count - 5} alertas pendentes.")
        }
        speakText(sb.toString())
    }

    override fun onCleared() {
        super.onCleared()
        ttsHelper.shutdown()
    }

    fun updateConfiguracao(config: ConfiguracaoEntity) {
        viewModelScope.launch {
            repository.updateConfiguracao(config)
        }
    }
}
