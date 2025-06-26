package com.example.remarket.ui.product.create

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.remarket.data.network.ProductRequest
import com.example.remarket.data.repository.CloudinaryService
import com.example.remarket.domain.usecase.CreateProductUseCase
import com.example.remarket.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class CreateProductViewModel @Inject constructor(
    private val createProductUseCase: CreateProductUseCase,
    private val cloudinaryService: CloudinaryService
) : ViewModel() {

    private val _brand = MutableStateFlow("")
    val brand: StateFlow<String> = _brand.asStateFlow()

    private val _model = MutableStateFlow("")
    val model: StateFlow<String> = _model.asStateFlow()

    private val _storage = MutableStateFlow("")
    val storage: StateFlow<String> = _storage.asStateFlow()

    private val _price = MutableStateFlow(0.0)
    val price: StateFlow<Double> = _price.asStateFlow()

    private val _priceText = MutableStateFlow("")
    val priceText: StateFlow<String> = _priceText.asStateFlow()

    private val _imei = MutableStateFlow("")
    val imei: StateFlow<String> = _imei.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _images = MutableStateFlow<List<String>>(emptyList())
    val images: StateFlow<List<String>> = _images.asStateFlow()

    private val _boxImageUrl = MutableStateFlow("")
    val boxImageUrl: StateFlow<String> = _boxImageUrl.asStateFlow()

    private val _invoiceUrl = MutableStateFlow("")
    val invoiceUrl: StateFlow<String> = _invoiceUrl.asStateFlow()

    private val _state = MutableStateFlow<Resource<Unit>>(Resource.Idle)
    val state: StateFlow<Resource<Unit>> = _state.asStateFlow()

    fun onBrandChanged(value: String) { _brand.value = value }
    fun onModelChanged(value: String) { _model.value = value }
    fun onStorageChanged(value: String) { _storage.value = value }
    fun onPriceTextChanged(text: String) {
        _priceText.value = text
        text.toDoubleOrNull()?.let { _price.value = it }
    }
    fun onImeiChanged(value: String) { _imei.value = value }
    fun onDescriptionChanged(value: String) { _description.value = value }
    fun addImage(uri: String) { _images.value = _images.value + uri }
    fun setBoxImage(uri: String) { _boxImageUrl.value = uri }
    fun setInvoiceImage(uri: String) { _invoiceUrl.value = uri }

    private fun validate(): String? {
        return when {
            brand.value.isBlank() -> "Por favor ingresa la marca."
            model.value.isBlank() -> "Por favor ingresa el modelo."
            storage.value.isBlank() -> "Por favor ingresa el almacenamiento."
            price.value <= 0.0 -> "Por favor ingresa un precio válido."
            imei.value.isBlank() -> "Por favor ingresa el IMEI."
            else -> null
        }
    }

    fun submit(context: Context, onSuccess: () -> Unit) {
        validate()?.let { msg ->
            _state.value = Resource.Error(msg)
            return
        }

        viewModelScope.launch {
            _state.value = Resource.Loading
            try {
                // 🟡 Intentar subir imágenes — si falla, continuar igual
                val uploadedImages = try {
                    _images.value.map { cloudinaryService.uploadImage(context, it) }
                } catch (e: Exception) {
                    Log.w("CreateProductVM", "Fallo al subir imágenes, usando URIs locales")
                    _images.value
                }

                val boxUrl = try {
                    _boxImageUrl.value.takeIf { it.isNotBlank() }?.let {
                        cloudinaryService.uploadImage(context, it)
                    }
                } catch (e: Exception) {
                    _boxImageUrl.value
                }

                val invoiceUrl = try {
                    _invoiceUrl.value.takeIf { it.isNotBlank() }?.let {
                        cloudinaryService.uploadImage(context, it)
                    }
                } catch (e: Exception) {
                    _invoiceUrl.value
                }

                // 🟢 Preparar request con URLs remotas o locales
                val req = ProductRequest(
                    brand = _brand.value,
                    model = _model.value,
                    storage = _storage.value,
                    price = _price.value,
                    imei = _imei.value,
                    description = _description.value,
                    imageUrls = uploadedImages,
                    boxImageUrl = boxUrl,
                    invoiceUrl = invoiceUrl
                )

                // 🟢 Guardar local y encolar sincronización
                val result = createProductUseCase(req)

                when (result) {
                    is Resource.Success -> {
                        _state.value = Resource.Success(Unit)
                        onSuccess()
                    }
                    is Resource.Error -> _state.value = Resource.Error(result.message)
                    else -> {}
                }

            } catch (e: Exception) {
                _state.value = Resource.Error("Error inesperado: ${e.message}")
            }
        }
    }
}
