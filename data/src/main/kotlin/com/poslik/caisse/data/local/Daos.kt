package com.poslik.caisse.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.poslik.caisse.domain.model.PrintStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface RegisterDao {
    @Query("SELECT * FROM register_config WHERE id = 1")
    suspend fun get(): RegisterConfigEntity?

    @Query("SELECT register_code FROM register_config WHERE id = 1")
    fun observeCode(): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(config: RegisterConfigEntity)

    @Query("UPDATE register_config SET last_number = :lastNumber WHERE id = 1")
    suspend fun updateLastNumber(lastNumber: Long): Int
}

@Dao
interface SaleDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSale(sale: SaleEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLines(lines: List<SaleLineEntity>)

    @Transaction
    @Query("SELECT * FROM sales WHERE sale_id = :saleId")
    suspend fun get(saleId: String): SaleWithLines?

    @Transaction
    @Query("SELECT * FROM sales ORDER BY ticket_number DESC")
    fun observeAll(): Flow<List<SaleWithLines>>

    @Transaction
    @Query("SELECT * FROM sales WHERE print_status IN ('PENDING', 'FAILED') ORDER BY ticket_number")
    suspend fun salesToPrint(): List<SaleWithLines>

    @Query(
        """
        UPDATE sales
        SET print_status = :status,
            last_print_error = :error,
            print_attempts = print_attempts + :attemptIncrement,
            version = version + 1
        WHERE sale_id = :saleId
        """,
    )
    suspend fun updatePrintStatus(saleId: String, status: PrintStatus, error: String?, attemptIncrement: Int): Int

    @Query("SELECT COUNT(*) FROM sales WHERE synced_version < version")
    fun observeUnsyncedCount(): Flow<Int>

    @Transaction
    @Query("SELECT * FROM sales WHERE synced_version < version AND sync_conflict = 0 ORDER BY ticket_number LIMIT :limit")
    suspend fun unsynced(limit: Int): List<SaleWithLines>

    /** Ne marque que si la vente n'a pas changé depuis la lecture : 0 ligne modifiée sinon. */
    @Query("UPDATE sales SET synced_version = :version WHERE sale_id = :saleId AND version = :version")
    suspend fun markSynced(saleId: String, version: Long): Int

    @Query("UPDATE sales SET sync_conflict = 1 WHERE sale_id = :saleId")
    suspend fun markConflict(saleId: String)
}
