package com.poslik.caisse.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.poslik.caisse.domain.model.PrintStatus

/** Une seule ligne : le code de cette caisse et le dernier numéro de ticket attribué. */
@Entity(tableName = "register_config")
data class RegisterConfigEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    @ColumnInfo(name = "register_code") val registerCode: String,
    @ColumnInfo(name = "last_number") val lastNumber: Long,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}

@Entity(
    tableName = "sales",
    indices = [
        // Garde-fou : même en cas de bug applicatif, SQLite refuse deux ventes sous le même numéro.
        Index(value = ["register_code", "ticket_number"], unique = true),
        Index(value = ["print_status"]),
    ],
)
data class SaleEntity(
    @PrimaryKey @ColumnInfo(name = "sale_id") val saleId: String,
    @ColumnInfo(name = "register_code") val registerCode: String,
    @ColumnInfo(name = "ticket_number") val ticketNumber: Long,
    @ColumnInfo(name = "total_millimes") val totalMillimes: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "print_status") val printStatus: PrintStatus,
    @ColumnInfo(name = "print_attempts") val printAttempts: Int,
    @ColumnInfo(name = "last_print_error") val lastPrintError: String?,
    /** Incrémentée à chaque modification locale. */
    @ColumnInfo(name = "version") val version: Long,
    /** Dernière version acquittée par Firebase ; `synced_version < version` = à envoyer. */
    @ColumnInfo(name = "synced_version") val syncedVersion: Long,
    @ColumnInfo(name = "sync_conflict") val syncConflict: Boolean,
)

@Entity(
    tableName = "sale_lines",
    primaryKeys = ["sale_id", "product_id"],
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["sale_id"],
            childColumns = ["sale_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SaleLineEntity(
    @ColumnInfo(name = "sale_id") val saleId: String,
    @ColumnInfo(name = "product_id") val productId: String,
    @ColumnInfo(name = "position") val position: Int,
    @ColumnInfo(name = "product_name") val productName: String,
    @ColumnInfo(name = "unit_price_millimes") val unitPriceMillimes: Long,
    @ColumnInfo(name = "quantity") val quantity: Int,
)

data class SaleWithLines(
    @Embedded val sale: SaleEntity,
    @Relation(parentColumn = "sale_id", entityColumn = "sale_id")
    val lines: List<SaleLineEntity>,
)
