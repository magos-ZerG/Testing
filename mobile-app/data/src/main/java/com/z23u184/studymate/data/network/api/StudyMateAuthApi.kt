package com.z23u184.studymate.data.network.api

import com.z23u184.studymate.data.network.dto.LoginRequestDto
import com.z23u184.studymate.data.network.dto.RefreshRequestDto
import com.z23u184.studymate.data.network.dto.RegisterRequestDto
import com.z23u184.studymate.data.network.dto.TokenPairResponseDto
import com.z23u184.studymate.data.network.dto.UserResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface StudyMateAuthApi {
    @POST("/api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequestDto): UserResponseDto

    @POST("/api/v1/auth/login")
    suspend fun login(@Body request: LoginRequestDto): TokenPairResponseDto

    @POST("/api/v1/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequestDto): TokenPairResponseDto

    @POST("/api/v1/auth/logout")
    suspend fun logout(@Body request: RefreshRequestDto)
}
