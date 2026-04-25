package ru.cbrf.rates.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.cbrf.rates.data.repository.RateRepositoryImpl
import ru.cbrf.rates.domain.repository.RateRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRateRepository(impl: RateRepositoryImpl): RateRepository
}
