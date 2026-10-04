package com.example.uniportal.di

import com.example.uniportal.util.SystemTimeProvider
import com.example.uniportal.util.TimeProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class TimeModule {
    @Binds
    abstract fun bindTimeProvider(impl: SystemTimeProvider): TimeProvider
}
