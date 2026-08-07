package com.xingzhu.di

import android.content.Context
import androidx.room.Room
import com.xingzhu.data.local.AppDatabase
import com.xingzhu.data.local.PoemDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME).build()

    @Provides
    fun providePoemDao(db: AppDatabase): PoemDao = db.poemDao()
}
