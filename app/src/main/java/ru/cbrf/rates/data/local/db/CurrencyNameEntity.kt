package ru.cbrf.rates.data.local.db

import androidx.room.Entity

@Entity(tableName = "currency_names", primaryKeys = ["charCode"])
data class CurrencyNameEntity(
    val charCode: String,
    val nameRu: String,
    val nameEn: String
)
