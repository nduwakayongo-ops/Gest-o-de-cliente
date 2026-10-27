package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.*
import kotlin.math.pow

enum class TipoCalculoJuros(val label: String, val descricao: String) {
    PADRAO_CARTEIRA("Oficial Mbuli (Capital + Juros)", "Prestação = Capital Mensal + Juros Mensal. Juros não reduzem a carteira"),
    SIMPLES("Juros Simples Directo", "Juros fixos calculados sobre o capital inicial"),
    COMPOSTO_PRICE("Tabela Price (Francês)", "Prestações fixas amortização exponencial"),
    SAC("Sistema SAC (Amortização Constante)", "Amortização fixa do capital, prestações decrescentes")
}

data class ItemParcelaSchedule(
    val numeroParcela: Int,
    val valorPrestacao: Double,
    val valorJuros: Double,
    val valorAmortizacao: Double,
    val saldoDevedorRestante: Double,
    val saldoCarteiraAntes: Double = 0.0,
    val isPaga: Boolean = false
)

data class CalculoSaldoDevedorState(
    val principal: Double = 0.0,
    val taxaPercentual: Double = 0.0,
    val prazoMeses: Int = 0,
    val parcelasPagas: Int = 0,
    val tipoCalculo: TipoCalculoJuros = TipoCalculoJuros.PADRAO_CARTEIRA,
    val capitalMensal: Double = 0.0,
    val jurosMensal: Double = 0.0,
    val valorJurosTotal: Double = 0.0,
    val valorTotalLiquidar: Double = 0.0,
    val prestacaoMensal: Double = 0.0,
    val capitalAmortizadoAteAgora: Double = 0.0,
    val jurosRecebidosAteAgora: Double = 0.0,
    val saldoDevedorAtual: Double = 0.0,
    val totalPagoAteAgora: Double = 0.0,
    val percentualQuitado: Double = 0.0,
    val cronograma: List<ItemParcelaSchedule> = emptyList()
)

class CalculadoraViewModel : ViewModel() {

    // Input States (Defaults matching the official Mbuli portfolio test case: 500.000 Kz, 12 meses, 7.5%, 1 parcela paga)
    private val _valorDesembolsadoInput = MutableStateFlow("500000")
    val valorDesembolsadoInput: StateFlow<String> = _valorDesembolsadoInput.asStateFlow()

    private val _taxaJurosInput = MutableStateFlow("7.5")
    val taxaJurosInput: StateFlow<String> = _taxaJurosInput.asStateFlow()

    private val _prazoMesesInput = MutableStateFlow("12")
    val prazoMesesInput: StateFlow<String> = _prazoMesesInput.asStateFlow()

    private val _parcelasPagasInput = MutableStateFlow("1")
    val parcelasPagasInput: StateFlow<String> = _parcelasPagasInput.asStateFlow()

    private val _tipoCalculo = MutableStateFlow(TipoCalculoJuros.PADRAO_CARTEIRA)
    val tipoCalculo: StateFlow<TipoCalculoJuros> = _tipoCalculo.asStateFlow()

    // Combined Reactive Calculated State
    val calculoState: StateFlow<CalculoSaldoDevedorState> = combine(
        _valorDesembolsadoInput,
        _taxaJurosInput,
        _prazoMesesInput,
        _parcelasPagasInput,
        _tipoCalculo
    ) { valDes, taxa, prazo, pagas, tipo ->
        fun parseDouble(s: String): Double =
            s.replace(",", ".").replace(" ", "").replace("[^0-9.]".toRegex(), "").toDoubleOrNull() ?: 0.0

        fun parseInt(s: String): Int =
            s.replace(" ", "").replace("[^0-9]".toRegex(), "").toIntOrNull() ?: 0

        val principal = parseDouble(valDes)
        val taxaPct = parseDouble(taxa)
        val nMeses = parseInt(prazo).coerceAtLeast(1)
        val nPagas = parseInt(pagas).coerceIn(0, nMeses)

        calcularSaldoDevedorEParcelas(
            principal = principal,
            taxaPercentual = taxaPct,
            prazoMeses = nMeses,
            parcelasPagas = nPagas,
            tipo = tipo
        )
    }.stateIn(
        scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalculoSaldoDevedorState()
    )

    fun updateValorDesembolsado(value: String) {
        _valorDesembolsadoInput.value = value
    }

    fun updateTaxaJuros(value: String) {
        _taxaJurosInput.value = value
    }

    fun updatePrazoMeses(value: String) {
        _prazoMesesInput.value = value
    }

    fun updateParcelasPagas(value: String) {
        _parcelasPagasInput.value = value
    }

    fun updateTipoCalculo(tipo: TipoCalculoJuros) {
        _tipoCalculo.value = tipo
    }

    companion object {
        fun calcularSaldoDevedorEParcelas(
            principal: Double,
            taxaPercentual: Double,
            prazoMeses: Int,
            parcelasPagas: Int,
            tipo: TipoCalculoJuros
        ): CalculoSaldoDevedorState {
            if (principal <= 0.0 || prazoMeses <= 0) {
                return CalculoSaldoDevedorState(
                    principal = principal,
                    taxaPercentual = taxaPercentual,
                    prazoMeses = prazoMeses,
                    parcelasPagas = parcelasPagas,
                    tipoCalculo = tipo
                )
            }

            val i = taxaPercentual / 100.0
            val n = prazoMeses
            val schedule = mutableListOf<ItemParcelaSchedule>()

            var totalJuros = 0.0
            var prestacaoMensalRef = 0.0

            when (tipo) {
                TipoCalculoJuros.PADRAO_CARTEIRA -> {
                    // REGRA DE OURO MBULI:
                    // Capital mensal = Capital inicial / Prazo
                    // Juros mensal = Capital inicial * Taxa mensal
                    // Prestação mensal = Capital mensal + Juros mensal
                    // A carteira é reduzida exclusivamente pelo Capital amortizado!
                    val capitalMensal = principal / n
                    val jurosMensal = principal * i
                    val pmt = capitalMensal + jurosMensal
                    totalJuros = jurosMensal * n
                    prestacaoMensalRef = pmt

                    var capitalRestante = principal
                    for (m in 1..n) {
                        val saldoAntes = capitalRestante
                        val amortizacao = if (m == n) saldoAntes else capitalMensal
                        capitalRestante = (capitalRestante - amortizacao).coerceAtLeast(0.0)
                        schedule.add(
                            ItemParcelaSchedule(
                                numeroParcela = m,
                                valorPrestacao = amortizacao + jurosMensal,
                                valorJuros = jurosMensal,
                                valorAmortizacao = amortizacao,
                                saldoDevedorRestante = capitalRestante,
                                saldoCarteiraAntes = saldoAntes,
                                isPaga = m <= parcelasPagas
                            )
                        )
                    }
                }

                TipoCalculoJuros.SIMPLES -> {
                    val capitalMensal = principal / n
                    val jurosMensal = principal * i
                    val pmt = capitalMensal + jurosMensal
                    totalJuros = jurosMensal * n
                    prestacaoMensalRef = pmt

                    var capitalRestante = principal
                    for (m in 1..n) {
                        val saldoAntes = capitalRestante
                        val amortizacao = if (m == n) saldoAntes else capitalMensal
                        capitalRestante = (capitalRestante - amortizacao).coerceAtLeast(0.0)
                        schedule.add(
                            ItemParcelaSchedule(
                                numeroParcela = m,
                                valorPrestacao = amortizacao + jurosMensal,
                                valorJuros = jurosMensal,
                                valorAmortizacao = amortizacao,
                                saldoDevedorRestante = capitalRestante,
                                saldoCarteiraAntes = saldoAntes,
                                isPaga = m <= parcelasPagas
                            )
                        )
                    }
                }

                TipoCalculoJuros.COMPOSTO_PRICE -> {
                    val pmt = if (i > 0) {
                        val factor = (1 + i).pow(n)
                        principal * (i * factor) / (factor - 1)
                    } else {
                        principal / n
                    }
                    prestacaoMensalRef = pmt
                    var saldoDevedor = principal
                    for (m in 1..n) {
                        val saldoAntes = saldoDevedor
                        val jurosMes = saldoDevedor * i
                        val amortizacaoMes = pmt - jurosMes
                        saldoDevedor = (saldoDevedor - amortizacaoMes).coerceAtLeast(0.0)
                        totalJuros += jurosMes

                        schedule.add(
                            ItemParcelaSchedule(
                                numeroParcela = m,
                                valorPrestacao = pmt,
                                valorJuros = jurosMes,
                                valorAmortizacao = amortizacaoMes,
                                saldoDevedorRestante = if (m == n) 0.0 else saldoDevedor,
                                saldoCarteiraAntes = saldoAntes,
                                isPaga = m <= parcelasPagas
                            )
                        )
                    }
                }

                TipoCalculoJuros.SAC -> {
                    val amortizacaoConstante = principal / n
                    var saldoDevedor = principal
                    for (m in 1..n) {
                        val saldoAntes = saldoDevedor
                        val jurosMes = saldoDevedor * i
                        val pmt = amortizacaoConstante + jurosMes
                        if (m == 1) prestacaoMensalRef = pmt
                        saldoDevedor = (saldoDevedor - amortizacaoConstante).coerceAtLeast(0.0)
                        totalJuros += jurosMes

                        schedule.add(
                            ItemParcelaSchedule(
                                numeroParcela = m,
                                valorPrestacao = pmt,
                                valorJuros = jurosMes,
                                valorAmortizacao = amortizacaoConstante,
                                saldoDevedorRestante = if (m == n) 0.0 else saldoDevedor,
                                saldoCarteiraAntes = saldoAntes,
                                isPaga = m <= parcelasPagas
                            )
                        )
                    }
                }
            }

            val capitalMensalGeral = if (n > 0) principal / n else 0.0
            val jurosMensalGeral = principal * i
            val parcelasEfetivas = parcelasPagas.coerceIn(0, n)
            val schedulePagas = schedule.take(parcelasEfetivas)

            // Capital amortizado acumulado (reduz a carteira)
            val capitalAmortizadoAteAgora = schedulePagas.sumOf { it.valorAmortizacao }
            // Juros recebidos acumulados (receita financeira)
            val jurosRecebidosAteAgora = schedulePagas.sumOf { it.valorJuros }
            // Total recebido acumulado = Capital Amortizado + Juros Recebidos
            val totalPagoAteAgora = capitalAmortizadoAteAgora + jurosRecebidosAteAgora
            // Saldo da Carteira de Crédito = Capital Inicial - Capital Amortizado
            // NUNCA descontar juros da carteira!
            val saldoDevedorAtual = (principal - capitalAmortizadoAteAgora).coerceAtLeast(0.0)

            val valorTotalLiquidar = principal + totalJuros
            val percentualQuitado = if (principal > 0) {
                (capitalAmortizadoAteAgora / principal) * 100.0
            } else {
                0.0
            }

            return CalculoSaldoDevedorState(
                principal = principal,
                taxaPercentual = taxaPercentual,
                prazoMeses = prazoMeses,
                parcelasPagas = parcelasEfetivas,
                tipoCalculo = tipo,
                capitalMensal = capitalMensalGeral,
                jurosMensal = jurosMensalGeral,
                valorJurosTotal = totalJuros,
                valorTotalLiquidar = valorTotalLiquidar,
                prestacaoMensal = prestacaoMensalRef,
                capitalAmortizadoAteAgora = capitalAmortizadoAteAgora,
                jurosRecebidosAteAgora = jurosRecebidosAteAgora,
                saldoDevedorAtual = saldoDevedorAtual,
                totalPagoAteAgora = totalPagoAteAgora,
                percentualQuitado = percentualQuitado.coerceIn(0.0, 100.0),
                cronograma = schedule
            )
        }
    }
}
