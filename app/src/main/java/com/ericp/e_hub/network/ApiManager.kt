package com.ericp.e_hub.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.ericp.e_hub.cache.ApiCache
import com.ericp.e_hub.config.ApiConfig
import com.ericp.e_hub.config.NextCloudConfig
import com.ericp.e_hub.utils.Endpoints
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiManager private constructor() {

    private val gson = Gson()

    companion object {
        private const val TIMEOUT_SECONDS = 30L
        @Volatile
        private var INSTANCE: ApiManager? = null

        fun getInstance(): ApiManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ApiManager().also { INSTANCE = it }
            }
        }
    }

    interface ApiService {
        @GET
        suspend fun get(
            @Url url: String,
            @Header("Authorization") authorization: String?
        ): Response<ResponseBody>

        @POST
        suspend fun post(
            @Url url: String,
            @Header("Authorization") authorization: String?,
            @Body body: RequestBody
        ): Response<ResponseBody>

        @PUT
        suspend fun put(
            @Url url: String,
            @Header("Authorization") authorization: String?,
            @Body body: RequestBody
        ): Response<ResponseBody>

        @DELETE
        suspend fun delete(
            @Url url: String,
            @Header("Authorization") authorization: String?
        ): Response<ResponseBody>
    }

    private fun createRetrofitService(context: Context): ApiService {
        val apiConfig = ApiConfig(context.applicationContext)

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(apiConfig.getServerUrl())
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun generateCacheKey(endpoint: String, method: String, params: Map<String, Any>? = null): String {
        val baseKey = "${method}_${endpoint}"
        if (params == null || params.isEmpty()) {
            return baseKey
        }
        // Sort params for consistency
        val sortedParams = params.toSortedMap()
        val paramsStr = sortedParams.entries.joinToString("&") { "${it.key}=${it.value}" }
        return "$baseKey?$paramsStr"
    }

    private fun getAuthHeader(context: Context): String? {
        val apiConfig = ApiConfig(context.applicationContext)
        val apiKey = apiConfig.getApiKey()
        return if (apiKey.isNullOrBlank()) null else "Bearer $apiKey"
    }

    suspend fun get(
        context: Context,
        endpoint: String,
        useCache: Boolean = true,
        cacheTtlHours: Long? = null
    ): ApiResult {
        val apiCache = ApiCache(context.applicationContext)
        val apiConfig = ApiConfig(context.applicationContext)
        val cacheKey = generateCacheKey(endpoint, "GET")

        // Return cached if available and valid
        if (useCache) {
            apiCache.get(cacheKey)?.let { cachedData ->
                try {
                    return ApiResult.Success(cachedData)
                } catch (_: JsonSyntaxException) {
                    apiCache.remove(cacheKey)
                }
            }
        }

        // If no network, return from cache even if expired or error
        if (!isNetworkAvailable(context)) {
            return ApiResult.Error("No network connection", cached = apiCache.contains(cacheKey))
        }

        return try {
            val service = createRetrofitService(context)
            val response = service.get(
                url = "${apiConfig.getServerUrl()}/$endpoint",
                authorization = getAuthHeader(context)
            )

            if (response.isSuccessful) {
                val responseBody = response.body()?.string() ?: ""

                // Cache if enabled
                if (useCache) {
                    apiCache.put(cacheKey, responseBody, cacheTtlHours)
                }

                ApiResult.Success(responseBody)
            } else {
                ApiResult.Error("HTTP Error: ${response.code()}")
            }

        } catch (e: IOException) {
            ApiResult.Error("Network error: ${e.message}")
        } catch (e: Exception) {
            ApiResult.Error("Unexpected error: ${e.message}")
        }
    }

    suspend fun post(
        context: Context,
        endpoint: String,
        data: Any
    ): ApiResult {
        val apiConfig = ApiConfig(context.applicationContext)

        if (!isNetworkAvailable(context)) {
            return ApiResult.Error("No network connection")
        }

        return try {
            val service = createRetrofitService(context)
            val jsonBody = gson.toJson(data)
            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

            val response = service.post(
                url = "${apiConfig.getServerUrl()}/$endpoint",
                authorization = getAuthHeader(context),
                body = requestBody
            )

            if (response.isSuccessful) {
                val responseBody = response.body()?.string() ?: ""
                ApiResult.Success(responseBody)
            } else {
                ApiResult.Error("HTTP Error: ${response.code()}")
            }

        } catch (e: IOException) {
            ApiResult.Error("Network error: ${e.message}")
        } catch (e: Exception) {
            ApiResult.Error("Unexpected error: ${e.message}")
        }
    }

    suspend fun put(
        context: Context,
        endpoint: String,
        data: Any
    ): ApiResult {
        val apiConfig = ApiConfig(context.applicationContext)

        if (!isNetworkAvailable(context)) {
            return ApiResult.Error("No network connection")
        }

        return try {
            val service = createRetrofitService(context)
            val jsonBody = gson.toJson(data)
            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

            val response = service.put(
                url = "${apiConfig.getServerUrl()}/$endpoint",
                authorization = getAuthHeader(context),
                body = requestBody
            )

            if (response.isSuccessful) {
                val responseBody = response.body()?.string() ?: ""
                ApiResult.Success(responseBody)
            } else {
                ApiResult.Error("HTTP Error: ${response.code()}")
            }

        } catch (e: IOException) {
            ApiResult.Error("Network error: ${e.message}")
        } catch (e: Exception) {
            ApiResult.Error("Unexpected error: ${e.message}")
        }
    }

    suspend fun delete(
        context: Context,
        endpoint: String
    ): ApiResult {
        val apiConfig = ApiConfig(context.applicationContext)

        if (!isNetworkAvailable(context)) {
            return ApiResult.Error("No network connection")
        }

        return try {
            val service = createRetrofitService(context)

            val response = service.delete(
                url = "${apiConfig.getServerUrl()}/$endpoint",
                authorization = getAuthHeader(context)
            )

            if (response.isSuccessful) {
                val responseBody = response.body()?.string() ?: ""
                ApiResult.Success(responseBody)
            } else {
                ApiResult.Error("HTTP Error: ${response.code()}")
            }

        } catch (e: IOException) {
            ApiResult.Error("Network error: ${e.message}")
        } catch (e: Exception) {
            ApiResult.Error("Unexpected error: ${e.message}")
        }
    }

    suspend fun testConnection(context: Context): ApiResult {
        // TODO: Replace with a proper endpoint to test connectivity
        return get(context, Endpoints.TODO, useCache = false)
    }

    fun testNextCloudConnection(context: Context, nextCloudConfig: NextCloudConfig): ApiResult {
        val testUrl = nextCloudConfig.getServerUrl()?.trimEnd('/') + nextCloudConfig.getWebdavEndpoint()

        if (!isNetworkAvailable(context)) {
            return ApiResult.Error("No network connection")
        }

        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url(testUrl)
                .header("Authorization", Credentials.basic(nextCloudConfig.getUsername() ?: "", nextCloudConfig.getPassword() ?: ""))
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    ApiResult.Success("Connection successful")
                } else {
                    ApiResult.Error("HTTP Error: ${response.code}" + if (response.code == 401) " (Unauthorized - check credentials)" else "")
                }
            }
        } catch (e: IOException) {
            ApiResult.Error("Network error: ${e.message}")
        } catch (e: Exception) {
            ApiResult.Error("Unexpected error: ${e.message}")
        }
    }

    fun clearCache(context: Context) {
        val apiCache = ApiCache(context.applicationContext)
        apiCache.clear()
    }

    fun clearEndpointCache(context: Context, endpoint: String) {
        val apiCache = ApiCache(context.applicationContext)
        apiCache.removeByEndpointPrefix(endpoint)
    }

    sealed class ApiResult {
        data class Success(val data: String) : ApiResult()
        data class Error(val message: String, val cached: Boolean = false) : ApiResult()
    }
}
