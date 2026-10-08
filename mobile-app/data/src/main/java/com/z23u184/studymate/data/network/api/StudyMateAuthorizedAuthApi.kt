package com.z23u184.studymate.data.network.api

import com.z23u184.studymate.data.network.dto.UserResponseDto
import retrofit2.http.GET

interface StudyMateAuthorizedAuthApi {
    @GET("/api/v1/auth/me")
    suspend fun me(): UserResponseDto
}
