package com.rift.data.usage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UsageDao {

    @Insert
    suspend fun insertEvent(event: LaunchEvent)

    @Query("SELECT * FROM launch_event ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentEvents(limit: Int = 5000): List<LaunchEvent>

    @Query("SELECT packageName, COUNT(*) as count FROM launch_event GROUP BY packageName ORDER BY count DESC LIMIT :limit")
    suspend fun getTopApps(limit: Int = 20): List<AppFrequency>

    @Query("DELETE FROM launch_event WHERE id NOT IN (SELECT id FROM launch_event ORDER BY timestamp DESC LIMIT :keep)")
    suspend fun trimOldEvents(keep: Int = 5000)
}

data class AppFrequency(val packageName: String, val count: Int)
