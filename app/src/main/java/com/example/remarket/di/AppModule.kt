package com.example.remarket.di

import android.content.Context
import androidx.room.Room
import com.example.remarket.data.local.AppDatabase
import com.example.remarket.data.local.ProductDao
import com.example.remarket.data.network.ApiService
import com.example.remarket.data.network.AuthInterceptor
import com.example.remarket.data.network.TokenManager
import com.example.remarket.data.repository.ConnectivityRepository
import com.example.remarket.data.repository.IConnectivityRepository
import com.example.remarket.data.repository.IProductRepository
import com.example.remarket.data.repository.ProductRepository
import com.example.remarket.data.repository.UserRepository
import com.example.remarket.domain.usecase.GetProductsUseCase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideConnectivityRepository(
        @ApplicationContext context: Context
    ): IConnectivityRepository = ConnectivityRepository(context)

    @Provides
    @Singleton
    fun provideTokenProvider(tokenManager: TokenManager): () -> String = {
        tokenManager.getToken() ?: "" // [cite: 50]
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = Firebase.auth

    @Provides @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

    @Provides @Singleton
    fun provideOkHttpClient(
        logging: HttpLoggingInterceptor,
        authInterceptor: AuthInterceptor
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build() // Aquí construimos el OkHttpClient correctamente


    @Provides @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("http://161.132.50.99:9364/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build() // Aquí construimos el Retrofit correctamente

    @Provides @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase { // [cite: 53]
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "remarket_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideProductDao(database: AppDatabase): ProductDao = database.productDao()


    @Provides @Singleton
    fun provideProductRepository(
        apiService: ApiService,
        productDao: ProductDao, // [cite: 54]
        @ApplicationContext context: Context
    ): IProductRepository = ProductRepository(apiService, productDao, context)

    @Provides
    @Singleton
    fun provideUserRepository(api: ApiService): UserRepository =
        UserRepository(api)

    @Provides @Singleton
    fun provideGetProductsUseCase(repo: IProductRepository): GetProductsUseCase =
        GetProductsUseCase(repo)
}