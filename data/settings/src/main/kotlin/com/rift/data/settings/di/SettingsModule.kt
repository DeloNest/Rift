package com.rift.data.settings.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import com.rift.data.settings.RiftSettings
import com.rift.data.settings.SettingsRepository
import com.rift.data.settings.SettingsRepositoryImpl
import com.rift.data.settings.SettingsSerializer
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
    fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<RiftSettings> =
        DataStoreFactory.create(
            serializer = SettingsSerializer,
            produceFile = { File(context.filesDir, "rift_settings.json") },
        )
}
