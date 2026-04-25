package ru.cbrf.rates.data.repository

import ru.cbrf.rates.data.local.db.RateDao
import ru.cbrf.rates.data.local.db.RateEntity
import ru.cbrf.rates.data.remote.CbrfApi
import ru.cbrf.rates.data.remote.CbrfXmlParser
import ru.cbrf.rates.domain.model.CurrencyMeta
import ru.cbrf.rates.domain.model.RateEntry
import ru.cbrf.rates.domain.repository.RateRepository
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RateRepositoryImpl @Inject constructor(
    private val api: CbrfApi,
    private val dao: RateDao
) : RateRepository {

    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val cbrfDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    override suspend fun getRatesForDate(date: LocalDate): List<RateEntry> {
        return dao.getRatesForDate(date.format(isoFormatter)).map { it.toDomain() }
    }

    override suspend fun fetchRatesIfNeeded(date: LocalDate): Result<Boolean> {
        val dateStr = date.format(isoFormatter)
        if (dao.getRatesForDate(dateStr).isNotEmpty()) return Result.success(true)
        return fetchFromNetwork(date)
    }

    override suspend fun forceRefresh(date: LocalDate): Result<Boolean> {
        return fetchFromNetwork(date)
    }

    override suspend fun getLatestAvailableDate(upTo: LocalDate): LocalDate? {
        return dao.getLatestDateUpTo(upTo.format(isoFormatter))
            ?.let { LocalDate.parse(it, isoFormatter) }
    }

    override suspend fun getCachedDates(): List<LocalDate> {
        return dao.getAllDates().map { LocalDate.parse(it, isoFormatter) }
    }

    private suspend fun fetchFromNetwork(date: LocalDate): Result<Boolean> {
        return runCatching {
            val dateParam = date.format(cbrfDateFormatter)
            val responseBody = api.getDailyRates(dateParam)
            val xmlString = responseBody.bytes().toString(Charset.forName("windows-1251"))
            val (publishDate, dtos) = CbrfXmlParser.parse(xmlString)

            if (dtos.isEmpty() || publishDate == null) return@runCatching false

            // CBRF returns the actual date in the response; if it doesn't match requested date,
            // the requested date has no published rate (e.g. future date not yet available)
            if (publishDate != date) return@runCatching false

            val entities = dtos.map { dto ->
                RateEntity(
                    date = date.format(isoFormatter),
                    charCode = dto.charCode,
                    numCode = dto.numCode,
                    nominal = dto.nominal,
                    nameRu = dto.nameRu,
                    nameEn = CurrencyMeta.nameEnFor(dto.charCode, dto.nameRu),
                    value = dto.value
                )
            }
            dao.insertRates(entities)

            // Prune rates older than 60 days to keep the DB small
            val cutoff = date.minusDays(60).format(isoFormatter)
            dao.deleteOlderThan(cutoff)

            true
        }
    }

    private fun RateEntity.toDomain() = RateEntry(
        date = LocalDate.parse(this.date, isoFormatter),
        charCode = charCode,
        nameRu = nameRu,
        nameEn = nameEn,
        nominal = nominal,
        value = value,
        flagEmoji = CurrencyMeta.flagFor(charCode)
    )
}
