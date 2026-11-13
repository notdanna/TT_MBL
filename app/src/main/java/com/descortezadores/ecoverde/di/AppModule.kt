package com.descortezadores.ecoverde.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.descortezadores.ecoverde.BuildConfig
import com.descortezadores.ecoverde.data.local.EcoVerdeDatabase
import com.descortezadores.ecoverde.data.local.dao.AnalisisCromaticoDao
import com.descortezadores.ecoverde.data.local.dao.ForestMonitoringDao
import com.descortezadores.ecoverde.data.remote.api.AuthApiService
import com.descortezadores.ecoverde.data.remote.api.MonitoringApiService
import com.descortezadores.ecoverde.data.remote.interceptor.JwtAuthInterceptor
import com.descortezadores.ecoverde.data.repository.AuthRepositoryImpl
import com.descortezadores.ecoverde.data.repository.BarkClassificationRepositoryImpl
import com.descortezadores.ecoverde.data.repository.ForestMonitoringRepositoryImpl
import com.descortezadores.ecoverde.data.repository.ImageSegmentationRepositoryImpl
import com.descortezadores.ecoverde.domain.repository.AuthRepository
import com.descortezadores.ecoverde.domain.repository.BarkClassificationRepository
import com.descortezadores.ecoverde.domain.repository.ForestMonitoringRepository
import com.descortezadores.ecoverde.domain.repository.ImageSegmentationRepository
import dagger.Binds
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
abstract class RepositoryBindingsModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindBarkClassificationRepository(
        impl: BarkClassificationRepositoryImpl
    ): BarkClassificationRepository

    @Binds
    @Singleton
    abstract fun bindImageSegmentationRepository(
        impl: ImageSegmentationRepositoryImpl
    ): ImageSegmentationRepository

    @Binds
    @Singleton
    abstract fun bindForestMonitoringRepository(
        impl: ForestMonitoringRepositoryImpl
    ): ForestMonitoringRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val SESSION_DATASTORE_NAME = "ecoverde_brigadista_session"

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(SESSION_DATASTORE_NAME) }
        )
    }

    @Provides
    @Singleton
    fun provideEcoVerdeDatabase(
        @ApplicationContext context: Context
    ): EcoVerdeDatabase {
        return Room.databaseBuilder(
            context,
            EcoVerdeDatabase::class.java,
            EcoVerdeDatabase.DATABASE_NAME
        )
            .addMigrations(EcoVerdeDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideAnalisisCromaticoDao(database: EcoVerdeDatabase): AnalisisCromaticoDao {
        return database.analisisCromaticoDao()
    }

    @Provides
    @Singleton
    fun provideForestMonitoringDao(database: EcoVerdeDatabase): ForestMonitoringDao {
        return database.forestMonitoringDao()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        jwtAuthInterceptor: JwtAuthInterceptor
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .addInterceptor(jwtAuthInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        // Conexión directa a la API en la nube (AWS API Gateway HTTPS)
        val baseUrl = BuildConfig.DEFAULT_BASE_URL

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideMonitoringApiService(retrofit: Retrofit): MonitoringApiService {
        return retrofit.create(MonitoringApiService::class.java)
    }
}

