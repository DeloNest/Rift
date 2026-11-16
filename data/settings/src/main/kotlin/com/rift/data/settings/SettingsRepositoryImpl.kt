package com.rift.data.settings

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<RiftSettings>,
) : SettingsRepository {

    override val settings: Flow<RiftSettings> = dataStore.data

    override suspend fun update(transform: (RiftSettings) -> RiftSettings) {
        dataStore.updateData(transform)
    }
}
