package ru.cbrf.rates.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RateDao {
    @Query("SELECT * FROM rates WHERE date = :date ORDER BY charCode ASC")
    suspend fun getRatesForDate(date: String): List<RateEntity>

    @Query("SELECT DISTINCT date FROM rates ORDER BY date DESC")
    suspend fun getAllDates(): List<String>

    @Query("SELECT DISTINCT date FROM rates WHERE date <= :upTo ORDER BY date DESC LIMIT 1")
    suspend fun getLatestDateUpTo(upTo: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRates(rates: List<RateEntity>)

    @Query("DELETE FROM rates WHERE date < :cutoff")
    suspend fun deleteOlderThan(cutoff: String)
}
