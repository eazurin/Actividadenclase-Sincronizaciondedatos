package com.example.remarket.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.remarket.data.repository.IProductRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
@Assisted workerParams: WorkerParameters,
private val productRepository: IProductRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "ProductSyncWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            productRepository.syncPendingChanges()

            productRepository.refreshProducts()

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}