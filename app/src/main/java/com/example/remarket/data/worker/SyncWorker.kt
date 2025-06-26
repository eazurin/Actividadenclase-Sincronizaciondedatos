package com.example.remarket.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.remarket.data.repository.IProductRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor( // [cite: 48]
    @Assisted appContext: Context, // [cite: 48]
    @Assisted workerParams: WorkerParameters, // [cite: 48]
    private val productRepository: IProductRepository // [cite: 48]
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "ProductSyncWorker" // [cite: 49]
    }

    override suspend fun doWork(): Result {
        return try {
            // 1. Primero, empujar los cambios locales al servidor.
            productRepository.syncPendingChanges()

            // 2. Después, traer los cambios del servidor.
            productRepository.refreshProducts()

            Result.success()
        } catch (e: Exception) {
            // Si hay un error (ej. sin red), se reintentará según la política definida.
            Result.retry() // [cite: 49]
        }
    }
}