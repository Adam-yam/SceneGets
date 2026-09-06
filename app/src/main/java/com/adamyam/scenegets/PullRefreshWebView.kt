package com.adamyam.scenegets

import android.content.Context
import android.view.MotionEvent
import android.webkit.WebView
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Android View 레이어에서 pull-to-refresh를 처리한다.
 * JS touchmove/preventDefault에 의존하지 않아 Chromium gesture arbitration 문제를 피한다.
 */
class PullRefreshWebView(
    context: Context,
    private val onPullChanged: (Float) -> Unit,
    private val onRefresh: () -> Unit
) : WebView(context) {

    companion object {
        private const val THRESHOLD_PX = 64f
        private const val MAX_PULL_PX = 96f
        private const val START_EPSILON_PX = 1f
        private const val HORIZONTAL_RATIO = 1.25f
    }

    private var downX = 0f
    private var downY = 0f
    private var pulling = false
    private var pullDistance = 0f
    private var refreshing = false

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                pulling = false
                pullDistance = 0f
                parent?.requestDisallowInterceptTouchEvent(false)
                return super.dispatchTouchEvent(event)
            }

            MotionEvent.ACTION_MOVE -> {
                if (!refreshing && !pulling && !canScrollVertically(-1)) {
                    val dx = event.x - downX
                    val dy = event.y - downY

                    // 첫 번째 의미 있는 아래 방향 이동에서 즉시 gesture를 선점한다.
                    // threshold까지 기다리면 Chromium이 native scrolling으로 gesture를
                    // 확정하여 이후 preventDefault/cancel이 늦어질 수 있다.
                    if (dy > START_EPSILON_PX && dy > abs(dx) * HORIZONTAL_RATIO) {
                        pulling = true
                        pullDistance = 0f
                        parent?.requestDisallowInterceptTouchEvent(true)

                        // WebView/Chromium에 이미 전달된 DOWN/MOVE gesture를 취소한다.
                        // 이후 MOVE/UP은 이 View가 독점적으로 처리한다.
                        val cancel = MotionEvent.obtain(event)
                        cancel.action = MotionEvent.ACTION_CANCEL
                        super.dispatchTouchEvent(cancel)
                        cancel.recycle()
                    }
                }

                if (pulling) {
                    val dy = max(0f, event.y - downY)
                    pullDistance = min(MAX_PULL_PX, dy * 0.75f)
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
                    pullDistance = 0f
                    parent?.requestDisallowInterceptTouchEvent(false)

                    animate()
                        .translationY(0f)
                        .setDuration(180L)
                        .withEndAction {
                            onPullChanged(0f)
                            if (shouldRefresh && !refreshing) {
                                refreshing = true
                                onRefresh()
                            }
                        }
                        .start()
                    return true
                }
            }
        }

        return super.dispatchTouchEvent(event)
    }

    fun finishRefresh() {
        refreshing = false
        pulling = false
        pullDistance = 0f
        animate().translationY(0f).setDuration(180L).start()
        onPullChanged(0f)
    }
}
