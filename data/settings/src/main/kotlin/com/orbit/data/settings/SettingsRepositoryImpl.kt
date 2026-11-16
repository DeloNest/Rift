package com.orbit.data.settings

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<OrbitSettings>,
) : SettingsRepository {

    override val settings: Flow<OrbitSettings> = dataStore.data

    override suspend fun update(transform: (OrbitSettings) -> OrbitSettings) {
        dataStore.updateData(transform)
    }
}
