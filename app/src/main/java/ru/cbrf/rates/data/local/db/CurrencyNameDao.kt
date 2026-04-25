package ru.cbrf.rates.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CurrencyNameDao {
    @Query("SELECT * FROM currency_names")
    suspend fun getAll(): List<CurrencyNameEntity>

    @Query("SELECT charCode FROM currency_names")
    suspend fun getAllCharCodes(): List<String>

    @Query("SELECT COUNT(*) FROM currency_names")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(names: List<CurrencyNameEntity>)
}
