package com.xingzhu.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PoemEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun poemDao(): PoemDao

    companion object {
        const val NAME = "xingzhu.db"

        /** v1 → v2：新增标题/作者拼音列（用于排序），旧数据由代码回填 */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE poem ADD COLUMN titlePinyin TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE poem ADD COLUMN authorPinyin TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
