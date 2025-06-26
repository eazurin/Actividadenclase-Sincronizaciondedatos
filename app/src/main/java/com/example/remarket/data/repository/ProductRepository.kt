package com.example.remarket.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.remarket.data.local.ProductDao
import com.example.remarket.data.local.ProductEntity
import com.example.remarket.data.local.toDomain
import com.example.remarket.data.local.toEntity
import com.example.remarket.data.model.Product
import com.example.remarket.data.model.toDomain
import com.example.remarket.data.network.ApiService
import com.example.remarket.data.network.ProductRequest
import com.example.remarket.data.network.ReportRequest
import com.example.remarket.data.worker.SyncWorker
import com.example.remarket.util.Resource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

class ProductRepository @Inject constructor(
    private val api: ApiService,
    private val productDao: ProductDao,
    @ApplicationContext private val context: Context
) : IProductRepository {

    private val workManager = WorkManager.getInstance(context)

    override fun getAllProducts(): Flow<Resource<List<Product>>> = flow {
        emit(Resource.Loading)
        productDao.getAllProducts().map { entities ->
            entities.map { it.toDomain() }
        }.collect { domainProducts ->
            emit(Resource.Success(domainProducts))
        }
        }.flowOn(Dispatchers.IO)

    override suspend fun createProduct(request: ProductRequest): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val newProduct = ProductEntity(
                id = UUID.randomUUID().toString(),
                sellerId = "temp_user",
                brand = request.brand,
                model = request.model,
                storage = request.storage,
                price = request.price,
                imei = request.imei,
                description = request.description,
                imagesJson = request.imageUrls.joinToString(","),
                box = request.boxImageUrl ?: "",
                invoiceUri = request.invoiceUrl ?: "",
                status = "pending_approval",
                active = true,
                createdAt = System.currentTimeMillis().toString(),
                updatedAt = System.currentTimeMillis().toString(),
                isSynced = false,
                lastModified = System.currentTimeMillis(),
                deletedLocally = false
            )

            productDao.insertOrUpdateProduct(newProduct)

            triggerSyncWorker()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Error al guardar localmente.")
        }
    }

    override suspend fun updateProduct(productId: String, request: ProductRequest): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val existingProduct = productDao.getProductById(productId) ?: return@withContext Resource.Error("Producto no encontrado localmente.")

            val updatedProduct = existingProduct.copy(
                brand = request.brand,
                model = request.model,
                storage = request.storage,
                price = request.price,
                imei = request.imei,
                description = request.description,
                imagesJson = request.imageUrls.joinToString(","),
                box = request.boxImageUrl ?: "",
                invoiceUri = request.invoiceUrl ?: "",
                isSynced = false,
                lastModified = System.currentTimeMillis()
            )

            productDao.insertOrUpdateProduct(updatedProduct)
            triggerSyncWorker()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Error al actualizar localmente.")
        }
    }

    override suspend fun deleteProduct(productId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val productToDelete = productDao.getProductById(productId) ?: return@withContext Resource.Error("Producto no encontrado.")

            val markedForDeletion = productToDelete.copy(
                deletedLocally = true,
                isSynced = false,
                lastModified = System.currentTimeMillis()
            )
            productDao.insertOrUpdateProduct(markedForDeletion)
            triggerSyncWorker()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Error al marcar para borrado.")
        }
    }

    override suspend fun syncPendingChanges() {
        withContext(Dispatchers.IO) {
            val unsyncedProducts = productDao.getUnsyncedProducts()
            if (unsyncedProducts.isEmpty()) return@withContext

            for (product in unsyncedProducts) {
                try {
                    if (product.deletedLocally) {
                        val response = api.deleteProduct(product.id)
                        if (response.isSuccessful) {
                            productDao.deleteProductsByIds(listOf(product.id))
                        }
                    } else {
                        val request = ProductRequest(
                            brand = product.brand,
                            model = product.model,
                            storage = product.storage,
                            price = product.price,
                            imei = product.imei,
                            description = product.description,
                            imageUrls = product.imagesJson.split(",").filter { it.isNotEmpty() },
                            boxImageUrl = product.box,
                            invoiceUrl = product.invoiceUri
                        )

                        val isNewProduct = try { UUID.fromString(product.id); true } catch (e: IllegalArgumentException) { false }

                        if (isNewProduct) {
                            val remoteProduct = api.createProduct(request)
                            // Reemplazar el producto local temporal con el del servidor
                            productDao.deleteProductsByIds(listOf(product.id))
                            productDao.insertOrUpdateProduct(remoteProduct.toEntity())
                        } else {
                            val remoteProduct = api.updateProduct(product.id, request)
                            productDao.insertOrUpdateProduct(remoteProduct.toEntity())
                        }
                    }
                } catch (e: Exception) {
                }
            }
        }
    }

    override suspend fun refreshProducts() {
        withContext(Dispatchers.IO) {
            try {
            val remoteProducts = api.getProducts()
            val unsyncedIds = productDao.getUnsyncedProducts().map { it.id }.toSet()

            // Filtrar para no sobrescribir cambios locales pendientes
            val productsToInsert = remoteProducts.filter { it.id !in unsyncedIds }

            productDao.insertAll(productsToInsert.map { it.toEntity() })
            } catch (e: Exception) {
                // El error se manejará silenciosamente, la app seguirá funcionando con datos locales.
                throw e
            }
        }
    }

    private fun triggerSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueue(syncRequest)
    }

    override fun triggerManualSyncFromUI() {
        triggerSyncWorker()
    }

    override suspend fun getProductById(productId: String): Flow<Resource<Product>> = flow {
        emit(Resource.Loading)
        try {
            val localProduct = productDao.getProductById(productId)
            if (localProduct != null && !localProduct.deletedLocally) {
                emit(Resource.Success(localProduct.toDomain()))
            } else {
                val dto = api.getProductById(productId) //
                productDao.insertOrUpdateProduct(dto.toEntity())
                emit(Resource.Success(dto.toDomain())) //
            }
        } catch (e: HttpException) {
        emit(Resource.Error("Error ${e.code()}: ${e.message()}")) // 
        } catch (e: IOException) {
        emit(Resource.Error("Error de red, por favor intente de nuevo.")) // 
    }
        }.flowOn(Dispatchers.IO)

    override suspend fun reportProduct(productId: String, reason: String): Flow<Boolean> = flow {
        try {
        val response = api.createReport(ReportRequest(productId, reason)) // 
        emit(response.isSuccessful)
        } catch (e: Exception) {
        emit(false)
    }
        }.flowOn(Dispatchers.IO)

    override suspend fun toggleFavorite(productId: String): Flow<Boolean> {
        TODO("Not yet implemented") // 
    }
}