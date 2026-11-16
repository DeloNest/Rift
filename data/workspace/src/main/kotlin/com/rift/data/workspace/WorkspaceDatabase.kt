package com.rift.data.workspace

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [WorkspaceItemEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class WorkspaceDatabase : RoomDatabase() {
    abstract fun workspaceDao(): WorkspaceDao
}
