package com.rift.data.workspace

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {

    @Query("SELECT * FROM workspace_item WHERE container = :container AND page = :page ORDER BY rank")
    fun getItemsByPage(container: Long, page: Int): Flow<List<WorkspaceItemEntity>>

    @Query("SELECT * FROM workspace_item WHERE container = :container ORDER BY page, rank")
    fun getItemsByContainer(container: Long): Flow<List<WorkspaceItemEntity>>

    @Query("SELECT * FROM workspace_item WHERE container = :folderId ORDER BY rank")
    fun getFolderItems(folderId: Long): Flow<List<WorkspaceItemEntity>>

    @Query("SELECT * FROM workspace_item WHERE type = ${WorkspaceItemEntity.TYPE_WIDGET}")
    fun getAllWidgets(): Flow<List<WorkspaceItemEntity>>

    @Query("SELECT DISTINCT page FROM workspace_item WHERE container = :container ORDER BY page")
    suspend fun getPages(container: Long): List<Int>

    @Query("SELECT * FROM workspace_item")
    suspend fun getAllItems(): List<WorkspaceItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WorkspaceItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<WorkspaceItemEntity>)

    @Update
    suspend fun update(item: WorkspaceItemEntity)

    @Delete
    suspend fun delete(item: WorkspaceItemEntity)

    @Query("DELETE FROM workspace_item WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM workspace_item")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(items: List<WorkspaceItemEntity>) {
        deleteAll()
        insertAll(items)
    }
}
