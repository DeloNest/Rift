package com.rift.data.settings

import androidx.datastore.core.Serializer
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

/**
 * DataStore serializer for RiftSettings using kotlinx.serialization.
 */
object SettingsSerializer : Serializer<RiftSettings> {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override val defaultValue: RiftSettings = RiftSettings()

    override suspend fun readFrom(input: InputStream): RiftSettings {
        return try {
            json.decodeFromString(
                RiftSettings.serializer(),
                input.readBytes().decodeToString(),
            )
        } catch (_: Exception) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: RiftSettings, output: OutputStream) {
        output.write(json.encodeToString(RiftSettings.serializer(), t).toByteArray())
    }
}
