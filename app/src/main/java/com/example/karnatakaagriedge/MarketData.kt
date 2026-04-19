package com.example.karnatakaagriedge

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// The structure of the Government's data
data class MandiResponse(
    val records: List<MandiRecord>
)

data class MandiRecord(
    val state: String,
    val district: String,
    val market: String,
    val commodity: String,
    val variety: String,
    val arrival_date: String,
    val modal_price: String
)

// The "Phone Line" to the government servers
interface AgmarknetApi {
    @GET("resource/9ef84268-d588-465a-a308-a864a43d0070")
    suspend fun getMarketPrices(
        @Query("api-key") apiKey: String,
        @Query("format") format: String = "json",
        @Query("filters[state]") state: String = "Karnataka",
        @Query("limit") limit: Int = 20
    ): MandiResponse
}

// The Client that handles the connection
object RetrofitClient {
    private const val BASE_URL = "https://api.data.gov.in/"

    val instance: AgmarknetApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AgmarknetApi::class.java)
    }
}