package com.example.unibus.data.api.model

data class LoginRequest(
    val email: String,
    val password: String
)

data class SignupRequest(
    val email: String,
    val password: String,
    val full_name: String? = null
)

data class TokenResponse(
    val access_token: String,
    val expires_at: String?,
    val user: UserResponse
)

data class UserResponse(
    val id: Int,
    val email: String,
    val full_name: String? = null,
    val is_active: Boolean? = null
)

data class BusArrivalDto(
    val route_no: String,
    val route_name: String? = null,
    val headsign: String? = null,
    val eta_minutes: Int? = null,
    val eta_minutes_next: Int? = null
)

data class BusArrivalsResponse(
    val stop_id: String,
    val stop_name: String? = null,
    val retrieved_at: String? = null,
    val arrivals: List<BusArrivalDto> = emptyList()
)
