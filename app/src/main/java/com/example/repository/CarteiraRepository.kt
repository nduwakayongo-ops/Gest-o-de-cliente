package com.example.repository

import com.example.data.dao.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar

data class DashboardSummary(
    val carteiraAtiva: Double,        // Saldo da Carteira de Crédito = Capital Desembolsado - Capital Amortizado
    val totalClientes: Int,
    val clientesAtivosCount: Int,
    val clientesLiquidadosCount: Int,
    val clientesEmAtrasoCount: Int,
    val totalDesembolsado: Double,    // Capital Desembolsado (total concedido)
    val capitalAmortizado: Double,    // Capital Amortizado (capital recuperado que reduz a carteira)
    val jurosRecebidos: Double,       // Juros Recebidos (receita acumulada que NÃO reduz a carteira)
    val totalRecebido: Double,        // Capital Amortizado + Juros Recebidos
    val valorEmAtraso: Double,
    val riscoPercentual: Double,
    val prestacoesVencendoCount: Int,
    val notificacoesCount: Int,
    val totalRecuperado: Double = totalRecebido // Mantido para compatibilidade com chamadas legadas
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
    val allCobrancas: Flow<List<CobrancaEntity>> = cobrancaDao.getAllCobrancasFlow()
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

    suspend fun getCreditoById(creditoId: Long): CreditoEntity? = creditoDao.getCreditoById(creditoId)

    suspend fun updateCliente(cliente: ClienteEntity) = clienteDao.updateCliente(cliente)
    suspend fun updateCredito(credito: CreditoEntity) = creditoDao.updateCredito(credito)
    suspend fun insertPagamento(pagamento: PagamentoEntity) {
        pagamentoDao.insertPagamento(pagamento)
        // Update credit paid installments or state
        val credito = creditoDao.getCreditoById(pagamento.creditoId)
        if (credito != null) {
            val novasPagas = credito.prestacoesPagas + 1
            val isLiquidadoTotal = novasPagas >= credito.quantidadePrestacoes
            val novoAtrasoVal = if (isLiquidadoTotal) 0.0 else (credito.valorEmAtraso - pagamento.valorPago).coerceAtLeast(0.0)
            val novoAtrasoDias = if (novoAtrasoVal <= 0.0) 0 else credito.diasEmAtraso
            val novoEstado = when {
                isLiquidadoTotal -> "Liquidado"
                novoAtrasoVal > 0.0 -> "Em atraso"
                else -> "Ativo"
            }
            
            val novoProximoVencimento = if (isLiquidadoTotal) 0L else {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = if (credito.dataPrimeiroPagamento > 0) credito.dataPrimeiroPagamento else System.currentTimeMillis()
                    add(Calendar.MONTH, novasPagas)
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

        // 1. Capital Desembolsado: Soma do capital efetivamente concedido
        val totalDesembolsado = creditos.sumOf { it.valorDesembolsado }

        // 2. Capital Amortizado: Apenas a fração de capital amortizado das prestações pagas
        // Fórmula: Capital mensal = valorDesembolsado / quantidadePrestacoes
        // Capital amortizado = Capital mensal * prestacoesPagas
        val capitalAmortizado = creditos.sumOf { credito ->
            val n = credito.quantidadePrestacoes
            val pagas = credito.prestacoesPagas.coerceIn(0, n)
            if (n > 0) (credito.valorDesembolsado / n) * pagas else 0.0
        }

        // 3. Juros Recebidos: Receita financeira apurada (NÃO reduz a carteira!)
        // Fórmula: Juros mensal = valorDesembolsado * (taxaPercentual / 100)
        // Juros recebidos = Juros mensal * prestacoesPagas
        val jurosRecebidos = creditos.sumOf { credito ->
            val n = credito.quantidadePrestacoes
            val pagas = credito.prestacoesPagas.coerceIn(0, n)
            (credito.valorDesembolsado * (credito.taxaPercentual / 100.0)) * pagas
        }

        // 4. Total Recebido = Capital Amortizado + Juros Recebidos
        val totalRecebido = capitalAmortizado + jurosRecebidos

        // 5. REGRA DE OURO: Saldo da Carteira de Crédito = Capital Desembolsado - Capital Amortizado
        // Juros recebidos NUNCA reduzem a carteira de crédito.
        val carteiraAtiva = (totalDesembolsado - capitalAmortizado).coerceAtLeast(0.0)

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
            capitalAmortizado = capitalAmortizado,
            jurosRecebidos = jurosRecebidos,
            totalRecebido = totalRecebido,
            valorEmAtraso = valorEmAtraso,
            riscoPercentual = riscoPercentual,
            prestacoesVencendoCount = prestacoesVencendoCount,
            notificacoesCount = notificacoesCount,
            totalRecuperado = totalRecebido
        )
    }
}
