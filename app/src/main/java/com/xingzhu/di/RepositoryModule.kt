package com.xingzhu.di

import com.xingzhu.data.local.PoemDao
import com.xingzhu.data.repository.PoemRepository
import com.xingzhu.data.source.CorpusLoader
import com.xingzhu.engine.PingZeEngine
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
