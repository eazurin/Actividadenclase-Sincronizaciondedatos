package com.example.remarket.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import com.example.remarket.data.model.ProductDto
import com.example.remarket.data.model.UserDto
import com.example.remarket.data.model.RegisterResponse
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {
    @POST("auth/register")
    suspend fun registerUser(@Body request: RegisterRequest): RegisterResponse

    // PRODUCTOS
    @GET("products")
    suspend fun getProducts(): List<ProductDto>

    @GET("products/{productId}")
    suspend fun getProductById(@Path("productId") productId: String): ProductDto

    @POST("products")
    suspend fun createProduct(@Body request: ProductRequest): ProductDto

    @PUT("products/{productId}")
    suspend fun updateProduct(@Path("productId") productId: String, @Body request: ProductRequest): ProductDto

    @DELETE("products/{productId}")
    suspend fun deleteProduct(@Path("productId") productId: String): Response<Unit>

    // USUARIOS
    @GET("users/{userId}")
    suspend fun getUserById(@Path("userId") userId: String): UserDto

    // REPORTES
    @POST("reports")
    suspend fun createReport(@Body request: ReportRequest): Response<Unit> // [cite: 14]
}