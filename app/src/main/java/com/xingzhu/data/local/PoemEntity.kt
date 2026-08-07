package com.xingzhu.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "poem")
data class PoemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String,
    val dynasty: String? = null,
    val form: String = "",
    val contentText: String,
    /** AnnotatedPoem 的序列化缓存（M1 接入引擎后写入） */
    val annotationJson: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
)
