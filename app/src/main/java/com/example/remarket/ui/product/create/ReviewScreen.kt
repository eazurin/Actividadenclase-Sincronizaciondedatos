// File: app/src/main/java/com/example/remarket/ui/product/create/ReviewScreen.kt
package com.example.remarket.ui.product.create

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.remarket.util.Resource

/**
 * Pantalla de revisión: solo muestra estado final del envío.
 */
@Composable
fun ReviewScreen(
    viewModel: CreateProductViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Venta en revisión", style = MaterialTheme.typography.titleLarge)
        Text("Esperando periodo de validación")

        when (state) {
            is Resource.Loading -> CircularProgressIndicator()
            is Resource.Error -> Text(
                text = (state as Resource.Error).message,
                color = MaterialTheme.colorScheme.error
            )
            is Resource.Success -> Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Hecho",
                modifier = Modifier.size(80.dp)
            )
            else -> {}
        }

        Spacer(modifier = Modifier.weight(1f))

        // Botón para volver o finalizar
        Button(onClick = onBack) {
            Text("Volver al inicio")
        }
    }
}