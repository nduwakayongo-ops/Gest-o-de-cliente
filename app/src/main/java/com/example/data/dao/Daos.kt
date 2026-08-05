package com.example.data.dao

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ClienteDao {
    @Query("SELECT * FROM clientes ORDER BY nome ASC")
    fun getAllClientesFlow(): Flow<List<ClienteEntity>>

    @Query("SELECT * FROM clientes ORDER BY nome ASC")
    suspend fun getAllClientes(): List<ClienteEntity>

    @Query("SELECT * FROM clientes WHERE id = :id")
    suspend fun getClienteById(id: Long): ClienteEntity?

    @Query("SELECT * FROM clientes WHERE id = :id")
    fun getClienteByIdFlow(id: Long): Flow<ClienteEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCliente(cliente: ClienteEntity): Long

    @Update
    suspend fun updateCliente(cliente: ClienteEntity)

    @Delete
    suspend fun deleteCliente(cliente: ClienteEntity)

    @Query("SELECT COUNT(*) FROM clientes")
    fun getCountClientesFlow(): Flow<Int>
}

@Dao
interface CreditoDao {
    @Query("SELECT * FROM creditos")
    fun getAllCreditosFlow(): Flow<List<CreditoEntity>>

    @Query("SELECT * FROM creditos")
    suspend fun getAllCreditos(): List<CreditoEntity>

    @Query("SELECT * FROM creditos WHERE clienteId = :clienteId")
    fun getCreditosByClienteFlow(clienteId: Long): Flow<List<CreditoEntity>>

    @Query("SELECT * FROM creditos WHERE id = :id")
    suspend fun getCreditoById(id: Long): CreditoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCredito(credito: CreditoEntity): Long

    @Update
    suspend fun updateCredito(credito: CreditoEntity)

    @Delete
    suspend fun deleteCredito(credito: CreditoEntity)
}

@Dao
interface PagamentoDao {
    @Query("SELECT * FROM pagamentos ORDER BY dataPagamento DESC")
    fun getAllPagamentosFlow(): Flow<List<PagamentoEntity>>

    @Query("SELECT * FROM pagamentos WHERE clienteId = :clienteId ORDER BY dataPagamento DESC")
    fun getPagamentosByClienteFlow(clienteId: Long): Flow<List<PagamentoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPagamento(pagamento: PagamentoEntity): Long

    @Delete
    suspend fun deletePagamento(pagamento: PagamentoEntity)
}

@Dao
interface CobrancaDao {
    @Query("SELECT * FROM cobrancas WHERE clienteId = :clienteId ORDER BY dataContato DESC")
    fun getCobrancasByClienteFlow(clienteId: Long): Flow<List<CobrancaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCobranca(cobranca: CobrancaEntity): Long

    @Delete
    suspend fun deleteCobranca(cobranca: CobrancaEntity)
}

@Dao
interface ConfiguracaoDao {
    @Query("SELECT * FROM configuracoes WHERE id = 1")
    fun getConfiguracaoFlow(): Flow<ConfiguracaoEntity?>

    @Query("SELECT * FROM configuracoes WHERE id = 1")
    suspend fun getConfiguracao(): ConfiguracaoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateConfiguracao(config: ConfiguracaoEntity)
}
