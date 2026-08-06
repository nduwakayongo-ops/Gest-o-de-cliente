package com.example.repository

import com.example.data.dao.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar

data class DashboardSummary(
    val carteiraAtiva: Double,
    val totalClientes: Int,
    val clientesAtivosCount: Int,
    val clientesLiquidadosCount: Int,
    val clientesEmAtrasoCount: Int,
    val totalDesembolsado: Double,
    val totalRecuperado: Double,
    val valorEmAtraso: Double,
    val riscoPercentual: Double,
    val prestacoesVencendoCount: Int,
    val notificacoesCount: Int
)

class CarteiraRepository(
    private val clienteDao: ClienteDao,
    private val creditoDao: CreditoDao,
    private val pagamentoDao: PagamentoDao,
    private val cobrancaDao: CobrancaDao,
    private val configuracaoDao: ConfiguracaoDao
) {
    val allClientes: Flow<List<ClienteEntity>> = clienteDao.getAllClientesFlow()
    val allCreditos: Flow<List<CreditoEntity>> = creditoDao.getAllCreditosFlow()
    val allPagamentos: Flow<List<PagamentoEntity>> = pagamentoDao.getAllPagamentosFlow()
    val configuracao: Flow<ConfiguracaoEntity?> = configuracaoDao.getConfiguracaoFlow()

    fun getClienteWithDetails(clienteId: Long): Flow<ClienteEntity?> = clienteDao.getClienteByIdFlow(clienteId)
    fun getCreditosByCliente(clienteId: Long): Flow<List<CreditoEntity>> = creditoDao.getCreditosByClienteFlow(clienteId)
    fun getPagamentosByCliente(clienteId: Long): Flow<List<PagamentoEntity>> = pagamentoDao.getPagamentosByClienteFlow(clienteId)
    fun getCobrancasByCliente(clienteId: Long): Flow<List<CobrancaEntity>> = cobrancaDao.getCobrancasByClienteFlow(clienteId)

    suspend fun insertClienteWithCredito(cliente: ClienteEntity, credito: CreditoEntity): Long {
        val clienteId = clienteDao.insertCliente(cliente)
        val creditoWithClienteId = credito.copy(clienteId = clienteId)
        creditoDao.insertCredito(creditoWithClienteId)
        return clienteId
    }

    suspend fun updateCliente(cliente: ClienteEntity) = clienteDao.updateCliente(cliente)
    suspend fun updateCredito(credito: CreditoEntity) = creditoDao.updateCredito(credito)
    suspend fun insertPagamento(pagamento: PagamentoEntity) {
        pagamentoDao.insertPagamento(pagamento)
        // Update credit paid installments or state
        val credito = creditoDao.getCreditoById(pagamento.creditoId)
        if (credito != null) {
            val novasPagas = credito.prestacoesPagas + 1
            val novoEstado = if (novasPagas >= credito.quantidadePrestacoes) "Liquidado" else credito.estado
            val novoAtrasoVal = if (novoEstado == "Liquidado") 0.0 else (credito.valorEmAtraso - pagamento.valorPago).coerceAtLeast(0.0)
            val novoAtrasoDias = if (novoAtrasoVal <= 0.0) 0 else credito.diasEmAtraso
            
            val novoProximoVencimento = if (novoEstado == "Liquidado") 0L else {
                val currentBase = if (credito.proximoVencimento > 0) credito.proximoVencimento else credito.dataPrimeiroPagamento
                val cal = Calendar.getInstance().apply {
                    timeInMillis = currentBase
                    add(Calendar.MONTH, 1)
                }
                cal.timeInMillis
            }

            creditoDao.updateCredito(
                credito.copy(
                    prestacoesPagas = novasPagas,
                    estado = novoEstado,
                    valorEmAtraso = novoAtrasoVal,
                    diasEmAtraso = novoAtrasoDias,
                    proximoVencimento = novoProximoVencimento
                )
            )
        }
    }

    suspend fun insertCobranca(cobranca: CobrancaEntity) = cobrancaDao.insertCobranca(cobranca)
    suspend fun deleteCliente(cliente: ClienteEntity) = clienteDao.deleteCliente(cliente)
    suspend fun deletePagamento(pagamento: PagamentoEntity) = pagamentoDao.deletePagamento(pagamento)
    suspend fun deleteCobranca(cobranca: CobrancaEntity) = cobrancaDao.deleteCobranca(cobranca)
    suspend fun updateConfiguracao(config: ConfiguracaoEntity) = configuracaoDao.insertOrUpdateConfiguracao(config)

    suspend fun zerarSistema() {
        clienteDao.deleteAllClientes()
        creditoDao.deleteAllCreditos()
        pagamentoDao.deleteAllPagamentos()
        cobrancaDao.deleteAllCobrancas()
    }

    val dashboardSummary: Flow<DashboardSummary> = combine(
        clienteDao.getAllClientesFlow(),
        creditoDao.getAllCreditosFlow(),
        pagamentoDao.getAllPagamentosFlow()
    ) { clientes, creditos, pagamentos ->
        val totalClientes = clientes.size

        val totalDesembolsado = creditos.sumOf { it.valorDesembolsado }
        val totalRecuperado = pagamentos.sumOf { it.valorPago }

        // Formula: Carteira Ativa = Total Desembolsado - Valor Recuperado
        val carteiraAtiva = (totalDesembolsado - totalRecuperado).coerceAtLeast(0.0)

        val valorEmAtraso = creditos.filter { it.estado == "Em atraso" || it.valorEmAtraso > 0 }
            .sumOf { it.valorEmAtraso }

        val riscoPercentual = if (carteiraAtiva > 0) {
            (valorEmAtraso / carteiraAtiva) * 100
        } else {
            0.0
        }

        val clientesAtivosCount = creditos.count { it.estado == "Ativo" || it.estado == "Em atraso" }
        val clientesLiquidadosCount = creditos.count { it.estado == "Liquidado" }
        val clientesEmAtrasoCount = creditos.count { it.estado == "Em atraso" || it.diasEmAtraso > 0 }

        val now = System.currentTimeMillis()
        val calLimit = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, 5)
        }.timeInMillis

        val prestacoesVencendoCount = creditos.count {
            it.estado == "Ativo" && it.proximoVencimento in now..calLimit
        }

        val notificacoesCount = clientesEmAtrasoCount + prestacoesVencendoCount

        DashboardSummary(
            carteiraAtiva = carteiraAtiva,
            totalClientes = totalClientes,
            clientesAtivosCount = clientesAtivosCount,
            clientesLiquidadosCount = clientesLiquidadosCount,
            clientesEmAtrasoCount = clientesEmAtrasoCount,
            totalDesembolsado = totalDesembolsado,
            totalRecuperado = totalRecuperado,
            valorEmAtraso = valorEmAtraso,
            riscoPercentual = riscoPercentual,
            prestacoesVencendoCount = prestacoesVencendoCount,
            notificacoesCount = notificacoesCount
        )
    }
}
