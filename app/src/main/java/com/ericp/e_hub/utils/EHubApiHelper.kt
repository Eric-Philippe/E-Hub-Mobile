package com.ericp.e_hub.utils

import android.content.Context
import com.ericp.e_hub.config.ApiConfig
import com.ericp.e_hub.network.ApiManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Class for managing API interactions
 */
class EHubApiHelper(private val context: Context) {

    private val apiManager = ApiManager.getInstance()
    private val apiConfig = ApiConfig(context)

    /**
     * Check if the API is configured
     */
    fun isApiConfigured(): Boolean {
        return apiConfig.isApiKeyConfigured()
    }

    fun fetchDataAsync(
        endpoint: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isApiConfigured()) {
            onError("API not configured. Please set your API key in settings.")
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            when (val result = apiManager.get(context, endpoint)) {
                is ApiManager.ApiResult.Success -> {
                    onSuccess(result.data)
                }
                is ApiManager.ApiResult.Error -> {
                    val errorMsg = if (result.cached) {
                        "No network - data may be outdated: ${result.message}"
                    } else {
                        "API Error: ${result.message}"
                    }
                    onError(errorMsg)
                }
            }
        }
    }

    fun postDataAsync(
        endpoint: String,
        data: Any,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isApiConfigured()) {
            onError("API not configured. Please set your API key in settings.")
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            when (val result = apiManager.post(context, endpoint, data)) {
                is ApiManager.ApiResult.Success -> {
                    onSuccess(result.data)
                }
                is ApiManager.ApiResult.Error -> {
                    onError("API Error: ${result.message}")
                }
            }
        }
    }

    /**
     * Empty the API cache
     */
    fun clearApiCache() {
        apiManager.clearCache(context)
    }

    /**
     * api/nonogram { "started": Date, "ended": Date }
     */
    fun submitNonogramAsync(
        data: Any,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isApiConfigured()) {
            onError("API not configured. Please set your API key in settings.")
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            when (val result = apiManager.post(context, Endpoints.NONOGRAM_SUBMIT, data)) {
                is ApiManager.ApiResult.Success -> {
                    onSuccess(result.data)
                }
                is ApiManager.ApiResult.Error -> {
                    val errorMsg = if (result.cached) {
                        "No network - data may be outdated: ${result.message}"
                    } else {
                        "API Error: ${result.message}"
                    }
                    onError(errorMsg)
                }
            }
        }
    }

    companion object {
        object Endpoints {
            const val HEALTH = "health"
            const val TODO = "api/todo"
            const val NONOGRAM_SUBMIT = "api/nonogram"
        }
    }
}
