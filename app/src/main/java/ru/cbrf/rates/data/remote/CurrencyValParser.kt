package ru.cbrf.rates.data.remote

import android.util.Xml
import java.io.StringReader

data class CurrencyValDto(
    val cbrId: String,
    val nameRu: String,
    val nameEn: String
)

object CurrencyValParser {
    fun parse(xml: String): List<CurrencyValDto> {
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(xml))

        val results = mutableListOf<CurrencyValDto>()
        var insideItem = false
        var currentTag = ""
        var cbrId = ""
        var nameRu = ""
        var nameEn = ""

        var eventType = parser.eventType
        while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                org.xmlpull.v1.XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    if (currentTag == "Item") {
                        insideItem = true
                        cbrId = parser.getAttributeValue(null, "ID") ?: ""
                        nameRu = ""; nameEn = ""
                    }
                }
                org.xmlpull.v1.XmlPullParser.TEXT -> {
                    if (insideItem) {
                        val text = parser.text.trim()
                        when (currentTag) {
                            "Name" -> nameRu = text
                            "EngName" -> nameEn = text
                        }
                    }
                }
                org.xmlpull.v1.XmlPullParser.END_TAG -> {
                    if (parser.name == "Item" && insideItem) {
                        insideItem = false
                        if (cbrId.isNotEmpty()) {
                            results += CurrencyValDto(cbrId, nameRu, nameEn)
                        }
                    }
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }
        return results
    }
}
