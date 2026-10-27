package com.example.util

import com.example.data.model.CreditoEntity
import com.example.data.model.PagamentoEntity

/**
 * Resultado estruturado do cálculo oficial de juros de mora diários.
 */
data class CalculoMoraResultado(
    val valorDesembolsado: Double,
    val taxaPercentual: Double,
    val quantidadePrestacoes: Int,
    val jurosMensais: Double,
    val totalJurosContratuais: Double,
    val taxaDiariaMora: Double = 0.04, // 4% ao dia sobre o total dos juros contratuais
    val jurosMoraDiarios: Double,
    val diasEmAtraso: Int,
    val jurosMoraAcumulados: Double,
    val saldoDevedorContratual: Double,
    val valorTotalEmDivida: Double
)

/**
 * Calculador Oficial de Juros de Mora Diários.
 *
 * Regra de Negócio Obrigatória:
 * 1. Juros de mora diários = Total dos juros contratuais × 4%
 * 2. Juros de mora acumulados = Juros de mora diários × Número de dias em atraso
 * 3. Valor total em dívida = Saldo devedor contratual + Juros de mora acumulados
 *
 * Exemplo de Validação Obrigatório (Seção 6.1):
 * - Capital concedido: 500.000,00 Kz
 * - Prazo: 3 meses
 * - Taxa de juros: 7,5% ao mês
 * - Juros mensais: 500.000,00 × 7,5% = 37.500,00 Kz
 * - Total dos juros contratuais: 37.500,00 × 3 = 112.500,00 Kz
 * - Juros de mora diários: 112.500,00 × 0,04 = 4.500,00 Kz
 * - Dias em atraso: 45
 * - Juros de mora acumulados: 4.500,00 × 45 = 202.500,00 Kz
 * - Saldo devedor contratual (sem amortização): 612.500,00 Kz
 * - Valor total em dívida: 612.500,00 + 202.500,00 = 815.000,00 Kz
 */
object MoraCalculatorHelper {

    fun calcular(
        valorDesembolsado: Double,
        taxaPercentual: Double,
        quantidadePrestacoes: Int,
        diasEmAtraso: Int,
        saldoDevedorContratual: Double? = null,
        prestacoesPagas: Int = 0
    ): CalculoMoraResultado {
        val dias = diasEmAtraso.coerceAtLeast(0)
        val prazo = quantidadePrestacoes.coerceAtLeast(1)

        // 1. Juros contratuais mensais e total dos juros contratuais
        val jurosMensais = valorDesembolsado * (taxaPercentual / 100.0)
        val totalJurosContratuais = jurosMensais * prazo

        // 2. Taxa diária de mora: 4% sobre o total dos juros contratuais
        val taxaDiaria = 0.04
        val jurosMoraDiarios = totalJurosContratuais * taxaDiaria

        // 3. Juros de mora acumulados simples (não compostos)
        val jurosMoraAcumulados = jurosMoraDiarios * dias

        // 4. Saldo devedor contratual (preservando amortizações e pagamentos reais)
        val saldoContratualReal = if (saldoDevedorContratual != null) {
            saldoDevedorContratual.coerceAtLeast(0.0)
        } else {
            val prestacaoMensal = (valorDesembolsado / prazo) + jurosMensais
            val totalContratual = prestacaoMensal * prazo
            val totalPago = prestacaoMensal * prestacoesPagas.coerceIn(0, prazo)
            (totalContratual - totalPago).coerceAtLeast(0.0)
        }

        // 5. Valor total em dívida
        val valorTotalEmDivida = saldoContratualReal + jurosMoraAcumulados

        return CalculoMoraResultado(
            valorDesembolsado = valorDesembolsado,
            taxaPercentual = taxaPercentual,
            quantidadePrestacoes = prazo,
            jurosMensais = jurosMensais,
            totalJurosContratuais = totalJurosContratuais,
            taxaDiariaMora = taxaDiaria,
            jurosMoraDiarios = jurosMoraDiarios,
            diasEmAtraso = dias,
            jurosMoraAcumulados = jurosMoraAcumulados,
            saldoDevedorContratual = saldoContratualReal,
            valorTotalEmDivida = valorTotalEmDivida
        )
    }

    /**
     * Overload direto aceitando CreditoEntity e lista de pagamentos.
     */
    fun calcular(
        credito: CreditoEntity,
        pagamentos: List<PagamentoEntity>? = null,
        diasAtrasoManual: Int? = null
    ): CalculoMoraResultado {
        val dias = diasAtrasoManual ?: if (credito.diasEmAtraso > 0) credito.diasEmAtraso else 0
        val prazo = credito.quantidadePrestacoes.coerceAtLeast(1)

        val totalJurosContratuais = (credito.valorDesembolsado * (credito.taxaPercentual / 100.0)) * prazo
        val totalContratual = credito.valorDesembolsado + totalJurosContratuais
        val totalPago = pagamentos?.sumOf { it.valorPago } ?: (credito.prestacaoMensal * credito.prestacoesPagas.coerceIn(0, prazo))
        val saldoContratual = (totalContratual - totalPago).coerceAtLeast(0.0)

        return calcular(
            valorDesembolsado = credito.valorDesembolsado,
            taxaPercentual = credito.taxaPercentual,
            quantidadePrestacoes = prazo,
            diasEmAtraso = dias,
            saldoDevedorContratual = saldoContratual,
            prestacoesPagas = credito.prestacoesPagas
        )
    }
}
