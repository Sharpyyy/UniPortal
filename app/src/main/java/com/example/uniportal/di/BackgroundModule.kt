package com.example.uniportal.di

import com.example.uniportal.data.sync.DefaultUniversitySyncer
import com.example.uniportal.data.sync.UniversitySyncer
import com.example.uniportal.work.WorkManagerScheduler
import com.example.uniportal.work.WorkScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class BackgroundModule {
    @Binds
    abstract fun bindUniversitySyncer(impl: DefaultUniversitySyncer): UniversitySyncer

    @Binds
    abstract fun bindWorkScheduler(impl: WorkManagerScheduler): WorkScheduler
}
