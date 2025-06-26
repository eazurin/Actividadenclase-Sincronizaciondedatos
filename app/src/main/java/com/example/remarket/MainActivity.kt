package com.example.remarket

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.remarket.data.repository.IProductRepository
import com.example.remarket.ui.navigation.AppNavGraph
import com.example.remarket.ui.theme.ReMarketTheme
import com.example.remarket.util.ConnectivityObserver
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var productRepository: IProductRepository
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val observer = ConnectivityObserver(applicationContext)
        observer.observe(this) { isConnected ->
            if (isConnected) {
                productRepository.triggerManualSyncFromUI()
            }
        }

        enableEdgeToEdge()
        setContent {
            ReMarketTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavGraph()
                }
            }
        }
    }
}