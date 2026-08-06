package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ClienteEntity
import com.example.data.model.CreditoEntity
import com.example.ui.components.HeaderBranding
import com.example.ui.screens.*
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.GestaoClientesTheme
import com.example.ui.theme.GrayBackground
import com.example.ui.viewmodel.CarteiraViewModel

class MainActivity : FragmentActivity() {

    private val viewModel: CarteiraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Schedule daily notifications at 08:00 and 17:00
        com.example.util.NotificationSchedulerHelper.scheduleDailyAlarms(applicationContext)

        setContent {
            val configuracao by viewModel.configuracao.collectAsStateWithLifecycle()
            val systemInDark = androidx.compose.foundation.isSystemInDarkTheme()
            val isDarkTheme = when (configuracao?.tema) {
                "Escuro" -> true
                "Claro" -> false
                else -> systemInDark
            }

            GestaoClientesTheme(darkTheme = isDarkTheme) {
                MainAppScreen(
                    viewModel = viewModel,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = {
                        val nextTema = if (isDarkTheme) "Claro" else "Escuro"
                        val currentConf = configuracao ?: com.example.data.model.ConfiguracaoEntity()
                        viewModel.updateConfiguracao(currentConf.copy(tema = nextTema))
                    }
                )
            }
        }
    }
}

enum class ScreenTab(val title: String, val route: String) {
    DASHBOARD("Início", "dashboard"),
    CLIENTES("Clientes", "clientes"),
    CALCULADORA("Calculadora", "calculadora"),
    CALENDARIO("Calendário", "calendario"),
    ESTATISTICAS("Simulador", "estatisticas"),
    RELATORIOS("Relatórios", "relatorios"),
    ALERTAS("Alertas", "alertas"),
    CONFIGURACOES("Configurações", "configuracoes"),
    ADD_CLIENTE("Novo Cliente", "add_cliente"),
    CLIENTE_DETAIL("Detalhes Cliente", "cliente_detail")
}

@Composable
fun MainAppScreen(
    viewModel: CarteiraViewModel,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {}
) {
    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()
    val summary by viewModel.dashboardSummary.collectAsStateWithLifecycle()
    val clientes by viewModel.filteredClientes.collectAsStateWithLifecycle()
    val allClientesList by viewModel.allClientes.collectAsStateWithLifecycle()
    val allCreditosList by viewModel.allCreditos.collectAsStateWithLifecycle()
    val allPagamentosList by viewModel.allPagamentos.collectAsStateWithLifecycle()
    val configuracao by viewModel.configuracao.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedStateFilter by viewModel.selectedStateFilter.collectAsStateWithLifecycle()
    val selectedBankFilter by viewModel.selectedBankFilter.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(ScreenTab.DASHBOARD) }
    var selectedClienteId by remember { mutableStateOf<Long?>(null) }
    var clienteToEdit by remember { mutableStateOf<ClienteEntity?>(null) }

    if (!isAuthenticated && configuracao?.pinSeguranca != null) {
        PinLockScreen(
            onUnlock = { pinInput -> viewModel.authenticate(pinInput) },
            fotoPerfilUri = configuracao?.fotoPerfilUri,
            biometriaAtiva = configuracao?.biometriaAtiva ?: true
        )
        return
    }

    val creditosMap = remember(allCreditosList) { allCreditosList.associateBy { it.clienteId } }
    val allClientesWithCredito = remember(allClientesList, creditosMap) {
        allClientesList.map { cliente -> Pair(cliente, creditosMap[cliente.id]) }
    }

    val selectedClientePair = remember(allClientesWithCredito, selectedClienteId) {
        allClientesWithCredito.find { it.first.id == selectedClienteId }
    }

    val selectedPagamentos = remember(allPagamentosList, selectedClienteId) {
        allPagamentosList.filter { it.clienteId == selectedClienteId }
    }

    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val isInternetConnected by viewModel.isInternetConnected.collectAsStateWithLifecycle()
    val calculadoraViewModel: com.example.ui.viewmodel.CalculadoraViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    Scaffold(
        topBar = {
            if (currentTab != ScreenTab.ADD_CLIENTE && currentTab != ScreenTab.CLIENTE_DETAIL) {
                HeaderBranding(
                    notificacoesCount = summary.notificacoesCount,
                    onNotificationClick = { currentTab = ScreenTab.ALERTAS },
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme,
                    fotoPerfilUri = configuracao?.fotoPerfilUri
                )
            }
        },
        bottomBar = {
            if (currentTab != ScreenTab.ADD_CLIENTE && currentTab != ScreenTab.CLIENTE_DETAIL) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    NavigationBarItem(
                        selected = currentTab == ScreenTab.DASHBOARD,
                        onClick = { currentTab = ScreenTab.DASHBOARD },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Início") },
                        label = { Text("Início", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = DeepBlue, indicatorColor = DeepBlue.copy(alpha = 0.12f)),
                        modifier = Modifier.testTag("nav_item_dashboard")
                    )

                    NavigationBarItem(
                        selected = currentTab == ScreenTab.CLIENTES,
                        onClick = { currentTab = ScreenTab.CLIENTES },
                        icon = { Icon(Icons.Default.People, contentDescription = "Clientes") },
                        label = { Text("Clientes", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = DeepBlue, indicatorColor = DeepBlue.copy(alpha = 0.12f)),
                        modifier = Modifier.testTag("nav_item_clientes")
                    )

                    NavigationBarItem(
                        selected = currentTab == ScreenTab.CALCULADORA,
                        onClick = { currentTab = ScreenTab.CALCULADORA },
                        icon = { Icon(Icons.Default.Calculate, contentDescription = "Calculadora") },
                        label = { Text("Calculadora", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = DeepBlue, indicatorColor = DeepBlue.copy(alpha = 0.12f)),
                        modifier = Modifier.testTag("nav_item_calculadora")
                    )

                    NavigationBarItem(
                        selected = currentTab == ScreenTab.CALENDARIO,
                        onClick = { currentTab = ScreenTab.CALENDARIO },
                        icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Calendário") },
                        label = { Text("Calendário", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = DeepBlue, indicatorColor = DeepBlue.copy(alpha = 0.12f)),
                        modifier = Modifier.testTag("nav_item_calendario")
                    )

                    NavigationBarItem(
                        selected = currentTab == ScreenTab.ESTATISTICAS,
                        onClick = { currentTab = ScreenTab.ESTATISTICAS },
                        icon = { Icon(Icons.Default.BarChart, contentDescription = "Estatísticas") },
                        label = { Text("Simulador", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = DeepBlue, indicatorColor = DeepBlue.copy(alpha = 0.12f)),
                        modifier = Modifier.testTag("nav_item_estatisticas")
                    )

                    NavigationBarItem(
                        selected = currentTab == ScreenTab.RELATORIOS || currentTab == ScreenTab.CONFIGURACOES,
                        onClick = { currentTab = ScreenTab.RELATORIOS },
                        icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = "Relatórios") },
                        label = { Text("Relatórios", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = DeepBlue, indicatorColor = DeepBlue.copy(alpha = 0.12f)),
                        modifier = Modifier.testTag("nav_item_relatorios")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentTab) {
                ScreenTab.DASHBOARD -> DashboardScreen(
                    summary = summary,
                    recentClientes = clientes,
                    onNavigateToClientes = { currentTab = ScreenTab.CLIENTES },
                    onNavigateToAddCliente = { currentTab = ScreenTab.ADD_CLIENTE },
                    onNavigateToAlertas = { currentTab = ScreenTab.ALERTAS },
                    onNavigateToRelatorios = { currentTab = ScreenTab.RELATORIOS },
                    onNavigateToSimulador = { currentTab = ScreenTab.ESTATISTICAS },
                    onSelectCliente = { id ->
                        selectedClienteId = id
                        currentTab = ScreenTab.CLIENTE_DETAIL
                    }
                )

                ScreenTab.CLIENTES -> ClientesListScreen(
                    clientes = clientes,
                    searchQuery = searchQuery,
                    selectedStateFilter = selectedStateFilter,
                    selectedBankFilter = selectedBankFilter,
                    sortOption = sortOption,
                    onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                    onStateFilterChange = { viewModel.updateStateFilter(it) },
                    onBankFilterChange = { viewModel.updateBankFilter(it) },
                    onSortOptionChange = { viewModel.updateSortOption(it) },
                    onSelectCliente = { id ->
                        selectedClienteId = id
                        currentTab = ScreenTab.CLIENTE_DETAIL
                    },
                    onAddClienteClick = {
                        clienteToEdit = null
                        currentTab = ScreenTab.ADD_CLIENTE
                    },
                    onEditCliente = { cliente ->
                        clienteToEdit = cliente
                        currentTab = ScreenTab.ADD_CLIENTE
                    },
                    onDeleteCliente = { cliente ->
                        viewModel.deleteCliente(cliente)
                    }
                )

                ScreenTab.CALCULADORA -> CalculadoraInteligenteScreen(
                    calculadoraViewModel = calculadoraViewModel,
                    onApplyToNewCliente = { principal, taxa, prazo ->
                        calculadoraViewModel.updateValorDesembolsado(principal.toInt().toString())
                        calculadoraViewModel.updateTaxaJuros(taxa.toInt().toString())
                        calculadoraViewModel.updatePrazoMeses(prazo.toString())
                        clienteToEdit = null
                        currentTab = ScreenTab.ADD_CLIENTE
                    },
                    onSpeakSummary = { text -> viewModel.speakText(text) }
                )

                ScreenTab.CALENDARIO -> CalendarioScreen(
                    clientesWithCredito = allClientesWithCredito,
                    onSelectCliente = { id ->
                        selectedClienteId = id
                        currentTab = ScreenTab.CLIENTE_DETAIL
                    }
                )

                ScreenTab.ESTATISTICAS -> EstatisticasScreen(
                    summary = summary
                )

                ScreenTab.RELATORIOS -> RelatoriosScreen(
                    summary = summary,
                    clientesWithCredito = allClientesWithCredito
                )

                ScreenTab.ALERTAS -> AlertasScreen(
                    clientesWithCredito = allClientesWithCredito,
                    onSelectCliente = { id ->
                        selectedClienteId = id
                        currentTab = ScreenTab.CLIENTE_DETAIL
                    },
                    onSpeakCliente = { cliente, credito -> viewModel.speakNotificationForCliente(cliente, credito) },
                    onSpeakAll = { viewModel.speakAllAlerts(allClientesWithCredito) },
                    onStopSpeak = { viewModel.stopSpeaking() },
                    isSpeaking = isSpeaking
                )

                ScreenTab.CONFIGURACOES -> BackupConfiguracoesScreen(
                    configuracao = configuracao,
                    isInternetConnected = isInternetConnected,
                    onSaveConfiguracao = { viewModel.updateConfiguracao(it) },
                    onZerarSistema = { viewModel.zerarSistema() }
                )

                ScreenTab.ADD_CLIENTE -> AddEditClienteScreen(
                    clienteToEdit = clienteToEdit,
                    creditoToEdit = allClientesWithCredito.find { it.first.id == clienteToEdit?.id }?.second,
                    onBackClick = { currentTab = ScreenTab.CLIENTES },
                    onSaveCliente = { cliente, credito ->
                        if (cliente.id != 0L) {
                            viewModel.updateCliente(cliente)
                            viewModel.updateCredito(credito)
                            clienteToEdit = null
                            currentTab = ScreenTab.CLIENTES
                        } else {
                            viewModel.saveClienteComCredito(cliente, credito) {
                                currentTab = ScreenTab.CLIENTES
                            }
                        }
                    }
                )

                ScreenTab.CLIENTE_DETAIL -> ClienteDetailScreen(
                    cliente = selectedClientePair?.first,
                    creditos = if (selectedClientePair?.second != null) listOf(selectedClientePair.second!!) else emptyList(),
                    pagamentos = selectedPagamentos,
                    cobrancas = emptyList(),
                    onBackClick = { currentTab = ScreenTab.CLIENTES },
                    onRegistrarPagamento = { pag -> viewModel.registrarPagamento(pag) {} },
                    onRegistrarCobranca = { cob -> viewModel.registrarCobranca(cob) {} },
                    onSpeakResumo = { cliente, credito -> viewModel.speakNotificationForCliente(cliente, credito) },
                    onEditCliente = { cliente ->
                        clienteToEdit = cliente
                        currentTab = ScreenTab.ADD_CLIENTE
                    },
                    onDeleteCliente = { cliente ->
                        viewModel.deleteCliente(cliente)
                        currentTab = ScreenTab.CLIENTES
                    },
                    onDeletePagamento = { pag -> viewModel.deletePagamento(pag) },
                    onDeleteCobranca = { cob -> viewModel.deleteCobranca(cob) }
                )
            }
        }
    }
}
