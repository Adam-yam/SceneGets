package com.adamyam.scenegets

import android.content.Context
import android.view.MotionEvent
import android.webkit.WebView
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * WebView 자체에서 pull-to-refresh 제스처를 처리한다.
 *
 * Chromium/WebView의 JS preventDefault() 타이밍에 의존하지 않고,
 * WebView가 세로 스크롤을 시작하기 전에 Android MotionEvent 단계에서
 * 아래 방향 제스처를 가로채기 때문에 기기별 WebView 차이가 거의 없다.
 */
class PullRefreshWebView(
    context: Context,
    private val onPullChanged: (Float) -> Unit,
    private val onRefresh: () -> Unit
) : WebView(context) {

    companion object {
        private const val THRESHOLD_PX = 64f
        private const val MAX_PULL_PX = 96f
        private const val HORIZONTAL_SLOP_RATIO = 1.15f
    }

    private var downX = 0f
    private var downY = 0f
    private var pulling = false
    private var pullDistance = 0f
    private var refreshing = false

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                pulling = false
                pullDistance = 0f
                return super.onTouchEvent(event)
            }

            MotionEvent.ACTION_MOVE -> {
                if (!refreshing && !pulling) {
                    val dx = event.x - downX
                    val dy = event.y - downY

                    // 수평 스와이프/작은 손떨림은 WebView에 그대로 맡긴다.
                    // 이미 최상단이고 아래 방향으로 충분히 명확하게 움직였을 때만 전환.
                    if (dy > 0f && dy > abs(dx) * HORIZONTAL_SLOP_RATIO && !canScrollVertically(-1)) {
                        pulling = true
                        pullDistance = 0f

                        // WebView가 이미 스크롤/클릭 제스처로 처리하지 못하도록
                        // 현재 gesture를 CANCEL하고 이후 이벤트는 우리가 독점한다.
                        val cancel = MotionEvent.obtain(event)
                        cancel.action = MotionEvent.ACTION_CANCEL
                        super.onTouchEvent(cancel)
                        cancel.recycle()
                        parent?.requestDisallowInterceptTouchEvent(true)
                    }
                }

                if (pulling) {
                    val dy = max(0f, event.y - downY)
                    // 처음에는 자연스럽게, 많이 당길수록 저항을 증가시킨다.
                    pullDistance = min(MAX_PULL_PX, dy * 0.5f)
                    translationY = pullDistance
                    onPullChanged(pullDistance)
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (pulling) {
                    val shouldRefresh = event.actionMasked == MotionEvent.ACTION_UP &&
                        pullDistance >= THRESHOLD_PX
                    pulling = false
                    val wasPulling = pullDistance
                    pullDistance = 0f

                    parent?.requestDisallowInterceptTouchEvent(false)
                    animate()
                        .translationY(0f)
                        .setDuration(180L)
                        .withEndAction {
                            onPullChanged(0f)
                            if (shouldRefresh) {
                                refreshing = true
                                onRefresh()
                            }
                        }
                        .start()
                    return true
                }
            }
        }

        return super.onTouchEvent(event)
    }

    /** 데이터가 WebView에 도착하면 JS에서 호출해 새로고침 잠금을 해제한다. */
    fun finishRefresh() {
        refreshing = false
        animate().translationY(0f).setDuration(180L).start()
        onPullChanged(0f)
    }
}
