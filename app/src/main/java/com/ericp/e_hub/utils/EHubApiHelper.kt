package com.ericp.e_hub.utils

import android.content.Context
import com.ericp.e_hub.config.ApiConfig
import com.ericp.e_hub.network.ApiManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Classe utilitaire pour simplifier l'utilisation de l'API
 * Exemple d'utilisation du système d'API Key implémenté
 */
class EHubApiHelper(private val context: Context) {

    private val apiManager = ApiManager.getInstance()
    private val apiConfig = ApiConfig(context)

    /**
     * Vérifie si l'API est configurée
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
            onError("API non configurée. Veuillez configurer votre clé API dans les paramètres.")
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            when (val result = apiManager.get(context, endpoint)) {
                is ApiManager.ApiResult.Success -> {
                    onSuccess(result.data)
                }
                is ApiManager.ApiResult.Error -> {
                    val errorMsg = if (result.cached) {
                        "Pas de réseau - données possiblement obsolètes: ${result.message}"
                    } else {
                        "Erreur API: ${result.message}"
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
            onError("API non configurée. Veuillez configurer votre clé API dans les paramètres.")
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            when (val result = apiManager.post(context, endpoint, data)) {
                is ApiManager.ApiResult.Success -> {
                    onSuccess(result.data)
                }
                is ApiManager.ApiResult.Error -> {
                    onError("Erreur API: ${result.message}")
                }
            }
        }
    }

    /**
     * Vider le cache en cas de besoin
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
            onError("API non configurée. Veuillez configurer votre clé API dans les paramètres.")
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            when (val result = apiManager.post(context, Endpoints.NONOGRAM_SUBMIT, data)) {
                is ApiManager.ApiResult.Success -> {
                    onSuccess(result.data)
                }
                is ApiManager.ApiResult.Error -> {
                    val errorMsg = if (result.cached) {
                        "Pas de réseau - données possiblement obsolètes: ${result.message}"
                    } else {
                        "Erreur API: ${result.message}"
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
