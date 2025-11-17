package com.dps.droidpadmacos.touchpad

import android.view.MotionEvent
import kotlin.math.abs
import kotlin.math.roundToInt

class TouchpadGestureDetector(
    private val onMove: (deltaX: Int, deltaY: Int) -> Unit,
    private val onLeftClick: () -> Unit,
    private val onRightClick: () -> Unit,
    private val onScroll: (deltaY: Int) -> Unit
) {

    private var lastX = 0f
    private var lastY = 0f
    private var isMoving = false
    private var touchStartTime = 0L
    private var initialPointerCount = 0
    private var isScrolling = false
    private var lastScrollY = 0f

    // Sensitivity multipliers
    private var movementSensitivity = 2.5f
    private var scrollSensitivity = 1.0f

    // Tap detection
    private val tapTimeThreshold = 200L // milliseconds
    private val tapMovementThreshold = 20f // pixels

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
    }

    private fun handlePointerDown(event: MotionEvent) {
        if (event.pointerCount == 2) {
            // Two fingers detected - prepare for scroll or right-click
            initialPointerCount = 2
            isScrolling = false
            lastScrollY = calculateAverageY(event)
            touchStartTime = System.currentTimeMillis()
        }
    }

    private fun handleActionMove(event: MotionEvent) {
        when (event.pointerCount) {
            1 -> handleSingleFingerMove(event)
            2 -> handleTwoFingerMove(event)
        }
    }

    private fun handleSingleFingerMove(event: MotionEvent) {
        if (initialPointerCount != 1) return

        val currentX = event.x
        val currentY = event.y

        val deltaX = (currentX - lastX) * movementSensitivity
        val deltaY = (currentY - lastY) * movementSensitivity

        // Check if movement is significant enough
        if (abs(deltaX) > 1 || abs(deltaY) > 1) {
            isMoving = true
            android.util.Log.d("TouchpadGesture", "Raw delta - X: $deltaX, Y: $deltaY")
            onMove(deltaX.roundToInt(), deltaY.roundToInt())
        }

        lastX = currentX
        lastY = currentY
    }

    private fun handleTwoFingerMove(event: MotionEvent) {
        if (initialPointerCount != 2 || event.pointerCount < 2) return

        val currentY = calculateAverageY(event)
        val deltaY = (currentY - lastScrollY) * scrollSensitivity

        if (abs(deltaY) > 5) {
            isScrolling = true
            // Invert scroll direction for natural scrolling (like macOS default)
            onScroll((-deltaY / 10).roundToInt())
        }

        lastScrollY = currentY
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
                }
                reset()
            }
            MotionEvent.ACTION_POINTER_UP -> {
                // One finger lifted, but others remain
                if (event.pointerCount == 2) {
                    // From 2 fingers to 1
                    val remainingIndex = if (event.actionIndex == 0) 1 else 0
                    lastX = event.getX(remainingIndex)
                    lastY = event.getY(remainingIndex)
                }
            }
        }
    }

    private fun calculateAverageY(event: MotionEvent): Float {
        return if (event.pointerCount >= 2) {
            (event.getY(0) + event.getY(1)) / 2
        } else {
            event.y
        }
    }

    private fun reset() {
        isMoving = false
        isScrolling = false
        initialPointerCount = 0
    }

    fun setMovementSensitivity(sensitivity: Float) {
        movementSensitivity = sensitivity.coerceIn(0.5f, 5.0f)
    }

    fun setScrollSensitivity(sensitivity: Float) {
        scrollSensitivity = sensitivity.coerceIn(0.5f, 3.0f)
    }
}
