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
    version = 3,
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
            val configDao = db.configuracaoDao()

            // Default Configuration (no simulated clients or mock data)
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
        }
    }
}
