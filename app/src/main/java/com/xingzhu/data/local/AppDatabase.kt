package com.xingzhu.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PoemEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun poemDao(): PoemDao

    companion object {
        const val NAME = "xingzhu.db"
    }
}
