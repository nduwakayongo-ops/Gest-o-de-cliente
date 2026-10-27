package com.example

import com.example.data.model.CreditoEntity
import com.example.data.model.PagamentoEntity
import com.example.util.EstadoMensagem
import com.example.util.LembreteHelper
import com.example.util.MensagensQueueManager
import com.example.util.MoraCalculatorHelper
import com.example.util.NotificationSchedulerHelper
import com.example.util.TipoCanalMensagem
import com.example.util.WhatsAppCloudApiConfig
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {

  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  // --- REQUISITO 6.1: EXEMPLO OBRIGATÓRIO DE JUROS DE MORA ---
  @Test
  fun testExemploObrigatorioJurosDeMoraSecao6_1() {
    // Dados do crédito:
    // Capital concedido: 500.000,00 Kz
    // Prazo: 3 meses
    // Taxa de juros: 7,5% ao mês
    // Dias em atraso: 45
    val capital = 500000.0
    val prazoMeses = 3
    val taxaMensal = 7.5
    val diasEmAtraso = 45

    val credito = CreditoEntity(
      id = 1L,
      clienteId = 1L,
      valorDesembolsado = capital,
      quantidadePrestacoes = prazoMeses,
      taxaPercentual = taxaMensal,
      valorJuros = capital * (taxaMensal / 100.0) * prazoMeses,
      prestacaoMensal = (capital / prazoMeses) + (capital * (taxaMensal / 100.0)),
      dataDesembolso = System.currentTimeMillis() - 90L * 24 * 3600 * 1000L,
      dataPrimeiroPagamento = System.currentTimeMillis() - 60L * 24 * 3600 * 1000L,
      proximoVencimento = System.currentTimeMillis() - 45L * 24 * 3600 * 1000L,
      estado = "Em atraso",
      diasEmAtraso = diasEmAtraso,
      valorEmAtraso = 0.0,
      prestacoesPagas = 0
    )

    val resultado = MoraCalculatorHelper.calcular(
      credito = credito,
      pagamentos = emptyList(),
      diasAtrasoManual = diasEmAtraso
    )

    // Juros mensais = 500.000 * 7,5% = 37.500,00 Kz
    val jurosMensaisEsperado = 37500.0
    // Total dos juros contratuais: 37.500 * 3 = 112.500,00 Kz
    val totalJurosContratuaisEsperado = 112500.0
    assertEquals(totalJurosContratuaisEsperado, resultado.totalJurosContratuais, 0.001)

    // Taxa diária de mora: 4% do total dos juros contratuais
    // Juros de mora diários: 112.500 * 0,04 = 4.500,00 Kz
    val jurosMoraDiariosEsperado = 4500.0
    assertEquals(jurosMoraDiariosEsperado, resultado.jurosMoraDiarios, 0.001)

    // Dias em atraso: 45
    assertEquals(45, resultado.diasEmAtraso)

    // Juros de mora acumulados: 4.500 * 45 = 202.500,00 Kz
    val jurosMoraAcumuladosEsperado = 202500.0
    assertEquals(jurosMoraAcumuladosEsperado, resultado.jurosMoraAcumulados, 0.001)

    // Saldo devedor contratual sem pagamentos:
    // Capital (500.000) + Total Juros (112.500) = 612.500,00 Kz
    val saldoDevedorContratualEsperado = 612500.0
    assertEquals(saldoDevedorContratualEsperado, resultado.saldoDevedorContratual, 0.001)

    // Valor total em dívida = Saldo devedor contratual + Juros de mora acumulados
    // 612.500,00 + 202.500,00 = 815.000,00 Kz
    val valorTotalEmDividaEsperado = 815000.0
    assertEquals(valorTotalEmDividaEsperado, resultado.valorTotalEmDivida, 0.001)

    // REGRAS OBRIGATÓRIAS (Seção 6.2):
    // 1. NÃO calcular 4% sobre o capital concedido (500.000 * 0.04 = 20.000 != 4.500)
    assertNotEquals(capital * 0.04, resultado.jurosMoraDiarios, 1.0)

    // 2. NÃO calcular 4% sobre a prestação (prestação = 204.166,67; 204.166,67 * 0.04 = 8.166,67 != 4.500)
    assertNotEquals(credito.prestacaoMensal * 0.04, resultado.jurosMoraDiarios, 1.0)

    // 3. NÃO calcular 4% sobre o saldo devedor (612.500 * 0.04 = 24.500 != 4.500)
    assertNotEquals(saldoDevedorContratualEsperado * 0.04, resultado.jurosMoraDiarios, 1.0)
  }

  // --- REQUISITO 5 & 6: PAGAMENTOS E RECALCULO DE SALDO ---
  @Test
  fun testMoraComAmortizacaoParcialPreservaSaldoReal() {
    val capital = 500000.0
    val prazoMeses = 3
    val taxaMensal = 7.5
    val diasEmAtraso = 10

    val credito = CreditoEntity(
      id = 2L,
      clienteId = 1L,
      valorDesembolsado = capital,
      quantidadePrestacoes = prazoMeses,
      taxaPercentual = taxaMensal,
      valorJuros = capital * (taxaMensal / 100.0) * prazoMeses,
      prestacaoMensal = (capital / prazoMeses) + (capital * (taxaMensal / 100.0)),
      dataDesembolso = System.currentTimeMillis() - 90L * 24 * 3600 * 1000L,
      dataPrimeiroPagamento = System.currentTimeMillis() - 60L * 24 * 3600 * 1000L,
      proximoVencimento = System.currentTimeMillis() - 10L * 24 * 3600 * 1000L,
      estado = "Em atraso",
      diasEmAtraso = diasEmAtraso,
      valorEmAtraso = 0.0,
      prestacoesPagas = 1
    )

    // 1 pagamento efetuado de 204.166,67 Kz
    val pag1 = PagamentoEntity(
      id = 101L,
      creditoId = 2L,
      clienteId = 1L,
      numeroPrestacao = 1,
      valorPago = credito.prestacaoMensal,
      dataPagamento = System.currentTimeMillis() - 40L * 24 * 3600 * 1000L,
      comprovativoRef = "REF123",
      observacao = "Transferência"
    )

    val resultado = MoraCalculatorHelper.calcular(
      credito = credito,
      pagamentos = listOf(pag1),
      diasAtrasoManual = diasEmAtraso
    )

    // Juros de mora diários permanecem 4% dos juros contratuais = 4.500 Kz
    assertEquals(4500.0, resultado.jurosMoraDiarios, 0.001)
    // Mora acumulada por 10 dias = 45.000 Kz
    assertEquals(45000.0, resultado.jurosMoraAcumulados, 0.001)

    // Saldo devedor contratual reduz pelo pagamento de 1 prestação:
    // 612.500 - 204.166,67 = 408.333,33 Kz
    val saldoEsperado = (612500.0 - credito.prestacaoMensal)
    assertEquals(saldoEsperado, resultado.saldoDevedorContratual, 0.01)

    // Total em dívida = Saldo devedor restante + Mora acumulada
    val totalEsperado = saldoEsperado + 45000.0
    assertEquals(totalEsperado, resultado.valorTotalEmDivida, 0.01)
  }

  // --- REQUISITO 2.1: LEMBRETE 5 DIAS ANTES DO VENCIMENTO ---
  @Test
  fun testCalculoDiasAteVencimento5DiasExatos() {
    // Simula hoje = 15 de Novembro, Vencimento = 20 de Novembro
    val calVencimento = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 14)
      set(Calendar.MINUTE, 30)
      add(Calendar.DAY_OF_YEAR, 5)
    }

    val diasAteVencimento = NotificationSchedulerHelper.calculateCalendarDaysUntil(calVencimento.timeInMillis)
    assertEquals(5, diasAteVencimento)
  }

  // --- REQUISITOS 4.1 & 4.2: ESTADOS DA FILA DE MENSAGENS E WHATSAPP CLOUD API ---
  @Test
  fun testEstruturaEstadosMensagemEFila() {
    // 6 estados obrigatórios (Seção 4.1):
    val estadosEsperados = listOf(
      EstadoMensagem.PENDENTE,
      EstadoMensagem.AGUARDANDO_REDE,
      EstadoMensagem.ENVIADA,
      EstadoMensagem.ENTREGUE,
      EstadoMensagem.FALHADA,
      EstadoMensagem.CANCELADA
    )
    assertEquals(6, estadosEsperados.size)

    // Canais suportados
    assertTrue(TipoCanalMensagem.values().contains(TipoCanalMensagem.SMS))
    assertTrue(TipoCanalMensagem.values().contains(TipoCanalMensagem.WHATSAPP))

    // WhatsApp Cloud API oficial endpoint (Seção 4.2)
    assertEquals("https://graph.facebook.com/v19.0", WhatsAppCloudApiConfig.API_BASE_URL)
    // Sem credenciais fake hardcoded
    assertFalse(WhatsAppCloudApiConfig.isConfigurado())
  }

  // --- REQUISITO 4.3: MODELOS DE MENSAGENS COM DADOS REAIS ---
  @Test
  fun testMensagemLembreteVencimentoSecao4_3() {
    val msg = LembreteHelper.formatMensagemLembreteVencimento(
      nomeCliente = "Maria António",
      dataReembolso = "20 de Novembro",
      valorAPagar = "204.166,67"
    )
    assertTrue(msg.contains("Prezado(a) Maria António"))
    assertTrue(msg.contains("204.166,67"))
    assertTrue(msg.contains("20 de Novembro"))
    assertTrue(msg.contains("comprovativo para actualização do registo"))
  }

  @Test
  fun testMensagemCobrancaAtrasoSecao4_3() {
    val msg = LembreteHelper.formatMensagemCobrancaAtraso(
      nomeCliente = "João Baptista",
      dataEmAtraso = "15 de Novembro",
      diasEmAtraso = 10,
      saldoPendenteStr = "250.000,00 Kz"
    )
    assertTrue(msg.contains("Prezado(a) João Baptista"))
    assertTrue(msg.contains("15 de Novembro"))
    assertTrue(msg.contains("10 dias"))
    assertTrue(msg.contains("250.000,00 Kz"))
    assertTrue(msg.contains("regularização com urgência"))
  }

  @Test
  fun testMensagemJurosMoraOficialSecao6() {
    val credito = CreditoEntity(
      id = 5L,
      clienteId = 10L,
      valorDesembolsado = 500000.0,
      quantidadePrestacoes = 3,
      taxaPercentual = 7.5,
      valorJuros = 112500.0,
      prestacaoMensal = 204166.67,
      dataDesembolso = 0L,
      dataPrimeiroPagamento = 0L,
      proximoVencimento = 0L,
      estado = "Em atraso",
      diasEmAtraso = 45,
      valorEmAtraso = 0.0,
      prestacoesPagas = 0
    )

    val msg = LembreteHelper.formatMensagemJurosMoraOficial(
      nomeCliente = "António Santos",
      credito = credito,
      diasAtrasoManual = 45
    )

    assertTrue(msg.contains("Prezado(a) António Santos"))
    assertTrue(msg.contains("45 dias"))
    assertTrue(msg.contains("4.500,00")) // mora diária
    assertTrue(msg.contains("202.500,00")) // mora acumulada
    assertTrue(msg.contains("815.000,00")) // valor total em dívida
  }

  @Test
  fun testFormatAngolaPhoneNumber() {
    assertEquals("244923456789", LembreteHelper.formatAngolaPhoneNumber("923456789"))
    assertEquals("244923456789", LembreteHelper.formatAngolaPhoneNumber("+244923456789"))
    assertEquals("244923456789", LembreteHelper.formatAngolaPhoneNumber("00244923456789"))
  }

  @Test
  fun testRegraDeOuroCarteiraCreditoExemploObrigatorio() {
    val capital = 500000.0
    val prazo = 12
    val taxa = 7.5

    val state = com.example.ui.viewmodel.CalculadoraViewModel.calcularSaldoDevedorEParcelas(
      principal = capital,
      taxaPercentual = taxa,
      prazoMeses = prazo,
      parcelasPagas = 1,
      tipo = com.example.ui.viewmodel.TipoCalculoJuros.PADRAO_CARTEIRA
    )

    assertEquals(41666.67, state.capitalMensal, 0.01)
    assertEquals(37500.0, state.jurosMensal, 0.01)
    assertEquals(79166.67, state.prestacaoMensal, 0.01)

    assertEquals(41666.67, state.capitalAmortizadoAteAgora, 0.01)
    assertEquals(37500.0, state.jurosRecebidosAteAgora, 0.01)
    assertEquals(79166.67, state.totalPagoAteAgora, 0.01)

    assertEquals(458333.33, state.saldoDevedorAtual, 0.01)

    val reducaoCarteira = capital - state.saldoDevedorAtual
    assertEquals(41666.67, reducaoCarteira, 0.01)
    assertNotEquals(79166.67, reducaoCarteira, 1.0)
  }

  @Test
  fun testEvolucaoCompleta12ParcelasAteLiquidacaoZero() {
    val capital = 500000.0
    val prazo = 12
    val taxa = 7.5

    val state = com.example.ui.viewmodel.CalculadoraViewModel.calcularSaldoDevedorEParcelas(
      principal = capital,
      taxaPercentual = taxa,
      prazoMeses = prazo,
      parcelasPagas = 12,
      tipo = com.example.ui.viewmodel.TipoCalculoJuros.PADRAO_CARTEIRA
    )

    assertEquals(12, state.cronograma.size)

    val somaAmortizacoes = state.cronograma.sumOf { it.valorAmortizacao }
    assertEquals(500000.0, somaAmortizacoes, 0.00001)
    assertEquals(500000.0, state.capitalAmortizadoAteAgora, 0.00001)
    assertEquals(0.0, state.saldoDevedorAtual, 0.00001)
    assertEquals(0.0, state.cronograma.last().saldoDevedorRestante, 0.00001)
    assertEquals(450000.0, state.valorJurosTotal, 0.00001)
    assertEquals(450000.0, state.jurosRecebidosAteAgora, 0.00001)
    assertEquals(950000.0, state.valorTotalLiquidar, 0.00001)
    assertEquals(950000.0, state.totalPagoAteAgora, 0.00001)
  }

  @Test
  fun testSeisRelatoriosPersonalizadosDefinidosECorretos() {
    val tipos = com.example.util.TipoRelatorioPersonalizado.values()
    assertEquals(6, tipos.size)

    assertEquals(1, com.example.util.TipoRelatorioPersonalizado.TODOS_CLIENTES.id)
    assertEquals(2, com.example.util.TipoRelatorioPersonalizado.CARTEIRA_ATIVA.id)
    assertEquals(3, com.example.util.TipoRelatorioPersonalizado.INADIMPLENTES_PENDENCIAS.id)
    assertEquals(4, com.example.util.TipoRelatorioPersonalizado.APENAS_REEMBOLSOS.id)
    assertEquals(5, com.example.util.TipoRelatorioPersonalizado.APENAS_DESEMBOLSOS.id)
    assertEquals(6, com.example.util.TipoRelatorioPersonalizado.GERAL.id)
  }
}
