package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.model.CreditoEntity
import com.example.ui.components.KwanzaFormatters

object LembreteHelper {

    /**
     * Modelo 1: Lembrete de Vencimento Antecipado (Antes do prazo / 5 dias antes)
     * Conforme Secção 4.3 do Requisito Oficial
     */
    fun formatMensagemLembreteVencimento(
        nomeCliente: String,
        dataReembolso: String,
        valorAPagar: String
    ): String {
        return "Prezado(a) $nomeCliente, informamos que a sua prestação de crédito no valor de $valorAPagar vence no dia $dataReembolso. Solicitamos a regularização dentro do prazo acordado. Caso já tenha efectuado o pagamento, agradecemos que apresente o comprovativo para actualização do registo. Obrigado."
    }

    /**
     * Modelo 2: Cobrança em Atraso / Solicitação de Comprovativo (Após o Vencimento)
     * Conforme Secção 4.3 do Requisito Oficial
     */
    fun formatMensagemCobrancaAtraso(
        nomeCliente: String,
        dataEmAtraso: String,
        diasEmAtraso: Int = 0,
        valorPrestacao: String = "",
        saldoPendente: String = "",
        saldoPendenteStr: String = ""
    ): String {
        val saldoEfetivo = if (saldoPendenteStr.isNotBlank()) saldoPendenteStr else saldoPendente
        val diasTexto = if (diasEmAtraso > 0) " há $diasEmAtraso dias" else ""
        val prestacaoTexto = if (valorPrestacao.isNotBlank()) " no valor de $valorPrestacao" else ""
        val saldoTexto = if (saldoEfetivo.isNotBlank()) " O saldo pendente calculado pelo sistema é de $saldoEfetivo." else ""
        return "Prezado(a) $nomeCliente, informamos que a sua prestação de crédito$prestacaoTexto com vencimento em $dataEmAtraso encontra-se em atraso$diasTexto.$saldoTexto Solicitamos a regularização com urgência para evitar o acréscimo de juros de mora. Caso já tenha efectuado o pagamento, agradecemos que apresente o comprovativo para actualização do registo. Obrigado."
    }

    /**
     * Modelo 3: Cobrança com Juros de Mora Oficial (4% ao dia sobre o total dos juros contratuais)
     * Conforme Secção 6 do Requisito Oficial
     */
    fun formatMensagemJurosMoraOficial(
        nomeCliente: String,
        diasEmAtraso: Int,
        valorDesembolsado: Double,
        taxaPercentual: Double,
        quantidadePrestacoes: Int,
        saldoDevedorContratual: Double? = null,
        prestacoesPagas: Int = 0
    ): String {
        val res = MoraCalculatorHelper.calcular(
            valorDesembolsado = valorDesembolsado,
            taxaPercentual = taxaPercentual,
            quantidadePrestacoes = quantidadePrestacoes,
            diasEmAtraso = diasEmAtraso,
            saldoDevedorContratual = saldoDevedorContratual,
            prestacoesPagas = prestacoesPagas
        )

        val diasTexto = "${res.diasEmAtraso} ${if (res.diasEmAtraso == 1) "dia" else "dias"}"
        val moraDiariaFormatada = KwanzaFormatters.formatAOA(res.jurosMoraDiarios, showDecimals = true)
        val moraAcumuladaFormatada = KwanzaFormatters.formatAOA(res.jurosMoraAcumulados, showDecimals = true)
        val saldoContratualFormatado = KwanzaFormatters.formatAOA(res.saldoDevedorContratual, showDecimals = true)
        val totalFormatado = KwanzaFormatters.formatAOA(res.valorTotalEmDivida, showDecimals = true)

        return "Prezado(a) $nomeCliente, informamos que o seu crédito encontra-se em atraso há $diasTexto. Em conformidade com o contrato, aplicam-se juros de mora diários de 4% sobre o total dos juros contratuais ($moraDiariaFormatada/dia), totalizando $moraAcumuladaFormatada de juros de mora acumulados. Saldo contratual em dívida: $saldoContratualFormatado. Valor total atual em dívida: $totalFormatado. Solicitamos a regularização imediata ou apresentação do comprovativo. Obrigado."
    }

    /**
     * Modelo 3 (Sobrecarga direta aceitando CreditoEntity)
     */
    fun formatMensagemJurosMoraOficial(
        nomeCliente: String,
        credito: CreditoEntity,
        diasAtrasoManual: Int? = null,
        valorEmAtrasoManual: Double? = null,
        contactoInstituicao: String = "+244 923 000 000"
    ): String {
        val res = MoraCalculatorHelper.calcular(
            credito = credito,
            diasAtrasoManual = diasAtrasoManual
        )
        return formatMensagemJurosMoraOficial(
            nomeCliente = nomeCliente,
            diasEmAtraso = res.diasEmAtraso,
            valorDesembolsado = res.valorDesembolsado,
            taxaPercentual = res.taxaPercentual,
            quantidadePrestacoes = res.quantidadePrestacoes,
            saldoDevedorContratual = res.saldoDevedorContratual,
            prestacoesPagas = credito.prestacoesPagas
        )
    }

    /**
     * Modelo 3 (Sobrecarga de compatibilidade para chamadas simplificadas)
     */
    fun formatMensagemJurosMora(
        nomeCliente: String,
        diasEmAtraso: Int,
        prestacaoMensal: Double
    ): String {
        val dias = diasEmAtraso.coerceAtLeast(1)
        val jurosMora = prestacaoMensal * 0.04 * dias
        val totalAPagar = prestacaoMensal + jurosMora

        val diasTexto = "$dias ${if (dias == 1) "dia" else "dias"}"
        val jurosMoraFormatado = KwanzaFormatters.formatAOA(jurosMora)
        val prestacaoFormatada = KwanzaFormatters.formatAOA(prestacaoMensal)
        val totalFormatado = KwanzaFormatters.formatAOA(totalAPagar)

        return "Prezado(a) $nomeCliente, em função do seu atraso de $diasTexto pagará juros de mora de 4%/dia no valor de $jurosMoraFormatado adicionado à prestação de $prestacaoFormatada, total a pagar: $totalFormatado. Solicitamos a regularização urgente. Melhores cumprimentos."
    }

    fun formatMensagemProximoVencimento(dataPagamento: String): String {
        return "Saudações prezado(a) Cliente, serve para lembrar que a sua prestação vence dia $dataPagamento. Pague a tempo, evitar pagar com multas. A Mbuli-Microcredito agradece!"
    }

    fun formatMensagemPagamentoVencido(dataPagamentoAtrasado: String): String {
        return "Prezado(a) Cliente, não identificamos o seu pagamento da sua dívida referenta a $dataPagamentoAtrasado. Se já o fez envia-os o comprovante para a verificação, caso não faço-o já, evita processos judicias com a instituição. A Mbuli-Microcredito agradece!"
    }

    fun formatAngolaPhoneNumber(phone: String): String {
        val clean = phone.replace("[^0-9]".toRegex(), "")
        return when {
            clean.startsWith("244") -> clean
            clean.length == 9 -> "244$clean"
            clean.length > 9 && clean.startsWith("00244") -> clean.removePrefix("00")
            else -> clean
        }
    }

    fun enviarWhatsApp(context: Context, telefone: String, mensagem: String) {
        val formattedPhone = formatAngolaPhoneNumber(telefone)
        if (formattedPhone.isBlank()) {
            Toast.makeText(context, "Telefone inválido ou não informado.", Toast.LENGTH_SHORT).show()
            return
        }

        val encodedText = Uri.encode(mensagem)
        val waDirectUri = Uri.parse("whatsapp://send?phone=$formattedPhone&text=$encodedText")

        // 1. Tentar abrir o WhatsApp diretamente offline via URI scheme nativo
        val directIntent = Intent(Intent.ACTION_VIEW, waDirectUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            // Tenta forçar o pacote do WhatsApp principal
            directIntent.setPackage("com.whatsapp")
            context.startActivity(directIntent)
            return
        } catch (_: Exception) {
            // Continua para tentar WhatsApp Business
        }

        try {
            // Tenta WhatsApp Business
            val waBusinessIntent = Intent(Intent.ACTION_VIEW, waDirectUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                setPackage("com.whatsapp.w4b")
            }
            context.startActivity(waBusinessIntent)
            return
        } catch (_: Exception) {
            // Continua para tentar sem especificar pacote
        }

        try {
            // Tenta abrir qualquer cliente compatível com whatsapp://
            val genericWaIntent = Intent(Intent.ACTION_VIEW, waDirectUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(genericWaIntent)
            return
        } catch (_: Exception) {
            // Continua para fallback via web
        }

        try {
            // Fallback via URL universal (caso navegador tenha mapeamento)
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=$encodedText")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val waUri = Uri.parse("https://wa.me/$formattedPhone?text=$encodedText")
                val waIntent = Intent(Intent.ACTION_VIEW, waUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(waIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "WhatsApp não encontrado. Abrindo envio via SMS Offline...", Toast.LENGTH_SHORT).show()
                enviarSms(context, telefone, mensagem)
            }
        }
    }

    fun enviarSms(context: Context, telefone: String, mensagem: String) {
        val cleanPhone = telefone.replace("[^0-9+]".toRegex(), "")
        if (cleanPhone.isBlank()) {
            Toast.makeText(context, "Telefone inválido ou não informado.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$cleanPhone")).apply {
                putExtra("sms_body", mensagem)
                putExtra(Intent.EXTRA_TEXT, mensagem)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intentFallback = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("sms:$cleanPhone")
                    putExtra("sms_body", mensagem)
                    putExtra(Intent.EXTRA_TEXT, mensagem)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intentFallback)
            } catch (err: Exception) {
                Toast.makeText(context, "Erro ao abrir aplicativo de SMS.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun ligarCliente(context: Context, telefone: String) {
        val cleanPhone = telefone.replace("[^0-9+]".toRegex(), "")
        if (cleanPhone.isBlank()) {
            Toast.makeText(context, "Telefone inválido ou não informado.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao abrir discador telefónico.", Toast.LENGTH_SHORT).show()
        }
    }
}

