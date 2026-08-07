package com.xingzhun.di

import com.xingzhun.data.local.PoemDao
import com.xingzhun.data.repository.PoemRepository
import com.xingzhun.data.source.CorpusLoader
import com.xingzhun.engine.PingZeEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideEngine(): PingZeEngine = PingZeEngine()

    @Provides
    fun provideRepository(
        poemDao: PoemDao,
        corpusLoader: CorpusLoader,
        engine: PingZeEngine,
    ): PoemRepository = PoemRepository(poemDao, corpusLoader, engine)
}
