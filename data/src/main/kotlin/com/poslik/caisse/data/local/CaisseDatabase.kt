package com.poslik.caisse.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.poslik.caisse.domain.model.PrintStatus

@Database(
    entities = [RegisterConfigEntity::class, SaleEntity::class, SaleLineEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class CaisseDatabase : RoomDatabase() {
    abstract fun registerDao(): RegisterDao

    abstract fun saleDao(): SaleDao

    companion object {
        const val NAME = "caisse.db"
    }
}

class Converters {
    @TypeConverter
    fun printStatusToString(status: PrintStatus): String = status.name

    @TypeConverter
    fun stringToPrintStatus(value: String): PrintStatus = PrintStatus.valueOf(value)
}
