package com.rift.data.workspace.di

import android.content.Context
import androidx.room.Room
import com.rift.data.workspace.WorkspaceDao
import com.rift.data.workspace.WorkspaceDatabase
import com.rift.data.workspace.WorkspaceRepository
import com.rift.data.workspace.WorkspaceRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WorkspaceBindsModule {

    @Binds
    @Singleton
    abstract fun bindWorkspaceRepository(impl: WorkspaceRepositoryImpl): WorkspaceRepository
}

@Module
@InstallIn(SingletonComponent::class)
object WorkspaceProvidesModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WorkspaceDatabase =
        Room.databaseBuilder(context, WorkspaceDatabase::class.java, "rift_workspace.db")
            .build()

    @Provides
    fun provideWorkspaceDao(db: WorkspaceDatabase): WorkspaceDao = db.workspaceDao()
}
