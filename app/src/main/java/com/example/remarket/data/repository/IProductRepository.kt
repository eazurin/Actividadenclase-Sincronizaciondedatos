package com.example.remarket.data.repository

import com.example.remarket.data.model.Product
import com.example.remarket.data.network.ProductRequest
import com.example.remarket.util.Resource
import kotlinx.coroutines.flow.Flow

interface IProductRepository {
    fun getAllProducts(): Flow<Resource<List<Product>>>
    suspend fun createProduct(request: ProductRequest): Resource<Unit>
    suspend fun updateProduct(productId: String, request: ProductRequest): Resource<Unit>
    suspend fun deleteProduct(productId: String): Resource<Unit>
    suspend fun getProductById(productId: String): Flow<Resource<Product>>

    fun triggerManualSyncFromUI()

    suspend fun syncPendingChanges()
    suspend fun refreshProducts()

    suspend fun reportProduct(productId: String, reason: String): Flow<Boolean>
    suspend fun toggleFavorite(productId: String): Flow<Boolean>
}