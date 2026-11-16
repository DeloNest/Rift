package com.orbit.data.settings.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import com.orbit.data.settings.OrbitSettings
import com.orbit.data.settings.SettingsRepository
import com.orbit.data.settings.SettingsRepositoryImpl
import com.orbit.data.settings.SettingsSerializer
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsBindsModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}

@Module
@InstallIn(SingletonComponent::class)
object SettingsProvidesModule {

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<OrbitSettings> =
        DataStoreFactory.create(
            serializer = SettingsSerializer,
            produceFile = { File(context.filesDir, "orbit_settings.json") },
        )
}
