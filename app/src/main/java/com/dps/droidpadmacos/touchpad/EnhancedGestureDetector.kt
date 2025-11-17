package com.dps.droidpadmacos.touchpad

import android.view.MotionEvent
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

class EnhancedGestureDetector(
    private val onMove: (deltaX: Int, deltaY: Int) -> Unit,
    private val onLeftClick: () -> Unit,
    private val onRightClick: () -> Unit,
    private val onMiddleClick: () -> Unit,
    private val onScroll: (deltaY: Int) -> Unit,
    private val onThreeFingerSwipeUp: () -> Unit,
    private val onThreeFingerSwipeDown: () -> Unit,
    private val onFourFingerSwipeLeft: () -> Unit,
    private val onFourFingerSwipeRight: () -> Unit,
    private val onPinchZoom: (scale: Float) -> Unit
) {

    private var lastX = 0f
    private var lastY = 0f
    private var isMoving = false
    private var touchStartTime = 0L
    private var initialPointerCount = 0
    private var isScrolling = false
    private var lastScrollY = 0f
    private var isGesturing = false

    // For three/four finger gestures
    private var gestureStartX = 0f
    private var gestureStartY = 0f

    // For pinch zoom
    private var initialDistance = 0f

    // Sensitivity settings
    private var movementSensitivity = 2.5f
    private var scrollSensitivity = 1.0f

    // Thresholds
    private val tapTimeThreshold = 200L
    private val tapMovementThreshold = 20f
    private val gestureThreshold = 100f

    fun handleTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                handleActionDown(event)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                handlePointerDown(event)
            }
            MotionEvent.ACTION_MOVE -> {
                handleActionMove(event)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                handleActionUp(event)
            }
            MotionEvent.ACTION_CANCEL -> {
                reset()
            }
        }
        return true
    }

    private fun handleActionDown(event: MotionEvent) {
        lastX = event.x
        lastY = event.y
        touchStartTime = System.currentTimeMillis()
        initialPointerCount = 1
        isMoving = false
        isScrolling = false
        isGesturing = false
    }

    private fun handlePointerDown(event: MotionEvent) {
        when (event.pointerCount) {
            2 -> {
                initialPointerCount = 2
                isScrolling = false
                lastScrollY = calculateAverageY(event)
                touchStartTime = System.currentTimeMillis()
                initialDistance = calculateDistance(event)
            }
            3 -> {
                initialPointerCount = 3
                isGesturing = false
                gestureStartX = calculateAverageX(event)
                gestureStartY = calculateAverageY(event)
                touchStartTime = System.currentTimeMillis()
            }
            4 -> {
                initialPointerCount = 4
                isGesturing = false
                gestureStartX = calculateAverageX(event)
                touchStartTime = System.currentTimeMillis()
            }
        }
    }

    private fun handleActionMove(event: MotionEvent) {
        when (event.pointerCount) {
            1 -> handleSingleFingerMove(event)
            2 -> handleTwoFingerMove(event)
            3 -> handleThreeFingerMove(event)
            4 -> handleFourFingerMove(event)
        }
    }

    private fun handleSingleFingerMove(event: MotionEvent) {
        if (initialPointerCount != 1) return

        val currentX = event.x
        val currentY = event.y

        val deltaX = (currentX - lastX) * movementSensitivity
        val deltaY = (currentY - lastY) * movementSensitivity

        if (abs(deltaX) > 1 || abs(deltaY) > 1) {
            isMoving = true
            onMove(deltaX.roundToInt(), deltaY.roundToInt())
        }

        lastX = currentX
        lastY = currentY
    }

    private fun handleTwoFingerMove(event: MotionEvent) {
        if (initialPointerCount != 2 || event.pointerCount < 2) return

        // Check for pinch zoom
        val currentDistance = calculateDistance(event)
        val distanceDelta = currentDistance - initialDistance

        if (abs(distanceDelta) > 20) {
            val scale = currentDistance / initialDistance
            onPinchZoom(scale)
            initialDistance = currentDistance
        } else {
            // Scroll
            val currentY = calculateAverageY(event)
            val deltaY = (currentY - lastScrollY) * scrollSensitivity

            if (abs(deltaY) > 5) {
                isScrolling = true
                // Natural scrolling like macOS
                onScroll((-deltaY / 10).roundToInt())
            }

            lastScrollY = currentY
        }
    }

    private fun handleThreeFingerMove(event: MotionEvent) {
        if (initialPointerCount != 3 || event.pointerCount < 3) return

        val currentY = calculateAverageY(event)
        val deltaY = currentY - gestureStartY

        if (!isGesturing && abs(deltaY) > gestureThreshold) {
            isGesturing = true
            if (deltaY > 0) {
                // Swipe down - Show all windows (Mission Control)
                onThreeFingerSwipeDown()
            } else {
                // Swipe up - Show desktop
                onThreeFingerSwipeUp()
            }
        }
    }

    private fun handleFourFingerMove(event: MotionEvent) {
        if (initialPointerCount != 4 || event.pointerCount < 4) return

        val currentX = calculateAverageX(event)
        val deltaX = currentX - gestureStartX

        if (!isGesturing && abs(deltaX) > gestureThreshold) {
            isGesturing = true
            if (deltaX > 0) {
                // Swipe right - Forward
                onFourFingerSwipeRight()
            } else {
                // Swipe left - Back
                onFourFingerSwipeLeft()
            }
        }
    }

    private fun handleActionUp(event: MotionEvent) {
        val touchDuration = System.currentTimeMillis() - touchStartTime
        val isQuickTap = touchDuration < tapTimeThreshold

        when (event.actionMasked) {
            MotionEvent.ACTION_UP -> {
                // Last finger lifted
                if (initialPointerCount == 1 && !isMoving && isQuickTap) {
                    // Single tap - left click
                    onLeftClick()
                } else if (initialPointerCount == 2 && !isScrolling && isQuickTap) {
                    // Two-finger tap - right click
                    onRightClick()
                } else if (initialPointerCount == 3 && !isGesturing && isQuickTap) {
                    // Three-finger tap - middle click (optional)
                    onMiddleClick()
                }
                reset()
            }
            MotionEvent.ACTION_POINTER_UP -> {
                // One finger lifted, but others remain
                if (event.pointerCount == 2) {
                    val remainingIndex = if (event.actionIndex == 0) 1 else 0
                    lastX = event.getX(remainingIndex)
                    lastY = event.getY(remainingIndex)
                }
            }
        }
    }

    private fun calculateAverageX(event: MotionEvent): Float {
        var sum = 0f
        for (i in 0 until event.pointerCount) {
            sum += event.getX(i)
        }
        return sum / event.pointerCount
    }

    private fun calculateAverageY(event: MotionEvent): Float {
        var sum = 0f
        for (i in 0 until event.pointerCount) {
            sum += event.getY(i)
        }
        return sum / event.pointerCount
    }

    private fun calculateDistance(event: MotionEvent): Float {
        if (event.pointerCount < 2) return 0f
        val dx = event.getX(0) - event.getX(1)
        val dy = event.getY(0) - event.getY(1)
        return sqrt(dx * dx + dy * dy)
    }

    private fun reset() {
        isMoving = false
        isScrolling = false
        isGesturing = false
        initialPointerCount = 0
    }

    fun setMovementSensitivity(sensitivity: Float) {
        movementSensitivity = sensitivity.coerceIn(0.5f, 5.0f)
    }

    fun setScrollSensitivity(sensitivity: Float) {
        scrollSensitivity = sensitivity.coerceIn(0.5f, 3.0f)
    }

    fun triggerMissionControl() {
        onThreeFingerSwipeUp()
    }

    fun triggerShowDesktop() {
        onThreeFingerSwipeDown()
    }
}
