// File: app/src/main/java/com/tuempresa/remarket/data/network/ApiService.kt
package com.example.remarket.data.network

import android.service.autofill.SaveRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import com.example.remarket.data.model.ProductDto
import com.example.remarket.data.model.UserDto
import com.example.remarket.data.model.RegisterResponse

import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {
    @POST("auth/register")
    suspend fun registerUser(@Body request: RegisterRequest): RegisterResponse

    @POST("products")
    suspend fun createProduct(@Body request: ProductRequest): ProductDto
    @GET("products")
    suspend fun getProducts(): List<ProductDto>

    @GET("products/{productId}")
    suspend fun getProductById(
        @Path("productId") productId: String
    ): ProductDto

    @POST("saved")
    suspend fun saveProduct(@Body request: SaveRequest): Response<Unit>

    @POST("reports")
    suspend fun createReport(@Body request: ReportRequest): Response<Unit>

    @GET("users/{userId}")
    suspend fun getUserById(
        @Path("userId") userId: String
    ): UserDto
}
