package com.example.remarket.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.remarket.data.model.Product
import com.example.remarket.data.network.TokenManager
import com.example.remarket.data.repository.IProductRepository
import com.example.remarket.domain.usecase.GetProductsUseCase
import com.example.remarket.util.Resource
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val products: List<Product> = emptyList(),
    val filteredProducts: List<Product> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val productRepository: IProductRepository,
    private val getProductsUseCase: GetProductsUseCase,
    private val firebaseAuth: FirebaseAuth,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        observeLocalProducts()
        refreshDataFromNetwork()
    }

    /**
     * Observar productos de la base local.
     * Esta función no falla aunque no haya internet.
     */
    private fun observeLocalProducts() {
        viewModelScope.launch {
            productRepository.getAllProducts().collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is Resource.Success -> {
                        val products = resource.data
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = null,
                                products = products,
                                filteredProducts = filter(products, it.searchQuery)
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "No se pudieron cargar los productos locales."
                            )
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * Refrescar productos desde el backend.
     * Si falla, NO actualiza la UI ni interfiere con los datos locales.
     */
    fun refreshDataFromNetwork() {
        viewModelScope.launch {
            try {
                productRepository.refreshProducts()
                // Éxito: no hace falta tocar el UIState, Room se actualizará solo
            } catch (e: Exception) {
                // No interrumpas la UI si ya hay productos locales
                if (_uiState.value.products.isEmpty()) {
                    _uiState.update { it.copy(error = "No se pudo sincronizar con el servidor.") }
                }
                // Si hay productos locales, no mostramos el error para no asustar al usuario
            }
        }
    }

    /**
     * Filtro de productos por búsqueda.
     */
    fun onSearchQueryChanged(q: String) {
        _uiState.update {
            it.copy(
                searchQuery = q,
                filteredProducts = filter(it.products, q)
            )
        }
    }

    private fun filter(list: List<Product>, q: String): List<Product> =
        if (q.isBlank()) {
            list
        } else {
            list.filter {
                it.brand.contains(q, ignoreCase = true) ||
                        it.model.contains(q, ignoreCase = true) ||
                        it.storage.contains(q, ignoreCase = true)
            }
        }

    fun onLogout() {
        firebaseAuth.signOut()
        tokenManager.clearToken()
    }
}