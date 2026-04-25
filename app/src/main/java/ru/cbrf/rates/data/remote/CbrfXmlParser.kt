package ru.cbrf.rates.data.remote

import android.util.Xml
import java.io.StringReader
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class CurrencyXmlDto(
    val charCode: String,
    val numCode: String,
    val nominal: Int,
    val nameRu: String,
    val value: Double,
    val publishDate: LocalDate?
)

object CbrfXmlParser {
    private val dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun parse(xml: String): Pair<LocalDate?, List<CurrencyXmlDto>> {
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(xml))

        var publishDate: LocalDate? = null
        val results = mutableListOf<CurrencyXmlDto>()

        var charCode = ""
        var numCode = ""
        var nominal = 1
        var nameRu = ""
        var value = 0.0
        var insideValute = false
        var currentTag = ""

        var eventType = parser.eventType
        while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                org.xmlpull.v1.XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    when (currentTag) {
                        "ValCurs" -> {
                            val dateAttr = parser.getAttributeValue(null, "Date")
                            publishDate = runCatching {
                                LocalDate.parse(dateAttr, dateFormat)
                            }.getOrNull()
                        }
                        "Valute" -> {
                            insideValute = true
                            charCode = ""; numCode = ""; nominal = 1; nameRu = ""; value = 0.0
                        }
                    }
                }
                org.xmlpull.v1.XmlPullParser.TEXT -> {
                    if (insideValute) {
                        val text = parser.text.trim()
                        when (currentTag) {
                            "CharCode" -> charCode = text
                            "NumCode" -> numCode = text
                            "Nominal" -> nominal = text.toIntOrNull() ?: 1
                            "Name" -> nameRu = text
                            "VunitRate" -> value = text.replace(",", ".").toDoubleOrNull() ?: 0.0
                        }
                    }
                }
                org.xmlpull.v1.XmlPullParser.END_TAG -> {
                    if (parser.name == "Valute" && insideValute) {
                        insideValute = false
                        if (charCode.isNotEmpty() && value > 0.0) {
                            results += CurrencyXmlDto(charCode, numCode, nominal, nameRu, value, publishDate)
                        }
                    }
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }

        return publishDate to results
    }
}
