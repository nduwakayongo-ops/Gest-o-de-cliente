package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clientes")
data class ClienteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val numeroInterno: String,
    val nome: String,
    val telefone: String,
    val sexo: String = "M", // "M" or "F"
    val provincia: String = "Luanda",
    val municipio: String = "Luanda",
    val comuna: String = "Ingombota",
    val endereco: String = "",
    val banco: String = "BAI",
    val numeroConta: String = "",
    val iban: String = "",
    val nif: String = "",
    val empresa: String = "",
    val salario: Double = 0.0,
    val fotoUri: String? = null,
    val documentoUri: String? = null,
    val observacoes: String = "",
    val dataCriacao: Long = System.currentTimeMillis()
)

@Entity(tableName = "creditos")
data class CreditoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clienteId: Long,
    val valorDesembolsado: Double,
    val valorLiquido: Double = 0.0,
    val taxaPercentual: Double,
    val valorJuros: Double = 0.0,
    val prestacaoMensal: Double,
    val quantidadePrestacoes: Int,
    val prestacoesPagas: Int = 0,
    val dataDesembolso: Long = 0L, // timestamp
    val dataPrimeiroPagamento: Long = 0L,
    val dataUltimoPagamento: Long = 0L,
    val diasCredito: Int = 0,
    val estado: String = "Ativo", // Ativo, Liquidado, Renegociado, Em atraso, Cancelado
    val proximoVencimento: Long = 0L,
    val valorEmAtraso: Double = 0.0,
    val diasEmAtraso: Int = 0
)

@Entity(tableName = "pagamentos")
data class PagamentoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val creditoId: Long,
    val clienteId: Long,
    val valorPago: Double,
    val dataPagamento: Long = System.currentTimeMillis(),
    val numeroPrestacao: Int,
    val comprovativoRef: String = "",
    val observacao: String = ""
)

@Entity(tableName = "cobrancas")
data class CobrancaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clienteId: Long,
    val dataContato: Long = System.currentTimeMillis(),
    val tipoContato: String = "Chamada", // Chamada, WhatsApp, Visita, Email
    val promessaPagamentoData: Long? = null,
    val observacao: String = "",
    val oficialResponsavel: String = "Nduwa Kayongo"
)

@Entity(tableName = "configuracoes")
data class ConfiguracaoEntity(
    @PrimaryKey val id: Int = 1,
    val pinSeguranca: String? = null,
    val biometriaAtiva: Boolean = false,
    val horarioNotificacao: String = "08:00",
    val diasAntecedenciaNotificacao: Int = 5,
    val moeda: String = "AOA",
    val tema: String = "Claro", // Claro, Escuro, Automatico
    val backupAutomatico: Boolean = true,
    val fotoPerfilUri: String? = null
)
