package ru.cbrf.rates.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [RateEntity::class, CurrencyNameEntity::class], version = 3, exportSchema = false)
abstract class RateDatabase : RoomDatabase() {
    abstract fun rateDao(): RateDao
    abstract fun currencyNameDao(): CurrencyNameDao
}
