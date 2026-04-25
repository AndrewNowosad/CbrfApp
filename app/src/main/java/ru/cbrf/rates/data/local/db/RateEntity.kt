package ru.cbrf.rates.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rates", primaryKeys = ["date", "charCode"])
data class RateEntity(
    val date: String,       // ISO-8601: yyyy-MM-dd
    val charCode: String,
    val numCode: String,
    val nominal: Int,
    val nameRu: String,
    val nameEn: String,
    val value: Double       // raw value from API (divide by nominal for unit rate)
)
