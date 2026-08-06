package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.*
import kotlin.math.pow

enum class TipoCalculoJuros(val label: String, val descricao: String) {
    COMPOSTO_PRICE("Tabela Price (Francês)", "Prestações fixas iguais, juros decrescentes"),
    SIMPLES("Juros Simples Directo", "Juros fixos calculados sobre o capital inicial"),
    SAC("Sistema SAC (Amortização Constante)", "Amortização fixa do capital, prestações decrescentes")
}

data class ItemParcelaSchedule(
    val numeroParcela: Int,
    val valorPrestacao: Double,
    val valorJuros: Double,
    val valorAmortizacao: Double,
    val saldoDevedorRestante: Double,
    val isPaga: Boolean = false
)

data class CalculoSaldoDevedorState(
    val principal: Double = 0.0,
    val taxaPercentual: Double = 0.0,
    val prazoMeses: Int = 0,
    val parcelasPagas: Int = 0,
    val tipoCalculo: TipoCalculoJuros = TipoCalculoJuros.COMPOSTO_PRICE,
    val valorJurosTotal: Double = 0.0,
    val valorTotalLiquidar: Double = 0.0,
    val prestacaoMensal: Double = 0.0,
    val saldoDevedorAtual: Double = 0.0,
    val totalPagoAteAgora: Double = 0.0,
    val percentualQuitado: Double = 0.0,
    val cronograma: List<ItemParcelaSchedule> = emptyList()
)

class CalculadoraViewModel : ViewModel() {

    // Input States
    private val _valorDesembolsadoInput = MutableStateFlow("1500000")
    val valorDesembolsadoInput: StateFlow<String> = _valorDesembolsadoInput.asStateFlow()

    private val _taxaJurosInput = MutableStateFlow("5.0")
    val taxaJurosInput: StateFlow<String> = _taxaJurosInput.asStateFlow()

    private val _prazoMesesInput = MutableStateFlow("12")
    val prazoMesesInput: StateFlow<String> = _prazoMesesInput.asStateFlow()

    private val _parcelasPagasInput = MutableStateFlow("3")
    val parcelasPagasInput: StateFlow<String> = _parcelasPagasInput.asStateFlow()

    private val _tipoCalculo = MutableStateFlow(TipoCalculoJuros.COMPOSTO_PRICE)
    val tipoCalculo: StateFlow<TipoCalculoJuros> = _tipoCalculo.asStateFlow()

    // Combined Reactive Calculated State
    val calculoState: StateFlow<CalculoSaldoDevedorState> = combine(
        _valorDesembolsadoInput,
        _taxaJurosInput,
        _prazoMesesInput,
        _parcelasPagasInput,
        _tipoCalculo
    ) { valDes, taxa, prazo, pagas, tipo ->
        val principal = valDes.toDoubleOrNull() ?: 0.0
        val taxaPct = taxa.toDoubleOrNull() ?: 0.0
        val nMeses = (prazo.toIntOrNull() ?: 12).coerceAtLeast(1)
        val nPagas = (pagas.toIntOrNull() ?: 0).coerceIn(0, nMeses)

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

            var saldoDevedor = principal
            var totalJuros = 0.0
            var prestacaoMensalRef = 0.0

            when (tipo) {
                TipoCalculoJuros.COMPOSTO_PRICE -> {
                    // Formula Price: PMT = P * [i * (1+i)^n] / [(1+i)^n - 1]
                    val pmt = if (i > 0) {
                        val factor = (1 + i).pow(n)
                        principal * (i * factor) / (factor - 1)
                    } else {
                        principal / n
                    }
                    prestacaoMensalRef = pmt

                    for (m in 1..n) {
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
                                isPaga = m <= parcelasPagas
                            )
                        )
                    }
                }

                TipoCalculoJuros.SIMPLES -> {
                    // Simple interest: Total Juros = P * i * (n/12 or n) -> here monthly rate i
                    totalJuros = principal * i
                    val valorTotal = principal + totalJuros
                    val pmt = valorTotal / n
                    prestacaoMensalRef = pmt

                    val amortizacaoMensal = principal / n
                    val jurosMensal = totalJuros / n

                    for (m in 1..n) {
                        saldoDevedor = (saldoDevedor - amortizacaoMensal).coerceAtLeast(0.0)
                        schedule.add(
                            ItemParcelaSchedule(
                                numeroParcela = m,
                                valorPrestacao = pmt,
                                valorJuros = jurosMensal,
                                valorAmortizacao = amortizacaoMensal,
                                saldoDevedorRestante = if (m == n) 0.0 else saldoDevedor,
                                isPaga = m <= parcelasPagas
                            )
                        )
                    }
                }

                TipoCalculoJuros.SAC -> {
                    // SAC: Amortizacao Constante A = P / n
                    val amortizacaoConstante = principal / n

                    for (m in 1..n) {
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
                                isPaga = m <= parcelasPagas
                            )
                        )
                    }
                }
            }

            val valorTotalLiquidar = principal + totalJuros
            val parcelasEfetivas = parcelasPagas.coerceIn(0, n)
            val scheduleAteAgora = schedule.take(parcelasEfetivas)
            val totalPagoAteAgora = scheduleAteAgora.sumOf { it.valorPrestacao }
            
            val saldoDevedorAtual = if (parcelasEfetivas == 0) {
                valorTotalLiquidar
            } else if (parcelasEfetivas >= n) {
                0.0
            } else {
                schedule[parcelasEfetivas - 1].saldoDevedorRestante + schedule.drop(parcelasEfetivas).sumOf { it.valorJuros }
            }

            val percentualQuitado = if (valorTotalLiquidar > 0) {
                (totalPagoAteAgora / valorTotalLiquidar) * 100.0
            } else {
                0.0
            }

            return CalculoSaldoDevedorState(
                principal = principal,
                taxaPercentual = taxaPercentual,
                prazoMeses = prazoMeses,
                parcelasPagas = parcelasEfetivas,
                tipoCalculo = tipo,
                valorJurosTotal = totalJuros,
                valorTotalLiquidar = valorTotalLiquidar,
                prestacaoMensal = prestacaoMensalRef,
                saldoDevedorAtual = saldoDevedorAtual.coerceAtLeast(0.0),
                totalPagoAteAgora = totalPagoAteAgora,
                percentualQuitado = percentualQuitado.coerceIn(0.0, 100.0),
                cronograma = schedule
            )
        }
    }
}
