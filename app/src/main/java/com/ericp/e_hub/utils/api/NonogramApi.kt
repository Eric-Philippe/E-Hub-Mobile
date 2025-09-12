package com.ericp.e_hub.utils.api

import android.content.Context
import com.ericp.e_hub.utils.EHubApiHelper
import com.ericp.e_hub.utils.Endpoints

class NonogramApi(context: Context) {
    private val apiHelper = EHubApiHelper(context)

    /**
     * api/nonogram { "started": Date, "ended": Date }
     */
    fun submitNonogramAsync(
        data: Any,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        apiHelper.postDataAsync(
            Endpoints.NONOGRAM,
            data,
            onSuccess,
            onError
        )
    }

    fun fetchTotalNonogramsAsync(
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        apiHelper.fetchDataAsync(
            endpoint = Endpoints.NONOGRAM + "/stats/total-games",
            onSuccess = onSuccess,
            onError = onError
        )
    }
}