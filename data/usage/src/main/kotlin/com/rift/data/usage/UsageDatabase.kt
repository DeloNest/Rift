package com.rift.data.usage

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [LaunchEvent::class],
    version = 1,
    exportSchema = true,
)
abstract class UsageDatabase : RoomDatabase() {
    abstract fun usageDao(): UsageDao
}
