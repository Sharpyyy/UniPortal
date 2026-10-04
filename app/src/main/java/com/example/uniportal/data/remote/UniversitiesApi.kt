package com.example.uniportal.data.remote

import com.example.uniportal.data.remote.dto.UniversityDto
import retrofit2.http.GET
import retrofit2.http.Query

interface UniversitiesApi {
    @GET("search")
    suspend fun search(
        @Query("name") name: String?,
        @Query("country") country: String?,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
    ): List<UniversityDto>

    @GET("search")
    suspend fun findByDomain(@Query("domain") domain: String): List<UniversityDto>

    companion object {
        const val BASE_URL = "http://universities.hipolabs.com/"
    }
}
