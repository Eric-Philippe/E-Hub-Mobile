package com.ericp.e_hub.utils.api

import android.content.Context
import com.ericp.e_hub.dto.State
import com.ericp.e_hub.dto.ToDoDto
import com.ericp.e_hub.dto.ToDoRequest
import com.ericp.e_hub.utils.EHubApiHelper
import com.ericp.e_hub.utils.Endpoints

class ToDoApi(context: Context) {
    private val apiHelper = EHubApiHelper(context)

    fun fetchToDos(
        onSuccess: (List<ToDoDto>) -> Unit,
        onError: (String) -> Unit
    ) {
        apiHelper.fetchDataAndParseAsync<List<ToDoDto>>(
            Endpoints.TODO,
            onSuccess,
            onError
        )
    }

    fun fetchSingleToDo(
        id: String,
        onSuccess: (ToDoDto) -> Unit,
        onError: (String) -> Unit
    ) {
        apiHelper.fetchDataAndParseAsync<ToDoDto>(
            "${Endpoints.TODO}/$id",
            onSuccess,
            onError
        )
    }

    fun createToDo(
        data: ToDoRequest,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        apiHelper.postDataAsync(
            Endpoints.TODO,
            data,
            onSuccess,
            onError
        )
    }

    fun updateToDo(
        id: String,
        data: ToDoRequest,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        apiHelper.putAsync(
            "${Endpoints.TODO}/$id",
            data,
            onSuccess,
            onError
        )
    }

    fun changeToDoState(
        id: String,
        newState: State,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val data = mapOf("state" to newState)
        apiHelper.patchAsync(
            "${Endpoints.TODO}/$id/state",
            data,
            onSuccess,
            onError
        )
    }

    fun deleteToDo(
        id: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        apiHelper.deleteAsync(
            "${Endpoints.TODO}/$id",
            onSuccess,
            onError
        )
    }
}