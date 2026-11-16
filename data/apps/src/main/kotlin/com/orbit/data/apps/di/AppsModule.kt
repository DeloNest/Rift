package com.orbit.data.apps.di

import com.orbit.data.apps.AppsRepository
import com.orbit.data.apps.AppsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppsModule {

    @Binds
    @Singleton
    abstract fun bindAppsRepository(impl: AppsRepositoryImpl): AppsRepository
}
