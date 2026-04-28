package ru.cbrf.rates.data.repository

import ru.cbrf.rates.data.local.db.CurrencyNameDao
import ru.cbrf.rates.data.local.db.CurrencyNameEntity
import ru.cbrf.rates.data.local.db.RateDao
import ru.cbrf.rates.data.local.db.RateEntity
import ru.cbrf.rates.data.remote.CbrfApi
import ru.cbrf.rates.data.remote.CbrfXmlParser
import ru.cbrf.rates.data.remote.CurrencyValParser
import ru.cbrf.rates.data.remote.CurrencyXmlDto
import ru.cbrf.rates.domain.model.CurrencyMeta
import ru.cbrf.rates.domain.model.RateEntry
import ru.cbrf.rates.domain.repository.RateRepository
import android.util.Log
import java.nio.charset.Charset
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RateRepositoryImpl @Inject constructor(
    private val api: CbrfApi,
    private val dao: RateDao,
    private val currencyNameDao: CurrencyNameDao
) : RateRepository {

    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val cbrfDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val CBRF_CHARSET = Charset.forName("windows-1251")

    private val currencyNamesMutex = Mutex()

    override suspend fun getRatesForDate(date: LocalDate): List<RateEntry> {
        val entities = dao.getRatesForDate(date.format(isoFormatter))
        if (entities.isEmpty()) return emptyList()
        val namesMap = currencyNameDao.getAll().associateBy { it.charCode }
        return entities.map { it.toDomain(namesMap) }
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
            val xmlString = responseBody.bytes().toString(CBRF_CHARSET)
            val (rawPublishDate, dtos) = CbrfXmlParser.parse(xmlString)
            if (dtos.isEmpty() || rawPublishDate == null) return@runCatching false
            val publishDate: LocalDate = rawPublishDate

            if (publishDate != date) {
                val today = LocalDate.now()
                // CBR returned an earlier date (weekend/holiday gap).
                // Cache past dates from publishDate up to (but not including) today — those are
                // final. Today's slot is intentionally left empty: CBR will publish today's real
                // rates later, and filling it now with stale data would prevent the next scheduled
                // fetch from picking up the fresh publication (fetchRatesIfNeeded is cache-first).
                if (date <= today && publishDate < date) {
                    val charCodes = dtos.map { it.charCode }.toSet()
                    val idToCharCode = dtos.associate { it.cbrId to it.charCode }
                    val fillUpTo = if (date == today) today.minusDays(1) else date
                    var d = publishDate
                    while (d <= fillUpTo) {
                        val dStr = d.format(isoFormatter)
                        if (dao.getRatesForDate(dStr).isEmpty()) {
                            dao.insertRates(buildEntities(dtos, dStr))
                        }
                        d = d.plusDays(1)
                    }
                    ensureCurrencyNamesLoaded(charCodes, idToCharCode)
                    // Return false when today's rates weren't cached yet so the caller knows
                    // today's data is still unavailable and can fall back to the previous date.
                    return@runCatching date != today
                }
                // Future date — real rates will be published later, don't cache yet.
                return@runCatching false
            }

            val entities = buildEntities(dtos, date.format(isoFormatter))
            dao.insertRates(entities)

            val cutoff = date.minusDays(60).format(isoFormatter)
            dao.deleteOlderThan(cutoff)

            val charCodes = entities.map { it.charCode }.toSet()
            val idToCharCode = dtos.associate { it.cbrId to it.charCode }
            ensureCurrencyNamesLoaded(charCodes, idToCharCode)

            true
        }.onFailure { Log.e(TAG, "fetchFromNetwork failed for date=$date", it) }
    }

    private fun buildEntities(dtos: List<CurrencyXmlDto>, dateStr: String): List<RateEntity> =
        dtos.map { dto ->
            RateEntity(
                date = dateStr,
                charCode = dto.charCode,
                cbrId = dto.cbrId,
                numCode = dto.numCode,
                nominal = dto.nominal,
                nameRu = dto.nameRu,
                nameEn = CurrencyMeta.nameEnFor(dto.charCode, dto.nameRu),
                value = dto.value
            )
        }

    private suspend fun ensureCurrencyNamesLoaded(
        charCodes: Set<String>,
        idToCharCode: Map<String, String>
    ) = currencyNamesMutex.withLock {
        val existingCodes = currencyNameDao.getAllCharCodes().toSet()
        if (existingCodes.isEmpty() || !existingCodes.containsAll(charCodes)) {
            fetchCurrencyNames(idToCharCode)
        }
    }

    private suspend fun fetchCurrencyNames(idToCharCode: Map<String, String>) {
        runCatching {
            val dtosD0 = CurrencyValParser.parse(api.getValuteListD0().bytes().toString(CBRF_CHARSET))
            val dtosD1 = CurrencyValParser.parse(api.getValuteListD1().bytes().toString(CBRF_CHARSET))
            // d=0 takes priority (more current names), d=1 fills in the rest
            val mergedById = (dtosD1 + dtosD0).distinctBy { it.cbrId }
            val entities = mergedById.mapNotNull { dto ->
                val charCode = idToCharCode[dto.cbrId] ?: return@mapNotNull null
                CurrencyNameEntity(
                    charCode = charCode,
                    nameRu = dto.nameRu,
                    nameEn = dto.nameEn.ifBlank { CurrencyMeta.nameEnFor(charCode, dto.nameRu) }
                )
            }
            if (entities.isNotEmpty()) {
                currencyNameDao.insertAll(entities)
            }
        }.onFailure { Log.e(TAG, "fetchCurrencyNames failed", it) }
    }

    companion object {
        private const val TAG = "RateRepositoryImpl"
    }

    private fun RateEntity.toDomain(namesMap: Map<String, CurrencyNameEntity>) = RateEntry(
        date = LocalDate.parse(this.date, isoFormatter),
        charCode = charCode,
        nameRu = namesMap[charCode]?.nameRu ?: nameRu,
        nameEn = namesMap[charCode]?.nameEn ?: nameEn,
        nominal = nominal,
        value = value,
        flagEmoji = CurrencyMeta.flagFor(charCode)
    )
}
