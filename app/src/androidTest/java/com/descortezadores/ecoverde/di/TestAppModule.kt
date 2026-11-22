package com.descortezadores.ecoverde.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.descortezadores.ecoverde.data.local.EcoVerdeDatabase
import com.descortezadores.ecoverde.data.local.dao.AnalisisCromaticoDao
import com.descortezadores.ecoverde.data.local.dao.ForestMonitoringDao
import com.descortezadores.ecoverde.data.remote.api.AuthApiService
import com.descortezadores.ecoverde.data.remote.api.MonitoringApiService
import com.descortezadores.ecoverde.data.remote.interceptor.JwtAuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Módulo Hilt de pruebas E2E que reemplaza [AppModule] para aislar:
 * - Room SQLite en memoria (sin alterar la base de datos real del usuario).
 * - DataStore temporal en `cacheDir` (sin alterar la sesión real guardada en el dispositivo).
 * - Endpoint local cerrado (`http://127.0.0.1:1/`) para garantizar fallback offline determinista a LiteRT y OpenCV.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AppModule::class]
)
object TestAppModule {

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        val dir = File(context.cacheDir, "e2e_test_datastore").apply { mkdirs() }
        val testPrefsFile = File(dir, "session_${UUID.randomUUID()}.preferences_pb")
        return PreferenceDataStoreFactory.create(
            produceFile = { testPrefsFile }
        )
    }

    @Provides
    @Singleton
    fun provideEcoVerdeDatabase(
        @ApplicationContext context: Context
    ): EcoVerdeDatabase {
        return Room.inMemoryDatabaseBuilder(
            context,
            EcoVerdeDatabase::class.java
        )
            .allowMainThreadQueries()
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
        return OkHttpClient.Builder()
            .addInterceptor(jwtAuthInterceptor)
            .connectTimeout(500, TimeUnit.MILLISECONDS)
            .readTimeout(500, TimeUnit.MILLISECONDS)
            .writeTimeout(500, TimeUnit.MILLISECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://127.0.0.1:1/")
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
