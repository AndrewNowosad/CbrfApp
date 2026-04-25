package ru.cbrf.rates.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.cbrf.rates.data.local.db.RateDao
import ru.cbrf.rates.data.local.db.RateDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideRateDatabase(@ApplicationContext context: Context): RateDatabase =
        Room.databaseBuilder(context, RateDatabase::class.java, "cbrf_rates.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideRateDao(db: RateDatabase): RateDao = db.rateDao()
}
