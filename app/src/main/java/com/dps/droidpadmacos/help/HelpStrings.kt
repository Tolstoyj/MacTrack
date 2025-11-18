package com.dps.droidpadmacos.help

/**
 * Multilingual help content for DroidPad
 * Supports: English, Spanish, French, German, Chinese, Japanese, Korean, Portuguese, Russian, Arabic
 */
object HelpStrings {

    enum class Language(val code: String, val displayName: String, val flag: String) {
        ENGLISH("en", "English", "🇺🇸"),
        SPANISH("es", "Español", "🇪🇸"),
        FRENCH("fr", "Français", "🇫🇷"),
        GERMAN("de", "Deutsch", "🇩🇪"),
        CHINESE("zh", "中文", "🇨🇳"),
        JAPANESE("ja", "日本語", "🇯🇵"),
        KOREAN("ko", "한국어", "🇰🇷"),
        PORTUGUESE("pt", "Português", "🇵🇹"),
        RUSSIAN("ru", "Русский", "🇷🇺"),
        ARABIC("ar", "العربية", "🇸🇦"),
        HINDI("hi", "हिंदी", "🇮🇳")
    }

    data class HelpContent(
        val title: String,
        val sections: List<HelpSection>
    )

    data class HelpSection(
        val title: String,
        val icon: String,
        val items: List<HelpItem>
    )

    data class HelpItem(
        val title: String,
        val description: String,
        val steps: List<String>? = null
    )

    fun getHelpContent(language: Language): HelpContent {
        return when (language) {
            Language.ENGLISH -> getEnglishContent()
            Language.SPANISH -> getSpanishContent()
            Language.FRENCH -> getFrenchContent()
            Language.GERMAN -> getGermanContent()
            Language.CHINESE -> getChineseContent()
            Language.JAPANESE -> getJapaneseContent()
            Language.KOREAN -> getKoreanContent()
            Language.PORTUGUESE -> getPortugueseContent()
            Language.RUSSIAN -> getRussianContent()
            Language.ARABIC -> getArabicContent()
            Language.HINDI -> getHindiContent()
        }
    }

    private fun getEnglishContent() = HelpContent(
        title = "DroidPad Help & Guide",
        sections = listOf(
            HelpSection(
                title = "Getting Started",
                icon = "🚀",
                items = listOf(
                    HelpItem(
                        title = "What is DroidPad?",
                        description = "Transform your Android phone into a wireless trackpad and keyboard for your Mac via Bluetooth. No additional software needed on Mac!",
                        steps = null
                    )
                )
            ),
            HelpSection(
                title = "Bluetooth Connection",
                icon = "📡",
                items = listOf(
                    HelpItem(
                        title = "First Time Setup",
                        description = "Follow these steps to connect via Bluetooth:",
                        steps = listOf(
                            "1. Tap 'Register' button in DroidPad",
                            "2. Allow discoverability when prompted",
                            "3. On Mac: Open System Settings → Bluetooth",
                            "4. Look for 'DroidPad Trackpad' (NOT your phone name)",
                            "5. Click 'Connect'",
                            "6. Wait for connection (you'll hear a beep)"
                        )
                    ),
                    HelpItem(
                        title = "Reconnection",
                        description = "After first setup, DroidPad remembers your Mac:",
                        steps = listOf(
                            "• App will auto-reconnect to last device",
                            "• Or select from recent devices list",
                            "• Or tap device name to connect manually"
                        )
                    ),
                    HelpItem(
                        title = "Troubleshooting Bluetooth",
                        description = "If connection fails:",
                        steps = listOf(
                            "⚠️ IMPORTANT: If previously paired, unpair/forget device first",
                            "• Ensure Bluetooth is ON on both devices",
                            "• Tap 'Reset All Connections' to start fresh",
                            "• Restart Bluetooth on Mac if needed",
                            "• Make sure phone is visible to Mac (accept discoverability)",
                            "• Try connecting from Mac Bluetooth settings"
                        )
                    )
                )
            ),
            HelpSection(
                title = "Trackpad Gestures",
                icon = "🖐️",
                items = listOf(
                    HelpItem(
                        title = "Basic Gestures",
                        description = "Standard trackpad controls:",
                        steps = listOf(
                            "• 1 Finger Drag: Move cursor",
                            "• 1 Finger Tap: Left click",
                            "• 2 Finger Tap: Right click",
                            "• 3 Finger Tap: Middle click",
                            "• 2 Finger Swipe: Scroll"
                        )
                    ),
                    HelpItem(
                        title = "macOS Gestures (Bluetooth Only)",
                        description = "Advanced multi-touch gestures:",
                        steps = listOf(
                            "• 3 Fingers Up: Mission Control",
                            "• 3 Fingers Down: Show Desktop",
                            "• 3 Fingers Left: Previous Space",
                            "• 3 Fingers Right: Next Space",
                            "• Pinch: Zoom in/out"
                        )
                    )
                )
            ),
            HelpSection(
                title = "Air Mouse Mode",
                icon = "📱",
                items = listOf(
                    HelpItem(
                        title = "What is Air Mouse?",
                        description = "Control cursor by tilting your phone in the air!",
                        steps = listOf(
                            "• Tap 'Air' button in full-screen mode to enable",
                            "• Tilt phone to move cursor",
                            "• Volume Down: Left click",
                            "• Volume Down (double tap): Double click",
                            "• Volume Down (long press): Toggle drag mode",
                            "• Volume Up: Right click"
                        )
                    ),
                    HelpItem(
                        title = "Air Mouse Tips",
                        description = "For best experience:",
                        steps = listOf(
                            "• Use in well-lit environment",
                            "• Keep phone movements smooth",
                            "• Calibrate by holding phone level",
                            "• Practice with small movements first"
                        )
                    )
                )
            ),
            HelpSection(
                title = "Keyboard Shortcuts",
                icon = "⌨️",
                items = listOf(
                    HelpItem(
                        title = "Built-in Shortcuts",
                        description = "Quick access keys in full-screen mode:",
                        steps = listOf(
                            "⌘C: Copy",
                            "⌘V: Paste",
                            "⌘X: Cut",
                            "⌘Z: Undo",
                            "⌘A: Select All",
                            "⌘T: New Tab",
                            "⌘W: Close Window",
                            "⌘Q: Quit App",
                            "⌘Space: Spotlight",
                            "ESC, ⌫ (Delete), ↵ (Enter)"
                        )
                    ),
                    HelpItem(
                        title = "System Controls",
                        description = "macOS system actions:",
                        steps = listOf(
                            "MC: Mission Control",
                            "Apps: App Switcher (⌘Tab)",
                            "Desk: Show Desktop",
                            "BG: Change background style"
                        )
                    )
                )
            ),
            HelpSection(
                title = "Common Issues",
                icon = "⚠️",
                items = listOf(
                    HelpItem(
                        title = "Can't Find 'DroidPad Trackpad'",
                        description = "Device not showing in Mac Bluetooth:",
                        steps = listOf(
                            "1. Ensure you tapped 'Register' in app",
                            "2. Check you accepted discoverability",
                            "3. If phone was previously paired, FORGET it first on Mac",
                            "4. Look for 'DroidPad Trackpad' NOT your phone name",
                            "5. Try 'Reset All Connections' in app"
                        )
                    ),
                    HelpItem(
                        title = "Connection Keeps Dropping",
                        description = "Unstable connection:",
                        steps = listOf(
                            "• Move closer to Mac (Bluetooth range: ~10m)",
                            "• Remove obstacles between devices",
                            "• Disable Wi-Fi/Bluetooth on other nearby devices",
                            "• Restart Bluetooth on both devices"
                        )
                    ),
                    HelpItem(
                        title = "Trackpad Not Responding",
                        description = "Cursor not moving:",
                        steps = listOf(
                            "• Check connection status indicator",
                            "• Verify you're on the trackpad screen",
                            "• Try tapping to wake up connection",
                            "• Restart the app",
                            "• Reconnect to Mac"
                        )
                    )
                )
            ),
            HelpSection(
                title = "Advanced Features",
                icon = "⚙️",
                items = listOf(
                    HelpItem(
                        title = "Recent Devices",
                        description = "Quick reconnection:",
                        steps = listOf(
                            "• App remembers up to 5 recent Macs",
                            "• Tap device name to reconnect instantly",
                            "• Clear history with 'Clear History' button"
                        )
                    ),
                    HelpItem(
                        title = "Background Styles",
                        description = "Customize trackpad appearance:",
                        steps = listOf(
                            "• Tap BG button to cycle through:",
                            "  - Gradient (default)",
                            "  - Solid color",
                            "  - Grid pattern"
                        )
                    ),
                    HelpItem(
                        title = "Connection Monitoring",
                        description = "Real-time status:",
                        steps = listOf(
                            "• Green badge: Connected",
                            "• Yellow badge: Waiting/Registering",
                            "• Red badge: Error/Disconnected",
                            "• Beep sound on successful connection"
                        )
                    )
                )
            ),
            HelpSection(
                title = "Tips & Tricks",
                icon = "💡",
                items = listOf(
                    HelpItem(
                        title = "Best Practices",
                        description = "For optimal experience:",
                        steps = listOf(
                            "✨ Enable 'Keep Screen On' in full-screen mode",
                            "✨ Clean your screen for better touch response",
                            "✨ Use landscape mode for larger trackpad area",
                            "✨ Stay within Bluetooth range (~10m) for best performance"
                        )
                    ),
                    HelpItem(
                        title = "Battery Saving",
                        description = "Extend battery life:",
                        steps = listOf(
                            "• Lower screen brightness",
                            "• Close app when not in use",
                            "• Unregister when done"
                        )
                    ),
                    HelpItem(
                        title = "Performance Optimization",
                        description = "Improve responsiveness:",
                        steps = listOf(
                            "• Close background apps",
                            "• Ensure good Bluetooth connection",
                            "• Keep devices within range",
                            "• Update to latest app version"
                        )
                    )
                )
            ),
            HelpSection(
                title = "Technical Details",
                icon = "🔧",
                items = listOf(
                    HelpItem(
                        title = "System Requirements",
                        description = "What you need:",
                        steps = listOf(
                            "Android:",
                            "• Android 6.0 or higher",
                            "• Bluetooth 4.0+",
                            "",
                            "Mac:",
                            "• macOS 10.12 or higher",
                            "• Bluetooth 4.0+"
                        )
                    ),
                    HelpItem(
                        title = "HID Protocol",
                        description = "How it works:",
                        steps = listOf(
                            "• Uses standard Bluetooth HID protocol",
                            "• No Mac software needed (uses built-in HID support)",
                            "• Emulates Apple Magic Trackpad",
                            "• Full multi-touch gesture support"
                        )
                    ),
                    HelpItem(
                        title = "Privacy & Security",
                        description = "Your data is safe:",
                        steps = listOf(
                            "✅ No data collection",
                            "✅ No internet required",
                            "✅ Local connection only",
                            "✅ No keylogging",
                            "✅ Open source code"
                        )
                    )
                )
            )
        )
    )

    private fun getSpanishContent() = HelpContent(
        title = "Ayuda y Guía de DroidPad",
        sections = listOf(
            HelpSection(
                title = "Primeros Pasos",
                icon = "🚀",
                items = listOf(
                    HelpItem(
                        title = "¿Qué es DroidPad?",
                        description = "Transforma tu teléfono Android en un trackpad inalámbrico y teclado para tu Mac. ¡No necesitas software adicional en Mac!",
                        steps = null
                    ),
                )
            ),
            HelpSection(
                title = "Conexión Bluetooth",
                icon = "📡",
                items = listOf(
                    HelpItem(
                        title = "Configuración Inicial",
                        description = "Sigue estos pasos para conectar vía Bluetooth:",
                        steps = listOf(
                            "1. Toca el botón 'Registrar' en DroidPad",
                            "2. Permite la visibilidad cuando se solicite",
                            "3. En Mac: Abre Configuración del Sistema → Bluetooth",
                            "4. Busca 'DroidPad Trackpad' (NO el nombre de tu teléfono)",
                            "5. Haz clic en 'Conectar'",
                            "6. Espera la conexión (escucharás un pitido)"
                        )
                    )
                )
            ),
            HelpSection(
                title = "Gestos del Trackpad",
                icon = "🖐️",
                items = listOf(
                    HelpItem(
                        title = "Gestos Básicos",
                        description = "Controles estándar del trackpad:",
                        steps = listOf(
                            "• Arrastrar con 1 dedo: Mover cursor",
                            "• Tocar con 1 dedo: Clic izquierdo",
                            "• Tocar con 2 dedos: Clic derecho",
                            "• Tocar con 3 dedos: Clic central",
                            "• Deslizar con 2 dedos: Desplazar"
                        )
                    )
                )
            ),
            HelpSection(
                title = "Problemas Comunes",
                icon = "⚠️",
                items = listOf(
                    HelpItem(
                        title = "No Encuentro 'DroidPad Trackpad'",
                        description = "El dispositivo no aparece en Bluetooth de Mac:",
                        steps = listOf(
                            "1. Asegúrate de haber tocado 'Registrar' en la app",
                            "2. Verifica que aceptaste la visibilidad",
                            "3. Si el teléfono estaba emparejado antes, OLVÍDALO primero en Mac",
                            "4. Busca 'DroidPad Trackpad' NO el nombre de tu teléfono",
                            "5. Intenta 'Restablecer Todas las Conexiones' en la app"
                        )
                    )
                )
            )
        )
    )

    // Simplified versions for other languages - can be expanded
    private fun getFrenchContent() = getEnglishContent().copy(title = "Aide et Guide DroidPad")
    private fun getGermanContent() = getEnglishContent().copy(title = "DroidPad Hilfe & Anleitung")
    private fun getChineseContent() = getEnglishContent().copy(title = "DroidPad 帮助与指南")
    private fun getJapaneseContent() = getEnglishContent().copy(title = "DroidPad ヘルプ＆ガイド")
    private fun getKoreanContent() = getEnglishContent().copy(title = "DroidPad 도움말 및 가이드")
    private fun getPortugueseContent() = getEnglishContent().copy(title = "Ajuda e Guia do DroidPad")
    private fun getRussianContent() = getEnglishContent().copy(title = "Справка и Руководство DroidPad")
    private fun getArabicContent() = getEnglishContent().copy(title = "مساعدة ودليل DroidPad")
    private fun getHindiContent() = getEnglishContent().copy(title = "DroidPad सहायता और मार्गदर्शिका")
}
