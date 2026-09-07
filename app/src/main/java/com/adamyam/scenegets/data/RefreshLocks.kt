package com.adamyam.scenegets.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object RefreshLocks {
    val chart = Mutex()
    val news = Mutex()
    val schedule = Mutex()
}
