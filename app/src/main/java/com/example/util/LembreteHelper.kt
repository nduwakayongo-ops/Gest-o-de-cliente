package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object LembreteHelper {

    fun formatMensagemProximoVencimento(dataPagamento: String): String {
        return "Caro cliente seu reembolso é já dia $dataPagamento, pague a tempo evita pagar com multas. Obrigado"
    }

    fun formatMensagemPagamentoVencido(dataPagamentoAtrasado: String): String {
        return "Caro cliente venceu a data do seu pagamento $dataPagamentoAtrasado paga hoje e mantém a relação saudável com a instituição. Obrigado!"
    }

    fun formatAngolaPhoneNumber(phone: String): String {
        val clean = phone.replace("[^0-9]".toRegex(), "")
        return when {
            clean.startsWith("244") -> clean
            clean.length == 9 -> "244$clean"
            else -> clean
        }
    }

    fun enviarWhatsApp(context: Context, telefone: String, mensagem: String) {
        val formattedPhone = formatAngolaPhoneNumber(telefone)
        try {
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(mensagem)}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Não foi possível abrir o WhatsApp. A abrir SMS...", Toast.LENGTH_SHORT).show()
            enviarSms(context, telefone, mensagem)
        }
    }

    fun enviarSms(context: Context, telefone: String, mensagem: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$telefone")).apply {
                putExtra("sms_body", mensagem)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao abrir aplicativo de SMS.", Toast.LENGTH_SHORT).show()
        }
    }

    fun ligarCliente(context: Context, telefone: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$telefone"))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao abrir discador.", Toast.LENGTH_SHORT).show()
        }
    }
}
