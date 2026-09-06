package com.adamyam.scenegets.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.adamyam.scenegets.data.ImageCache

/**
 * 뉴스 썸네일/앨범 커버 캐시 폴더를 주기적으로 정리한다.
 * 위젯 자동 갱신(WorkManager)만으로도 캐시가 계속 쌓일 수 있으므로,
 * 앱 실행 여부와 무관하게 하루 한 번 이 워커가 오래되거나 용량을 초과한 파일을 지운다.
 */
class ImageCacheCleanupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        ImageCache.cleanup(applicationContext)
        return Result.success()
    }

    companion object {
        const val UNIQUE_PERIODIC = "image_cache_cleanup_periodic"
    }
}
