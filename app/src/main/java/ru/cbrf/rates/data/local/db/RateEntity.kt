package ru.cbrf.rates.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rates", primaryKeys = ["date", "charCode"])
data class RateEntity(
    val date: String,
    val charCode: String,
    val cbrId: String,
    val numCode: String,
    val nominal: Int,
    val nameRu: String,
    val nameEn: String,
    val value: Double
)
