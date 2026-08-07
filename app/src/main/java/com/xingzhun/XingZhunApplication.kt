package com.xingzhun

import android.app.Application
import com.xingzhun.data.repository.PoemRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class XingZhunApplication : Application() {

    @Inject
    lateinit var repository: PoemRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        scope.launch { repository.ensureCorpusSeeded() }
    }
}
