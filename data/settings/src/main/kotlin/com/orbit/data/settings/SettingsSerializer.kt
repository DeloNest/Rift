package com.orbit.data.settings

import androidx.datastore.core.Serializer
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

/**
 * DataStore serializer for OrbitSettings using kotlinx.serialization.
 */
object SettingsSerializer : Serializer<OrbitSettings> {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override val defaultValue: OrbitSettings = OrbitSettings()

    override suspend fun readFrom(input: InputStream): OrbitSettings {
        return try {
            json.decodeFromString(
                OrbitSettings.serializer(),
                input.readBytes().decodeToString(),
            )
        } catch (_: Exception) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: OrbitSettings, output: OutputStream) {
        output.write(json.encodeToString(OrbitSettings.serializer(), t).toByteArray())
    }
}
