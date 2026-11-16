package com.orbit.data.settings

import kotlinx.coroutines.flow.Flow

/**
 * Repository for reading and writing Orbit settings.
 */
interface SettingsRepository {
    val settings: Flow<OrbitSettings>
    suspend fun update(transform: (OrbitSettings) -> OrbitSettings)
}
