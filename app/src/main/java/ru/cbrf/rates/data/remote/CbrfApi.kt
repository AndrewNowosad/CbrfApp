package ru.cbrf.rates.data.remote

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query

interface CbrfApi {
    /**
     * Fetches daily rates from CBRF XML feed.
     * @param dateReq date in format dd/MM/yyyy, or null for today
     */
    @GET("XML_daily.asp")
    suspend fun getDailyRates(
        @Query("date_req") dateReq: String? = null
    ): ResponseBody

    @GET("XML_val.asp")
    suspend fun getValuteListD0(): ResponseBody

    @GET("XML_val.asp?d=1")
    suspend fun getValuteListD1(): ResponseBody
}
