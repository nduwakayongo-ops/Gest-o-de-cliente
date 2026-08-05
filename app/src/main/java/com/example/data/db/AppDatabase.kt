package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.*
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@Database(
    entities = [
        ClienteEntity::class,
        CreditoEntity::class,
        PagamentoEntity::class,
        CobrancaEntity::class,
        ConfiguracaoEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun clienteDao(): ClienteDao
    abstract fun creditoDao(): CreditoDao
    abstract fun pagamentoDao(): PagamentoDao
    abstract fun cobrancaDao(): CobrancaDao
    abstract fun configuracaoDao(): ConfiguracaoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nduwa_kayongo_credit_db"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDatabase(database)
                    }
                }
            }
        }

        suspend fun populateDatabase(db: AppDatabase) {
            val clienteDao = db.clienteDao()
            val creditoDao = db.creditoDao()
            val pagamentoDao = db.pagamentoDao()
            val cobrancaDao = db.cobrancaDao()
            val configDao = db.configuracaoDao()

            // Default Config
            configDao.insertOrUpdateConfiguracao(
                ConfiguracaoEntity(
                    id = 1,
                    pinSeguranca = null,
                    biometriaAtiva = false,
                    horarioNotificacao = "08:00",
                    diasAntecedenciaNotificacao = 5,
                    moeda = "AOA",
                    tema = "Claro"
                )
            )

            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance()

            // Cliente 1: Isabel Muamuiombo (Vencendo em 5 dias)
            cal.timeInMillis = now
            cal.add(Calendar.DAY_OF_YEAR, 5)
            val vencimentoIsabel = cal.timeInMillis

            val id1 = clienteDao.insertCliente(
                ClienteEntity(
                    numeroInterno = "CLI-1001",
                    nome = "Isabel Muamuiombo",
                    telefone = "+244 923 456 789",
                    sexo = "F",
                    provincia = "Luanda",
                    municipio = "Viana",
                    comuna = "Zango",
                    endereco = "Rua do Zango 3, Casa 14",
                    banco = "Banco Económico",
                    numeroConta = "123456789012",
                    iban = "AO06 0040 0000 1234 5678 9012 3",
                    nif = "541298372",
                    empresa = "Ministério da Educação",
                    salario = 350000.0,
                    observacoes = "Cliente pontual nas parcelas anteriores."
                )
            )

            creditoDao.insertCredito(
                CreditoEntity(
                    clienteId = id1,
                    valorDesembolsado = 1500000.0,
                    valorLiquido = 1425000.0,
                    taxaPercentual = 5.0,
                    valorJuros = 225000.0,
                    prestacaoMensal = 87215.99,
                    quantidadePrestacoes = 20,
                    prestacoesPagas = 6,
                    dataDesembolso = now - (180L * 24 * 3600 * 1000),
                    dataPrimeiroPagamento = now - (150L * 24 * 3600 * 1000),
                    dataUltimoPagamento = now - (30L * 24 * 3600 * 1000),
                    diasCredito = 600,
                    estado = "Ativo",
                    proximoVencimento = vencimentoIsabel,
                    valorEmAtraso = 0.0,
                    diasEmAtraso = 0
                )
            )

            // Cliente 2: Mateus Agostinho (Em Atraso - 16 dias)
            cal.timeInMillis = now
            cal.add(Calendar.DAY_OF_YEAR, -16)
            val vencimentoMateus = cal.timeInMillis

            val id2 = clienteDao.insertCliente(
                ClienteEntity(
                    numeroInterno = "CLI-1002",
                    nome = "Mateus Agostinho",
                    telefone = "+244 912 345 678",
                    sexo = "M",
                    provincia = "Luanda",
                    municipio = "Belas",
                    comuna = "Talatona",
                    endereco = "Via A1, Condomínio Girassol",
                    banco = "BAI",
                    numeroConta = "987654321098",
                    iban = "AO06 0045 0000 9876 5432 1098 7",
                    nif = "109823741",
                    empresa = "Sonangol E.P.",
                    salario = 650000.0,
                    observacoes = "Solicitou adiamento da prestação por atraso salarial."
                )
            )

            creditoDao.insertCredito(
                CreditoEntity(
                    clienteId = id2,
                    valorDesembolsado = 2800000.0,
                    valorLiquido = 2660000.0,
                    taxaPercentual = 4.5,
                    valorJuros = 378000.0,
                    prestacaoMensal = 145000.00,
                    quantidadePrestacoes = 22,
                    prestacoesPagas = 8,
                    dataDesembolso = now - (240L * 24 * 3600 * 1000),
                    dataPrimeiroPagamento = now - (210L * 24 * 3600 * 1000),
                    dataUltimoPagamento = now - (46L * 24 * 3600 * 1000),
                    diasCredito = 660,
                    estado = "Em atraso",
                    proximoVencimento = vencimentoMateus,
                    valorEmAtraso = 145000.00,
                    diasEmAtraso = 16
                )
            )

            cobrancaDao.insertCobranca(
                CobrancaEntity(
                    clienteId = id2,
                    dataContato = now - (2L * 24 * 3600 * 1000),
                    tipoContato = "WhatsApp",
                    promessaPagamentoData = now + (3L * 24 * 3600 * 1000),
                    observacao = "Cliente confirmou pagamento até próxima sexta-feira.",
                    oficialResponsavel = "Nduwa Kayongo"
                )
            )

            // Cliente 3: Domingos Neto (Em Atraso - 32 dias)
            cal.timeInMillis = now
            cal.add(Calendar.DAY_OF_YEAR, -32)
            val vencimentoDomingos = cal.timeInMillis

            val id3 = clienteDao.insertCliente(
                ClienteEntity(
                    numeroInterno = "CLI-1003",
                    nome = "Domingos Neto",
                    telefone = "+244 934 112 233",
                    sexo = "M",
                    provincia = "Benguela",
                    municipio = "Lobito",
                    comuna = "Restinga",
                    endereco = "Av. da Independência, nº 45",
                    banco = "BFA",
                    numeroConta = "554433221100",
                    iban = "AO06 0006 0000 5544 3322 1100 9",
                    nif = "223344551",
                    empresa = "Porto do Lobito",
                    salario = 420000.0,
                    observacoes = "Contato realizado com cobrança via visita."
                )
            )

            creditoDao.insertCredito(
                CreditoEntity(
                    clienteId = id3,
                    valorDesembolsado = 3500000.0,
                    valorLiquido = 3325000.0,
                    taxaPercentual = 5.0,
                    valorJuros = 525000.0,
                    prestacaoMensal = 185000.00,
                    quantidadePrestacoes = 24,
                    prestacoesPagas = 10,
                    dataDesembolso = now - (330L * 24 * 3600 * 1000),
                    dataPrimeiroPagamento = now - (300L * 24 * 3600 * 1000),
                    dataUltimoPagamento = now - (62L * 24 * 3600 * 1000),
                    diasCredito = 720,
                    estado = "Em atraso",
                    proximoVencimento = vencimentoDomingos,
                    valorEmAtraso = 370000.00, // 2 parcelas em atraso
                    diasEmAtraso = 32
                )
            )

            // Cliente 4: Teresa Kiala (Vencendo Hoje)
            cal.timeInMillis = now
            val vencimentoTeresa = cal.timeInMillis

            val id4 = clienteDao.insertCliente(
                ClienteEntity(
                    numeroInterno = "CLI-1004",
                    nome = "Teresa Kiala",
                    telefone = "+244 945 667 889",
                    sexo = "F",
                    provincia = "Luanda",
                    municipio = "Cazenga",
                    comuna = "Tala Hady",
                    endereco = "Rua do Comércio, nº 88",
                    banco = "Banco Atlântico",
                    numeroConta = "667788990011",
                    iban = "AO06 0055 0000 6677 8899 0011 2",
                    nif = "998877665",
                    empresa = "Hospital Geral de Luanda",
                    salario = 280000.0,
                    observacoes = "Prestação vence hoje."
                )
            )

            creditoDao.insertCredito(
                CreditoEntity(
                    clienteId = id4,
                    valorDesembolsado = 1200000.0,
                    valorLiquido = 1140000.0,
                    taxaPercentual = 4.0,
                    valorJuros = 144000.0,
                    prestacaoMensal = 72000.00,
                    quantidadePrestacoes = 18,
                    prestacoesPagas = 5,
                    dataDesembolso = now - (150L * 24 * 3600 * 1000),
                    dataPrimeiroPagamento = now - (120L * 24 * 3600 * 1000),
                    dataUltimoPagamento = now - (30L * 24 * 3600 * 1000),
                    diasCredito = 540,
                    estado = "Ativo",
                    proximoVencimento = vencimentoTeresa,
                    valorEmAtraso = 0.0,
                    diasEmAtraso = 0
                )
            )

            // Cliente 5: António Silva (Liquidado)
            val id5 = clienteDao.insertCliente(
                ClienteEntity(
                    numeroInterno = "CLI-1005",
                    nome = "António Silva",
                    telefone = "+244 921 998 877",
                    sexo = "M",
                    provincia = "Huíla",
                    municipio = "Lubango",
                    comuna = "Comercial",
                    endereco = "Rua da Se, nº 12",
                    banco = "BIC",
                    numeroConta = "112233445566",
                    iban = "AO06 0051 0000 1122 3344 5566 4",
                    nif = "332211445",
                    empresa = "Governo Provincial da Huíla",
                    salario = 500000.0,
                    observacoes = "Crédito totalmente quitado com antecedência."
                )
            )

            creditoDao.insertCredito(
                CreditoEntity(
                    clienteId = id5,
                    valorDesembolsado = 2000000.0,
                    valorLiquido = 1900000.0,
                    taxaPercentual = 5.0,
                    valorJuros = 250000.0,
                    prestacaoMensal = 125000.00,
                    quantidadePrestacoes = 18,
                    prestacoesPagas = 18,
                    dataDesembolso = now - (600L * 24 * 3600 * 1000),
                    dataPrimeiroPagamento = now - (570L * 24 * 3600 * 1000),
                    dataUltimoPagamento = now - (30L * 24 * 3600 * 1000),
                    diasCredito = 540,
                    estado = "Liquidado",
                    proximoVencimento = 0L,
                    valorEmAtraso = 0.0,
                    diasEmAtraso = 0
                )
            )

            pagamentoDao.insertPagamento(
                PagamentoEntity(
                    creditoId = 1,
                    clienteId = id1,
                    valorPago = 87215.99,
                    dataPagamento = now - (30L * 24 * 3600 * 1000),
                    numeroPrestacao = 6,
                    comprovativoRef = "REC-2026-0012",
                    observacao = "Pagamento via transferência bancária."
                )
            )

            // Populate additional clients to total 12 representative active clients in local database
            val names = listOf(
                "Maria Fernandes", "João Baptista", "Ana Paula Coxe", "Fernando Manuel",
                "Rosa Francisco", "Carlos Eduardo", "Graça Miguel"
            )
            val banks = listOf("BAI", "BFA", "Banco Económico", "Banco Atlântico", "Standard Bank")
            val empresas = listOf("ENDE", "EPAL", "TAAG", "Unitel", "Movicel", "Angola Telecom")

            names.forEachIndexed { idx, name ->
                cal.timeInMillis = now
                cal.add(Calendar.DAY_OF_YEAR, (idx % 8) - 2)
                val venc = cal.timeInMillis

                val cId = clienteDao.insertCliente(
                    ClienteEntity(
                        numeroInterno = "CLI-10${6 + idx}",
                        nome = name,
                        telefone = "+244 92${idx} 112 334",
                        sexo = if (idx % 2 == 0) "F" else "M",
                        provincia = "Luanda",
                        banco = banks[idx % banks.size],
                        empresa = empresas[idx % empresas.size],
                        salario = 300000.0 + (idx * 50000)
                    )
                )

                val valDes = 1000000.0 + (idx * 300000)
                val isAtraso = idx == 2
                val estado = if (isAtraso) "Em atraso" else "Ativo"

                creditoDao.insertCredito(
                    CreditoEntity(
                        clienteId = cId,
                        valorDesembolsado = valDes,
                        valorLiquido = valDes * 0.95,
                        taxaPercentual = 5.0,
                        valorJuros = valDes * 0.1,
                        prestacaoMensal = valDes / 12,
                        quantidadePrestacoes = 12,
                        prestacoesPagas = 3 + idx,
                        dataDesembolso = now - (120L * 24 * 3600 * 1000),
                        dataPrimeiroPagamento = now - (90L * 24 * 3600 * 1000),
                        dataUltimoPagamento = now - (30L * 24 * 3600 * 1000),
                        diasCredito = 360,
                        estado = estado,
                        proximoVencimento = venc,
                        valorEmAtraso = if (isAtraso) 250000.0 else 0.0,
                        diasEmAtraso = if (isAtraso) 14 else 0
                    )
                )
            }
        }
    }
}
