package com.example.remarket.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.remarket.data.model.Product
import com.example.remarket.data.model.ProductDto
import java.util.UUID

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val sellerId: String,
    val brand: String,
    val model: String,
    val storage: String,
    val price: Double,
    val imei: String,
    val description: String,
    val imagesJson: String,
    val box: String,
    val invoiceUri: String,
    val status: String,
    val active: Boolean,
    val createdAt: String,
    val updatedAt: String,

    // --- Campos para Sincronización ---
    val isSynced: Boolean = false,
    val lastModified: Long = System.currentTimeMillis(),
    val deletedLocally: Boolean = false
)

fun ProductEntity.toDomain(): Product = Product(
    id = id, // [cite: 5]
    sellerId = sellerId, // [cite: 5]
    brand = brand, // [cite: 5]
    model = model, // [cite: 5]
    storage = storage, // [cite: 5]
    price = price, // [cite: 5]
    imei = imei, // [cite: 5]
    description = description, // [cite: 5]
    images = imagesJson.split(",").filter { it.isNotEmpty() }, // [cite: 5]
    box = box, // [cite: 5]
    invoiceUri = invoiceUri, // [cite: 5]
    status = status, // [cite: 5]
    active = active, // [cite: 5]
    createdAt = createdAt, // [cite: 5]
    updatedAt = updatedAt // [cite: 5]
)

fun ProductDto.toEntity(): ProductEntity = ProductEntity(
    id = id,
    sellerId = sellerId, // [cite: 6]
    brand = brand, // [cite: 6]
    model = model, // [cite: 6]
    storage = storage, // [cite: 6]
    price = price, // [cite: 6]
    imei = imei, // [cite: 6]
    description = description, // [cite: 6]
    imagesJson = imageUrls.joinToString(","), // [cite: 6]
    box = boxImageUrl, // [cite: 6]
    invoiceUri = invoiceUrl, // [cite: 6]
    status = status, // [cite: 6]
    active = active, // [cite: 6]
    createdAt = createdAt, // [cite: 6]
    updatedAt = updatedAt, // [cite: 6]
    isSynced = true, // Los datos que vienen del servidor están sincronizados
    lastModified = System.currentTimeMillis(),
    deletedLocally = false
)