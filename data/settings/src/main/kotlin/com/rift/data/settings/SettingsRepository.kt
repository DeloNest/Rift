package com.rift.data.settings

import kotlinx.coroutines.flow.Flow

/**
 * Repository for reading and writing Rift settings.
 */
interface SettingsRepository {
    val settings: Flow<RiftSettings>
    suspend fun update(transform: (RiftSettings) -> RiftSettings)
}
