package ru.cbrf.rates.domain.model

object CurrencyMeta {
    data class Info(val flagEmoji: String, val nameEn: String)

    private val data = mapOf(
        "AUD" to Info("🇦🇺", "Australian Dollar"),
        "AZN" to Info("🇦🇿", "Azerbaijan Manat"),
        "GBP" to Info("🇬🇧", "British Pound"),
        "AMD" to Info("🇦🇲", "Armenian Dram"),
        "BYN" to Info("🇧🇾", "Belarusian Ruble"),
        "BGN" to Info("🇧🇬", "Bulgarian Lev"),
        "BRL" to Info("🇧🇷", "Brazilian Real"),
        "HUF" to Info("🇭🇺", "Hungarian Forint"),
        "KRW" to Info("🇰🇷", "South Korean Won"),
        "HKD" to Info("🇭🇰", "Hong Kong Dollar"),
        "DKK" to Info("🇩🇰", "Danish Krone"),
        "USD" to Info("🇺🇸", "US Dollar"),
        "EUR" to Info("🇪🇺", "Euro"),
        "INR" to Info("🇮🇳", "Indian Rupee"),
        "KZT" to Info("🇰🇿", "Kazakhstani Tenge"),
        "CAD" to Info("🇨🇦", "Canadian Dollar"),
        "KGS" to Info("🇰🇬", "Kyrgyzstani Som"),
        "CNY" to Info("🇨🇳", "Chinese Yuan"),
        "MDL" to Info("🇲🇩", "Moldovan Leu"),
        "NOK" to Info("🇳🇴", "Norwegian Krone"),
        "PLN" to Info("🇵🇱", "Polish Zloty"),
        "RON" to Info("🇷🇴", "Romanian Leu"),
        "XDR" to Info("🌐", "SDR"),
        "SGD" to Info("🇸🇬", "Singapore Dollar"),
        "TJS" to Info("🇹🇯", "Tajikistani Somoni"),
        "TRY" to Info("🇹🇷", "Turkish Lira"),
        "TMT" to Info("🇹🇲", "Turkmenistani Manat"),
        "UZS" to Info("🇺🇿", "Uzbekistani Som"),
        "UAH" to Info("🇺🇦", "Ukrainian Hryvnia"),
        "CZK" to Info("🇨🇿", "Czech Koruna"),
        "SEK" to Info("🇸🇪", "Swedish Krona"),
        "CHF" to Info("🇨🇭", "Swiss Franc"),
        "ZAR" to Info("🇿🇦", "South African Rand"),
        "JPY" to Info("🇯🇵", "Japanese Yen"),
        "EGP" to Info("🇪🇬", "Egyptian Pound"),
        "IDR" to Info("🇮🇩", "Indonesian Rupiah"),
        "QAR" to Info("🇶🇦", "Qatari Riyal"),
        "VND" to Info("🇻🇳", "Vietnamese Dong"),
        "AED" to Info("🇦🇪", "UAE Dirham"),
        "THB" to Info("🇹🇭", "Thai Baht"),
    )

    fun flagFor(charCode: String) = data[charCode]?.flagEmoji ?: "💱"
    fun nameEnFor(charCode: String, fallback: String) = data[charCode]?.nameEn ?: fallback
}
