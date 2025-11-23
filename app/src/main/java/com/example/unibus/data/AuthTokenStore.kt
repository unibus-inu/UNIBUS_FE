package com.example.unibus.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AuthTokenStore {
    private val tokenState = MutableStateFlow<String?>(null)

    fun setToken(token: String?) {
        tokenState.value = token
    }

    fun clear() {
        tokenState.value = null
    }

    fun token(): String? = tokenState.value

    fun observe(): StateFlow<String?> = tokenState
}
