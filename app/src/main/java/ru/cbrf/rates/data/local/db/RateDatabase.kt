package ru.cbrf.rates.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [RateEntity::class], version = 1, exportSchema = false)
abstract class RateDatabase : RoomDatabase() {
    abstract fun rateDao(): RateDao
}
