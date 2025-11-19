package com.dps.droidpadmacos.bluetooth

/**
 * Interface Segregation: Small, focused interfaces for HID operations
 * Dependency Inversion: ViewModels depend on these abstractions, not concrete implementations
 */

/**
 * Interface for mouse output operations
 */
interface MouseOutput {
    fun sendMouseMovement(deltaX: Int, deltaY: Int): Boolean
    fun sendLeftClick(): Boolean
    fun sendRightClick(): Boolean
    fun sendScroll(deltaY: Int): Boolean
    fun sendMouseButtonPress(button: Byte): Boolean
    fun sendMouseButtonRelease(): Boolean
}

/**
 * Interface for keyboard output operations
 */
interface KeyboardOutput {
    suspend fun sendKeyPress(keyCode: Byte)
    suspend fun sendKeyPress(modifiers: Byte, keyCode: Byte)
}

/**
 * Interface for system control operations
 */
interface SystemControlOutput {
    suspend fun sendMissionControl()
    suspend fun sendShowDesktop()
    suspend fun sendAppSwitcher()
    suspend fun sendSpotlight()
    suspend fun sendSwitchToPreviousDesktop()
    suspend fun sendSwitchToNextDesktop()
}

/**
 * Interface for media control operations
 */
interface MediaControlOutput {
    suspend fun sendVolumeUp()
    suspend fun sendVolumeDown()
    suspend fun sendMute()
    suspend fun sendBrightnessUp()
    suspend fun sendBrightnessDown()
}

/**
 * Interface for keyboard shortcuts
 */
interface KeyboardShortcutOutput {
    suspend fun sendCopy()
    suspend fun sendPaste()
    suspend fun sendCut()
    suspend fun sendUndo()
    suspend fun sendSelectAll()
    suspend fun sendNewTab()
    suspend fun sendCloseWindow()
    suspend fun sendQuitApp()
}

/**
 * Combined interface for all HID outputs
 * This is what BluetoothHidService implements
 */
interface HidOutput :
    MouseOutput,
    KeyboardOutput,
    SystemControlOutput,
    MediaControlOutput,
    KeyboardShortcutOutput

/**
 * Adapter to make BluetoothHidService implement HidOutput
 * This allows gradual migration without breaking existing code
 */
class HidServiceAdapter(
    private val hidService: BluetoothHidService
) : HidOutput {

    // MouseOutput implementation
    override fun sendMouseMovement(deltaX: Int, deltaY: Int): Boolean {
        // Convert to HID report format
        val sendX = deltaX.coerceIn(-127, 127).toByte()
        val sendY = deltaY.coerceIn(-127, 127).toByte()
        return hidService.sendMouseReport(0, sendX, sendY, 0)
    }

    override fun sendLeftClick(): Boolean {
        // Click is a press and release sequence
        hidService.sendMouseReport(HidConstants.BUTTON_LEFT, 0, 0, 0)
        Thread.sleep(50)
        return hidService.sendMouseReport(HidConstants.BUTTON_NONE, 0, 0, 0)
    }

    override fun sendRightClick(): Boolean {
        hidService.sendMouseReport(HidConstants.BUTTON_RIGHT, 0, 0, 0)
        Thread.sleep(50)
        return hidService.sendMouseReport(HidConstants.BUTTON_NONE, 0, 0, 0)
    }

    override fun sendScroll(deltaY: Int): Boolean {
        val scrollAmount = deltaY.coerceIn(-127, 127).toByte()
        return hidService.sendMouseReport(HidConstants.BUTTON_NONE, 0, 0, scrollAmount)
    }

    override fun sendMouseButtonPress(button: Byte): Boolean {
        return hidService.sendMouseReport(button, 0, 0, 0)
    }

    override fun sendMouseButtonRelease(): Boolean {
        return hidService.sendMouseReport(HidConstants.BUTTON_NONE, 0, 0, 0)
    }

    // KeyboardOutput implementation
    override suspend fun sendKeyPress(keyCode: Byte) {
        hidService.sendKeyPress(HidConstants.MOD_NONE, keyCode)
    }

    override suspend fun sendKeyPress(modifiers: Byte, keyCode: Byte) {
        hidService.sendKeyPress(modifiers, keyCode)
    }

    // SystemControlOutput implementation
    override suspend fun sendMissionControl() = hidService.sendMissionControl()
    override suspend fun sendShowDesktop() = hidService.sendShowDesktop()
    override suspend fun sendAppSwitcher() = hidService.sendAppSwitcher()
    override suspend fun sendSpotlight() {
        hidService.sendKeyPress(HidConstants.MOD_LEFT_GUI, HidConstants.KEY_SPACE)
    }
    override suspend fun sendSwitchToPreviousDesktop() = hidService.sendSwitchToPreviousDesktop()
    override suspend fun sendSwitchToNextDesktop() = hidService.sendSwitchToNextDesktop()

    // MediaControlOutput implementation
    override suspend fun sendVolumeUp() = hidService.sendVolumeUp()
    override suspend fun sendVolumeDown() = hidService.sendVolumeDown()
    override suspend fun sendMute() = hidService.sendMute()
    override suspend fun sendBrightnessUp() = hidService.sendBrightnessUp()
    override suspend fun sendBrightnessDown() = hidService.sendBrightnessDown()

    // KeyboardShortcutOutput implementation
    override suspend fun sendCopy() {
        hidService.sendKeyPress(HidConstants.MOD_LEFT_GUI, HidConstants.KEY_C)
    }

    override suspend fun sendPaste() {
        hidService.sendKeyPress(HidConstants.MOD_LEFT_GUI, HidConstants.KEY_V)
    }

    override suspend fun sendCut() {
        hidService.sendKeyPress(HidConstants.MOD_LEFT_GUI, HidConstants.KEY_X)
    }

    override suspend fun sendUndo() {
        hidService.sendKeyPress(HidConstants.MOD_LEFT_GUI, HidConstants.KEY_Z)
    }

    override suspend fun sendSelectAll() {
        hidService.sendKeyPress(HidConstants.MOD_LEFT_GUI, HidConstants.KEY_A)
    }

    override suspend fun sendNewTab() {
        hidService.sendKeyPress(HidConstants.MOD_LEFT_GUI, HidConstants.KEY_T)
    }

    override suspend fun sendCloseWindow() {
        hidService.sendKeyPress(HidConstants.MOD_LEFT_GUI, HidConstants.KEY_W)
    }

    override suspend fun sendQuitApp() {
        hidService.sendKeyPress(HidConstants.MOD_LEFT_GUI, HidConstants.KEY_Q)
    }
}