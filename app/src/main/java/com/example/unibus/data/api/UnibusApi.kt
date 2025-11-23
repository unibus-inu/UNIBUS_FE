package com.example.unibus.data.api

import com.example.unibus.data.api.model.BusArrivalsResponse
import com.example.unibus.data.api.model.LoginRequest
import com.example.unibus.data.api.model.SignupRequest
import com.example.unibus.data.api.model.TokenResponse
import com.example.unibus.data.api.model.UserResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface UnibusApi {
    @POST("/v1/auth/login")
    suspend fun login(@Body payload: LoginRequest): Response<TokenResponse>

    @POST("/v1/auth/signup")
    suspend fun signup(@Body payload: SignupRequest): Response<UserResponse>

    @GET("/v1/auth/me")
    suspend fun me(@Header("Authorization") authHeader: String?): Response<UserResponse>

    @GET("/v1/bus/arrivals")
    suspend fun busArrivals(): Response<BusArrivalsResponse>
}
