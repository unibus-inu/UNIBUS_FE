package com.example.unibus.data

import com.example.unibus.data.api.model.BusArrivalsResponse
import com.example.unibus.data.api.model.LoginRequest
import com.example.unibus.data.api.model.SignupRequest
import com.example.unibus.data.api.model.TokenResponse
import com.example.unibus.data.api.model.UserResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

object UnibusRepository {
    private val api = NetworkModule.api

    suspend fun login(email: String, password: String): TokenResponse = withContext(Dispatchers.IO) {
        val res = api.login(LoginRequest(email.trim(), password))
        val body = res.requireBody("로그인에 실패했습니다.")
        AuthTokenStore.setToken(body.access_token)
        body
    }

    suspend fun signup(email: String, password: String, fullName: String?): UserResponse =
        withContext(Dispatchers.IO) {
            val res = api.signup(SignupRequest(email.trim(), password, fullName?.trim().takeUnless { it.isNullOrBlank() }))
            res.requireBody("회원가입에 실패했습니다.")
        }

    suspend fun me(): UserResponse = withContext(Dispatchers.IO) {
        val res = api.me(AuthTokenStore.token()?.let { "Bearer $it" })
        res.requireBody("사용자 정보를 불러오지 못했습니다.")
    }

    suspend fun busArrivals(): BusArrivalsResponse = withContext(Dispatchers.IO) {
        val res = api.busArrivals()
        res.requireBody("버스 도착 정보를 불러오지 못했습니다.")
    }

    private fun <T> Response<T>.requireBody(defaultMessage: String): T {
        if (isSuccessful) {
            val body = body()
            if (body != null) return body
        }
        val msg = try {
            errorBody()?.string()
        } catch (_: Exception) {
            null
        }
        throw IllegalStateException(msg?.takeIf { it.isNotBlank() } ?: defaultMessage)
    }
}
